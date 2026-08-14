# 채팅 Firestore → MySQL 마이그레이션 참고

> 최종 수정일: 2026-03-28
> 유지 규칙: 채팅 데이터 이관 관련 신규 발견사항이 생기면 이 문서를 먼저 갱신하고, 필요 시 `api-specification.md`, `domain-analysis.md`, `erd.md`, `implementation-roadmap.md`에도 함께 반영한다.

---

## 1. 문서 목적

이 문서는 현재 Firebase/Firestore 기반 채팅 데이터를 나중에 Spring/MySQL로 옮길 때
반드시 알고 있어야 하는 구조, 변환 포인트, 주의사항을 기록한다.

이 문서는 한 번 쓰고 끝나는 메모가 아니다.

- Firestore 실데이터를 추가로 확인했을 때
- 이관 스크립트 설계를 시작했을 때
- 커스텀 채팅방, 읽음 상태, 요약 필드 정책이 바뀌었을 때

위와 같은 변화가 생기면 계속 덧붙여 갱신해야 한다.

또한 이 문서는 프론트/백엔드 레포에 동시에 유지한다.
문서가 바뀌면 상대 레포의 동일 문서도 즉시 같은 내용으로 동기화한다.

---

## 2. 이번 정리의 근거

이번 정리는 아래 자료를 기준으로 작성했다.

- 프론트 레포의 Firestore 구조 문서
  - `SKTaxi/docs/project/firestore-data-structure.md`
- 프론트 레포의 기존 Firebase/Functions 코드
  - `firebase-cloud-functions/src/index.ts`
  - 기존 Firestore 의존 경로
- 현재 Spring/MySQL 백엔드 채팅 구현
  - `ChatService`, `ChatRoom`, `ChatRoomMember`, `ChatMessage`
  - 공개 채팅방 seed/backfill, join/leave/create 계약

참고:

- 이번 환경에서는 Firebase MCP로 현재 Firestore 실데이터에 직접 접속하지 못했다.
- 이유: MCP 환경에 활성 Firebase project가 연결되어 있지 않았다.
- 따라서 Firestore 실데이터 최신 상태는 위 문서와 코드 기준으로 추정/정리했다.
- 나중에 실제 Firebase project에 붙을 수 있으면 이 문서에 “실데이터 검증 결과”를 추가해야 한다.

---

## 3. 현재 Firestore 채팅 데이터 구조 요약

### 3.1 공개 채팅방

- 컬렉션: `chatRooms/{chatRoomId}`
- 하위 메시지: `chatRooms/{chatRoomId}/messages/{messageId}`
- 사용자별 읽음 상태:
  - `users/{uid}/chatRoomStates/{chatRoomId}`
- 사용자별 알림 설정:
  - `users/{uid}/chatRoomNotifications/{chatRoomId}`

핵심 필드:

- room:
  - `name`
  - `type` (`university`, `department`, `game`, `custom`)
  - `department`
  - `description`
  - `createdBy`
  - `members[]`
  - `isPublic`
  - `maxMembers`
  - `lastMessage{text,senderId,senderName,timestamp}`
  - `unreadCount.{uid}` (과거 필드, 현재는 사실상 비권장)
  - `createdAt`
  - `updatedAt`
- message:
  - `text`
  - `senderId`
  - `senderName`
  - `type` (`text`, `image`, `system`)
  - `readBy[]` (과거 필드, 현재는 사실상 비권장)
  - `createdAt`
  - `clientCreatedAt`

### 3.2 택시 파티 채팅

- 컬렉션: `chats/{partyId}/messages/{messageId}`

핵심 필드:

- `partyId`
- `senderId`
- `senderName`
- `type` (`user`, `system`, `account`, `arrived`, `end`)
- `text`
- `accountData`
- `arrivalData`
- `createdAt`
- `updatedAt`

### 3.3 마인크래프트 연계

- RTDB: `mc_chat/messages`
- Cloud Functions가 RTDB 이벤트를 받아 Firestore `chatRooms/.../messages`에 동기화했다.

즉, 현재 Firebase 쪽 채팅은

- 공개 채팅방 메타데이터
- 공개 채팅방 메시지
- 파티 채팅 메시지
- 사용자별 읽음 상태
- 사용자별 채팅방 알림 설정

이 서로 분산된 구조다.

---

## 4. 현재 MySQL/Spring 채팅 구조 요약

### 4.1 핵심 테이블/엔티티

- `chat_rooms`
- `chat_room_members`
- `chat_messages`

### 4.2 공개 채팅방 정책

- 공식 공개방 seed:
  - `public:university`
  - `public:game:minecraft`
  - 학과방 전체 (`public:department:<sha256-prefix>`)
- 공개방 visibility:
  - `UNIVERSITY`, `GAME`, `CUSTOM`: 전체 사용자 노출
  - `DEPARTMENT`: 본인 학과와 일치하는 방만 노출
- 미참여 public room:
  - 목록/상세 조회 가능
  - 메시지 조회 불가
  - unread는 0
- 참여/나가기:
  - `POST /v1/chat-rooms/{id}/join`
  - `DELETE /v1/chat-rooms/{id}/members/me`
- 커스텀 공개방 생성:
  - `POST /v1/chat-rooms`
  - 생성자는 자동 joined

### 4.3 요약 필드

MySQL 쪽은 목록 성능을 위해 room summary 필드를 room 자체에 유지한다.

예:

- `memberCount`
- `messageCount`
- `lastMessageType`
- `lastMessageText`
- `lastMessageSenderName`
- `lastMessageTimestamp`

즉 Firestore처럼 메시지 컬렉션만 보고 실시간 계산하는 구조가 아니라,
요약 필드가 이미 정리된 상태를 전제로 한다.

### 4.4 메시지 응답 계약 보강

- 현재 Spring 채팅 REST/STOMP payload는 메시지 단위로 `senderPhotoUrl`을 함께 내려준다.
- `senderPhotoUrl`의 source of truth는 항상 `members.photo_url`이다.
- `linked_accounts.photo_url` fallback은 사용하지 않는다.
- `chat_messages` 테이블에 별도 컬럼을 추가해 저장하지 않고, 응답 시점에 `senderId -> members.photo_url`로 매핑한다.
- 프로필 사진이 없으면 `senderPhotoUrl`은 필드 생략이 아니라 명시적 `null`로 직렬화한다.

---

## 5. Firestore → MySQL 이관 시 가장 중요한 주의사항

### 5.1 공식 공개방은 현재 canonical ID에 맞춰 넣어야 한다

현재 공개방 seed/backfill은 아래 ID를 기준으로 동작한다.

- `public:university`
- `public:game:minecraft`
- `public:department:<sha256-prefix>`

따라서 Firestore 데이터를 이관할 때도 공식 공개방은 이 ID들로 맞춰 넣는 것이 가장 안전하다.

반대로 Firestore에 있던 기존 room ID를 그대로 들고 오면:

- startup seeder가 공식 공개방을 새로 만들 수 있고
- 공개방이 중복될 수 있다.

즉, 공식 공개방은 “기존 Firestore ID 보존”보다
“현재 Spring 정책이 기대하는 canonical ID로 정규화”가 우선이다.

### 5.2 방 데이터만 옮기면 안 된다

이관 대상은 최소한 아래 3개다.

- `ChatRoom`
- `ChatRoomMember`
- `ChatMessage`

그리고 room summary 필드도 같이 맞춰야 한다.

- `memberCount`
- `messageCount`
- `lastMessageType`
- `lastMessageText`
- `lastMessageSenderName`
- `lastMessageTimestamp`

즉 단순 문서 복사가 아니라,
room/member/message와 summary를 함께 맞추는 ETL이 필요하다.

### 5.3 memberId 정합성이 먼저 확보되어야 한다

채팅 membership과 메시지 sender는 결국 `memberId`에 묶여 있다.

따라서 채팅 데이터 이관 전에:

- Firestore 시절 uid
- MySQL member id

가 같은 체계인지 먼저 확인해야 한다.

이게 맞지 않으면:

- senderName/작성자
- room membership
- unread 계산

이 모두 깨진다.

### 5.4 학과방은 현재 departments master 기준과 맞춰야 한다

현재 학과방 seed는 `departments.active = true`인 학과를 기준으로 생성된다.

따라서 Firestore 학과방을 옮길 때:

- 이름은 `{학과명} 채팅방`
- department 값은 현재 `departments.name` 값과 일치
- room id는 `public:department:<sha256-prefix>`

로 맞춰야 한다.

기존 Firestore 학과명 표기가 달랐다면 정규화 맵핑이 필요하다.

### 5.5 Firestore의 과거 필드와 현재 source of truth를 구분해야 한다

현재 Firestore 구조 문서 기준으로 아래 필드는 과거 흔적일 수 있다.

- `chatRooms.unreadCount.{uid}`
- `messages.readBy[]`

현재 Spring/MySQL은 이 구조를 그대로 가져가지 않는다.

대신:

- `chat_room_members.last_read_at`
- room summary + message count

를 기준으로 unread를 계산한다.

따라서 과거 Firestore unread/readBy 필드는
그대로 복사하기보다 변환 전략을 정해야 한다.

---

## 6. 지금 상태에서 추천하는 이관 전략

### 6.1 공식 공개방

권장:

- Firestore 공개방을 그대로 복제하지 말고
- 현재 Spring 정책 기준 canonical room으로 재생성/정규화

즉,

- 학교 전체방
- 마인크래프트방
- 학과방

은 MySQL 쪽 현재 계약을 기준으로 다시 세우는 편이 안전하다.

### 6.2 기존 membership

선택지가 2개다.

1. membership까지 이관
2. membership은 비우고 사용자가 다시 join

현재 일반 공개 채팅방 정책이 “수동 참여”이므로,
membership을 굳이 전부 옮기지 않고 다시 join하게 하는 것도 가능하다.

이 경우 장점:

- 이관 복잡도 감소
- 학과 변경/공개방 visibility 정책 재정렬이 쉬움

### 6.3 메시지 이력

메시지 이력이 정말 필요한 경우에만 이관한다.

공개방 메시지 이력까지 옮기려면:

- message type 매핑
- senderId 정합성
- createdAt/clientCreatedAt 해석
- room summary 재계산

이 함께 필요하다.

즉 메시지 이력까지 포함하면 난이도는 꽤 올라간다.

### 6.4 커스텀방

커스텀방은 정책상 공개 탐색 가능한 방이다.

따라서 이관 시 아래 중 하나를 선택해야 한다.

1. Firestore 커스텀방과 membership/messages를 함께 옮긴다
2. 공식 공개방만 먼저 MySQL로 운영하고, 커스텀방은 새 체계부터 다시 시작한다

이 선택은 데이터 보존 요구에 따라 달라진다.

---

## 7. 나중에 실제 이관 작업을 시작할 때 반드시 추가 확인할 것

- Firebase MCP나 별도 스크립트로 실제 Firestore `chatRooms`, `users/*/chatRoomStates`, `users/*/chatRoomNotifications`, `chats/*/messages` 표본 조회
- 공식 공개방 ID/이름/설명/department 값 실제 현황
- Firestore의 커스텀방 수와 메시지 양
- member uid 체계가 현재 MySQL member id와 일치하는지
- unread/readBy 과거 필드를 어떤 규칙으로 버리거나 변환할지
- Minecraft 방이 현재도 별도 동기화 규칙을 가지는지

이 확인이 끝나면 그때:

- 이관 범위
- ID 매핑 표
- ETL 순서
- 검증 체크리스트

를 이 문서에 추가한다.

---

## 8. 현재 결론

- 지금 공개방 seed는 “진짜 SQL migration 파일”이 아니라 `ApplicationReadyEvent` 기반 idempotent seeder다.
- 공식 공개방은 현재 Spring이 기대하는 canonical ID로 맞춰 이관하는 것이 안전하다.
- Firestore → MySQL 채팅 이관은 단순 room 복사가 아니라 room/member/message/summary를 함께 맞춰야 한다.
- membership과 메시지 이력을 어디까지 옮길지는 별도 결정이 필요하다.
- 공식 공개방은 재정규화, 커스텀방은 별도 판단 전략이 현실적이다.

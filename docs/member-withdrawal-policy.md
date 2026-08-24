# Member 탈퇴 정책

> 최종 수정일: 2026-08-24
> 관련 문서: [API 명세](./api-specification.md) | [도메인 분석](./domain-analysis.md) | [ERD](./erd.md) | [구현 로드맵](./implementation-roadmap.md) | [친구 기능 기준 명세](./features/friends.md)

---

## 1. 목적

Phase 10은 회원 탈퇴를 단순 row 삭제가 아니라 계정 라이프사이클 정책으로 다룬다.

- 탈퇴 후 인증/인가를 즉시 차단한다.
- 연관 도메인 데이터 정합성을 유지한다.
- 운영상 필요한 최소 이력은 남기되 개인정보는 제거한다.
- 재가입 정책을 `POST /v1/members`의 멱등 정책과 충돌 없이 정의한다.

---

## 2. 기본 정책

### 2.1 탈퇴 방식

- `members`는 hard delete 하지 않는다.
- `members.status = WITHDRAWN`, `members.withdrawn_at`을 기록하는 soft delete tombstone 방식을 사용한다.
- 탈퇴 시 회원 row에 남아 있는 개인정보는 스크럽한다.
  - `email`은 unique 충돌 방지를 위해 `withdrawn+{SHA256(uid)}@deleted.skuri.local` 형식의 placeholder 값으로 치환한다.
  - `studentId`, `department`, `photoUrl`, `realname`, 계좌 정보는 제거한다.
  - `nickname`은 `탈퇴한 사용자`로 고정한다.
  - ACTIVE 닉네임 고유 키 `nickname_key`는 null로 비워 탈퇴 후 닉네임 재사용을 허용한다.
  - `notificationSetting`은 전체 비활성 기본값으로 재설정한다.

### 2.2 탈퇴 시점

- 유예 기간 없이 즉시 탈퇴한다.
- 탈퇴 완료 직후 보호 API 접근은 `403 MEMBER_WITHDRAWN`으로 차단한다.

### 2.3 재가입 정책

- 같은 Firebase UID로는 재가입할 수 없다.
- `POST /v1/members`는 활성 회원에 대해서만 기존 회원 반환(멱등)을 유지한다.
- 탈퇴한 동일 UID가 `POST /v1/members`를 호출하면 `409 WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`를 반환한다.
- 재가입은 새 Firebase user, 즉 새 UID 발급 케이스만 허용한다.

### 2.4 Firebase Auth 처리

- 로컬 DB 탈퇴 처리와 Firebase Auth 삭제는 하나의 DB 트랜잭션으로 묶지 않는다.
- DB 트랜잭션 커밋 후 after-commit 후처리에서 Firebase user 삭제를 best-effort로 시도한다.
- Firebase 삭제 실패 시에도 서버는 withdrawn 상태를 기준으로 즉시 접근을 차단한다.

### 2.5 배포 마이그레이션 정책

- `members.status`는 엔티티/스키마 기준 `NOT NULL`을 유지한다.
- 따라서 Phase 10 이전 운영 DB를 업그레이드할 때는 앱 기동 전에 legacy 회원 row의 `status`를 수동으로 채워야 한다.
- 현재 저장소는 `spring.jpa.hibernate.ddl-auto=update`를 사용하므로, 이 선행 작업 없이 바로 기동하면 schema update 시점에 실패하거나 기존 회원 조회가 깨질 수 있다.
- 운영 마이그레이션 필수 절차:

```sql
ALTER TABLE members
    ADD COLUMN status VARCHAR(20) NULL AFTER is_admin;

UPDATE members
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE members
    MODIFY COLUMN status VARCHAR(20) NOT NULL;
```

- 위 SQL은 Phase 10 코드가 올라간 애플리케이션을 처음 실행하기 전에 수행한다.
- 이미 `status` 컬럼이 추가된 환경이라면 `UPDATE ... WHERE status IS NULL`과 `MODIFY COLUMN ... NOT NULL`만 적용한다.
- 상세 절차는 [`deployment-guide.md`](./deployment-guide.md)의 Phase 10 마이그레이션 항목을 함께 참조한다.

---

## 3. 도메인별 처리 규칙

| 도메인 | 처리 방식 |
|------|-----------|
| Member | row 보존, status/withdrawnAt 기록, 개인정보 스크럽 |
| LinkedAccount | 전량 hard delete |
| TaxiParty | 리더 탈퇴 시 active party를 `WITHDRAWED`로 종료, 일반 멤버는 `OPEN/CLOSED` 파티에서 자동 이탈, `ARRIVED` 참여 중이면 탈퇴 차단 |
| Chat | `chat_room_members` 정리, room memberCount 동기화, 과거 메시지는 보존 |
| Board | 게시글/댓글 본문 보존, 작성자 표시만 `탈퇴한 사용자`로 익명화, 좋아요/북마크 기록 삭제 및 카운트 보정 |
| Notice | 댓글 본문 보존, 작성자 표시 익명화, 좋아요/읽음 기록 삭제 |
| Support | 문의/신고 이력은 보존, inquiry의 구조화 개인정보만 마스킹 |
| Notification | `user_notifications`, `fcm_tokens` 전량 삭제, SSE 연결 종료 |
| Academic | `user_timetables` 전량 삭제 |
| Friend | 공개 프로필 삭제, 활성 친구 코드 영구 폐기, 요청·관계·설정·차단·시간표 공유 정리, 처리 중 초대 만료 |

### 3.1 TaxiParty

- 리더 탈퇴:
  - `OPEN`, `CLOSED`, `ARRIVED`의 active party를 `ENDED + WITHDRAWED`로 종료한다.
  - 해당 파티의 `PENDING` join request는 `DECLINED`로 정리한다.
- 일반 멤버 탈퇴:
  - `OPEN`, `CLOSED` 파티에 속해 있으면 자동 탈퇴 처리한다.
  - `ARRIVED` 파티에 속해 있으면 정산 회피 방지를 위해 회원 탈퇴 자체를 거부한다.
- 요청 상태:
  - 탈퇴 회원이 요청자인 `PENDING` join request는 `CANCELED`로 정리한다.

### 3.2 Chat

- 채팅방 membership은 모두 제거한다.
- `chat_rooms.member_count`는 즉시 재계산한다.
- 이미 연결된 채팅 WebSocket 세션도 best-effort로 종료하고, 남아 있는 세션의 `/topic/chat/**` 수신은 차단한다.
- 과거 `chat_messages.sender_name`까지 일괄 수정하지는 않는다.
  - Phase 10에서는 이력 보존을 우선한다.

### 3.3 Board / Notice

- 게시글, 댓글, 공지댓글 본문은 유지한다.
- 작성자 참조는 다음 방식으로 익명화한다.
  - `authorId` 또는 `userId`는 `withdrawn-member`
  - 표시 이름은 `탈퇴한 사용자`
  - 프로필 이미지는 제거
- 기존 좋아요/북마크/읽음 상태는 탈퇴 회원 기준으로 정리한다.

### 3.4 Support

- 운영 추적과 분쟁 대응을 위해 inquiry/report record는 보존한다.
- 자동 마스킹 대상은 inquiry의 구조화 사용자 프로필 필드로 제한한다.
- 자유서술 본문 전체를 자동 치환하지는 않는다.
  - 오탐/누락 가능성이 높아 운영 정책 영역으로 남긴다.

### 3.5 Notification / 실시간 연결

- `user_notifications`, `fcm_tokens`는 hard delete 한다.
- 회원 탈퇴 완료 후 notification SSE, party SSE, join-request SSE 연결을 종료한다.

### 3.6 Friend

> Friend derived-data cleanup은 Member 탈퇴 트랜잭션 안에서 실행한다. PENDING 초대 terminal 전이는 각 TaxiParty·Chat aggregate가 계속 소유한다.

- 탈퇴 트랜잭션은 해당 Member row를 잠그고 `WITHDRAWN` 상태를 먼저 확정한 뒤 Friend cleanup을 수행한다.
- `FriendProfile`과 `publicId`, `nicknameSearchable`은 삭제한다.
- 현재 활성 친구 코드는 단일 `FriendCodeRegistry`에서 `RETIRED`로 바꾸고 소유 회원 연결을 제거한다. `normalizedCode` tombstone은 영구 보존하며 재발급·탈퇴로 폐기된 코드는 다른 회원에게 다시 할당하지 않는다. ACTIVE 코드의 `ownerMemberId`는 non-null unique이고 RETIRED row만 null을 허용한다.
- 받은·보낸 친구 요청, friendship, 양방향 즐겨찾기, 차단·피차단 관계, 시간표 공개 기본값과 친구별 override를 정리한다.
- TaxiParty와 Chat이 소유한 발송·수신 `PENDING` 초대는 각각 `EXPIRED + MEMBER_WITHDRAWN`으로 전이하고 `respondedAt`을 기록한다. 이미 terminal인 초대와 한 번 기록된 `expiryReason`은 변경하지 않는다.
- Support의 inquiry/report record는 기존 보존 정책을 유지한다.
- 탈퇴와 경쟁해 대기하던 Friend mutation·초대 처리·lazy provisioning은 같은 Member 잠금 획득 후 요청자와 대상의 `ACTIVE`·프로필 완료 상태를 다시 확인하고, 하나라도 조건을 만족하지 않으면 새 Friend row나 파생 데이터를 생성·복원하지 않는다.
- 검증에는 탈퇴와 요청·차단·즐겨찾기·공유 설정·초대·lazy provisioning의 경쟁, 폐기 코드 미재사용, 초대 만료 사유 불변성을 포함한다.

---

## 4. 인증/보안 정책

- 보호 API에 탈퇴 회원 토큰이 들어오면 `403 MEMBER_WITHDRAWN`을 반환한다.
- 예외적으로 `POST /v1/members`는 withdrawn UID가 명시적 `409`를 받을 수 있도록 인증 필터 차단 대상에서 제외한다.
- 감사 로그는 전용 audit table 대신 애플리케이션 로그에 남긴다.
- Firebase 계정 삭제 실패는 운영 경고 로그로 남기고, 서버 접근은 withdrawn 상태로 계속 차단한다.

---

## 5. Phase 10 범위와 후속 과제

### 5.1 이번 Phase에서 구현

- 회원 lifecycle 필드 도입 (`status`, `withdrawnAt`)
- `DELETE /v1/members/me`
- 도메인별 정합성 처리와 개인정보 스크럽
- OpenAPI/문서/테스트 동기화

### 5.2 후속 Phase 또는 운영 과제

- 전용 감사 로그 테이블/조회 UI
- Support 자유서술 본문의 고급 PII 탐지/마스킹
- 유예 기간/탈퇴 취소 워크플로우
- Chat 과거 메시지 전면 익명화 정책
- Friend·초대·탈퇴 경쟁 회귀 테스트 확대

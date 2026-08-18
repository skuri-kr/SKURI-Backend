# SKURI 친구 기능 기준 명세

> 문서 상태: 정책 및 구현 계획 승인 완료, 런타임 미구현
> 기준일: 2026-08-18
> 구현 게이트: 이 문서와 모바일 구현 계획을 검토한 뒤, 사용자의 별도 코드 구현 승인을 받아야 한다.
> 모바일 구현 계획: SKURI-Frontend의 docs/plans/friend-feature-implementation.md

---

## 1. 문서 목적과 기준

이 문서는 SKURI 친구 기능의 제품 정책, 도메인 경계, 권한, 상태 전이, 데이터 노출 범위와 예정 API를 고정하는 단일 기준이다.

- 친구 관계와 차단, 초대, 시간표 공개 범위의 최종 판단자는 백엔드다.
- 모바일은 이 문서의 계약을 소비하며 클라이언트 상태만으로 권한을 판단하지 않는다.
- 이 문서의 API는 아직 구현되지 않은 예정 계약이다. 현재 운영 API로 해석하지 않는다.
- 실제 구현 시 런타임 OpenAPI와 docs/api-specification.md를 같은 PR에서 동기화한다.
- 구현 중 정책 변경이 필요하면 코드를 먼저 바꾸지 않고 이 문서의 결정 기록을 갱신한 뒤 승인을 받는다.

### 1.1 용어 구분

| 용어 | 의미 |
| --- | --- |
| SKURI 친구 | 두 회원 사이에 수락을 거쳐 성립한 상호 소셜 관계 |
| 친구 요청 | 한 회원이 다른 회원에게 보내는 단방향 요청 |
| 마인크래프트 SELF 계정 | 회원 본인이 등록한 대표 마인크래프트 계정 |
| 마인크래프트 FRIEND 계정 | SELF 계정 아래에 회원이 보조로 등록한 계정 역할 |
| 친구 초대 | 이미 친구인 회원을 택시파티 또는 공개 채팅방으로 초대하는 행위 |

마인크래프트의 FRIEND 역할은 SKURI 친구 관계와 무관하다.

---

## 2. 승인된 V1 범위

### 2.1 친구 관계

- 한 회원이 친구 요청을 보내고 상대가 수락하면 상호 친구가 된다.
- 친구 추가 수단은 친구 코드, QR, 닉네임 검색이다.
- 딥링크 URL을 통한 친구 추가는 V1에서 구현하지 않는다.
- 친구 요청은 생성 후 30일이 지나면 만료된다.
- 거절 후 재요청 대기 시간과 발송량 제한은 두지 않는다.
- 동일 상대에게 처리 중인 PENDING 요청은 중복 생성하지 않는다.
- 자기 자신, 기존 친구, 차단 관계에는 요청할 수 없다.
- 양쪽이 동시에 요청한 경우 새 요청을 중복 생성하지 않고 친구 관계 성립으로 처리한다.
- 요청자는 PENDING 요청을 취소할 수 있다.

### 2.2 친구 코드와 QR

- 친구 코드는 회원 ID나 Firebase UID를 노출하지 않는 무작위 코드다.
- 표시 형식은 SKR-7K4M-9Q2D 형태로 하며 대소문자를 구분하지 않는다.
- 0/O, 1/I/L처럼 혼동하기 쉬운 문자는 생성 문자 집합에서 제외한다.
- 코드는 회원당 활성 코드 한 개만 유지한다.
- 재발급하면 이전 코드는 즉시 무효화되며 기존 친구 관계에는 영향을 주지 않는다.
- QR payload는 skuri-friend:v1:{friendCode} 형식의 버전 계약을 사용한다.
- QR 인식 후 바로 요청하지 않고 대상 공개 프로필 확인과 명시적 발송 단계를 거친다.
- URL 딥링크는 QR payload에 포함하지 않는다.

### 2.3 닉네임 검색

- 인증된 ACTIVE 회원만 검색할 수 있다.
- 검색 허용 설정을 켠 회원만 결과에 포함한다.
- 검색 허용 기본값은 false다.
- 닉네임 2글자 이상 부분 일치로 검색하며 최대 20건을 반환한다.
- 닉네임은 고유하지 않으므로 결과에는 프로필 사진과 학과를 함께 제공한다.
- 이메일, 실명, 학번, Firebase UID는 검색 결과에 노출하지 않는다.
- 차단 관계는 양방향 모두 검색 결과에서 제외한다.

### 2.4 즐겨찾기와 정렬

- 즐겨찾기는 사용자 방향별 비공개 설정이며 상대에게 알리지 않는다.
- 친구 목록, 시간표 친구 목록, 초대 친구 선택 목록은 같은 정렬을 사용한다.
- 정렬 우선순위는 즐겨찾기 내림차순, 한글 닉네임 가나다순, 회원 ID 오름차순이다.
- 닉네임이 같아도 안정적인 순서를 유지한다.

### 2.5 시간표 공유

시간표 공개 범위는 다음 세 단계다.

| 범위 | 친구에게 제공하는 데이터 |
| --- | --- |
| PRIVATE | 시간표 데이터 미제공 |
| BUSY_ONLY | 요일, 시작 교시, 종료 교시만 제공 |
| DETAILS | 과목명, 요일, 교시, 교수명, 강의실, 학점 등 상세 제공 |

- 기본 공개 범위는 PRIVATE다.
- 회원은 전역 기본값을 변경할 수 있다.
- 친구별 예외 설정이 전역 기본값보다 우선한다.
- 친구 관계가 종료되면 해당 친구의 예외 설정을 제거한다.
- 친구 시간표 조회는 요청 학기 기준이며, 자신의 현재 선택 학기와 함께 사용한다.
- PRIVATE 친구도 친구 목록에는 표시하지만 시간표 데이터는 반환하지 않는다.
- 친구가 보는 시간표는 읽기 전용이다.
- 친구 시간표 응답에는 외부 공유 기능을 제공하지 않는다.
- 원래 시간표 소유자만 자신의 시간표를 외부 공유할 수 있다.

공통 공강 계산 규칙:

- 월요일부터 금요일, 1교시부터 12교시까지 계산한다.
- 양쪽 시간표에서 시간이 지정된 정규 과목과 직접 입력 과목을 점유 시간으로 본다.
- 시간이 없는 온라인 수업은 계산에서 제외한다.
- BUSY_ONLY 이상이면 계산할 수 있다.
- 모바일이 허용된 busy slot만으로 계산하며 숨겨진 과목 상세를 추론하거나 표시하지 않는다.

같이 듣는 수업 규칙:

- DETAILS 범위에서만 제공한다.
- 같은 학기의 공식 강의 courseId가 같은 경우만 같은 수업으로 판단한다.
- 직접 입력 수업은 이름이 같아도 오탐 방지를 위해 같은 수업으로 판정하지 않는다.

### 2.6 택시파티 친구 초대

- 리더뿐 아니라 현재 파티 참가자 모두 자신의 SKURI 친구를 초대할 수 있다.
- OPEN 상태 파티에서만 초대를 보낼 수 있다.
- 초대자는 발송 시점에 파티 참가자여야 한다.
- 수신자는 초대자의 현재 친구여야 한다.
- 수신자의 명시적 수락 후 파티에 참여한다.
- 리더의 추가 승인은 요구하지 않는다.
- 초대는 좌석을 예약하지 않는다.
- 남은 자리보다 많은 대기 초대를 발송할 수 있다.
- 수락 시점에 파티 상태, 정원, 기존 참여, 다른 활성 파티 참여 여부, 친구·차단 관계를 다시 검증한다.
- 정원이 먼저 찼다면 뒤늦은 수락은 실패하며 파티 정원 마감 안내를 반환한다.
- 파티가 OPEN이 아니게 되면 처리되지 않은 초대는 더 이상 수락할 수 없다.

### 2.7 공개 채팅방 친구 초대

- UNIVERSITY, DEPARTMENT, GAME, 공개 CUSTOM 채팅방만 지원한다.
- PARTY 채팅방은 택시파티 초대 흐름을 사용한다.
- 1:1 채팅과 비공개 채팅방은 V1에서 구현하지 않는다.
- 초대자는 발송 및 수락 시점에 해당 공개방의 참여자여야 한다.
- 수신자는 초대자의 현재 친구여야 한다.
- 학과방 등 기존 채팅방 입장 자격을 발송 및 수락 시점에 모두 검증한다.
- 공개방 초대는 생성 후 7일이 지나면 만료된다.
- 방 삭제, 비공개 전환, 기존 참여, 친구 해제, 차단, 초대자 탈퇴 시 더 이상 수락할 수 없다.

### 2.8 마인크래프트 계정 노출

- 친구가 등록한 SELF 계정과 그 SELF 아래의 모든 FRIEND 계정을 제공한다.
- 친구 목록에서는 대표 SELF 게임명과 전체 계정 수를 요약 제공한다.
- 친구 상세에서는 SELF를 부모, FRIEND를 자식으로 계층 표시한다.
- 친구 관계가 성립하면 별도의 마인크래프트 공개 설정 없이 볼 수 있다.
- 제공 필드는 계정용 불투명 ID, 계정 역할, 에디션, 게임명, 아바타 UUID, 부모 계정용 불투명 ID다.
- normalizedKey, linkedAt, lastSeenAt, 온라인 상태, 내부 ownerMemberId는 제공하지 않는다.
- FRIEND 계정 소유권 이전은 V1에서 구현하지 않는다.

### 2.9 친구 끊기와 차단

친구 끊기:

- 한쪽이 확인 후 실행하면 상호 친구 관계를 즉시 제거한다.
- 친구 끊기 알림은 발송하지 않는다.
- 양방향 즐겨찾기, 시간표 친구별 예외, 처리 중인 친구 기반 초대를 정리한다.
- 공개 게시물과 공개 채팅 메시지는 숨기거나 삭제하지 않는다.

차단:

- 차단은 차단자 기준 단방향 관계다.
- 차단 시 기존 친구 관계, 양방향 PENDING 친구 요청, 친구 기반 택시·공개방 초대를 정리한다.
- 차단 관계에서는 친구 검색, 요청, 친구 상세, 친구 시간표, 마인크래프트 계정, 택시·채팅 초대를 제공하지 않는다.
- 차단 해제 후 친구 관계와 공유 설정은 자동 복원하지 않는다.
- V1에서는 공개 게시판과 공개 채팅의 기존 콘텐츠를 전역 필터링하지 않는다.

### 2.10 알림

신규 알림 타입:

- FRIEND_REQUEST
- FRIEND_ACCEPTED
- PARTY_INVITATION
- CHAT_ROOM_INVITATION

알림 정책:

- 회원 알림 설정에 친구 및 초대 알림 항목을 추가한다.
- 친구 요청은 수신자에게, 친구 수락은 원 요청자에게 알린다.
- 택시파티와 공개방 초대는 수신자에게 알린다.
- 거절, 취소, 친구 끊기, 차단은 푸시 알림을 발송하지 않는다.
- 푸시를 끈 경우에도 서버 인박스의 요청·초대 원본 상태는 유지한다.
- FRIEND_REQUEST는 친구 허브 요청 탭, FRIEND_ACCEPTED는 수락한 친구 상세, PARTY_INVITATION과 CHAT_ROOM_INVITATION은 친구 허브 초대 탭으로 이동한다.

---

## 3. V1에 포함하는 제품 기능

- 친구 즐겨찾기와 상단 고정 정렬
- 친구 코드 표시·복사·재발급
- QR 표시와 인앱 스캔
- 닉네임 검색 허용 설정과 검색
- 받은 요청, 보낸 요청, 택시·채팅 초대 통합 배지
- 시간표 전역 공개 범위와 친구별 예외
- 친구 한 명과의 공통 공강
- 공식 courseId 기준 같이 듣는 수업
- 택시파티·공개방 초대 가능 친구 필터
- 친구 상세의 전체 마인크래프트 계정 계층
- 친구 끊기와 소셜 차단

---

## 4. V1 제외 및 TODO

다음 항목은 문서에 남기되 이번 구현 범위에 포함하지 않는다.

- URL 딥링크 친구 추가
- 1:1 친구 채팅
- 비공개 친구 그룹 채팅
- 여러 친구의 공통 공강
- 공강 기반 밥·스터디 빠른 제안
- 마인크래프트 FRIEND 계정 소유권 이전
- 친구 그룹과 카테고리
- 같은 수업·학과 기반 친구 추천
- 연락처 기반 추천
- 상호 친구 이름과 목록
- 온라인 상태와 최근 활동 시각
- 자동 친구 추천과 추천 알림
- 공개 게시판·공개 채팅에서 차단 상대 콘텐츠 전역 숨김
- 관리자 친구 관계망 조회와 강제 친구 관리

---

## 5. 도메인 경계

Friend 기능을 하나의 거대한 서비스에 모으지 않고 각 도메인의 최종 책임을 유지한다.

| 도메인 | 책임 |
| --- | --- |
| friend 신규 도메인 | 친구 코드, 검색 허용, 요청, 상호 관계, 즐겨찾기, 친구 끊기, 차단 |
| academic | 시간표 공개 기본값·친구별 예외, 권한에 맞춘 친구 시간표 projection |
| taxiparty | 초대 생성, OPEN·정원·참여 상태 검증, 수락 동시성 |
| chat | 공개방 초대 생성, 방 유형·공개 여부·입장 자격 검증 |
| minecraft | 친구에게 제공 가능한 계정 projection |
| notification | 도메인 이벤트를 인박스·FCM·SSE로 전달 |
| member | ACTIVE 회원 확인, 공개 프로필, 탈퇴 정리 orchestration |

Friend 도메인은 다른 도메인의 내부 엔티티를 직접 수정하지 않는다. 초대 수락과 시간표 조회는 해당 도메인의 Service가 최종 권한과 상태를 확인한다.

---

## 6. 예정 데이터 모델

이 절은 구현 전 논리 모델이며 실제 컬럼명과 마이그레이션은 구현 PR에서 ERD와 함께 확정한다.

### 6.1 friend_profiles

| 필드 | 설명 |
| --- | --- |
| member_id | 회원 내부 ID, PK |
| public_id | 친구 기능 전용 무작위 공개 ID, unique |
| friend_code | 정규화된 활성 코드, unique |
| nickname_searchable | 닉네임 검색 허용, 기본 false |
| created_at | 생성 시각 |
| rotated_at | 재발급 시각 |

public_id와 friend_code는 members.id 또는 Firebase UID에서 파생하지 않는다. 모바일과 외부 API는 members.id를 친구 식별자로 노출하지 않는다.

### 6.2 friend_requests

| 필드 | 설명 |
| --- | --- |
| id | 요청 ID |
| requester_id | 요청자 |
| recipient_id | 수신자 |
| status | PENDING, ACCEPTED, DECLINED, CANCELED, EXPIRED |
| expires_at | created_at + 30일 |
| responded_at | 수락·거절·취소·만료 처리 시각 |
| active_pair_key | PENDING 중복 방지를 위한 정규화 키 |

### 6.3 friendships

| 필드 | 설명 |
| --- | --- |
| id | 관계 ID |
| member_low_id | 정렬된 작은 회원 ID |
| member_high_id | 정렬된 큰 회원 ID |
| created_at | 친구 성립 시각 |

member_low_id + member_high_id는 unique다.

### 6.4 friend_preferences

| 필드 | 설명 |
| --- | --- |
| owner_member_id | 설정 소유자 |
| friend_member_id | 친구 |
| favorite | 즐겨찾기 여부 |

두 ID 조합은 unique이며 상대 방향의 설정과 독립적이다.

### 6.5 member_blocks

| 필드 | 설명 |
| --- | --- |
| blocker_id | 차단자 |
| blocked_id | 차단 대상 |
| created_at | 차단 시각 |

두 ID 조합은 unique다.

### 6.6 timetable_share_settings

| 필드 | 설명 |
| --- | --- |
| owner_member_id | 시간표 소유자, PK |
| default_scope | PRIVATE, BUSY_ONLY, DETAILS |

### 6.7 timetable_share_overrides

| 필드 | 설명 |
| --- | --- |
| owner_member_id | 시간표 소유자 |
| friend_member_id | 적용 친구 |
| scope | PRIVATE, BUSY_ONLY, DETAILS |

두 ID 조합은 unique이며 친구 관계 종료 시 삭제한다.

### 6.8 party_invitations

| 필드 | 설명 |
| --- | --- |
| id | 초대 ID |
| party_id | 대상 파티 |
| inviter_id | 초대한 파티 참가자 |
| invitee_id | 초대받은 친구 |
| status | PENDING, ACCEPTED, DECLINED, CANCELED, EXPIRED |
| responded_at | 처리 시각 |

### 6.9 chat_room_invitations

| 필드 | 설명 |
| --- | --- |
| id | 초대 ID |
| chat_room_id | 공개 채팅방 |
| inviter_id | 초대한 방 참가자 |
| invitee_id | 초대받은 친구 |
| status | PENDING, ACCEPTED, DECLINED, CANCELED, EXPIRED |
| expires_at | created_at + 7일 |
| responded_at | 처리 시각 |

동일 파티·방과 같은 수신자에 대한 PENDING 초대는 중복 생성하지 않는다.

---

## 7. 상태 전이

### 7.1 친구 요청

~~~text
PENDING ── 수락 ──> ACCEPTED + friendship 생성
   ├────── 거절 ──> DECLINED
   ├────── 요청자 취소 ──> CANCELED
   ├────── 30일 경과 ──> EXPIRED
   └────── 상대의 역방향 요청 ──> ACCEPTED + friendship 생성
~~~

Terminal 상태에서 다시 상태를 변경하지 않는다.

### 7.2 친구 관계

~~~text
친구 요청 수락 ──> ACTIVE friendship
ACTIVE ── 친구 끊기 ──> 삭제
ACTIVE ── 어느 한쪽 차단 ──> 삭제
회원 탈퇴 ──> 관련 관계 전체 삭제
~~~

### 7.3 택시파티 초대

~~~text
PENDING ── 수락 성공 ──> ACCEPTED + 파티 참여
   ├────── 거절 ──> DECLINED
   ├────── 발송자 취소 ──> CANCELED
   └────── 파티 비OPEN·정원 마감·관계 상실 ──> EXPIRED 또는 수락 실패
~~~

정원 확인과 파티 참여는 같은 트랜잭션과 잠금 경계에서 처리한다.

### 7.4 공개방 초대

~~~text
PENDING ── 수락 성공 ──> ACCEPTED + 공개방 참여
   ├────── 거절 ──> DECLINED
   ├────── 발송자 취소 ──> CANCELED
   └────── 7일 경과·자격 상실·방 상태 변경 ──> EXPIRED
~~~

---

## 8. 권한 및 데이터 노출 행렬

| 기능 | 요구 조건 | 서버 최종 검증 |
| --- | --- | --- |
| 닉네임 검색 | 인증 ACTIVE 회원 | 검색 허용, 차단 아님 |
| 친구 요청 | 인증 ACTIVE 회원 | self·기존 친구·중복 PENDING·차단 아님 |
| 친구 상세 | 상호 친구 | ACTIVE 관계와 차단 없음 |
| 친구 시간표 | 상호 친구 | 최종 공유 범위와 차단 |
| 친구 Minecraft | 상호 친구 | 허용 필드 projection과 차단 |
| 택시 초대 | OPEN 파티 참가자 | 친구, 대상 참여 가능, 중복 초대 아님 |
| 택시 수락 | 초대 수신자 | 파티 lock 후 OPEN·정원·활성 파티 재검증 |
| 공개방 초대 | 공개방 참가자 | 공개 non-PARTY, 친구, 입장 자격 |
| 공개방 수락 | 초대 수신자 | 만료·방 상태·입장 자격·초대자 참여 재검증 |
| 즐겨찾기 | 상호 친구 | 설정 소유자 방향만 변경 |
| 친구 끊기 | 상호 친구 중 한 명 | 양방향 파생 데이터 정리 |
| 차단 | 인증 ACTIVE 회원 | self 차단 금지, 멱등 처리 |

---

## 9. 예정 API 계약

아래 경로는 구현 설계를 위한 기준이며 아직 운영 API가 아니다. 구현 PR에서 Controller, DTO, OpenAPI examples, Contract 테스트와 함께 최종 고정한다.

### 9.1 친구 핵심

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/friends | 친구 목록, 즐겨찾기·가나다순 |
| GET | /v1/friends/{friendPublicId} | 친구 상세 |
| DELETE | /v1/friends/{friendPublicId} | 친구 끊기 |
| PATCH | /v1/friends/{friendPublicId}/favorite | 즐겨찾기 변경 |
| GET | /v1/friends/search | nickname 검색 |
| GET | /v1/friends/me/code | 내 친구 코드 |
| POST | /v1/friends/me/code/regenerate | 친구 코드 재발급 |
| PATCH | /v1/friends/me/privacy | 닉네임 검색 허용 설정 |
| GET | /v1/friends/inbox-counts | 받은 요청·초대 badge count |

친구 목록 최소 응답:

- friendPublicId
- nickname
- department
- photoUrl
- favorite
- effectiveTimetableScope
- primaryMinecraftGameName
- minecraftAccountCount

### 9.2 친구 요청

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/friend-requests | direction 기준 받은·보낸 요청 |
| POST | /v1/friend-requests | targetPublicId 또는 friendCode로 요청 |
| POST | /v1/friend-requests/{requestId}/accept | 수락 |
| POST | /v1/friend-requests/{requestId}/decline | 거절 |
| DELETE | /v1/friend-requests/{requestId} | 요청자 취소 |

POST 요청은 targetPublicId와 friendCode 중 정확히 하나만 허용한다. 닉네임 검색 결과와 코드 확인 결과는 내부 members.id 대신 targetPublicId를 제공한다.

### 9.3 차단

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/friends/blocks | 내 차단 목록 |
| POST | /v1/friends/blocks | 회원 차단 |
| DELETE | /v1/friends/blocks/{memberPublicId} | 차단 해제 |

### 9.4 시간표 공유

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/timetables/my/sharing-settings | 내 기본값·친구별 예외 |
| PATCH | /v1/timetables/my/sharing-settings | 기본 공개 범위 변경 |
| PUT | /v1/timetables/my/sharing-overrides/{friendPublicId} | 친구별 예외 저장 |
| DELETE | /v1/timetables/my/sharing-overrides/{friendPublicId} | 친구별 예외 제거 |
| GET | /v1/timetables/friends/{friendPublicId} | 친구 시간표 조회 |

친구 시간표 응답은 effectiveScope를 항상 포함한다.

- PRIVATE: 시간표 필드는 비우고 공개되지 않았음을 표현한다.
- BUSY_ONLY: slots만 제공하고 course 식별·이름 필드는 제공하지 않는다.
- DETAILS: 허용된 course와 slot 상세를 제공한다.

### 9.5 마인크래프트

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/friends/{friendPublicId}/minecraft-accounts | 친구가 등록한 전체 계정의 안전 projection |

응답은 SELF와 FRIEND의 계층을 만들 수 있는 최소 필드만 제공한다.

### 9.6 택시파티 초대

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/parties/{partyId}/invitations/eligible-friends | 초대 가능한 친구 |
| POST | /v1/parties/{partyId}/invitations | 한 명 이상 초대 |
| GET | /v1/party-invitations/received | 내가 받은 초대 |
| POST | /v1/party-invitations/{invitationId}/accept | 초대 수락 |
| POST | /v1/party-invitations/{invitationId}/decline | 초대 거절 |
| DELETE | /v1/party-invitations/{invitationId} | 발송자 취소 |

eligible 응답은 이미 참여, 다른 활성 파티, 차단, 중복 PENDING 등 초대 불가 대상을 제외한다.

### 9.7 공개 채팅방 초대

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/chat-rooms/{chatRoomId}/invitations/eligible-friends | 초대 가능한 친구 |
| POST | /v1/chat-rooms/{chatRoomId}/invitations | 한 명 이상 초대 |
| GET | /v1/chat-room-invitations/received | 내가 받은 초대 |
| POST | /v1/chat-room-invitations/{invitationId}/accept | 초대 수락 |
| POST | /v1/chat-room-invitations/{invitationId}/decline | 초대 거절 |
| DELETE | /v1/chat-room-invitations/{invitationId} | 발송자 취소 |

---

## 10. 동시성, 멱등성, 보안

- 친구 요청 수락은 요청 row와 정규화된 friendship pair를 잠그거나 unique constraint 충돌을 멱등 성공으로 해석한다.
- 양방향 동시 요청은 friendship 한 건만 만든다.
- 같은 요청·초대의 accept 재호출은 이미 성공한 동일 수신자라면 멱등 응답을 우선한다.
- 택시 초대 수락은 기존 TaxiParty 참여 로직과 같은 잠금 경계를 사용한다.
- 공개방 초대 수락은 기존 joinChatRoom 자격 검증을 재사용한다.
- 발송량 제한은 두지 않지만 입력 검증, PENDING 중복 방지와 payload 크기 제한은 적용한다.
- 친구 코드와 QR payload를 로그에 원문으로 남기지 않는다.
- 검색 결과와 친구 projection에 내부 members.id, 이메일, 실명, 학번, Firebase UID를 포함하지 않는다.
- 목록 API에서 N+1 조회가 생기지 않도록 projection 또는 batch 조회를 사용한다.

---

## 11. 회원 탈퇴와 정리

회원 탈퇴 시 다음 데이터를 정리한다.

- 친구 공개 ID, 친구 코드와 검색 허용 설정
- 받은·보낸 친구 요청
- 모든 친구 관계와 양방향 즐겨찾기
- 차단·피차단 관계
- 시간표 공개 기본값과 친구별 예외
- 발송·수신한 처리 중 택시파티·공개방 초대

기존 도메인의 공개 게시물, 채팅 tombstone, 파티 이력은 각 도메인의 기존 탈퇴 정책을 유지한다.

---

## 12. 관리자·운영 범위

- V1 관리자 페이지에는 친구 관계망 조회, 친구 강제 생성·삭제, 시간표 공개 설정 조회를 추가하지 않는다.
- 친구 요청 발송량 제한도 운영 정책으로 추가하지 않는다.
- 기존 MEMBER 신고를 통해 괴롭힘을 신고할 수 있다.
- 운영 로그와 지표에는 요청·수락·거절·차단·초대 성공/실패 횟수를 개인정보 없는 집계 형태로 남길 수 있다.
- 친구 코드, 시간표 상세, 마인크래프트 내부 식별 키는 운영 로그에 기록하지 않는다.

---

## 13. 구현 및 배포 순서

1. 기준 문서 검토·병합
2. Friend 핵심 데이터 모델, 요청, 관계, 즐겨찾기, 차단
3. 모바일 친구 허브와 코드·닉네임·QR 흐름
4. Academic 시간표 공유와 모바일 아코디언
5. Minecraft 안전 projection과 친구 상세 계층
6. TaxiParty 초대와 모바일 택시 채팅 진입점
7. 공개 Chat 초대와 모바일 공개방 진입점
8. 알림·배지·딥링크 목적지 통합 검증

백엔드는 기존 앱과 호환되는 additive API로 먼저 배포한다. 모바일 노출은 필요한 백엔드 API 배포 확인 후 진행한다.

각 런타임 PR은 다음을 함께 갖춰야 한다.

- Service 권한·상태 전이 테스트
- Controller Contract 테스트
- OpenAPI 성공·오류 examples
- docs/api-specification.md 동기화
- 엔티티 변경 시 docs/erd.md
- 도메인 경계 변경 시 docs/domain-analysis.md와 docs/role-definition.md
- 관련 Serena Memory 동기화

---

## 14. 검증 기준

### 14.1 백엔드 자동 검증

- 친구 요청 정상, self, 차단, 중복 PENDING, 양방향 동시 요청
- 30일 만료와 만료 요청 수락 차단
- 즐겨찾기 방향 독립성과 정렬
- PRIVATE, BUSY_ONLY, DETAILS projection 필드 미노출 검증
- 친구 해제·차단 후 시간표와 Minecraft 접근 차단
- OPEN이 아닌 파티 초대 차단
- 택시 마지막 좌석 동시 수락에서 한 명만 성공
- 공개 non-PARTY 방만 초대 가능
- 학과방 입장 자격과 7일 만료
- 알림 설정 off 시 FCM 미발송, 인박스 원본 유지
- 탈퇴 cleanup

### 14.2 통합·실기기 검증

- 세 계정 이상으로 요청, 수락, 거절, 재요청, 차단
- 두 실제 기기에서 FCM, 인박스, cold/warm start 이동
- QR 카메라 권한 허용·거절·설정 복귀
- 서로 다른 공개 범위의 시간표와 공통 공강
- 택시 마지막 좌석 경쟁 수락
- 공개방 초대 만료와 입장 자격 변경

실기기 검증을 수행하지 않았다면 자동 테스트 통과와 구분해 보고한다.

---

## 15. 구현 중지선

이 문서 PR은 설계 기준만 고정한다.

- Entity, migration, Controller, Service, DTO, 테스트 코드를 만들지 않는다.
- 모바일 화면, navigation, API client, 네이티브 QR 의존성을 만들거나 수정하지 않는다.
- 실제 코드 구현은 사용자의 별도 명시적 승인 후 시작한다.

---

## 16. 문서 검토 결과

검토일: 2026-08-18

- [x] 승인된 V1과 TODO가 구분되어 있다.
- [x] 친구 요청 30일 만료, 거절 cooldown·발송량 제한 없음이 반영되어 있다.
- [x] 시간표 PRIVATE, BUSY_ONLY, DETAILS와 친구별 예외·재공유 금지가 반영되어 있다.
- [x] TaxiParty 참가자 전원 초대와 수락 시 정원 동시성 검증이 반영되어 있다.
- [x] 공개 non-PARTY 채팅방만 초대하며 1:1·비공개 채팅은 제외되어 있다.
- [x] Minecraft SELF와 모든 FRIEND 계정, 최근 접속·온라인 상태 미노출이 반영되어 있다.
- [x] 차단이 소셜 기능에만 적용되고 공개 콘텐츠 전역 필터가 제외되어 있다.
- [x] 기존 Firebase UID 결합 회원 ID를 새 친구 API에 노출하지 않도록 friendPublicId 계약을 추가했다.
- [x] 예정 API가 현재 운영 API와 구분되어 있다.
- [x] 실제 코드 구현 승인 gate가 명시되어 있다.

현재 런타임의 docs/api-specification.md, docs/domain-analysis.md, docs/erd.md, docs/role-definition.md에는 미구현 친구 엔티티와 API를 현재형으로 추가하지 않았다. 각 런타임 PR에서 실제 구현과 함께 동기화한다.

---

## 17. 결정 기록

| 날짜 | 결정 |
| --- | --- |
| 2026-08-18 | 친구 코드, QR, 닉네임 검색을 V1에 포함하고 URL 딥링크는 TODO로 보류 |
| 2026-08-18 | 닉네임 검색은 opt-in, 2글자 이상 부분 일치, 최대 20건으로 확정 |
| 2026-08-18 | 친구 요청 30일 만료, 거절 cooldown과 발송량 제한 없음 |
| 2026-08-18 | 시간표 기본 PRIVATE, BUSY_ONLY·DETAILS와 친구별 예외, 재공유 금지 확정 |
| 2026-08-18 | 공통 공강은 월~금 1~12교시, 같이 듣는 수업은 공식 courseId 기준 |
| 2026-08-18 | 택시 참가자 전원이 친구를 초대하며, 좌석은 예약하지 않고 수락 시 재검증 |
| 2026-08-18 | 공개 채팅방 초대만 지원하고 초대 만료는 7일 |
| 2026-08-18 | 친구의 Minecraft SELF와 모든 FRIEND 계정을 제공하되 최근 접속·온라인 상태는 숨김 |
| 2026-08-18 | 차단은 소셜 기능에 적용하고 공개 콘텐츠 전역 숨김은 제외 |
| 2026-08-18 | V1 관리자 친구 관계망 운영 UI는 추가하지 않음 |

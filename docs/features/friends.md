# SKURI 친구 기능 기준 명세

> 문서 상태: Foundation·관계 Core, Core 출시 준비, 친구 화면 완성, 시간표 공유와 친구 초대 Backend [#85](https://github.com/skuri-kr/SKURI-Backend/pull/85)·Frontend [#27](https://github.com/skuri-kr/SKURI-Frontend/pull/27) 전달 완료. 초대·정원·파티원 UX 보완 진행 중이며 알림과 PENDING 초대 정리를 제외한 회원 탈퇴 cleanup은 후속 단계다.
> 기준일: 2026-08-24
> 다음 구현 단위: 초대·정원·파티원 UX 보완을 전달한 뒤 알림·PENDING 초대 외 회원 탈퇴 cleanup을 구현한다.
> 모바일 구현 계획: SKURI-Frontend의 docs/plans/friend-feature-implementation.md

---

## 1. 문서 목적과 기준

이 문서는 SKURI 친구 기능의 제품 정책, 도메인 경계, 권한, 상태 전이, 데이터 노출 범위와 예정 API를 고정하는 단일 기준이다.

- 친구 관계와 차단, 초대, 시간표 공개 범위의 최종 판단자는 백엔드다.
- 모바일은 이 문서의 계약을 소비하며 클라이언트 상태만으로 권한을 판단하지 않는다.
- `GET /v1/friends/me/code`, `POST /v1/friends/me/code/regenerate`, `POST /v1/friend-codes/preview`, `GET/PATCH /v1/friends/me/privacy`, 9.1~9.7의 관계 Core·시간표 공유·Minecraft projection·친구 초대는 런타임 API다. 9.8 이후의 알림 확장은 예정 계약이며 현재 운영 API로 해석하지 않는다.
- 실제 구현 시 런타임 OpenAPI와 docs/api-specification.md를 같은 PR에서 동기화한다.
- 구현 중 정책 변경이 필요하면 코드를 먼저 바꾸지 않고 이 문서의 결정 기록을 갱신한 뒤 승인을 받는다.

### 1.1 단계 종료 문서 정합성 게이트

각 구현 단계의 코드·테스트가 끝난 뒤 해당 단계의 최종 PR을 마무리하기 전에, 구현과 문서의 정합성을 반드시 점검한다.

- 백엔드 런타임 source of truth, 이 문서, 모바일 구현 계획, OpenAPI, ERD, 배포·회원 탈퇴 문서를 상호 대조한다.
- API 변경이 있으면 Controller·DTO·OpenAPI와 모바일 DTO·mapper·화면 소비 계약을 함께 대조한다.
- 정책 충돌, 완료·예정 범위 오표기, 구현과 문서의 drift를 해소하고 필요한 문서 갱신을 최종 PR에 포함한다.
- 최종 PR에는 대조한 범위와 자동·수동 검증 결과를 기록한다. 아직 하지 못한 운영·실기기 검증은 완료로 표시하지 않는다.

### 1.2 완료된 PR 이력

친구 기능의 현재 기준선은 아래 병합 PR로 구성된다.

| 저장소 | PR | 완료 범위 |
| --- | --- | --- |
| Backend | [#78](https://github.com/skuri-kr/SKURI-Backend/pull/78) | 친구 기능 기준 명세, 도메인 경계, 상태·권한·탈퇴 계획 문서화 |
| Backend | [#79](https://github.com/skuri-kr/SKURI-Backend/pull/79) | FriendProfile·영구 코드 registry, 코드 조회·재발급·preview, 검색 공개 설정, provisioning·backfill |
| Backend | [#80](https://github.com/skuri-kr/SKURI-Backend/pull/80) | 친구 요청·관계·즐겨찾기·친구 끊기·차단·닉네임 검색·PENDING 목록·badge API |
| Backend | [#81](https://github.com/skuri-kr/SKURI-Backend/pull/81) | 프로필 완료 eligibility, ACTIVE 닉네임 정책, Friend 데이터 lifecycle, 관계 상태와 출시 전 데이터 정리 절차 |
| Backend | [#82](https://github.com/skuri-kr/SKURI-Backend/pull/82) | 친구 출시 운영 postcheck CTE 검증 보정 |
| Backend | [#83](https://github.com/skuri-kr/SKURI-Backend/pull/83) | 친구 목록·수락 응답 Minecraft 요약과 SELF·FRIEND 안전 projection |
| Backend | [#84](https://github.com/skuri-kr/SKURI-Backend/pull/84) | 시간표 공개 범위·친구별 예외·친구 시간표 projection과 관계 종료 cleanup |
| Frontend | [#22](https://github.com/skuri-kr/SKURI-Frontend/pull/22) | 모바일 친구 기능 정보 구조·화면·상태·검증 계획 문서화 |
| Frontend | [#23](https://github.com/skuri-kr/SKURI-Frontend/pull/23) | FriendHub·FriendAdd·FriendDetail·FriendSettings와 관계 Core 연동 |
| Frontend | [#24](https://github.com/skuri-kr/SKURI-Frontend/pull/24) | Core 출시 준비 UX, 회원가입·프로필 닉네임 정책과 관계 Core 수동 QA 보완 |
| Frontend | [#25](https://github.com/skuri-kr/SKURI-Frontend/pull/25) | 친구 QR 생성·스캔과 친구 Minecraft SELF·FRIEND 계정 표시 |
| Frontend | [#26](https://github.com/skuri-kr/SKURI-Frontend/pull/26) | 시간표 공유 설정과 친구 시간표 accordion·공통 공강·같이 듣는 수업 |

PR #23 수동 QA에서 발견한 가입 완료 판정, 닉네임 정책, 검색·요청 상태 문제는 #81·#24에서 보완했다. 이는 기존 완료 범위를 되돌린 것이 아니라 실제 배포 전에 회원과 Friend 데이터의 생성 자격을 바로잡은 출시 준비 작업이다.

### 1.2 용어 구분

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
- 재발급·탈퇴로 RETIRED가 된 코드는 영구 예약하고 다른 회원에게 다시 할당하지 않는다. 과거에 복사한 코드와 QR은 이후에도 계속 일반적인 대상 없음으로만 해석되어야 한다.
- QR payload는 skuri-friend:v1:{friendCode} 형식의 버전 계약을 사용한다.
- QR 인식 후 바로 요청하지 않고 대상 공개 프로필 확인과 명시적 발송 단계를 거친다.
- 코드 입력과 QR 해석 결과는 부작용 없는 친구 코드 preview API로 확인한다.
- preview 응답은 friendPublicId, 닉네임, 프로필 사진, 학과와 현재 요청 가능 상태만 제공하며 친구 요청을 생성하지 않는다.
- 사용자가 확인 화면에서 발송을 누른 뒤 preview 응답의 friendPublicId로 별도 친구 요청을 생성한다.
- 요청자와 대상 사이 어느 방향으로든 차단이 있으면 preview를 거부하고, 잘못되거나 폐기된 코드와 같은 일반적인 대상 없음 응답을 사용해 차단 여부를 노출하지 않는다.
- URL 딥링크는 QR payload에 포함하지 않는다.
- FriendProfile과 최초 ACTIVE 코드는 프로필을 완료한 ACTIVE 회원에게만 발급한다. 최초 소셜 로그인으로 Member row만 만들어지고 학번·학과·유효 닉네임이 없는 회원에게는 발급하지 않는다. 프로필 미완료 회원은 현재 예약어 닉네임을 변경하지 않은 채 학번·학과만 부분 수정해 프로필을 완료할 수 없다.
- 친구 API의 lazy provisioning과 기동 backfill도 같은 가입 완료 판정을 사용하며, 미완료 회원을 Friend 데이터로 복원하지 않는다.
- 일반적인 완료 회원의 재발급·탈퇴 코드는 RETIRED로 영구 보존한다. 단, 친구 모바일 기능 첫 배포 전에는 실제 사용자에게 공유·사용된 친구 코드가 없고 기존 데이터가 테스트 데이터라는 전제에서, 일회성 운영 cleanup으로 미완료 회원의 FriendProfile·소유 ACTIVE 코드와 당시 존재하는 모든 RETIRED 코드를 완전히 삭제한다. 이 예외는 첫 출시 전 한 번만 적용하며 이후 정상 코드 수명주기에 적용하지 않는다.

### 2.3 닉네임 검색

- 인증되고 프로필을 완료한 ACTIVE 회원만 검색할 수 있다.
- 검색 허용 설정을 켠 회원만 결과에 포함한다.
- 검색 허용 기본값은 true다.
- FriendSettings는 서버의 현재 nicknameSearchable 값을 먼저 조회하고 사용자가 변경한 최종 값을 PATCH 응답으로 다시 받는다.
- 닉네임 1글자 이상 부분 일치로 검색하며 한 페이지는 최대 20건이다.
- `%`, `_`, `!`는 검색 문법이 아니라 닉네임의 일반 문자로 해석한다. 서버는 SQL LIKE escape를 적용해 이 문자만으로 검색 공개 회원 전체를 열거할 수 없게 한다.
- 결과는 닉네임 가나다순, friendPublicId 오름차순으로 안정 정렬한다.
- opaque cursor 기반으로 hasNext와 nextCursor를 반환하며 cursor는 마지막 결과의 정렬 위치를 서버만 해석할 수 있게 표현한다.
- 같은 검색어와 cursor를 사용해 다음 페이지를 조회하고 검색어가 바뀌면 cursor를 다시 사용할 수 없다.
- 신규 가입이나 프로필 변경으로 확정하는 닉네임은 ACTIVE 회원 사이에서 중복될 수 없다. WITHDRAWN 회원의 닉네임은 재사용할 수 있다.
- 닉네임 중복은 trim·Unicode NFC·소문자 `nickname_key`를 기준으로 하며, 운영 MySQL의 `utf8mb4_unicode_ci` 비교 규칙에 따라 대소문자와 악센트 차이도 같은 닉네임으로 취급한다. 예를 들어 `Jose`와 `José`는 함께 사용할 수 없다.
- 기존 ACTIVE 회원에 이미 존재하는 중복 닉네임은 임의 변경하지 않는다. 해당 회원이 닉네임을 그대로 두고 다른 프로필 필드만 수정할 수는 있지만, 새 닉네임을 확정할 때는 ACTIVE 중복 검사를 통과해야 한다.
- `스쿠리 유저`, `운영자`가 포함된 닉네임은 회원가입과 프로필 편집에서 사용할 수 없다. 비교 전 앞뒤 공백과 Unicode 표현 차이를 정규화하고, 예약어 판정에서는 중간 공백으로 우회할 수 없게 한다.
- 닉네임 중복과 예약어 검사는 별도 중복 확인 버튼을 두지 않고 회원가입·프로필 저장 요청에서 서버가 최종 검증한다. 중복은 `409`, 예약어는 `422`로 거부하며 모바일은 현재 화면에 머물러 안내한다.
- 기존 중복 닉네임이 남아 있을 수 있으므로 검색 결과에는 프로필 사진과 학과를 함께 제공하고 필요한 화면에서만 friendPublicId 식별 코드를 보조로 표시한다.
- 이메일, 실명, 학번, Firebase UID는 검색 결과에 노출하지 않는다.
- 검색 결과의 공개 식별자 필드명은 friendPublicId로 통일한다.
- 차단 관계는 양방향 모두 검색 결과에서 제외한다.

가입 완료 판정은 다음을 모두 만족하는 경우다.

- Member 상태가 ACTIVE다.
- nickname이 Java `String.isBlank()` 기준으로 null·빈 문자열·공백 문자열이 아니다.
- studentId가 Java `String.isBlank()` 기준으로 null·빈 문자열·공백 문자열이 아니다.
- department가 Java `String.isBlank()` 기준으로 null·빈 문자열·공백 문자열이 아니다.
- photoUrl은 선택값이므로 완료 판정에 포함하지 않는다.

- 프로필 완료의 공백 판정은 Java `String.isBlank()` 의미를 기준으로 backfill·검색 repository query와 운영 preflight·cleanup·postcheck SQL에서 동일해야 한다.
- 프로필 미완료 회원의 최초 완료 전환에서는 현재 닉네임이 예약어인 채로 학번·학과만 채우는 부분 수정을 `NICKNAME_RESERVED`로 거부한다. 이미 완료된 기존 예약어 닉네임 회원은 grandfathering 대상이므로 일반 완료 판정과 Friend 기능에서 제외하지 않는다.

예약어 판정에 사용하는 Unicode 공백 제거 기준은 Java 닉네임 입력 검증과 회원가입·프로필 편집 API에서 동일해야 한다. 가입 완료 판정, backfill·검색 repository query, 운영 preflight·cleanup·postcheck SQL에는 예약어 조건을 넣지 않는다.

검색·친구 코드 preview의 요청 행동 상태는 Boolean 하나로 축약하지 않고 다음 enum을 사용한다.

| 상태 | 의미 | 모바일 행동 |
| --- | --- | --- |
| REQUESTABLE | 활성 관계나 PENDING 요청이 없음 | `요청` |
| INCOMING_PENDING | 상대가 나에게 보낸 PENDING 요청이 있음 | `수락` |
| OUTGOING_PENDING | 내가 상대에게 보낸 PENDING 요청이 있음 | 비활성 `요청 보냄` |
| ALREADY_FRIEND | 이미 friendship이 있음 | 비활성 `이미 친구` |

INCOMING_PENDING에서 기존 요청 생성 API를 호출하면 역방향 PENDING 요청을 수락해 friendship을 만드는 기존 서버 계약을 사용한다. 명시적 재검색·preview는 과거 클라이언트 상태가 아니라 서버 최신 상태를 반환한다.

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

- 월요일부터 금요일, 1교시부터 15교시까지 계산한다.
- 야간 수업 펼침·접기는 모바일 표시 상태일 뿐 공통 공강 계산 범위를 줄이지 않는다.
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
- 수신자의 명시적 수락은 항상 필요하다.
- 파티장이 보낸 초대는 수신자 수락과 동시에 참여가 확정된다.
- 일반 참가자가 보낸 초대는 수신자 수락 시 같은 파티의 PENDING 동승 요청을 재사용하거나 없으면 새로 만들고, 파티장의 수락을 한 번 더 받아야 참여가 확정된다.
- 초대는 좌석을 예약하지 않는다.
- 남은 자리보다 많은 대기 초대를 발송할 수 있다.
- 수락 시점에 파티 상태, 정원, 기존 참여, 다른 활성 파티 참여 여부, 친구·차단 관계를 다시 검증한다.
- 파티가 OPEN이 아니게 되거나 정원이 먼저 차면 처리되지 않은 PENDING 초대는 EXPIRED로 확정하고 구체적인 만료 사유를 반환한다.
- 초대자가 파티 참가자가 아니게 되거나 기존 친구 관계가 종료·차단된 경우에도 해당 PENDING 초대를 EXPIRED로 확정한다.
- 정원이 다시 생기거나 파티가 다시 열려도 EXPIRED 초대는 복원하지 않으며 새 초대를 발송해야 한다.
- 수신자가 다른 활성 파티에 참여 중인 상태는 대상 파티가 여전히 OPEN이고 자리가 있으며 친구 관계가 유효한 동안에만 PENDING을 유지할 수 있는 일시적 수락 실패다.
- 파티 상태·정원·참여자·친구 관계 변경 시 선제적으로 만료시키고, 받은 초대 목록·수락 처리에서도 누락된 만료를 재검증한다. badge count는 이 선제 전이가 저장한 PENDING 상태를 DB에서 바로 집계해 초대 수에 비례한 보정 transaction을 만들지 않는다.
- 관리자 `CLOSE`, `CANCEL`, `END`도 PENDING 초대를 EXPIRED + TARGET_UNAVAILABLE로 정리한다. 이후 `REOPEN`해도 만료된 초대는 복원하지 않는다.
- 관리자가 일반 참가자를 제거하면 그 참가자가 해당 파티에 보낸 PENDING 초대는 EXPIRED + INVITER_LEFT로 정리한다.
- 같은 파티에 대한 PENDING 참가 요청과 친구 초대가 함께 있으면, 파티장 초대 수락은 즉시 참가를 확정하고 참가 요청을 CANCELED로 정리한다. 일반 참가자 초대 수락은 기존 요청을 재사용하거나 새 요청을 만들어 파티장 승인을 기다린다. 참가 요청 수락이 실제 참가를 확정하면 아직 PENDING인 초대는 EXPIRED + ALREADY_JOINED로 같은 트랜잭션에서 정리한다.
- 다중 선택 발송은 batch 전체 원자성이 아니라 수신자별 원자성을 사용한다. 각 수신자는 SENT, ALREADY_PENDING, ALREADY_MEMBER, NOT_ELIGIBLE 중 하나의 결과를 가진다.
- 응답 item 순서는 요청의 friendPublicId 순서를 유지하고 SENT item만 새 초대를 생성한다. 차단·친구 관계 상실·다른 활성 파티처럼 민감하거나 변동 가능한 사유는 NOT_ELIGIBLE로 통합한다.
- 파티 없음·비OPEN, 초대자 비참여, 잘못된 batch 형식 같은 요청 전체 조건이 실패할 때만 초대를 하나도 만들지 않고 4xx로 응답한다.

### 2.7 공개 채팅방 친구 초대

- UNIVERSITY, DEPARTMENT, GAME, 공개 CUSTOM 채팅방만 지원한다.
- PARTY 채팅방은 택시파티 초대 흐름을 사용한다.
- 1:1 채팅과 비공개 채팅방은 V1에서 구현하지 않는다.
- 초대자는 발송 및 수락 시점에 해당 공개방의 참여자여야 한다.
- 수신자는 초대자의 현재 친구여야 한다.
- 학과방 등 기존 채팅방 입장 자격을 발송 및 수락 시점에 모두 검증한다.
- 공개방 초대는 생성 후 7일이 지나면 만료된다.
- 방 삭제, 비공개 전환, 정원 마감, 기존 참여, 친구 해제, 차단, 초대자 탈퇴 시 PENDING 초대를 EXPIRED로 확정한다.
- 정원이 다시 생기거나 최대 인원이 늘어나도 EXPIRED 초대는 복원하지 않으며 새 초대를 발송해야 한다.
- 방 상태·정원·참여자·친구 관계 변경 시 선제적으로 만료시키고, 받은 초대 목록·수락 처리에서도 누락된 만료를 재검증한다. badge count는 `expires_at > now`인 PENDING만 DB에서 집계하고 시간 만료 저장은 한 호출당 최대 100건으로 제한한다.
- 회원 탈퇴로 모든 방에서 제거될 때 해당 회원이 발송·수신한 PENDING 초대는 EXPIRED + MEMBER_WITHDRAWN으로 정리한다.
- 학과 변경으로 기존 학과방에서 제거될 때 그 방에서 보낸 PENDING 초대는 EXPIRED + INVITER_LEFT, 해당 회원이 받은 모든 학과방 PENDING 초대는 EXPIRED + ELIGIBILITY_CHANGED로 정리한다.
- 관리자 공개방 삭제는 방 행을 먼저 잠그고 그 방의 PENDING 초대를 EXPIRED + TARGET_UNAVAILABLE로 정리한 뒤 메시지·멤버십·방을 삭제한다.
- 다중 선택 발송은 택시파티와 같은 수신자별 SENT, ALREADY_PENDING, ALREADY_MEMBER, NOT_ELIGIBLE 결과를 요청 순서대로 반환한다.
- 공개 non-PARTY 방 여부, 초대자 참여와 방 전체 자격이 실패하면 전체 4xx이며, 수신자별 관계·차단·입장 자격 경쟁은 NOT_ELIGIBLE로 처리해 다른 SENT 결과를 되돌리지 않는다.

### 2.8 마인크래프트 계정 노출

- 친구가 등록한 SELF 계정과 그 SELF 아래의 모든 FRIEND 계정을 제공한다.
- 친구 목록에서는 대표 SELF 게임명과 전체 계정 수를 요약 제공한다.
- 친구 상세에서는 SELF를 부모, FRIEND를 자식으로 계층 표시한다.
- 친구 관계가 성립하면 별도의 마인크래프트 공개 설정 없이 볼 수 있다.
- 제공 필드는 게임명, 에디션, 아바타 UUID다. SELF 부모와 FRIEND 자식의 구조는 응답의 중첩 배열로만 표현한다.
- 계정 ID, 계정 역할 원문, 부모 계정 ID, normalizedKey, linkedAt, lastSeenAt, 온라인 상태, 내부 ownerMemberId는 제공하지 않는다.
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
- 차단 관계에서는 친구 코드 preview, 친구 검색, 요청, 친구 상세, 친구 시간표, 마인크래프트 계정, 택시·채팅 초대를 제공하지 않는다.
- 차단 해제 후 친구 관계와 공유 설정은 자동 복원하지 않는다.
- V1에서는 공개 게시판과 공개 채팅의 기존 콘텐츠를 전역 필터링하지 않는다.

### 2.10 알림

신규 알림 타입:

- FRIEND_REQUEST
- FRIEND_ACCEPTED
- FRIEND_DECLINED
- PARTY_INVITATION
- CHAT_ROOM_INVITATION

알림 정책:

- 기존 회원 알림 설정 요청·응답에 `friendAndInvitationNotifications` 단일 Boolean 필드를 추가한다.
- 신규 회원 기본값은 true이며 기존 회원도 migration 또는 backfill로 true를 채운다. backfill 완료 전 null은 true로 해석하고 응답에는 항상 유효 Boolean 값을 반환한다.
- 부분 PATCH에서 필드가 null 또는 생략되면 기존 값을 유지한다.
- 친구·초대 알림의 유효 수신 조건은 `allNotifications && friendAndInvitationNotifications`다.
- `partyNotifications`는 최초 PARTY_INVITATION을 제어하지 않고, 초대 수락 후 기존 파티 활동 알림에만 적용한다.
- 친구 요청은 수신자에게, 친구 수락과 거절은 원 요청자에게 알린다.
- 택시파티와 공개방 초대는 수신자에게 알린다.
- 요청 취소, 친구 끊기, 차단은 푸시 알림을 발송하지 않는다.
- 유효 수신 조건이 false면 일반 알림 인박스 row, 알림 SSE와 FCM은 생성·전송하지 않지만 FriendHub의 친구 요청·초대 도메인 원본과 PENDING badge는 유지한다.
- FRIEND_REQUEST payload는 requestId, FRIEND_ACCEPTED payload는 friendPublicId를 포함한다.
- FRIEND_DECLINED payload는 requestId를 포함하되 V1 terminal 이력을 카드로 재구성하지 않는다.
- PARTY_INVITATION과 CHAT_ROOM_INVITATION payload는 invitationId와 invitationType을 포함한다.
- FRIEND_REQUEST와 FRIEND_DECLINED는 친구 허브 요청 탭, FRIEND_ACCEPTED는 수락한 친구 상세, 초대 알림은 친구 허브 초대 탭의 해당 invitationId 카드로 이동한다.

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
- 친구 요청의 거절·취소·만료 terminal 이력 조회
- 공개 게시판·공개 채팅에서 차단 상대 콘텐츠 전역 숨김
- 관리자 친구 관계망 조회와 강제 친구 관리

---

## 5. 도메인 경계

Friend 기능을 하나의 거대한 서비스에 모으지 않고 각 도메인의 최종 책임을 유지한다.

| 도메인 | 책임 |
| --- | --- |
| friend 신규 도메인 | ACTIVE·RETIRED 친구 코드 registry, 검색 허용, 요청, 상호 관계, 즐겨찾기, 친구 끊기, 차단 |
| academic | 시간표 공개 기본값·친구별 예외, 권한에 맞춘 친구 시간표 projection, 관계 종료·차단 시 양방향 예외 정리 |
| taxiparty | 초대 생성, OPEN·정원·참여 상태 검증, 수락 동시성 |
| chat | 공개방 초대 생성, 방 유형·공개 여부·입장 자격 검증 |
| minecraft | 친구에게 제공 가능한 계정 projection |
| notification | 도메인 이벤트를 인박스·FCM·SSE로 전달 |
| member | ACTIVE 회원 확인, 공개 프로필, 탈퇴 정리 orchestration |

Friend 도메인은 다른 도메인의 내부 엔티티를 직접 수정하지 않는다. 초대 수락과 시간표 조회는 해당 도메인의 Service가 최종 권한과 상태를 확인한다.

---

## 6. 데이터 모델

`friend_profiles`, `friend_code_registry`, `friend_requests`, `friendships`, `friend_preferences`, `member_blocks`, 시간표 공유의 `timetable_sharing_settings`, `timetable_share_overrides`, 초대의 `party_invitations`, `chat_room_invitations`는 런타임 테이블이다. 알림 설정 관련 표는 이후 구현 단위의 논리 모델이며 실제 컬럼명과 마이그레이션은 해당 구현 PR에서 ERD와 함께 확정한다.

ACTIVE 닉네임 중복은 서비스 조회만으로 판단하지 않고 동시 저장도 막는 DB unique claim을 사용한다. `members.nickname_key`는 새로 가입하거나 닉네임을 변경해 정책을 통과한 ACTIVE 회원의 정규화 키이며 nullable unique다. 운영 MySQL `utf8mb4_unicode_ci` 비교로 대소문자·악센트 차이는 같은 claim으로 취급한다. 기존 중복 닉네임은 임의 변경하지 않고 grandfathering을 위해 claim을 강제로 채우지 않는다. 기존 닉네임과 동일한 값을 유지한 프로필 수정은 허용하고, 새 값으로 변경할 때는 claim이 없는 기존 ACTIVE 닉네임까지 조회해 중복을 거부한다. 탈퇴 시 claim을 해제해 닉네임 재사용을 허용한다. 실제 컬럼·인덱스는 Core 출시 준비 PR에서 `docs/erd.md`와 동기화한다.

### 6.1 friend_profiles

| 필드 | 설명 |
| --- | --- |
| member_id | 회원 내부 ID, PK |
| public_id | 친구 기능 전용 무작위 공개 ID, unique |
| active_friend_code_id | friend_code_registry의 ACTIVE 코드 참조, unique |
| nickname_searchable | 닉네임 검색 허용, 기본 true |
| created_at | 생성 시각 |
| rotated_at | 재발급 시각 |

public_id와 활성 친구 코드는 members.id 또는 Firebase UID에서 파생하지 않는다. 모바일과 외부 API는 members.id를 친구 식별자로 노출하지 않는다.
코드 preview, 닉네임 검색, 요청 생성, 친구·차단 목록과 상세에서 이 공개 ID의 JSON·path field 명칭은 friendPublicId로 통일한다.

friend_code_registry:

| 필드 | 설명 |
| --- | --- |
| id | 불투명 내부 ID |
| normalized_code | 정규화된 친구 코드, 영구 unique |
| owner_member_id | ACTIVE일 때 소유 회원, RETIRED면 null 허용, non-null unique |
| status | ACTIVE, RETIRED |
| issued_at | 발급 시각 |
| retired_at | 재발급·탈퇴로 폐기된 시각, ACTIVE면 null |

- 생성기는 registry에 하이픈을 제거한 대문자 `normalized_code`를 INSERT하고 영구 unique 충돌 시 새 무작위 값으로 제한된 횟수만 재시도한다. API는 `SKR-XXXX-XXXX` 표시 형식만 반환한다.
- 재발급은 현재 registry row를 RETIRED로 바꾸고 owner_member_id를 제거한 뒤 새 ACTIVE row와 profile 참조를 같은 트랜잭션에서 확정한다.
- RETIRED registry row는 일반적인 탈퇴 cleanup에서도 삭제하지 않는다. 다만 모바일 첫 출시 전 실제 사용 이력이 없고 모든 데이터가 테스트 데이터라는 전제에서 실행하는 일회성 cleanup은 아래 Core 출시 준비 예외에 따라 당시 존재하던 RETIRED row 전체를 제거한다. preview는 ACTIVE row와 ACTIVE Member profile이 함께 존재할 때만 성공한다.
- ACTIVE row는 owner_member_id와 이를 참조하는 FriendProfile이 모두 필요하다. owner_member_id의 non-null unique와 profile의 active_friend_code_id unique로 회원당 ACTIVE 코드 한 개와 코드당 profile 한 개를 보장한다.

기존 회원 provisioning:

- 앱 기동 시 FriendProfile이 없는 프로필 완료 ACTIVE Member만 고정 크기 batch로 조회해 backfill한다. 이미 profile이 있는 완료 회원은 재조회·잠금·재발급하지 않는다.
- 신규 회원은 최초 소셜 로그인 시점이 아니라 미완료에서 완료로 전이된 프로필 저장 트랜잭션의 after-commit 후 provisioning한다. backfill과 같은 멱등 provisioning service를 사용하고 이미 존재하는 profile은 다시 만들지 않는다.
- public_id와 friend_code_registry.normalized_code는 각각 unique constraint 충돌 시 새 무작위 값을 생성해 제한된 횟수만 재시도하며 members.id나 Firebase UID에서 파생하지 않는다.
- 친구 API 진입 시 프로필 완료 ACTIVE Member인데 profile이 없는 비정상·과도기 상태만 같은 service로 lazy ensure해 self-heal한다. 미완료 회원은 `409 MEMBER_PROFILE_INCOMPLETE`로 거부하고 Friend 데이터를 만들지 않는다.
- backfill과 lazy ensure는 기존 FriendProfile이 참조한 코드 row 자체가 유실된 손상을 정상 상태로 간주하지 않는다. 운영 검증에서 orphan·깨진 참조를 별도로 탐지하고 자동으로 새 코드를 덮어 발급하지 않는다.
- 배포 검증에서 프로필 완료 ACTIVE Member 수와 FriendProfile 보유 완료 ACTIVE Member 수가 일치하고, 미완료 회원 profile이 0건이며, 중복 public_id·ACTIVE 코드가 없고 모든 profile이 ACTIVE registry row를 참조하는지 확인한 뒤 모바일 진입점을 노출한다.

Core 출시 준비의 일회성 운영 cleanup:

- 새 eligibility 코드가 모든 Backend 인스턴스에 배포된 뒤 실행한다. 구버전 provisioning이 살아 있는 동안 먼저 삭제하지 않는다.
- read-only preflight에서 미완료 회원과 연결된 FriendProfile·소유 ACTIVE 친구 코드·요청·관계·즐겨찾기·차단과, 당시 존재하는 RETIRED registry row 전체를 정확히 집계한다.
- 대상 회원과 연결된 파생 Friend 데이터를 관계 순서대로 정리하고 FriendProfile과 소유 ACTIVE registry row를 같은 작업에서 완전히 삭제한다. 모바일 첫 출시 전 테스트 데이터 cleanup에서는 당시 존재하는 RETIRED registry row 전체도 함께 제거한다.
- 기존 완료 회원의 정상 ACTIVE 코드와 기존 중복 닉네임은 수정하지 않는다. RETIRED row 전체 제거는 실제 사용 이력이 없는 첫 출시 전 한 번에만 적용하며, 출시 후에는 일반 정책대로 tombstone을 보존한다.
- postcheck에서 미완료 회원 Friend row 0건, 남은 RETIRED registry row 0건, orphan 0건, 완료 회원 누락 0건을 확인한다.
- 실행 파일은 `docs/sql/2026-08-21-friend-core-readiness-preflight.sql` → `docs/sql/2026-08-21-friend-core-readiness-cleanup.sql` → `docs/sql/2026-08-21-friend-core-readiness-postcheck.sql` 순서로 사용한다. cleanup procedure에는 preflight에서 기록한 정확한 7개 건수를 전달하며 불일치하면 전체 트랜잭션을 rollback한다.

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

`active_pair_key`와 friendship의 low/high pair는 Java 문자열 비교 기준으로 정규화한다. DB collation에 따른 정렬은 비관적 잠금 획득 순서에만 사용하며 저장·조회 key의 기준으로 사용하지 않는다.

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

### 6.6 timetable_sharing_settings

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
| expiry_reason | EXPIRED terminal 사유, 그 외 상태는 null |
| responded_at | 처리 시각 |
| active_target_key | `{partyId}:{inviteeId}`. PENDING일 때만 non-null unique |
| created_at, updated_at | 생성·수정 시각 |

### 6.9 chat_room_invitations

| 필드 | 설명 |
| --- | --- |
| id | 초대 ID |
| chat_room_id | 공개 채팅방 |
| inviter_id | 초대한 방 참가자 |
| invitee_id | 초대받은 친구 |
| status | PENDING, ACCEPTED, DECLINED, CANCELED, EXPIRED |
| expires_at | created_at + 7일 |
| expiry_reason | EXPIRED terminal 사유, 그 외 상태는 null |
| responded_at | 처리 시각 |
| active_target_key | `{chatRoomId}:{inviteeId}`. PENDING일 때만 non-null unique |
| created_at, updated_at | 생성·수정 시각 |

동일 파티·방과 같은 수신자에 대한 PENDING 초대는 중복 생성하지 않는다.

초대 expiry_reason:

- INVITATION_TIMEOUT, TARGET_UNAVAILABLE, CAPACITY_FULL, INVITER_LEFT, ALREADY_JOINED, RELATIONSHIP_UNAVAILABLE, ELIGIBILITY_CHANGED, MEMBER_WITHDRAWN을 외부 안전 enum으로 사용한다.
- expires_at 경과는 INVITATION_TIMEOUT, 파티 비OPEN·방 삭제·비공개 전환 등 대상 aggregate 사용 불가는 TARGET_UNAVAILABLE, 정원 마감은 CAPACITY_FULL, 초대자의 파티·방 이탈은 INVITER_LEFT, 수신자의 기존 참여는 ALREADY_JOINED로 기록한다.
- 차단 여부와 구체적인 관계 상실 원인은 RELATIONSHIP_UNAVAILABLE로 통합한다.
- 관계 외 학과방 자격 같은 입장 조건 변경은 ELIGIBILITY_CHANGED, 초대자·수신자 탈퇴는 MEMBER_WITHDRAWN으로 기록한다. 학과 변경 시 수신자가 받은 기존 학과방 초대도 ELIGIBILITY_CHANGED로 즉시 만료한다. 택시 수신자의 다른 활성 파티 참여는 앞선 terminal 조건이 없는 동안 재시도 가능한 상태이므로 expiry_reason을 기록하지 않고 PENDING을 유지한다.
- PENDING에서 EXPIRED로 전이하는 트랜잭션이 expiry_reason과 responded_at을 함께 한 번만 기록하며 이후 상태 회복이나 lazy reconciliation이 값을 덮어쓰지 않는다.
- 공개방 초대는 expires_at 경과를 취소보다 먼저 판정하므로 기한 뒤 취소 요청도 CANCELED가 아니라 EXPIRED + INVITATION_TIMEOUT으로 확정한다. eligible 조회는 기한이 지난 PENDING을 제외하고, 같은 대상 재발송은 기존 행을 먼저 timeout 만료한 뒤 새 초대를 만든다.
- 공개방에 직접 참여하면서 마지막 좌석을 채우면 참여한 회원의 초대를 먼저 EXPIRED + ALREADY_JOINED로 정리하고, 남은 다른 PENDING 초대만 EXPIRED + CAPACITY_FULL로 정리한다.
- 받은 초대 응답은 status가 EXPIRED일 때만 expiryReason을 제공하고 현재 파티·방 상태로 과거 사유를 재계산하지 않는다.
- 받은 초대 이력의 inviter가 현재 사용자와 양방향 차단 관계이면 프로필 요약을 nullable로 마스킹한다.

### 6.10 기존 회원 알림 설정 확장

| 필드 | 설명 |
| --- | --- |
| friend_and_invitation_notifications | 친구 요청·수락·거절과 택시파티·공개방 초대 알림 허용, 기본 true |

- 기존 NotificationSetting에 포함하며 별도 친구 설정 테이블을 만들지 않는다.
- DB migration과 backfill 완료 전 호환 구간에서는 null을 true로 해석한다.
- API 요청·응답 필드명은 `friendAndInvitationNotifications`를 사용한다.

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

- expires_at이 현재 시각 이하인 PENDING 요청은 EXPIRED로 저장하고 responded_at을 기록하며 active_pair_key를 해제한다.
- 받은·보낸 요청 목록, inbox-counts, 코드 preview·닉네임 검색의 요청 가능 상태, 새 요청 생성, 수락·거절·취소, 역방향 요청 자동 수락은 PENDING을 판단하기 전에 만료를 lazy reconciliation한다.
- 개별 변경 경로는 잠근 요청 행에서 만료를 먼저 `EXPIRED`로 커밋한 뒤 terminal `409`을 반환한다. 수락·역방향 자동 수락의 성공 응답은 같은 mutation 트랜잭션에서 만든 친구 공개 snapshot을 사용한다.
- 목록·badge는 유효 PENDING만 반환한다. 요청 목록은 DB cursor와 제한된 batch 조회를 사용하며, 중간의 만료 후보만 lazy reconciliation한 뒤 다음 제한 batch를 조회한다.
- 10분 주기의 만료 batch는 최대 100건씩 정리하는 보조 수단이며, 정확성은 각 PENDING 의존 경로의 lazy reconciliation으로 보장한다. 이 작업은 회원 전체를 순회하거나 잠그지 않고 만료 후보 request ID만 조회한 뒤 요청별 독립 트랜잭션에서 처리한다. 만료 전이는 기존 회원 행이 있으면 상태와 무관하게 잠그므로 탈퇴 회원의 오래된 요청도 terminal 정리할 수 있다.

### 7.2 친구 관계

~~~text
친구 요청 수락 ──> ACTIVE friendship
ACTIVE ── 친구 끊기 ──> 삭제
ACTIVE ── 어느 한쪽 차단 ──> 삭제
회원 탈퇴 ──> 관련 관계 전체 삭제
~~~

### 7.3 택시파티 초대

~~~text
PENDING ── 파티장 초대 수락 ──> ACCEPTED + 파티 참여 (같은 파티 PENDING 동승 요청은 CANCELED)
   ├────── 참가자 초대 수락 ──> ACCEPTED + 동승 요청 PENDING (기존 요청 재사용 또는 새 생성)
   ├────── 거절 ──> DECLINED
   ├────── 발송자 취소 ──> CANCELED
   ├────── 파티 비OPEN·정원 마감 ──> EXPIRED
   ├────── 초대자 파티 이탈·기존 참여 ──> EXPIRED
   └────── 친구 해제·차단 ──> EXPIRED
~~~

정원 확인과 파티 참여 또는 동승 요청 생성은 같은 트랜잭션과 잠금 경계에서 처리한다. 일반 참가자의 초대 수락은 같은 파티의 기존 PENDING 동승 요청이 있으면 이를 재사용하고, 없으면 새로 생성한 뒤 `result=LEADER_APPROVAL_PENDING`과 해당 `joinRequestId`를 반환한다. 파티장 초대 수락은 즉시 참가하며 `result=JOINED`를 반환하고 같은 파티의 PENDING 동승 요청을 CANCELED로 정리한다. 정원이 가득 차는 순간 남아 있는 PENDING 초대와 동승 요청을 각각 `EXPIRED + CAPACITY_FULL`로 전환하며, 초대 목록·수락 진입에서는 누락된 terminal 조건을 lazy reconciliation한다. badge count는 선제 전이 결과를 DB에서 직접 세며, EXPIRED는 파티 재개방이나 자리 발생으로 복원하지 않는다. 수신자의 다른 활성 파티 참여만 대상 초대의 다른 terminal 조건이 충족되지 않은 동안 PENDING을 유지할 수 있는 재시도 가능 사유다.

관리자 `CLOSE`, `CANCEL`, `END`도 같은 만료 규칙을 적용하고 `REOPEN`은 기존 EXPIRED 초대를 복원하지 않는다. 관리자 멤버 제거는 제거된 참가자가 보낸 해당 파티의 PENDING 초대를 INVITER_LEFT로 만료한다.

같은 회원이 동일 파티에 PENDING 참가 요청과 PENDING 친구 초대를 함께 가진 경우, 파티장 초대 수락은 즉시 참가를 확정하고 참가 요청을 CANCELED로 만든다. 일반 참가자의 초대 수락은 참가를 확정하지 않고 기존 참가 요청을 재사용해 파티장 승인을 기다린다. 참가 요청 수락이 먼저 실제 참가를 확정하면 아직 PENDING인 초대는 EXPIRED + ALREADY_JOINED로 정리한다.

### 7.4 공개방 초대

~~~text
PENDING ── 수락 성공 ──> ACCEPTED + 공개방 참여
   ├────── 거절 ──> DECLINED
   ├────── 발송자 취소 ──> CANCELED
   ├────── 7일 경과·자격 상실·방 상태 변경 ──> EXPIRED
   └────── 정원 마감·기존 참여 ──> EXPIRED
~~~

정원 제한이 있는 공개방이 가득 차는 순간 남아 있는 PENDING 초대를 EXPIRED로 전환한다. 초대 목록·수락 진입에서는 누락된 terminal 조건을 lazy reconciliation하고, badge count는 기한이 남은 PENDING만 DB에서 직접 집계한다. 자리 발생이나 최대 인원 증가로 EXPIRED를 복원하지 않는다.

학과 변경은 기존 학과방에서 발송한 초대뿐 아니라 변경 회원이 받은 모든 학과방 PENDING 초대도 ELIGIBILITY_CHANGED로 만료한다. 관리자 공개방 삭제는 방 잠금 뒤 PENDING 초대를 TARGET_UNAVAILABLE로 먼저 만료하고 방을 제거한다.

---

## 8. 권한 및 데이터 노출 행렬

| 기능 | 요구 조건 | 서버 최종 검증 |
| --- | --- | --- |
| 친구 코드 preview | 인증 ACTIVE 회원 | 유효 코드, self 아님, 양방향 차단 아님. 차단은 일반 대상 없음으로 응답 |
| 닉네임 검색 | 인증 ACTIVE 회원 | 검색 허용, 차단 아님 |
| 친구 요청 | 인증 ACTIVE 회원 | self·기존 친구·중복 PENDING·차단 아님 |
| 친구 상세 | 상호 친구 | ACTIVE 관계와 차단 없음 |
| 친구 시간표 | 상호 친구 | 최종 공유 범위와 차단 |
| 친구 Minecraft | 상호 친구 | 허용 필드 projection과 차단 |
| 택시 초대 | OPEN 파티 참가자 | 친구, 대상 참여 가능, 중복 초대 아님 |
| 택시 수락 | 초대 수신자 | 파티 lock 후 OPEN·정원·활성 파티 재검증 |
| 공개방 초대 | 공개방 참가자 | 공개 non-PARTY, 친구, 입장 자격 |
| 공개방 수락 | 초대 수신자 | 만료·방 상태·정원·입장 자격·초대자 참여 재검증 |
| 즐겨찾기 | 상호 친구 | 설정 소유자 방향만 변경 |
| 친구 끊기 | 상호 친구 중 한 명 | 양방향 파생 데이터 정리 |
| 차단 | 인증 ACTIVE 회원 | self 차단 금지, 멱등 처리 |

---

## 9. API 계약

`POST /v1/friend-codes/preview`, `GET/POST /v1/friends/me/code*`, `GET/PATCH /v1/friends/me/privacy`, 9.1~9.7의 관계 Core·시간표 공유·Minecraft projection·친구 초대 API는 런타임 OpenAPI와 Contract·Service 테스트로 고정했다. 9.8의 알림 확장은 구현 설계를 위한 예정 계약이며 현재 운영 API가 아니다.

### 9.1 친구 핵심

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/friends | 친구 목록, 즐겨찾기·가나다순 |
| GET | /v1/friends/{friendPublicId} | 친구 상세 |
| DELETE | /v1/friends/{friendPublicId} | 친구 끊기 |
| PATCH | /v1/friends/{friendPublicId}/favorite | 즐겨찾기 변경 |
| GET | /v1/friends/search | nickname·opaque cursor 기반 검색, 페이지당 최대 20건 |
| POST | /v1/friend-codes/preview | 친구 코드·QR 대상 공개 프로필 확인, 요청 생성 없음 |
| GET | /v1/friends/me/code | 내 친구 코드 |
| POST | /v1/friends/me/code/regenerate | 친구 코드 재발급 |
| GET | /v1/friends/me/privacy | 현재 닉네임 검색 허용 설정 조회 |
| PATCH | /v1/friends/me/privacy | 닉네임 검색 허용 설정 |
| GET | /v1/friends/inbox-counts | 받은 요청·초대 badge count |

친구 목록 최소 응답:

- friendPublicId
- nickname
- department
- photoUrl
- favorite

친구 목록·상세는 위 다섯 필드와 nullable `primaryMinecraftGameName`, `minecraftAccountCount`, `effectiveTimetableScope`를 반환한다. 대표 SELF 계정이 없으면 게임명은 null이고, 계정 수는 등록된 SELF·FRIEND 전체 수다. `effectiveTimetableScope`는 **해당 친구가 나에게 공개하는 실제 범위**이며, 친구별 예외가 있으면 예외가 기본값보다 우선한다.

관계 Core의 HTTP 응답은 다음처럼 고정한다.

- `DELETE /v1/friends/{friendPublicId}`, `PATCH /v1/friends/{friendPublicId}/favorite`, 요청 거절·취소, 차단·차단 해제는 성공 시 `204 No Content`다.
- Core 출시 준비 이후 닉네임 검색과 친구 코드 preview는 다섯 공개 프로필 필드와 `relationshipState`를 반환한다. 기존 `canSendFriendRequest` Boolean은 제거하며, 차단 대상은 검색에서 제외하고 preview는 일반 대상 없음으로 마스킹한다.
- 차단 목록 항목은 `friendPublicId`, `nickname`, `department`, `photoUrl`, `blockedAt`을 반환한다.
- `inbox-counts`는 유효 PENDING 친구 요청·택시파티 초대·공개방 초대를 각각 계산하고 total은 세 값의 합이다. 친구 요청은 기존 bounded lazy expiry를 사용한다. 파티 초대 count는 선제 terminal 전이가 저장한 PENDING을 직접 세며, 공개방 초대 count는 `expires_at > now` 조건으로 기한이 남은 PENDING만 세고 시간 만료 저장을 최대 100건으로 제한한다. 받은 초대 목록과 mutation은 누락된 terminal 조건의 lazy reconciliation 안전망을 유지한다.

검색 query와 응답:

- query: 1~50자 nickname 필수, cursor 선택, size 기본 20·최대 20
- response: items, hasNext, nextCursor
- items는 닉네임 가나다순, friendPublicId 오름차순이며 cursor는 같은 nickname query에만 사용할 수 있다.

privacy 조회·변경 응답:

- GET과 PATCH 모두 null이 아닌 nicknameSearchable을 반환한다.
- PATCH body의 nicknameSearchable은 필수 Boolean이며 저장된 최종 값을 응답한다.
- 모바일은 GET 완료 전 로컬 기본값으로 toggle을 추측하지 않는다.
- 신규 profile 기본값과 기존 운영 데이터의 정책 기본값은 true다. 모바일은 사용자가 toggle을 누르면 낙관적으로 반영하고 PATCH 실패 시 마지막 서버 확인 값으로 원복한다.

inbox-counts 응답:

- incomingRequestCount: 내가 받은 유효 PENDING 친구 요청 수
- partyInvitationCount: 내가 받은 유효 PENDING 택시파티 초대 수
- chatRoomInvitationCount: 내가 받은 유효 PENDING 공개방 초대 수
- totalActionCount: 위 세 값의 합계
- 내가 보낸 PENDING 친구 요청은 어떤 badge count에도 포함하지 않는다.

### 9.2 친구 요청

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/friend-requests | direction·opaque cursor 기준 현재 PENDING 받은·보낸 요청 |
| POST | /v1/friend-requests | friendPublicId로 요청 |
| POST | /v1/friend-requests/{requestId}/accept | 수락 |
| POST | /v1/friend-requests/{requestId}/decline | 거절 |
| DELETE | /v1/friend-requests/{requestId} | 요청자 취소 |

`POST /v1/friend-codes/preview`는 body의 friendCode를 정규화해 공개 프로필과 friendPublicId를 반환하는 부작용 없는 확인 API다. 양방향 차단은 잘못되거나 폐기된 코드와 같은 일반 대상 없음 응답으로 처리한다. 친구 코드는 query string이나 로그에 원문으로 남기지 않는다. `POST /v1/friend-requests`는 확인·검색 결과의 friendPublicId만 받으며 내부 members.id와 friendCode를 요청 생성 계약에 사용하지 않는다.

친구 요청 생성·수락 mutation 응답:

- 일반 요청 생성은 `PENDING`과 생성한 `requestId`를 반환한다.
- 대상이 나에게 보낸 유효 PENDING 요청이 있으면 새 요청을 만들지 않고 기존 요청을 `ACCEPTED`로 전이해 friendship을 만든다. 이 역방향 요청 호출은 `ACCEPTED`와 친구 공개 프로필을 반환한다.
- 수락 API의 재호출은 이미 같은 friendship이 성립한 경우 같은 친구 공개 프로필을 반환하는 멱등 성공이다.
- 거절·취소는 현재 PENDING인 요청만 전이할 수 있으며, 이미 terminal 상태이면 `409`로 처리한다.
- accept·decline·cancel에서 존재하지 않는 requestId는 `404 FRIEND_REQUEST_NOT_FOUND`다. ACTIVE 대상 확인이 불가능하거나 차단 마스킹이 필요한 경우는 `404 FRIEND_TARGET_NOT_FOUND`를 사용한다.
- 차단 관계의 대상은 차단 사실을 노출하지 않는다. 친구 요청 생성은 `404 FRIEND_TARGET_NOT_FOUND`로 처리하고, 친구 코드 preview·닉네임 검색에서도 동일하게 일반 대상 없음으로 숨긴다.

Core 출시 준비 이전 preview의 `canSendFriendRequest`는 기존 친구와 유효 PENDING 요청을 모두 false로 축약해 교차 요청·이미 친구를 구분하지 못했다. Core 출시 준비 PR에서 이 필드를 `relationshipState` enum으로 교체한다. 양방향 차단은 preview 자체를 `404 FRIEND_CODE_NOT_FOUND`로 마스킹하며 상태로 구분해 노출하지 않는다. 재발급 제한 중 `POST /v1/friends/me/code/regenerate`는 `429 FRIEND_CODE_REGENERATION_COOLDOWN`과 초 단위 `Retry-After` 헤더를 반환한다. 전송 timeout 이후 앱은 새 재발급 요청을 자동 재시도하지 않고 `GET /v1/friends/me/code`로 현재 코드를 조회해 조정한다.

요청 목록 계약:

- query의 direction은 RECEIVED 또는 SENT 필수이며 cursor는 선택, size는 기본 20·최대 20이다.
- V1 목록은 lazy expiry를 반영한 현재 PENDING 요청만 반환하고 terminal 요청 이력은 제공하지 않는다.
- 정렬은 createdAt DESC, requestId DESC이며 response는 items, hasNext, nextCursor를 사용한다.
- 처리 직후 terminal 상태는 mutation 응답과 클라이언트의 짧은 완료 상태로 표시한 뒤 목록에서 제거한다. terminal 이력 조회는 후속 TODO다.

### 9.3 차단

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/friends/blocks | 내 차단 목록 |
| POST | /v1/friends/blocks | 회원 차단 |
| DELETE | /v1/friends/blocks/{friendPublicId} | 차단 해제 |

차단 목록 item은 friendPublicId, 닉네임, 프로필 사진, 학과와 차단 시각을 제공한다. 차단 해제 path는 이 item의 friendPublicId를 사용하며 내부 members.id를 노출하지 않는다.

### 9.4 시간표 공유

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/timetables/my/sharing-settings | 내 기본값·친구별 예외 |
| PATCH | /v1/timetables/my/sharing-settings | 기본 공개 범위 변경 |
| PUT | /v1/timetables/my/sharing-overrides/{friendPublicId} | 친구별 예외 저장 |
| DELETE | /v1/timetables/my/sharing-overrides/{friendPublicId} | 친구별 예외 제거 |
| GET | /v1/timetables/friends/{friendPublicId}?semester={semester} | 필수 학기 기준 친구 시간표 조회 |

semester는 `2026-1` 형식의 필수 query parameter다. 친구 시간표 응답은 요청을 해석한 semester와 effectiveScope를 항상 포함한다.

- PRIVATE: 시간표 필드는 비우고 공개되지 않았음을 표현한다.
- BUSY_ONLY: slots만 제공하고 course 식별·이름 필드는 제공하지 않는다.
- DETAILS: 허용된 course와 slot 상세를 제공한다.

구현 규칙:

- 기본 범위의 저장 레코드가 없으면 `PRIVATE`를 적용한다.
- 친구별 예외는 기본 범위보다 우선하며, 현재 상호 친구인 대상만 설정·조회할 수 있다.
- 친구 끊기 또는 차단 시 Friend가 관계 전이를 orchestration하고, 같은 트랜잭션에 참여하는 Academic cleanup 서비스가 양방향 시간표 공유 예외를 삭제한다.
- `PRIVATE`는 시간표 존재 여부도 공개하지 않아 `hasTimetable=false`, 빈 `courses`·`slots`만 반환한다.
- `BUSY_ONLY`는 점유 시간만 반환하고, `DETAILS`에서만 강의 상세를 반환한다. 직접 입력 강의는 `courseId=null`이다.

### 9.5 마인크래프트

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/friends/{friendPublicId}/minecraft-accounts | 친구가 등록한 전체 계정의 안전 projection |

상호 친구이고 차단 관계가 아닐 때만 조회한다. 응답은 `selfAccounts` 배열이며 각 SELF는 `gameName`, `edition`, `avatarUuid`, `friendAccounts`를 갖고 각 FRIEND는 `gameName`, `edition`, `avatarUuid`만 갖는다. 내부 회원·계정 식별자, 부모 ID, normalizedKey, linkedAt, lastSeenAt, 온라인 상태는 반환하지 않는다.

### 9.6 택시파티 초대

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/parties/{partyId}/invitations/eligible-friends | 초대 가능한 친구 |
| POST | /v1/parties/{partyId}/invitations | 한 명 이상 초대 |
| GET | /v1/party-invitations/received | 내가 받은 초대 |
| POST | /v1/party-invitations/{invitationId}/accept | 초대 수락 |
| POST | /v1/party-invitations/{invitationId}/decline | 초대 거절 |
| DELETE | /v1/party-invitations/{invitationId} | 발송자 취소 |

eligible 응답은 `friends`, `alreadyPendingFriends`, `alreadyMemberFriends`를 분리한다. 다른 활성 파티, 차단 등 민감한 사유는 목록에서 제외하고 `notEligibleCount`에만 포함한다. 정원이 가득 찬 파티는 조회 자체를 실패시키지 않고 `canInvite=false`, `unavailableReason=PARTY_FULL`, `remainingCapacity=0`과 세 목록을 반환한다.

batch 요청과 응답:

- 요청은 중복을 제거한 순서 있는 friendPublicIds를 받으며 첫 등장 순서를 유지한다.
- 각 item은 friendPublicId, outcome과 nullable invitationId를 반환한다.
- SENT는 생성된 invitationId가 필수다. ALREADY_PENDING은 기존 PENDING 초대의 inviter가 현재 요청자일 때만 같은 invitationId를 반환하고 다른 참가자가 발송한 초대면 null이다. ALREADY_MEMBER와 NOT_ELIGIBLE은 null이다.
- 각 수신자 처리는 독립된 원자적 경계이며 일부 item 실패로 이미 SENT인 초대를 rollback하지 않는다.
- batch orchestration은 item 결과를 요청 순서대로 모아 200으로 반환한다. 파티 전체 조건이나 요청 형식 오류만 전체 4xx다.

### 9.7 공개 채팅방 초대

| Method | Path | 설명 |
| --- | --- | --- |
| GET | /v1/chat-rooms/{chatRoomId}/invitations/eligible-friends | 초대 가능한 친구 |
| POST | /v1/chat-rooms/{chatRoomId}/invitations | 한 명 이상 초대 |
| GET | /v1/chat-room-invitations/received | 내가 받은 초대 |
| POST | /v1/chat-room-invitations/{invitationId}/accept | 초대 수락 |
| POST | /v1/chat-room-invitations/{invitationId}/decline | 초대 거절 |
| DELETE | /v1/chat-room-invitations/{invitationId} | 발송자 취소 |

공개방 batch도 택시파티와 같은 요청 순서·수신자별 outcome·nullable invitationId·부분 성공 계약을 사용한다. 차단 여부와 구체적인 관계 상실 사유는 NOT_ELIGIBLE 밖으로 노출하지 않는다.

### 9.8 회원 알림 설정 확장

기존 회원 알림 설정 PATCH 요청과 조회 응답에 `friendAndInvitationNotifications`를 additive field로 추가한다.

- 요청: nullable Boolean이며 null·생략은 기존 값 유지
- 응답: null이 아닌 유효 Boolean
- 신규·기존 회원 유효 기본값: true
- 알림 대상 조건: `allNotifications && friendAndInvitationNotifications`
- 조건이 false여도 친구 요청·초대 원본과 FriendHub PENDING badge는 유지

## 10. 동시성, 멱등성, 보안

- friendPublicId를 받는 Friend write는 대상 해석 전에 호출자의 ACTIVE·프로필 완료 상태를 읽기 검증해 미완료 회원이 대상 존재 여부를 구분하지 못하게 한다. 이후 실제 write 전에 ordered Member pair 잠금을 획득하고 호출자 자격을 다시 검증한다.
- 모든 Friend write와 lazy provisioning은 실제 상태 확정 전에 작업에 관련된 Member row를 잠그고 상태를 다시 읽는다. 회원 쌍 작업은 양쪽 모두, 내 코드·privacy 같은 단일 회원 작업은 현재 회원이 ACTIVE여야만 계속한다.
- 동일 회원 쌍을 변경하는 친구 요청 생성·terminal 전이, 친구 관계 삭제, 차단·차단 해제, 즐겨찾기 변경, 친구별 시간표 override 생성·변경·삭제는 두 Member row를 내부 ID 오름차순으로 같은 PESSIMISTIC_WRITE 잠금 경계에서 획득한다.
- friendPublicId 해석 결과만 신뢰하지 않고 잠금 획득 후 요청자와 대상 Member를 다시 읽어 둘 다 ACTIVE인지 확인한다. 하나라도 WITHDRAWN이면 어떤 Friend row나 파생 데이터도 만들거나 복원하지 않는다.
- 친구 요청 accept·decline·cancel·expire·역방향 자동 수락은 ordered Member pair 다음 FriendRequest 행을 PESSIMISTIC_WRITE로 잠그고 상태·expires_at을 다시 읽는다. 최초로 PENDING을 terminal 상태로 바꾼 트랜잭션만 friendship 생성이나 active_pair_key 해제 같은 부수효과를 수행한다.
- 시간 기준 만료 batch처럼 Member pair가 필요 없는 경로는 FriendRequest 행만 잠그고 그 뒤 Member pair 잠금을 추가로 획득하지 않아 잠금 순서를 역전하지 않는다.
- 잠금 획득 후 차단, 유효 PENDING 요청과 friendship을 다시 조회하고 조건을 재검증한다. 즐겨찾기와 시간표 override는 ACTIVE friendship이 없으면 쓰지 않는다. unique constraint는 마지막 중복 방어선이며 공통 잠금을 대체하지 않는다.
- 택시파티·공개방 초대의 accept·decline·cancel·expire도 Invitation 행을 PESSIMISTIC_WRITE로 잠그고 PENDING을 재확인한 트랜잭션만 terminal 상태와 참여 부수효과를 확정한다.
- 초대 수락과 lazy reconciliation의 잠금 전 권한·대상 확인은 Invitation과 Party·ChatRoom aggregate entity를 영속화하지 않는 scalar/projection snapshot으로 수행한다. 최종 상태 전이는 고정 순서로 aggregate와 Invitation 행을 잠근 뒤 다시 읽은 상태만 사용하므로, 동시 decline·cancel·timeout·정원 마감이 먼저 확정된 초대를 수락으로 되돌리거나 정원을 초과하지 않는다.
- 초대 생성·수락과 파티·방 상태가 필요한 선제 만료의 잠금 순서는 ordered Member pair, Party 또는 ChatRoom aggregate, Invitation 행 순서로 고정한다. 참가 요청 수락도 requester Member를 먼저 잠그고 Party를 잠근다. 관리자 파티 상태 변경·멤버 제거와 공개방 삭제도 aggregate를 먼저 잠근 뒤 관련 Invitation을 정리한다. 회원 탈퇴는 발송·수신 PENDING 초대 대상 ID와 실제 참여 대상 ID를 합쳐 정렬한 뒤 대상 aggregate를 먼저 잠그고 그 대상의 Invitation만 만료한다. decline·cancel·시간 만료처럼 Invitation만 잠그는 경로는 이후 aggregate나 Member pair 잠금을 추가로 얻지 않는다.
- 친구 관계를 전제로 하는 택시파티·공개방 초대 생성과 수락은 위 고정 순서 안에서 친구·차단 상태를 재검증한다.
- 친구 코드 발급·재발급과 lazy provisioning은 해당 Member row를 PESSIMISTIC_WRITE로 잠근 뒤 ACTIVE를 재확인한다. 탈퇴가 먼저 확정됐다면 FriendProfile이나 ACTIVE 코드 registry row를 생성하지 않는다.
- 양방향 동시 요청은 friendship 한 건만 만든다.
- 같은 요청·초대의 accept 재호출은 이미 성공한 동일 수신자라면 멱등 응답을 우선한다. 택시 초대는 최초 수락 결과(`JOINED` 또는 `LEADER_APPROVAL_PENDING`)와 후자의 `joinRequestId`를 초대 행에 확정 저장해, 이후 동승 요청 처리·파티 상태 변화와 무관하게 동일 응답을 반환한다.
- 택시 초대 수락은 기존 TaxiParty 참여 로직과 같은 잠금 경계를 사용한다.
- 공개방 초대 수락은 기존 joinChatRoom 자격 검증을 재사용한다.
- 발송량 제한은 두지 않지만 입력 검증, PENDING 중복 방지와 payload 크기 제한은 적용한다.
- 친구 코드와 QR payload를 로그에 원문으로 남기지 않는다.
- 검색 결과와 친구 projection에 내부 members.id, 이메일, 실명, 학번, Firebase UID를 포함하지 않는다.
- 목록 API에서 N+1 조회가 생기지 않도록 projection 또는 batch 조회를 사용한다.

---

## 11. 회원 탈퇴와 정리

회원 탈퇴 트랜잭션은 Member를 WITHDRAWN으로 변경한 뒤 다음 데이터를 정리한다.

- FriendProfile의 친구 공개 ID와 검색 허용 설정을 삭제하고 현재 ACTIVE 코드는 RETIRED로 바꾸되 registry tombstone은 영구 보존
- 받은·보낸 친구 요청
- 모든 친구 관계와 양방향 즐겨찾기
- 차단·피차단 관계
- 시간표 공개 기본값과 친구별 예외
- 발송·수신한 PENDING 택시파티·공개방 초대는 EXPIRED + MEMBER_WITHDRAWN으로 전이

- 탈퇴 orchestration과 Friend mutation은 같은 Member row 잠금을 공유하며, 대기 중이던 mutation은 잠금 후 WITHDRAWN을 확인해 중단한다.
- Support 신고 이력은 기존 탈퇴 정책대로 보존한다.
- 상세한 Phase 14 계획 cleanup은 docs/member-withdrawal-policy.md와 함께 유지한다.

기존 도메인의 공개 게시물, 채팅 tombstone, 파티 이력은 각 도메인의 기존 탈퇴 정책을 유지한다.

---

## 12. 관리자·운영 범위

- V1 관리자 페이지에는 친구 관계망 조회, 친구 강제 생성·삭제, 시간표 공개 설정 조회를 추가하지 않는다.
- 친구 요청 발송량 제한도 운영 정책으로 추가하지 않는다.
- 운영 로그와 지표에는 요청·수락·거절·차단·초대 성공/실패 횟수를 개인정보 없는 집계 형태로 남길 수 있다.
- 친구 코드, 시간표 상세, 마인크래프트 내부 식별 키는 운영 로그에 기록하지 않는다.

---

## 13. 구현 및 배포 순서

완료된 기준선:

1. Backend #78 기준 문서
2. Backend #79 Friend Foundation
3. Backend #80 관계 Core
4. Backend #81 Core 출시 준비
5. Backend #82 출시 postcheck 문서 보정
6. Backend #83 친구 화면 완성 API
7. Frontend #22 모바일 계획
8. Frontend #23 관계 Core 모바일
9. Frontend #24 Core 출시 준비 UX
10. Frontend #25 친구 화면 완성 UX
11. Backend #84 시간표 공유 API
12. Frontend #26 시간표 공유 UX
13. Backend #85 친구 초대 API
14. Frontend #27 친구 초대 UX

시간표 공유는 Backend #84·Frontend #26에서, 친구 초대는 Backend #85·Frontend #27에서 구현·테스트·문서 정합성 점검과 리뷰 보완을 마쳐 전달을 완료했다. 현재 초대·정원·파티원 UX 보완을 진행하며, 그 뒤 남는 승인 구현은 알림·PENDING 초대 외 탈퇴 정리 한 단계다.

1. 친구 초대 (Backend #85·Frontend #27 전달 완료)
   - TaxiParty와 공개 Chat 수신자별 부분 성공 초대
   - FriendHub 초대 탭과 공통 친구 선택 UX
2. 초대·정원·파티원 UX 보완 (진행 중)
   - 파티장과 일반 참가자 초대의 수락 후 상태 전이 분리
   - 가득 찬 파티의 초대·동승 요청 종료와 재개 차단
   - 초대 가능·초대 중·참여 중 목록과 파티원 목록·리더 강퇴 UI
3. 알림·나머지 탈퇴 정리 (후속)
   - 친구 요청·수락·거절과 초대 인박스·FCM·SSE·화면 이동
   - PENDING 초대 외 모든 Friend·공유 파생 데이터의 회원 탈퇴 cleanup

각 단계는 저장소당 최대 1개 PR로 진행한다. 친구 초대는 Backend·Frontend 각각 1개 PR, 이후 알림·나머지 탈퇴 정리도 Backend·Frontend 각각 1개 PR로 전달하며 Admin 친구 관계망 UI는 V1 제외 범위라 PR을 만들지 않는다. 단계 내부에서 서로 다른 도메인·테스트·문서는 작은 Conventional Commit으로 구분하고, 변경량 때문에 PR 분리가 필요하면 먼저 사용자 승인을 받는다.

Core 출시 준비의 `canSendFriendRequest` → `relationshipState` 교체는 친구 FE가 아직 배포되지 않았으므로 구버전 호환 field를 유지하지 않는다. 이후 단계는 가능한 한 additive API로 Backend를 먼저 배포하고, 모바일 노출은 필요한 API 배포 확인 후 진행한다.

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
- 프로필 완료 ACTIVE Member FriendProfile backfill, 멱등 재실행, public_id·registry code 충돌 재시도와 완료 회원 누락 0건
- 최초 소셜 로그인 미완료 회원에게 FriendProfile·코드가 발급되지 않고 완료 전 Friend API가 `409 MEMBER_PROFILE_INCOMPLETE`로 실패하는지 검증
- 예약 닉네임과 ACTIVE 중복 닉네임 저장 거부, 동시 저장 unique 보장, WITHDRAWN 후 재사용, 기존 중복 nickname grandfathering 검증
- 운영 cleanup fixture에서 미완료 회원 Friend row 0건, orphan 0건, 완료 회원 누락 0건 검증
- 재발급·탈퇴한 RETIRED 코드 영구 미재사용과 과거 코드·QR preview 실패
- 1글자 닉네임 검색, 안정 정렬, 20건 cursor 경계, 기존 중복 닉네임 다음 페이지와 잘못된 query·cursor 조합
- 친구 코드 preview가 요청을 생성하지 않고 friendPublicId 공개 프로필만 반환하는지 검증
- 양방향 차단 시 코드 preview가 일반 대상 없음으로 실패하고 차단 여부를 노출하지 않는지 검증
- 30일 만료가 목록·badge·preview·검색·생성·수락·거절·취소·역방향 요청 전에 반영되고 active_pair_key를 해제하는지 검증
- 요청 accept와 decline·cancel·expire 경쟁에서 terminal 상태와 friendship 부수효과가 하나만 남는지 검증
- 요청·차단·즐겨찾기·공유 설정·초대·lazy provisioning과 회원 탈퇴 경쟁에서 잠금 후 ACTIVE 재확인으로 Friend 데이터가 다시 생기지 않는지 검증
- 친구 요청 수락과 차단 경쟁에서 차단 후 friendship이 남지 않는지 검증
- 즐겨찾기 방향 독립성과 정렬
- 즐겨찾기·시간표 override 쓰기와 친구 끊기·차단 경쟁 후 파생 설정이 남지 않는지 검증
- PRIVATE, BUSY_ONLY, DETAILS projection 필드 미노출 검증
- 선택 학기 요청·응답 일치와 과거 학기 시간표 없음 상태 검증
- 친구 해제·차단 후 시간표와 Minecraft 접근 차단
- OPEN이 아닌 파티 초대 차단
- 택시·공개방 batch의 수신자별 SENT·ALREADY_PENDING·ALREADY_MEMBER·NOT_ELIGIBLE 순서, invitationId 조건과 일부 성공 비rollback
- 택시 마지막 좌석 동시 수락에서 한 명만 성공
- 초대 accept와 decline·cancel·expire 경쟁에서 terminal 상태와 파티·방 참여 부수효과가 하나만 남는지 검증
- 공개 non-PARTY 방만 초대 가능
- 학과방 입장 자격, 7일 만료와 정원 마감 시 EXPIRED·비복원
- inbox-counts가 받은 PENDING 요청·초대만 합산하고 보낸 요청을 제외하는지 검증
- 알림 설정 신규·기존 회원 기본 true, 마스터 우선순위와 backfill 검증
- 알림 설정 off 시 일반 알림 인박스·SSE·FCM 미생성, FriendHub 요청·초대 원본과 PENDING badge 유지
- 파티 비OPEN·정원 마감·관계 상실 시 PENDING 초대 EXPIRED와 비복원 검증
- 두 초대의 EXPIRED 전이 시 안전 expiryReason을 한 번만 저장하고 상태 회복 후에도 같은 사유를 반환하는지 검증
- privacy GET·PATCH가 저장된 nicknameSearchable을 반환하고 PENDING 요청 목록 cursor가 20건 경계와 안정 정렬을 지키는지 검증
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

## 15. 구현 승인 상태

초기 기준 문서의 코드 구현 중지선은 사용자의 단계별 승인으로 해제되었다. 현재 승인 범위는 택시파티·공개 채팅방 친구 초대의 Backend·Frontend 런타임, 테스트, OpenAPI, ERD와 관련 문서 동기화까지다. 알림은 다음 단계 승인 범위로 유지한다. 회원 탈퇴 cleanup 중 발송·수신 PENDING 초대 정리는 초대 런타임의 필수 정합성 경계로 현재 범위에 포함하며, 나머지 Phase 14 cleanup은 후속 범위다.

---

## 16. 문서 검토 결과

검토일: 2026-08-23

- [x] Backend #78·#79·#80·#81·#82·#83·#84와 Frontend #22·#23·#24·#25·#26의 완료 범위가 후속 단계와 구분되어 있다.
- [x] 승인 V1의 1·2·3단계 전달 완료, 남은 2단계와 저장소별 단계당 최대 1개 PR 계획이 구분되어 있다.
- [x] 가입 완료 판정과 미완료 회원 Friend 데이터 비생성·일회성 cleanup이 명시되어 있다.
- [x] ACTIVE 닉네임 예약·중복·탈퇴 후 재사용과 기존 중복 grandfathering이 명시되어 있다.
- [x] 검색 기본 true·1글자와 관계 상태 enum 계약이 명시되어 있다.
- [x] 친구 요청 거절 알림이 마지막 알림 단계 범위로 보존되어 있다.
- [x] 승인된 V1과 TODO가 구분되어 있다.
- [x] 친구 요청 30일 만료, 거절 cooldown·발송량 제한 없음이 반영되어 있다.
- [x] 시간표 PRIVATE, BUSY_ONLY, DETAILS와 친구별 예외·재공유 금지가 반영되어 있다.
- [x] TaxiParty 참가자 전원 초대와 수락 시 정원 동시성 검증이 반영되어 있다.
- [x] 공개 non-PARTY 채팅방만 초대하며 1:1·비공개 채팅은 제외되어 있다.
- [x] Minecraft SELF와 모든 FRIEND 계정, 최근 접속·온라인 상태 미노출이 반영되어 있다.
- [x] 차단이 소셜 기능에만 적용되고 공개 콘텐츠 전역 필터가 제외되어 있다.
- [x] 기존 Firebase UID 결합 회원 ID를 새 친구 API에 노출하지 않도록 friendPublicId 계약을 추가했다.
- [x] 친구 코드 preview와 명시적 친구 요청 생성을 분리했다.
- [x] 친구 시간표 요청·응답 학기와 초대 알림 target 계약을 명시했다.
- [x] 파티 초대 terminal 상태와 친구 쌍 공통 잠금 경계를 명시했다.
- [x] 친구·초대 알림 설정 필드, 기본값, 우선순위와 backfill을 명시했다.
- [x] 공개 식별자 field를 friendPublicId로 통일하고 차단 목록·해제 계약을 명시했다.
- [x] 공개방 정원 마감 초대의 EXPIRED·비복원 정책을 명시했다.
- [x] 즐겨찾기·시간표 override와 관계 삭제의 공통 pair lock을 명시했다.
- [x] 기존 프로필 완료 ACTIVE 회원 FriendProfile provisioning·충돌 재시도·모바일 노출 gate를 명시했다.
- [x] 모든 요청·초대 terminal 전이의 행 잠금과 고정 잠금 순서를 명시했다.
- [x] 친구 요청 만료가 모든 PENDING 의존 경로에서 active_pair_key 해제와 함께 반영된다.
- [x] 닉네임 검색 cursor·안정 정렬과 다중 초대 수신자별 부분 성공 계약을 명시했다.
- [x] RETIRED 친구 코드 영구 미재사용 registry와 ACTIVE 코드 참조를 명시했다.
- [x] 모든 Friend mutation·lazy provisioning이 잠금 후 Member ACTIVE를 재확인한다.
- [x] 초대 outcome의 invitationId 조건과 immutable expiryReason을 명시했다.
- [x] nicknameSearchable 조회 API와 PENDING 요청 cursor 목록 범위를 명시했다.
- [x] canonical Member 탈퇴 정책과 Phase 14 cleanup이 동기화 대상임을 명시했다.
- [x] 예정 API가 현재 운영 API와 구분되어 있다.
- [x] Foundation과 관계 Core의 실제 코드 구현 범위가 현재 런타임 상태로 전환되어 있다.

docs/domain-analysis.md와 docs/role-definition.md에는 Friend를 Supporting 런타임 도메인으로 표시하고 Foundation·관계 Core와 Phase 14 협력 책임을 구분한다. Foundation·관계 Core, 시간표 공유와 TaxiParty·공개방 초대의 런타임 엔티티·API는 docs/api-specification.md·docs/erd.md에 동기화했으며, Notification과 PENDING 초대 정리를 제외한 회원 탈퇴 cleanup은 해당 런타임 PR에서 실제 구현과 함께 현재형으로 전환한다.

---

## 17. 결정 기록

| 날짜 | 결정 |
| --- | --- |
| 2026-08-18 | 친구 코드, QR, 닉네임 검색을 V1에 포함하고 URL 딥링크는 TODO로 보류 |
| 2026-08-18 | 닉네임 검색은 opt-in, 2글자 이상 부분 일치, 페이지당 최대 20건의 opaque cursor 방식으로 확정 — 기본값·최소 길이는 2026-08-21 결정으로 대체 |
| 2026-08-18 | 친구 요청 30일 만료, 거절 cooldown과 발송량 제한 없음 |
| 2026-08-18 | 시간표 기본 PRIVATE, BUSY_ONLY·DETAILS와 친구별 예외, 재공유 금지 확정 |
| 2026-08-18 | 공통 공강은 야간 수업을 포함한 월~금 1~15교시, 같이 듣는 수업은 공식 courseId 기준 |
| 2026-08-18 | 택시 참가자 전원이 친구를 초대하며, 좌석은 예약하지 않고 수락 시 재검증 |
| 2026-08-18 | 공개 채팅방 초대만 지원하고 초대 만료는 7일 |
| 2026-08-18 | 친구의 Minecraft SELF와 모든 FRIEND 계정을 제공하되 최근 접속·온라인 상태는 숨김 |
| 2026-08-18 | 차단은 소셜 기능에 적용하고 공개 콘텐츠 전역 숨김은 제외 |
| 2026-08-18 | V1 관리자 친구 관계망 운영 UI는 추가하지 않음 |
| 2026-08-18 | 친구 코드 preview는 요청 생성과 분리하고 외부 공개 식별자는 friendPublicId로 통일 |
| 2026-08-18 | 친구 시간표는 선택한 semester를 필수로 요청하고 응답에도 같은 학기를 포함 |
| 2026-08-18 | 파티 비OPEN·정원 마감·관계 상실 초대는 EXPIRED이며 상태 회복 후에도 복원하지 않음 |
| 2026-08-18 | 친구·초대 알림은 friendAndInvitationNotifications 단일 설정, 기본 true, allNotifications 우선 |
| 2026-08-18 | badge는 받은 PENDING 요청·초대만 합산하고 보낸 요청은 제외 |
| 2026-08-18 | 정원이 찬 공개방 초대는 EXPIRED이며 자리 발생 후에도 복원하지 않음 |
| 2026-08-18 | Friend는 Supporting 유형의 런타임 도메인으로 Foundation·관계 Core를 구현하고, 기존 도메인 협력은 후속 범위로 분리 |
| 2026-08-18 | 기존 ACTIVE 회원은 batch backfill 후 누락 0건을 확인하고 멱등 lazy provisioning을 안전망으로 사용 — 대상 eligibility는 2026-08-21 결정으로 대체 |
| 2026-08-18 | 친구 요청·초대의 모든 terminal 전이는 상태 행 잠금과 고정된 상위 잠금 순서를 사용 |
| 2026-08-18 | 친구 요청 만료는 모든 PENDING 의존 경로에서 lazy reconciliation하고 만료 batch는 보조 수단으로 사용 |
| 2026-08-18 | 다중 초대는 수신자별 부분 성공이며 민감한 부적격 사유는 NOT_ELIGIBLE로 통합 |
| 2026-08-18 | 친구 코드는 단일 registry에서 ACTIVE·RETIRED로 관리하고 RETIRED 코드는 탈퇴 후에도 영구 미재사용 |
| 2026-08-18 | Friend mutation과 lazy provisioning은 Member 잠금 후 요청자·대상의 ACTIVE 상태를 다시 검증 |
| 2026-08-18 | batch 초대는 SENT와 본인 ALREADY_PENDING에만 invitationId를 제공하고 EXPIRED 사유는 immutable enum으로 저장 |
| 2026-08-18 | nicknameSearchable은 GET·PATCH로 서버 값을 제공하고 요청 목록은 PENDING 전용 20건 cursor 방식 |
| 2026-08-18 | 친구 코드 재발급은 24시간에 한 번으로 제한하고, 제한 중에는 `429`와 `Retry-After`로 다음 재시도 가능 시점을 전달 |
| 2026-08-21 | Backend #78·#79·#80과 Frontend #22·#23을 완료 이력으로 고정하고 후속 구현과 구분 |
| 2026-08-21 | 승인 V1은 Core 출시 준비, 친구 화면 완성, 시간표 공유, 친구 초대, 알림·나머지 탈퇴 정리의 5단계·저장소별 단계당 1개 PR로 진행 |
| 2026-08-21 | FriendProfile·최초 코드는 프로필 완료 ACTIVE 회원에게만 발급하고 backfill·lazy ensure도 같은 eligibility를 사용 |
| 2026-08-21 | 친구 FE 첫 배포 전에는 실제 사용 이력이 없는 테스트 데이터라는 전제에서 미완료 회원 FriendProfile·소유 ACTIVE 코드와 당시 모든 RETIRED 코드를 일회성 cleanup으로 삭제하고, 이후 RETIRED 코드는 영구 미재사용 유지 |
| 2026-08-21 | nicknameSearchable 기본값을 true로 변경하고 닉네임 검색은 1글자부터 허용 |
| 2026-08-21 | 프로필 완료 공백 판정을 Java `String.isBlank()`와 repository·운영 SQL에 동일하게 적용 |
| 2026-08-21 | 스쿠리 유저·운영자 포함 닉네임을 금지하고 신규·변경 닉네임은 ACTIVE 회원 사이에서만 고유하며 탈퇴 후 재사용 허용 |
| 2026-08-21 | 닉네임 중복 비교는 운영 MySQL `utf8mb4_unicode_ci` 규칙을 따라 대소문자·악센트 차이를 같은 닉네임으로 취급 |
| 2026-08-21 | 기존 중복 닉네임은 임의 변경하지 않고 새 닉네임 확정 시에만 unique claim을 요구 |
| 2026-08-21 | canSendFriendRequest 호환 field 없이 REQUESTABLE·INCOMING_PENDING·OUTGOING_PENDING·ALREADY_FRIEND 관계 상태로 교체 |
| 2026-08-21 | 친구 요청 거절 알림은 알림 단계에서 원 요청자에게 제공하고 FriendHub 요청 탭으로 이동 |
| 2026-08-21 | 각 구현 단계의 최종 PR 전에는 런타임·친구 명세·모바일 계획·OpenAPI·ERD·운영 문서를 대조하고 drift를 같은 PR에서 해소 |
| 2026-08-22 | Core 출시 준비(#81·#24)와 친구 화면 완성(#83·#25)을 완료 이력으로 고정하고, 다음 구현 단위를 시간표 공유로 전환 |
| 2026-08-22 | 시간표 공유 Backend·Frontend 구현과 문서 정합성 점검을 현재 PR 범위에서 완료하고, 친구 초대를 다음 승인 구현 단위로 전환 |
| 2026-08-22 | 가입 완료 판정은 ACTIVE와 비어 있지 않은 nickname·studentId·department만 사용하고, 예약어 검사는 신규·변경 닉네임 입력과 미완료 회원의 최초 완료 전환에만 적용 |
| 2026-08-23 | 프로필 미완료 회원은 예약어 닉네임을 변경하지 않은 부분 수정으로 최초 완료 상태가 될 수 없고, 이미 완료된 기존 예약어 닉네임 회원은 계속 허용 |
| 2026-08-23 | 시간표 공유 예외의 friend→owner 역방향 조회에 `(friend_member_id, owner_member_id)` 인덱스 사용 |
| 2026-08-23 | 시간표 공유 Backend #84·Frontend #26의 리뷰 보완과 문서 정합성 점검을 마쳐 3단계 전달 완료로 전환 |
| 2026-08-24 | 친구 초대 Backend #85·Frontend #27 전달 후 보완 단계에서 파티장 초대는 수락 즉시 참가, 일반 참가자 초대는 수락 후 동승 요청과 파티장 승인을 거치도록 확정 |
| 2026-08-24 | 가득 찬 파티는 모집 재개·새 동승 요청·초대 발송을 차단하고 남은 PENDING 동승 요청을 EXPIRED + CAPACITY_FULL로 종료하며, eligible 조회는 상태 목록과 canInvite=false를 정상 반환 |
| 2026-08-24 | 초대 시트는 초대 가능·초대 중·참여 중을 함께 표시하고, 파티원 목록은 참가자 전체에게 제공하되 강퇴는 파티장에게만 허용 |

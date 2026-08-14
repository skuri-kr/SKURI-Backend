# 코드 스타일 & 컨벤션

## 아키텍처
- Controller는 요청 검증과 응답 변환만 담당한다.
- Service가 비즈니스 규칙, 상태 전이, 권한 판단을 담당한다.
- 부가효과(알림, SSE fan-out, push)는 핵심 트랜잭션과 분리한다.
- SSE subscribe 메서드는 long-lived request 수명을 트랜잭션/JPA 세션 수명과 섞지 않는다. 초기 snapshot은 전용 read-only 서비스에서 DTO payload로 계산한 뒤 emitter를 생성/등록/전송한다.
- 마인크래프트 bridge는 앱용 STOMP와 분리된 전용 internal HTTP + SSE 채널로 유지한다. 플러그인 인증은 `X-Skuri-Minecraft-Secret` 헤더 기반이고, source of truth는 Firebase RTDB가 아니라 Spring/MySQL이다.
- 상태 변경 후 알림 발행은 `AfterCommitApplicationEventPublisher`로 after-commit semantics를 보장한다.
- 회원 프로필 사진 삭제는 PATCH의 null 의미를 바꾸지 않고 전용 `DELETE /v1/members/me/photo`에서 처리한다. DB의 `photo_url` null 반영을 우선하고, 내부 `PROFILE_IMAGE` storage 정리는 after-commit/best-effort로 분리한다.
- 프로필 `photoUrl`은 외부 URL이거나 현재 저장값 그대로인 경우를 제외하면, 반드시 본인이 업로드한 member-scoped `PROFILE_IMAGE` URL이어야 한다. 다른 회원의 내부 URL이나 legacy unscoped 내부 URL을 새 값으로 재사용하지 않는다.
- 회원 탈퇴는 hard delete 대신 tombstone(`status=WITHDRAWN`, `withdrawnAt`) + 개인정보 스크럽을 기본으로 한다.
- 탈퇴로 인한 외부 후처리(Firebase 삭제, SSE 연결 종료)는 핵심 트랜잭션 안에서 직접 처리하지 않고 after-commit 리스너로 분리한다.
- 같은 Firebase UID의 탈퇴 회원은 재활성화하지 않는다. `POST /v1/members`는 활성 회원에만 멱등이고, withdrawn UID에는 `WITHDRAWN_MEMBER_REJOIN_NOT_ALLOWED`를 반환한다.
- `NOT NULL` lifecycle 컬럼을 운영 DB에 추가할 때는 앱 기동 전에 수동 마이그레이션으로 legacy row를 먼저 채운다.
- Admin controller는 class-level `@AdminApiAccess`를 사용하고 raw `@PreAuthorize("hasRole('ADMIN')")`를 반복하지 않는다.
- Academic 도메인에서는 `Course`(공식 강의)와 `UserTimetableManualCourse`(직접 입력 강의)를 합치지 않는다. 공식 온라인 강의는 `Course.isOnline=true` + 빈 `schedules` + `location=null` 정규화를 사용하고, 직접 입력 온라인 강의와 동일하게 `slots[]` 및 시간 충돌 검사에서 제외한다.
- Campus 배너는 `app-notices`를 재사용하지 않고 별도 `domain/campus`로 관리한다. `displayOrder`는 생성/삭제/재정렬 후에도 1부터 시작하는 연속값을 유지하며, 순서 변경 계열 작업(create/delete/reorder)은 빈 테이블 첫 생성 경쟁까지 막기 위해 공통 lock으로 직렬화한다. PATCH는 누락 필드와 명시적 `null`을 구분한다.
- 학식 메뉴 category key는 reaction target id 안정성을 위해 영문/숫자/_/-만 허용한다. 학식 reaction upsert는 첫 저장 경쟁과 재시도/더블탭을 500 없이 처리하기 위해 주차 메뉴 row lock으로 직렬화한다.
- 상태 변경 Admin API는 `@AdminAudit`로 감사 대상을 선언하고, 감사 로직은 interceptor/filter 공통 계층에서 처리한다.
- `@AdminAudit`의 `targetId`와 snapshot lookup은 서비스가 쓰는 canonical 키(`semester=2026-1`, `platform=ios`) 기준으로 맞추고, request body 기반 값은 공통 request-body cache를 통해 preHandle 단계에서 복원한다.
- 감사 로그 실패는 warn 수준으로 남기고 비즈니스 API를 500으로 깨지 않게 best-effort로 처리한다.
- Support Admin 목록은 `AdminPageRequestPolicy` 기준 `page=0`, `size=20`, `size<=100`, 정렬 `createdAt,DESC` 규약을 유지한다.
- 이미지 업로드 context는 도메인별로 분리하고, Campus 배너 이미지는 `CAMPUS_BANNER_IMAGE` 관리자 전용 context + `campus-banners/YYYY/MM/DD` 경로를 사용한다.
- 일반 Chat 읽음 처리 외부 계약은 ISO 8601 UTC(`Instant`)로 유지하고, 내부 `ChatRoomMember.lastReadAt` 비교/저장은 `Asia/Seoul` 기준 `LocalDateTime`으로 정규화한다. unread의 source of truth는 서버 저장값이다.

## 댓글 익명 정책
- board/notice 댓글 익명 번호는 scope 내 작성자 기준으로 재사용하고, 내부 `anonId`는 길이 초과를 막기 위해 짧은 안정 해시를 사용한다.
- 댓글 수정 API에서 익명 여부를 바꿀 때는 `false -> true`만 scope aggregate lock을 추가로 잡아 순번 경쟁을 직렬화하고, `true -> false`는 `anonId`와 `anonymousOrder`를 함께 정리한다.

## 응답/예외
- 모든 REST 응답은 `ApiResponse<T>`를 사용한다.
- OpenAPI 2xx 성공 응답은 Scalar `Show schema`에서 `data`의 concrete type이 보이도록 raw `ApiResponse.class`만 두지 않는다. 필요하면 도메인별 OpenAPI 전용 wrapper schema를 사용한다.
- 예외는 `BusinessException + ErrorCode`로 표현하고 `GlobalExceptionHandler`에서 일관 처리한다.
- Admin 403 판별은 `ApiAccessDeniedErrorResolver`로 공통화하고 `/v1/admin/**`는 `ADMIN_REQUIRED`를 반환한다.
- Notification 전용 최소 ErrorCode는 `NOTIFICATION_NOT_FOUND`, `NOT_NOTIFICATION_OWNER`를 사용한다.

## 운영/환경변수
- 프로필 파일은 정책, `.env`/Secrets는 실제 값을 담당한다.
- `application.yaml`은 공통 설정과 datasource 공통 인증정보(username/password/driver-class), `application-local.yaml`/`application-local-emulator.yaml`/`application-prod.yaml`은 프로필별 `datasource.url`과 인증 정책을 담당한다.
- 공통 JPA 정책은 `spring.jpa.open-in-view=false`이며, 커넥션 진단 기본값은 `spring.datasource.hikari.connection-timeout=30000`, `spring.datasource.hikari.leak-detection-threshold=20000`이다. 환경별 override는 `DB_CONNECTION_TIMEOUT_MS`, `DB_LEAK_DETECTION_THRESHOLD_MS`를 사용한다.
- `application-local.yaml`, `application-local-emulator.yaml`은 민감값 없이 정책만 담고 Git으로 추적한다.
- 로컬 기본 프로필은 `local`, Firebase Emulator 검증은 `local-emulator`, 운영은 `prod`, 자동 테스트는 `test`를 사용한다.
- `local`은 실제 Firebase 기반 통합 테스트용이므로 `FIREBASE_PROJECT_ID`와 서비스 계정 파일 경로를 환경변수로 받아야 한다.
- `local-emulator`는 Firebase Auth Emulator 기반 백엔드 단독 테스트용이며 `FIREBASE_AUTH_EMULATOR_HOST`, `FIREBASE_PROJECT_ID`만 주로 사용하고 `FIREBASE_CREDENTIALS_PATH`, `GOOGLE_APPLICATION_CREDENTIALS`는 비워 두는 것을 기본으로 한다.
- IntelliJ 실행 설정의 환경 변수 칸에는 `.env` 파일 경로를 넣지 않고 `KEY=value` 형식만 입력한다. `local-emulator`에서 prod용 Firebase 경로가 섞이면 환경 가드가 즉시 실패하도록 유지한다.
- 두 로컬 프로필의 기본 DB는 `localhost:3306`이며, Docker MySQL(`3307`) 같은 다른 포트를 쓰고 싶으면 `DB_URL`로 덮어쓴다.
- `local-emulator`는 기본적으로 로컬 DB 스키마를 재생성하지 않도록 유지하고, 초기화가 필요하면 별도 DB를 사용한다.
- 기본 `docker-compose.yml`은 Firebase 자격증명 파일을 자동 마운트하지 않으므로, Docker에서 실제 Firebase 인증까지 검증하려면 별도 volume mount 또는 호스트 `bootRun`이 필요하다.
- Firebase 서비스 계정 JSON은 서버 파일로 보관하고 `GOOGLE_APPLICATION_CREDENTIALS` 경로만 주입한다.
- `prod`에서는 OpenAPI UI/JSON을 기본 비노출로 운영하고, health/info만 최소 공개한다.
- 운영 app는 `docker-compose.prod.yml`에서 `${APP_HOST_BIND:-127.0.0.1}:${APP_HOST_PORT:-8080}:8080` loopback 바인딩을 유지하고, Nginx만 `127.0.0.1:8080` 으로 프록시하도록 운영한다.
- 운영 MySQL에 관리자 도구로 접근해야 하면 `docker-compose.prod.yml`에서 `127.0.0.1:3307:3306` 같은 loopback 바인딩만 허용하고 SSH 터널을 통해 접속한다. 공용 바인딩은 사용하지 않는다.
- 브라우저 기반 관리자 페이지의 REST API CORS 허용 Origin은 `API_ALLOWED_ORIGIN_PATTERNS`로, WebSocket `/ws` Origin은 `CHAT_WS_ALLOWED_ORIGIN_PATTERNS`로 분리 관리한다.
- CD의 admin REST CORS smoke check는 `CD_SMOKE_CORS_ORIGIN`을 우선 사용하고, 비어 있으면 `API_ALLOWED_ORIGIN_PATTERNS`의 첫 번째 exact origin을 재사용한다.
- CD workflow는 `production-deploy` concurrency 그룹으로 최신 run만 유지하고 이전 run은 자동 취소한다.
- Firebase/RTDB 원본 이관은 외부 스크립트 대신 `infra/migration`의 스프링 1회성 배치 러너로 구현한다. 운영 앱과 분리해 `migration.enabled=true`, `spring.main.web-application-type=none`으로 별도 실행하고, 도메인 서비스 부가효과를 타지 않도록 `JdbcTemplate` 중심으로 적재한다.
- cutover migration은 `users`를 source of truth로 보고, users export에 없는 timetable과 live MySQL `courses`에 없는 학기의 timetable은 적재하지 않고 `timetable-skips.json`에 남긴다. 마인크래프트 계정은 `users[].minecraftAccount.accounts[]`를 canonical source로 사용하고 RTDB whitelist export는 검증/보강용으로만 사용한다.
- 관리자 회원 목록 API는 Support Admin 목록 규약과 동일하게 `AdminPageRequestPolicy`를 사용하고 필터는 `query/status/isAdmin/department`를 유지한다. 정렬은 `sortBy/sortDirection`으로 확장하되 기본값은 `joinedAt,DESC`, null 값은 항상 마지막이다. 이름 컬럼은 `members.realname`을 사용하고, `lastLoginOs`, `currentAppVersion`은 최근 활성 FCM 대표 토큰의 `fcm_tokens.platform`, `fcm_tokens.app_version`을 함께 사용한다.
- `POST /v1/members/me/fcm-tokens`의 `appVersion`은 optional이다. 신규 토큰 등록 시 미전송하면 `null`로 저장하고, 같은 token 재등록 시 `null` 또는 빈 문자열이면 기존 값을 유지한다.
- 회원/관리자 회원 검색/직접 입력 강의의 `department` 입력은 `DepartmentService.normalizeSupported`로 정규화하고, `departments.active = true`에 없는 값은 `VALIDATION_ERROR`로 처리한다.
- 관리자 회원 활동 요약 API는 ACTIVE 회원만 허용하고, 현재 저장된 post/comment/party/inquiry/report 데이터만 읽는 read-only orchestration으로 구현한다. 댓글 집계/최근 댓글은 삭제되지 않은 comment이면서 부모 post도 삭제되지 않은 경우만 포함한다. 탈퇴 회원은 `CONFLICT + MEMBER_ACTIVITY_NOT_AVAILABLE_FOR_WITHDRAWN`으로 막고, 과거 활동 복원/상태 변경 로직을 넣지 않는다.
- 회원 관리자 권한 변경은 Service에서만 판단하고, 탈퇴 회원은 `CONFLICT`, 자기 자신의 권한 변경은 `BAD_REQUEST + SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED`로 막는다.
- 관리자 회원 상세 응답은 `bankAccount`, `notificationSetting`을 유지하되, 관리자 권한 변경 감사는 `@AdminAudit` + 최소 member snapshot(`id/email/nickname/isAdmin/status`)만 before/after diff에 남긴다.
- 마지막 관리자 수 계산 정책은 코드/문서 근거가 생기기 전까지 임의 추가하지 않는다.


## Admin TaxiParty API 규칙
- 관리자 TaxiParty API도 class-level `@AdminApiAccess`와 상태 변경/운영 write용 `@AdminAudit`를 사용한다.
- 관리자 강제 상태 변경은 새 상태 머신을 만들지 않고 `Party.close/reopen/cancel/forceEnd` 같은 기존 엔티티 전이만 재사용한다. 허용 액션은 현재 `CLOSE`, `REOPEN`, `CANCEL`, `END` subset이며, 임의 상태 점프나 별도 override policy는 문서 근거 없이 추가하지 않는다.
- `END`는 현재 `ARRIVED -> ENDED`로만 허용되는 `forceEnd()`를 그대로 따른다. 운영 편의보다 도메인 불변식을 우선한다.
- 관리자 파티 목록은 `AdminPageRequestPolicy` 기준 `page=0`, `size=20`, `size<=100`을 유지하고 기본 정렬은 `departureTime,DESC` 후 `createdAt,DESC`다.
- 관리자 멤버 제거도 기존 aggregate 규칙을 재사용한다. leader 제거/리더 승계는 이번 범위에 포함하지 않고, `ARRIVED`, `ENDED` 상태에서는 제거를 허용하지 않는다.
- 관리자 시스템 메시지는 party chat room이 있어야 하고, leader/member를 사칭하지 않는다. 내부 source는 `ADMIN_SYSTEM`, 표시 기준은 `senderName=관리자`, `senderPhotoUrl=null`이다.
- 관리자 join request 조회는 현재 `PENDING`만 latest-first(`requestedAt DESC`)로 읽는다. 승인/거절 액션은 후속 범위다.
- 관리자 파티 상태 변경 감사 snapshot은 `id/status/endReason/settlementStatus/endedAt` 최소 필드만 저장하고, 멤버 제거/시스템 메시지도 각각 최소 snapshot(`partyId/memberId/isLeader/joinedAt`, `id/chatRoomId/senderId/senderName/type/source/text/createdAt`)만 남긴다.

## Admin Board API 규칙
- 관리자 Board API도 class-level `@AdminApiAccess`와 write API용 `@AdminAudit`를 사용한다.
- moderation 상태는 `VISIBLE`, `HIDDEN`, `DELETED`만 사용한다. 새 상태 머신을 만들지 않고 `isHidden` + 기존 soft delete(`isDeleted`) 조합으로 표현한다.
- 허용 전이는 게시글/댓글 모두 `VISIBLE -> HIDDEN`, `HIDDEN -> VISIBLE`, `VISIBLE/HIDDEN -> DELETED`만 지원한다. `DELETED`는 terminal로 두고 복구하지 않는다.
- hard delete는 추가하지 않는다. 게시글 `DELETED`는 기존 `Post.markDeleted()`, 댓글 `DELETED`는 기존 placeholder soft delete를 재사용한다.
- public board 조회는 `HIDDEN` 게시글을 제외한다. 댓글은 thread 구조 보존을 위해 `HIDDEN`도 public 응답에서 placeholder로 마스킹하고 write/like lookup에서는 active 대상에서 제외한다.
- 관리자 board 목록은 `AdminPageRequestPolicy` 기준 `page=0`, `size=20`, `size<=100`을 유지하고 기본 정렬은 게시글/댓글 모두 `createdAt,DESC`다.
- 관리자 moderation 감사 snapshot은 과도한 개인정보나 본문 전문을 남기지 않는다. 게시글은 `id/authorId/category/anonymous/hidden/deleted`, 댓글은 `id/postId/authorId/parentId/anonymous/hidden/deleted` 최소 필드만 저장한다.
- pin/공지 고정, 신고 연계 뷰, 작성자 제재, batch moderation은 문서 근거 전까지 board admin 범위에 섞지 않는다.


- 관리자 대시보드 API는 read-only 조회 모델로 유지한다. 상태 변경, sync 실행, 배치성 보정 로직을 끼워 넣지 않는다.
- 관리자 대시보드 집계/일자 버킷은 `Asia/Seoul`로 고정한다. `activity.days`는 `7 | 30`만 허용하고, `recent-items`는 Inquiry/Report/AppNotice/Party를 `createdAt DESC`로 병합한 결과만 노출한다.
- `summary.totalMembers`처럼 의미가 애매할 수 있는 KPI는 구현과 문서/PR에서 같은 정의를 사용한다. 현재는 `members` 전체 row 기준이며 tombstone(`WITHDRAWN`)을 포함한다.
- 관리자 대시보드 AppNotice source는 `publishedAt <= now`인 게시 공지로 한정한다. 학교 공지 sync 이력 같은 별도 계약이 없는 데이터를 임의로 섞지 않는다.

- 학사 일정 bulk sync는 새 상태 머신이나 `academicYear` 같은 스키마 컬럼을 추가하지 않고 기존 엔티티/테이블을 유지한 채 read/write model만 확장한다.
- bulk sync는 bulk create가 아니라 범위 sync semantics를 따른다. `scopeStartDate ~ scopeEndDate` 안에서는 자연키 `title + startDate + endDate + type` 기준으로 update/create/delete를 계산하고, scope 밖 일정은 건드리지 않는다.
- bulk API의 `type` 하위호환 정규화(`single|multi` -> enum 대문자)는 `PUT /v1/admin/academic-schedules/bulk`에만 적용하고 기존 단건 CRUD 계약은 그대로 유지한다.
- bulk sync 검증 실패(잘못된 scope, scope 밖 일정, 요청 내부 자연키 중복)는 새 CONFLICT 코드를 만들지 않고 기존 `VALIDATION_ERROR` + 422 흐름으로 처리한다.
- 관리자 학사 일정 bulk write 감사는 row별 전체 before/after diff보다 summary snapshot(`scopeStartDate`, `scopeEndDate`, `created`, `updated`, `deleted`)을 우선한다.


- 관리자 Chat read API는 새 도메인 모델을 만들지 않고 기존 `ChatService`/`ChatMessageRepository`/party-chat canonical id(`party:{partyId}`)를 재사용한다.
- 관리자 공개 채팅방 read 응답은 기존 DTO를 재사용할 수 있지만, 관리자 개인 상태 필드는 계약상 고정값(`joined=false`, `unreadCount=0`, `isMuted=false`, `lastReadAt=null`)으로 명시하고 OpenAPI/문서와 같이 유지한다.
- 관리자 read-only API(`GET /v1/admin/chat-rooms*`, `GET /v1/admin/parties/{partyId}/messages`)에는 `@AdminAudit`를 붙이지 않는다.
- 관리자 공개 채팅방 조회는 user API visibility 규칙을 그대로 재사용하지 않는다. `DEPARTMENT`도 관리자에게는 전체 공개방으로 노출하고, PARTY 메시지는 `/v1/admin/parties/{partyId}/messages`로 분리한다.

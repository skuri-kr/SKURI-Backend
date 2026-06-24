# 작업 완료 시 체크리스트

## 머지 전 필수 검증
1. `./gradlew build` 성공
2. 변경된 기능 관련 Contract/Service/Event/Security 테스트 수행 (이미지 업로드 변경이면 MIME/용량뿐 아니라 해상도/총 픽셀 제한 케이스도 포함)
3. API 정상/예외 케이스 최소 1개 이상 확인
3-1. 회원 프로필 사진 계약을 바꿨다면 `DELETE /v1/members/me/photo` 성공/회원없음과 `PATCH /v1/members/me`의 `photoUrl: null` no-op 유지, 본인 소유 member-scoped `PROFILE_IMAGE` 허용, 타인/legacy unscoped 내부 URL 거절, 외부 URL 분기, 원본+썸네일 정리, OpenAPI success allowlist 회귀를 함께 확인
4. `/v1/images`처럼 업로드/정적 리소스가 추가되면 공개 업로드 경로(GET)와 도메인 재사용 플로우(예: Board 저장)를 함께 확인
5. `ApiResponse` 에러 포맷 일관성 확인
6. OpenAPI example과 실제 `errorCode/message` 일치 확인
7. Scalar/Swagger의 success response `Show schema`에서 contract-critical `data` 필드가 concrete type으로 노출되는지 확인\n7-1. OpenAPI 문서화 전수 작업을 건드렸다면 `OpenApiSuccessSchemaCoverageIntegrationTest`와 `OpenApiUiAvailabilityIntegrationTest`를 함께 실행해 `/v3/api-docs` 기준 회귀를 확인\n8. OpenAPI/문서 동기화 확인 (`/v3/api-docs` 기준, `docs/api-specification.md`, lifecycle 정책 문서 포함)
8-1. shared 문서를 수정했다면 backend 문서와 함께 `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`, `/Users/jisung/SKTaxi/docs/spring-migration/erd.md`를 같은 작업에서 즉시 동기화하고, `role-definition.md`를 건드렸다면 frontend 사본도 함께 동기화한다.
8-2. Campus 배너 계약을 바꿨다면 공개 노출 조건(`isActive`, `displayStartAt`, `displayEndAt`), `displayOrder` 연속값 유지, `IN_APP`/`EXTERNAL_URL` action 정합성, `CAMPUS_BANNER_IMAGE` 컨텍스트 문서화를 같이 확인한다.
8-3. TaxiParty/Chat 계약을 바꿨다면 join accept/member leave/close/reopen SYSTEM 메시지, ARRIVED/END 서버 메시지, ACCOUNT/settlement snapshot payload 예시가 OpenAPI와 런타임 응답에서 일치하는지 확인
8-4. 공개 채팅방 계약을 바꿨다면 공식 공개방 seed 생성, joined/not joined summary 필드, 미참여 공개방 detail 허용 + messages 차단, 학과 변경 시 학과방 membership 제거를 함께 확인
8-5. 회원/공개방 정책을 바꿨다면 active member 없이 create/join이 가능한지, department alias 정규화와 unsupported department 422가 맞는지, seed가 multi-instance에서도 중복 실패 없이 올라가는지 확인
8-6. 일반 Chat 읽음 계약을 바꿨다면 `PATCH /v1/chat-rooms/{id}/read`가 JS `new Date().toISOString()` UTC 문자열을 그대로 받고, markAsRead 후 summary/detail 재조회에서도 unread가 복원되지 않는지 확인한다. shared 문서를 수정했다면 `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`를 같은 작업에서 즉시 동기화한다.
8-7. Academic 계약을 바꿨다면 공식 강의 `isOnline` 값이 `/v1/courses`와 `/v1/timetables/my`에 실제로 반영되는지, 공식 온라인 강의가 `courses[]`에는 보이지만 `slots[]`에는 없고 충돌 검사에서도 제외되는지, `POST /v1/admin/courses/bulk`에서 `isOnline=true + schedule=[]` 성공 / `isOnline=true + schedule 존재` 422 / `isOnline` 생략 하위호환을 함께 확인한다. backend 문서와 `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`, `/Users/jisung/SKTaxi/docs/spring-migration/domain-analysis.md`, `/Users/jisung/SKTaxi/docs/spring-migration/implementation-roadmap.md`, `/Users/jisung/skuri-admin/docs/implementation-plan.md`, `/Users/jisung/skuri-admin/docs/backend-api-gap.md`, `/Users/jisung/skuri-admin/README.md` 동기화를 확인한다.
9. 회원 라이프사이클 변경이면 탈퇴 후 접근 차단, 동일 UID 재가입 차단, 연관 도메인 정합성 회귀 확인
10. SSE/Auth long-lived 경로를 건드렸다면 subscribe 메서드가 트랜잭션을 오래 유지하지 않는지, `spring.jpa.open-in-view=false`가 공통 설정에 유지되는지, Firebase auth async 재디스패치에서 member 조회가 재실행되지 않는지 확인
11. 마인크래프트 bridge를 건드렸다면 `/internal/minecraft/**`, `/v1/minecraft/**`, `/v1/members/me/minecraft-accounts/**`, `/v1/sse/minecraft`의 200/4xx 계약, Minotar avatar 규칙, plugin outbound SSE 이벤트(`CHAT_FROM_APP`, `WHITELIST_SNAPSHOT`, `WHITELIST_UPSERT`, `WHITELIST_REMOVE`, `HEARTBEAT`)와 문서 동기화를 확인한다.
12. Serena Memory 동기화 확인
12. Admin 공통 변경이면 대표 Admin API에 대해 `401` / `403 ADMIN_REQUIRED` / 관리자 성공 시나리오를 확인한다.
13. 상태 변경 Admin API를 건드렸다면 `admin_audit_logs` row 생성과 `actor/target/diff` snapshot을 확인하고, `target_id`가 raw 입력이 아닌 canonical 키로 저장되는지 함께 확인한다.
14. Support Admin 목록 규약을 바꿨다면 `page/size` validation, `PageResponse`, 고정 정렬 문서 동기화를 함께 확인한다.
15. Inquiry 첨부 계약을 바꿨다면 `attachments` 생략/null -> 빈 배열 정규화, 최대 3개 제한, MIME 검증, `GET /v1/inquiries/my`와 Admin 문의 응답의 `attachments: []` 고정 규칙, `INQUIRY_IMAGE` context 문서화를 함께 확인한다.

16. board/notice 댓글 익명 정책을 건드렸다면 `false -> true`(기존 번호 재사용/신규 번호 부여), `true -> false`(`anonId`/`anonymousOrder` 정리), `isAnonymous` omission 시 기존 값 유지, OpenAPI request/response example 동기화를 함께 확인한다.

## 운영/배포 변경 시 추가 검증
1. `./gradlew build` 성공
2. `docker compose` 설정 파일 문법/기동 절차 확인
2-1. 로컬 Docker 이미지 빌드 컨텍스트에 `application-local.yaml`, `application-local-emulator.yaml`이 포함되는지 확인
3. `/actuator/health` 응답 확인
4. `docker-compose.prod.yml` 렌더링과 MySQL/Redis/media 영속 볼륨 정책 확인
5. 운영 app host 바인딩이 `127.0.0.1:<APP_HOST_PORT>` loopback 으로만 열리는지 확인
6. `GET /v1/app-versions/android` 같은 공개 API smoke check 확인
7. `prod`에서 OpenAPI가 기본 비노출인지 확인
8. 브라우저 관리자 페이지가 있으면 허용 Origin의 REST CORS preflight와 WebSocket Origin 설정을 함께 확인
9. 로컬 프로필 변경 시 `local`은 실제 Firebase 자격증명 경로가 필요한지, `local-emulator`는 자격증명 경로 없이도 실행되는지 함께 확인
10. 배포 전/후 체크리스트와 rollback 문서 동기화 확인
11. 운영 MySQL 접근 정책을 바꿨다면 host 바인딩이 `127.0.0.1` loopback 으로만 열리는지 확인
12. Phase 10 이전 운영 DB 업그레이드면 앱 기동 전에 `members.status` 수동 마이그레이션 SQL을 먼저 적용했는지 확인
13. 관리자 회원 API를 건드렸다면 `GET /v1/admin/members`의 `PageResponse + query/status/isAdmin/department + sortBy/sortDirection` 규약, 기본 정렬 `joinedAt,DESC`, null-last 정책, 이름 컬럼 `realname`, 대표 FCM 토큰 기준 `lastLoginOs/currentAppVersion`, `POST /v1/members/me/fcm-tokens`의 optional `appVersion` 저장/재등록 null 유지 정책을 함께 확인한다. `GET /v1/admin/members/{memberId}/activity`의 ACTIVE-only + current-data-only 정책, 삭제된 부모 게시글 댓글 제외 규칙, 비관리자 `403 ADMIN_REQUIRED`, 상세 응답의 `bankAccount`/`notificationSetting` 유지, 활동 요약의 count/recent list 정의, 권한 변경 성공/자기 자신 변경 `400 SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED`/탈퇴 회원 `409`, admin audit diff의 최소 snapshot(`id/email/nickname/isAdmin/status`)도 함께 확인한다.
14. 관리자 회원 정책은 activity summary에서 상태 변경/복원 로직을 추가하지 말고, 권한 변경도 self role change 금지만 기본으로 두며, 마지막 관리자 수 계산 같은 추가 제약은 docs/PR open question 없이 임의 확장하지 않는다.
15. 사용자 관리 placeholder를 닫는 작업이면 backend 문서뿐 아니라 `/Users/jisung/skuri-admin/docs/backend-api-gap.md`, `/Users/jisung/skuri-admin/docs/implementation-plan.md`, `/Users/jisung/skuri-admin/README.md`, 그리고 공유 계약 문서인 `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md` 동기화를 확인한다.


16. 관리자 TaxiParty API를 건드렸다면 `GET /v1/admin/parties`, `GET /v1/admin/parties/{partyId}`, `PATCH /v1/admin/parties/{partyId}/status`의 관리자 성공/비관리자 `403`/허용되지 않은 전이 `409`를 함께 확인한다.
17. 관리자 TaxiParty status action을 바꿨다면 허용 액션 subset(`CLOSE/REOPEN/CANCEL/END`)이 현재 엔티티 상태 머신과 정확히 일치하는지, 임의 상태 점프를 열지 않았는지 확인한다.
18. 관리자 TaxiParty status 변경을 건드렸다면 `admin_audit_logs` before/after snapshot이 최소 필드(`id/status/endReason/settlementStatus/endedAt`)만 담는지 확인한다.
19. 관리자 `/parties` placeholder를 닫는 작업이면 backend 문서뿐 아니라 `/Users/jisung/skuri-admin/docs/backend-api-gap.md`, `/Users/jisung/skuri-admin/docs/implementation-plan.md`, `/Users/jisung/skuri-admin/README.md`, `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`, `/Users/jisung/SKTaxi/docs/spring-migration/frontend-api-coverage.md`, `/Users/jisung/SKTaxi/docs/spring-migration/frontend-migration-status.md`, `/Users/jisung/SKTaxi/docs/spring-migration/user-screens-backend-api-gaps.md` 동기화를 확인한다.
20. 관리자 TaxiParty follow-up API를 건드렸다면 `DELETE /v1/admin/parties/{partyId}/members/{memberId}`, `POST /v1/admin/parties/{partyId}/messages/system`, `GET /v1/admin/parties/{partyId}/join-requests`의 관리자 성공/비관리자 `403`/도메인 `404|409|422`를 함께 확인한다.
21. 관리자 멤버 제거를 건드렸다면 leader 제거 금지, `ARRIVED`/`ENDED` 제거 금지, 채팅방 membership sync, leave 시스템 메시지, SSE `KICKED`, `PartyMemberKicked` notification event 재사용 여부를 확인한다.
22. 관리자 시스템 메시지를 건드렸다면 party chat room 없음 `404 CHAT_ROOM_NOT_FOUND`, blank/too-long validation `422`, `senderName=관리자`, `senderPhotoUrl=null`, admin audit chat-message snapshot을 함께 확인한다.
23. 관리자 join request 조회를 건드렸다면 `PENDING`만 latest-first(`requestedAt DESC`)로 내려가는지, 응답 필드가 현재 member 도메인 값(`nickname/realname/photoUrl/department/studentId`)만 사용하는지 확인한다.
24. 관리자 `/boards` placeholder를 닫는 작업이면 backend 문서뿐 아니라 `/Users/jisung/skuri-admin/docs/backend-api-gap.md`, `/Users/jisung/skuri-admin/docs/implementation-plan.md`, `/Users/jisung/skuri-admin/README.md`, `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`, `/Users/jisung/SKTaxi/docs/spring-migration/frontend-api-coverage.md`, `/Users/jisung/SKTaxi/docs/spring-migration/frontend-migration-status.md`, `/Users/jisung/SKTaxi/docs/spring-migration/user-screens-backend-api-gaps.md` 동기화를 확인한다.
25. 관리자 Board API를 건드렸다면 `GET /v1/admin/posts`, `GET /v1/admin/posts/{postId}`, `PATCH /v1/admin/posts/{postId}/moderation`, `GET /v1/admin/comments`, `PATCH /v1/admin/comments/{commentId}/moderation`의 관리자 성공/비관리자 `403`/도메인 `404|409|422`를 함께 확인한다.
26. 관리자 Board moderation을 건드렸다면 `VISIBLE/HIDDEN/DELETED` 전이만 허용되는지, `DELETED` 복구를 열지 않았는지, hard delete를 추가하지 않았는지 확인한다.
27. public board 규칙을 건드렸다면 `HIDDEN` 게시글이 public 목록/상세/내 게시글/북마크에서 제외되는지, `HIDDEN` 댓글이 public 댓글 응답에서 placeholder로 마스킹되는지, commentCount 증감이 active comment 기준과 일치하는지 확인한다.
28. 관리자 Board write API를 건드렸다면 `admin_audit_logs` row 생성과 before/after snapshot이 최소 필드만 담는지, 본문 전문/과도한 개인정보가 diff에 남지 않는지 확인한다.
29. Board OpenAPI를 건드렸다면 `/v3/api-docs`, `/swagger-ui/index.html`, `/scalar`에서 admin board 200/4xx example이 분리 노출되는지 함께 확인한다.


30. 관리자 `/dashboard` read-model을 건드렸다면 `GET /v1/admin/dashboard/summary`, `GET /v1/admin/dashboard/activity`, `GET /v1/admin/dashboard/recent-items`의 관리자 성공/비관리자 `403`/validation `422`를 함께 확인한다.
31. 관리자 대시보드 집계를 바꿨다면 `Asia/Seoul` 버킷, `summary.newMembersToday`의 `joinedAt` 기준, `summary.totalMembers`의 전체 row 기준, `recent-items`의 AppNotice source(`publishedAt <= now`)와 `createdAt DESC` 병합 정렬을 문서/테스트/PR에 같이 남긴다.
32. 관리자 `/dashboard` 작업이면 backend 문서뿐 아니라 `/Users/jisung/skuri-admin/docs/backend-api-gap.md`, `/Users/jisung/skuri-admin/docs/implementation-plan.md`, `/Users/jisung/skuri-admin/README.md`, `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`, `/Users/jisung/SKTaxi/docs/spring-migration/frontend-api-coverage.md`, `/Users/jisung/SKTaxi/docs/spring-migration/frontend-migration-status.md`, `/Users/jisung/SKTaxi/docs/spring-migration/user-screens-backend-api-gaps.md` 동기화를 확인한다.

33. 학사 일정 bulk sync를 건드렸다면 `PUT /v1/admin/academic-schedules/bulk`의 정상 sync, 비관리자 `403`, 잘못된 scope `422`, 자연키 중복 `422`, scope 밖 일정 `422`를 Contract 테스트와 수동 검증으로 함께 확인한다.
34. 학사 일정 bulk sync를 건드렸다면 동일 payload 재호출 시 `created=0, updated=0, deleted=0` idempotency가 유지되는지, 일부 항목 제거 시 `deleted` count가 증가하는지, 자연키 동일 항목의 `description/isPrimary`만 update 되는지 확인한다.
35. 학사 일정 bulk sync 문서화 작업이면 backend 문서뿐 아니라 `/Users/jisung/skuri-admin/docs/backend-api-gap.md`, `/Users/jisung/skuri-admin/docs/implementation-plan.md`, `/Users/jisung/skuri-admin/README.md`, `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`, `/Users/jisung/SKTaxi/docs/spring-migration/domain-analysis.md`, `/Users/jisung/SKTaxi/docs/spring-migration/implementation-roadmap.md`를 같은 작업에서 동기화한다.
36. 학사 일정 bulk sync OpenAPI를 건드렸다면 `/v3/api-docs`, `/swagger-ui/index.html`, `/scalar`에서 200/401/403/422 example이 분리 노출되는지, 자연키/범위/type normalize 설명이 계약 문서와 동일한지 확인한다.
37. 마이그레이션 러너를 건드렸다면 dry-run과 apply의 핵심 분기, report 파일 생성, 기존 row 보존 규칙(예: notice count 보존), 실행 명령 문서화를 함께 확인한다. Notice 썸네일 작업이면 `NOTICE_THUMBNAILS` plan의 keyset batch, `scanned/updated/no_image/failed` 집계, 네트워크 재크롤링 금지, 동일 실행 재호출 시 idempotent 동작을 같이 확인한다.
38. Notice 목록 경로를 건드렸다면 `/v1/notices`가 저장된 `thumbnail_url`만 사용하고, 목록 query에서 `body_html/body_text/attachments`를 select하지 않는 구조인지 테스트/리뷰로 확인한다.
38. cutover migration 러너를 건드렸다면 `member-rejects.json`, `timetable-rejects.json`, `minecraft-rejects.json`, `course-matches.json`, `timetable-skips.json` 생성과 unknown user/live MySQL 미관리 학기 timetable discard 정책, live MySQL `courses` lookup 매칭 규칙을 함께 확인한다.

39. 관리자 Chat read API를 건드렸다면 `GET /v1/admin/chat-rooms`, `GET /v1/admin/chat-rooms/{chatRoomId}`, `GET /v1/admin/chat-rooms/{chatRoomId}/members`, `GET /v1/admin/chat-rooms/{chatRoomId}/messages`, `GET /v1/admin/parties/{partyId}/messages`의 관리자 성공/비관리자 `403`/존재하지 않는 room|party `404`/cursor validation `422`를 Contract 테스트로 확인한다.
40. 관리자 공개 채팅방 조회를 바꿨다면 `DEPARTMENT` 포함 전체 public non-party 노출, 멤버 목록의 멤버십 정렬/회원 프로필 nullable 정책, membership 없는 메시지 조회, DTO 고정값(`joined=false`, `unreadCount=0`, `isMuted=false`, `lastReadAt=null`)과 OpenAPI/문서 일치를 함께 확인한다.
41. 관리자 Chat read 작업이면 backend 문서뿐 아니라 `/Users/jisung/skuri-admin/docs/backend-api-gap.md`, `/Users/jisung/skuri-admin/docs/implementation-plan.md`, `/Users/jisung/skuri-admin/README.md`, `/Users/jisung/SKTaxi/docs/spring-migration/api-specification.md`, `/Users/jisung/SKTaxi/docs/spring-migration/domain-analysis.md`, `/Users/jisung/SKTaxi/docs/spring-migration/implementation-roadmap.md` 동기화를 확인한다.

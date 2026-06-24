# 코드베이스 구조 상세

## 핵심 엔트리포인트
- `SkuriBackendApplication.java`
- 프로필 파일: `src/main/resources/application.yaml`, `application-local.yaml`, `application-local-emulator.yaml`, `application-prod.yaml`, `src/test/resources/application-test.yaml`
- 배포/운영: `Dockerfile`, `docker-compose.yml`, `docker-compose.prod.yml`, `.github/workflows/ci.yml`, `.github/workflows/cd.yml`
- 주요 문서: `docs/project-overview.md`, `docs/implementation-roadmap.md`, `docs/api-specification.md`, `docs/domain-analysis.md`, `docs/erd.md`, `docs/role-definition.md`

## 주요 패키지 메모
- `domain/taxiparty`: 파티 상태 전이, 정산 snapshot, taxi history/summary, 목록/SSE 요약 DTO가 `participantSummaries`로 현재 멤버 프로필 사진(`members.photo_url`)과 리더 여부를 함께 노출
- `domain/chat`: 공개/파티 채팅, SYSTEM 메시지, 읽음 처리, 공개방 seed, `ChatService`가 REST/STOMP `ChatMessageResponse.senderPhotoUrl`을 `members.photo_url`로 매핑
- `domain/board`: 게시글/댓글/좋아요/북마크, 이미지 연결
- `domain/notice`: 학교 공지, 공지 댓글, 북마크, 앱 공지. `Notice.thumbnailUrl` 저장 컬럼과 `NoticeSummaryProjection` 기반 `/v1/notices` 목록 경량화가 들어가 있으며, 목록 경로는 `bodyHtml/bodyText/attachments`를 select하지 않는다.
- `domain/academic`: 강의, 시간표, 학사 일정, 시간표 학기 목록/직접 입력 강의/온라인 강의 `isOnline` 계약; 공식 강의 `Course`도 `isOnline`을 가지며 bulk 업로드(`AdminBulkCourseRequest`)에서 온라인 강의를 받을 수 있다. `TimetableService`는 공식 온라인 강의와 직접 입력 온라인 강의를 모두 `slots[]`/충돌 검사에서 제외하고, `CourseService`는 온라인 공식 강의를 `schedule=[]`, `location=null`로 정규화한다.
- `domain/campus`: 캠퍼스 배너 공개/관리자 API
- `domain/support`: 문의, 신고(게시글/댓글/회원/채팅 메시지/일반 채팅방/택시파티), 앱 버전, 법적 문서, 학식; 학식은 `CafeteriaMenu.menu_entries` JSON 컬럼과 구조화 응답 `categories`, `menuEntries`를 함께 사용한다. `CafeteriaMenuService`가 저장 시 같은 주의 동일 `category + title` 메타데이터 일관성을 검증하고, `menus` 비교 시 빈 카테고리 생략을 `menuEntries` 빈 배열과 동치로 정규화한다. 실제 사용자 반응은 `CafeteriaMenuReaction`, `CafeteriaMenuReactionRepository`, `CafeteriaMenuReactionService`, `CafeteriaMenuReactionController`가 담당하며, stable weekly menu id(`weekId + category + title` 기반)와 `cafeteria_menu_reactions` 집계를 조회 응답 `likeCount`/`dislikeCount`/`myReaction`에 주입한다. 관리자 요청의 `likeCount`/`dislikeCount`는 deprecated 입력으로만 허용하고 저장 시 무시한다. `ReportService`가 board/chat/taxiparty 저장소를 조회해 `targetAuthorId`를 해석한다.
- `domain/notification`: 인앱 알림, unread count, SSE snapshot
- `domain/image` + `infra/storage`: 이미지 업로드/썸네일 생성과 provider별 storage 추상화. `ImageUploadService`는 `PROFILE_IMAGE`를 `profiles/{memberId}/YYYY/MM/DD/{uuid}.{ext}` member-scoped 경로로 저장하고, `ProfileImageStorageService`는 profile `photoUrl`이 현재 값 그대로인지/본인 소유 internal URL인지 검증하며 삭제 cleanup도 본인 소유로 검증된 scoped URL에만 원본 + `_thumb` 파일 정리를 수행한다. `StorageRepository.resolveRelativePath`를 `LocalStorageRepository`/`FirebaseStorageRepository`가 구현해 public URL을 storage key로 복원한다.
- `domain/minecraft`: 마인크래프트 계정/서버 상태/온라인 플레이어/bridge event, inbound event registry(`minecraft_inbound_events`), public API(`/v1/minecraft/**`, `/v1/members/me/minecraft-accounts/**`, `/v1/sse/minecraft`)와 plugin internal API(`/internal/minecraft/**`) 및 SSE fan-out/replay tie-breaker
- `infra/auth`: Firebase 인증 필터, Admin guard, Security 설정, Minecraft internal secret filter
- `infra/admin`: admin audit, admin list 공통 인프라
- `infra/openapi`: OpenAPI 그룹/예시/wrapper schema

## Inquiry Attachment 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/support/entity/InquiryAttachment.java`: 문의 첨부 이미지 메타데이터 value object
- `src/main/java/com/skuri/skuri_backend/domain/support/entity/converter/InquiryAttachmentListJsonConverter.java`: inquiry attachments JSON 컬럼 converter
- `src/main/java/com/skuri/skuri_backend/domain/support/dto/request/CreateInquiryAttachmentRequest.java`: 문의 생성용 첨부 메타데이터 요청 DTO
- `src/main/java/com/skuri/skuri_backend/domain/support/entity/Inquiry.java`: `attachments` JSON 컬럼 추가
- `src/main/java/com/skuri/skuri_backend/domain/support/service/InquiryService.java`: attachments null->[] 정규화, 최대 3개/MIME 검증, 사용자/관리자 응답 변환
- `src/main/java/com/skuri/skuri_backend/domain/image/dto/request/ImageUploadContext.java`: `INQUIRY_IMAGE` context 추가
- `src/test/java/com/skuri/skuri_backend/domain/support/controller/InquiryControllerContractTest.java`, `InquiryAdminControllerContractTest.java`: attachments 요청/응답 계약 검증
- `src/test/java/com/skuri/skuri_backend/domain/support/service/InquiryServiceTest.java`: attachments 정규화/검증 서비스 테스트
- `src/test/java/com/skuri/skuri_backend/domain/image/service/ImageUploadServiceTest.java`: `INQUIRY_IMAGE` 저장 경로 검증

## Legal Document 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/support/entity/LegalDocument.java`: 문서 본문, 배너, 푸터, 활성 여부를 저장하는 엔티티
- `src/main/java/com/skuri/skuri_backend/domain/support/controller/LegalDocumentController.java`: 공개 조회 API
- `src/main/java/com/skuri/skuri_backend/domain/support/controller/LegalDocumentAdminController.java`: 관리자 목록/상세/저장/삭제 API
- `src/main/java/com/skuri/skuri_backend/domain/support/service/LegalDocumentService.java`: documentKey 정규화, 공개/관리자 응답 변환, upsert/delete 비즈니스 규칙
- `src/main/java/com/skuri/skuri_backend/domain/support/service/LegalDocumentSeedMigration.java`: 초기 이용약관/개인정보 처리방침 1회성 seed 로직
- `src/main/java/com/skuri/skuri_backend/domain/support/repository/LegalDocumentRepository.java`: active 공개 조회와 전체 관리자 목록 조회
- `src/main/java/com/skuri/skuri_backend/common/seed/entity/SeedMigration.java`, `.../repository/SeedMigrationRepository.java`: seed 적용 이력 기록용 marker 저장소
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiLegalSchemas.java`, `OpenApiLegalExamples.java`: legal document API용 OpenAPI schema/example 정의

## Migration 관련 파일
- `src/main/java/com/skuri/skuri_backend/infra/migration`: 1회성 데이터 이관 공통 인프라 (`MigrationRunner`, `MigrationProperties`, 리포트/JSON/Timestamp 유틸)
- `src/main/java/com/skuri/skuri_backend/infra/migration/notice/NoticeMigrationJob.java`: Firestore notices JSON -> MySQL notices upsert 러너. 기존 공지 row는 카운터를 보존하고, 신규/갱신 row의 `thumbnail_url`은 `body_html` 첫 이미지 기준으로 함께 적재한다.
- `src/main/java/com/skuri/skuri_backend/infra/migration/notice/NoticeThumbnailMigrationJob.java`: 기존 notices row를 keyset(`id > lastId`) batch로 스캔해 `thumbnail_url`을 backfill하는 러너. dry-run/apply, `scanned/updated/no_image/failed` 집계를 지원한다.
- `src/main/java/com/skuri/skuri_backend/infra/migration/cutover`: 회원/linked account/FCM token/시간표/마인크래프트 계정을 함께 적재하는 cutover 러너. live MySQL `courses` lookup과 reject report(`member/timetable/minecraft-rejects.json`, `course-matches.json`), skip report(`timetable-skips.json`)를 생성한다.
- `src/test/java/com/skuri/skuri_backend/infra/migration/notice/NoticeMigrationJobDataJpaTest.java`: notice migration apply/dry-run 검증
- `src/test/java/com/skuri/skuri_backend/infra/migration/cutover/CutoverMigrationJobDataJpaTest.java`: cutover migration apply/dry-run 검증

## 테스트 포인트
- `src/test/java/com/skuri/skuri_backend/domain/support/controller/LegalDocumentControllerContractTest.java`
- `src/test/java/com/skuri/skuri_backend/domain/support/controller/LegalDocumentAdminControllerContractTest.java`
- `src/test/java/com/skuri/skuri_backend/domain/support/service/LegalDocumentServiceTest.java`
- `src/test/java/com/skuri/skuri_backend/domain/support/service/LegalDocumentSeedMigrationTest.java`
- `src/test/java/com/skuri/skuri_backend/infra/openapi/OpenApiSuccessSchemaCoverageIntegrationTest.java`

## Admin Member API 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/member/controller/MemberAdminController.java`: 관리자 회원 목록/상세/활동 요약/권한 변경 API와 OpenAPI 어노테이션, self role change `400 SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED` 계약 포함
- `src/main/java/com/skuri/skuri_backend/domain/member/service/MemberAdminService.java`: 회원 검색/필터/상세/활동 요약/권한 변경 비즈니스 로직과 자기 자신의 관리자 권한 변경 차단
- `src/main/java/com/skuri/skuri_backend/domain/member/dto/response/AdminMemberSummaryResponse.java`, `AdminMemberDetailResponse.java`, `AdminMemberActivityResponse.java`, `dto/request/UpdateMemberAdminRoleRequest.java`: 관리자 전용 DTO (`AdminMemberDetailResponse`는 `bankAccount`, `notificationSetting` 유지, `AdminMemberActivityResponse`는 count + recent lists)
- `src/main/java/com/skuri/skuri_backend/domain/member/constant/AdminMemberSortField.java`: 관리자 회원 목록 정렬 whitelist (`id`, `realname`, `email`, `nickname`, `department`, `studentId`, `joinedAt`, `lastLogin`, `lastLoginOs`, `currentAppVersion`)
- `src/main/java/com/skuri/skuri_backend/domain/member/repository/MemberRepository.java`, `MemberRepositoryImpl.java`, `AdminMemberSummaryProjection.java`: 관리자 회원 목록 custom native query, 최신 FCM 대표 토큰 기준 `lastLoginOs`/`currentAppVersion` 계산, null-last 정렬 지원
- `src/main/java/com/skuri/skuri_backend/domain/notification/entity/FcmToken.java`, `dto/request/RegisterFcmTokenRequest.java`, `service/FcmTokenService.java`, `controller/FcmTokenController.java`: `fcm_tokens.app_version` 저장, optional `appVersion` 등록/갱신 정책, 기존 토큰 재등록 시 null 유지 규칙
- `src/main/java/com/skuri/skuri_backend/infra/admin/audit/AdminAuditSnapshotFactory.java`: 관리자 권한 변경용 최소 member snapshot(`id/email/nickname/isAdmin/status`) 감사 로그 지원
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiMemberSchemas.java`, `OpenApiMemberExamples.java`, `OpenApiConfig.java`: 관리자 회원 API schema/example/group 반영
- `src/test/java/com/skuri/skuri_backend/domain/member/controller/MemberAdminControllerContractTest.java`, `service/MemberAdminServiceTest.java`, `repository/MemberRepositoryDataJpaTest.java`: 관리자 회원 목록 계약/서비스/정렬 repository 테스트
- `src/test/java/com/skuri/skuri_backend/infra/auth/AdminApiGuardIntegrationTest.java`, `infra/admin/audit/AdminAuditIntegrationTest.java`: admin guard/audit 회귀 검증 확장


## Admin TaxiParty API 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/taxiparty/controller/PartyAdminController.java`: 관리자 파티 목록/상세/상태 변경 + 멤버 제거 + 시스템 메시지 + pending join request 조회 API와 OpenAPI 어노테이션
- `src/main/java/com/skuri/skuri_backend/domain/taxiparty/service/TaxiPartyAdminService.java`: 관리자 검색/상세/허용 상태 전이 orchestration, 멤버 제거/시스템 메시지/대기 join request read model 정의
- `src/main/java/com/skuri/skuri_backend/domain/taxiparty/dto/request/CreateAdminPartySystemMessageRequest.java`, `dto/request/UpdateAdminPartyStatusRequest.java`, `dto/response/AdminPartyJoinRequestResponse.java`, `AdminPartySummaryResponse.java`, `AdminPartyDetailResponse.java`, `AdminPartyLeaderResponse.java`, `constant/AdminPartyStatusAction.java`: 관리자 follow-up DTO와 액션 enum
- `src/main/java/com/skuri/skuri_backend/domain/chat/service/ChatService.java`, `domain/chat/entity/ChatMessage.java`: 관리자 시스템 메시지 생성(`SYSTEM` + `ADMIN_SYSTEM` source), `senderPhotoUrl=null` 표시 규칙
- `src/main/java/com/skuri/skuri_backend/domain/taxiparty/repository/PartyRepository.java`, `JoinRequestRepository.java`: 관리자 목록 검색 query, pending join request count + 최신순 pending 목록 지원
- `src/main/java/com/skuri/skuri_backend/infra/admin/audit/AdminAuditSnapshotFactory.java`, `AdminAuditActions.java`, `AdminAuditTargetTypes.java`, `AdminAuditTargetKeyResolver.java`: 파티 상태 변경/멤버 제거/시스템 메시지 admin audit target/action/snapshot 지원
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiTaxiPartySchemas.java`, `OpenApiTaxiPartyExamples.java`, `OpenApiConfig.java`: 관리자 파티 follow-up API schema/example/group 반영
- `src/test/java/com/skuri/skuri_backend/domain/taxiparty/controller/PartyAdminControllerContractTest.java`, `service/TaxiPartyAdminServiceTest.java`: 관리자 파티 follow-up Contract/Service 테스트
- `src/test/java/com/skuri/skuri_backend/infra/admin/audit/AdminAuditIntegrationTest.java`, `infra/auth/AdminApiGuardIntegrationTest.java`, `infra/openapi/AdminOpenApiConventionTest.java`, `infra/openapi/OpenApiResponseExamplesConventionTest.java`, `infra/openapi/OpenApiSuccessSchemaCoverageIntegrationTest.java`: admin guard/audit/OpenAPI 회귀 검증 확장

## Admin Board API 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/board/controller/BoardAdminController.java`: 관리자 게시글/댓글 목록·상세·moderation API와 OpenAPI, `@AdminApiAccess`, `@AdminAudit` 선언
- `src/main/java/com/skuri/skuri_backend/domain/board/service/BoardAdminService.java`: 관리자 검색/상세/게시글·댓글 moderation 전이 orchestration, 기본 정렬 `createdAt DESC`
- `src/main/java/com/skuri/skuri_backend/domain/board/constant/BoardModerationStatus.java`, `dto/request/UpdateBoardModerationRequest.java`, `dto/response/AdminPostSummaryResponse.java`, `AdminPostDetailResponse.java`, `AdminCommentSummaryResponse.java`, `BoardModerationResponse.java`: 관리자 moderation enum/요청/응답 DTO
- `src/main/java/com/skuri/skuri_backend/domain/board/repository/PostRepository.java`, `CommentRepository.java`, `AdminPostSummaryProjection.java`, `AdminCommentSummaryProjection.java`: 관리자 목록 query, public hidden filter, admin update lock/read model 지원
- `src/main/java/com/skuri/skuri_backend/domain/board/entity/Post.java`, `Comment.java`: `isHidden` 플래그, 관리자 hide/unhide, soft delete 재사용 정책
- `src/main/java/com/skuri/skuri_backend/infra/admin/audit/AdminAuditActions.java`, `AdminAuditTargetTypes.java`, `AdminAuditSnapshotFactory.java`: 게시글/댓글 moderation 감사 로그 action/target/minimal snapshot 지원
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiBoardExamples.java`, `OpenApiBoardSchemas.java`, `OpenApiConfig.java`: board admin API example/wrapper schema/group 반영
- `src/test/java/com/skuri/skuri_backend/domain/board/controller/BoardAdminControllerContractTest.java`, `service/BoardAdminServiceTest.java`: 관리자 board Contract/Service 테스트
- `src/test/java/com/skuri/skuri_backend/infra/admin/audit/AdminAuditIntegrationTest.java`, `infra/auth/AdminApiGuardIntegrationTest.java`, `infra/openapi/AdminOpenApiConventionTest.java`, `infra/openapi/OpenApiResponseExamplesConventionTest.java`: admin board guard/audit/OpenAPI 회귀 검증


## Admin Dashboard API 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/admin/dashboard/controller/AdminDashboardController.java`: 관리자 대시보드 summary/activity/recent-items API와 OpenAPI 어노테이션
- `src/main/java/com/skuri/skuri_backend/domain/admin/dashboard/service/AdminDashboardService.java`: KPI 집계, Seoul 버킷 activity 시계열, recent-items 병합 정렬 read model
- `src/main/java/com/skuri/skuri_backend/domain/admin/dashboard/dto/response/AdminDashboardSummaryResponse.java`, `AdminDashboardActivityResponse.java`, `AdminDashboardRecentItemResponse.java`, `AdminDashboardRecentItemType.java`: 관리자 대시보드 응답 DTO
- `src/main/java/com/skuri/skuri_backend/domain/app/repository/AppNoticeRepository.java`, `domain/member/repository/MemberRepository.java`, `domain/support/repository/InquiryRepository.java`, `ReportRepository.java`, `domain/taxiparty/repository/PartyRepository.java`: dashboard 집계/최근 항목 조회용 count/query 메서드
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiDashboardExamples.java`, `OpenApiDashboardSchemas.java`, `OpenApiConfig.java`: dashboard OpenAPI schema/example/group 반영
- `src/test/java/com/skuri/skuri_backend/domain/admin/dashboard/controller/AdminDashboardControllerContractTest.java`, `service/AdminDashboardServiceTest.java`: dashboard Contract/Service 테스트
- `src/test/java/com/skuri/skuri_backend/infra/auth/AdminApiGuardIntegrationTest.java`, `infra/openapi/AdminOpenApiConventionTest.java`, `OpenApiResponseExamplesConventionTest.java`: dashboard admin guard/OpenAPI 회귀 확장


## Admin AcademicSchedule Bulk Sync 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/academic/controller/AcademicScheduleAdminController.java`: 단건 CRUD에 더해 `PUT /v1/admin/academic-schedules/bulk` 계약과 OpenAPI example/audit 선언을 포함한다.
- `src/main/java/com/skuri/skuri_backend/domain/academic/service/AcademicScheduleService.java`: 단건 정규화 규칙을 재사용하면서 bulk sync 자연키 매칭, 범위 검증, lowercase type 정규화, created/updated/deleted count 계산을 담당한다.
- `src/main/java/com/skuri/skuri_backend/domain/academic/dto/request/AdminBulkAcademicSchedulesRequest.java`, `AdminBulkAcademicScheduleItemRequest.java`, `dto/response/AdminBulkAcademicSchedulesResponse.java`: bulk sync request/response DTO.
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiAcademicExamples.java`, `OpenApiAcademicSchemas.java`: bulk sync success/validation examples와 wrapper schema.
- `src/test/java/com/skuri/skuri_backend/domain/academic/controller/AcademicScheduleAdminControllerContractTest.java`, `service/AcademicScheduleServiceDataJpaTest.java`, `infra/admin/audit/AdminAuditIntegrationTest.java`, `infra/auth/AdminApiGuardIntegrationTest.java`: bulk sync Contract/Service/Audit/Admin guard 회귀 검증.


## Admin Chat Read API 관련 파일
- `src/main/java/com/skuri/skuri_backend/domain/chat/controller/ChatAdminRoomController.java`: 관리자 공개 채팅방 목록/상세/멤버/메시지 조회 + 기존 생성/삭제 API를 함께 제공한다.
- `src/main/java/com/skuri/skuri_backend/domain/chat/service/ChatAdminService.java`, `ChatService.java`: 관리자 public non-party room 조회, 멤버십+회원 표시 정보 조회, membership 없는 메시지 page 조회를 기존 chat/member domain/service/repository로 재사용한다.
- `src/main/java/com/skuri/skuri_backend/domain/taxiparty/controller/PartyAdminController.java`, `service/TaxiPartyAdminService.java`: 관리자 파티 상세 메시지 조회를 `party:{partyId}` canonical room으로 위임한다.
- `src/main/java/com/skuri/skuri_backend/infra/openapi/OpenApiChatExamples.java`, `OpenApiTaxiPartyExamples.java`: 관리자 chat read API 예시를 관리한다.
- `src/test/java/com/skuri/skuri_backend/domain/chat/controller/ChatAdminRoomControllerContractTest.java`, `domain/chat/service/ChatServiceTest.java`, `domain/taxiparty/controller/PartyAdminControllerContractTest.java`, `domain/taxiparty/service/TaxiPartyAdminServiceTest.java`: 관리자 chat read contract/service 회귀를 담당한다.

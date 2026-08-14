# SKURI Backend - 프로젝트 개요

## 목적
성결대학교 학생 대상 택시 합승/커뮤니티/공지/학사 서비스 백엔드다. 핵심 도메인은 TaxiParty이며, 알림과 실시간 기능은 핵심 트랜잭션의 부가효과로 분리한다.

## 기술 스택
- Java 21, Spring Boot 4.0.3, Gradle
- MySQL 8.4 (prod), H2 (test)
- Spring Data JPA + Hibernate
- Firebase Admin SDK (ID Token 검증, FCM sender)
- 실시간: WebSocket(STOMP), SSE
- OpenAPI: springdoc Swagger UI + Scalar

## 도메인 구조
- member: 회원, 프로필, 알림 설정, FCM 토큰, 탈퇴/재가입 정책. `PATCH /v1/members/me`는 partial update semantics를 유지해 `photoUrl: null`을 no-op로 해석하고, 내부 storage URL은 현재 값 그대로이거나 본인이 업로드한 member-scoped `PROFILE_IMAGE` URL일 때만 허용한다. 명시적 삭제는 `DELETE /v1/members/me/photo`로 처리하며, storage cleanup은 본인 소유로 검증된 `profiles/{memberId}/...` 경로에만 after-commit/best-effort로 수행한다. legacy unscoped 내부 URL은 DB만 null 처리한다.
- taxiparty: 파티 생성/참여/정산/상태 전이, SSE, 택시 history/summary, 홈 목록 요약 `PartySummaryResponse.participantSummaries`로 현재 멤버 `id/photoUrl/nickname/isLeader`를 함께 제공
- chat: 공개 채팅방/파티 채팅방, SYSTEM/ARRIVED/END 서버 메시지, 공개방 seed, 메시지 payload `senderPhotoUrl`은 `members.photo_url`만 사용
- minecraft: `public:game:minecraft` 공개 채팅방, plugin 전용 internal bridge(`/internal/minecraft/**` + SSE), 앱/플러그인 간 양방향 채팅, 서버 상태, 온라인 플레이어 스냅샷, 화이트리스트/JE·BE 검증 정책을 Spring이 source of truth로 관리하며, plugin inbound `eventId` idempotency와 outbox replay tie-breaker(`minecraft_bridge_events.id`)를 서버가 보장
- board: 게시글/댓글/좋아요/북마크, 이미지 썸네일 규칙
- notice: 학교 공지/댓글/읽음/북마크/앱 공지. `notices.thumbnail_url`는 `TEXT` 캐시 컬럼이며, `http/https` 또는 기존 상대경로 이미지 위주로만 저장한다. `data:`/`blob:`/과도하게 긴 값은 `null` 처리하고, `/v1/notices`는 projection query로 필요한 컬럼만 읽는다. 기존 row는 `NOTICE_THUMBNAILS` migration plan으로 DB `body_html`만 읽어 backfill한다.
- academic: 강의/시간표/학사 일정, `GET /v1/timetables/my/semesters`, `GET /v1/courses/filter-options`, 직접 입력 강의(`UserTimetableManualCourse`), 공식 강의 `Course.isOnline`, 시간표 응답 `courses[] + slots[]` + `isOnline` 계약. 강의 필터 학과/학년/이수구분은 해당 학기 `courses` 값을 중복 제거해 제공하고 category 단축 표기는 bulk 저장 전에 장문 canonical 표기로 정규화한다. 직접 입력 학과는 `departments` master를 사용한다. 공식 온라인 강의는 직접 입력 온라인 강의와 같은 의미로 `slots[]`/충돌 검사에서 제외하지만 저장 모델은 분리 유지
- campus: 캠퍼스 배너 공개/관리자 API
- support: 문의/신고(게시글/댓글/회원/채팅 메시지/일반 채팅방/택시파티)/앱 버전/법적 문서/학식. 학식은 기존 `menus`와 함께 `categories`, `menuEntries`(stable weekly id, title, badges, 실제 `likeCount`/`dislikeCount`, `myReaction`)를 제공하며 가격은 계약에서 제외한다. 보조 태그는 관리자 입력 메타데이터로 저장하고, 관리자 요청의 `likeCount`/`dislikeCount`는 deprecated 입력으로 남기되 저장 시 무시한다. 같은 주 안의 동일 `category + title`은 날짜가 달라도 동일 메타데이터를 강제한다. 카테고리 코드는 메뉴 ID 안정성을 위해 영문/숫자/_/-만 허용한다. `menus` 검증 시 빈 카테고리 생략은 `menuEntries`의 빈 배열과 같은 의미로 본다. 사용자 반응은 `PUT /v1/cafeteria-menu-reactions/{menuId}` 한 개의 upsert API로 등록/전환/취소하며, source of truth는 `cafeteria_menu_reactions` row다. 반응 upsert는 같은 요청 재시도/더블탭에서도 500이 나지 않도록 주차 row lock으로 직렬화한다.
- notification: 인앱 인박스, FCM, SSE, 이벤트 기반 알림 처리

## Inquiry Attachment 메모
- 문의 첨부 이미지는 `POST /v1/images?context=INQUIRY_IMAGE` 업로드 결과 메타데이터를 `Inquiry.attachments` JSON 컬럼으로 저장한다.
- `POST /v1/inquiries`의 `attachments`는 optional이며 요청에서 생략하거나 `null`이면 서버에서 빈 배열로 정규화한다.
- 첨부는 최대 3개, 허용 MIME은 `image/jpeg`, `image/png`, `image/webp`다.
- `GET /v1/inquiries/my`, `GET /v1/admin/inquiries`/`PATCH /v1/admin/inquiries/{inquiryId}/status` 응답은 항상 `attachments: []` 형태를 유지하며 null을 반환하지 않는다.
- Inquiry의 기존 `adminMemo` 저장값은 사용자 공개 답변이다. 사용자용 `GET /v1/inquiries/my`는 이를 nullable `answer`로 제공하고, 관리자용 `memo` 입력도 문의 사용자에게 공개된다.
- 회원 탈퇴 후에도 문의 기록과 첨부 이미지 메타데이터는 보존하고, inquiry의 구조화 개인정보만 마스킹한다.

## Legal Document 메모
- `LegalDocument`는 `termsOfUse`, `privacyPolicy` 두 고정 키를 `legal_documents` 테이블에서 관리한다.
- 공개 API는 `GET /v1/legal-documents/{documentKey}`이며 `isActive=true` 문서만 조회 가능하다. 비활성 또는 미존재 문서는 `404 LEGAL_DOCUMENT_NOT_FOUND`를 반환한다.
- 관리자 API는 `GET /v1/admin/legal-documents`, `GET /v1/admin/legal-documents/{documentKey}`, `PUT /v1/admin/legal-documents/{documentKey}`, `DELETE /v1/admin/legal-documents/{documentKey}`다.
- 초기 이용약관/개인정보 처리방침 2건은 `seed_migrations` 기반 1회성 seed migration으로 적재한다. 이후에는 관리자 CRUD로만 수정한다.
- 문서 본문 구조는 프론트 계약과 맞춘 `banner`, `sections`, `footerLines`를 JSON 컬럼으로 저장한다.

## Anonymous Comment Policy
- board/notice 댓글의 `anonId`는 `{scopeId}:{userId}` 기반의 짧은 안정 해시(`ac:` prefix, 최대 35자)로 생성한다.
- 같은 게시글/공지 안에서 같은 사용자가 익명 댓글을 다시 작성하거나 `PATCH`로 `isAnonymous=true`로 전환하면 기존 `anonymousOrder`를 재사용하고, 없으면 `max(anonymousOrder)+1`을 부여한다.
- `PATCH /v1/comments/{commentId}`, `PATCH /v1/notice-comments/{commentId}`는 optional `isAnonymous`를 지원하며, `true -> false` 전환 시 `anonId`, `anonymousOrder`를 정리한다.

## 인프라/공통
- 공통 응답은 `ApiResponse`, 예외는 `GlobalExceptionHandler`, `ErrorCode` 중심으로 처리한다.
- 로컬 프로필은 `local`, `local-emulator`, 운영은 `prod`, 자동 테스트는 `test`를 사용한다.
- OpenAPI는 로컬에서 노출하고 `prod`에서는 기본 비노출이다.
- 상태 변경 이후 알림/외부 후처리는 after-commit semantics를 따른다.
- Admin API는 `@AdminApiAccess`, `ADMIN_REQUIRED`, `admin_audit_logs` 기반 정책을 사용한다.
- 별도 1회성 데이터 이관은 `infra/migration` 아래의 스프링 배치 러너로 수행한다. 평소 app 서버는 영향 없이 유지하고, `migration.enabled=true`와 `spring.main.web-application-type=none` 설정으로만 실행 후 종료한다.
- 이관 러너는 `plan=NOTICES`(Firestore notices import), `plan=NOTICE_THUMBNAILS`(기존 notices `thumbnail_url` backfill), `plan=CUTOVER`(회원/시간표/마인크래프트 컷오버)를 지원한다. 운영에서는 app 컨테이너와 분리된 1회성 실행을 기본으로 하고, 필요하면 `NOTICE_SYNC_SCHEDULER_ENABLED=false`로 공지 스케줄러를 잠시 끈 뒤 dry-run/apply를 순차 실행한다.

## Admin Member API 메모
- `member` 도메인은 관리자 백오피스 회원 관리 API를 제공한다: `GET /v1/admin/members`, `GET /v1/admin/members/{memberId}`, `GET /v1/admin/members/{memberId}/activity`, `PATCH /v1/admin/members/{memberId}/admin-role`.
- 관리자 목록은 `query/status/isAdmin/department` 필터와 `sortBy/sortDirection` 정렬을 지원한다. 기본값은 `joinedAt desc`, null 값은 항상 마지막이며 이름 컬럼은 `realname`을 사용한다. `lastLoginOs`, `currentAppVersion`은 최근 활성 FCM 토큰(`coalesce(last_used_at, created_at)` 최신)의 `fcm_tokens.platform`, `fcm_tokens.app_version`을 같은 대표 토큰 기준으로 사용한다.
- 학과 runtime source of truth는 `departments(name PK, active, displayOrder)`다. `GET /v1/departments`는 활성 학과를 반환하고, 회원/직접 입력 강의 검증과 공개 학과방 seed가 이를 사용한다. `members.department`, `chat_rooms.department`, `user_timetable_manual_courses.department`는 FK 적용 대상이며, 원본 자유 텍스트인 `courses.department`와 공지 발행부서 의미의 `notices.department`는 제외한다.
- `POST /v1/members/me/fcm-tokens`는 optional `appVersion`을 받는다. 신규 토큰 등록 시 미전송하면 `null`로 저장하고, 같은 토큰 재등록 시 `null` 또는 빈 문자열이면 기존 값을 유지한다.
- 관리자 상세는 목록 필드 외에 `photoUrl`, `withdrawnAt`, `bankAccount`, `notificationSetting`을 포함한다.
- 관리자 활동 요약은 ACTIVE 회원만 제공하며, 현재 저장된 post/comment/party/inquiry/report 데이터 기준 count + domain별 recent 5건 read model을 반환한다. 댓글은 삭제되지 않은 comment이면서 부모 post도 삭제되지 않은 경우만 포함하고, 탈퇴 회원은 `409 MEMBER_ACTIVITY_NOT_AVAILABLE_FOR_WITHDRAWN`으로 비제공 처리한다.
- 관리자 권한 변경은 기존 `members.is_admin` boolean만 갱신하며, 탈퇴 회원은 `409 CONFLICT`, 자기 자신의 권한 변경은 `400 SELF_ADMIN_ROLE_CHANGE_NOT_ALLOWED`로 차단한다. 마지막 관리자 수 계산 정책은 후속 결정 전까지 추가하지 않는다.
- 관리자 상세 응답은 `bankAccount`, `notificationSetting`을 유지하지만 admin-role 감사 snapshot은 `id/email/nickname/isAdmin/status` 최소 필드만 저장한다.

## Admin TaxiParty API 메모
- `taxiparty` 도메인은 관리자 백오피스용 TaxiParty API를 제공한다: `GET /v1/admin/parties`, `GET /v1/admin/parties/{partyId}`, `PATCH /v1/admin/parties/{partyId}/status`, `DELETE /v1/admin/parties/{partyId}/members/{memberId}`, `POST /v1/admin/parties/{partyId}/messages/system`, `GET /v1/admin/parties/{partyId}/join-requests`.
- 관리자 목록은 `page/size/status/departureDate/query` 필터를 지원하고, 기본 정렬은 `departureTime desc, createdAt desc`다. 검색 범위는 출발지/도착지/leader uid/leader nickname이다.
- 관리자 상세는 목록 필드 외에 `leader`, `members`, `pendingJoinRequestCount`, `settlementStatus`, `settlement`, `chatRoomId`, `createdAt/updatedAt/endedAt`를 제공한다. 현재 도메인에 없는 `gender`, `lastStatusChangedAt`는 억지로 만들지 않는다.
- 관리자 상태 변경 액션은 현재 상태 머신을 재사용하는 `CLOSE`, `REOPEN`, `CANCEL`, `END`만 허용한다. 임의 상태 점프는 허용하지 않는다.
- 관리자 멤버 제거는 일반 멤버만 허용하고 leader 제거는 `PARTY_LEADER_REMOVAL_NOT_ALLOWED`로 막는다. `ARRIVED`, `ENDED` 상태에서는 멤버 제거를 허용하지 않는다. 부수효과는 기존 `removeMember` 로직(채팅방 membership sync, leave 시스템 메시지, SSE `KICKED`, `PartyMemberKicked` notification event)을 재사용한다.
- 관리자 시스템 메시지는 party chat room이 있을 때만 생성한다. 내부적으로 `SYSTEM` + `ADMIN_SYSTEM` source를 사용해 leader/member 사칭을 피하고, 응답/표시 기준 `senderName`은 `관리자`, `senderPhotoUrl`은 `null`이다.
- 관리자 join request 조회는 현재 `PENDING`만 `requestedAt(createdAt) desc` 최신순으로 반환한다. 승인/거절 액션은 아직 제공하지 않는다.
- 관리자 write audit(status 변경, 멤버 제거, 시스템 메시지)는 `admin_audit_logs`에 최소 snapshot만 남긴다. 파티 상태 변경은 `id/status/endReason/settlementStatus/endedAt`, 멤버 제거는 `partyId/memberId/isLeader/joinedAt`, 시스템 메시지는 `id/chatRoomId/senderId/senderName/type/source/text/createdAt` 기준으로 기록한다.

## Admin Board API 메모
- `board` 도메인은 관리자 백오피스용 moderation API를 제공한다: `GET /v1/admin/posts`, `GET /v1/admin/posts/{postId}`, `PATCH /v1/admin/posts/{postId}/moderation`, `GET /v1/admin/comments`, `PATCH /v1/admin/comments/{commentId}/moderation`.
- 관리자 목록은 게시글/댓글 모두 `page/size`와 검색 필터를 지원하고 기본 정렬은 `createdAt desc`다. 게시글 필터는 `query/category/moderationStatus/authorId`, 댓글 필터는 `postId/query/moderationStatus/authorId`를 사용한다.
- moderation 상태는 `VISIBLE`, `HIDDEN`, `DELETED`만 지원한다. `HIDDEN`은 `isHidden=true`, `DELETED`는 기존 soft delete를 재사용한다.
- 허용 전이는 게시글/댓글 모두 `VISIBLE <-> HIDDEN`, `VISIBLE/HIDDEN -> DELETED`이고, `DELETED`는 복구하지 않는다. pin/고정 정책과 신고 연계 뷰는 후속 범위다.
- public board는 관리자 moderation을 반영한다. `HIDDEN` 게시글은 public 목록/상세/내 게시글/북마크에서 제외되고, `HIDDEN` 댓글은 thread 구조 유지를 위해 placeholder로 마스킹된다.
- 관리자 write audit은 `admin_audit_logs`에 최소 snapshot만 남긴다. 게시글은 `id/authorId/category/anonymous/hidden/deleted`, 댓글은 `id/postId/authorId/parentId/anonymous/hidden/deleted` 기준으로 기록한다.

## Admin Dashboard API 메모
- 관리자 대시보드 read-model API는 `GET /v1/admin/dashboard/summary`, `GET /v1/admin/dashboard/activity`, `GET /v1/admin/dashboard/recent-items`를 제공한다.
- 모든 집계와 일자 버킷 기준은 `Asia/Seoul`이다. `summary.newMembersToday`는 `members.joinedAt` 기준 오늘 `00:00 ~ generatedAt`, `activity.days`는 `7 | 30`만 허용한다.
- `summary.totalMembers`는 `members` 전체 row 기준이다. soft delete tombstone(`WITHDRAWN`)도 포함하며, ACTIVE-only count는 현재 계약에 포함하지 않는다.
- `recent-items` source는 Inquiry/Report/AppNotice/Party만 사용하고, AppNotice는 `publishedAt <= now`인 게시 공지만 포함한다. 학교 공지 sync 이력이나 운영 action API는 이 read model 범위에 포함하지 않는다.

## Admin AcademicSchedule API 메모
- `academic` 도메인은 기존 단건 CRUD(`POST /v1/admin/academic-schedules`, `PUT /v1/admin/academic-schedules/{scheduleId}`, `DELETE /v1/admin/academic-schedules/{scheduleId}`)를 유지하면서 `PUT /v1/admin/academic-schedules/bulk` 범위 bulk sync API를 추가했다.
- bulk sync는 `scopeStartDate ~ scopeEndDate` 안에 완전히 포함되는 기존 일정만 대상으로 하고, 자연키 `title + startDate + endDate + type` 기준으로 create/update/delete를 한 번에 계산한다.
- `description`, `isPrimary`는 자연키가 같은 일정의 변경 가능 필드다.
- bulk API의 `type`은 하위호환을 위해 `single|multi|SINGLE|MULTI` 입력을 모두 허용하고 서비스에서 대문자 enum으로 정규화한다.
- 관리자 write audit은 row별 상세 diff 대신 `scopeStartDate`, `scopeEndDate`, `created`, `updated`, `deleted` 요약 snapshot을 남긴다.


## Admin Chat Read API 메모
- `chat`/`taxiparty` 도메인은 관리자 read-only 채팅 조회 API를 제공한다: `GET /v1/admin/chat-rooms`, `GET /v1/admin/chat-rooms/{chatRoomId}`, `GET /v1/admin/chat-rooms/{chatRoomId}/members`, `GET /v1/admin/chat-rooms/{chatRoomId}/messages`, `GET /v1/admin/parties/{partyId}/messages`.
- 관리자 공개 채팅방 조회는 `isPublic=true && type!=PARTY`만 대상으로 하며, membership/학과 제한을 우회한다. 목록/상세 응답은 기존 chat DTO를 재사용하되 `joined=false`, `unreadCount=0`, `isMuted=false`, `lastReadAt=null` 고정값을 사용하고, 멤버 조회는 `chat_room_members`를 `joinedAt ASC, memberId ASC`로 정렬해 회원 표시 정보와 함께 내려준다.
- 관리자 파티 채팅 조회는 `party:{partyId}` canonical room 기준으로 기존 메시지 cursor pagination 계약을 그대로 재사용한다.
- 위 관리자 채팅 read API는 운영 조회 전용이므로 `admin_audit_logs` 적재 대상에 포함하지 않는다.

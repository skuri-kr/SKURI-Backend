# SKURI Taxi — 프로젝트 종합 문서

> 운영 규칙: 본 문서의 제품/아키텍처 내용이 변경되면 Serena Memory `project_overview`, `codebase_structure`도 함께 갱신한다.

---

## 1. 프로젝트 소개

**SKURI Taxi**는 성결대학교 학생을 위한 택시 동승 + 캠퍼스 라이프 통합 모바일 앱이다.

- 택시 동승: 파티 생성/참여, 모집 마감/재개, 도착, 정산, 파티 채팅
- 학교 공지: 크롤링 공지, 읽음/좋아요/북마크, 댓글
- 커뮤니티: 게시글/댓글/좋아요/이미지
- 채팅: 공개 채팅방 + 택시 파티 채팅
- 생활 정보: 학과 master, 강의 필터가 포함된 시간표, 학식, 학사 일정, 마인크래프트 정보

대상 사용자는 `@sungkyul.ac.kr` 이메일 계정을 가진 성결대학교 학생이다.

현재 앱 버전은 `v1.2.7` 기준으로 관리한다.

---

## 2. 현재 시스템 구성

### 프론트엔드

- React Native `0.79.2`
- React `19`
- TypeScript `5`
- React Navigation v7
- Reanimated / Gesture Handler / Gorhom Bottom Sheet
- React Native Maps, WebView, image picker/resizer
- Firebase Auth / Messaging / Analytics / Crashlytics client SDK 유지
- 도메인 데이터는 Spring REST + SSE + STOMP 기준으로 연결

프론트 코드는 기능 단위 폴더 구조를 사용한다.

- [src/app](/Users/jisung/SKTaxi/src/app): 부트스트랩, 네비게이션, 앱 레벨 provider
- [src/features](/Users/jisung/SKTaxi/src/features): auth, taxi, notice, chat, board, campus 등 도메인 기능
- [src/shared](/Users/jisung/SKTaxi/src/shared): 공용 UI, API, hooks, design system

### 백엔드

- Spring Boot `4.0.3`
- Spring MVC + Validation
- Spring Security
- Spring Data JPA / Hibernate
- MySQL
- STOMP/WebSocket, SSE
- OpenAPI / Swagger

백엔드는 MySQL을 핵심 도메인 데이터 저장소로 사용한다. Firebase는 보조 인프라로 남아 있다.

- Firebase Auth / Admin SDK: 토큰 검증 및 회원 인증 연동
- Firebase Cloud Messaging: 푸시 알림 발송
- StorageRepository: 이미지 저장 추상화
  - 기본 provider: LOCAL 파일 시스템
  - 선택 provider: FIREBASE
- 마인크래프트 연동: public/internal API, public SSE, bridge outbox까지 Spring 백엔드 기준으로 구현
- 1회성 Firebase -> MySQL 데이터 이관은 `infra/migration` 아래의 스프링 배치 러너로 수행하며, 운영 app 컨테이너와 분리해 `migration.enabled=true` + `spring.main.web-application-type=none` 설정으로 한 번 실행 후 종료한다. cutover 시간표 이관은 live MySQL `courses`에 없는 학기와 users export에 없는 `userId`를 reject가 아니라 discard 정책으로 건너뛰고 `timetable-skips.json`에 남긴다.

백엔드 패키지 구조는 도메인 중심이다.

- [domain/member](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/member): 회원 프로필과 DB 기반 canonical 학과 master (`GET /v1/departments`)
- [domain/taxiparty](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/taxiparty): 택시 파티, 동승 요청, 정산
- [domain/chat](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/chat): 공개 채팅 + 파티 채팅
- [domain/notice](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/notice): 학교 공지, 댓글, 북마크
- [domain/support](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/support): 문의/신고, 문의 첨부 이미지 메타데이터, 앱 버전, 법적 문서, 학식
- [domain/board](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/board): 커뮤니티 게시판
- [domain/share](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/share): 짧은 공유 코드, 앱 ID 해석, 제한된 공개 콘텐츠 미리보기
- [domain/notification](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/notification): 인앱 알림 + 푸시
- [domain/academic](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/domain/academic): 강의/시간표/학사 일정, 학기별 학과·학년·이수구분 필터 옵션과 category canonical 정규화. 공식 강의 학과는 `courses.department` 원본, 직접 입력 강의 학과는 `departments` master를 사용
- [infra](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/infra): auth, storage, openapi, admin 지원
- [infra/migration](/Users/jisung/skuri-backend/src/main/java/com/skuri/skuri_backend/infra/migration): Firestore/RTDB export를 읽어 MySQL로 적재하는 1회성 notice/cutover migration 러너. cutover 시간표는 live MySQL에 없는 학기/unknown user를 skip하고 `timetable-skips.json`, `course-matches.json`으로 추적한다.

---

## 3. 앱 구조

### 인증/진입 흐름

앱은 다음 순서로 진입한다.

1. 로그인 상태 확인
2. 프로필 완성 여부 확인
3. 권한 온보딩 여부 확인
4. 메인 탭 진입

### 메인 탭 구조

현재 메인 탭은 아래 네 개다.

- `CampusTab`: 홈, 프로필, 설정, 학교 생활 정보
- `TaxiTab`: 택시 파티 목록/생성/채팅/정산
- `NoticeTab`: 학교 공지
- `CommunityTab`: 공개 채팅 + 커뮤니티 게시판

프론트 네비게이션 진입점은 [MainNavigator.tsx](/Users/jisung/SKTaxi/src/app/navigation/MainNavigator.tsx) 이다.

### 주요 도메인 특징

#### 택시 파티

- 파티 상태: `OPEN`, `CLOSED`, `ARRIVED`, `ENDED`
- 리더/멤버 역할 분리
- 정원 도달은 참여·수락만 막고 모집 상태를 자동 전이하지 않으며, 모집 마감·재개는 리더 또는 관리자의 명시적 선택만 반영
- `CLOSED` 상태의 현재 참가자도 친구를 초대할 수 있고, 일반 공개 동승 요청만 차단
- `ARRIVED` 시점에 정산 스냅샷을 확정
- `ARRIVED` 이후 멤버가 나가더라도 정산 대상은 유지될 수 있음
- 파티 채팅은 별도 채팅방으로 운영

#### 공개 채팅

- 공개 채팅방 목록/상세/참여/나가기 지원
- 공개 채팅방 참여/나가기와 파티 채팅 멤버 입장/퇴장은 실제 `SYSTEM` chat message로 저장되고 STOMP topic으로 브로드캐스트됨
- 공개 채팅/파티 채팅 메시지 payload는 `senderName`과 함께 `senderPhotoUrl`을 내려준다.
- 일반 앱 메시지는 `members.photo_url`을 사용하고, 마인크래프트 origin 메시지는 Minotar URL을 사용한다.
- 공개 채팅과 택시 파티 채팅은 같은 채팅 도메인을 공유하되 계약은 분리되어 있음
- 내 `TEXT` 메시지는 전송 후 15분 안에 수정할 수 있으며, 삭제는 채팅 위치와 과거 읽기 흐름을 보존하는 tombstone 방식으로 처리한다.
- 메시지 수정·삭제는 전용 WebSocket 이벤트로 전달하고, 삭제된 채팅 이미지는 신고 증거와 활성 참조가 없을 때만 재시도 큐로 정리한다.

#### 친구

- 프로필 완료 ACTIVE 회원은 친구 코드·QR·닉네임 검색으로 친구 요청을 보낼 수 있다.
- 친구 요청·수락·거절, 즐겨찾기, 친구 끊기·차단, 시간표 공유·마인크래프트 projection을 제공한다.
- 현재 파티 참가자는 친구를 택시파티에 초대하고, 공개 non-PARTY 채팅방 참가자는 친구를 초대할 수 있다.
- 친구 요청·초대 알림은 `allNotifications && friendAndInvitationNotifications`를 만족할 때 인앱 인박스·SSE·FCM으로 전달한다.

#### 마인크래프트

- 공개 게임방 canonical id는 `public:game:minecraft`다.
- 마인크래프트 채팅은 일반 공개 채팅방으로 저장하되, 플러그인은 Spring internal API + SSE bridge로 연결한다.
- 서버 상태, 온라인 플레이어 목록, 화이트리스트, JE/BE 검증 규칙은 별도 `minecraft` 도메인으로 이관한다.
- 구현/운영 상세와 이관 배경은 [minecraft-spring-migration-plan.md](/Users/jisung/skuri-backend/docs/minecraft-spring-migration-plan.md) 기준으로 본다.

#### 공지/게시판

- 공지는 크롤링 데이터 기반
- 댓글, 좋아요, 북마크, 읽음 상태 제공
- 게시판은 이미지 업로드와 익명/실명 정책을 함께 지원
- 외부 공유는 `link.skuri.kr/{notice|board}/{8자리 코드}`를 사용하고, Share 도메인이 원본의 공개 가능 상태를 재확인한다.
- 웹 미리보기는 원문 전체가 아니라 공지의 제한된 텍스트·이미지·표 블록 또는 게시물의 익명 안전 텍스트 projection만 사용한다.

---

## 4. 실시간 처리와 알림

### 실시간 처리

- 택시 파티 상태/동승 요청: SSE
- 공개 채팅/파티 채팅: STOMP over WebSocket

중요 운영 원칙:

- SSE 연결은 오래 유지될 수 있지만 JDBC connection을 오래 점유하면 안 된다.
- 파티/알림/동승 요청 SSE는 snapshot 계산과 `SseEmitter` 수명을 분리해서 관리한다.

### 푸시/인앱 알림

푸시와 인앱 알림은 canonical `type + data` 계약을 사용한다.

- 파티 상태 변화: `PARTY_CLOSED`, `PARTY_REOPENED`, `PARTY_ARRIVED`, `PARTY_ENDED`
- 파티 채팅/공개 채팅: `CHAT_MESSAGE`
- 동승 요청: `JOIN_REQUEST_CREATED`, `JOIN_REQUEST_ACCEPTED` 등
- 친구·초대: `FRIEND_REQUEST`, `FRIEND_ACCEPTED`, `FRIEND_DECLINED`, `PARTY_INVITATION`, `CHAT_ROOM_INVITATION`

파티 상태 변화는 채팅 푸시가 아니라 `PARTY_*` 알림이 책임진다. 일반 채팅 메시지만 `CHAT_MESSAGE` 푸시를 사용하고, 일반 공개 채팅/파티 채팅의 멤버 입장/퇴장 `SYSTEM` 메시지는 히스토리/실시간 표시만 하고 push는 보내지 않는다. 단, 마인크래프트방 시스템 메시지는 별도 정책 문서에 따라 push 대상에 포함한다.

세부 계약은 [api-specification.md](/Users/jisung/skuri-backend/docs/api-specification.md) 를 기준으로 본다.

---

## 5. 데이터 원칙

### Source of Truth

- 핵심 도메인 데이터의 source of truth는 MySQL이다.
- 프론트는 REST + SSE/WebSocket 조합으로 서버 상태를 반영한다.

### 택시 정산

- `ARRIVED` 시 `taxiFare`, `perPersonAmount`, `splitMemberCount`, `settlementTargetMemberIds`를 확정한다.
- 이후 leave가 일어나더라도 정산 스냅샷은 재계산하지 않을 수 있다.
- 이 경우 정산 대상 항목은 `leftParty`, `leftAt`, `displayName` 같은 snapshot 정보를 유지한다.

### 채팅 읽음 처리

- 읽음 처리는 `lastReadAt` 단조 증가 정책을 따른다.
- 메시지 `createdAt`을 그대로 `lastReadAt`으로 round-trip 가능한 계약을 유지한다.

---

## 6. 참고 문서

상세 설계와 계약은 아래 문서를 source of truth로 사용한다.

- [api-specification.md](/Users/jisung/skuri-backend/docs/api-specification.md)
- [domain-analysis.md](/Users/jisung/skuri-backend/docs/domain-analysis.md)
- [implementation-roadmap.md](/Users/jisung/skuri-backend/docs/implementation-roadmap.md)
- [erd.md](/Users/jisung/skuri-backend/docs/erd.md)
- [minecraft-spring-migration-plan.md](/Users/jisung/skuri-backend/docs/minecraft-spring-migration-plan.md)
- [tech-strategy.md](/Users/jisung/skuri-backend/docs/tech-strategy.md)

프론트 레포의 [docs/spring-migration](/Users/jisung/SKTaxi/docs/spring-migration) 폴더에는 위 핵심 문서들의 동기화 복제본을 유지한다.

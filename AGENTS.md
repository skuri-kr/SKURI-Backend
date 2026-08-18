# AGENTS.md

## 문서 목적
이 문서는 `skuri-backend` 저장소에서 작업하는 에이전트/기여자가 동일한 기준으로 구현, 검증, 리뷰, 문서화를 수행하기 위한 운영 규칙이다.

## 프로젝트 기준 문서
작업 전 아래 문서를 최신 기준으로 확인한다.
- `docs/project-overview.md`
- `docs/implementation-roadmap.md`
- `docs/api-specification.md`
- `docs/domain-analysis.md`
- `docs/erd.md`
- `docs/role-definition.md`

## 기술 기준
- Language: Java 21
- Framework: Spring Boot 4.0.3
- Build: Gradle (`./gradlew`)
- Database: MySQL (JPA)

## 작업 범위 규칙
1. 모든 작업은 현재 요청된 목적(기능 추가, 버그 수정, 리팩터링, 운영 작업) 범위 안에서 수행한다.
2. 요청 범위를 벗어나는 구조 변경이나 의존성 추가는 사전 합의 없이 반영하지 않는다.
3. 설계 문서와 구현 의도가 충돌하면, 현재 합의된 요구사항을 우선 적용하고 근거를 남긴다.
4. 범위 외 개선 아이디어는 "제안 사항"으로 분리해 전달하고, 본 변경과 분리한다.
5. 범위 외 코드 수정이 불가피하면 별도 PR 분리를 우선하며, 같은 PR에 포함 시 근거/영향 범위를 PR 본문에 명시한다.

## 코드 아키텍처 규칙
1. 공통 응답 포맷은 `ApiResponse`를 사용한다.
2. 예외 처리는 `GlobalExceptionHandler`에서 일관되게 처리한다.
3. 비즈니스 규칙은 Service 레이어에서 판단한다.
4. Controller는 요청 검증/응답 변환 중심으로 유지한다.
5. 도메인 규칙(상태 전이, 권한)은 클라이언트가 아닌 서버에서 최종 판단한다.

## OpenAPI 문서화 규칙
1. 신규/수정 API를 구현할 때 Springdoc OpenAPI 어노테이션을 함께 반영한다.
2. Controller 단위에 `@Tag`, 엔드포인트 단위에 `@Operation`을 작성한다.
3. 상태코드별 응답은 `@ApiResponses`/`@ApiResponse`로 명시한다.
4. 요청/응답 DTO에는 `@Schema`(description/example/nullable)를 작성해 스키마를 명확히 한다.
5. 인증 요구사항은 `@SecurityRequirement`로 명시한다. (공개 API는 보안 요구를 비운다)
6. 새 도메인 API가 추가되면 `GroupedOpenApi` 그룹 설정도 함께 갱신한다.
7. 머지 전 `/v3/api-docs`, `/swagger-ui/index.html`, `/scalar` 노출 여부를 확인한다.
8. REST API는 **선언한 모든 responseCode**에 대해 `content` + `examples`를 작성한다.
9. `@ApiResponse`에 description만 두고 예시를 생략하지 않는다. (Scalar/Swagger 예시 혼동 방지)
10. 공통 에러(401/403/404/409/422/500) 예시는 상수/공통 정의를 재사용해 일관성을 유지한다.
11. 예외: `204 No Content`, `text/event-stream`(SSE)은 API 특성에 맞는 예시를 별도로 명시한다.
11-1. SSE `200` 예시는 단일 예시 1개만 두지 말고, 최소 `stream_full` + 이벤트별(`SNAPSHOT`, `HEARTBEAT`, 도메인 이벤트) `@ExampleObject`를 함께 선언한다.
11-2. SSE 이벤트별 예시는 실제 `event name`/`data payload` 스키마와 1:1로 맞춘다. (`SseEmitter.event().name(...).data(...)` 기준)
12. 하나의 상태코드에서 여러 비즈니스 에러코드가 발생 가능하면 `@ExampleObject`를 복수로 선언해 **에러코드별 예시를 분리**한다.
13. `CONFLICT/NOT_FOUND/FORBIDDEN` 같은 포괄 예시는 최소화하고, 가능한 경우 도메인별 실제 `errorCode/message` 예시를 우선한다.
14. OpenAPI 예시 상수는 단일 대형 파일로 두지 않고 도메인별로 분리한다.
   - 예: `OpenApiCommonExamples`, `OpenApiMemberExamples`, `OpenApiTaxiPartyExamples`
15. `BusinessException`에서 커스텀 메시지를 사용하는 경우, OpenAPI 예시 메시지도 런타임 메시지와 동일하게 맞춘다.
16. Service/Entity 예외 규칙 또는 `ErrorCode`가 변경되면 OpenAPI 예시 상수와 Controller `@ApiResponses`를 같은 PR에서 동기화한다.
17. 머지 전 Scalar에서 최소 1개 API를 선택해 200/4xx 탭 예시가 서로 다르게 노출되는지 확인한다.
18. API 계약의 런타임 기준은 `/v3/api-docs`로 고정하며, `docs/api-specification.md`는 같은 PR에서 반드시 동기화한다.

## 브랜치 규칙
1. `main`은 항상 안정 상태로 유지하고 직접 작업/커밋하지 않는다.
2. 기능 1개 또는 버그 1개당 브랜치 1개로 작업한다.
3. 한 브랜치에는 하나의 작업 목적만 담는다.
4. 브랜치명은 아래 형식을 사용한다.
   - 기능: `feat/<topic>`
   - 버그: `fix/<topic>`
   - 문서: `docs/<topic>`
   - 기타: `chore/<topic>`

## 커밋 규칙
1. Conventional Commits를 사용한다.
2. 타입은 영어, 본문(설명)은 한국어로 작성한다.
3. 권장 타입: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`.
4. 예시: `fix: 404 예외를 ApiResponse NOT_FOUND로 매핑`

## PR/머지 규칙
1. 모든 변경은 작업 브랜치에서 완료한 뒤 PR로 `main`에 머지한다.
2. PR 없이 `main`에 직접 머지하지 않는다.
3. 머지 방식은 `Squash merge`를 기본으로 사용한다.
4. 머지 후 작업 브랜치는 삭제한다.

## 작업 순서 (Git 기본 플로우)
1. `main` 최신화
   - `git checkout main`
   - `git pull origin main`
2. 작업 브랜치 생성
   - `git checkout -b feat/<topic>` (상황에 맞게 `fix/docs/chore`)
3. 작업 후 커밋
   - `git add .`
   - `git commit -m "feat: ..."`
4. 원격 푸시
   - `git push -u origin <branch-name>`
5. GitHub에서 PR 생성 후 머지
6. 로컬 브랜치 정리
   - `git checkout main`
   - `git pull origin main`
   - `git branch -d <branch-name>`

## GitHub 설정 권장
1. `main` 브랜치 보호 활성화
2. `Require a pull request before merging` 활성화
3. `Require status checks to pass` 활성화 (`./gradlew build`)
4. `Squash merge` 사용
5. 머지 후 브랜치 자동 삭제 활성화

## AI 에이전트 협업 규칙
1. 브랜치는 에이전트 기준이 아니라 기능/버그 기준으로 나눈다.
2. 같은 브랜치에서 여러 기능을 동시에 진행하지 않는다.
3. 작업 결과는 반드시 PR 단위로 검토 후 머지한다.

## 문서 동기화 규칙
아래 변경이 있으면 문서를 함께 수정한다.
1. API 계약 변경: `docs/api-specification.md`
2. 엔티티/관계/인덱스 변경: `docs/erd.md`
3. 도메인 책임/경계 변경: `docs/domain-analysis.md`, `docs/role-definition.md`
4. 구현 계획/운영 정책 변경: 관련 계획 문서(예: `docs/implementation-roadmap.md`)

## Serena Memory 동기화 규칙
1. Serena 온보딩 메모리(`.serena/memories/*.md`)는 `.gitignore`에 포함된 로컬 도구 상태다. Git에 강제로 추가하지 않는다.
2. 코드/기능/정책 변경과 같은 작업 단위에서 관련 로컬 메모리를 함께 갱신하고, PR 설명에 변경한 메모리 이름과 요약을 남긴다.
3. 최소 점검 대상 메모리:
   - `project_overview`, `codebase_structure`, `code_style_and_conventions`, `suggested_commands`, `task_completion_checklist`
4. 변경 유형별 동기화 기준:
   - 아키텍처/도메인 구조 변경: `project_overview`, `codebase_structure`
   - 코딩 규칙/리뷰 기준/운영 정책 변경: `code_style_and_conventions`, `task_completion_checklist`
   - 실행/빌드/테스트/운영 명령 변경: `suggested_commands`
5. 동기화 절차:
   - Serena 도구를 사용할 수 있으면 기존 메모리를 읽고 편집한다.
   - Serena 도구를 사용할 수 없으면 로컬 `.serena/memories/*.md`를 직접 확인하고 갱신한다.
   - PR 설명에 "Serena Memory 동기화 내역"을 포함하되, ignored 파일을 PR diff에 포함시키지 않는다.
6. 작업 완료 전 로컬 메모리 파일 목록과 변경 대상 내용을 확인해 누락 여부를 점검한다.

## 로컬 실행/테스트 환경 규칙
1. 에이전트가 로컬에서 서버/테스트를 실행할 때 기본 프로필은 `local`로 간주한다.
2. `local` 프로필은 "프론트 + 백엔드"를 함께 붙여 실제 Firebase ID Token 흐름을 확인하는 로컬 통합 테스트용이다.
3. `local-emulator` 프로필은 "백엔드만" 실행하고 Firebase Auth Emulator가 발급한 토큰으로 인증 흐름을 빠르게 검증하는 용도다.
4. `src/main/resources/application-local.yaml`을 사용한다면 아래 명령으로 실행한다.
   - 서버 실행: `SPRING_PROFILES_ACTIVE=local ./gradlew bootRun`
   - 테스트 실행: `SPRING_PROFILES_ACTIVE=local ./gradlew test`
5. `application-local.yaml` 없이 실행해야 하는 경우, 아래 환경변수를 먼저 설정한다.
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
6. `local` 프로필은 실제 Firebase를 사용하므로 `FIREBASE_PROJECT_ID`, `FIREBASE_CREDENTIALS_PATH`(또는 `GOOGLE_APPLICATION_CREDENTIALS`)가 필요하다.
7. `local-emulator` 프로필은 `FIREBASE_AUTH_EMULATOR_HOST`, `FIREBASE_PROJECT_ID`가 필요하며, `FIREBASE_CREDENTIALS_PATH`, `GOOGLE_APPLICATION_CREDENTIALS`는 비워 두는 것을 기본으로 한다.
8. 로컬 DB 기본값은 `localhost:3306`이다. 다만 Docker MySQL을 다른 포트(예: `3307`)로 띄웠다면 `DB_URL` 환경변수로 덮어쓴다.
9. 로컬 실행 전제 조건:
   - 로컬 MySQL 서버가 기동 중이거나, Docker MySQL/Redis가 기동 중일 것
   - 대상 DB(예: `skuri`)가 생성되어 있을 것
10. CI와 로컬 환경은 분리한다.
   - CI는 `CI_DB_*` GitHub Secrets 사용
   - 로컬 DB 계정/비밀번호를 CI 값으로 재사용하지 않음
11. 민감 정보는 코드/문서/커밋에 직접 작성하지 않는다.
   - `application-local.yaml`, `application-local-emulator.yaml`은 실행 정책만 담고 Git으로 추적한다.
   - 실제 비밀값은 `.env`, 서버 환경변수, 로컬 파일로 분리하고 커밋하지 않는다.

## 검증 기준 (머지 전 최소)
1. `./gradlew build` 성공
2. 변경된 기능과 직접 관련된 테스트/검증 수행
3. API 동작 변경 시 정상/예외 케이스를 최소 1개 이상 확인
4. 공통 에러 포맷(`ApiResponse`)이 깨지지 않는지 확인
5. OpenAPI 상태코드별 example이 실제 응답(`errorCode/message`)과 불일치하지 않는지 확인

## 테스트 작성 규칙 (필수)
1. 신규/수정 API마다 Contract 테스트를 작성한다.
   - 최소 케이스: `정상 1개 + 인증/권한 1개 + 검증/비즈니스 예외 1개`
2. 상태 전이/권한/정산/동시성 규칙을 변경하면 Service 테스트를 함께 작성한다.
   - 최소 케이스: `성공 1개 + 실패 1개(상태 위반 또는 권한 위반)`
3. 응답 스키마를 변경하면 필드 존재/미노출(예: nullable, 분기 응답) Contract 검증을 추가한다.
4. PR 설명에는 "이번 변경으로 추가/수정된 테스트 목록"을 반드시 포함한다.

## 리뷰 체크리스트
1. 요청 범위를 벗어난 코드가 없는가
2. 상태 머신/권한/인증 규칙이 서버에서 강제되는가
3. 예외가 적절한 HTTP 상태와 `errorCode`로 매핑되는가
4. 응답 스키마가 문서와 일치하는가
5. 변경에 필요한 문서 업데이트가 포함됐는가

## 금지 사항
1. 승인 없는 파괴적 명령(`git reset --hard`, 대량 파일 삭제) 실행 금지
2. 근거 없는 대규모 리팩터링 금지
3. 문서/코드 불일치를 의도적으로 방치하는 커밋 금지

## 운영 메모
- 인증은 Firebase ID Token 검증 기반으로 확장한다.
- 실시간 통신은 채팅(WebSocket)과 이벤트(SSE)를 분리한다.
- WebSocket은 CONNECT 인증뿐 아니라 SEND/SUBSCRIBE 목적지별 인가를 서버에서 강제한다.
- WebSocket CORS는 `*` 허용을 금지하고 프로필/환경별 허용 Origin만 설정한다.
- 핵심 도메인 우선순위는 `TaxiParty`이며, 상태 전이/동시성/정산 규칙을 가장 엄격히 검증한다.

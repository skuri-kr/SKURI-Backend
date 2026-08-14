# SKURI Phase 9 배포 / 운영 가이드

## 1. 확정 전략

| 항목 | 결정 |
|------|------|
| 로컬 실행 | 평소 개발은 호스트 앱(`bootRun`/IDE) + `docker-compose.yml`의 `MySQL + Redis` 조합을 기본으로 사용 |
| 운영 구조 | `OCI Compute 1대`에서 `docker-compose.prod.yml`로 `app + MySQL + Redis` 기동 |
| Redis 운영 반영 | 현재 앱 로직에는 미연결이지만 단일 인스턴스 운영 compose에 포함 |
| 프로필 | `application / local / local-emulator / prod / test` |
| CD 방식 | 반자동. `main` 반영 후 GitHub `production` 환경 승인 시 멀티플랫폼 이미지로 배포하고, 새 run이 시작되면 이전 run은 자동 취소 |
| OpenAPI 노출 | `local/local-emulator` 노출, `prod` 기본 비노출 |
| Firebase 자격증명 | 서버 파일 + `GOOGLE_APPLICATION_CREDENTIALS` 경로 주입 |

## 2. 환경변수 원칙

- 로컬 개발: `.env`
- 운영 서버: 서버 안의 `.env`
- GitHub Actions: GitHub Secrets
- 프로필 파일(`application-*.yaml`)은 환경별 정책을 공유하고, `.env`는 실제 값을 주입한다.
- 실제 비밀값은 Git 저장소에 넣지 않는다.
- Firebase 서비스 계정 JSON은 `.env`에 본문을 넣지 않고 파일로 두고 경로만 `.env`에 넣는다.
- 브라우저 관리자 페이지가 REST API를 호출하면 `API_ALLOWED_ORIGIN_PATTERNS`를, WebSocket을 사용하면 `CHAT_WS_ALLOWED_ORIGIN_PATTERNS`를 각각 설정한다.
- CD의 admin REST CORS smoke check는 `CD_SMOKE_CORS_ORIGIN`을 우선 사용하고, 비어 있으면 `API_ALLOWED_ORIGIN_PATTERNS`의 첫 번째 exact origin을 재사용한다.

예시:

```env
SPRING_PROFILES_ACTIVE=prod
APP_HOST_BIND=127.0.0.1
APP_HOST_PORT=8080
OPENAPI_ENABLED=false
API_ALLOWED_ORIGIN_PATTERNS=https://admin.skuri.example
CHAT_WS_ALLOWED_ORIGIN_PATTERNS=https://admin.skuri.example
CD_SMOKE_CORS_ORIGIN=https://admin.skuri.example
MEDIA_STORAGE_BASE_DIR=/app/var/media
MEDIA_STORAGE_PROVIDER=LOCAL
MEDIA_STORAGE_PUBLIC_BASE_URL=https://api.skuri.example/uploads
MEDIA_STORAGE_FIREBASE_BUCKET=
MYSQL_DATABASE=skuri
MYSQL_USER=skuri
MYSQL_PASSWORD=<db-user-password>
MYSQL_ROOT_PASSWORD=<db-root-password>
MYSQL_HOST_BIND=127.0.0.1
MYSQL_HOST_PORT=3307
FIREBASE_CREDENTIALS_FILE=/opt/skuri/secrets/firebase-admin.json
GOOGLE_APPLICATION_CREDENTIALS=/app/secrets/firebase-admin.json
FIREBASE_CREDENTIALS_PATH=/app/secrets/firebase-admin.json
```

- 운영 MySQL은 앱 컨테이너가 내부 네트워크 `mysql:3306`으로 사용한다.
- 운영 app 포트도 `${APP_HOST_BIND:-127.0.0.1}:${APP_HOST_PORT:-8080}` loopback 바인딩을 유지해 Nginx만 접근하게 한다.
- Workbench 같은 운영자 접속은 호스트 loopback `${MYSQL_HOST_BIND:-127.0.0.1}:${MYSQL_HOST_PORT:-3307}` 로만 열고 SSH를 통해 접근한다.
- `0.0.0.0:3306` 같은 공용 바인딩은 사용하지 않는다.
- `MEDIA_STORAGE_PROVIDER=LOCAL`이면 이미지 업로드는 `media-prod-data -> /app/var/media` 볼륨에 저장하고, 공개 URL은 `MEDIA_STORAGE_PUBLIC_BASE_URL`로 reverse proxy 도메인을 지정한다.
- `MEDIA_STORAGE_PROVIDER=FIREBASE`이면 `MEDIA_STORAGE_FIREBASE_BUCKET`에 업로드하고, 응답 URL은 Firebase Storage download URL을 사용한다. compose 설정상 local media volume은 남아 있어도, 현재 런타임에서는 FIREBASE provider의 업로드/공개 경로로 사용하지 않는다.

## 3. 로컬 실행

1. 루트의 `.env.example`를 기준으로 `.env`를 준비한다.
2. 평소 개발은 아래처럼 MySQL/Redis만 Docker로 올리고, 앱은 IDE 또는 `bootRun`으로 실행하는 방식을 권장한다.

```bash
docker compose up -d mysql redis
```

3. 앱을 호스트에서 직접 실행할 때는 필요한 값만 쉘 환경변수나 IDE 실행 설정에 주입한다.
   IntelliJ 환경 변수 칸에는 `.env` 파일 경로를 넣지 말고 `KEY=value` 형식으로 직접 입력한다.

`local` 프로필 예시:

```bash
DB_URL=jdbc:mysql://localhost:3306/skuri?serverTimezone=Asia/Seoul&characterEncoding=UTF-8 \
DB_USERNAME=root \
DB_PASSWORD=1234 \
FIREBASE_PROJECT_ID=sktaxi-acb4c \
FIREBASE_CREDENTIALS_PATH=/Users/<user>/skuri-backend/serviceAccountKey.json \
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

`local-emulator` 프로필 예시:

```bash
DB_URL=jdbc:mysql://localhost:3306/skuri?serverTimezone=Asia/Seoul&characterEncoding=UTF-8 \
DB_USERNAME=root \
DB_PASSWORD=1234 \
FIREBASE_PROJECT_ID=sktaxi-acb4c \
FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 \
FIREBASE_CREDENTIALS_PATH= \
GOOGLE_APPLICATION_CREDENTIALS= \
SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
```

4. 로컬에서도 app까지 Docker로 띄워서 "배포와 비슷한 방식"을 확인하고 싶다면 아래처럼 전체 compose를 올린다.

```bash
docker compose up -d --build
```

확인 포인트:

- `http://localhost:8080/actuator/health`
- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/scalar`

참고:

- `local`은 프론트와 함께 실제 Firebase ID Token 흐름을 검증하는 통합 테스트용이다. 이때는 실제 서비스 계정 파일 경로가 필요하다.
- `local-emulator`는 백엔드 단독 인증 테스트용이다. 이 프로필에서는 `FIREBASE_CREDENTIALS_PATH`와 `GOOGLE_APPLICATION_CREDENTIALS`를 비워 두는 것이 안전하다.
- `local-emulator`에서는 전체 `.env`를 그대로 로드하지 말고 emulator에 필요한 값만 별도로 주입하는 편이 안전하다.
- 두 로컬 프로필 모두 기본 DB는 `localhost:3306`을 바라보지만, 필요하면 `DB_URL`로 다른 포트(예: Docker MySQL `3307`)를 덮어쓸 수 있다.
- 현재 기본 `docker-compose.yml`은 Firebase 자격증명 파일을 자동 마운트하지 않는다. Docker에서 실제 Firebase 인증까지 검증하려면 자격증명 파일 volume mount를 별도로 추가하거나, 앱은 호스트에서 `bootRun`으로 실행한다.
- Redis는 아직 앱 로직에 연결되지 않았지만, 운영 compose에서는 향후 캐시 도입 자리를 미리 확보하기 위해 함께 기동한다.
- 로컬 개발/검증은 `local`과 `local-emulator` 두 프로필로만 운영한다.
- `local-emulator`는 기본적으로 로컬 DB 스키마를 재생성하지 않는다. 초기화가 필요하면 emulator 전용 DB를 별도로 사용한다.

## 4. 운영 서버 준비

운영 서버는 다음을 미리 준비해야 한다.

- Docker Engine
- Docker Compose Plugin
- 앱 배포 디렉터리
  - 기본값: `/opt/skuri/app`
- 운영 `.env`
  - 기본 위치: `/opt/skuri/app/.env`
- Firebase 서비스 계정 JSON
  - 예: `/opt/skuri/secrets/firebase-admin.json`
- OCI 인스턴스 안의 Docker 영속 볼륨
  - `mysql-prod-data`
  - `redis-prod-data`
  - `media-prod-data`
- 공개 도메인용 reverse proxy
  - 예: Nginx `80/443` -> 앱 `127.0.0.1:8080`
- Cloudflare를 쓰는 경우 SSL/TLS 모드
  - 권장: `Full (strict)`

운영 서버에 두는 파일:

- `/opt/skuri/app/.env`
- `/opt/skuri/app/docker-compose.prod.yml`
- `/opt/skuri/secrets/firebase-admin.json`

운영 공개 권장:

- `api.<domain>`은 Cloudflare DNS에 연결하고, Nginx가 `80/443`을 받아 앱 `8080`으로 프록시한다.
- 앱 컨테이너 자체도 `${APP_HOST_BIND:-127.0.0.1}:${APP_HOST_PORT:-8080}` loopback 으로만 bind해 외부 인터페이스 `0.0.0.0:8080` 노출을 방지한다.
- HTTPS가 정상화되면 OCI 보안 규칙에서 `8080` 외부 공개는 닫고 `22/80/443`만 유지한다.
- Workbench 접속용 MySQL 포트는 `127.0.0.1:3307` 같은 loopback 바인딩만 허용하고, SSH 터널 없이 직접 공개하지 않는다.

## 5. GitHub Actions CD 흐름

반자동 배포 흐름은 다음과 같다.

1. `main`에 push 또는 merge
2. `CD` workflow가 자동 시작
3. Docker 이미지를 GHCR에 push
4. `Deploy To Production` job이 `production` 환경 승인 대기 상태로 멈춤
5. GitHub에서 `Review deployments`
6. `Approve and deploy`
7. OCI 서버에 접속해서 최신 이미지 pull 및 재기동
8. 서버 내부에서 `health + 공개 API + admin CORS preflight + prod OpenAPI 비노출` smoke check

추가 정책:

- `concurrency.group = production-deploy`, `cancel-in-progress = true`로 설정해 새 `main` push가 오면 이전 CD run은 자동 취소한다.
- 승인할 때는 항상 가장 최신 commit의 run만 남아 있는지 확인한다.

중요:

- 이 흐름이 진짜 반자동이 되려면 GitHub Repository Settings에서 `production` Environment에 `Required reviewers`를 설정해야 한다.
- Environment 보호 규칙이 없으면 deploy job은 자동으로 바로 진행된다.

## 6. GitHub Secrets

`production` Environment 기준으로 아래 Secrets를 준비한다.

- `PROD_HOST`
- `PROD_SSH_USERNAME`
- `PROD_SSH_KEY`
- `PROD_SSH_PORT` (선택, 기본값 `22`)
- `PROD_DEPLOY_DIR` (선택, 기본값 `/opt/skuri/app`)
- `PROD_GHCR_USERNAME`
- `PROD_GHCR_READ_TOKEN`

참고:

- GitHub Actions는 `.env` 자체를 저장하지 않는다.
- CI/CD에서는 Secrets를 사용하고, 운영 서버는 자신의 `.env`를 유지한다.
- CD는 `linux/amd64,linux/arm64` 멀티플랫폼 이미지를 push하므로, 나중에 AWS x86 서버로 옮겨도 compose 파일을 크게 바꾸지 않아도 된다.

## 7. OpenAPI 운영 정책

- `local`, `local-emulator`: `/v3/api-docs`, `/swagger-ui/index.html`, `/scalar` 사용 가능
- `prod`: 기본 비노출
- 운영에서 임시로 열어야 하면 `.env`의 `OPENAPI_ENABLED=true`로 조정할 수 있다.

## 8. Redis 운영 범위

이번 Phase의 Redis 범위는 아래까지만 포함한다.

- 로컬 `docker-compose.yml`에 Redis 컨테이너 추가
- 공통 설정 파일에 Redis host/port 환경변수 자리 확보
- 운영 설계 문서에 “후속 캐시 도입 후보”로 정리

이번 Phase에서 하지 않는 것:

- `@Cacheable` 적용
- 캐시 무효화 정책 설계
- 외부 관리형 Redis 연결

후속 후보:

- 파티 목록 캐시
- 공지 목록 캐시
- FCM 토큰 조회 캐시

## 9. 배포 전 체크리스트

- `./gradlew build` 성공
- Docker 이미지 빌드 성공
- 운영 `.env` 값 최신화 확인
- Firebase 자격증명 파일 존재 확인
- `MYSQL_*` 값과 Docker 영속 볼륨 상태 확인
- OpenAPI 노출 정책(`OPENAPI_ENABLED`) 확인
- GitHub `production` Environment 보호 규칙 확인

## 10. Phase 10 회원 lifecycle 마이그레이션

Phase 10 이전 버전에서 운영 중이던 DB를 업그레이드할 때는 `members.status`를 앱 기동 전에 수동으로 채워야 한다.

- 배경:
  - `Member.status`는 엔티티 기준 `nullable = false`다.
  - 현재 앱은 `spring.jpa.hibernate.ddl-auto=update`를 사용한다.
  - 따라서 legacy `members` row가 있는 상태에서 선행 SQL 없이 새 버전을 먼저 띄우면 schema update가 실패할 수 있다.
- 적용 시점:
  - Phase 10 코드가 포함된 버전을 서버에 올리기 전에 MySQL에서 먼저 실행한다.
- 권장 순서:
  1. 앱을 내린다.
  2. 아래 SQL을 운영 DB에 적용한다.
  3. 결과를 검증한다.
  4. 새 앱 버전을 기동한다.

```sql
ALTER TABLE members
    ADD COLUMN status VARCHAR(20) NULL AFTER is_admin;

UPDATE members
SET status = 'ACTIVE'
WHERE status IS NULL;

ALTER TABLE members
    MODIFY COLUMN status VARCHAR(20) NOT NULL;
```

검증 SQL:

```sql
SELECT status, COUNT(*) AS count
FROM members
GROUP BY status;
```

추가 메모:

- 이미 `status` 컬럼이 있는 환경이면 `UPDATE ... WHERE status IS NULL`과 `MODIFY COLUMN ... NOT NULL`만 적용하면 된다.
- `withdrawn_at`은 nullable 컬럼이라 기존 운영 DB에서는 Hibernate update에 맡겨도 되지만, 운영 표준 절차상 상태 컬럼 선행 마이그레이션은 필수로 본다.
- 관련 정책 설명은 [`member-withdrawal-policy.md`](./member-withdrawal-policy.md)를 함께 참조한다.

## 10.1 학과 master/FK 및 강의 이수구분 정규화

학과 master와 강의 필터가 포함된 버전은 아래 순서로 운영 DB를 먼저 준비한 뒤 배포한다. 현재 런타임이 `spring.jpa.hibernate.ddl-auto=update`를 사용하더라도, 기존 데이터가 있는 컬럼의 FK와 데이터 정규화는 Hibernate에 맡기지 않는다.

### 확인된 운영 데이터 기준

2026-08-14에 전달받은 운영 조회 결과의 기준값은 다음과 같다. 이 값이 달라졌다면 이 문서의 기대 건수를 그대로 적용하지 말고 작업을 중단해 재검토한다.

- `courses`: 2,179건
- 단축 이수구분: `전선` 647건, `전필` 45건, `교선` 176건, `교필` 180건
- 정규화 대상 합계: 1,048건
- `members.department`: null 12건, non-null 1,001건, canonical master 밖의 값 0건
- `chat_rooms.department`: 29개 canonical 학과가 각각 1건, master 밖의 값 0건

이 수치는 사용자가 운영 DB에서 실행해 전달한 결과를 기준으로 하며, 문서 작성자가 별도 운영 DB 접속으로 재인증한 값은 아니다.

### 준비된 SQL 파일

- [`sql/2026-08-14-department-course-preflight.sql`](./sql/2026-08-14-department-course-preflight.sql): 대상 DB의 테이블/컬럼/collation/category 분포/FK를 읽기 전용으로 확인한다.
- [`sql/2026-08-14-department-course-migration.sql`](./sql/2026-08-14-department-course-migration.sql): 환경 검증, 동적 collation 일치, 학과 seed, 직접 입력 학과 컬럼, category 정규화, FK 생성을 수행하는 procedure를 정의한다. 파일을 읽는 것만으로 migration이 실행되지는 않는다.
- [`sql/2026-08-14-department-course-postcheck.sql`](./sql/2026-08-14-department-course-postcheck.sql): 적용 후 category 잔여값, 학과 orphan, FK 3개를 검증한다.

비밀번호는 명령행에 직접 적지 않고 `mysql -p` 프롬프트 또는 운영자가 관리하는 MySQL login path를 사용한다. `.env` 파일을 `source`해서 shell 환경으로 펼치지 않는다.

#### 로컬 DB 적용

현재 Mac의 기본 로컬 대상은 `127.0.0.1:3306/skuri`다. 새 DB라면 먼저 현재 백엔드를 `JPA_DDL_AUTO=update`로 한 번 기동해 legacy 필수 테이블을 만든다. 기존 DB라면 바로 아래 절차를 사용한다.

```bash
mysql -h 127.0.0.1 -P 3306 -u <LOCAL_DB_USER> -p skuri
```

mysql prompt에서 실행한다.

```sql
SOURCE docs/sql/2026-08-14-department-course-preflight.sql;
SOURCE docs/sql/2026-08-14-department-course-migration.sql;
CALL migrate_department_course(-1);
SOURCE docs/sql/2026-08-14-department-course-postcheck.sql;
DROP PROCEDURE migrate_department_course;
```

- `-1`은 로컬 데이터의 실제 정규화 대상 건수를 허용한다는 의미다.
- 지원하지 않는 category, canonical master로 해석할 수 없는 학과, FK schema 불일치는 로컬에서도 즉시 중단한다.
- 로컬도 운영과 같은 FK 3개를 갖도록 맞춰야 개발/테스트 환경에서 무결성 차이를 발견할 수 있다.

#### 운영 DB 적용

앱을 내리고 백업을 확보한 뒤 SSH tunnel을 통해 운영 MySQL의 loopback 포트에 접속한다. 예시는 로컬 tunnel endpoint가 `127.0.0.1:3307`인 경우다.

```bash
mysql -h 127.0.0.1 -P 3307 -u <PROD_DB_USER> -p <PROD_DB_NAME>
```

mysql prompt에서 먼저 preflight 결과가 이 절의 운영 기준과 여전히 같은지 확인한 뒤 실행한다.

```sql
SOURCE docs/sql/2026-08-14-department-course-preflight.sql;
SOURCE docs/sql/2026-08-14-department-course-migration.sql;
CALL migrate_department_course(1048);
SOURCE docs/sql/2026-08-14-department-course-postcheck.sql;
DROP PROCEDURE migrate_department_course;
```

- 운영 `1048`은 전달받은 2026-08-14 조회 결과에 대한 안전장치다.
- 실행 시점의 preflight 결과가 달라졌다면 숫자만 임의로 바꾸지 말고 신규 데이터 유입 원인을 확인한 뒤 기대값을 다시 승인한다.
- MySQL DDL은 rollback되지 않으므로 백업과 앱 중지가 필수다. procedure는 가능한 오류를 DDL 전에 검사하지만, 실행 중 인프라 장애까지 원자적으로 되돌릴 수는 없다.
- migration 성공 후에만 새 백엔드를 배포하고 API smoke test를 진행한다.

### 1. 사전 조건과 스키마 확인

1. 앱을 내리고 DB 백업을 확보한다.
2. 아래 쿼리로 FK 양쪽 컬럼의 타입/문자셋/collation을 확인한다.
3. `departments.name`은 참조 컬럼과 정확히 같은 `VARCHAR(50)`, character set, collation으로 만든다. 하나라도 다르면 FK를 추가하지 말고 먼저 스키마를 맞춘다.

```sql
SELECT table_name, column_name, column_type, character_set_name,
       collation_name, is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
    (table_name = 'members' AND column_name = 'department')
    OR (table_name = 'chat_rooms' AND column_name = 'department')
    OR (table_name = 'user_timetable_manual_courses' AND column_name = 'department')
  )
ORDER BY table_name;
```

### 2. 학과 master 생성과 seed

아래 `CHARACTER SET`/`COLLATE` 값은 1단계에서 확인한 실제 운영 값으로 대체한다.

```sql
CREATE TABLE departments (
    name VARCHAR(50) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL,
    PRIMARY KEY (name),
    INDEX idx_departments_active_order (active, display_order, name)
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = <MEMBERS_DEPARTMENT_CHARACTER_SET>
  COLLATE = <MEMBERS_DEPARTMENT_COLLATION>;

INSERT INTO departments (name, active, display_order) VALUES
('신학과', TRUE, 1),
('기독교교육상담학과', TRUE, 2),
('문화선교학과', TRUE, 3),
('영어영문학과', TRUE, 4),
('중어중문학과', TRUE, 5),
('국어국문학과', TRUE, 6),
('사회복지학과', TRUE, 7),
('국제개발협력학과', TRUE, 8),
('행정학과', TRUE, 9),
('관광학과', TRUE, 10),
('경영학과', TRUE, 11),
('글로벌물류학과', TRUE, 12),
('산업경영공학과', TRUE, 13),
('유아교육과', TRUE, 14),
('체육교육과', TRUE, 15),
('교직부', TRUE, 16),
('컴퓨터공학과', TRUE, 17),
('정보통신공학과', TRUE, 18),
('미디어소프트웨어학과', TRUE, 19),
('도시디자인정보공학과', TRUE, 20),
('음악학부', TRUE, 21),
('실용음악과', TRUE, 22),
('공연음악예술학부', TRUE, 23),
('연기예술학과', TRUE, 24),
('영화영상학과', TRUE, 25),
('연극영화학부', TRUE, 26),
('뷰티디자인학과', TRUE, 27),
('융합학부', TRUE, 28),
('파이데이아학부', TRUE, 29);

ALTER TABLE user_timetable_manual_courses
    ADD COLUMN department VARCHAR(50)
        CHARACTER SET <MEMBERS_DEPARTMENT_CHARACTER_SET>
        COLLATE <MEMBERS_DEPARTMENT_COLLATION>
        NULL AFTER professor;
```

`user_timetable_manual_courses.department`도 `departments.name`과 동일한 character set/collation이어야 한다. 컬럼이 이미 있으면 `ADD COLUMN`은 실행하지 않고, 1단계 조회를 다시 실행해 실제 속성을 확인한다.

### 3. FK 전 orphan 검증

모든 결과가 0건이어야 한다. `소프트웨어학과`가 발견되면 `미디어소프트웨어학과`로 명시적으로 정규화한 뒤 다시 검증한다. 그 외 값은 임의 매핑하지 않는다.

```sql
SELECT m.department, COUNT(*) AS count
FROM members m
LEFT JOIN departments d ON d.name = m.department
WHERE m.department IS NOT NULL AND d.name IS NULL
GROUP BY m.department;

SELECT c.department, COUNT(*) AS count
FROM chat_rooms c
LEFT JOIN departments d ON d.name = c.department
WHERE c.department IS NOT NULL AND d.name IS NULL
GROUP BY c.department;

SELECT mc.department, COUNT(*) AS count
FROM user_timetable_manual_courses mc
LEFT JOIN departments d ON d.name = mc.department
WHERE mc.department IS NOT NULL AND d.name IS NULL
GROUP BY mc.department;
```

### 4. 강의 이수구분 정규화

이 단계만 별도 트랜잭션으로 실행한다. `normalized_count = 1048`이고 사후 집계가 아래 기대값과 일치할 때만 `COMMIT`한다. 다르면 `ROLLBACK`하고 원인을 확인한다.

```sql
START TRANSACTION;

UPDATE courses
SET category = CASE category
    WHEN '전선' THEN '전공선택'
    WHEN '전필' THEN '전공필수'
    WHEN '교선' THEN '교양선택'
    WHEN '교필' THEN '교양필수'
    ELSE category
END
WHERE category IN ('전선', '전필', '교선', '교필');

SELECT ROW_COUNT() AS normalized_count;

SELECT category, COUNT(*) AS count
FROM courses
GROUP BY category
ORDER BY category;

-- 기대값:
-- 교양선택 331, 교양필수 386, 교직 57,
-- 전공선택 1294, 전공필수 111 (합계 2179)

-- 검증 성공 시 운영자가 COMMIT;
-- 검증 실패 시 운영자가 ROLLBACK;
```

신규 bulk 강의 입력은 애플리케이션에서도 같은 단축 표기를 canonical 장문 표기로 정규화하므로, 마이그레이션 뒤에 단축 표기가 다시 쌓이지 않는다.

### 5. FK 추가

orphan 검증이 모두 0건이고 세 컬럼의 타입/문자셋/collation이 같을 때만 실행한다. 학과명 변경/삭제가 회원·공개채팅방·직접 입력 강의에 암묵적으로 전파되지 않도록 `RESTRICT`를 사용한다.

```sql
ALTER TABLE members
    ADD CONSTRAINT fk_members_department
    FOREIGN KEY (department) REFERENCES departments(name)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE chat_rooms
    ADD CONSTRAINT fk_chat_rooms_department
    FOREIGN KEY (department) REFERENCES departments(name)
    ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE INDEX idx_user_timetable_manual_courses_department
    ON user_timetable_manual_courses(department);

ALTER TABLE user_timetable_manual_courses
    ADD CONSTRAINT fk_user_timetable_manual_courses_department
    FOREIGN KEY (department) REFERENCES departments(name)
    ON UPDATE RESTRICT ON DELETE RESTRICT;
```

- `courses.department`: 강의 원본의 자유 텍스트이므로 FK를 두지 않는다. 강의 화면 필터는 해당 학기 값의 중복 제거 목록을 사용한다.
- `notices.department`: 회원 소속 학과가 아니라 공지 발행 부서/출처 의미이므로 FK를 두지 않는다.

### 6. 애플리케이션 배포 후 확인

```sql
SELECT name, active, display_order
FROM departments
ORDER BY display_order, name;

SELECT category, COUNT(*) AS count
FROM courses
GROUP BY category
ORDER BY category;
```

- 인증 후 `GET /v1/departments`가 활성 29개 학과를 순서대로 반환하는지 확인한다.
- `GET /v1/courses/filter-options?semester=<현재 학기>`가 강의 테이블 기반 학과/학년/이수구분을 반환하는지 확인한다.
- 프로필 학과 저장과 직접 입력 강의 학과 저장에서 미지원 학과가 `422 VALIDATION_ERROR`로 거부되는지 확인한다.

## 11. 배포 후 체크리스트

- `docker ps`에서 `skuri-backend` 정상 실행 확인
- `docker compose -f docker-compose.prod.yml ps`에서 app가 `127.0.0.1:<APP_HOST_PORT>->8080/tcp` 로만 열렸는지 확인
- `curl http://127.0.0.1:<APP_HOST_PORT>/actuator/health`
- 공개 API smoke check
  - `GET /v1/app-versions/{platform}`
- admin REST CORS preflight
  - `OPTIONS /v1/app-versions/{platform}` with `Origin: ${CD_SMOKE_CORS_ORIGIN}` 또는 `API_ALLOWED_ORIGIN_PATTERNS`의 첫 번째 exact origin
- 인증 API smoke check
  - 토큰이 있으면 `GET /v1/members/me`
- 컨테이너 로그 확인
  - `docker logs --tail 200 skuri-backend`
- OpenAPI가 prod에서 의도대로 닫혀 있는지 확인
- `ss -ltnp | grep <APP_HOST_PORT>` 결과가 `127.0.0.1:<APP_HOST_PORT>`만 가리키는지 확인
- MySQL 운영자 접속이 필요하면 `docker compose -f docker-compose.prod.yml ps`로 `127.0.0.1:3307->3306/tcp` 같은 loopback 바인딩만 노출되는지 확인

## 12. MySQL Workbench 접속

현재 운영 compose는 MySQL을 인터넷에 직접 공개하지 않고, 서버 자신의 loopback 포트로만 바인딩한다.
권장 방식은 `Standard TCP/IP over SSH`다.

1. OCI 서버에서 MySQL 포트가 loopback으로만 열렸는지 확인한다.

```bash
cd /opt/skuri/app
docker compose -f docker-compose.prod.yml ps
ss -ltnp | grep 3307
```

기대 결과:
- `127.0.0.1:3307->3306/tcp` 처럼 loopback만 노출
- `0.0.0.0:3307` 이 아니어야 함

2. 운영 `.env`에서 접속 정보를 확인한다.

```bash
cd /opt/skuri/app
grep -E '^(MYSQL_USER|MYSQL_PASSWORD|MYSQL_HOST_PORT|MYSQL_HOST_BIND)=' .env
```

3. MySQL Workbench에서 새 연결을 만든다.

- Connection Name: `SKURI Production DB`
- Connection Method: `Standard TCP/IP over SSH`
- SSH Hostname: `<운영서버도메인또는IP>:22`
- SSH Username: `<운영서버 SSH 계정>`
- SSH Key File: `<로컬 개인키 경로>`
- MySQL Hostname: `127.0.0.1`
- MySQL Server Port: `3307`
- Username: `.env`의 `MYSQL_USER`
- Password: `.env`의 `MYSQL_PASSWORD`

4. `Test Connection`을 누른다.

5. 접속이 안 되면 아래 순서로 확인한다.

```bash
cd /opt/skuri/app
docker compose -f docker-compose.prod.yml ps
docker logs --tail 100 skuri-mysql
ss -ltnp | grep 3307
```

주의:
- Workbench 연결을 위해 MySQL을 `0.0.0.0:3306` 으로 공개하지 않는다.
- 컨테이너 내부 DB 주소는 계속 `mysql:3306` 이며, Workbench용 loopback 포트와는 용도가 다르다.

## 13. 롤백

롤백은 “이전 이미지 태그로 다시 기동” 방식으로 진행한다.

1. GHCR에서 이전 정상 이미지 태그를 확인한다.
2. 서버에서 해당 태그를 지정해 다시 배포한다.

예시:

```bash
cd /opt/skuri/app
IMAGE_URI=ghcr.io/<owner>/<repo>:<previous-sha> docker compose -f docker-compose.prod.yml up -d
```

롤백 후 반드시 다시 확인할 것:

- `/actuator/health`
- 공개 API 1개
- 인증 API 1개
- 최근 에러 로그
- MySQL 컨테이너와 볼륨이 유지됐는지 확인

## 14. 향후 외부 AI 서비스 메모

향후 Python 기반 AI 서버를 붙일 가능성이 있다면 지금 단계에서 최소한 아래 환경변수 네이밍만 확보해두면 충분하다.

- `AI_SERVICE_BASE_URL`
- `AI_SERVICE_AUTH_TOKEN`
- `AI_SERVICE_TIMEOUT_MS`

권장 원칙:

- Spring과 Python은 HTTP API로 통신
- AI 장애가 공지 기본 조회를 깨지 않도록 fallback 유지
- 운영에서는 가능하면 같은 VPC/사설망에 배치

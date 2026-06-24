# 개발에 필요한 명령어 모음

## 빌드
```bash
./gradlew build
./gradlew compileJava compileTestJava
```

## Member 프로필 사진 삭제/소유권 검증 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.member.service.MemberServiceTest" --tests "com.skuri.skuri_backend.domain.member.controller.MemberControllerContractTest" --tests "com.skuri.skuri_backend.domain.image.service.ImageUploadServiceTest" --tests "com.skuri.skuri_backend.domain.image.service.ProfileImageStorageServiceTest" --tests "com.skuri.skuri_backend.domain.image.controller.ImageControllerContractTest" --tests "com.skuri.skuri_backend.infra.storage.LocalStorageRepositoryTest" --tests "com.skuri.skuri_backend.infra.storage.FirebaseStorageRepositoryTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest"
./gradlew build
```

## Inquiry Attachment 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.support.controller.InquiryControllerContractTest" --tests "com.skuri.skuri_backend.domain.support.controller.InquiryAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.support.service.InquiryServiceTest" --tests "com.skuri.skuri_backend.domain.image.service.ImageUploadServiceTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest" --tests "com.skuri.skuri_backend.infra.admin.audit.AdminAuditIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest"
./gradlew build
```

## Legal Document 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.support.controller.LegalDocumentControllerContractTest" --tests "com.skuri.skuri_backend.domain.support.controller.LegalDocumentAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.support.service.LegalDocumentServiceTest" --tests "com.skuri.skuri_backend.domain.support.service.LegalDocumentSeedMigrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest"
./gradlew build
```

## Minecraft bridge 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.minecraft.controller.*" --tests "com.skuri.skuri_backend.domain.minecraft.service.MinecraftAccountServiceDataJpaTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest"
./gradlew build
```

## Board/Notice 익명 댓글 작업 검증
```bash
./gradlew clean compileJava
./gradlew build -x test
# 선택 컴파일 방식으로 변경 범위 테스트만 검증
./gradlew -I /tmp/skuri-selected-tests.gradle test
./gradlew -I /tmp/skuri-openapi-tests.gradle test
```

## 자주 쓰는 테스트
```bash
./gradlew test
./gradlew test --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.admin.audit.AdminAuditIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest"
```

## Migration 러너 검증
```bash
./gradlew compileJava test --tests "*NoticeMigrationJobDataJpaTest" --tests "*NoticeThumbnailMigrationJobDataJpaTest" --tests "*NoticeSyncServiceTest"
./gradlew test --tests "*CutoverMigrationJobDataJpaTest"
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun --args='--migration.enabled=true --migration.plan=NOTICES --migration.mode=DRY_RUN --migration.notice-file=/Users/jisung/skuri-backend/data-to-migration/notices-export.json --migration.report-dir=/Users/jisung/skuri-backend/data-to-migration/reports --spring.main.web-application-type=none --spring.task.scheduling.enabled=false'
source .env\nunset FIREBASE_CREDENTIALS_PATH GOOGLE_APPLICATION_CREDENTIALS\nFIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=local-migration SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun --args='--migration.enabled=true --migration.plan=NOTICE_THUMBNAILS --migration.mode=DRY_RUN --migration.batch-size=500 --migration.report-dir=/Users/jisung/skuri-backend/data-to-migration/reports --spring.main.web-application-type=none --spring.task.scheduling.enabled=false'\nsource .env\nunset FIREBASE_CREDENTIALS_PATH GOOGLE_APPLICATION_CREDENTIALS\nFIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=local-migration SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun --args='--migration.enabled=true --migration.plan=NOTICE_THUMBNAILS --migration.mode=APPLY --migration.batch-size=500 --migration.report-dir=/Users/jisung/skuri-backend/data-to-migration/reports --spring.main.web-application-type=none --spring.task.scheduling.enabled=false'
```

## Academic 온라인 강의 검증
```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home ./gradlew test --tests "com.skuri.skuri_backend.domain.academic.controller.CourseControllerContractTest" --tests "com.skuri.skuri_backend.domain.academic.controller.CourseAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.academic.controller.TimetableControllerContractTest" --tests "com.skuri.skuri_backend.domain.academic.service.CourseServiceTest" --tests "com.skuri.skuri_backend.domain.academic.service.CourseServiceDataJpaTest" --tests "com.skuri.skuri_backend.domain.academic.service.TimetableServiceTest" --tests "com.skuri.skuri_backend.domain.academic.repository.CourseRepositoryDataJpaTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiUiAvailabilityIntegrationTest"
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18081 ./gradlew bootRun
```

## 서버 실행
```bash
docker compose up -d mysql redis
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
DB_URL=jdbc:mysql://localhost:3306/skuri?serverTimezone=Asia/Seoul&characterEncoding=UTF-8 DB_USERNAME=root DB_PASSWORD=1234 FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=sktaxi-acb4c FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator ./gradlew bootRun
```

## Admin Member API 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.member.controller.MemberAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.member.service.MemberAdminServiceTest" --tests "com.skuri.skuri_backend.domain.member.repository.MemberRepositoryDataJpaTest" --tests "com.skuri.skuri_backend.domain.notification.controller.NotificationControllerContractTest" --tests "com.skuri.skuri_backend.domain.notification.service.FcmTokenServiceTest" --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiUiAvailabilityIntegrationTest"
./gradlew build
```

## Admin Member Activity 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18081 ./gradlew bootRun
curl http://127.0.0.1:18081/v1/admin/members/<MEMBER_ID>/activity -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl http://127.0.0.1:18081/v3/api-docs
curl http://127.0.0.1:18081/swagger-ui/index.html
curl http://127.0.0.1:18081/scalar
```

## local-emulator 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18081 ./gradlew bootRun
curl -X PATCH "http://127.0.0.1:18081/v1/admin/members/<ADMIN_MEMBER_ID>/admin-role" -H "Authorization: Bearer <ADMIN_ID_TOKEN>" -H 'Content-Type: application/json' -d '{"isAdmin":false}'
curl http://127.0.0.1:18081/v3/api-docs
curl http://127.0.0.1:18081/swagger-ui/index.html
curl http://127.0.0.1:18081/scalar
```


## Admin TaxiParty API 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.taxiparty.controller.PartyAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyAdminServiceTest" --tests "com.skuri.skuri_backend.infra.admin.audit.AdminAuditIntegrationTest" --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiUiAvailabilityIntegrationTest"
./gradlew build
```

## Admin TaxiParty 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18080 ./gradlew bootRun
curl "http://127.0.0.1:18080/v1/admin/parties?page=0&size=20" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl "http://127.0.0.1:18080/v1/admin/parties/<PARTY_ID>" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl -X PATCH "http://127.0.0.1:18080/v1/admin/parties/<PARTY_ID>/status" -H "Authorization: Bearer <ADMIN_ID_TOKEN>" -H 'Content-Type: application/json' -d '{"action":"CLOSE"}'
curl -X DELETE "http://127.0.0.1:18080/v1/admin/parties/<PARTY_ID>/members/<MEMBER_ID>" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl -X POST "http://127.0.0.1:18080/v1/admin/parties/<PARTY_ID>/messages/system" -H "Authorization: Bearer <ADMIN_ID_TOKEN>" -H 'Content-Type: application/json' -d '{"message":"관리자 안내 메시지"}'
curl "http://127.0.0.1:18080/v1/admin/parties/<PARTY_ID>/join-requests" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl http://127.0.0.1:18080/v3/api-docs
curl http://127.0.0.1:18080/swagger-ui/index.html
curl http://127.0.0.1:18080/scalar
```

## Admin Board API 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.board.controller.BoardAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.board.service.BoardAdminServiceTest" --tests "com.skuri.skuri_backend.infra.admin.audit.AdminAuditIntegrationTest" --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiUiAvailabilityIntegrationTest"
./gradlew build
```

## Admin Board 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18082 ./gradlew bootRun
curl "http://127.0.0.1:18082/v1/admin/posts?page=0&size=20" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl "http://127.0.0.1:18082/v1/admin/posts/<POST_ID>" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl -X PATCH "http://127.0.0.1:18082/v1/admin/posts/<POST_ID>/moderation" -H "Authorization: Bearer <ADMIN_ID_TOKEN>" -H 'Content-Type: application/json' -d '{"status":"HIDDEN"}'
curl "http://127.0.0.1:18082/v1/admin/comments?page=0&size=20" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl -X PATCH "http://127.0.0.1:18082/v1/admin/comments/<COMMENT_ID>/moderation" -H "Authorization: Bearer <ADMIN_ID_TOKEN>" -H 'Content-Type: application/json' -d '{"status":"DELETED"}'
curl http://127.0.0.1:18082/v3/api-docs
curl http://127.0.0.1:18082/swagger-ui/index.html
curl http://127.0.0.1:18082/scalar
```


## Admin Dashboard API 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.admin.dashboard.controller.AdminDashboardControllerContractTest" --tests "com.skuri.skuri_backend.domain.admin.dashboard.service.AdminDashboardServiceTest" --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiUiAvailabilityIntegrationTest"
./gradlew build
```

## Admin Dashboard 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18083 ./gradlew bootRun
curl "http://127.0.0.1:18083/v1/admin/dashboard/summary" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl "http://127.0.0.1:18083/v1/admin/dashboard/activity?days=7" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl "http://127.0.0.1:18083/v1/admin/dashboard/recent-items?limit=10" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl http://127.0.0.1:18083/v3/api-docs
curl http://127.0.0.1:18083/swagger-ui/index.html
curl http://127.0.0.1:18083/scalar
```


## Admin AcademicSchedule Bulk Sync 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.academic.controller.AcademicScheduleAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.academic.service.AcademicScheduleServiceDataJpaTest" --tests "com.skuri.skuri_backend.infra.auth.AdminApiGuardIntegrationTest" --tests "com.skuri.skuri_backend.infra.admin.audit.AdminAuditIntegrationTest"
./gradlew test --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiUiAvailabilityIntegrationTest"
./gradlew build
```

## Admin AcademicSchedule Bulk Sync 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_USE_EMULATOR=true FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18081 ./gradlew bootRun
curl -X PUT "http://127.0.0.1:18081/v1/admin/academic-schedules/bulk" -H "Authorization: Bearer <ADMIN_ID_TOKEN>" -H 'Content-Type: application/json' --data @bulk-academic-schedules.json
curl http://127.0.0.1:18081/v3/api-docs
curl http://127.0.0.1:18081/swagger-ui/index.html
curl http://127.0.0.1:18081/scalar
```


## Admin Chat Read API 작업 검증
```bash
./gradlew test --tests "com.skuri.skuri_backend.domain.chat.controller.ChatAdminRoomControllerContractTest" --tests "com.skuri.skuri_backend.domain.chat.service.ChatServiceTest" --tests "com.skuri.skuri_backend.domain.chat.service.ChatAdminServiceTest" --tests "com.skuri.skuri_backend.domain.taxiparty.controller.PartyAdminControllerContractTest" --tests "com.skuri.skuri_backend.domain.taxiparty.service.TaxiPartyAdminServiceTest" --tests "com.skuri.skuri_backend.infra.openapi.AdminOpenApiConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiResponseExamplesConventionTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiSuccessSchemaCoverageIntegrationTest" --tests "com.skuri.skuri_backend.infra.openapi.OpenApiUiAvailabilityIntegrationTest"
./gradlew build
```

## Admin Chat Read API 수동 검증
```bash
source .env
firebase emulators:start --only auth --project "$FIREBASE_PROJECT_ID"
DB_URL=$DB_URL DB_USERNAME=$DB_USERNAME DB_PASSWORD=$DB_PASSWORD FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 FIREBASE_PROJECT_ID=$FIREBASE_PROJECT_ID FIREBASE_CREDENTIALS_PATH= GOOGLE_APPLICATION_CREDENTIALS= SPRING_PROFILES_ACTIVE=local-emulator SERVER_PORT=18080 ./gradlew bootRun
curl "http://127.0.0.1:18080/v1/admin/chat-rooms?type=DEPARTMENT" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl "http://127.0.0.1:18080/v1/admin/chat-rooms/<CHAT_ROOM_ID>" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl "http://127.0.0.1:18080/v1/admin/chat-rooms/<CHAT_ROOM_ID>/members" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl "http://127.0.0.1:18080/v1/admin/chat-rooms/<CHAT_ROOM_ID>/messages?size=2" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl "http://127.0.0.1:18080/v1/admin/parties/<PARTY_ID>/messages?size=2" -H "Authorization: Bearer <ADMIN_ID_TOKEN>"
curl http://127.0.0.1:18080/v3/api-docs
curl http://127.0.0.1:18080/swagger-ui/index.html
curl http://127.0.0.1:18080/scalar
```

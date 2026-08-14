package com.skuri.skuri_backend.infra.openapi;

public final class OpenApiAcademicExamples {

    private OpenApiAcademicExamples() {
    }

    public static final String SUCCESS_COURSE_FILTER_OPTIONS = """
            {
              "success": true,
              "data": {
                "departments": ["교양", "컴퓨터공학과"],
                "grades": [1, 2, 3, 4],
                "categories": ["교양선택", "교양필수", "전공선택", "전공필수"]
              }
            }
            """;

    public static final String SUCCESS_COURSE_LIST_PAGE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "course_uuid",
                    "semester": "2026-1",
                    "code": "01255",
                    "division": "001",
                    "name": "민법총칙",
                    "credits": 3,
                    "isOnline": false,
                    "professor": "문상혁",
                    "department": "법학과",
                    "grade": 2,
                    "category": "전공선택",
                    "location": "영401",
                    "note": null,
                    "schedule": [
                      {
                        "dayOfWeek": 1,
                        "startPeriod": 3,
                        "endPeriod": 4
                      },
                      {
                        "dayOfWeek": 3,
                        "startPeriod": 3,
                        "endPeriod": 4
                      }
                    ]
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_COURSE_LIST_PAGE_WITH_OFFICIAL_ONLINE = """
            {
              "success": true,
              "data": {
                "content": [
                  {
                    "id": "course_online_uuid",
                    "semester": "2026-1",
                    "code": "20797",
                    "division": "001",
                    "name": "사랑의인문학(KCU온라인강좌)",
                    "credits": 3,
                    "isOnline": true,
                    "professor": null,
                    "department": "교양",
                    "grade": 1,
                    "category": "교양선택",
                    "location": null,
                    "note": null,
                    "schedule": []
                  }
                ],
                "page": 0,
                "size": 20,
                "totalElements": 1,
                "totalPages": 1,
                "hasNext": false,
                "hasPrevious": false
              }
            }
            """;

    public static final String SUCCESS_TIMETABLE = """
            {
              "success": true,
              "data": {
                "id": "timetable_uuid",
                "semester": "2026-1",
                "courseCount": 1,
                "totalCredits": 3,
                "courses": [
                  {
                    "id": "course_uuid",
                    "code": "01255",
                    "division": "001",
                    "name": "민법총칙",
                    "professor": "문상혁",
                    "location": "영401",
                    "category": "전공선택",
                    "department": "법학과",
                    "credits": 3,
                    "isOnline": false,
                    "schedule": [
                      {
                        "dayOfWeek": 1,
                        "startPeriod": 3,
                        "endPeriod": 4
                      }
                    ]
                  }
                ],
                "slots": [
                  {
                    "courseId": "course_uuid",
                    "courseName": "민법총칙",
                    "code": "01255",
                    "dayOfWeek": 1,
                    "startPeriod": 3,
                    "endPeriod": 4,
                    "professor": "문상혁",
                    "location": "영401"
                  }
                ]
              }
            }
            """;

    public static final String SUCCESS_TIMETABLE_WITH_OFFICIAL_ONLINE = """
            {
              "success": true,
              "data": {
                "id": "timetable_uuid",
                "semester": "2026-1",
                "courseCount": 1,
                "totalCredits": 3,
                "courses": [
                  {
                    "id": "course_online_uuid",
                    "code": "20797",
                    "division": "001",
                    "name": "사랑의인문학(KCU온라인강좌)",
                    "professor": null,
                    "location": null,
                    "category": "교양선택",
                    "department": "교양",
                    "credits": 3,
                    "isOnline": true,
                    "schedule": []
                  }
                ],
                "slots": []
              }
            }
            """;

    public static final String SUCCESS_TIMETABLE_SEMESTERS = """
            {
              "success": true,
              "data": [
                {
                  "id": "2026-1",
                  "label": "2026-1학기"
                },
                {
                  "id": "2025-2",
                  "label": "2025-2학기"
                }
              ]
            }
            """;

    public static final String SUCCESS_TIMETABLE_WITH_MANUAL_ONLINE = """
            {
              "success": true,
              "data": {
                "id": "timetable_uuid",
                "semester": "2026-1",
                "courseCount": 1,
                "totalCredits": 2,
                "courses": [
                  {
                    "id": "manual_course_uuid",
                    "code": "직접 입력",
                    "division": null,
                    "name": "플랫폼세미나",
                    "professor": "직접 입력",
                    "location": null,
                    "category": null,
                    "department": "컴퓨터공학과",
                    "credits": 2,
                    "isOnline": true,
                    "schedule": []
                  }
                ],
                "slots": []
              }
            }
            """;

    public static final String SUCCESS_ACADEMIC_SCHEDULE_LIST = """
            {
              "success": true,
              "data": [
                {
                  "id": "schedule_uuid",
                  "title": "1학기 개강",
                  "startDate": "2026-03-02",
                  "endDate": "2026-03-02",
                  "type": "SINGLE",
                  "isPrimary": true,
                  "description": "2026학년도 1학기 개강일"
                },
                {
                  "id": "schedule_uuid_2",
                  "title": "중간고사",
                  "startDate": "2026-04-15",
                  "endDate": "2026-04-21",
                  "type": "MULTI",
                  "isPrimary": true,
                  "description": "2026학년도 1학기 중간고사"
                }
              ]
            }
            """;

    public static final String SUCCESS_ADMIN_ACADEMIC_SCHEDULE = """
            {
              "success": true,
              "data": {
                "id": "schedule_uuid",
                "title": "중간고사",
                "startDate": "2026-04-15",
                "endDate": "2026-04-21",
                "type": "MULTI",
                "isPrimary": true,
                "description": "2026학년도 1학기 중간고사"
              }
            }
            """;

    public static final String SUCCESS_ADMIN_ACADEMIC_SCHEDULE_BULK_SYNC = """
            {
              "success": true,
              "data": {
                "scopeStartDate": "2026-03-01",
                "scopeEndDate": "2027-02-28",
                "created": 12,
                "updated": 37,
                "deleted": 5
              }
            }
            """;

    public static final String SUCCESS_ADMIN_COURSE_BULK = """
            {
              "success": true,
              "data": {
                "semester": "2026-1",
                "created": 120,
                "updated": 5,
                "deleted": 3
              }
            }
            """;

    public static final String ERROR_ADMIN_COURSE_BULK_CONFLICT =
            "{\"success\":false,\"message\":\"강의 bulk 처리 중 충돌이 발생했습니다.\",\"errorCode\":\"CONFLICT\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String ERROR_ADMIN_ONLINE_COURSE_SCHEDULE_NOT_EMPTY =
            "{\"success\":false,\"message\":\"온라인 강의는 schedule을 비워야 합니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-29T10:00:00\"}";

    public static final String SUCCESS_ADMIN_COURSE_DELETE = """
            {
              "success": true,
              "data": {
                "semester": "2026-1",
                "created": 0,
                "updated": 0,
                "deleted": 125
              }
            }
            """;

    public static final String ERROR_COURSE_NOT_FOUND =
            "{\"success\":false,\"message\":\"강의를 찾을 수 없습니다.\",\"errorCode\":\"COURSE_NOT_FOUND\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String ERROR_TIMETABLE_CONFLICT =
            "{\"success\":false,\"message\":\"시간이 겹치는 강의가 이미 시간표에 있습니다.\",\"errorCode\":\"TIMETABLE_CONFLICT\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String ERROR_COURSE_ALREADY_IN_TIMETABLE =
            "{\"success\":false,\"message\":\"이미 시간표에 추가된 강의입니다.\",\"errorCode\":\"COURSE_ALREADY_IN_TIMETABLE\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String ERROR_MANUAL_COURSE_DEPARTMENT_UNSUPPORTED =
            "{\"success\":false,\"message\":\"지원하지 않는 department입니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-08-14T10:00:00\"}";

    public static final String ERROR_ACADEMIC_SCHEDULE_NOT_FOUND =
            "{\"success\":false,\"message\":\"학사 일정을 찾을 수 없습니다.\",\"errorCode\":\"ACADEMIC_SCHEDULE_NOT_FOUND\",\"timestamp\":\"2026-03-07T14:00:00\"}";

    public static final String ERROR_ADMIN_ACADEMIC_SCHEDULE_BULK_SCOPE_INVALID =
            "{\"success\":false,\"message\":\"scopeStartDate는 scopeEndDate보다 늦을 수 없습니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-29T10:00:00\"}";

    public static final String ERROR_ADMIN_ACADEMIC_SCHEDULE_BULK_DUPLICATE =
            "{\"success\":false,\"message\":\"같은 title/startDate/endDate/type 조합이 요청에 중복되었습니다: 입학식 / 개강|2026-03-03|2026-03-03|SINGLE\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-29T10:00:00\"}";

    public static final String ERROR_ADMIN_ACADEMIC_SCHEDULE_BULK_OUT_OF_SCOPE =
            "{\"success\":false,\"message\":\"모든 일정은 scopeStartDate와 scopeEndDate 범위 안에 있어야 합니다.\",\"errorCode\":\"VALIDATION_ERROR\",\"timestamp\":\"2026-03-29T10:00:00\"}";
}

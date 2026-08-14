-- 학과 master/FK 및 강의 category migration 정의
--
-- 이 파일은 procedure만 정의하며 migration을 자동 실행하지 않는다.
-- 정의 후 환경에 맞는 기대 건수로 직접 CALL해야 한다.
--
-- 로컬: CALL migrate_department_course(-1);
--   - 현재 정규화 대상 건수를 허용하되, 지원하지 않는 category/학과는 차단한다.
-- 운영: CALL migrate_department_course(1048);
--   - 2026-08-14 운영 사전 조회 결과와 정확히 일치할 때만 진행한다.
--
-- MySQL DDL은 transaction rollback 대상이 아니므로 실행 전 백업과 앱 중지가 필수다.

DROP PROCEDURE IF EXISTS migrate_department_course;

DELIMITER //

CREATE PROCEDURE migrate_department_course(IN p_expected_category_updates INT)
main: BEGIN
    DECLARE v_required_table_count INT DEFAULT 0;
    DECLARE v_required_column_count INT DEFAULT 0;
    DECLARE v_departments_exists INT DEFAULT 0;
    DECLARE v_manual_department_exists INT DEFAULT 0;
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_unsupported_count INT DEFAULT 0;
    DECLARE v_category_update_count INT DEFAULT 0;
    DECLARE v_updated_count INT DEFAULT 0;
    DECLARE v_member_charset VARCHAR(64);
    DECLARE v_member_collation VARCHAR(64);
    DECLARE v_member_length BIGINT;
    DECLARE v_chat_charset VARCHAR(64);
    DECLARE v_chat_collation VARCHAR(64);
    DECLARE v_chat_length BIGINT;
    DECLARE v_manual_charset VARCHAR(64);
    DECLARE v_manual_collation VARCHAR(64);
    DECLARE v_manual_length BIGINT;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '대상 database를 먼저 선택해야 합니다.';
    END IF;

    IF p_expected_category_updates IS NULL
       OR p_expected_category_updates < -1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'expected category updates는 -1 이상이어야 합니다.';
    END IF;

    SELECT COUNT(*)
    INTO v_required_table_count
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name IN (
        'members',
        'chat_rooms',
        'courses',
        'user_timetable_manual_courses'
      );

    IF v_required_table_count <> 4 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '필수 legacy table 4개를 먼저 준비해야 합니다.';
    END IF;

    SELECT COUNT(*)
    INTO v_required_column_count
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND (
        (table_name = 'members' AND column_name = 'department')
        OR (table_name = 'chat_rooms' AND column_name = 'department')
        OR (table_name = 'courses' AND column_name = 'category')
      );

    IF v_required_column_count <> 3 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '필수 legacy column 3개를 먼저 준비해야 합니다.';
    END IF;

    SELECT character_set_name, collation_name, character_maximum_length
    INTO v_member_charset, v_member_collation, v_member_length
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'members'
      AND column_name = 'department';

    SELECT character_set_name, collation_name, character_maximum_length
    INTO v_chat_charset, v_chat_collation, v_chat_length
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_rooms'
      AND column_name = 'department';

    IF v_member_charset IS NULL
       OR v_member_collation IS NULL
       OR v_member_length <> 50
       OR v_chat_charset <> v_member_charset
       OR v_chat_collation <> v_member_collation
       OR v_chat_length <> v_member_length THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'members/chat_rooms department schema가 서로 다릅니다.';
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name IN ('members', 'chat_rooms', 'user_timetable_manual_courses')
      AND column_name = 'department'
      AND referenced_table_name IS NOT NULL
      AND NOT (
        referenced_table_name = 'departments'
        AND referenced_column_name = 'name'
      );

    IF v_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'department column에 예상하지 않은 FK가 있습니다.';
    END IF;

    SELECT COUNT(*)
    INTO v_category_update_count
    FROM courses
    WHERE OCTET_LENGTH(category) <> OCTET_LENGTH(TRIM(category))
       OR TRIM(category) IN ('전선', '전필', '교선', '교필');

    IF p_expected_category_updates >= 0
       AND v_category_update_count <> p_expected_category_updates THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'category 정규화 대상 건수가 기대값과 다릅니다.';
    END IF;

    SELECT COUNT(*)
    INTO v_unsupported_count
    FROM courses
    WHERE category IS NULL
       OR TRIM(category) = ''
       OR TRIM(category) NOT IN (
         '전선', '전필', '교선', '교필',
         '전공선택', '전공필수', '교양선택', '교양필수', '교직'
       );

    IF v_unsupported_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '지원하지 않는 course category가 있습니다.';
    END IF;

    SELECT COUNT(*)
    INTO v_departments_exists
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'departments';

    SELECT COUNT(*)
    INTO v_manual_department_exists
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'user_timetable_manual_courses'
      AND column_name = 'department';

    IF v_departments_exists = 1 THEN
        SELECT COUNT(*)
        INTO v_count
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'departments'
          AND column_name IN ('name', 'active', 'display_order');

        IF v_count <> 3 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = '기존 departments table schema가 예상과 다릅니다.';
        END IF;

        SELECT COUNT(*)
        INTO v_count
        FROM information_schema.key_column_usage
        WHERE table_schema = DATABASE()
          AND table_name = 'departments'
          AND column_name = 'name'
          AND constraint_name = 'PRIMARY';

        IF v_count <> 1 THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'departments.name이 PRIMARY KEY가 아닙니다.';
        END IF;

        SELECT character_set_name, collation_name, character_maximum_length
        INTO v_manual_charset, v_manual_collation, v_manual_length
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'departments'
          AND column_name = 'name';

        IF v_manual_charset <> v_member_charset
           OR v_manual_collation <> v_member_collation
           OR v_manual_length <> v_member_length THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'departments.name schema가 참조 column과 다릅니다.';
        END IF;
    END IF;

    IF v_manual_department_exists = 1 THEN
        SELECT character_set_name, collation_name, character_maximum_length
        INTO v_manual_charset, v_manual_collation, v_manual_length
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'user_timetable_manual_courses'
          AND column_name = 'department';

        IF v_manual_charset <> v_member_charset
           OR v_manual_collation <> v_member_collation
           OR v_manual_length <> v_member_length THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'manual course department schema가 참조 column과 다릅니다.';
        END IF;
    END IF;

    DROP TEMPORARY TABLE IF EXISTS skuri_department_course_migration_expected;
    SET @migration_sql = CONCAT(
      'CREATE TEMPORARY TABLE skuri_department_course_migration_expected (',
      'name VARCHAR(50) CHARACTER SET ', v_member_charset,
      ' COLLATE ', v_member_collation, ' NOT NULL,',
      'display_order INT NOT NULL,',
      'PRIMARY KEY (name)',
      ') ENGINE=MEMORY'
    );
    PREPARE migration_stmt FROM @migration_sql;
    EXECUTE migration_stmt;
    DEALLOCATE PREPARE migration_stmt;

    INSERT INTO skuri_department_course_migration_expected (name, display_order) VALUES
    ('신학과', 1),
    ('기독교교육상담학과', 2),
    ('문화선교학과', 3),
    ('영어영문학과', 4),
    ('중어중문학과', 5),
    ('국어국문학과', 6),
    ('사회복지학과', 7),
    ('국제개발협력학과', 8),
    ('행정학과', 9),
    ('관광학과', 10),
    ('경영학과', 11),
    ('글로벌물류학과', 12),
    ('산업경영공학과', 13),
    ('유아교육과', 14),
    ('체육교육과', 15),
    ('교직부', 16),
    ('컴퓨터공학과', 17),
    ('정보통신공학과', 18),
    ('미디어소프트웨어학과', 19),
    ('도시디자인정보공학과', 20),
    ('음악학부', 21),
    ('실용음악과', 22),
    ('공연음악예술학부', 23),
    ('연기예술학과', 24),
    ('영화영상학과', 25),
    ('연극영화학부', 26),
    ('뷰티디자인학과', 27),
    ('융합학부', 28),
    ('파이데이아학부', 29);

    IF v_departments_exists = 1 THEN
        SELECT COUNT(*)
        INTO v_unsupported_count
        FROM members m
        WHERE m.department IS NOT NULL
          AND m.department <> '소프트웨어학과'
          AND NOT EXISTS (
            SELECT 1
            FROM skuri_department_course_migration_expected e
            WHERE e.name = m.department
          )
          AND NOT EXISTS (
            SELECT 1 FROM departments d WHERE d.name = m.department
          );

        SELECT v_unsupported_count + COUNT(*)
        INTO v_unsupported_count
        FROM chat_rooms c
        WHERE c.department IS NOT NULL
          AND c.department <> '소프트웨어학과'
          AND NOT EXISTS (
            SELECT 1
            FROM skuri_department_course_migration_expected e
            WHERE e.name = c.department
          )
          AND NOT EXISTS (
            SELECT 1 FROM departments d WHERE d.name = c.department
          );
    ELSE
        SELECT COUNT(*)
        INTO v_unsupported_count
        FROM members m
        WHERE m.department IS NOT NULL
          AND m.department <> '소프트웨어학과'
          AND NOT EXISTS (
            SELECT 1
            FROM skuri_department_course_migration_expected e
            WHERE e.name = m.department
          );

        SELECT v_unsupported_count + COUNT(*)
        INTO v_unsupported_count
        FROM chat_rooms c
        WHERE c.department IS NOT NULL
          AND c.department <> '소프트웨어학과'
          AND NOT EXISTS (
            SELECT 1
            FROM skuri_department_course_migration_expected e
            WHERE e.name = c.department
          );
    END IF;

    IF v_manual_department_exists = 1 THEN
        IF v_departments_exists = 1 THEN
            SELECT v_unsupported_count + COUNT(*)
            INTO v_unsupported_count
            FROM user_timetable_manual_courses mc
            WHERE mc.department IS NOT NULL
              AND mc.department <> '소프트웨어학과'
              AND NOT EXISTS (
                SELECT 1
                FROM skuri_department_course_migration_expected e
                WHERE e.name = mc.department
              )
              AND NOT EXISTS (
                SELECT 1 FROM departments d WHERE d.name = mc.department
              );
        ELSE
            SELECT v_unsupported_count + COUNT(*)
            INTO v_unsupported_count
            FROM user_timetable_manual_courses mc
            WHERE mc.department IS NOT NULL
              AND mc.department <> '소프트웨어학과'
              AND NOT EXISTS (
                SELECT 1
                FROM skuri_department_course_migration_expected e
                WHERE e.name = mc.department
              );
        END IF;
    END IF;

    IF v_unsupported_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'canonical master로 해석할 수 없는 학과가 있습니다.';
    END IF;

    IF v_departments_exists = 0 THEN
        SET @migration_sql = CONCAT(
          'CREATE TABLE departments (',
          'name VARCHAR(50) CHARACTER SET ', v_member_charset,
          ' COLLATE ', v_member_collation, ' NOT NULL,',
          'active BOOLEAN NOT NULL DEFAULT TRUE,',
          'display_order INT NOT NULL,',
          'PRIMARY KEY (name),',
          'INDEX idx_departments_active_order (active, display_order, name)',
          ') ENGINE=InnoDB DEFAULT CHARACTER SET ', v_member_charset,
          ' COLLATE ', v_member_collation
        );
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;

    INSERT IGNORE INTO departments (name, active, display_order)
    SELECT name, TRUE, display_order
    FROM skuri_department_course_migration_expected;

    IF v_manual_department_exists = 0 THEN
        SET @migration_sql = CONCAT(
          'ALTER TABLE user_timetable_manual_courses ',
          'ADD COLUMN department VARCHAR(50) CHARACTER SET ', v_member_charset,
          ' COLLATE ', v_member_collation, ' NULL AFTER professor'
        );
        PREPARE migration_stmt FROM @migration_sql;
        EXECUTE migration_stmt;
        DEALLOCATE PREPARE migration_stmt;
    END IF;

    START TRANSACTION;

    UPDATE members
    SET department = '미디어소프트웨어학과'
    WHERE department = '소프트웨어학과';

    UPDATE chat_rooms
    SET department = '미디어소프트웨어학과'
    WHERE department = '소프트웨어학과';

    UPDATE user_timetable_manual_courses
    SET department = '미디어소프트웨어학과'
    WHERE department = '소프트웨어학과';

    UPDATE courses
    SET category = CASE TRIM(category)
        WHEN '전선' THEN '전공선택'
        WHEN '전필' THEN '전공필수'
        WHEN '교선' THEN '교양선택'
        WHEN '교필' THEN '교양필수'
        ELSE TRIM(category)
    END
    WHERE OCTET_LENGTH(category) <> OCTET_LENGTH(TRIM(category))
       OR TRIM(category) IN ('전선', '전필', '교선', '교필');

    SET v_updated_count = ROW_COUNT();

    IF v_updated_count <> v_category_update_count THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'migration 중 category 대상 건수가 변경되었습니다.';
    END IF;

    SELECT COUNT(*)
    INTO v_unsupported_count
    FROM members m
    LEFT JOIN departments d ON d.name = m.department
    WHERE m.department IS NOT NULL
      AND d.name IS NULL;

    SELECT v_unsupported_count + COUNT(*)
    INTO v_unsupported_count
    FROM chat_rooms c
    LEFT JOIN departments d ON d.name = c.department
    WHERE c.department IS NOT NULL
      AND d.name IS NULL;

    SELECT v_unsupported_count + COUNT(*)
    INTO v_unsupported_count
    FROM user_timetable_manual_courses mc
    LEFT JOIN departments d ON d.name = mc.department
    WHERE mc.department IS NOT NULL
      AND d.name IS NULL;

    IF v_unsupported_count > 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = '학과 정규화 후에도 orphan이 남았습니다.';
    END IF;

    COMMIT;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'user_timetable_manual_courses'
      AND column_name = 'department'
      AND seq_in_index = 1;

    IF v_count = 0 THEN
        CREATE INDEX idx_user_timetable_manual_courses_department
            ON user_timetable_manual_courses(department);
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'members'
      AND column_name = 'department'
      AND referenced_table_name = 'departments'
      AND referenced_column_name = 'name';

    IF v_count = 0 THEN
        ALTER TABLE members
            ADD CONSTRAINT fk_members_department
            FOREIGN KEY (department) REFERENCES departments(name)
            ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'chat_rooms'
      AND column_name = 'department'
      AND referenced_table_name = 'departments'
      AND referenced_column_name = 'name';

    IF v_count = 0 THEN
        ALTER TABLE chat_rooms
            ADD CONSTRAINT fk_chat_rooms_department
            FOREIGN KEY (department) REFERENCES departments(name)
            ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;

    SELECT COUNT(*)
    INTO v_count
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'user_timetable_manual_courses'
      AND column_name = 'department'
      AND referenced_table_name = 'departments'
      AND referenced_column_name = 'name';

    IF v_count = 0 THEN
        ALTER TABLE user_timetable_manual_courses
            ADD CONSTRAINT fk_user_timetable_manual_courses_department
            FOREIGN KEY (department) REFERENCES departments(name)
            ON UPDATE RESTRICT ON DELETE RESTRICT;
    END IF;

    DROP TEMPORARY TABLE skuri_department_course_migration_expected;

    SELECT DATABASE() AS migrated_database,
           v_updated_count AS normalized_category_rows,
           (SELECT COUNT(*) FROM departments) AS department_rows;
END//

DELIMITER ;

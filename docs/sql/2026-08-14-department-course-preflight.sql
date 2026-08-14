-- 학과 master/FK 및 강의 category migration 사전 점검
-- 읽기 전용 쿼리만 포함한다.
-- mysql client에서 대상 schema를 선택한 뒤 SOURCE로 실행한다.

SELECT DATABASE() AS target_database, VERSION() AS mysql_version;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'members',
    'chat_rooms',
    'courses',
    'user_timetable_manual_courses',
    'departments'
  )
ORDER BY table_name;

SELECT table_name,
       column_name,
       column_type,
       character_set_name,
       collation_name,
       is_nullable
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
    (table_name = 'members' AND column_name = 'department')
    OR (table_name = 'chat_rooms' AND column_name = 'department')
    OR (table_name = 'user_timetable_manual_courses' AND column_name = 'department')
    OR (table_name = 'courses' AND column_name = 'category')
    OR table_name = 'departments'
  )
ORDER BY table_name, ordinal_position;

SELECT category, COUNT(*) AS count
FROM courses
GROUP BY category
ORDER BY category;

SELECT COUNT(*) AS category_rows_to_normalize
FROM courses
WHERE OCTET_LENGTH(category) <> OCTET_LENGTH(TRIM(category))
   OR TRIM(category) IN ('전선', '전필', '교선', '교필');

SELECT category, COUNT(*) AS unsupported_category_count
FROM courses
WHERE category IS NULL
   OR TRIM(category) = ''
   OR TRIM(category) NOT IN (
     '전선', '전필', '교선', '교필',
     '전공선택', '전공필수', '교양선택', '교양필수', '교직'
   )
GROUP BY category
ORDER BY category;

SELECT department, COUNT(*) AS count
FROM members
GROUP BY department
ORDER BY department;

SELECT department, COUNT(*) AS count
FROM chat_rooms
GROUP BY department
ORDER BY department;

SELECT table_name,
       constraint_name,
       column_name,
       referenced_table_name,
       referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND column_name = 'department'
  AND table_name IN (
    'members',
    'chat_rooms',
    'user_timetable_manual_courses'
  )
ORDER BY table_name, constraint_name;

-- 학과 master/FK 및 강의 category migration 사후 점검
-- migration을 적용한 대상 schema에서 실행한다.

SELECT DATABASE() AS target_database, VERSION() AS mysql_version;

SELECT name, active, display_order
FROM departments
ORDER BY display_order, name;

SELECT category, COUNT(*) AS count
FROM courses
GROUP BY category
ORDER BY category;

SELECT COUNT(*) AS remaining_category_rows_to_normalize
FROM courses
WHERE OCTET_LENGTH(category) <> OCTET_LENGTH(TRIM(category))
   OR TRIM(category) IN ('전선', '전필', '교선', '교필');

SELECT m.department, COUNT(*) AS orphan_count
FROM members m
LEFT JOIN departments d ON d.name = m.department
WHERE m.department IS NOT NULL
  AND d.name IS NULL
GROUP BY m.department;

SELECT c.department, COUNT(*) AS orphan_count
FROM chat_rooms c
LEFT JOIN departments d ON d.name = c.department
WHERE c.department IS NOT NULL
  AND d.name IS NULL
GROUP BY c.department;

SELECT mc.department, COUNT(*) AS orphan_count
FROM user_timetable_manual_courses mc
LEFT JOIN departments d ON d.name = mc.department
WHERE mc.department IS NOT NULL
  AND d.name IS NULL
GROUP BY mc.department;

SELECT table_name,
       constraint_name,
       column_name,
       referenced_table_name,
       referenced_column_name
FROM information_schema.key_column_usage
WHERE table_schema = DATABASE()
  AND column_name = 'department'
  AND referenced_table_name = 'departments'
  AND referenced_column_name = 'name'
  AND table_name IN (
    'members',
    'chat_rooms',
    'user_timetable_manual_courses'
  )
ORDER BY table_name;

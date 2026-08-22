-- Friend Core 출시 준비 운영 DB 사전 점검
-- 읽기 전용 SELECT만 포함한다. cleanup 실행 전에 결과 건수를 기록한다.
-- 프로필 완료의 공백 판정은 Java String.isBlank()와 동일하다.
-- 친구 코드 건수는 미완료 회원의 소유 ACTIVE 코드와 첫 출시 전 전체 RETIRED 코드를 합산한다.

SELECT DATABASE() AS target_database, VERSION() AS mysql_version;

SELECT table_name
FROM information_schema.tables
WHERE table_schema = DATABASE()
  AND table_name IN (
    'members',
    'friend_profiles',
    'friend_code_registry',
    'friend_requests',
    'friendships',
    'friend_preferences',
    'member_blocks'
  )
ORDER BY table_name;

SELECT table_name, column_name, column_type, is_nullable, column_key, column_default
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
    (table_name = 'members' AND column_name IN ('id', 'status', 'nickname', 'nickname_key', 'student_id', 'department'))
    OR (table_name = 'friend_profiles' AND column_name IN ('member_id', 'active_friend_code_id', 'nickname_searchable'))
    OR (table_name = 'friend_code_registry' AND column_name IN ('id', 'owner_member_id', 'status'))
  )
ORDER BY table_name, ordinal_position;

-- 아래 target 정의는 cleanup procedure와 동일해야 한다.
WITH incomplete AS (
    SELECT m.id AS member_id
    FROM members m
    WHERE m.status = 'ACTIVE'
      AND (
        m.nickname IS NULL
        OR REGEXP_REPLACE(m.nickname, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
        OR m.student_id IS NULL
        OR REGEXP_REPLACE(m.student_id, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
        OR m.department IS NULL
        OR REGEXP_REPLACE(m.department, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
      )
)
SELECT
    (SELECT COUNT(*) FROM incomplete) AS incomplete_active_members,
    (SELECT COUNT(*) FROM friend_profiles p JOIN incomplete t ON t.member_id = p.member_id) AS incomplete_friend_profiles,
    (SELECT COUNT(*) FROM friend_code_registry c WHERE c.status = 'RETIRED' OR EXISTS (SELECT 1 FROM incomplete t WHERE t.member_id = c.owner_member_id)) AS incomplete_active_and_pre_release_retired_friend_codes,
    (SELECT COUNT(*) FROM friend_requests r WHERE EXISTS (SELECT 1 FROM incomplete t WHERE t.member_id = r.requester_id OR t.member_id = r.recipient_id)) AS incomplete_related_friend_requests,
    (SELECT COUNT(*) FROM friendships f WHERE EXISTS (SELECT 1 FROM incomplete t WHERE t.member_id = f.member_low_id OR t.member_id = f.member_high_id)) AS incomplete_related_friendships,
    (SELECT COUNT(*) FROM friend_preferences p WHERE EXISTS (SELECT 1 FROM incomplete t WHERE t.member_id = p.owner_member_id OR t.member_id = p.friend_member_id)) AS incomplete_related_friend_preferences,
    (SELECT COUNT(*) FROM member_blocks b WHERE EXISTS (SELECT 1 FROM incomplete t WHERE t.member_id = b.blocker_id OR t.member_id = b.blocked_id)) AS incomplete_related_member_blocks;

-- cleanup 대상 확인. 실제 운영 결과를 외부 문서나 PR에 복사하지 않는다.
WITH incomplete AS (
    SELECT m.id AS member_id
    FROM members m
    WHERE m.status = 'ACTIVE'
      AND (
        m.nickname IS NULL
        OR REGEXP_REPLACE(m.nickname, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
        OR m.student_id IS NULL
        OR REGEXP_REPLACE(m.student_id, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
        OR m.department IS NULL
        OR REGEXP_REPLACE(m.department, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
      )
)
SELECT m.id, m.nickname, m.student_id, m.department,
       p.public_id, p.active_friend_code_id,
       c.status AS active_code_status
FROM incomplete t
JOIN members m ON m.id = t.member_id
LEFT JOIN friend_profiles p ON p.member_id = t.member_id
LEFT JOIN friend_code_registry c ON c.owner_member_id = t.member_id
ORDER BY m.id;

-- 기존 ACTIVE 중복 닉네임은 임의 변경하지 않는다. 이 결과는 현황 확인용이다.
SELECT LOWER(TRIM(nickname)) AS legacy_nickname_key, COUNT(*) AS active_member_count
FROM members
WHERE status = 'ACTIVE'
  AND nickname IS NOT NULL
  AND TRIM(nickname) <> ''
GROUP BY LOWER(TRIM(nickname))
HAVING COUNT(*) > 1
ORDER BY active_member_count DESC, legacy_nickname_key;

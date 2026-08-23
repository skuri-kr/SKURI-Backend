-- Friend Core 출시 준비 운영 DB 사후 점검
-- cleanup과 신규 서버의 프로필 완료 회원 backfill이 끝난 뒤 실행한다.
-- 프로필 완료의 공백 판정은 Java String.isBlank()와 동일하다.

SELECT DATABASE() AS target_database, VERSION() AS mysql_version;

-- 미완료 ACTIVE 회원과 연결된 Friend 파생 데이터와 첫 출시 전 RETIRED 코드는 모두 0이어야 한다.
WITH incomplete_members AS (
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
    (SELECT COUNT(*) FROM friend_profiles p JOIN incomplete_members t ON t.member_id = p.member_id) AS incomplete_friend_profiles,
    (SELECT COUNT(*) FROM friend_code_registry c JOIN incomplete_members t ON t.member_id = c.owner_member_id) AS incomplete_friend_codes,
    (SELECT COUNT(*) FROM friend_code_registry c WHERE c.status = 'RETIRED') AS remaining_pre_release_retired_friend_codes,
    (SELECT COUNT(*) FROM friend_requests r WHERE EXISTS (SELECT 1 FROM incomplete_members t WHERE t.member_id = r.requester_id OR t.member_id = r.recipient_id)) AS incomplete_friend_requests,
    (SELECT COUNT(*) FROM friendships f WHERE EXISTS (SELECT 1 FROM incomplete_members t WHERE t.member_id = f.member_low_id OR t.member_id = f.member_high_id)) AS incomplete_friendships,
    (SELECT COUNT(*) FROM friend_preferences p WHERE EXISTS (SELECT 1 FROM incomplete_members t WHERE t.member_id = p.owner_member_id OR t.member_id = p.friend_member_id)) AS incomplete_friend_preferences,
    (SELECT COUNT(*) FROM member_blocks b WHERE EXISTS (SELECT 1 FROM incomplete_members t WHERE t.member_id = b.blocker_id OR t.member_id = b.blocked_id)) AS incomplete_member_blocks;

-- 프로필 완료 ACTIVE 회원의 FriendProfile 누락은 0이어야 한다.
SELECT COUNT(*) AS complete_active_members_without_friend_profile
FROM members m
LEFT JOIN friend_profiles p ON p.member_id = m.id
WHERE m.status = 'ACTIVE'
  AND m.nickname IS NOT NULL
  AND REGEXP_REPLACE(m.nickname, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') <> ''
  AND m.student_id IS NOT NULL
  AND REGEXP_REPLACE(m.student_id, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') <> ''
  AND m.department IS NOT NULL
  AND REGEXP_REPLACE(m.department, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') <> ''
  AND p.member_id IS NULL;

-- 모든 FriendProfile은 완료 ACTIVE 회원과 자기 소유 ACTIVE 코드를 참조해야 한다.
SELECT COUNT(*) AS invalid_friend_profile_references
FROM friend_profiles p
LEFT JOIN members m ON m.id = p.member_id
LEFT JOIN friend_code_registry c ON c.id = p.active_friend_code_id
WHERE m.id IS NULL
   OR m.status <> 'ACTIVE'
   OR m.nickname IS NULL
   OR REGEXP_REPLACE(m.nickname, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
   OR m.student_id IS NULL
   OR REGEXP_REPLACE(m.student_id, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
   OR m.department IS NULL
   OR REGEXP_REPLACE(m.department, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
   OR c.id IS NULL
   OR c.status <> 'ACTIVE'
   OR c.owner_member_id <> p.member_id;

-- nickname_key는 nullable이지만 non-null 값은 중복이 없어야 한다.
SELECT nickname_key, COUNT(*) AS count
FROM members
WHERE nickname_key IS NOT NULL
GROUP BY nickname_key
HAVING COUNT(*) > 1;

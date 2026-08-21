-- Friend Core 출시 준비 운영 DB 사후 점검
-- cleanup과 신규 서버의 프로필 완료 회원 backfill이 끝난 뒤 실행한다.

SELECT DATABASE() AS target_database, VERSION() AS mysql_version;

-- 미완료 ACTIVE 회원과 연결된 Friend 파생 데이터는 모두 0이어야 한다.
DROP TEMPORARY TABLE IF EXISTS skuri_incomplete_friend_members_postcheck;
CREATE TEMPORARY TABLE skuri_incomplete_friend_members_postcheck (
    member_id VARCHAR(36) PRIMARY KEY
) ENGINE=MEMORY AS
SELECT m.id AS member_id
FROM members m
WHERE m.status = 'ACTIVE'
  AND (
    m.nickname IS NULL
    OR TRIM(m.nickname) = ''
    OR REPLACE(LOWER(TRIM(m.nickname)), ' ', '') LIKE '%스쿠리유저%'
    OR REPLACE(LOWER(TRIM(m.nickname)), ' ', '') LIKE '%운영자%'
    OR m.student_id IS NULL
    OR TRIM(m.student_id) = ''
    OR m.department IS NULL
    OR TRIM(m.department) = ''
  );

SELECT
    (SELECT COUNT(*) FROM friend_profiles p JOIN skuri_incomplete_friend_members_postcheck t ON t.member_id = p.member_id) AS incomplete_friend_profiles,
    (SELECT COUNT(*) FROM friend_code_registry c JOIN skuri_incomplete_friend_members_postcheck t ON t.member_id = c.owner_member_id) AS incomplete_friend_codes,
    (SELECT COUNT(*) FROM friend_requests r WHERE EXISTS (SELECT 1 FROM skuri_incomplete_friend_members_postcheck t WHERE t.member_id = r.requester_id OR t.member_id = r.recipient_id)) AS incomplete_friend_requests,
    (SELECT COUNT(*) FROM friendships f WHERE EXISTS (SELECT 1 FROM skuri_incomplete_friend_members_postcheck t WHERE t.member_id = f.member_low_id OR t.member_id = f.member_high_id)) AS incomplete_friendships,
    (SELECT COUNT(*) FROM friend_preferences p WHERE EXISTS (SELECT 1 FROM skuri_incomplete_friend_members_postcheck t WHERE t.member_id = p.owner_member_id OR t.member_id = p.friend_member_id)) AS incomplete_friend_preferences,
    (SELECT COUNT(*) FROM member_blocks b WHERE EXISTS (SELECT 1 FROM skuri_incomplete_friend_members_postcheck t WHERE t.member_id = b.blocker_id OR t.member_id = b.blocked_id)) AS incomplete_member_blocks;

-- 프로필 완료 ACTIVE 회원의 FriendProfile 누락은 0이어야 한다.
SELECT COUNT(*) AS complete_active_members_without_friend_profile
FROM members m
LEFT JOIN friend_profiles p ON p.member_id = m.id
WHERE m.status = 'ACTIVE'
  AND m.nickname IS NOT NULL
  AND TRIM(m.nickname) <> ''
  AND REPLACE(LOWER(TRIM(m.nickname)), ' ', '') NOT LIKE '%스쿠리유저%'
  AND REPLACE(LOWER(TRIM(m.nickname)), ' ', '') NOT LIKE '%운영자%'
  AND m.student_id IS NOT NULL
  AND TRIM(m.student_id) <> ''
  AND m.department IS NOT NULL
  AND TRIM(m.department) <> ''
  AND p.member_id IS NULL;

-- 모든 FriendProfile은 완료 ACTIVE 회원과 자기 소유 ACTIVE 코드를 참조해야 한다.
SELECT COUNT(*) AS invalid_friend_profile_references
FROM friend_profiles p
LEFT JOIN members m ON m.id = p.member_id
LEFT JOIN friend_code_registry c ON c.id = p.active_friend_code_id
WHERE m.id IS NULL
   OR m.status <> 'ACTIVE'
   OR m.nickname IS NULL
   OR TRIM(m.nickname) = ''
   OR REPLACE(LOWER(TRIM(m.nickname)), ' ', '') LIKE '%스쿠리유저%'
   OR REPLACE(LOWER(TRIM(m.nickname)), ' ', '') LIKE '%운영자%'
   OR m.student_id IS NULL
   OR TRIM(m.student_id) = ''
   OR m.department IS NULL
   OR TRIM(m.department) = ''
   OR c.id IS NULL
   OR c.status <> 'ACTIVE'
   OR c.owner_member_id <> p.member_id;

-- nickname_key는 nullable이지만 non-null 값은 중복이 없어야 한다.
SELECT nickname_key, COUNT(*) AS count
FROM members
WHERE nickname_key IS NOT NULL
GROUP BY nickname_key
HAVING COUNT(*) > 1;

DROP TEMPORARY TABLE IF EXISTS skuri_incomplete_friend_members_postcheck;

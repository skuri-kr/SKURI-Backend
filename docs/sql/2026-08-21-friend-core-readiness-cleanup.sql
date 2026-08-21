-- Friend Core 출시 준비 운영 DB 일회성 cleanup
-- 이 파일은 procedure만 정의하며 자동 실행하지 않는다.
-- 신규 서버 배포 후, preflight의 정확한 7개 건수를 인자로 전달해 직접 CALL한다.
-- 친구 코드 건수에는 미완료 회원의 소유 ACTIVE 코드와 첫 출시 전 전체 RETIRED 코드가 함께 포함된다.
-- 프로필 완료의 공백 판정은 Java String.isBlank()와 동일하다.
-- 예: CALL cleanup_incomplete_friend_data(10, 8, 8, 2, 1, 2, 1);

DROP PROCEDURE IF EXISTS cleanup_incomplete_friend_data;

DELIMITER //

CREATE PROCEDURE cleanup_incomplete_friend_data(
    IN p_expected_members INT,
    IN p_expected_profiles INT,
    IN p_expected_codes INT,
    IN p_expected_requests INT,
    IN p_expected_friendships INT,
    IN p_expected_preferences INT,
    IN p_expected_blocks INT
)
main: BEGIN
    DECLARE v_required_table_count INT DEFAULT 0;
    DECLARE v_count INT DEFAULT 0;
    DECLARE v_deleted INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS skuri_incomplete_friend_members_cleanup;
        RESIGNAL;
    END;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '대상 database를 먼저 선택해야 합니다.';
    END IF;

    IF p_expected_members IS NULL OR p_expected_members < 0
       OR p_expected_profiles IS NULL OR p_expected_profiles < 0
       OR p_expected_codes IS NULL OR p_expected_codes < 0
       OR p_expected_requests IS NULL OR p_expected_requests < 0
       OR p_expected_friendships IS NULL OR p_expected_friendships < 0
       OR p_expected_preferences IS NULL OR p_expected_preferences < 0
       OR p_expected_blocks IS NULL OR p_expected_blocks < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '모든 기대 건수는 0 이상의 값이어야 합니다.';
    END IF;

    SELECT COUNT(*) INTO v_required_table_count
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
      );

    IF v_required_table_count <> 7 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '필수 Friend table 7개를 확인해야 합니다.';
    END IF;

    START TRANSACTION;

    DROP TEMPORARY TABLE IF EXISTS skuri_incomplete_friend_members_cleanup;
    CREATE TEMPORARY TABLE skuri_incomplete_friend_members_cleanup (
        member_id VARCHAR(36) PRIMARY KEY
    ) ENGINE=MEMORY AS
    SELECT m.id AS member_id
    FROM members m
    WHERE m.status = 'ACTIVE'
      AND (
        m.nickname IS NULL
        OR REGEXP_REPLACE(m.nickname, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
        OR REGEXP_REPLACE(LOWER(TRIM(m.nickname)), '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{00A0}\\x{1680}\\x{2000}-\\x{200A}\\x{2028}\\x{2029}\\x{202F}\\x{205F}\\x{3000}]', '') LIKE '%스쿠리유저%'
        OR REGEXP_REPLACE(LOWER(TRIM(m.nickname)), '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{00A0}\\x{1680}\\x{2000}-\\x{200A}\\x{2028}\\x{2029}\\x{202F}\\x{205F}\\x{3000}]', '') LIKE '%운영자%'
        OR m.student_id IS NULL
        OR REGEXP_REPLACE(m.student_id, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
        OR m.department IS NULL
        OR REGEXP_REPLACE(m.department, '[\\x{0009}-\\x{000D}\\x{001C}-\\x{001F}\\x{0020}\\x{1680}\\x{2000}-\\x{2006}\\x{2008}-\\x{200A}\\x{2028}\\x{2029}\\x{205F}\\x{3000}]', '') = ''
      );

    SELECT COUNT(*) INTO v_count FROM skuri_incomplete_friend_members_cleanup;
    IF v_count <> p_expected_members THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '미완료 ACTIVE 회원 건수가 preflight와 다릅니다.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM friend_profiles p
    JOIN skuri_incomplete_friend_members_cleanup t ON t.member_id = p.member_id;
    IF v_count <> p_expected_profiles THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '미완료 회원 FriendProfile 건수가 preflight와 다릅니다.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM friend_code_registry c
    WHERE c.status = 'RETIRED'
       OR EXISTS (
            SELECT 1
            FROM skuri_incomplete_friend_members_cleanup t
            WHERE t.member_id = c.owner_member_id
       );
    IF v_count <> p_expected_codes THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'cleanup 대상 친구 코드 건수가 preflight와 다릅니다.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM friend_requests r
    WHERE EXISTS (
        SELECT 1 FROM skuri_incomplete_friend_members_cleanup t
        WHERE t.member_id = r.requester_id OR t.member_id = r.recipient_id
    );
    IF v_count <> p_expected_requests THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '미완료 회원 친구 요청 건수가 preflight와 다릅니다.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM friendships f
    WHERE EXISTS (
        SELECT 1 FROM skuri_incomplete_friend_members_cleanup t
        WHERE t.member_id = f.member_low_id OR t.member_id = f.member_high_id
    );
    IF v_count <> p_expected_friendships THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '미완료 회원 friendship 건수가 preflight와 다릅니다.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM friend_preferences p
    WHERE EXISTS (
        SELECT 1 FROM skuri_incomplete_friend_members_cleanup t
        WHERE t.member_id = p.owner_member_id OR t.member_id = p.friend_member_id
    );
    IF v_count <> p_expected_preferences THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '미완료 회원 즐겨찾기 건수가 preflight와 다릅니다.';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM member_blocks b
    WHERE EXISTS (
        SELECT 1 FROM skuri_incomplete_friend_members_cleanup t
        WHERE t.member_id = b.blocker_id OR t.member_id = b.blocked_id
    );
    IF v_count <> p_expected_blocks THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '미완료 회원 차단 건수가 preflight와 다릅니다.';
    END IF;

    DELETE p FROM friend_preferences p
    WHERE EXISTS (
        SELECT 1 FROM skuri_incomplete_friend_members_cleanup t
        WHERE t.member_id = p.owner_member_id OR t.member_id = p.friend_member_id
    );
    SET v_deleted = ROW_COUNT();
    IF v_deleted <> p_expected_preferences THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '즐겨찾기 삭제 건수가 기대값과 다릅니다.';
    END IF;

    DELETE r FROM friend_requests r
    WHERE EXISTS (
        SELECT 1 FROM skuri_incomplete_friend_members_cleanup t
        WHERE t.member_id = r.requester_id OR t.member_id = r.recipient_id
    );
    SET v_deleted = ROW_COUNT();
    IF v_deleted <> p_expected_requests THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '친구 요청 삭제 건수가 기대값과 다릅니다.';
    END IF;

    DELETE f FROM friendships f
    WHERE EXISTS (
        SELECT 1 FROM skuri_incomplete_friend_members_cleanup t
        WHERE t.member_id = f.member_low_id OR t.member_id = f.member_high_id
    );
    SET v_deleted = ROW_COUNT();
    IF v_deleted <> p_expected_friendships THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'friendship 삭제 건수가 기대값과 다릅니다.';
    END IF;

    DELETE b FROM member_blocks b
    WHERE EXISTS (
        SELECT 1 FROM skuri_incomplete_friend_members_cleanup t
        WHERE t.member_id = b.blocker_id OR t.member_id = b.blocked_id
    );
    SET v_deleted = ROW_COUNT();
    IF v_deleted <> p_expected_blocks THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '차단 삭제 건수가 기대값과 다릅니다.';
    END IF;

    DELETE p FROM friend_profiles p
    JOIN skuri_incomplete_friend_members_cleanup t ON t.member_id = p.member_id;
    SET v_deleted = ROW_COUNT();
    IF v_deleted <> p_expected_profiles THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'FriendProfile 삭제 건수가 기대값과 다릅니다.';
    END IF;

    DELETE c FROM friend_code_registry c
    WHERE c.status = 'RETIRED'
       OR EXISTS (
            SELECT 1
            FROM skuri_incomplete_friend_members_cleanup t
            WHERE t.member_id = c.owner_member_id
       );
    SET v_deleted = ROW_COUNT();
    IF v_deleted <> p_expected_codes THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = '친구 코드 삭제 건수가 기대값과 다릅니다.';
    END IF;

    COMMIT;
    DROP TEMPORARY TABLE IF EXISTS skuri_incomplete_friend_members_cleanup;

    SELECT p_expected_members AS cleaned_incomplete_members,
           p_expected_profiles AS deleted_friend_profiles,
           p_expected_codes AS deleted_friend_codes,
           p_expected_requests AS deleted_friend_requests,
           p_expected_friendships AS deleted_friendships,
           p_expected_preferences AS deleted_friend_preferences,
           p_expected_blocks AS deleted_member_blocks;
END//

DELIMITER ;

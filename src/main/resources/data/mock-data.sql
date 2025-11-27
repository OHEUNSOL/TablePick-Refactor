use portfolio;

SET @@cte_max_recursion_depth = 1000001;

-- [1] 생성할 개수 설정
SET @MEMBER_COUNT = 100000;
SET @RESTAURANT_COUNT = 900000;
SET @CATEGORY_COUNT = 50;
SET @TAG_COUNT = 100;
SET @MENU_PER_RESTAURANT = 20; -- (요청사항 반영)

-- =================================================================
-- 1. Member (회원) - 10만 개
-- =================================================================
INSERT INTO member (
    email, nickname, password, gender, birthdate, phone_number,
    is_member_deleted, roles, provider, provider_id, created_at, updated_at
)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n <= @MEMBER_COUNT -- <= 로 수정
)
SELECT
    CONCAT('testuser', LPAD(n, 7, '0'), '@example.com'), -- UNIQUE 이메일
    CONCAT('테스트유저', n),
    'temp_password_hash',
    ELT(MOD(n, 2) + 1, 'MALE', 'FEMALE'),
    DATE_SUB(CURDATE(), INTERVAL FLOOR(7000 + RAND() * 20000) DAY),
    CONCAT('010-', LPAD(MOD(n, 9000) + 1000, 4, '0'), '-', LPAD(MOD(n+10, 9000) + 1000, 4, '0')),
    0, 'USER', 'KAKAO', CONCAT('provider_', n),
    NOW() - INTERVAL FLOOR(RAND() * 365) DAY,
    NOW() - INTERVAL FLOOR(RAND() * 365) DAY
FROM numbers;

-- =================================================================
-- 2. RestaurantCategory (카테고리) - 50개
-- =================================================================
INSERT INTO restaurant_category (name)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n <= @CATEGORY_COUNT
)
SELECT
    CONCAT('카테고리_', LPAD(n, 2, '0')) -- '카테고리_01', '카테고리_02', ...
FROM numbers;

-- =================================================================
-- 3. Tag (태그) - 100개
-- =================================================================
INSERT INTO tag (name)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n <= @TAG_COUNT
)
SELECT
    CONCAT('편의시설_태그_', LPAD(n, 3, '0')) -- '편의시설_태그_001', ...
FROM numbers;

-- =================================================================
-- 4. Restaurant (식당) - 1만 개
-- =================================================================
INSERT INTO restaurant (
    name, restaurant_phone_number, address,
    xcoordinate, ycoordinate, max_capacity, restaurant_category_id
)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n <= @RESTAURANT_COUNT -- <= 로 수정
)
SELECT
    CONCAT('맛있는 식당 ', n),
    CONCAT('02-', LPAD(MOD(n, 900) + 100, 3, '0'), '-', LPAD(MOD(n, 9000) + 1000, 4, '0')),
    CONCAT('서울시 어딘가 ', n, '번길'),
    (37.4 + RAND() * 0.2), -- 위도
    (126.8 + RAND() * 0.4), -- 경도
    FLOOR(20 + RAND() * 80),
    1 + MOD(n - 1, @CATEGORY_COUNT) -- 카테고리 ID (1~50)
FROM numbers;

-- =================================================================
-- 5. Menu (메뉴) - 20만 개 (식당당 20개) [수정됨]
-- =================================================================
INSERT INTO menu (name, price, restaurant_id)
WITH RECURSIVE numbers AS ( -- 1. 식당 ID (1 ~ @RESTAURANT_COUNT)
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n <= @RESTAURANT_COUNT
), menu_items AS ( -- 2. 메뉴 번호 (1 ~ 20)
    SELECT 1 AS menu_num
    UNION ALL
    SELECT menu_num + 1 FROM menu_items WHERE menu_num < @MENU_PER_RESTAURANT
)
SELECT
    CONCAT('시그니처 메뉴', LPAD(menu_items.menu_num, 2, '0')), -- '시그니처 메뉴01', ... '20'
    FLOOR(10 + RAND() * 100) * 1000,
    numbers.n -- 식당 ID
FROM numbers
         CROSS JOIN menu_items; -- 3. (10000 * 20) = 20만 개의 유니크한 조합 생성

-- =================================================================
-- 6. RestaurantImage (이미지) - 3만 개 (식당당 3개)
-- =================================================================
INSERT INTO restaurant_image (image_url, restaurant_id)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < (@RESTAURANT_COUNT * 3)
)
SELECT
    CONCAT('https://example.com/img_', n, '.jpg'),
    1 + MOD(n - 1, @RESTAURANT_COUNT)
FROM numbers;

-- =================================================================
-- 7. RestaurantTag (식당-태그 N:M) - 3만 개 (식당당 3개)
-- =================================================================
INSERT INTO restaurant_tag (restaurant_id, tag_id)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < (@RESTAURANT_COUNT * 3)
)
SELECT
    1 + MOD(n - 1, @RESTAURANT_COUNT), -- 식당 ID (1~10000)
    1 + MOD(n - 1, @TAG_COUNT)       -- 태그 ID (1~100)
FROM numbers;

-- =================================================================
-- 8. RestaurantOperatingHour (영업시간) - 7만 개 (식당당 7일)
-- =================================================================
INSERT INTO restaurant_operating_hour (day_of_week, open_time, close_time, is_holiday, restaurant_id)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n <= @RESTAURANT_COUNT -- <= 로 수정
), days AS (
    SELECT 0 AS day_num UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
)
SELECT
    CASE days.day_num
        WHEN 0 THEN 'MONDAY' WHEN 1 THEN 'TUESDAY' WHEN 2 THEN 'WEDNESDAY'
        WHEN 3 THEN 'THURSDAY' WHEN 4 THEN 'FRIDAY' WHEN 5 THEN 'SATURDAY' ELSE 'SUNDAY'
        END,
    '09:00:00',
    '22:00:00',
    IF(days.day_num = 6, IF(RAND() > 0.8, 1, 0), 0), -- 일요일 20% 휴무
    numbers.n -- 식당 ID
FROM numbers CROSS JOIN days;

-- =================================================================
-- 9. ReservationSlot (예약 슬롯) - 120만 개 (식당당 120개 = 30일 * 4타임)
-- =================================================================
INSERT INTO reservation_slot (date, time, count, version, restaurant_id)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n <= @RESTAURANT_COUNT -- <= 로 수정
), dates AS (
    SELECT 0 AS day_offset UNION ALL SELECT 1  UNION ALL SELECT 2  UNION ALL SELECT 3  UNION ALL SELECT 4  UNION ALL
    SELECT 5  UNION ALL SELECT 6  UNION ALL SELECT 7  UNION ALL SELECT 8  UNION ALL SELECT 9  UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL
    SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL
    SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29
), times AS (
    SELECT '17:00:00' AS slot_time UNION ALL SELECT '18:00:00' UNION ALL SELECT '19:00:00' UNION ALL SELECT '20:00:00'
)
SELECT
    DATE_ADD(CURDATE(), INTERVAL dates.day_offset DAY),
    times.slot_time,
    FLOOR(5 + RAND() * 10),
    0,
    numbers.n
FROM numbers
         CROSS JOIN dates
         CROSS JOIN times;

-- =================================================================
-- 완료 확인
-- =================================================================
SELECT '목 데이터 생성 완료' AS Message,
       (SELECT COUNT(*) FROM member) AS member_count,
       (SELECT COUNT(*) FROM restaurant_category) AS category_count,
       (SELECT COUNT(*) FROM tag) AS tag_count,
       (SELECT COUNT(*) FROM restaurant) AS restaurant_count,
       (SELECT COUNT(*) FROM menu) AS menu_count,
       (SELECT COUNT(*) FROM restaurant_tag) AS tag_map_count,
       (SELECT COUNT(*) FROM reservation_slot) AS slot_count;
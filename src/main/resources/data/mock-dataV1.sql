use portfolio;

SET @@cte_max_recursion_depth = 2000001;

-- [1] 생성할 개수 설정
SET @MEMBER_COUNT = 50000;
SET @RESTAURANT_COUNT = 1000000;
SET @CATEGORY_COUNT = 50;

-- =================================================================
-- 1. Member (회원)
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
-- 4. Restaurant (식당)
-- =================================================================
INSERT INTO restaurant (
    name,
    restaurant_phone_number,
    address,
    xcoordinate,
    ycoordinate,
    max_capacity,
    main_image_url,          -- 🔹 새로 추가
    restaurant_category_id
)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n <= @RESTAURANT_COUNT
)
SELECT
    CONCAT('맛있는 식당 ', n),
    CONCAT('02-', LPAD(MOD(n, 900) + 100, 3, '0'),
           '-', LPAD(MOD(n, 9000) + 1000, 4, '0')),
    CONCAT('서울시 어딘가 ', n, '번길'),
    (37.4 + RAND() * 0.2),          -- 위도
    (126.8 + RAND() * 0.4),         -- 경도
    FLOOR(20 + RAND() * 80),        -- max_capacity
    CONCAT('https://example.com/main_', n, '.jpg'),  -- 🔹 main_image_url 랜덤(규칙) 값
    1 + MOD(n - 1, @CATEGORY_COUNT) -- 카테고리 ID (1~50)
FROM numbers;

-- =================================================================
-- 6. RestaurantImage (이미지) - (식당당 2개)
-- =================================================================
INSERT INTO restaurant_image (image_url, restaurant_id)
WITH RECURSIVE numbers AS (
    SELECT 1 as n
    UNION ALL
    SELECT n + 1 FROM numbers WHERE n < (@RESTAURANT_COUNT * 2)
)
SELECT
    CONCAT('https://example.com/img_', n, '.jpg'),
    1 + MOD(n - 1, @RESTAURANT_COUNT)
FROM numbers;

-- =================================================================
-- 9. ReservationSlot (예약 슬롯) - 120만 개 (식당 1만개 × 30일 × 4타임)
-- =================================================================
SET @SLOT_RESTAURANT_MAX = 10000;  -- 슬롯을 생성할 식당 개수 (1 ~ 10000번 식당)

INSERT INTO reservation_slot (date, time, count, version, restaurant_id)
WITH RECURSIVE numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1
    FROM numbers
    WHERE n <= @SLOT_RESTAURANT_MAX  -- 1 ~ 10000까지만 슬롯 생성
), dates AS (
    SELECT 0 AS day_offset UNION ALL SELECT 1  UNION ALL SELECT 2  UNION ALL SELECT 3  UNION ALL SELECT 4  UNION ALL
    SELECT 5  UNION ALL SELECT 6  UNION ALL SELECT 7  UNION ALL SELECT 8  UNION ALL SELECT 9  UNION ALL
    SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL
    SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL
    SELECT 20 UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL
    SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29
), times AS (
    SELECT '17:00:00' AS slot_time UNION ALL
    SELECT '18:00:00' UNION ALL
    SELECT '19:00:00' UNION ALL
    SELECT '20:00:00'
)
SELECT
    DATE_ADD(CURDATE(), INTERVAL dates.day_offset DAY),  -- 30일
    times.slot_time,                                     -- 4타임
    FLOOR(5 + RAND() * 10),                              -- 5~14석
    0,
    numbers.n                                            -- restaurant_id: 1 ~ 10000
FROM numbers
         CROSS JOIN dates
         CROSS JOIN times;
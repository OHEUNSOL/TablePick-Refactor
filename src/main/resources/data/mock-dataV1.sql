use portfolio;

SET @@cte_max_recursion_depth = 2000001;

-- [1] 생성할 개수 설정
SET @MEMBER_COUNT = 50000;
SET @RESTAURANT_COUNT = 1000000;
SET @CATEGORY_COUNT = 100;

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
    5,        -- max_capacity
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
-- 9. ReservationSlot (예약 슬롯) - (식당 2만개 × 7일 × 10타임 = 140만 개)
-- =================================================================
SET @SLOT_RESTAURANT_MAX = 20000;

INSERT INTO reservation_slot (date, time, count, version, restaurant_id)
WITH RECURSIVE
    restaurants AS (
        SELECT 1 AS rid
        UNION ALL
        SELECT rid + 1 FROM restaurants WHERE rid < @SLOT_RESTAURANT_MAX
    ),
    days AS (
        SELECT 1 AS day_idx
        UNION ALL
        SELECT day_idx + 1 FROM days WHERE day_idx < 7
    ),
    times AS (
        SELECT 0 AS time_idx
        UNION ALL
        SELECT time_idx + 1 FROM times WHERE time_idx < 9
    )
SELECT
    DATE_ADD(CURDATE(), INTERVAL days.day_idx DAY) AS date,
  -- 11:00 ~ 20:00 (10타임)
  SEC_TO_TIME((11 + times.time_idx) * 3600) AS time,

  -- ✅ 0~5 골고루: 반드시 time_idx가 들어가야 합니다(그리고 숫자여야 함)
  MOD(
    ((restaurants.rid - 1) * 70)
    + ((days.day_idx - 1) * 10)
    + CAST(times.time_idx AS SIGNED),
    6
  ) AS count,

  0 AS version,
  restaurants.rid AS restaurant_id
FROM restaurants
    CROSS JOIN days
    CROSS JOIN times;
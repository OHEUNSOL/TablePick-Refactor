import http from 'k6/http';
import { check } from 'k6';
import { Trend, Counter } from 'k6/metrics';
import exec from 'k6/execution';

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// === 1. 환경 변수 설정 ===
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 실행 모드 설정
const TYPE = __ENV.TYPE || 'v2_opt';
const MODE = __ENV.MODE || 'hot';

const TARGET_RESTAURANT_ID = Number(__ENV.RESTAURANT_ID || 1);
const RANDOM_MAX_ID = 100;

const RESERVATION_DATE = __ENV.RESERVATION_DATE || '2025-11-05';
const RESERVATION_TIME = __ENV.RESERVATION_TIME || '17:00';
const PARTY_SIZE = Number(__ENV.PARTY_SIZE || 2);

// [수정됨] DB에 저장된 유저 ID 범위 설정
// 예: testuser0000004 ~ testuser0000100 까지 사용하고 싶다면 아래 숫자를 조절하세요.
const USER_ID_START = 4;   // 시작 번호 (예: testuser0000004)
const USER_ID_END = 100;   // 끝 번호 (예: testuser0000100) - 실제 DB에 있는 만큼 늘리세요!

const successCount = new Counter('reservation_success_count');
const soldOutCount = new Counter('reservation_sold_out_count');

// === 2. 메트릭 정의 ===
const v2OptTrend = new Trend('http_req_duration_v2_opt');
const v2PesTrend = new Trend('http_req_duration_v2_pes');
const v3SyncTrend = new Trend('http_req_duration_v3_sync');
const v3AsyncTrend = new Trend('http_req_duration_v3_async');
const v3KafkaTrend = new Trend('http_req_duration_v3_kafka');
const v4KafkaRedis = new Trend('http_req_duration_v4_kafka_redis');

// === 3. 시나리오 정의 ===
const scenarios = {
    v2_opt: {
        executor: 'constant-vus',
        exec: 'testV2Optimistic',
        vus: 30,
        duration: '20s',
        tags: { version: 'v2_opt' },
    },
    v2_pes: {
        executor: 'constant-vus',
        exec: 'testV2Pessimistic',
        vus: 30,
        duration: '20s',
        tags: { version: 'v2_pes' },
    },
    v3_sync: {
        executor: 'constant-vus',
        exec: 'testV3Sync',
        vus: 30,
        duration: '20s',
        tags: { version: 'v3_sync' },
    },
    v3_async: {
        executor: 'constant-vus',
        exec: 'testV3Async',
        vus: 30,
        duration: '20s',
        tags: { version: 'v3_async' },
    },
    v3_kafka: {
        executor: 'constant-vus',
        exec: 'testV3Kafka',
        vus: 30,
        duration: '20s',
        tags: { version: 'v3_kafka' },
    },
    v4_redis: {
        executor: 'constant-vus',
        exec: 'testV4KafkaRedis',
        vus: 30,
        duration: '20s',
        tags: { version: 'v4_kafka_redis' },
    },
};

export const options = {
    scenarios: {
        [TYPE]: scenarios[TYPE],
    },
};

// === 4. 헬퍼 함수 ===

function getRandomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

// [핵심 수정] 숫자 포맷팅 함수 (7자리 0 채우기)
// 4 -> "0000004", 12 -> "0000012"
function pad(number, length) {
    var str = '' + number;
    while (str.length < length) {
        str = '0' + str;
    }
    return str;
}

// [수정됨] 범위 내의 유효한 유저 생성 함수
function getValidUser() {
    if (!exec.vu) return `testuser${pad(USER_ID_START, 7)}@example.com`;

    // 사용할 수 있는 총 유저 수 계산
    const totalUsers = USER_ID_END - USER_ID_START + 1;

    // 현재 VU ID를 기준으로 순차적으로 할당 (Round Robin)
    // VU가 100명이고 유저가 10명이면, 1번 유저를 10명이 공유하게 됨
    const offset = (exec.vu.idInTest - 1) % totalUsers;
    const currentId = USER_ID_START + offset;

    // 포맷팅: testuser + 00000XX + @example.com
    return `testuser${pad(currentId, 7)}@example.com`;
}

function getTargetRestaurantId() {
    if (MODE === 'random') {
        return getRandomInt(1, RANDOM_MAX_ID);
    }
    return TARGET_RESTAURANT_ID;
}

function postReservation(path, trendMetric) {
    const validUser = getValidUser();
    const targetId = getTargetRestaurantId();

    const url = `${BASE_URL}${path}?username=${encodeURIComponent(validUser)}`;

    const payload = JSON.stringify({
        restaurantId: targetId,
        reservationDate: RESERVATION_DATE,
        reservationTime: RESERVATION_TIME,
        partySize: PARTY_SIZE,
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { endpoint: path },
    };

    const res = http.post(url, payload, params);

    trendMetric.add(res.timings.duration);

    // 1. 성공 (200, 201)
    if (res.status === 200 || res.status === 201) {
        successCount.add(1);
    }
    // 2. 정상 방어 (409 만석 or 400 재고부족)
    else if (res.status === 409 || (res.status === 400 && res.body.includes("EXCEED_RESERVATION_LIMIT"))) {
        // "이미 마감되었습니다"는 에러가 아니므로 FAIL 로그 안 찍음!
        soldOutCount.add(1);
    }
    // 3. 진짜 에러 (그 외의 400이나 500 에러)
    else {
        console.error(`[REAL FAIL] Status: ${res.status}, Body: ${res.body}`);
    }

    // 체크 로직 (400, 409도 handled로 간주하여 k6가 pass 처리하게 함)
    check(res, {
        'status is handled': (r) =>
            [200, 201, 204, 400, 409].includes(r.status),
    });
}

// === 5. 실제 실행 함수들 ===

export function testV2Optimistic() {
    postReservation('/api/reservations/test/v2/optimistic', v2OptTrend);
}

export function testV2Pessimistic() {
    postReservation('/api/reservations/test/v2/pessimistic', v2PesTrend);
}

export function testV3Sync() {
    postReservation('/api/reservations/test/v3/sync/pes', v3SyncTrend);
}

export function testV3Async() {
    postReservation('/api/reservations/test/v3/async/pes', v3AsyncTrend);
}

export function testV3Kafka() {
    postReservation('/api/reservations/test/v3/kafka/pes', v3KafkaTrend);
}

export function testV4KafkaRedis() {
    postReservation('/api/reservations/test/v4/kafka-redis/pes', v4KafkaRedis);
}

export function handleSummary(data) {
    return {
        // 1. stdout: 터미널에 텍스트로 출력 (강제)
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    };
}
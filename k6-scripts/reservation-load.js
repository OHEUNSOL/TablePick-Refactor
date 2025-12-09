import http from 'k6/http';
import { check } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USERNAME = __ENV.USERNAME || 'test@test.com';

const RESTAURANT_ID = Number(__ENV.RESTAURANT_ID || 1);
const RESERVATION_DATE = __ENV.RESERVATION_DATE || '2025-12-31'; // yyyy-MM-dd
const RESERVATION_TIME = __ENV.RESERVATION_TIME || '18:00';      // HH:mm
const PARTY_SIZE = Number(__ENV.PARTY_SIZE || 2);

// 버전별 응답시간 측정용 메트릭
const v2OptTrend = new Trend('http_req_duration_v2_opt');
const v2PesTrend = new Trend('http_req_duration_v2_pes');
const v3SyncTrend   = new Trend('http_req_duration_v3_sync');
const v3AsyncTrend  = new Trend('http_req_duration_v3_async');
const v3KafkaTrend  = new Trend('http_req_duration_v3_kafka');
const v4KafkaRedis  = new Trend('http_req_duration_v4_kafka_redis');

export const options = {
    scenarios: {
        v2_optimistic: {
            executor: 'constant-vus',
            exec: 'testV2Optimistic',
            vus: 50,
            duration: '30s',
            startTime: '50s',
            tags: { version: 'v2_opt' },
        },
        v2_pessimistic: {
            executor: 'constant-vus',
            exec: 'testV2Pessimistic',
            vus: 50,
            duration: '30s',
            startTime: '85s',
            tags: { version: 'v2_pes' },
        },
        v3_sync: {
            executor: 'constant-vus',
            exec: 'testV3Sync',
            vus: 50,
            duration: '30s',
            startTime: '120s',
            tags: { version: 'v3_sync' },
        },
        v3_async: {
            executor: 'constant-vus',
            exec: 'testV3Async',
            vus: 50,
            duration: '30s',
            startTime: '155s',
            tags: { version: 'v3_async' },
        },
        v3_kafka: {
            executor: 'constant-vus',
            exec: 'testV3Kafka',
            vus: 50,
            duration: '30s',
            startTime: '190s',
            tags: { version: 'v3_kafka' },
        },
        v4_kafka_redis: {
            executor: 'constant-vus',
            exec: 'testV4KafkaRedis',
            vus: 50,
            duration: '30s',
            startTime: '225s',
            tags: { version: 'v4_kafka_redis' },
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.05'],
        'http_req_duration{version:v4_kafka_redis}': ['p(95)<500'],
    },
};

function buildBody() {
    return JSON.stringify({
        restaurantId: RESTAURANT_ID,
        reservationDate: RESERVATION_DATE,
        reservationTime: RESERVATION_TIME,
        partySize: PARTY_SIZE,
    });
}

function postReservation(path, trendMetric) {
    const url = `${BASE_URL}${path}?username=${encodeURIComponent(USERNAME)}`;
    const payload = buildBody();
    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { endpoint: path },
    };

    const res = http.post(url, payload, params);
    trendMetric.add(res.timings.duration);

    // 만석/중복 등 비즈니스 에러를 포함해 “정상 흐름”으로 본다
    check(res, {
        'status is 2xx or business 4xx': (r) =>
            [200, 201, 204, 400, 409].includes(r.status),
    });
}


/**
 * V2 + FacadeV0 (낙관락)
 * - 동시성 제어(낙관락) 적용
 * - 외부 기능은 아직 트랜잭션 안/직후에서 동기 실행
 */
export function testV2Optimistic() {
    postReservation('/api/reservations/test/v2/optimistic', v2OptTrend);
}

/**
 * V2 (비관락 버전)
 */
export function testV2Pessimistic() {
    postReservation('/api/reservations/test/v2/pessimistic', v2PesTrend);
}

/**
 * V3 + FacadeV1
 * - 트랜잭션 분리 후 외부 기능 동기 실행
 */
export function testV3Sync() {
    postReservation('/api/reservations/test/v3/sync', v3SyncTrend);
}

/**
 * V3 + FacadeV2
 * - 외부 기능 @Async 비동기 실행
 */
export function testV3Async() {
    postReservation('/api/reservations/test/v3/async', v3AsyncTrend);
}

/**
 * V3 + FacadeV3
 * - 외부 기능 Kafka 비동기 실행
 */
export function testV3Kafka() {
    postReservation('/api/reservations/test/v3/kafka', v3KafkaTrend);
}

/**
 * V4 + FacadeV4
 * - Kafka + Redis FULL 슬롯 캐싱
 */
export function testV4KafkaRedis() {
    postReservation('/api/reservations/test/v4/kafka-redis', v4KafkaRedis);
}
import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RESERVATION_ID = __ENV.RESERVATION_ID || 1;
// 실행할 때 타입을 입력받음 (기본값: sync)
const TEST_TYPE = __ENV.TYPE || 'sync';

// 시나리오 설정 객체
const scenarios = {
    sync: {
        executor: 'constant-vus',
        vus: 20,
        duration: '20s',
        exec: 'sendSync',
    },
    async: {
        executor: 'constant-vus',
        vus: 20,
        duration: '20s',
        exec: 'sendAsync',
    },
    kafka: {
        executor: 'constant-vus',
        vus: 20,
        duration: '20s',
        exec: 'sendKafka',
    },
};

export const options = {
    // 입력받은 TYPE에 해당하는 시나리오만 실행
    scenarios: {
        [TEST_TYPE]: scenarios[TEST_TYPE],
    },
};

function buildUrl(suffix) {
    return `${BASE_URL}/api/test/email/reservations/${RESERVATION_ID}/${suffix}`;
}

export function sendSync() {
    const res = http.post(buildUrl('sync'), null);
    check(res, { 'sync 2xx': (r) => r.status >= 200 && r.status < 300 });
}

export function sendAsync() {
    const res = http.post(buildUrl('async'), null);
    check(res, { 'async 2xx': (r) => r.status >= 200 && r.status < 300 });
}

export function sendKafka() {
    const res = http.post(buildUrl('kafka'), null);
    check(res, { 'kafka 2xx': (r) => r.status >= 200 && r.status < 300 });
}

export function handleSummary(data) {
    return {
        // 1. stdout: 터미널에 텍스트로 출력 (강제)
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    };
}
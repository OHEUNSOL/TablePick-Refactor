import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// 환경 변수 설정
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RESERVATION_ID = __ENV.RESERVATION_ID || 1;
const TYPE = __ENV.TYPE || 'async'; // sync, async, kafka
const LOAD_PROFILE = __ENV.PROFILE || 'safety'; // safety(일반), crash(폭파)

// 1. 부하 프로필 정의
const profiles = {
    // [안전 모드] 성능 비교용 (VUs 50 고정)
    safety: {
        executor: 'constant-vus',
        vus: 20,
        duration: '30s',
    },
    // [파괴 모드] 한계 돌파용 (VUs 0 -> 500 급증)
    crash: {
        executor: 'ramping-vus',
        startVUs: 0,
        stages: [
            { duration: '10s', target: 10 },  // 웜업
            { duration: '20s', target: 100 }, // 폭주
            { duration: '10s', target: 0 },   // 종료
        ],
    },
};

// 2. 시나리오 매핑
export const options = {
    scenarios: {
        [TYPE]: {
            ...profiles[LOAD_PROFILE], // 선택한 프로필(safety/crash) 적용
            exec: getExecFunctionName(TYPE),
        },
    },
    // 파괴 테스트 시 에러가 많이 나도 멈추지 않게 설정
    thresholds: {
        http_req_failed: ['rate<1.00'], // 에러율 100%여도 테스트 강행
    },
};

// 함수 이름 매핑 도우미
function getExecFunctionName(type) {
    if (type === 'sync') return 'sendSync';
    if (type === 'async') return 'sendAsync';
    if (type === 'kafka') return 'sendKafka';
    return 'sendAsync';
}

function buildUrl(suffix) {
    return `${BASE_URL}/api/test/email/reservations/${RESERVATION_ID}/${suffix}`;
}

export function sendSync() {
    const res = http.post(buildUrl('sync'), null);
    check(res, { 'sync 2xx': (r) => r.status >= 200 && r.status < 300 });

    if (LOAD_PROFILE === 'safety') {
        sleep(0.8);  // 0.3s 정도 대기 → VU 20이면 대략 60~70 rps 수준
    }
}

export function sendAsync() {
    const res = http.post(buildUrl('async'), null);
    // Crash 테스트 시 500 에러 체크를 위해 성공/실패 모두 집계
    check(res, {
        'async success': (r) => r.status >= 200 && r.status < 300,
        'async failed (500/Timeout)': (r) => r.status >= 500
    });

    if (LOAD_PROFILE === 'safety') {
        sleep(0.8);
    }
}

export function sendKafka() {
    const res = http.post(buildUrl('kafka'), null);
    check(res, { 'kafka 2xx': (r) => r.status >= 200 && r.status < 300 });
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    };
}
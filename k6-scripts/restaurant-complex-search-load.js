import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// --- 커스텀 메트릭 ---
const searchLatency = new Trend('restaurant_search_latency_ms', true);
const successRate = new Rate('restaurant_search_success_rate');

// --- 옵션 설정 ---
export const options = {
    stages: [
        { duration: '30s', target: 10 },  // 워밍업
        { duration: '1m', target: 50 },   // 실제 부하 구간
        { duration: '30s', target: 0 },   // 종료
    ],
    thresholds: {
        restaurant_search_latency_ms: ['p(95) < 200'],
        restaurant_search_success_rate: ['rate > 0.99'],
        http_req_failed: ['rate < 0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VERSION = __ENV.VERSION || 'v0'; // v0 / v1

// ✅ 데이터 생성 기준에 맞춘 값들
// 카테고리 1~50 + 필터 없음(null)
const CATEGORY_IDS = [null, ...Array.from({ length: 50 }, (_, i) => i + 1)];

// 11:00 ~ 20:00 (10타임)
const TIMES = Array.from({ length: 10 }, (_, i) => `${String(11 + i).padStart(2, '0')}:00:00`);

function randomChoice(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

// 오늘 기준 내일부터 7일 뒤까지: D+1 ~ D+7
function randomDateString() {
    const today = new Date();
    const offset = 1 + Math.floor(Math.random() * 7); // 1~7
    const d = new Date(today.getTime() + offset * 24 * 60 * 60 * 1000);

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`;
}

export default function () {
    const categoryId = randomChoice(CATEGORY_IDS);
    const reservationDate = randomDateString();
    const reservationTime = randomChoice(TIMES);

    const page = 0;
    const size = 100;

    let url =
        `${BASE_URL}/api/restaurants/complex-search/${VERSION}` +
        `?reservationDate=${reservationDate}` +
        `&reservationTime=${reservationTime}` +
        `&page=${page}&size=${size}`;

    if (categoryId !== null) {
        url += `&categoryId=${categoryId}`;
    }

    const res = http.get(url, {
        headers: { 'Content-Type': 'application/json' },
    });

    // 메트릭 기록
    searchLatency.add(res.timings.duration);
    successRate.add(res.status === 200);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(1);
}

export function handleSummary(data) {
    return {
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}
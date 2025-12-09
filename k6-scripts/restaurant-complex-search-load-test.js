import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

// --- 커스텀 메트릭 ---
const searchLatency = new Trend('restaurant_search_latency_ms', true);
const successRate = new Rate('restaurant_search_success_rate');

// --- 옵션 설정 ---
// 인덱스 적용 전/후를 같은 스크립트로 재사용해서 비교하면 됩니다.
export const options = {
    // VU 수 & 기간은 필요에 따라 조절해서 사용하세요.
    stages: [
        { duration: '30s', target: 10 },  // 워밍업
        { duration: '1m', target: 50 },   // 실제 부하 구간
        { duration: '30s', target: 0 },   // 종료
    ],
    thresholds: {
        restaurant_search_latency_ms: ['p(95) < 200'], // p95 200ms 아래를 목표 예시
        restaurant_search_success_rate: ['rate > 0.99'],
        http_req_failed: ['rate < 0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 테스트용 카테고리 / 시간 값들 (실제 DB에 존재하는 값으로 맞춰 주세요)
const CATEGORY_IDS = [1, 2, 3, null]; // null은 "카테고리 필터 없음" 케이스
const TIMES = ['18:00:00', '19:00:00', '20:00:00'];

function randomChoice(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

// 오늘 기준 +0~7일 사이 랜덤 날짜
function randomDateString() {
    const today = new Date();
    const offset = Math.floor(Math.random() * 8); // 0~7
    const d = new Date(today.getTime() + offset * 24 * 60 * 60 * 1000);

    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');

    return `${year}-${month}-${day}`; // ISO.DATE 형식
}

export default function () {
    const categoryId = randomChoice(CATEGORY_IDS);
    const reservationDate = randomDateString();
    const reservationTime = randomChoice(TIMES);

    const page = 0;
    const size = 10;

    // categoryId는 null일 경우 쿼리스트링에서 아예 빼는 식으로 구성
    let url = `${BASE_URL}/api/restaurants/complex-search?reservationDate=${reservationDate}` +
        `&reservationTime=${reservationTime}` +
        `&page=${page}&size=${size}`;

    if (categoryId !== null) {
        url += `&categoryId=${categoryId}`;
    }

    const res = http.get(url, {
        headers: {
            'Content-Type': 'application/json',
        },
    });

    // 메트릭 기록
    searchLatency.add(res.timings.duration);
    successRate.add(res.status === 200);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    // 너무 빡세지 않게 살짝 쉼
    sleep(1);
}
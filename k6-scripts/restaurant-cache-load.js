import http from 'k6/http';
import { check, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// ===========================
// 1. 환경 변수 설정
// ===========================
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 실행 모드: performance(기본), penetration(관통), stability(동시성/안정성)
const TEST_SCENARIO = __ENV.SCENARIO || 'performance';

// 테스트할 API 버전 (v0 ~ v4)
const VERSION = __ENV.VERSION || 'v0';

// ===========================
// 2. 시나리오별 설정 값 (동적 할당)
// ===========================
let targetVUs = 20;
let targetDuration = '30s';
let targetRestaurantId = 1; // 존재하는 ID (기본값)

// 시나리오별 파라미터 조정
if (TEST_SCENARIO === 'performance') {
    // [1단계] 기본 성능 비교: 적당한 부하로 속도 차이 확인
    targetVUs = 30;
    targetRestaurantId = 1; // DB에 있는 ID
} else if (TEST_SCENARIO === 'penetration') {
    // [2단계] 캐시 관통(Penetration): DB에 없는 ID를 조회하여 방어력 확인
    targetVUs = 50;
    targetRestaurantId = -9999; // DB에 절대 없는 ID
} else if (TEST_SCENARIO === 'stability') {
    // [3단계] 핫키/아발란치(HotKey): 매우 높은 동시성으로 캐시 만료 시점의 DB 부하 확인
    targetVUs = 100; // 순간적으로 많은 유저가 몰림
    targetRestaurantId = 1;
}

// k6 옵션 설정
export const options = {
    scenarios: {
        defined_scenario: {
            executor: 'constant-vus',
            vus: targetVUs,
            duration: targetDuration,
            tags: { test_type: `${TEST_SCENARIO}_${VERSION}` },
        },
    },
    // p95(95% 유저의 응답속도), p99(튀는 값 확인) 위주로 확인
    summaryTrendStats: ['avg', 'p(90)', 'p(95)', 'p(99)'],
};

// ===========================
// 3. 테스트 실행 로직
// ===========================
export default function () {
    const url = `${BASE_URL}/api/test/restaurants/${VERSION}/${targetRestaurantId}`;

    const res = http.get(url);

    check(res, {
        'status is 200': (r) => r.status === 200,
        // 관통 테스트(v1)의 경우 없는 ID면 500 에러나 예외가 터질 수도 있으니 상황에 맞게 조정 필요
        // v2부터는 없는 ID도 정상적으로 빈 객체 반환하며 200 OK여야 함
    });

    // stability 테스트는 "캐시 만료 순간"을 보기 위해 딜레이 없이 맹공격 (sleep 0)
    // performance 테스트는 리얼한 트래픽 패턴을 위해 약간의 sleep 추가 가능
    if (TEST_SCENARIO === 'stability') {
        sleep(0);
    } else {
        sleep(0.5);
    }
}

export function handleSummary(data) {
    return {
        'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    };
}
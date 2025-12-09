import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RESTAURANT_ID = __ENV.RESTAURANT_ID || 1;

export const options = {
    scenarios: {
        restaurant_v0_no_cache: {
            executor: 'constant-vus',
            vus: 20,
            duration: '20s',
            exec: 'getV0',
        },
        restaurant_v2_null_cache: {
            executor: 'constant-vus',
            vus: 50,
            duration: '20s',
            exec: 'getV2',
            startTime: '25s',
        },
        restaurant_v3_jitter: {
            executor: 'constant-vus',
            vus: 50,
            duration: '20s',
            exec: 'getV3',
            startTime: '50s',
        },
        restaurant_v4_hotkey: {
            executor: 'constant-vus',
            vus: 50,
            duration: '20s',
            exec: 'getV4',
            startTime: '75s',
        },
    },
};

function get(path) {
    const url = `${BASE_URL}/api/test/restaurants/${path}/${RESTAURANT_ID}`;
    const res = http.get(url);
    check(res, { 'status 2xx': (r) => r.status >= 200 && r.status < 300 });
    sleep(0.2);
}

export function getV0() { get('v0'); }
export function getV1() { get('v1'); } // 필요하면 시나리오에 추가
export function getV2() { get('v2'); }
export function getV3() { get('v3'); }
export function getV4() { get('v4'); }

export default function () {}
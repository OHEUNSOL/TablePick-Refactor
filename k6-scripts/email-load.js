import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const RESERVATION_ID = __ENV.RESERVATION_ID || 1; // 테스트용 예약 ID

export const options = {
    scenarios: {
        email_sync: {
            executor: 'constant-vus',
            vus: 20,
            duration: '20s',
            exec: 'sendSync',
        },
        email_async: {
            executor: 'constant-vus',
            vus: 20,
            duration: '20s',
            exec: 'sendAsync',
            startTime: '25s',
        },
        email_kafka: {
            executor: 'constant-vus',
            vus: 20,
            duration: '20s',
            exec: 'sendKafka',
            startTime: '50s',
        },
    },
};

function buildUrl(suffix) {
    return `${BASE_URL}/api/test/email/reservations/${RESERVATION_ID}/${suffix}`;
}

export function sendSync() {
    const res = http.post(buildUrl('sync'), null);
    check(res, { 'sync 2xx': (r) => r.status >= 200 && r.status < 300 });
    sleep(0.5);
}

export function sendAsync() {
    const res = http.post(buildUrl('async'), null);
    check(res, { 'async 2xx': (r) => r.status >= 200 && r.status < 300 });
    sleep(0.5);
}

export function sendKafka() {
    const res = http.post(buildUrl('kafka'), null);
    check(res, { 'kafka 2xx': (r) => r.status >= 200 && r.status < 300 });
    sleep(0.5);
}

export default function () {}
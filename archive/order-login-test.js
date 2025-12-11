import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import exec from 'k6/execution';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

const orderDuration = new Trend('order_duration');
const orderCounter = new Counter('orders_created');
const errorCounter = new Counter('order_errors');
export const slowReqRate = new Rate('slow_req_rate');

const BASE_URL = 'http://localhost:8080';
// const BASE_URL = 'http://172.16.24.62:8080';

const baseScenarios = {
    // Phase 1: 평상시 트래픽 (50-100 TPS)
    normal: {
        executor: 'constant-vus',
        vus: 80,  // sleep(1) 고려하여 80 VUs = 약 80 TPS
        duration: '30s',
        startTime: '0s',
        tags: { scenario: 'normal' },
    },
    
    // Phase 2: 트래픽 증가 (100->500 TPS)
    increase: {
        executor: 'ramping-vus',
        startVUs: 80,
        stages: [
            { duration: '15s', target: 250 },  // 80->250 VUs
            { duration: '15s', target: 500 },  // 250->500 VUs
            { duration: '30s', target: 500 },   // 500 TPS 유지
        ],
        startTime: '30s',
        tags: { scenario: 'increase' },
    },
    
    // Phase 3: 드랍 이벤트 (500->1000 TPS)
    drop: {
        executor: 'ramping-vus',
        startVUs: 500,
        stages: [
            { duration: '15s', target: 1000 },  // 급증
            { duration: '1m', target: 1000 },   // 피크 유지
            { duration: '15s', target: 200 },   // 감소
        ],
        startTime: '1m30s',
        tags: { scenario: 'drop' },
    },
    
    // Phase 4: 대형 이벤트 스파이크 (2000-3000 TPS)
    spike: {
        executor: 'ramping-vus',
        startVUs: 200,
        stages: [
            { duration: '20s', target: 2000 },  // 급격한 증가
            { duration: '30s', target: 3000 },  // 최대 부하
            { duration: '1m', target: 3000 },   // 피크 유지
            { duration: '30s', target: 100 },   // 급격한 감소
        ],
        startTime: '3m',
        tags: { scenario: 'spike' },
    },
};

const runList = (__ENV.SCENARIOS || "all")
  .split(",")
  .map(s => s.trim());

function buildScenarios() {
  if (runList.includes("all")) return baseScenarios;

  const selected = {};
  for (const [name, config] of Object.entries(baseScenarios)) {
    if (runList.includes(name)) {
      selected[name] = config;
    }
  }
  return selected;
}

export const options = {
    summaryTrendStats: ['min', 'avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
    scenarios: buildScenarios(),
    thresholds: {
        http_req_failed: ['rate==0'],                 // 요청 실패 0
        'checks{type:status}': ['rate>0.999'],        // status 체크 성공률 99.9%
        order_duration: ['p(95)<1000', 'p(99)<2000'],         // 전체 p95 < 1초
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],
        'slow_req_rate': ['rate<0.01'],               // 느린 응답 < 1%

         // 시나리오별 세부 임계값
        'http_req_duration{scenario:normal}': ['p(95)<500'],        // 평상시 500ms
        'http_req_duration{scenario:increase}': ['p(95)<700'],      // 증가시 700ms
        'http_req_duration{scenario:drop}': ['p(95)<1000'],         // 드랍시 1초
        'http_req_duration{scenario:spike}': ['p(95)<2000'],        // 스파이크시 2초
        
         // 비즈니스 메트릭
        orders_created: ['count>1000'],                       // 최소 1000개 주문
        order_errors: ['count<100'],                          // 에러 100개 미만
    }
};



export function setup() {
    const payload = JSON.stringify({
        identifier: 'test',
        password: 'test!234',
    });

    const params = {
        headers: { 'Content-Type': 'application/json' },
        tags: { endpoint: 'login' },
    };

    const res = http.post(`${BASE_URL}/api/auth/login`, payload, params);

    check(res, {
            'login status is 200': (r) => r.status === 200,
        },
    );

    const cookies = res.cookies['__Host-AT'];
    const accessTokenCookie = cookies && cookies[0] ? cookies[0].value : null;

    if (!accessTokenCookie) {
        console.error('로그인 실패: __Host-AT 쿠키 없음, status:', res.status, 'body:', res.body);
    }

    return { accessTokenCookie };
}

function createOrder(accessTokenCookie){
    const clientRequestId = uuidv4();
    const payload = JSON.stringify({
        clientRequestId,
        productOptionId: 412952,
    });

    const params = {
        headers: {
           'Content-Type': 'application/json',
        },
        cookies: {
            '__Host-AT': accessTokenCookie,
        },
        tags: { endpoint: 'order' },
    };

    const res = http.post(`${BASE_URL}/api/orders`, payload, params);


    const isSuccess = check(
        res,
        {
            'order status is 200': (r) => r.status === 200,
        },
        { type: 'status', endpoint: 'order' },
    );

    if (isSuccess) {
        orderCounter.add(1);
    } else {
        errorCounter.add(1);
    }

    orderDuration.add(res.timings.duration);

    const isFast = res.timings.duration < 500;

    check(res, {
        'response time < 500ms': () => isFast,
    }, { type: 'latency', endpoint: 'order' });

    slowReqRate.add(!isFast);

    return res;
}

export default function (data) {
    const accessTokenCookie = data.accessTokenCookie;

    if(!accessTokenCookie){
        console.error('로그인 실패: AccessToken 쿠키 없음');
        sleep(1);
        return;
    }

    createOrder(accessTokenCookie);

     // 시나리오별 다른 sleep 패턴
    const scenario = __ITER === 0 ? 'normal' : exec.scenario.name;
    
    if (scenario === 'spike') {
        // 스파이크 시: 매우 짧은 대기
        sleep(Math.random() * 0.3);  // 0-300ms
    } else if (scenario === 'drop') {
        // 드랍 시: 짧은 대기
        sleep(Math.random() * 0.5 + 0.2);  // 200-700ms
    } else if (scenario === 'increase') {
        // 증가 시: 중간 대기
        sleep(Math.random() * 0.5 + 0.5);  // 500ms-1초
    } else {
        // 평상시: 기존 패턴 유지
        sleep(1);
    }
}

// 테스트 종료 후 요약
export function handleSummary(data) {
    const orders = data.metrics.orders_created?.values?.count || 0;
    const errors = data.metrics.order_errors?.values?.count || 0;
    const total = orders + errors;
    const successRate = total > 0 ? (orders / total * 100) : 0;
    const p95 = data.metrics.order_duration?.values?.['p(95)'];
    const p99 = data.metrics.order_duration?.values?.['p(99)'];
    const slowRate = data.metrics.slow_req_rate?.values?.rate;

    // 커스텀 Summary (지금처럼 유지)
    console.log('\n=== ZEEBRA Load Test Summary ===');
    console.log(`Total Requests: ${total}`);
    console.log(`Successful Orders: ${orders}`);
    console.log(`Failed Orders: ${errors}`);
    console.log(`Success Rate: ${successRate.toFixed(2)}%`);
    console.log(`p95 Latency: ${p95 ? p95.toFixed(0) : 'N/A'}ms`);
    console.log(`p99 Latency: ${p99 ? p99.toFixed(0) : 'N/A'}ms`);
    console.log(
        `Slow Requests (>500ms): ${
        slowRate !== undefined ? (slowRate * 100).toFixed(2) : 'N/A'
        }%`,
    );

    return {
        // 1) k6 기본 스타일 summary를 터미널(stdout)에 출력
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
        // 2) JSON 파일 저장 (기존 기능 유지)
        'summary.json': JSON.stringify(data, null, 2),
    };
}
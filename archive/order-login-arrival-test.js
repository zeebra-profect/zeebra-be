import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';
import exec from 'k6/execution';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// 커스텀 메트릭
const orderDuration = new Trend('order_duration');
const orderCounter = new Counter('orders_created');
const errorCounter = new Counter('order_errors');
export const slowReqRate = new Rate('slow_req_rate');

// const BASE_URL = 'http://localhost:8080';
// const BASE_URL = 'http://172.16.24.62:8080';
const BASE_URL = 'https://api.zeebra.shop';


const baseScenarios = {
  // Phase 1: 평상시 트래픽 (점진적 워밍업 포함)
  normal: {
    executor: 'ramping-arrival-rate',
    startRate: 10,              // 낮은 곳에서 시작
    timeUnit: '1s',
    preAllocatedVUs: 100,
    maxVUs: 400,
    startTime: '0s',
    stages: [
      { duration: '30s', target: 50 },   // 워밍업: 10 -> 50
      { duration: '30s', target: 80 },   // 50 -> 80 TPS
      { duration: '1m', target: 80 },    // 평상시 80 TPS 유지
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'normal' },
  },
  // 숫자로 vus 점진적 증가하는 시나리오
  step_150: {
    executor: 'ramping-arrival-rate',
    startRate: 80,              // normal 종료 시점과 일치
    timeUnit: '1s',
    preAllocatedVUs: 200,
    maxVUs: 800,
    startTime: '2m',
    stages: [
      { duration: '30s', target: 150 },  // 80 -> 150
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'step_150' },
  },
  step_250: {
    executor: 'ramping-arrival-rate',
    startRate: 150,              // 150 종료 시점과 일치
    timeUnit: '1s',
    preAllocatedVUs: 800,
    maxVUs: 1000,
    startTime: '2m30s',
    stages: [
      { duration: '30s', target: 250 },  // 150 -> 250
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'step_250' },
  },
  step_400: {
    executor: 'ramping-arrival-rate',
    startRate: 250,              // 250 종료 시점과 일치
    timeUnit: '1s',
    preAllocatedVUs: 1000,
    maxVUs: 1500,
    startTime: '3m',
    stages: [
      { duration: '30s', target: 400 },  // 250 -> 400
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'step_400' },
  },
  step_500: {
    executor: 'ramping-arrival-rate',
    startRate: 400,              // 400 종료 시점과 일치
    timeUnit: '1s',
    preAllocatedVUs: 1500,
    maxVUs: 3000,
    startTime: '3m30s',
    stages: [
      { duration: '30s', target: 500 },  // 400 -> 500
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'step_500' },
  },
  stability: {
    executor: 'ramping-arrival-rate',
    startRate: 500,              // 500 종료 시점과 일치
    timeUnit: '1s',
    preAllocatedVUs: 3000,
    maxVUs: 4000,
    startTime: '4m',
    stages: [
      { duration: '1m', target: 500 },   // 500 TPS 유지
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'stability' },
  },

  // Phase 2: 트래픽 증가 (80 -> 500 TPS, 더 점진적으로)
  increase: {
    executor: 'ramping-arrival-rate',
    startRate: 80,              // normal 종료 시점과 일치
    timeUnit: '1s',
    preAllocatedVUs: 200,
    maxVUs: 800,
    startTime: '2m',
    stages: [
      { duration: '30s', target: 150 },  // 80 -> 150
      { duration: '30s', target: 250 },  // 150 -> 250
      { duration: '30s', target: 400 },  // 250 -> 400
      { duration: '30s', target: 500 },  // 400 -> 500
      { duration: '1m', target: 500 },   // 500 TPS 유지
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'increase' },
  },

  // Phase 3: 드랍 이벤트 (500 -> 1000 -> 500 TPS)
  drop: {
    executor: 'ramping-arrival-rate',
    startRate: 500,             // increase 종료와 일치
    timeUnit: '1s',
    preAllocatedVUs: 700,
    maxVUs: 1500,
    startTime: '5m',
    stages: [
      { duration: '30s', target: 750 },  // 500 -> 750
      { duration: '30s', target: 1000 }, // 750 -> 1000 TPS
      { duration: '1m30s', target: 1000 }, // 피크 유지
      { duration: '30s', target: 500 },  // 1000 -> 500 (다음 단계 연결용)
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'drop' },
  },

  // Phase 4: 대형 이벤트 스파이크 (500 -> 3000 -> 100 TPS)
  spike: {
    executor: 'ramping-arrival-rate',
    startRate: 500,             // drop 종료와 일치
    timeUnit: '1s',
    preAllocatedVUs: 1500,
    maxVUs: 4000,
    startTime: '8m',
    stages: [
      { duration: '30s', target: 1000 }, // 500 -> 1000
      { duration: '30s', target: 1500 }, // 1000 -> 1500
      { duration: '30s', target: 2000 }, // 1500 -> 2000
      { duration: '30s', target: 2500 }, // 2000 -> 2500
      { duration: '20s', target: 3000 }, // 2500 -> 3000 (피크)
      { duration: '1m', target: 3000 },  // 피크 유지
      { duration: '30s', target: 1000 }, // 점진적 감소
      { duration: '30s', target: 100 },  // 최종 감소
    ],
    exec: 'createOrderFlow',
    tags: { scenario: 'spike' },
  },
};

// SCENARIOS 환경변수로 실행할 시나리오 선택
// 예: SCENARIOS=normal,spike  /  SCENARIOS=all
const runList = (__ENV.SCENARIOS || 'all')
  .split(',')
  .map((s) => s.trim());

function buildScenarios() {
  if (runList.includes('all')) return baseScenarios;

  const selected = {};
  for (const [name, config] of Object.entries(baseScenarios)) {
    if (runList.includes(name)) {
      selected[name] = config;
    }
  }
  return selected;
}

const p95 = {
    normal: 500,
    step_150: 500,
    step_250: 500,
    step_400: 700,
    step_500: 1100,
    stability: 1100
}

export const options = {
  summaryTrendStats: ['min', 'avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: buildScenarios(),
  thresholds: {
    http_req_failed: ['rate==0'],                 // 요청 실패 0
    'checks{scenario:normal, type:status}': ['rate>0.999'],        // status 체크 성공률 99.9%
    'checks{scenario:step_150, type:status}': ['rate>0.999'],
    'checks{scenario:step_250, type:status}': ['rate>0.999'],
    'checks{scenario:step_400, type:status}': ['rate>0.999'],
    'checks{scenario:step_500, type:status}': ['rate>0.999'],
    'checks{scenario:stability, type:status}': ['rate>0.999'],
    // 'checks{scenario:increase, type:status}': ['rate>0.999'],
    // 'checks{scenario:drop, type:status}': ['rate>0.999'],
    // 'checks{scenario:spike, type:status}': ['rate>0.999'],
    order_duration: ['p(95)<1100', 'p(99)<2000'], // 전체 p95 < 1초
    slow_req_rate: ['rate<0.01'],                 // 느린 응답 < 1%
    
    // 시나리오별 세부 임계값 (tags.scenario 값과 일치해야 함)
    http_req_duration: ['p(95)<1100', 'p(99)<2000'],
    'http_req_duration{scenario:normal}': ['p(95)<500'],   // 평상시 500ms
    'http_req_duration{scenario:step_150}': ['p(95)<500'],   
    'http_req_duration{scenario:step_250}': ['p(95)<500'],  
    'http_req_duration{scenario:step_400}': ['p(95)<700'],   
    'http_req_duration{scenario:step_500}': ['p(95)<1100'],   
    'http_req_duration{scenario:stability}': ['p(95)<1100'],   
    // 'http_req_duration{scenario:increase}': ['p(95)<700'], // 증가시 700ms
    // 'http_req_duration{scenario:drop}': ['p(95)<1000'],    // 드랍시 1초
    // 'http_req_duration{scenario:spike}': ['p(95)<2000'],   // 스파이크시 2초

    // 비즈니스 메트릭
    orders_created: ['count>1000'], // 최소 1000개 주문
    'orders_created{scenario:normal}': ['count>7600'],
    'orders_created{scenario:step_150}': ['count>3400'],
    'orders_created{scenario:step_250}': ['count>5900'],
    'orders_created{scenario:step_400}': ['count>9700'],
    'orders_created{scenario:step_500}': ['count>13000'],
    'orders_created{scenario:stability}': ['count>30000'],
    // 'orders_created{scenario:increase}': ['count>62000'],
    // 'orders_created{scenario:drop}': ['count>150000'],
    // 'orders_created{scenario:spike}': ['count>490000'],
    order_errors: ['count<100'],    // 에러 100개 미만
    'order_errors{scenario:normal}': ['count<1'],
    'order_errors{scenario:step_150}': ['count<1'],
    'order_errors{scenario:step_250}': ['count<1'],
    'order_errors{scenario:step_400}': ['count<1'],
    'order_errors{scenario:step_500}': ['count<1'],
    'order_errors{scenario:stability}': ['count<1'],
    // 'order_errors{scenario:increase}': ['count<1'],
    // 'order_errors{scenario:drop}': ['count<1'],
    // 'order_errors{scenario:spike}': ['count<1'],
  },
};

// 로그인(setup)
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
  });

  const cookies = res.cookies['__Host-AT'];
  const accessTokenCookie = cookies && cookies[0] ? cookies[0].value : null;

  if (!accessTokenCookie) {
    console.error(
      '로그인 실패: __Host-AT 쿠키 없음, status:',
      res.status,
      'body:',
      res.body,
    );
  }

  return { accessTokenCookie };
}

// 주문 생성
function createOrder(accessTokenCookie, scenarioTag) {

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
        tags: { endpoint: 'order', scenario: scenarioTag },
    };

    const res = http.post(`${BASE_URL}/api/orders`, payload, params);

    const isSuccess = check(
        res,
        {
        'order status is 200': (r) => r.status === 200,
        },
        { type: 'status', endpoint: 'order', scenario: scenarioTag },
    );

    if (isSuccess) {
        orderCounter.add(1, { scenario: scenarioTag });
    } else {
        errorCounter.add(1, { scenario: scenarioTag });
    }

    orderDuration.add(res.timings.duration, { scenario: scenarioTag });

    const isFast = res.timings.duration < p95[scenarioTag];

    check(
        res,
        {
        'response time < p95': () => isFast,
        },
        { type: 'latency', endpoint: 'order', scenario: scenarioTag },
    );

    slowReqRate.add(!isFast, { scenario: scenarioTag });

    return res;
}

// VU 실행 함수
export function createOrderFlow(data) {
    const accessTokenCookie = data.accessTokenCookie;
    const scenarioTag = exec.scenario.name;  

    if (!accessTokenCookie) {
        console.error('로그인 실패: AccessToken 쿠키 없음');
        // arrival-rate에서는 여기서 sleep은 큰 의미는 없음
        sleep(1);
        return;
    }

    createOrder(accessTokenCookie,  scenarioTag);
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

  console.log('\n=== ZEEBRA Load Test Summary ===');
  console.log(`Total Requests: ${total}`);
  console.log(`Success Rate: ${successRate.toFixed(2)}%`);
  console.log(`p95 Latency: ${p95 ? p95.toFixed(0) : 'N/A'}ms`);
  console.log(`p99 Latency: ${p99 ? p99.toFixed(0) : 'N/A'}ms`);
  console.log(
    `Slow Requests (>target p95): ${
      slowRate !== undefined ? (slowRate * 100).toFixed(2) : 'N/A'
    }%`,
  );

  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    summary: JSON.stringify(data, null, 2),
  };
}

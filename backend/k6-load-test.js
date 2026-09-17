import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },  // Ramp-up to 50 virtual users
    { duration: '1m', target: 200 },  // Spike to 200 virtual users (Festival Sale peak)
    { duration: '30s', target: 0 },   // Ramp-down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests must complete within 500ms
    http_req_failed: ['rate<0.01'],    // Error rate must be < 1%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080/api/v1';

export default function () {
  // 1. Browsing Products (Catalog Search)
  const productsRes = http.get(`${BASE_URL}/products?page=0&size=10`);
  check(productsRes, {
    'catalog status is 200': (r) => r.status === 200,
    'catalog response time < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(1);

  // 2. View Product Details
  const productDetailRes = http.get(`${BASE_URL}/products/laddu-gopal-poshak-red`);
  check(productDetailRes, {
    'product detail status is 200 or 404': (r) => r.status === 200 || r.status === 404,
  });

  sleep(1);
}

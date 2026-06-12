import http from 'k6/http';
import { check, sleep } from 'k6';

// k6 Options: Simulates a high-concurrency surge of users trying to check out
export const options = {
  stages: [
    { duration: '5s', target: 100 },  // Ramp up to 100 VUs in 5 seconds
    { duration: '20s', target: 100 }, // Stay at 100 VUs for 20 seconds (flash sale peak)
    { duration: '5s', target: 0 },    // Ramp down to 0 VUs in 5 seconds
  ],
  thresholds: {
    http_req_failed: ['rate<0.1'],     // Less than 10% of requests should fail
  },
};

const BASE_URL = __ENV.GATEWAY_URL || 'http://localhost:8000';

export default function () {
  // Use k6 VU (Virtual User) ID to map to one of our 200 pre-seeded users
  // user_1 to user_200, passwords are pass_1 to pass_200
  const userId = (__VU - 1) % 200 + 1;
  const username = `user_${userId}`;
  const password = `pass_${userId}`;

  const loginHeaders = { 'Content-Type': 'application/json' };
  const loginPayload = JSON.stringify({ username: username, password: password });

  // Step 1: Login to get token
  let loginRes = http.post(`${BASE_URL}/auth/login`, loginPayload, { headers: loginHeaders });
  let loginSuccess = check(loginRes, {
    'login status is 200': (r) => r.status === 200,
    'has auth token': (r) => r.json('token') !== undefined,
  });

  if (!loginSuccess) {
    console.error(`VU ${__VU}: Login failed for user ${username}. Status: ${loginRes.status}. Body: ${loginRes.body}`);
    sleep(1);
    return;
  }

  const token = loginRes.json('token');
  const headersWithAuth = {
    'token': token,
    'Content-Type': 'application/json',
  };

  // Step 2: Browse inventory stock (GET /inventory/stock)
  let stockRes = http.get(`${BASE_URL}/inventory/stock`);
  check(stockRes, {
    'get inventory status is 200': (r) => r.status === 200,
  });

  sleep(0.1); // Small delay to simulate user looking at page

  // Step 3: Add laptop to cart (POST /cart/add)
  const cartPayload = JSON.stringify({ item: 'laptop', quantity: 1 });
  let cartRes = http.post(`${BASE_URL}/cart/add`, cartPayload, { headers: headersWithAuth });
  check(cartRes, {
    'add to cart status is 200': (r) => r.status === 200,
  });

  sleep(0.1); // Small delay to simulate clicking "Go to checkout"

  // Step 4: Perform checkout (POST /payment/checkout)
  const checkoutPayload = JSON.stringify({
    cart_items: [
      { item: 'laptop', quantity: 1 }
    ]
  });
  let checkoutRes = http.post(`${BASE_URL}/payment/checkout`, checkoutPayload, { headers: headersWithAuth });
  
  check(checkoutRes, {
    'checkout returns 200': (r) => r.status === 200,
    'checkout success or expected out-of-stock': (r) => {
      if (r.status !== 200) return false;
      const body = r.json();
      // Returns success: true, or success: false with error: "Item laptop is out of stock" / "Insufficient wallet balance"
      return body.success === true || (body.success === false && (body.error.includes('out of stock') || body.error.includes('balance')));
    }
  });

  // Space out iterations slightly
  sleep(1);
}

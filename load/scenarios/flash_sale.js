import { performCheckoutFlow } from '../utils/checkout.js';
import { getRandomUser } from '../utils/data.js';
import defaultThresholds from '../thresholds/default.js';

export const options = {
  stages: [
    { duration: '30s', target: 10000 }, // 0 -> 10000
    { duration: '1m', target: 10000 },  // 10000 hold
    { duration: '30s', target: 0 },     // ramp down
  ],
  thresholds: Object.assign({}, defaultThresholds, {
    http_req_duration: ['p(95)<2000', 'p(99)<5000'], // looser thresholds during spike
  })
};

const FLASH_PRODUCT_ID = __ENV.FLASH_PRODUCT_ID || 1;

export default function () {
  const userId = getRandomUser(); // Each VU represents a unique user (mostly unique, from 1-1000, wait, we might need truly unique or just random. We use random 1-1000 for now. Since 10k VUs hit it, there will be collisions. We can add a larger user pool if needed, but the checkout flow doesn't strictly check user existence strictly for concurrency issues.)
  
  // To avoid collisions in idempotency and user data we could just use a totally random ID for flash sale
  const uniqueUserId = Math.floor(Math.random() * 1000000); 

  // Stampede: no think time
  performCheckoutFlow(uniqueUserId, FLASH_PRODUCT_ID, 1, false);
}

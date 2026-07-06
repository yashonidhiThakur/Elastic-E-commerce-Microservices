import { sleep } from 'k6';
import { get, post } from '../utils/http.js';
import { performCheckoutFlow } from '../utils/checkout.js';
import { getRandomUser, getRandomProduct } from '../utils/data.js';
import defaultThresholds from '../thresholds/default.js';

export const options = {
  stages: [
    { duration: '1m', target: 500 }, // 0 -> 500
    { duration: '3m', target: 500 }, // 500 hold
    { duration: '1m', target: 1000 }, // 500 -> 1000
    { duration: '5m', target: 1000 }, // 1000 hold
    { duration: '1m', target: 0 },   // ramp down
  ],
  thresholds: defaultThresholds
};

export default function () {
  const userId = getRandomUser();
  const productId = getRandomProduct();
  
  const rand = Math.random();
  
  if (rand < 0.7) {
    // 70% GET /products/{id}
    get(`/products/${productId}`, userId);
    sleep(1);
  } else if (rand < 0.9) {
    // 20% POST /cart/add
    post('/cart/add', {
      productId: productId.toString(),
      quantity: 1
    }, userId);
    sleep(1);
  } else {
    // 10% POST /checkout
    performCheckoutFlow(userId, productId, 1, true);
  }
}

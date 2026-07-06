import { sleep } from 'k6';
import { get, post } from '../utils/http.js';
import { performCheckoutFlow } from '../utils/checkout.js';
import { getRandomUser, getRandomProduct } from '../utils/data.js';
import defaultThresholds from '../thresholds/default.js';

export const options = {
  stages: [
    { duration: '30m', target: 500 }, // 30 minutes soak test at 500 VUs
  ],
  thresholds: defaultThresholds
};

export default function () {
  const userId = getRandomUser();
  const productId = getRandomProduct();
  
  const rand = Math.random();
  
  if (rand < 0.7) {
    get(`/products/${productId}`, userId);
    sleep(1);
  } else if (rand < 0.9) {
    post('/cart/add', {
      productId: productId.toString(),
      quantity: 1
    }, userId);
    sleep(1);
  } else {
    performCheckoutFlow(userId, productId, 1, true);
  }
}

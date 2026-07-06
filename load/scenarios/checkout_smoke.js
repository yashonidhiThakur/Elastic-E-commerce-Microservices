import { sleep } from 'k6';
import { performCheckoutFlow } from '../utils/checkout.js';
import { getRandomUser, getRandomProduct } from '../utils/data.js';

export const options = {
  vus: 1,
  duration: '10s',
};

export default function () {
  const userId = getRandomUser();
  const productId = getRandomProduct();
  
  performCheckoutFlow(userId, productId, 1, true);
  sleep(1);
}

import { check, sleep } from 'k6';
import { post, get } from './http.js';
import { generateIdempotencyKey } from './data.js';
import { Counter } from 'k6/metrics';

export const checkoutSuccessCount = new Counter('checkout_success_count');

export function performCheckoutFlow(userId, productId, quantity, withThinkTime = true) {
  // 1. Add to cart
  const cartRes = post('/cart/add', {
    productId: productId.toString(),
    quantity: quantity
  }, userId);
  
  check(cartRes, {
    'cart add status is 200': (r) => r.status === 200,
  });

  if (withThinkTime) sleep(1);

  // 2. Checkout
  const idempotencyKey = generateIdempotencyKey();
  const payload = {
    items: [{ productId: productId.toString(), quantity: quantity }],
    totalAmount: 100 // dummy amount
  };
  const checkoutRes = post('/checkout', payload, userId, {
    'Idempotency-Key': idempotencyKey
  });

  const isAccepted = check(checkoutRes, {
    'checkout status is 202': (r) => r.status === 202,
  });

  if (!isAccepted) return false;

  let orderId;
  try {
    orderId = checkoutRes.json('orderId');
  } catch (e) {
    return false;
  }
  
  if (!orderId) return false;

  // 3. Poll for terminal state
  let terminal = false;
  let success = false;
  let attempts = 0;
  
  while (!terminal && attempts < 10) {
    if (withThinkTime) sleep(1);
    attempts++;
    
    const orderRes = get(`/orders/${orderId}`, userId);
    if (orderRes.status === 200) {
      const status = orderRes.json('status');
      if (status === 'CONFIRMED') {
        terminal = true;
        success = true;
        checkoutSuccessCount.add(1);
      } else if (status === 'FAILED' || status === 'CANCELLED') {
        terminal = true;
      }
    }
  }

  return success;
}

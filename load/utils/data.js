import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

export function getRandomUser() {
  // Assuming users 1 to 1000 exist in the DB
  return Math.floor(Math.random() * 1000) + 1;
}

export function getRandomProduct() {
  // Assuming products 1 to 100 exist in the DB
  return Math.floor(Math.random() * 100) + 1;
}

export function generateIdempotencyKey() {
  return uuidv4();
}

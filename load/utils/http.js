import http from 'k6/http';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function getHeaders(userId) {
  return {
    'Content-Type': 'application/json',
    'X-User-Id': userId,
  };
}

export function get(endpoint, userId) {
  return http.get(`${BASE_URL}${endpoint}`, {
    headers: getHeaders(userId)
  });
}

export function post(endpoint, payload, userId, additionalHeaders = {}) {
  const headers = Object.assign({}, getHeaders(userId), additionalHeaders);
  return http.post(`${BASE_URL}${endpoint}`, JSON.stringify(payload), {
    headers: headers
  });
}

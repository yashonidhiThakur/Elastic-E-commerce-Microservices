export default {
  http_req_duration: ['p(95)<1000', 'p(99)<3000'],
  http_req_failed:   ['rate<0.01'],
  checks:            ['rate>0.99'],
};

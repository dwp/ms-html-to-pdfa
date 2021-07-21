import { check } from 'k6';
import { post } from 'k6/http';
import { b64encode } from 'k6/encoding';

// Default options (override with cli args)
export let options = {
  vus: 20,
  duration: '1m',
  thresholds: {
    http_req_waiting: ['p(95)<500'], // 95% of requests should be below 500ms
  },
};

// Load test payload from external source
const payload = open(`/k6/load-test/payload.html`);

// Setup
export function setup () {
  return {
    pageHtml: b64encode(payload),
  };
}

// Test
export default function ({ pageHtml }) {
  const url = `http://${__ENV.TARGET_HOST || 'host.docker.internal:6677'}/generatePdf`;
  const payload = JSON.stringify({
    page_html: pageHtml,
    conformance_level: 'PDFA_1_A',
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const response = post(url, payload, params);

  check(response, {
    'is status 200': (r) => r.status === 200,
    'is not empty': (r) => r.body.length > 0,
  });
}

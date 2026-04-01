import http from 'k6/http';
import { check } from 'k6';

export const options = {
	scenarios: {
		stress_test: {
			executor: 'ramping-arrival-rate',
			startRate: 5,
			timeUnit: '1s',
			preAllocatedVUs: 100,
			maxVUs: 250,
			stages: [
				{ target: 50, duration: '30s' },
				{ target: 100, duration: '30s' },
				{ target: 125, duration: '30s' },
				{ target: 150, duration: '45s' },
				{ target: 175, duration: '1m' },
				{ target: 200, duration: '1m' },
				{ target: 200, duration: '1m' },
				{ target: 200, duration: '1m' },
				{ target: 175, duration: '1m' },
				{ target: 150, duration: '45s' },
				{ target: 125, duration: '30s' },
				{ target: 100, duration: '30s' },
				{ target: 75, duration: '30s' },
				{ target: 50, duration: '30s' },
				{ target: 25, duration: '30s' },
				{ target: 0, duration: '30s' },
			],
		},
	},
	// Extensive stats to see the "tail latency" impact of caching
	summaryTrendStats: ['avg', 'med', 'p(80)', 'p(90)', 'p(95)', 'p(99)', 'p(99.9)'],
	thresholds: {
		http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: false }],
		http_req_duration: [{ threshold: 'p(95)<1000', abortOnFail: false }],
	},
};

export default function () {
	const jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3VybHMuaHJpZGF5a2guaW4vb2F1dGgvY2FsbGJhY2siLCJzdWIiOjIsImF1ZCI6InVybHMuaHJpZGF5a2guaW4iLCJleHAiOjE3NzUwNjY2NjksIm5iZiI6MTc3NTA2NTc2OSwiaWF0IjoxNzc1MDY1NzY5LCJqdGkiOjUzLCJ2ZXIiOjEsImVtYWlsIjoiaHJpZGF5a2gxMjM0QGdtYWlsLmNvbSIsInBmcCI6Imh0dHBzOi8vYXZhdGFycy5naXRodWJ1c2VyY29udGVudC5jb20vdS85MzA1MDU4Mj92PTQifQ.XMhqAFp0uCwkqW7SeVZ0D54MHuScYAQ8zd30rqMuA1s";
	const rt = "U8NueU_D9QSVpDMj1WMtyMz_dMoN4GsQ4xuYpv0VyD787H7gzWZFtrLr0Xuyk2X3P2pmDyUNWG52XhtXAFQ4cA";
	const params = {
		headers: {
			'Cookie': `jwt=${jwt}; refreshToken=${rt}`
		}
	};

	const res = http.get('https://urls.hridaykh.in/', params);

	check(res, {
		'status is 200': (r) => r.status === 200,
	});
}
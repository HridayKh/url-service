import http from 'k6/http';
import { check } from 'k6';

export const options = {
	scenarios: {
		stress_test: {
			executor: 'ramping-arrival-rate',
			startRate: 30,       // Start at 10 iterations per second
			timeUnit: '1s',
			preAllocatedVUs: 100, // Initial pool of VUs
			maxVUs: 250,          // Max VUs if response times get slow
			stages: [
				{ target: 50, duration: '30s' }, // Ramp to 50 RPS
				{ target: 100, duration: '30s' }, // Ramp to 100 RPS
				{ target: 150, duration: '30s' }, // Ramp to 150 RPS
				{ target: 200, duration: '30s' }, // Ramp to 200 RPS
				{ target: 200, duration: '30s' }, // Hold 200 RPS to check stability
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
	const jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3VybHMuaHJpZGF5a2guaW4vb2F1dGgvY2FsbGJhY2siLCJzdWIiOjIsImF1ZCI6InVybHMuaHJpZGF5a2guaW4iLCJleHAiOjE3NzU2NjI0MDgsIm5iZiI6MTc3NTY2MTUwOCwiaWF0IjoxNzc1NjYxNTA4LCJqdGkiOjUzLCJ2ZXIiOjEsImVtYWlsIjoiaHJpZGF5a2gxMjM0QGdtYWlsLmNvbSIsInBmcCI6Imh0dHBzOi8vYXZhdGFycy5naXRodWJ1c2VyY29udGVudC5jb20vdS85MzA1MDU4Mj92PTQifQ.xlL_rJDm6J57f-XojcrisEoyM2o71GdrRx8PudguIYI";
	const rt = "KR6QeKPEfcn_7BanjpoNPzXZ-Ivq2S0r4GAwmrlfV-iAhvHNjoo0KK43GO75dnM2n_Z8948JvBp6COA-0f1ERg";
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

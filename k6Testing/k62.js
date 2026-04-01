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
	const jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3VybHMuaHJpZGF5a2guaW4vb2F1dGgvY2FsbGJhY2siLCJzdWIiOjIsImF1ZCI6InVybHMuaHJpZGF5a2guaW4iLCJleHAiOjE3NzUwNjQ1NjYsIm5iZiI6MTc3NTA2MzY2NiwiaWF0IjoxNzc1MDYzNjY2LCJqdGkiOjUzLCJ2ZXIiOjEsImVtYWlsIjoiaHJpZGF5a2gxMjM0QGdtYWlsLmNvbSIsInBmcCI6Imh0dHBzOi8vYXZhdGFycy5naXRodWJ1c2VyY29udGVudC5jb20vdS85MzA1MDU4Mj92PTQifQ.ZCZKM2T6So6ZSpBgepCSnLHPi31Bn8IiA7vizoIy6i8";
	const rt = "3HwvnROdw6Y0QmX12MMufWI1HmD7x-Ubh7AQ6q9oSj8CFUP2fpSwXUQqX200CeEwyxSeJxWLSdILk1rPwTpEQA";
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
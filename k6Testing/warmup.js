import http from 'k6/http';
import { check } from 'k6';

export const options = {
	scenarios: {
		warmup: {
			executor: 'ramping-arrival-rate',
			startRate: 5,
			timeUnit: '1s',
			preAllocatedVUs: 50,
			maxVUs: 100,
			stages: [
				{ target: 10, duration: '30s' },
				{ target: 30, duration: '1m' },
				{ target: 70, duration: '1m' },
				{ target: 100, duration: '1m' },
			],
		},
	},
	summaryTrendStats: ['avg', 'med', 'p(80)', 'p(90)', 'p(95)', 'p(99)', 'p(99.9)'],
	thresholds: {
		http_req_failed: [{ threshold: 'rate<0.05', abortOnFail: false }],
		http_req_duration: [{ threshold: 'p(95)<1000', abortOnFail: false }],
	},
};

export default function () {
	const jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL3VybHMuaHJpZGF5a2guaW4vb2F1dGgvY2FsbGJhY2siLCJzdWIiOjIsImF1ZCI6InVybHMuaHJpZGF5a2guaW4iLCJleHAiOjE3NzUwNjU3MjUsIm5iZiI6MTc3NTA2NDgyNSwiaWF0IjoxNzc1MDY0ODI1LCJqdGkiOjUzLCJ2ZXIiOjEsImVtYWlsIjoiaHJpZGF5a2gxMjM0QGdtYWlsLmNvbSIsInBmcCI6Imh0dHBzOi8vYXZhdGFycy5naXRodWJ1c2VyY29udGVudC5jb20vdS85MzA1MDU4Mj92PTQifQ.m6EQ-5-KqYp3AuDj8GTM_PKjeoirLyUy17ngbwWuy2c";
	const rt = "cCN2gbyLtubfZ8ZhSPFr_PQtOS-CumvWOqtw24Bg3wNbaQ8cfqgev6ENVSbDu9PKrnKJFInPWqDSUERZM4HpOg";
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
import http from 'k6/http';
import { sleep } from 'k6';
import { check } from 'k6';

// Use the BASE_URL environment variable for the API base URL
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080'; // Fallback to localhost if not set

export default function () {
    const url = `${BASE_URL}/rates?from=USD&to=JPY`;
    const params = {
        headers: {
            'token': '10dc303535874aeccc86a8251e6992f5',
        },
    };

    const response = http.get(url, params);

    // Parse the JSON response
    const data = JSON.parse(response.body);

    // Get the current timestamp
    const currentTime = new Date();

    // Parse the response timestamp
    const responseTime = new Date(data.timestamp);

    // Check if the response timestamp is not older than 5 minutes
    const fiveMinutesInMillis = 5 * 60 * 1000; // 5 minutes in milliseconds
    const isRecent = currentTime - responseTime <= fiveMinutesInMillis;

    // Assert that the timestamp is recent
    check(isRecent, {
        'response timestamp is recent': (v) => v === true,
    });

    sleep(0.1); // 100 ms delay
}

export const options = {
    iterations: 11000,
    vus: 1,  // 1 virtual user
};

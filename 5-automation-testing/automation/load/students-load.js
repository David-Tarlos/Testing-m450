import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

/**
 * Uebung 3 - Lasttest der Student-API mit k6.
 *
 *   k6 run students-load.js                    (Standard: Szenario "load")
 *   k6 run students-load.js -e SCENARIO=smoke
 *   k6 run students-load.js -e SCENARIO=stress
 *
 * Ergebnis zusaetzlich als JSON sichern:
 *   k6 run students-load.js --summary-export=results/summary-load.json
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const SCENARIO = __ENV.SCENARIO || 'load';

// http_req_duration mischt Lesen und Schreiben zu einer Zahl. Getrennt gemessen
// sieht man, ob das Schreiben teurer ist als das Lesen.
const leseDauer = new Trend('lese_dauer', true);
const schreibDauer = new Trend('schreib_dauer', true);

const SZENARIEN = {
  smoke: {
    executor: 'shared-iterations',
    vus: 1,
    iterations: 10,
  },
  load: {
    executor: 'ramping-vus',
    stages: [
      { duration: '15s', target: 20 },
      { duration: '30s', target: 20 },
      { duration: '10s', target: 0 },
    ],
  },
  stress: {
    executor: 'ramping-arrival-rate',
    startRate: 50,
    timeUnit: '1s',
    preAllocatedVUs: 50,
    maxVUs: 400,
    stages: [
      { duration: '20s', target: 200 },
      { duration: '20s', target: 600 },
      { duration: '10s', target: 0 },
    ],
  },
};

export const options = {
  scenarios: { [SCENARIO]: SZENARIEN[SCENARIO] },
  // Thresholds machen aus dem Lasttest ein Pass/Fail: wird einer gerissen,
  // endet k6 mit Exit-Code 99 und eine Pipeline koennte rot werden.
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate>0.99'],
    lese_dauer: ['p(95)<300'],
    schreib_dauer: ['p(95)<600'],
  },
};

/** Laeuft einmal vor allen VUs - bricht frueh ab, wenn das Backend fehlt. */
export function setup() {
  const response = http.get(`${BASE_URL}/students`);
  if (response.status !== 200) {
    throw new Error(`Backend nicht erreichbar auf ${BASE_URL} (Status ${response.status})`);
  }
  console.log(`Start: ${response.json().length} Studenten in der Datenbank`);
}

/** Das, was jeder virtuelle Nutzer wiederholt tut. */
export default function () {
  const liste = http.get(`${BASE_URL}/students`);
  leseDauer.add(liste.timings.duration);
  check(liste, {
    'Status 200': (r) => r.status === 200,
    'Liste ist nicht leer': (r) => r.json().length > 0,
  });

  // Realistischer Mix: die meisten schauen nur, jeder fuenfte erfasst etwas.
  if (Math.random() < 0.2) {
    const name = `k6-${__VU}-${__ITER}`;
    const angelegt = http.post(
      `${BASE_URL}/students`,
      JSON.stringify({ name, email: `${name}@tbz.ch` }),
      { headers: { 'Content-Type': 'application/json' } },
    );
    schreibDauer.add(angelegt.timings.duration);
    check(angelegt, { 'Anlegen erfolgreich': (r) => r.status === 200 });
  }

  sleep(1); // Denkzeit - ohne sie misst man nur die eigene CPU
}

/** Laeuft einmal am Ende. */
export function teardown() {
  const anzahl = http.get(`${BASE_URL}/students`).json().length;
  console.log(`Ende: ${anzahl} Studenten in der Datenbank`);
}

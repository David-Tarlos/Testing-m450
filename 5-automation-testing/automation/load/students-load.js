import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

/**
 * Uebung 3 - Lasttest der Student-API mit k6.
 *
 * Aufruf:
 *   k6 run students-load.js                  (Standard: Szenario "load")
 *   k6 run students-load.js -e SCENARIO=smoke
 *   k6 run students-load.js -e SCENARIO=stress
 *   k6 run students-load.js -e BASE_URL=http://host.docker.internal:8081   (im Container)
 *
 * Das Skript demonstriert bewusst mehrere k6-Konzepte auf einmal:
 * Szenarien mit verschiedenen Executors, Thresholds als Pass/Fail-Kriterium,
 * Checks, eigene Metriken, Gruppen, setup/teardown und handleSummary.
 */

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const SCENARIO = __ENV.SCENARIO || 'load';

// --- Eigene Metriken -------------------------------------------------------
// http_req_duration mischt Lesen und Schreiben. Getrennte Trends zeigen, ob
// das Schreiben teurer ist als das Lesen.
const leseDauer = new Trend('lese_dauer', true);
const schreibDauer = new Trend('schreib_dauer', true);
const fachlicheFehler = new Rate('fachliche_fehler');
const angelegteStudenten = new Counter('angelegte_studenten');

// --- Szenarien -------------------------------------------------------------
const SZENARIEN = {
  // Kurzer Rauchtest: laeuft die Strecke ueberhaupt?
  smoke: {
    executor: 'shared-iterations',
    vus: 1,
    iterations: 10,
    maxDuration: '30s',
  },
  // Normallast: langsam auf 20 gleichzeitige Nutzer hochfahren und halten.
  load: {
    executor: 'ramping-vus',
    startVUs: 0,
    stages: [
      { duration: '15s', target: 20 },
      { duration: '30s', target: 20 },
      { duration: '10s', target: 0 },
    ],
    gracefulRampDown: '5s',
  },
  // Stress: feste Anfragerate statt fester Nutzerzahl. So sieht man, ab
  // welcher Rate der Server nicht mehr mitkommt (die VUs stauen sich dann).
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
  // Thresholds machen aus dem Lasttest ein Pass/Fail-Kriterium: wird eines
  // gerissen, beendet k6 den Lauf mit Exit-Code 99 - CI-tauglich.
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500', 'p(99)<1000'],
    checks: ['rate>0.99'],
    lese_dauer: ['p(95)<300'],
    schreib_dauer: ['p(95)<600'],
  },
};

// --- setup: laeuft einmal vor allen VUs -------------------------------------
export function setup() {
  const response = http.get(`${BASE_URL}/students`);
  if (response.status !== 200) {
    throw new Error(`Backend nicht erreichbar auf ${BASE_URL} (Status ${response.status})`);
  }
  const startAnzahl = response.json().length;
  console.log(`Start: ${startAnzahl} Studenten in der Datenbank`);
  return { startAnzahl };
}

// --- Das, was jeder virtuelle Nutzer wiederholt tut --------------------------
export default function () {
  group('Studenten lesen', () => {
    const response = http.get(`${BASE_URL}/students`, {
      tags: { name: 'GET /students' },
    });

    leseDauer.add(response.timings.duration);

    const ok = check(response, {
      'Status 200': (r) => r.status === 200,
      'Antwort ist ein Array': (r) => Array.isArray(r.json()),
      'Liste ist nicht leer': (r) => r.json().length > 0,
    });
    fachlicheFehler.add(!ok);
  });

  // Realistischer Mix: die meisten Nutzer schauen nur, jeder fuenfte erfasst.
  if (Math.random() < 0.2) {
    group('Student anlegen', () => {
      const name = `k6-${__VU}-${__ITER}`;
      const response = http.post(
        `${BASE_URL}/students`,
        JSON.stringify({ name, email: `${name}@tbz.ch` }),
        {
          headers: { 'Content-Type': 'application/json' },
          tags: { name: 'POST /students' },
        },
      );

      schreibDauer.add(response.timings.duration);

      const ok = check(response, {
        'Status 200': (r) => r.status === 200,
      });
      fachlicheFehler.add(!ok);
      if (ok) angelegteStudenten.add(1);
    });
  }

  sleep(1); // Denkzeit - ohne sie misst man nur die eigene CPU
}

// --- teardown: laeuft einmal nach allen VUs ---------------------------------
export function teardown(data) {
  const endAnzahl = http.get(`${BASE_URL}/students`).json().length;
  console.log(
    `Ende: ${endAnzahl} Studenten (${endAnzahl - data.startAnzahl} waehrend des Laufs angelegt)`,
  );
}

// --- Eigene Zusammenfassung -------------------------------------------------
// handleSummary ersetzt die Standardausgabe. Wir schreiben zusaetzlich eine
// JSON-Datei, damit sich Laeufe spaeter vergleichen lassen.
export function handleSummary(data) {
  const m = data.metrics;
  const wert = (name, feld) => m[name]?.values?.[feld];
  const ms = (v) => (v === undefined ? '-' : `${v.toFixed(1)} ms`);
  const pct = (v) => (v === undefined ? '-' : `${(v * 100).toFixed(2)} %`);

  const zeilen = [
    '',
    '='.repeat(62),
    `  Szenario: ${SCENARIO}   Ziel: ${BASE_URL}`,
    `  Dauer: ${(data.state.testRunDurationMs / 1000).toFixed(1)} s`,
    '='.repeat(62),
    `  Anfragen total       ${wert('http_reqs', 'count') ?? '-'}`,
    `  Anfragen pro Sekunde ${wert('http_reqs', 'rate')?.toFixed(1) ?? '-'}`,
    `  Max. gleichzeitige VU ${wert('vus_max', 'max') ?? '-'}`,
    `  Fehlerquote HTTP     ${pct(wert('http_req_failed', 'rate'))}`,
    `  Checks bestanden     ${pct(wert('checks', 'rate'))}`,
    `  Angelegte Studenten  ${wert('angelegte_studenten', 'count') ?? 0}`,
    '-'.repeat(62),
    `  Antwortzeit  med ${ms(wert('http_req_duration', 'med'))}` +
      `  p95 ${ms(wert('http_req_duration', 'p(95)'))}` +
      `  max ${ms(wert('http_req_duration', 'max'))}`,
    `  davon Lesen  p95 ${ms(wert('lese_dauer', 'p(95)'))}`,
    `  davon Schreiben p95 ${ms(wert('schreib_dauer', 'p(95)'))}`,
    '-'.repeat(62),
    '  Thresholds:',
  ];

  for (const [name, metrik] of Object.entries(m)) {
    for (const [regel, ergebnis] of Object.entries(metrik.thresholds ?? {})) {
      zeilen.push(`    ${ergebnis.ok ? 'OK  ' : 'FAIL'}  ${name}: ${regel}`);
    }
  }
  zeilen.push('='.repeat(62), '');

  return {
    stdout: zeilen.join('\n'),
    [`results/summary-${SCENARIO}.json`]: JSON.stringify(data, null, 2),
  };
}

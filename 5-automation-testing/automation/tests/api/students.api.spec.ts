import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * Uebung 1 - automatisierte Tests der REST-Schnittstelle.
 *
 * Playwright ohne Browser: die `request`-Fixture ist ein reiner HTTP-Client.
 * So deckt ein Werkzeug beide Uebungen ab (API + GUI).
 *
 * Die Tests legen echte Daten an. Deshalb nie auf eine exakte Anzahl pruefen,
 * sondern auf "enthaelt" - sonst faellt der zweite Lauf um.
 */

type Student = { id: number; name: string; email: string };

/** Eindeutiger Name pro Lauf, damit sich Laeufe nicht in die Quere kommen. */
const unique = (prefix: string) =>
  `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;

async function getStudents(request: APIRequestContext): Promise<Student[]> {
  const response = await request.get('/students');
  expect(response.status()).toBe(200);
  return response.json();
}

test.describe('GET /students', () => {
  test('antwortet mit 200 und einem JSON-Array', async ({ request }) => {
    const response = await request.get('/students');

    expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toContain('application/json');
    expect(Array.isArray(await response.json())).toBe(true);
  });

  test('enthaelt die fuenf Studenten aus dem CommandLineRunner', async ({ request }) => {
    const names = (await getStudents(request)).map((s) => s.name);

    expect(names).toEqual(
      expect.arrayContaining(['Jonas', 'Patrick', 'Yves', 'Peter', 'Ann']),
    );
  });

  test('jeder Eintrag haelt den Contract ein (id, name, email)', async ({ request }) => {
    const students = await getStudents(request);

    expect(students.length).toBeGreaterThan(0);
    for (const student of students) {
      expect(typeof student.id).toBe('number');
      expect(typeof student.name).toBe('string');
      expect(typeof student.email).toBe('string');
      // keine unerwarteten Felder - schlaegt an, sobald jemand das Schema aendert
      expect(Object.keys(student).sort()).toEqual(['email', 'id', 'name']);
    }
  });
});

test.describe('POST /students', () => {
  test('legt einen Studenten an, der danach in der Liste steht', async ({ request }) => {
    const name = unique('Ada');
    const email = `${name}@tbz.ch`;

    const response = await request.post('/students', { data: { name, email } });

    // Ist-Zustand: der Controller gibt `void` zurueck. REST-ueblich waere
    // 201 Created mit Location-Header und dem angelegten Objekt im Body.
    expect(response.status()).toBe(200);
    expect(await response.text()).toBe('');

    const created = (await getStudents(request)).find((s) => s.name === name);
    expect(created).toBeDefined();
    expect(created!.email).toBe(email);
    expect(created!.id).toBeGreaterThan(0);
  });

  test('ignoriert eine mitgeschickte id und unbekannte Felder', async ({ request }) => {
    const name = unique('Linus');

    const response = await request.post('/students', {
      data: { id: 999999, name, email: 'l@tbz.ch', rolle: 'admin', unbekannt: 42 },
    });
    expect(response.status()).toBe(200);

    const created = (await getStudents(request)).find((s) => s.name === name);
    expect(created!.id).not.toBe(999999);
    // unbekannte Felder werden stillschweigend verworfen statt abgelehnt
    expect(Object.keys(created!).sort()).toEqual(['email', 'id', 'name']);
  });

  test('lehnt kaputtes JSON mit 400 ab', async ({ request }) => {
    const response = await request.post('/students', {
      headers: { 'Content-Type': 'application/json' },
      data: '{"name":',
    });

    expect(response.status()).toBe(400);
  });

  test('lehnt einen falschen Content-Type mit 415 ab', async ({ request }) => {
    const response = await request.post('/students', {
      headers: { 'Content-Type': 'text/plain' },
      data: 'nur text',
    });

    expect(response.status()).toBe(415);
  });
});

test.describe('CORS und Routen', () => {
  test('erlaubt Port 4200, aber keine fremde Origin', async ({ request }) => {
    const erlaubt = await request.get('/students', {
      headers: { Origin: 'http://localhost:4200' },
    });
    expect(erlaubt.headers()['access-control-allow-origin']).toBe('http://localhost:4200');

    const fremd = await request.get('/students', {
      headers: { Origin: 'http://evil.example.com' },
    });
    expect(fremd.headers()['access-control-allow-origin']).toBeUndefined();
  });

  test('die API kann nur lesen und anlegen - alles andere ist 404', async ({ request }) => {
    expect((await request.get('/students/1')).status()).toBe(404);
    expect((await request.delete('/students/1')).status()).toBe(404);
    expect((await request.get('/gibtsnicht')).status()).toBe(404);
  });
});

/**
 * Urspruenglich nahm der Endpunkt jede Eingabe an (leere Namen, ungueltige
 * E-Mails, sogar `{}` - alles 200 OK). Das Bonus-Feature hat die Luecke
 * geschlossen, deshalb erwarten die Tests jetzt 400 mit feldgenauen Meldungen.
 */
test.describe('Eingabevalidierung (Bonus-Feature)', () => {
  test('lehnt leere Werte mit 400 und Meldungen zu beiden Feldern ab', async ({ request }) => {
    const response = await request.post('/students', { data: { name: '', email: '' } });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.error).toBe('Validierungsfehler');
    expect(body.fields.name).toContain('Name darf nicht leer sein');
    expect(body.fields.email).toContain('E-Mail darf nicht leer sein');
  });

  test('lehnt ein komplett leeres Objekt mit 400 ab', async ({ request }) => {
    const response = await request.post('/students', { data: {} });

    expect(response.status()).toBe(400);
    expect(Object.keys((await response.json()).fields).sort()).toEqual(['email', 'name']);
  });

  test('meldet nur das Feld, das tatsaechlich falsch ist', async ({ request }) => {
    const response = await request.post('/students', {
      data: { name: unique('Bob'), email: 'das-ist-keine-email' },
    });

    expect(response.status()).toBe(400);
    const body = await response.json();
    expect(body.fields.email).toContain('keine gueltige Adresse');
    expect(body.fields.name).toBeUndefined(); // der Name war ja in Ordnung
  });

  /** Die Laengenregel prueft nur das Backend - darauf baut der E2E-Test auf. */
  test('lehnt einen zu langen Namen mit 400 ab', async ({ request }) => {
    const response = await request.post('/students', {
      data: { name: 'A'.repeat(101), email: 'lang@tbz.ch' },
    });

    expect(response.status()).toBe(400);
    expect((await response.json()).fields.name).toContain('100 Zeichen');
  });

  test('speichert nichts bei Ablehnung, laesst Gueltiges aber durch', async ({ request }) => {
    const abgelehnt = unique('Abgelehnt');
    const gueltig = unique('Gueltig');

    expect((await request.post('/students', {
      data: { name: abgelehnt, email: 'kaputt' },
    })).status()).toBe(400);

    expect((await request.post('/students', {
      data: { name: gueltig, email: `${gueltig.toLowerCase()}@tbz.ch` },
    })).status()).toBe(200);

    const names = (await getStudents(request)).map((s) => s.name);
    expect(names).not.toContain(abgelehnt);
    expect(names).toContain(gueltig);
  });
});

import { test, expect } from '@playwright/test';
import { BACKEND_URL } from '../../playwright.config';

/**
 * Uebung 2 - End-to-End-Tests des Angular-GUI in einem echten Chromium.
 *
 * Kein Mocking: der Browser redet mit dem echten Angular-Dev-Server auf 4200,
 * der wiederum mit dem echten Spring-Boot-Backend auf 8081. Ein Test faellt
 * also auch dann um, wenn CORS falsch konfiguriert oder das Backend tot ist -
 * genau das ist bei einem E2E-Test gewollt.
 *
 * Die Tests laufen parallel und legen echte Daten an. Deshalb nie auf eine
 * exakte Zeilenzahl pruefen, sondern immer auf "enthaelt" - sonst vergleicht
 * man zwei Momentaufnahmen eines Zustands, den andere Tests gerade veraendern.
 */

const unique = (prefix: string) =>
  `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;

/** Legt ueber das Formular einen Studenten an und wartet auf die Umleitung. */
async function erfasse(page: import('@playwright/test').Page, name: string) {
  await page.goto('/addstudents');
  await page.locator('#name').fill(name);
  await page.locator('#email').fill(`${name.toLowerCase()}@tbz.ch`);
  await page.getByRole('button', { name: 'Submit' }).click();
  await expect(page).toHaveURL(/\/students$/);
}

test.describe('Navigation', () => {
  test('die Startseite verlinkt auf Liste und Formular', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle('TBZ Students');
    await expect(page.locator('img[alt="Responsive image"]')).toBeVisible();

    await page.getByRole('link', { name: 'List Students' }).click();
    await expect(page).toHaveURL(/\/students$/);
    await expect(page.locator('table')).toBeVisible();

    await page.getByRole('link', { name: 'Add Students' }).click();
    await expect(page).toHaveURL(/\/addstudents$/);
    await expect(page.locator('#name')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
  });
});

test.describe('Studentenliste', () => {
  test('zeigt die Daten aus dem Backend in den richtigen Spalten', async ({ page }) => {
    await page.goto('/students');

    await expect(page.locator('table thead th')).toHaveText(['#', 'Name', 'Email']);
    // die Seed-Daten des CommandLineRunner muessen im GUI ankommen
    for (const name of ['Jonas', 'Patrick', 'Yves', 'Peter', 'Ann']) {
      await expect(page.getByRole('cell', { name, exact: true })).toBeVisible();
    }
  });

  test('zeigt die E-Mail als mailto-Link', async ({ page }) => {
    await page.goto('/students');

    await expect(page.getByRole('link', { name: 'jonas@tbz.ch' }))
      .toHaveAttribute('href', 'mailto:jonas@tbz.ch');
  });
});

test.describe('Studenten erfassen', () => {
  test('der Submit-Button ist erst mit beiden Feldern aktiv', async ({ page }) => {
    await page.goto('/addstudents');
    const submit = page.getByRole('button', { name: 'Submit' });

    await expect(submit).toBeDisabled();

    await page.locator('#name').fill('Nur ein Name');
    await expect(submit).toBeDisabled();

    await page.locator('#email').fill('voll@tbz.ch');
    await expect(submit).toBeEnabled();
  });

  /**
   * Der eigentliche End-to-End-Durchstich: GUI -> HTTP -> Spring -> H2 -> GUI.
   * Die anschliessende API-Abfrage beweist, dass wirklich gespeichert wurde
   * und nicht nur die Anzeige stimmt.
   */
  test('ein erfasster Student landet in der Liste und in der Datenbank',
    async ({ page, request }) => {
      const name = unique('E2E-Student');

      await erfasse(page, name);

      await expect(page.getByRole('cell', { name, exact: true })).toBeVisible();
      await expect(page.getByRole('link', { name: `${name.toLowerCase()}@tbz.ch` }))
        .toBeVisible();

      const fromApi = await (await request.get(`${BACKEND_URL}/students`)).json();
      expect(fromApi.map((s: { name: string }) => s.name)).toContain(name);
    });

  test('nach dem Neuladen ist der Student noch da', async ({ page }) => {
    const name = unique('Persistent');

    await erfasse(page, name);
    await page.reload();

    await expect(page.getByRole('cell', { name, exact: true })).toBeVisible();
  });
});

/**
 * Beim Schreiben der Tests gefunden: die Fehlermeldungen hingen an `pristine`
 * ("noch nie angefasst") statt an `invalid`.
 *
 *   <div [hidden]="!name.pristine" class="alert alert-danger">Name is required</div>
 *
 * Die Meldung stand also auf dem leeren Formular und verschwand, sobald man
 * tippte - auch wenn man das Feld danach wieder leerte. Genau dann braucht man
 * sie aber. Das Bonus-Feature hat die Logik korrigiert (siehe bonus-feature.md),
 * diese Tests sichern das neue Verhalten ab.
 */
test.describe('Formularvalidierung (Bonus-Feature)', () => {
  test('die Meldung steht nicht auf dem leeren Formular, aber nach dem Leeren',
    async ({ page }) => {
      await page.goto('/addstudents');
      const name = page.locator('#name');

      // unberuehrt: keine Meldung - genau das war vorher falsch
      await expect(page.getByText('Name is required')).toBeHidden();

      await name.fill('etwas');
      await name.fill(''); // wieder leer -> ungueltig und angefasst

      await expect(page.getByText('Name is required')).toBeVisible();
      await expect(page.getByRole('button', { name: 'Submit' })).toBeDisabled();
    });

  test('ein ungueltiges E-Mail-Format wird gemeldet und verschwindet wieder',
    async ({ page }) => {
      await page.goto('/addstudents');
      const email = page.locator('#email');
      const meldung = page.getByText('Email is not a valid address');
      const submit = page.getByRole('button', { name: 'Submit' });

      await page.locator('#name').fill('Testperson');
      await email.fill('das-ist-keine-email');
      await email.blur();

      await expect(meldung).toBeVisible();
      await expect(submit).toBeDisabled();

      await email.fill('richtig@tbz.ch');

      await expect(meldung).toBeHidden();
      await expect(submit).toBeEnabled();
    });

  /**
   * Verteidigung in der Tiefe: die Laengenregel prueft nur das Backend. Dieser
   * Test geht deshalb den ganzen Weg GUI -> 400 -> Fehleranzeige.
   */
  test('zeigt Backend-Fehler an, die der Client nicht selbst erkennt', async ({ page }) => {
    await page.goto('/addstudents');

    await page.locator('#name').fill('A'.repeat(101));
    await page.locator('#email').fill('zulang@tbz.ch');
    await page.getByRole('button', { name: 'Submit' }).click();

    await expect(page.locator('#server-errors')).toBeVisible();
    await expect(page.getByText(/100 Zeichen/)).toBeVisible();
    // kein Weiterleiten - der Nutzer bleibt am Formular und kann korrigieren
    await expect(page).toHaveURL(/\/addstudents$/);
  });
});

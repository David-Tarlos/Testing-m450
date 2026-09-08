import { test, expect, Page } from '@playwright/test';
import { BACKEND_URL } from '../../playwright.config';

/**
 * Uebung 2 - End-to-End-Tests des Angular-GUI in einem echten Chromium.
 *
 * Nichts wird gemockt: der Browser redet mit dem echten Angular auf 4200, das
 * mit dem echten Backend auf 8081. Ein Test faellt also auch dann um, wenn CORS
 * falsch steht oder das Backend tot ist - genau das ist bei E2E gewollt.
 */

const unique = (prefix: string) =>
  `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;

/** Legt ueber das Formular einen Studenten an und wartet auf die Umleitung. */
async function erfasse(page: Page, name: string) {
  await page.goto('/addstudents');
  await page.locator('#name').fill(name);
  await page.locator('#email').fill(`${name.toLowerCase()}@tbz.ch`);
  await page.getByRole('button', { name: 'Submit' }).click();
  await expect(page).toHaveURL(/\/students$/);
}

test.describe('Navigation', () => {
  test('die Startseite zeigt Titel und beide Navigationslinks', async ({ page }) => {
    await page.goto('/');

    await expect(page).toHaveTitle('TBZ Students');
    await expect(page.getByRole('link', { name: 'List Students' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Add Students' })).toBeVisible();
  });

  test('die Links fuehren auf Liste und Formular', async ({ page }) => {
    await page.goto('/');

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
  test('zeigt die Studenten aus dem Backend an', async ({ page }) => {
    await page.goto('/students');

    // die Seed-Daten des CommandLineRunner muessen im GUI ankommen
    for (const name of ['Jonas', 'Patrick', 'Yves', 'Peter', 'Ann']) {
      await expect(page.getByRole('cell', { name, exact: true })).toBeVisible();
    }
  });

  test('hat die Spalten #, Name und Email', async ({ page }) => {
    await page.goto('/students');

    await expect(page.locator('table thead th')).toHaveText(['#', 'Name', 'Email']);
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
   * Der eigentliche Durchstich: GUI -> HTTP -> Spring -> H2 -> GUI.
   * Die API-Abfrage danach beweist, dass wirklich gespeichert wurde und nicht
   * nur die Anzeige stimmt.
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
 * ("noch nie angefasst") statt an `invalid`. Die Meldung stand also auf dem
 * leeren Formular und verschwand, sobald man tippte - auch wenn man das Feld
 * danach wieder leerte. Das Bonus-Feature hat das korrigiert.
 */
test.describe('Formularvalidierung (Bonus-Feature)', () => {
  test('das unberuehrte Formular zeigt keine Fehlermeldung', async ({ page }) => {
    await page.goto('/addstudents');

    await expect(page.getByText('Name is required')).toBeHidden();
    await expect(page.getByText('Email is required')).toBeHidden();
  });

  test('die Meldung erscheint, sobald ein Pflichtfeld geleert wurde', async ({ page }) => {
    await page.goto('/addstudents');
    const name = page.locator('#name');

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

      await page.locator('#name').fill('Testperson');
      await email.fill('das-ist-keine-email');
      await email.blur();

      await expect(meldung).toBeVisible();
      await expect(page.getByRole('button', { name: 'Submit' })).toBeDisabled();

      await email.fill('richtig@tbz.ch');

      await expect(meldung).toBeHidden();
      await expect(page.getByRole('button', { name: 'Submit' })).toBeEnabled();
    });

  /** Die Laengenregel prueft nur das Backend - hier der Weg GUI -> 400 -> Anzeige. */
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

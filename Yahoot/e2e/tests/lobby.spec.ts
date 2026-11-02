import { expect, test } from '@playwright/test';
import { hostGame, joinAs, phone } from './helpers';

test('Handy tritt bei und der Beamer sieht den Namen', async ({ page, browser }) => {
  const code = await hostGame(page);
  await expect(page.getByRole('button', { name: 'Spiel starten' })).toBeDisabled();
  await expect(page.getByAltText('QR-Code zum Beitreten')).toBeVisible();

  const lena = await phone(browser);
  await lena.goto('/join');
  await lena.getByLabel('Spielcode').fill(code);
  await lena.getByLabel('Dein Name').fill('Lena');
  await lena.getByRole('button', { name: 'Mitspielen' }).click();

  await expect(lena.getByRole('heading', { name: 'Hallo Lena!' })).toBeVisible();
  await expect(page.getByRole('listitem').filter({ hasText: 'Lena' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Spiel starten' })).toBeEnabled();
});

test('Code im Link ist schon eingetragen', async ({ page, browser }) => {
  const code = await hostGame(page);

  const tim = await phone(browser);
  await tim.goto(`/join?code=${code}`);

  await expect(tim.getByLabel('Spielcode')).toHaveValue(code);
});

test('Doppelter Name wird abgelehnt', async ({ page, browser }) => {
  const code = await hostGame(page);
  const first = await phone(browser);
  const second = await phone(browser);

  for (const player of [first, second]) {
    await player.goto(`/join?code=${code}`);
    await player.getByLabel('Dein Name').fill('Mia');
    await player.getByRole('button', { name: 'Mitspielen' }).click();
  }

  await expect(first.getByRole('heading', { name: 'Hallo Mia!' })).toBeVisible();
  await expect(second.getByRole('alert')).toContainText('bereits vergeben');
});

test('Host startet und alle sehen die erste Frage', async ({ page, browser }) => {
  const code = await hostGame(page);
  const noah = await joinAs(browser, code, 'Noah');
  await expect(page.getByRole('button', { name: 'Spiel starten' })).toBeEnabled();

  await page.getByRole('button', { name: 'Spiel starten' }).click();

  await expect(page.getByText('Frage 1 von 6')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Wie viele Kantone hat die Schweiz?' })).toBeVisible();
  await expect(noah.getByText('Frage 1 von 6')).toBeVisible();
  await expect(noah.getByRole('button', { name: '26', exact: true })).toBeEnabled();
});

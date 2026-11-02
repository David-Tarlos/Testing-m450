import { expect, test, WebSocketRoute } from '@playwright/test';
import { answer, closeQuestion, hostGame, joinAs, openSabotage, phone, playCard, startGame } from './helpers';

test('Neu laden auf Handy und Beamer fuehrt zurueck ins laufende Spiel', async ({ page, browser }) => {
  const code = await hostGame(page);
  const tim = await joinAs(browser, code, 'Tim');
  await startGame(page);

  await tim.reload();
  await expect(tim.getByRole('heading', { name: 'Wie viele Kantone hat die Schweiz?' })).toBeVisible();
  await answer(tim, '26');

  await page.reload();
  await expect(page.getByRole('heading', { name: 'Wie viele Kantone hat die Schweiz?' })).toBeVisible();
  await closeQuestion(page);
  await expect(page.locator('app-answer-buttons .correct')).toHaveText('26');
});

test('Link zu einem neuen Spiel fuehrt im selben Tab nicht ins alte zurueck', async ({ page, browser }) => {
  const first = await hostGame(page);
  const tim = await joinAs(browser, first, 'Tim');

  // Das neue Spiel eroeffnet ein zweiter Beamer, das erste laeuft weiter.
  const secondHost = await page.context().newPage();
  const second = await hostGame(secondHost);
  await tim.goto(`/join?code=${second}`);

  await expect(tim.getByLabel('Spielcode')).toHaveValue(second);
  await tim.getByLabel('Dein Name').fill('Tim');
  await tim.getByRole('button', { name: 'Mitspielen' }).click();
  await expect(tim.getByRole('heading', { name: 'Hallo Tim!' })).toBeVisible();
  await expect(secondHost.getByRole('listitem').filter({ hasText: 'Tim' })).toBeVisible();
});

test('Nach einem Abbruch holt das Handy Verpasstes nach und spielt weiter', async ({ page, browser }) => {
  const code = await hostGame(page);
  const lena = await joinAs(browser, code, 'Lena');

  // Die Verbindung von Tim laeuft ueber Playwright, damit der Test sie gezielt kappen kann.
  const tim = await phone(browser);
  const sockets: WebSocketRoute[] = [];
  await tim.routeWebSocket(/\/ws$/, (socket) => {
    socket.connectToServer();
    sockets.push(socket);
  });
  await tim.goto(`/join?code=${code}`);
  await tim.getByLabel('Dein Name').fill('Tim');
  await tim.getByRole('button', { name: 'Mitspielen' }).click();
  await expect(tim.getByRole('heading', { name: 'Hallo Tim!' })).toBeVisible();
  await startGame(page);
  await expect(tim.getByRole('heading', { name: 'Wie viele Kantone hat die Schweiz?' })).toBeVisible();
  await answer(lena, '26');

  await sockets[0].close();
  // Waehrend Tim weg ist, geht das Spiel weiter.
  await closeQuestion(page);
  await openSabotage(page);

  await expect.poll(() => sockets.length, { timeout: 10_000 }).toBeGreaterThan(1);
  await expect(tim.getByRole('heading', { name: 'Sabotage!' })).toBeVisible({ timeout: 10_000 });
  await playCard(tim, 'Schild');
});

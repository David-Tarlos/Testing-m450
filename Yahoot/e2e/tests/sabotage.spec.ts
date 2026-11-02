import { expect, Page, test } from '@playwright/test';
import {
  answer,
  closeQuestion,
  hostGame,
  joinAs,
  nextQuestion,
  openSabotage,
  playCard,
  ranking,
  startGame,
} from './helpers';

interface Round {
  code: string;
  lena: Page;
  tim: Page;
}

/** Lena und Tim antworten auf die erste Frage richtig und stehen danach in der Sabotage-Phase. */
async function toSabotage(host: Page, browser: Parameters<typeof joinAs>[0]): Promise<Round> {
  const code = await hostGame(host);
  const lena = await joinAs(browser, code, 'Lena');
  const tim = await joinAs(browser, code, 'Tim');
  await startGame(host);
  await answer(lena, '26');
  await answer(tim, '26');
  await closeQuestion(host);
  await openSabotage(host);
  return { code, lena, tim };
}

test('Freeze sperrt das Opfer 3 Sekunden, die Aufloesung verraet den Angreifer', async ({ page, browser }) => {
  const { lena, tim } = await toSabotage(page, browser);
  await playCard(lena, 'Freeze');
  await expect(lena.getByText('Gespielt: Freeze auf Tim')).toBeVisible();

  await nextQuestion(page);

  const rhein = tim.getByRole('button', { name: 'Rhein', exact: true });
  await expect(tim.getByText('Du bist eingefroren!')).toBeVisible();
  await expect(rhein).toBeDisabled();
  // Sofort pruefen statt warten: Nur das Opfer ist gesperrt.
  const lenasRhein = lena.getByRole('button', { name: 'Rhein', exact: true });
  await expect(lenasRhein).toBeVisible();
  expect(await lenasRhein.isEnabled()).toBe(true);
  // Waehrend der Frage bleibt der Angreifer geheim.
  await expect(page.getByText('Lena hat Tim eingefroren')).toBeHidden();

  await expect(rhein).toBeEnabled({ timeout: 5000 });
  await answer(tim, 'Rhein');

  await closeQuestion(page);
  await expect(page.getByText('Lena hat Tim eingefroren')).toBeVisible();
});

test('Shuffle laesst die Buttons springen, die Sabotage endet von selbst', async ({ page, browser }) => {
  const { lena, tim } = await toSabotage(page, browser);
  await playCard(lena, 'Shuffle');

  // Niemand drueckt "Naechste Frage": Nach 10 Sekunden geht es von allein weiter.
  await expect(tim.getByRole('heading', { name: 'Welcher Fluss fliesst durch Basel?' })).toBeVisible({ timeout: 13_000 });

  const buttons = tim.locator('app-answer-buttons button');
  const first = await buttons.allTextContents();
  await expect.poll(() => buttons.allTextContents(), { timeout: 4000 }).not.toEqual(first);
  await expect(lena.locator('app-answer-buttons button').first()).toHaveText('Aare');

  await answer(tim, 'Rhein');
});

test('Klau holt die Haelfte der Punkte des Opfers', async ({ page, browser }) => {
  const { code, lena, tim } = await toSabotage(page, browser);
  const before = await ranking(page, code);
  await playCard(lena, 'Klau');
  await nextQuestion(page);

  await answer(tim, 'Rhein');
  await closeQuestion(page);

  await expect(page.getByText('Lena hat bei Tim geklaut')).toBeVisible();
  const after = await ranking(page, code);
  const gain = (name: string) =>
    after.find((row) => row.name === name)!.score - before.find((row) => row.name === name)!.score;
  expect(gain('Lena')).toBeGreaterThanOrEqual(250);
  expect(Math.abs(gain('Lena') - gain('Tim'))).toBeLessThanOrEqual(1);
});

test('Schild blockt den Angriff und das sehen alle', async ({ page, browser }) => {
  const { lena, tim } = await toSabotage(page, browser);
  await playCard(tim, 'Schild');
  await playCard(lena, 'Freeze');

  await nextQuestion(page);

  // Sofort pruefen statt warten: Nach 3 Sekunden waere auch ein eingefrorenes Handy wieder frei.
  const rhein = tim.getByRole('button', { name: 'Rhein', exact: true });
  await expect(rhein).toBeVisible();
  expect(await rhein.isEnabled()).toBe(true);
  expect(await tim.getByText('Du bist eingefroren!').isVisible()).toBe(false);
  await answer(tim, 'Rhein');

  await closeQuestion(page);
  await expect(page.getByText('Tim hat den Angriff von Lena geblockt')).toBeVisible();
});

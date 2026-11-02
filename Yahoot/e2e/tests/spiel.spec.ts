import { expect, test } from '@playwright/test';
import { answer, closeQuestion, hostGame, joinAs, nextQuestion, openSabotage, startGame } from './helpers';

const SOLUTIONS = ['26', 'Rhein', 'Dufourspitze', '1848', 'Bern', '4'];

test('Ein ganzes Spiel bis zum Sieger', async ({ page, browser }) => {
  const code = await hostGame(page);
  const lena = await joinAs(browser, code, 'Lena');
  const tim = await joinAs(browser, code, 'Tim');
  await startGame(page);

  for (let round = 0; round < SOLUTIONS.length; round++) {
    await expect(page.getByText(`Frage ${round + 1} von 6`)).toBeVisible();
    await answer(lena, SOLUTIONS[round]);
    await closeQuestion(page);

    if (round < SOLUTIONS.length - 1) {
      await expect(page.locator('app-answer-buttons .correct')).toHaveText(SOLUTIONS[round]);
      await openSabotage(page);
      await nextQuestion(page);
    }
  }

  await expect(page.getByRole('heading', { name: 'Lena gewinnt!' })).toBeVisible();
  await expect(lena.getByRole('heading', { name: 'Du bist Platz 1!' })).toBeVisible();
  await expect(tim.getByRole('heading', { name: 'Du bist Platz 2!' })).toBeVisible();
});

import { Browser, devices, expect, Page } from '@playwright/test';

export async function phone(browser: Browser): Promise<Page> {
  const context = await browser.newContext({ ...devices['Pixel 7'], baseURL: 'http://localhost:8080' });
  return context.newPage();
}

export async function hostGame(page: Page): Promise<string> {
  await page.goto('/host');
  await page.getByRole('button', { name: /Allgemeinwissen/ }).click();
  const code = page.getByTestId('game-code');
  await expect(code).toHaveText(/^[A-Z2-9]{6}$/);
  return (await code.textContent())!;
}

export async function joinAs(browser: Browser, code: string, name: string): Promise<Page> {
  const player = await phone(browser);
  await player.goto(`/join?code=${code}`);
  await player.getByLabel('Dein Name').fill(name);
  await player.getByRole('button', { name: 'Mitspielen' }).click();
  await expect(player.getByRole('heading', { name: `Hallo ${name}!` })).toBeVisible();
  return player;
}

export async function startGame(host: Page): Promise<void> {
  await host.getByRole('button', { name: 'Spiel starten' }).click();
  await expect(host.getByText('Frage 1 von 6')).toBeVisible();
}

/** Tippt eine Antwort an und wartet, bis der Server sie angenommen hat. */
export async function answer(player: Page, option: string): Promise<void> {
  await player.getByRole('button', { name: option, exact: true }).click();
  await expect(player.getByText('Antwort gespeichert')).toBeVisible();
}

export async function closeQuestion(host: Page): Promise<void> {
  await host.getByRole('button', { name: 'Frage schliessen' }).click();
}

export async function openSabotage(host: Page): Promise<void> {
  await host.getByRole('button', { name: 'Sabotage starten' }).click();
  await expect(host.getByRole('heading', { name: 'Sabotage!' })).toBeVisible();
}

export async function nextQuestion(host: Page): Promise<void> {
  await host.getByRole('button', { name: 'Nächste Frage' }).click();
}

/** Spielt eine Karte und wartet auf die Bestätigung des Servers. */
export async function playCard(player: Page, card: string): Promise<void> {
  await player.getByRole('button', { name: new RegExp(`^${card} \\(`) }).click();
  await expect(player.getByText(/^Gespielt:/)).toBeVisible();
}

export interface ScoreRow {
  name: string;
  score: number;
  energy: number;
}

export async function ranking(page: Page, code: string): Promise<ScoreRow[]> {
  const response = await page.request.get(`/api/games/${code}`);
  return (await response.json()).ranking;
}

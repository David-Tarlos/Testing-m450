import { devices, expect, test } from '@playwright/test';

test('Startseite laedt im Desktop-Browser', async ({ page }) => {
  await page.goto('/');

  await expect(page).toHaveTitle('Yahoot');
  await expect(page.getByRole('heading', { name: 'Yahoot' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Spiel hosten' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Mitspielen' })).toBeVisible();
});

test('Startseite laedt auf dem Handy', async ({ browser }) => {
  const context = await browser.newContext({ ...devices['Pixel 7'], baseURL: 'http://localhost:8080' });
  const page = await context.newPage();

  await page.goto('/');

  await expect(page.getByRole('heading', { name: 'Yahoot' })).toBeVisible();
  await page.getByRole('link', { name: 'Mitspielen' }).click();
  await expect(page.getByLabel('Spielcode')).toBeVisible();
  await context.close();
});

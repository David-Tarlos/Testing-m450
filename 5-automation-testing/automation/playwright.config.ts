import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright-Konfiguration fuer die Student-App.
 *
 * Zwei Projekte:
 *   api  - Uebung 1: testet die REST-Schnittstelle direkt (kein Browser noetig)
 *   e2e  - Uebung 2: testet das Angular-GUI in einem echten Chromium
 *
 * Beide Server werden bei Bedarf automatisch gestartet (webServer unten).
 * Laeuft schon etwas auf dem Port, wird das bestehende Server-Fenster genutzt.
 */

export const BACKEND_URL = process.env.BACKEND_URL ?? 'http://localhost:8081';
export const FRONTEND_URL = process.env.FRONTEND_URL ?? 'http://localhost:4200';

// Das Backend baut mit JDK 17 und 21 (die pom.xml pinnt Lombok 1.18.34; die von
// Spring Boot 3.1.2 verwaltete 1.18.28 brach auf JDK 21 ab). Standardmaessig
// laeuft Maven mit dem JDK aus dem PATH - per JDK_HOME laesst sich eines erzwingen.
const JDK_OVERRIDE = process.env.JDK_HOME ?? process.env.JDK17_HOME;

const APP_ROOT = '../spring-boot-angular-basic-lw2';

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,
  reporter: [['html', { open: 'never' }], ['list']],

  use: {
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    {
      name: 'api',
      testDir: './tests/api',
      use: { baseURL: BACKEND_URL },
    },
    {
      name: 'e2e',
      testDir: './tests/e2e',
      use: { ...devices['Desktop Chrome'], baseURL: FRONTEND_URL },
    },
  ],

  webServer: [
    {
      command: 'mvn -B spring-boot:run',
      cwd: APP_ROOT,
      url: `${BACKEND_URL}/students`,
      reuseExistingServer: true,
      timeout: 180_000,
      stdout: 'pipe',
      ...(JDK_OVERRIDE ? { env: { JAVA_HOME: JDK_OVERRIDE } } : {}),
    },
    {
      command: 'npm start',
      cwd: `${APP_ROOT}/src/main/js/my-app`,
      url: FRONTEND_URL,
      reuseExistingServer: true,
      timeout: 240_000,
      stdout: 'pipe',
    },
  ],
});

import { defineConfig, devices } from '@playwright/test';

// Testet das fertige JAR, also genau das, was auch ausgeliefert wird.
// Vorher bauen: im Frontend "npm run build", im Backend "mvn -Pwith-frontend -DskipTests package".
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'reports/html', open: 'never' }],
    ['junit', { outputFile: 'reports/junit.xml' }],
  ],
  use: {
    baseURL: 'http://localhost:8080',
    trace: 'retain-on-failure',
    ...devices['Desktop Chrome'],
  },
  webServer: {
    command: 'java -jar ../backend/target/yahoot-0.0.1-SNAPSHOT.jar',
    url: 'http://localhost:8080/actuator/health',
    reuseExistingServer: !process.env.CI,
    timeout: 60_000,
  },
});

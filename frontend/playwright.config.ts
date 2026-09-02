import { defineConfig, devices } from '@playwright/test'

const backendCommand = process.platform === 'win32'
  ? '..\\backend\\gradlew.bat -p ..\\backend bootRun --no-daemon'
  : '../backend/gradlew -p ../backend bootRun --no-daemon'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: 'list',
  use: {
    baseURL: 'http://127.0.0.1:5173',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
  webServer: [
    {
      command: backendCommand,
      cwd: process.cwd(),
      url: 'http://127.0.0.1:8080/api/v1/actuator/health',
      timeout: 180_000,
      reuseExistingServer: !process.env.CI,
      env: { ...process.env, SPRING_PROFILES_ACTIVE: 'dev' },
    },
    {
      command: 'npm run dev -- --host 127.0.0.1',
      cwd: process.cwd(),
      url: 'http://127.0.0.1:5173',
      timeout: 60_000,
      reuseExistingServer: !process.env.CI,
      env: { ...process.env, VITE_ENABLE_MOCKS: 'false' },
    },
  ],
})

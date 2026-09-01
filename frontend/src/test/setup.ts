import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, vi } from 'vitest'
import { resetMockState } from '../mocks/handlers'
import { server } from './server'

// openapi-fetch captures the current fetch implementation when its client is created.
// Start MSW before test modules import and create that client.
server.listen({ onUnhandledRequest: 'error' })

afterEach(() => {
  cleanup()
  server.resetHandlers()
  resetMockState()
})

afterAll(() => server.close())

Object.assign(navigator, {
  clipboard: {
    writeText: vi.fn().mockResolvedValue(undefined),
  },
})

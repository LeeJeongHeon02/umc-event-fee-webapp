import createClient from 'openapi-fetch'
import type { paths } from './schema'
import type {
  AdminDashboardResponse,
  AdminDuesPaymentPage,
  AdminEventParticipantPage,
  AdminEventCreateRequest,
  AdminEventUpdateRequest,
  AdminEventResponse,
  AdminPaymentReviewRequest,
  AdminPaymentReviewResponse,
  CancelParticipationRequest,
  CancelParticipationResponse,
  EventDetail,
  EventPage,
  JoinEventResponse,
  MeResponse,
  MemberPart,
  PaymentDetail,
  PaymentPage,
  PaymentReportRequest,
  PaymentReportResponse,
  Problem,
} from './types'

const apiBaseUrl = new URL('/api/v1', window.location.origin).toString().replace(/\/$/, '')

const client = createClient<paths>({
  baseUrl: apiBaseUrl,
  credentials: 'include',
})

client.use({
  async onRequest({ request }) {
    if (import.meta.env.PROD && ['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method)) {
      let csrfCookie = document.cookie.split('; ').find((cookie) => cookie.startsWith('XSRF-TOKEN='))
      if (!csrfCookie) {
        await fetch(`${apiBaseUrl}/auth/csrf`, { credentials: 'include' })
        csrfCookie = document.cookie.split('; ').find((cookie) => cookie.startsWith('XSRF-TOKEN='))
      }
      if (csrfCookie) request.headers.set('X-XSRF-TOKEN', decodeURIComponent(csrfCookie.split('=')[1]))
    }
    return request
  },
})

export class ApiError extends Error {
  readonly status: number
  readonly code: string

  constructor(problem: Partial<Problem>, status: number) {
    super(problem.detail ?? '요청을 처리하지 못했습니다.')
    this.name = 'ApiError'
    this.status = status
    this.code = problem.code ?? 'UNKNOWN_ERROR'
  }
}

function throwApiError(error: unknown, response: Response): never {
  throw new ApiError((error ?? {}) as Partial<Problem>, response.status)
}

export async function getMe(): Promise<MeResponse> {
  const { data, error, response } = await client.GET('/me')
  if (!data) throwApiError(error, response)
  return data
}

export async function completeOnboarding(input: {
  name: string
  part: MemberPart
}): Promise<MeResponse> {
  const { data, error, response } = await client.PATCH('/me/onboarding', {
    body: input,
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function getEvents(): Promise<EventPage> {
  const { data, error, response } = await client.GET('/events', {
    params: { query: { page: 0, size: 20 } },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function getEvent(eventId: number): Promise<EventDetail> {
  const { data, error, response } = await client.GET('/events/{eventId}', {
    params: { path: { eventId } },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function joinEvent(eventId: number): Promise<JoinEventResponse> {
  const { data, error, response } = await client.POST('/events/{eventId}/participation', {
    params: { path: { eventId } },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function cancelEventParticipation(
  eventId: number,
  input: CancelParticipationRequest,
): Promise<CancelParticipationResponse> {
  const { data, error, response } = await client.POST('/events/{eventId}/participation/cancel', {
    params: { path: { eventId } },
    body: input,
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function getMyPayments(): Promise<PaymentPage> {
  const { data, error, response } = await client.GET('/me/payment-obligations', {
    params: { query: { page: 0, size: 20 } },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function getPayment(paymentId: number): Promise<PaymentDetail> {
  const { data, error, response } = await client.GET('/payment-obligations/{paymentId}', {
    params: { path: { paymentId } },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function reportPayment(
  paymentId: number,
  input: PaymentReportRequest,
): Promise<PaymentReportResponse> {
  const { data, error, response } = await client.POST(
    '/payment-obligations/{paymentId}/reports',
    {
      params: { path: { paymentId } },
      body: input,
    },
  )
  if (!data) throwApiError(error, response)
  return data
}

export async function getAdminDashboard(): Promise<AdminDashboardResponse> {
  const { data, error, response } = await client.GET('/admin/dashboard')
  if (!data) throwApiError(error, response)
  return data
}

export async function getAdminEvents(): Promise<AdminEventResponse[]> {
  const { data, error, response } = await client.GET('/admin/events')
  if (!data) throwApiError(error, response)
  return data
}

export async function createAdminEvent(input: AdminEventCreateRequest): Promise<AdminEventResponse> {
  const { data, error, response } = await client.POST('/admin/events', { body: input })
  if (!data) throwApiError(error, response)
  return data
}

export async function updateAdminEvent(eventId: number, input: AdminEventUpdateRequest): Promise<AdminEventResponse> {
  const { data, error, response } = await client.PATCH('/admin/events/{eventId}', {
    params: { path: { eventId } },
    body: input,
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function publishAdminEvent(eventId: number, version: number): Promise<AdminEventResponse> {
  const { data, error, response } = await client.POST('/admin/events/{eventId}/publish', {
    params: { path: { eventId } },
    body: { version },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function deleteAdminEvent(eventId: number, version: number): Promise<void> {
  const { error, response } = await client.DELETE('/admin/events/{eventId}', {
    params: { path: { eventId }, query: { version } },
  })
  if (!response.ok) throwApiError(error, response)
}

export async function getAdminEventParticipants(eventId: number): Promise<AdminEventParticipantPage> {
  const { data, error, response } = await client.GET('/admin/events/{eventId}/participants', {
    params: { path: { eventId }, query: { page: 0, size: 100 } },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function getAdminDuesPayments(duesRoundId: number): Promise<AdminDuesPaymentPage> {
  const { data, error, response } = await client.GET('/admin/dues-rounds/{duesRoundId}/payments', {
    params: { path: { duesRoundId }, query: { page: 0, size: 100 } },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function reviewAdminPayment(
  paymentId: number,
  decision: 'confirm' | 'reject',
  input: AdminPaymentReviewRequest,
): Promise<AdminPaymentReviewResponse> {
  const request = decision === 'confirm'
    ? client.POST('/admin/payment-obligations/{paymentId}/confirm', {
        params: { path: { paymentId } },
        body: input,
      })
    : client.POST('/admin/payment-obligations/{paymentId}/reject', {
        params: { path: { paymentId } },
        body: input,
      })
  const { data, error, response } = await request
  if (!data) throwApiError(error, response)
  return data
}

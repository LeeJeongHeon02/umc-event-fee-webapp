import createClient from 'openapi-fetch'
import type { paths } from './schema'
import type {
  AdminDashboardResponse,
  AdminDuesPaymentPage,
  AdminDuesRoundRequest,
  AdminDuesRoundResponse,
  AdminDuesPublishResponse,
  AdminEventParticipantPage,
  AdminEventCreateRequest,
  AdminEventUpdateRequest,
  AdminEventResponse,
  AdminPaymentReviewRequest,
  AdminPaymentReviewResponse,
  AdminMemberResponse,
  MemberRole,
  AdminPaymentRow,
  PaymentSettingRequest,
  PaymentSettingResponse,
  NotificationResponse,
  CancelParticipationRequest,
  CancelParticipationResponse,
  EventDetail,
  EventPage,
  JoinEventResponse,
  LocalLoginRequest,
  LocalLoginResponse,
  LocalRegisterRequest,
  LocalRegisterResponse,
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

export async function logoutMember(): Promise<void> {
  const { error, response } = await client.POST('/auth/logout')
  // An expired session is already logged out. Other failures must remain visible and retryable.
  if (!response.ok && response.status !== 401) throwApiError(error, response)
}

export async function registerLocalMember(input: LocalRegisterRequest): Promise<LocalRegisterResponse> {
  const { data, error, response } = await client.POST('/auth/local/register', { body: input })
  if (!data) throwApiError(error, response)
  return data
}

export async function loginLocalMember(input: LocalLoginRequest): Promise<LocalLoginResponse> {
  const { data, error, response } = await client.POST('/auth/local/login', { body: input })
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

export async function closeAdminEvent(eventId: number, version: number): Promise<AdminEventResponse> {
  const { data, error, response } = await client.POST('/admin/events/{eventId}/close', {
    params: { path: { eventId } }, body: { version },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function cancelAdminEvent(eventId: number, version: number, reason: string): Promise<void> {
  const { data, error, response } = await client.POST('/admin/events/{eventId}/cancel', {
    params: { path: { eventId } }, body: { version, reason },
  })
  if (!data) throwApiError(error, response)
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

export async function getAdminDuesRounds(): Promise<AdminDuesRoundResponse[]> {
  const { data, error, response } = await client.GET('/admin/dues-rounds')
  if (!data) throwApiError(error, response)
  return data
}

export async function createAdminDuesRound(input: AdminDuesRoundRequest): Promise<AdminDuesRoundResponse> {
  const { data, error, response } = await client.POST('/admin/dues-rounds', { body: input })
  if (!data) throwApiError(error, response)
  return data
}

export async function publishAdminDuesRound(id: number, version: number): Promise<AdminDuesPublishResponse> {
  const { data, error, response } = await client.POST('/admin/dues-rounds/{duesRoundId}/publish', {
    params: { path: { duesRoundId: id } }, body: { version },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function deleteAdminDuesRound(id: number, version: number): Promise<void> {
  const { error, response } = await client.DELETE('/admin/dues-rounds/{duesRoundId}', {
    params: { path: { duesRoundId: id }, query: { version } },
  })
  if (!response.ok) throwApiError(error, response)
}

export async function getAdminMembers(): Promise<AdminMemberResponse[]> {
  const { data, error, response } = await client.GET('/admin/members')
  if (!data) throwApiError(error, response)
  return data
}

export async function approveAdminMember(id: number, version: number): Promise<AdminMemberResponse> {
  const { data, error, response } = await client.POST('/admin/members/{memberId}/approve', {
    params: { path: { memberId: id } }, body: { version },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function suspendAdminMember(id: number, version: number): Promise<AdminMemberResponse> {
  const { data, error, response } = await client.POST('/admin/members/{memberId}/suspend', {
    params: { path: { memberId: id } }, body: { version },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function changeAdminMemberRole(id: number, role: MemberRole, version: number): Promise<AdminMemberResponse> {
  const { data, error, response } = await client.PATCH('/admin/members/{memberId}/role', {
    params: { path: { memberId: id } }, body: { role, version },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function getAdminRefunds(): Promise<AdminPaymentRow[]> {
  const { data, error, response } = await client.GET('/admin/refunds')
  if (!data) throwApiError(error, response)
  return data
}

export async function getAdminPaymentReports(): Promise<AdminPaymentRow[]> {
  const { data, error, response } = await client.GET('/admin/payment-reports')
  if (!data) throwApiError(error, response)
  return data
}

export async function completeAdminRefund(paymentId: number, version: number, note: string): Promise<AdminPaymentReviewResponse> {
  const { data, error, response } = await client.POST('/admin/payment-obligations/{paymentId}/refund', {
    params: { path: { paymentId } }, body: { version, note },
  })
  if (!data) throwApiError(error, response)
  return data
}

export async function getAdminPaymentSettings(): Promise<PaymentSettingResponse[]> {
  const { data, error, response } = await client.GET('/admin/payment-settings')
  if (!data) throwApiError(error, response)
  return data
}

export async function createAdminPaymentSetting(input: PaymentSettingRequest): Promise<PaymentSettingResponse> {
  const { data, error, response } = await client.POST('/admin/payment-settings', { body: input })
  if (!data) throwApiError(error, response)
  return data
}

export async function getNotifications(): Promise<NotificationResponse> {
  const { data, error, response } = await client.GET('/notifications')
  if (!data) throwApiError(error, response)
  return data
}

export async function readNotification(id: number): Promise<void> {
  const { data, error, response } = await client.POST('/notifications/{notificationId}/read', {
    params: { path: { notificationId: id } },
  })
  if (!data) throwApiError(error, response)
}

export async function readAllNotifications(): Promise<void> {
  const { error, response } = await client.POST('/notifications/read-all')
  if (!response.ok) throwApiError(error, response)
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

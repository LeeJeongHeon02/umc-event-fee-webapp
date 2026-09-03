import { http, HttpResponse } from 'msw'
import type {
  AdminEventCreateRequest,
  AdminEventResponse,
  AdminEventUpdateRequest,
  AdminDuesRoundResponse,
  AdminMemberResponse,
  EventDetail,
  MeResponse,
  PaymentDetail,
  PaymentReportRequest,
} from '../services/types'
import {
  activeMember,
  adminDuesPayments,
  adminDuesRound,
  adminEventParticipants,
  adminEventSummary,
  createAdminDashboard,
  createEventDetails,
  createPaymentDetails,
  eventFixtures,
  paymentFixture,
  pendingMember,
} from './fixtures'

let currentMember: MeResponse = { ...activeMember }
let eventDetails: EventDetail[] = createEventDetails()
let paymentDetails: PaymentDetail[] = createPaymentDetails()
let currentAdminEventParticipants = structuredClone(adminEventParticipants)
let currentAdminDuesPayments = structuredClone(adminDuesPayments)

const createAdminEvents = (): AdminEventResponse[] => [
  {
    id: 42,
    title: '2026 가을 해커톤',
    summary: '밤샘 없이 완성하는 교내 해커톤',
    description: '팀을 구성해 서비스를 만드는 행사입니다.',
    location: '공학관 101호',
    startsAt: '2026-09-15T10:00:00Z',
    endsAt: '2026-09-15T13:00:00Z',
    registrationDeadline: '2026-09-14T10:00:00Z',
    capacity: 50,
    joinedCount: 23,
    feeAmount: 15000,
    status: 'PUBLISHED',
    allowLateCancellation: false,
    createdAt: '2026-08-20T00:00:00Z',
    updatedAt: '2026-08-21T00:00:00Z',
    version: 1,
  },
]
let currentAdminEvents = createAdminEvents()
let currentAdminDuesRounds: AdminDuesRoundResponse[] = [{
  id: 7, title: adminDuesRound.title, amount: adminDuesRound.amount, dueAt: adminDuesRound.dueAt,
  status: 'PUBLISHED', bankName: '카카오뱅크', accountNumber: '3333-12-3456789', accountHolder: '김총무',
  targetCount: 48, createdAt: '2026-08-20T00:00:00Z', updatedAt: '2026-08-21T00:00:00Z', version: 1,
}]
let currentAdminMembers: AdminMemberResponse[] = [{
  ...activeMember, kakaoProfileName: activeMember.kakaoProfileName ?? '', displayNickname: activeMember.displayNickname ?? '',
  createdAt: '2026-08-20T00:00:00Z', updatedAt: '2026-08-21T00:00:00Z', version: 0,
}]

export function resetMockState() {
  currentMember = { ...activeMember }
  eventDetails = createEventDetails()
  paymentDetails = createPaymentDetails()
  currentAdminEventParticipants = structuredClone(adminEventParticipants)
  currentAdminDuesPayments = structuredClone(adminDuesPayments)
  currentAdminEvents = createAdminEvents()
  currentAdminDuesRounds = [{ id: 7, title: adminDuesRound.title, amount: adminDuesRound.amount, dueAt: adminDuesRound.dueAt, status: 'PUBLISHED', bankName: '카카오뱅크', accountNumber: '3333-12-3456789', accountHolder: '김총무', targetCount: 48, createdAt: '2026-08-20T00:00:00Z', updatedAt: '2026-08-21T00:00:00Z', version: 1 }]
}

function problem(status: number, code: string, detail: string) {
  const title = ({ 400: 'Bad Request', 401: 'Unauthorized', 403: 'Forbidden', 404: 'Not Found', 409: 'Conflict' } as Record<number, string>)[status] ?? 'Internal Server Error'
  return HttpResponse.json(
    {
      type: 'about:blank',
      title,
      status,
      code,
      detail,
      instance: '/api/v1/mock',
      timestamp: new Date().toISOString(),
      fieldErrors: [],
    },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  )
}

export const handlers = [
  http.get(/\/api\/v1\/me$/, () => HttpResponse.json(currentMember)),
  http.get(/\/api\/v1\/notifications$/, () => HttpResponse.json({ items: [], unreadCount: 0 })),
  http.post(/\/api\/v1\/notifications\/read-all$/, () => new HttpResponse(null, { status: 204 })),

  http.patch(/\/api\/v1\/me\/onboarding$/, async ({ request }) => {
    const body = (await request.json()) as { name: string; part: MeResponse['part'] }
    currentMember = {
      ...pendingMember,
      name: body.name,
      part: body.part,
      displayNickname: `${partLabel(body.part)} ${body.name}`,
    }
    return HttpResponse.json(currentMember)
  }),

  http.get(/\/api\/v1\/admin\/events$/, () => HttpResponse.json(currentAdminEvents)),

  http.post(/\/api\/v1\/admin\/events$/, async ({ request }) => {
    const body = (await request.json()) as AdminEventCreateRequest
    const now = new Date().toISOString()
    const created: AdminEventResponse = {
      ...body,
      id: Math.max(42, ...currentAdminEvents.map((event) => event.id)) + 1,
      joinedCount: 0,
      status: 'DRAFT',
      createdAt: now,
      updatedAt: now,
      version: 0,
    }
    currentAdminEvents = [created, ...currentAdminEvents]
    return HttpResponse.json(created, { status: 201 })
  }),

  http.patch(/\/api\/v1\/admin\/events\/\d+$/, async ({ request }) => {
    const eventId = Number(new URL(request.url).pathname.match(/events\/(\d+)$/)?.[1])
    const body = (await request.json()) as AdminEventUpdateRequest
    const index = currentAdminEvents.findIndex((event) => event.id === eventId)
    if (index < 0) return problem(404, 'RESOURCE_NOT_FOUND', '행사를 찾을 수 없습니다.')
    if (currentAdminEvents[index].status !== 'DRAFT' || currentAdminEvents[index].version !== body.version) {
      return problem(409, 'EVENT_STATE_CONFLICT', '행사 상태가 최신이 아닙니다.')
    }
    currentAdminEvents[index] = {
      ...currentAdminEvents[index],
      ...body,
      updatedAt: new Date().toISOString(),
      version: body.version + 1,
    }
    return HttpResponse.json(currentAdminEvents[index])
  }),

  http.post(/\/api\/v1\/admin\/events\/\d+\/publish$/, async ({ request }) => {
    const eventId = Number(new URL(request.url).pathname.match(/events\/(\d+)\/publish$/)?.[1])
    const body = (await request.json()) as { version: number }
    const index = currentAdminEvents.findIndex((event) => event.id === eventId)
    if (index < 0) return problem(404, 'RESOURCE_NOT_FOUND', '행사를 찾을 수 없습니다.')
    if (currentAdminEvents[index].status !== 'DRAFT' || currentAdminEvents[index].version !== body.version) {
      return problem(409, 'EVENT_STATE_CONFLICT', '행사 상태가 최신이 아닙니다.')
    }
    currentAdminEvents[index] = { ...currentAdminEvents[index], status: 'PUBLISHED', version: body.version + 1 }
    return HttpResponse.json(currentAdminEvents[index])
  }),

  http.delete(/\/api\/v1\/admin\/events\/\d+$/, ({ request }) => {
    const eventId = Number(new URL(request.url).pathname.match(/events\/(\d+)$/)?.[1])
    const index = currentAdminEvents.findIndex((event) => event.id === eventId)
    if (index < 0) return problem(404, 'RESOURCE_NOT_FOUND', '행사를 찾을 수 없습니다.')
    if (currentAdminEvents[index].status !== 'DRAFT') {
      return problem(409, 'EVENT_NOT_DELETABLE', '초안 행사만 삭제할 수 있습니다.')
    }
    currentAdminEvents.splice(index, 1)
    return new HttpResponse(null, { status: 204 })
  }),

  http.post(/\/api\/v1\/admin\/events\/\d+\/(?:close|cancel)$/, async ({ request }) => {
    const match = new URL(request.url).pathname.match(/events\/(\d+)\/(close|cancel)$/)
    const index = currentAdminEvents.findIndex((event) => event.id === Number(match?.[1]))
    if (index < 0) return problem(404, 'RESOURCE_NOT_FOUND', '행사를 찾을 수 없습니다.')
    const body = (await request.json()) as { version: number }
    currentAdminEvents[index] = { ...currentAdminEvents[index], status: match?.[2] === 'close' ? 'CLOSED' : 'CANCELED', version: body.version + 1 }
    return match?.[2] === 'close' ? HttpResponse.json(currentAdminEvents[index]) : HttpResponse.json({ eventStatus: 'CANCELED', voidedPaymentCount: 2, refundPendingCount: 1, version: body.version + 1 })
  }),

  http.get(/\/api\/v1\/admin\/dues-rounds$/, () => HttpResponse.json(currentAdminDuesRounds)),
  http.post(/\/api\/v1\/admin\/dues-rounds$/, async ({ request }) => {
    const body = await request.json() as Omit<AdminDuesRoundResponse, 'id' | 'status' | 'targetCount' | 'createdAt' | 'updatedAt'>
    const now = new Date().toISOString()
    const round: AdminDuesRoundResponse = { ...body, id: 8, status: 'DRAFT', targetCount: 0, createdAt: now, updatedAt: now }
    currentAdminDuesRounds = [round, ...currentAdminDuesRounds]
    return HttpResponse.json(round, { status: 201 })
  }),
  http.post(/\/api\/v1\/admin\/dues-rounds\/\d+\/publish$/, async ({ request }) => {
    const id = Number(new URL(request.url).pathname.match(/dues-rounds\/(\d+)\/publish$/)?.[1])
    const body = await request.json() as { version: number }
    const index = currentAdminDuesRounds.findIndex((round) => round.id === id)
    currentAdminDuesRounds[index] = { ...currentAdminDuesRounds[index], status: 'PUBLISHED', targetCount: 5, version: body.version + 1 }
    return HttpResponse.json({ duesRound: currentAdminDuesRounds[index], createdPaymentCount: 5 })
  }),
  http.delete(/\/api\/v1\/admin\/dues-rounds\/\d+$/, ({ request }) => {
    const id = Number(new URL(request.url).pathname.match(/dues-rounds\/(\d+)$/)?.[1])
    currentAdminDuesRounds = currentAdminDuesRounds.filter((round) => round.id !== id)
    return new HttpResponse(null, { status: 204 })
  }),
  http.get(/\/api\/v1\/admin\/members$/, () => HttpResponse.json(currentAdminMembers)),
  http.get(/\/api\/v1\/admin\/refunds$/, () => HttpResponse.json([])),
  http.get(/\/api\/v1\/admin\/payment-settings$/, () => HttpResponse.json([{
    id: 1, bankName: '카카오뱅크', accountNumber: '3333-12-3456789', accountHolder: '김총무',
    kakaoPayReceiveUrl: 'https://qr.kakaopay.com/example', active: true, createdBy: 15,
    createdAt: '2026-08-20T00:00:00Z',
  }])),

  http.get(/\/api\/v1\/events$/, () =>
    HttpResponse.json({
      items: eventDetails.map(({ description: _description, ...event }) => event),
      page: 0,
      size: 20,
      totalElements: eventFixtures.length,
      totalPages: 1,
    }),
  ),

  http.get(/\/api\/v1\/events\/\d+$/, ({ request }) => {
    const eventId = Number(new URL(request.url).pathname.match(/events\/(\d+)$/)?.[1])
    const event = eventDetails.find((item) => item.id === eventId)
    return event ? HttpResponse.json(event) : problem(404, 'RESOURCE_NOT_FOUND', '행사를 찾을 수 없습니다.')
  }),

  http.post(/\/api\/v1\/events\/\d+\/participation$/, ({ request }) => {
    const eventId = Number(new URL(request.url).pathname.match(/events\/(\d+)\/participation$/)?.[1])
    const eventIndex = eventDetails.findIndex((item) => item.id === eventId)
    if (eventIndex < 0) return problem(404, 'RESOURCE_NOT_FOUND', '행사를 찾을 수 없습니다.')
    if (eventDetails[eventIndex].myParticipation?.status === 'JOINED') {
      return problem(409, 'ALREADY_PARTICIPATING', '이미 참가 중인 행사입니다.')
    }

    const event = eventDetails[eventIndex]
    const payment = {
      ...paymentFixture,
      id: eventId === 42 ? 311 : 400 + eventId,
      amount: event.feeAmount,
      status: event.feeAmount === 0 ? ('NOT_REQUIRED' as const) : ('UNPAID' as const),
      source: { type: 'EVENT' as const, id: event.id, title: event.title },
    }
    const participation = {
      id: 100 + eventId,
      status: 'JOINED' as const,
      joinedAt: new Date().toISOString(),
      version: 0,
    }
    eventDetails[eventIndex] = {
      ...event,
      joinedCount: event.joinedCount + 1,
      myParticipationStatus: 'JOINED',
      myPaymentStatus: payment.status,
      myParticipation: participation,
      myPayment: payment,
      canJoin: false,
      canCancel: true,
    }
    return HttpResponse.json({ participation, payment }, { status: 201 })
  }),

  http.post(/\/api\/v1\/events\/\d+\/participation\/cancel$/, async ({ request }) => {
    const eventId = Number(new URL(request.url).pathname.match(/events\/(\d+)\/participation\/cancel$/)?.[1])
    const eventIndex = eventDetails.findIndex((item) => item.id === eventId)
    if (eventIndex < 0) return problem(404, 'RESOURCE_NOT_FOUND', '행사를 찾을 수 없습니다.')
    const event = eventDetails[eventIndex]
    if (event.myParticipation?.status !== 'JOINED') {
      return problem(409, 'PARTICIPATION_STATE_CONFLICT', '참가 상태가 최신이 아닙니다.')
    }
    const body = (await request.json()) as { version: number; reason?: string }
    if (event.myParticipation.version !== body.version) {
      return problem(409, 'PARTICIPATION_STATE_CONFLICT', '참가 상태가 최신이 아닙니다.')
    }

    const paymentStatus = event.myPaymentStatus === 'CONFIRMED' ? 'REFUND_PENDING' : 'VOID'
    const canceledAt = new Date().toISOString()
    eventDetails[eventIndex] = {
      ...event,
      joinedCount: Math.max(0, event.joinedCount - 1),
      myParticipationStatus: 'CANCELED',
      myPaymentStatus: paymentStatus,
      myParticipation: {
        ...event.myParticipation,
        status: 'CANCELED',
        canceledAt,
        version: event.myParticipation.version + 1,
      },
      myPayment: event.myPayment ? { ...event.myPayment, status: paymentStatus } : event.myPayment,
      canJoin: false,
      canCancel: false,
    }
    const paymentIndex = paymentDetails.findIndex((payment) => payment.id === event.myPayment?.id)
    if (paymentIndex >= 0) {
      paymentDetails[paymentIndex] = {
        ...paymentDetails[paymentIndex],
        status: paymentStatus,
        version: paymentDetails[paymentIndex].version + 1,
        updatedAt: canceledAt,
      }
    }
    return HttpResponse.json({
      participationStatus: 'CANCELED',
      paymentStatus,
      refundRequired: paymentStatus === 'REFUND_PENDING',
    })
  }),

  http.get(/\/api\/v1\/me\/payment-obligations$/, () =>
    HttpResponse.json({
      items: paymentDetails,
      page: 0,
      size: 20,
      totalElements: paymentDetails.length,
      totalPages: 1,
    }),
  ),

  http.get(/\/api\/v1\/payment-obligations\/\d+$/, ({ request }) => {
    const paymentId = Number(new URL(request.url).pathname.match(/payment-obligations\/(\d+)$/)?.[1])
    const payment = paymentDetails.find((item) => item.id === paymentId)
    return payment
      ? HttpResponse.json(payment)
      : problem(404, 'RESOURCE_NOT_FOUND', '납부 항목을 찾을 수 없습니다.')
  }),

  http.post(/\/api\/v1\/payment-obligations\/\d+\/reports$/, async ({ request }) => {
    const paymentId = Number(new URL(request.url).pathname.match(/payment-obligations\/(\d+)\/reports$/)?.[1])
    const paymentIndex = paymentDetails.findIndex((item) => item.id === paymentId)
    if (paymentIndex < 0) return problem(404, 'RESOURCE_NOT_FOUND', '납부 항목을 찾을 수 없습니다.')

    const payment = paymentDetails[paymentIndex]
    if (!['UNPAID', 'REJECTED'].includes(payment.status)) {
      return problem(409, 'PAYMENT_STATE_CONFLICT', '현재 상태에서는 송금을 신고할 수 없습니다.')
    }

    const body = (await request.json()) as PaymentReportRequest
    const report = {
      id: 1000 + paymentId,
      method: body.method,
      senderName: body.senderName,
      transferredAt: body.transferredAt,
      note: body.note,
      reportedAt: new Date().toISOString(),
    }
    paymentDetails[paymentIndex] = {
      ...payment,
      status: 'REPORTED',
      latestReport: report,
      statusHistory: [
        ...payment.statusHistory,
        {
          fromStatus: payment.status,
          toStatus: 'REPORTED',
          changedAt: report.reportedAt,
        },
      ],
      version: payment.version + 1,
      updatedAt: report.reportedAt,
    }
    const eventIndex = eventDetails.findIndex((item) => item.myPayment?.id === paymentId)
    if (eventIndex >= 0) {
      eventDetails[eventIndex] = {
        ...eventDetails[eventIndex],
        myPaymentStatus: 'REPORTED',
        myPayment: { ...eventDetails[eventIndex].myPayment!, status: 'REPORTED' },
      }
    }
    return HttpResponse.json(
      {
        report,
        paymentStatus: 'REPORTED',
        version: payment.version + 1,
      },
      { status: 201 },
    )
  }),

  http.get(/\/api\/v1\/admin\/dashboard$/, () => {
    const dashboard = createAdminDashboard()
    const currentReported = [
      ...currentAdminDuesPayments.map((payment) => payment.status),
      ...currentAdminEventParticipants.map((participant) => participant.paymentStatus),
    ].filter((status) => status === 'REPORTED').length
    return HttpResponse.json({
      ...dashboard,
      reportedCount: dashboard.reportedCount - 4 + currentReported,
      recentReports: currentAdminDuesPayments.filter((payment) => payment.status === 'REPORTED'),
    })
  }),

  http.get(/\/api\/v1\/admin\/events\/\d+\/participants$/, ({ request }) => {
    const eventId = Number(new URL(request.url).pathname.match(/events\/(\d+)\/participants$/)?.[1])
    if (eventId !== adminEventSummary.id) {
      return problem(404, 'RESOURCE_NOT_FOUND', '행사를 찾을 수 없습니다.')
    }
    return HttpResponse.json({
      event: {
        ...adminEventSummary,
        reportedCount: adminEventSummary.reportedCount - 2 + currentAdminEventParticipants.filter((participant) => participant.paymentStatus === 'REPORTED').length,
        confirmedCount: adminEventSummary.confirmedCount - 1 + currentAdminEventParticipants.filter((participant) => participant.paymentStatus === 'CONFIRMED').length,
      },
      items: currentAdminEventParticipants,
      page: 0,
      size: 100,
      totalElements: currentAdminEventParticipants.length,
      totalPages: 1,
    })
  }),

  http.get(/\/api\/v1\/admin\/dues-rounds\/\d+\/payments$/, ({ request }) => {
    const duesRoundId = Number(new URL(request.url).pathname.match(/dues-rounds\/(\d+)\/payments$/)?.[1])
    if (duesRoundId !== adminDuesRound.id) {
      return problem(404, 'RESOURCE_NOT_FOUND', '회비 차수를 찾을 수 없습니다.')
    }
    const confirmedRows = currentAdminDuesPayments.filter((payment) => payment.status === 'CONFIRMED')
    return HttpResponse.json({
      duesRound: {
        ...adminDuesRound,
        reportedCount: adminDuesRound.reportedCount - 2 + currentAdminDuesPayments.filter((payment) => payment.status === 'REPORTED').length,
        confirmedCount: adminDuesRound.confirmedCount - 1 + confirmedRows.length,
        confirmedAmount: adminDuesRound.confirmedAmount - adminDuesRound.amount + confirmedRows.length * adminDuesRound.amount,
      },
      items: currentAdminDuesPayments,
      page: 0,
      size: 100,
      totalElements: currentAdminDuesPayments.length,
      totalPages: 1,
    })
  }),

  http.post(/\/api\/v1\/admin\/payment-obligations\/\d+\/confirm$/, ({ request }) =>
    reviewPayment(request, 'CONFIRMED'),
  ),

  http.post(/\/api\/v1\/admin\/payment-obligations\/\d+\/reject$/, ({ request }) =>
    reviewPayment(request, 'REJECTED'),
  ),
]

async function reviewPayment(request: Request, status: 'CONFIRMED' | 'REJECTED') {
  const paymentId = Number(new URL(request.url).pathname.match(/payment-obligations\/(\d+)\/(?:confirm|reject)$/)?.[1])
  const body = (await request.json()) as { version: number; note?: string }
  const duesIndex = currentAdminDuesPayments.findIndex((payment) => payment.paymentId === paymentId)
  const eventIndex = currentAdminEventParticipants.findIndex((participant) => participant.paymentId === paymentId)
  const current = duesIndex >= 0
    ? currentAdminDuesPayments[duesIndex]
    : eventIndex >= 0
      ? currentAdminEventParticipants[eventIndex]
      : undefined
  const currentStatus = current && ('status' in current ? current.status : current.paymentStatus)
  if (!current) return problem(404, 'RESOURCE_NOT_FOUND', '납부 항목을 찾을 수 없습니다.')
  if (currentStatus !== 'REPORTED' || current.version !== body.version) {
    return problem(409, 'PAYMENT_STATE_CONFLICT', '이미 처리되었거나 최신 상태가 아닙니다.')
  }

  const version = current.version + 1
  if (duesIndex >= 0) {
    currentAdminDuesPayments[duesIndex] = { ...currentAdminDuesPayments[duesIndex], status, version }
  }
  if (eventIndex >= 0) {
    currentAdminEventParticipants[eventIndex] = {
      ...currentAdminEventParticipants[eventIndex],
      paymentStatus: status,
      version,
    }
  }
  return HttpResponse.json({
    paymentId,
    status,
    version,
    reviewedAt: new Date().toISOString(),
  })
}

function partLabel(part: MeResponse['part']): string {
  return (
    {
      PLAN: 'Plan',
      DESIGN: 'Design',
      PE_WEB: 'PE(Web)',
      PE_MOBILE: 'PE(Mobile)',
    } as const
  )[part ?? 'PE_WEB']
}

import { http, HttpResponse } from 'msw'
import type {
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

export function resetMockState() {
  currentMember = { ...activeMember }
  eventDetails = createEventDetails()
  paymentDetails = createPaymentDetails()
  currentAdminEventParticipants = structuredClone(adminEventParticipants)
  currentAdminDuesPayments = structuredClone(adminDuesPayments)
}

function problem(status: number, code: string, detail: string) {
  return HttpResponse.json(
    {
      title: status === 404 ? 'Not Found' : 'Conflict',
      status,
      code,
      detail,
      timestamp: new Date().toISOString(),
    },
    { status },
  )
}

export const handlers = [
  http.get(/\/api\/v1\/me$/, () => HttpResponse.json(currentMember)),

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

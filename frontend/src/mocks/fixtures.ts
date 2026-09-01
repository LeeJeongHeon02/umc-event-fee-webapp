import type {
  AdminDashboardResponse,
  AdminDuesRoundSummary,
  AdminEventParticipant,
  AdminEventSummary,
  AdminPaymentRow,
  EventDetail,
  EventListItem,
  MeResponse,
  PaymentDetail,
  PaymentSummary,
} from '../services/types'

export const activeMember: MeResponse = {
  id: 15,
  kakaoProfileName: '길동',
  name: '홍길동',
  part: 'PE_WEB',
  displayNickname: 'PE(Web) 홍길동',
  role: 'STAFF',
  status: 'ACTIVE',
  onboardingCompleted: true,
  approvedAt: '2026-08-25T04:00:00Z',
}

export const adminEventSummary: AdminEventSummary = {
  id: 42,
  title: '2026 가을 해커톤',
  startsAt: '2026-09-15T10:00:00Z',
  capacity: 50,
  joinedCount: 23,
  feeAmount: 15000,
  unpaidCount: 6,
  reportedCount: 3,
  confirmedCount: 14,
}

export const adminDuesRound: AdminDuesRoundSummary = {
  id: 7,
  title: '2026년 2학기 회비',
  amount: 30000,
  dueAt: '2026-09-30T14:59:59Z',
  targetCount: 48,
  unpaidCount: 12,
  reportedCount: 4,
  confirmedCount: 32,
  confirmedAmount: 960000,
}

export const adminEventParticipants: AdminEventParticipant[] = [
  {
    participationId: 84,
    memberId: 15,
    nickname: 'PE(Web) 홍길동',
    name: '홍길동',
    part: 'PE_WEB',
    joinedAt: '2026-09-02T10:20:00Z',
    paymentId: 311,
    amount: 15000,
    paymentStatus: 'UNPAID',
    version: 0,
  },
  {
    participationId: 85,
    memberId: 16,
    nickname: 'Design 김민지',
    name: '김민지',
    part: 'DESIGN',
    joinedAt: '2026-09-02T11:10:00Z',
    paymentId: 313,
    amount: 15000,
    paymentStatus: 'REPORTED',
    latestReport: {
      id: 913,
      method: 'KAKAO_PAY_CODE',
      senderName: '김민지',
      transferredAt: '2026-09-01T09:30:00Z',
      reportedAt: '2026-09-01T09:32:00Z',
    },
    version: 1,
  },
  {
    participationId: 86,
    memberId: 17,
    nickname: 'Plan 이수현',
    name: '이수현',
    part: 'PLAN',
    joinedAt: '2026-09-03T03:15:00Z',
    paymentId: 314,
    amount: 15000,
    paymentStatus: 'CONFIRMED',
    latestReport: {
      id: 914,
      method: 'BANK_TRANSFER',
      senderName: '이수현',
      reportedAt: '2026-09-01T03:00:00Z',
    },
    version: 2,
  },
  {
    participationId: 87,
    memberId: 18,
    nickname: 'PE(Mobile) 박서준',
    name: '박서준',
    part: 'PE_MOBILE',
    joinedAt: '2026-09-03T05:40:00Z',
    paymentId: 315,
    amount: 15000,
    paymentStatus: 'REPORTED',
    latestReport: {
      id: 915,
      method: 'BANK_TRANSFER',
      senderName: '박서준',
      reportedAt: '2026-09-01T05:41:00Z',
    },
    version: 1,
  },
]

export const adminDuesPayments: AdminPaymentRow[] = [
  {
    paymentId: 701,
    memberId: 15,
    nickname: 'PE(Web) 홍길동',
    name: '홍길동',
    part: 'PE_WEB',
    amount: 30000,
    status: 'REPORTED',
    dueAt: '2026-09-30T14:59:59Z',
    latestReport: {
      id: 1701,
      method: 'BANK_TRANSFER',
      senderName: '홍길동',
      reportedAt: '2026-09-01T08:10:00Z',
    },
    version: 1,
  },
  {
    paymentId: 702,
    memberId: 16,
    nickname: 'Design 김민지',
    name: '김민지',
    part: 'DESIGN',
    amount: 30000,
    status: 'CONFIRMED',
    dueAt: '2026-09-30T14:59:59Z',
    latestReport: {
      id: 1702,
      method: 'KAKAO_PAY_CODE',
      senderName: '김민지',
      reportedAt: '2026-08-31T04:20:00Z',
    },
    version: 2,
  },
  {
    paymentId: 703,
    memberId: 17,
    nickname: 'Plan 이수현',
    name: '이수현',
    part: 'PLAN',
    amount: 30000,
    status: 'UNPAID',
    dueAt: '2026-09-30T14:59:59Z',
    version: 0,
  },
  {
    paymentId: 704,
    memberId: 18,
    nickname: 'PE(Mobile) 박서준',
    name: '박서준',
    part: 'PE_MOBILE',
    amount: 30000,
    status: 'REPORTED',
    dueAt: '2026-09-30T14:59:59Z',
    latestReport: {
      id: 1704,
      method: 'BANK_TRANSFER',
      senderName: '박서준',
      reportedAt: '2026-09-01T05:41:00Z',
    },
    version: 1,
  },
  {
    paymentId: 705,
    memberId: 19,
    nickname: 'PE(Web) 최유진',
    name: '최유진',
    part: 'PE_WEB',
    amount: 30000,
    status: 'REJECTED',
    dueAt: '2026-09-30T14:59:59Z',
    latestReport: {
      id: 1705,
      method: 'BANK_TRANSFER',
      senderName: '최유진',
      reportedAt: '2026-08-30T12:10:00Z',
    },
    version: 2,
  },
]

export function createAdminDashboard(): AdminDashboardResponse {
  return {
    memberCount: 48,
    pendingMemberCount: 3,
    upcomingEventCount: 3,
    unpaidCount: 18,
    reportedCount: 7,
    expectedAmount: 1785000,
    confirmedAmount: 1290000,
    collectionRate: 72.3,
    upcomingEvents: [
      adminEventSummary,
      {
        id: 43,
        title: '신입 부원 네트워킹 데이',
        startsAt: '2026-09-23T09:30:00Z',
        capacity: 40,
        joinedCount: 11,
        feeAmount: 0,
        unpaidCount: 0,
        reportedCount: 0,
        confirmedCount: 0,
      },
    ],
    activeDuesRounds: [adminDuesRound],
    recentReports: adminDuesPayments.filter((payment) => payment.status === 'REPORTED'),
  }
}

export const pendingMember: MeResponse = {
  id: 51,
  kakaoProfileName: '길동',
  name: '홍길동',
  part: 'PE_WEB',
  displayNickname: 'PE(Web) 홍길동',
  role: 'MEMBER',
  status: 'PENDING',
  onboardingCompleted: true,
}

export const eventFixtures: EventListItem[] = [
  {
    id: 42,
    title: '2026 가을 해커톤',
    summary: '팀을 꾸려 하루 동안 교내 문제를 해결해요.',
    location: '공학관 101호',
    startsAt: '2026-09-15T10:00:00Z',
    endsAt: '2026-09-15T12:00:00Z',
    registrationDeadline: '2026-09-12T14:59:59Z',
    capacity: 50,
    joinedCount: 23,
    feeAmount: 15000,
    status: 'PUBLISHED',
    myParticipationStatus: 'JOINED',
    myPaymentStatus: 'UNPAID',
  },
  {
    id: 43,
    title: '신입 부원 네트워킹 데이',
    summary: '파트를 넘어 서로의 관심사와 프로젝트를 나눠요.',
    location: '학생회관 라운지',
    startsAt: '2026-09-23T09:30:00Z',
    endsAt: '2026-09-23T11:00:00Z',
    registrationDeadline: '2026-09-20T14:59:59Z',
    capacity: 40,
    joinedCount: 11,
    feeAmount: 0,
    status: 'PUBLISHED',
  },
  {
    id: 44,
    title: '사이드 프로젝트 데모데이',
    summary: '동아리원들의 프로젝트를 발표하고 피드백을 주고받아요.',
    location: '온라인 · Discord',
    startsAt: '2026-10-02T10:00:00Z',
    registrationDeadline: '2026-09-29T14:59:59Z',
    joinedCount: 8,
    feeAmount: 5000,
    status: 'PUBLISHED',
  },
]

export const paymentFixture: PaymentSummary = {
  id: 311,
  type: 'EVENT_FEE',
  amount: 15000,
  status: 'UNPAID',
  dueAt: '2026-09-12T14:59:59Z',
  source: {
    type: 'EVENT',
    id: 42,
    title: '2026 가을 해커톤',
    dueAt: '2026-09-12T14:59:59Z',
  },
  updatedAt: '2026-09-02T10:20:00Z',
  version: 0,
}

export const duesFixture: PaymentSummary = {
  id: 312,
  type: 'MEMBERSHIP_DUE',
  amount: 30000,
  status: 'REPORTED',
  dueAt: '2026-09-30T14:59:59Z',
  source: {
    type: 'DUES_ROUND',
    id: 7,
    title: '2026년 2학기 회비',
    dueAt: '2026-09-30T14:59:59Z',
  },
  updatedAt: '2026-09-01T08:10:00Z',
  version: 1,
}

export function createEventDetails(): EventDetail[] {
  return eventFixtures.map((event) => ({
    ...event,
    description:
      event.id === 42
        ? '기획, 디자인, 웹, 모바일 파트가 섞인 팀으로 하루 동안 교내의 작은 문제를 해결합니다. 결과물의 완성도보다 함께 문제를 정의하고 빠르게 실험하는 경험에 집중해요.'
        : event.summary ?? '',
    allowLateCancellation: false,
    canJoin: !event.myParticipationStatus,
    canCancel: event.myParticipationStatus === 'JOINED',
    myParticipation:
      event.myParticipationStatus === 'JOINED'
        ? {
            id: 84,
            status: 'JOINED',
            joinedAt: '2026-09-02T10:20:00Z',
            version: 0,
          }
        : undefined,
    myPayment: event.id === 42 ? paymentFixture : undefined,
  }))
}

export function createPaymentDetails(): PaymentDetail[] {
  return [
    {
      ...paymentFixture,
      paymentDestination: {
        bankName: '카카오뱅크',
        accountNumber: '3333-12-3456789',
        accountHolder: '김총무',
        kakaoPayReceiveUrl: 'https://qr.kakaopay.com/example',
      },
      statusHistory: [
        {
          toStatus: 'UNPAID',
          changedAt: '2026-09-02T10:20:00Z',
        },
      ],
    },
    {
      ...duesFixture,
      paymentDestination: {
        bankName: '카카오뱅크',
        accountNumber: '3333-12-3456789',
        accountHolder: '김총무',
        kakaoPayReceiveUrl: 'https://qr.kakaopay.com/example',
      },
      latestReport: {
        id: 899,
        method: 'BANK_TRANSFER',
        senderName: '홍길동',
        reportedAt: '2026-09-01T08:10:00Z',
      },
      statusHistory: [
        {
          toStatus: 'UNPAID',
          changedAt: '2026-08-28T02:00:00Z',
        },
        {
          fromStatus: 'UNPAID',
          toStatus: 'REPORTED',
          changedAt: '2026-09-01T08:10:00Z',
        },
      ],
    },
  ]
}

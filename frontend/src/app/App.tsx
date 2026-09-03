import { Navigate, Route, Routes } from 'react-router-dom'
import { AppShell } from './AppShell'
import { AdminShell } from './AdminShell'
import { AdminDashboardPage } from '../pages/AdminDashboardPage'
import { AdminEventParticipantsPage } from '../pages/AdminEventParticipantsPage'
import { AdminFeePaymentsPage } from '../pages/AdminFeePaymentsPage'
import { AdminEventsPage } from '../pages/AdminEventsPage'
import { EventDetailPage } from '../pages/EventDetailPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { OnboardingPage } from '../pages/OnboardingPage'
import { PaymentPage } from '../pages/PaymentPage'
import { PendingPage } from '../pages/PendingPage'
import { AdminMembersPage } from '../pages/AdminMembersPage'
import { AdminDuesRoundsPage } from '../pages/AdminDuesRoundsPage'
import { AdminRefundsPage } from '../pages/AdminRefundsPage'
import { MemberGate, StaffGate } from './RouteGates'
import { AdminPaymentSettingsPage } from '../pages/AdminPaymentSettingsPage'
import { NotificationsPage } from '../pages/NotificationsPage'

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/onboarding" element={<OnboardingPage />} />
      <Route path="/pending" element={<PendingPage />} />
      <Route element={<MemberGate />}>
        <Route element={<AppShell />}>
          <Route path="/home" element={<HomePage />} />
          <Route path="/events/:eventId" element={<EventDetailPage />} />
          <Route path="/payments/:paymentId" element={<PaymentPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
        </Route>
      </Route>
      <Route element={<StaffGate />}>
        <Route path="/admin" element={<AdminShell />}>
          <Route index element={<AdminDashboardPage />} />
          <Route path="events" element={<AdminEventsPage />} />
          <Route path="events/:eventId/participants" element={<AdminEventParticipantsPage />} />
          <Route path="dues" element={<AdminDuesRoundsPage />} />
          <Route path="fees/:duesRoundId/payments" element={<AdminFeePaymentsPage />} />
          <Route path="members" element={<AdminMembersPage />} />
          <Route path="refunds" element={<AdminRefundsPage />} />
          <Route path="settings" element={<AdminPaymentSettingsPage />} />
        </Route>
      </Route>
      <Route path="/" element={<Navigate to="/home" replace />} />
      <Route path="*" element={<Navigate to="/home" replace />} />
    </Routes>
  )
}

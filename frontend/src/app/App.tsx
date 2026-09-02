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

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/onboarding" element={<OnboardingPage />} />
      <Route path="/pending" element={<PendingPage />} />
      <Route element={<AppShell />}>
        <Route path="/home" element={<HomePage />} />
        <Route path="/events/:eventId" element={<EventDetailPage />} />
        <Route path="/payments/:paymentId" element={<PaymentPage />} />
      </Route>
      <Route path="/admin" element={<AdminShell />}>
        <Route index element={<AdminDashboardPage />} />
        <Route path="events" element={<AdminEventsPage />} />
        <Route path="events/:eventId/participants" element={<AdminEventParticipantsPage />} />
        <Route path="fees/:duesRoundId/payments" element={<AdminFeePaymentsPage />} />
      </Route>
      <Route path="/" element={<Navigate to="/home" replace />} />
      <Route path="*" element={<Navigate to="/home" replace />} />
    </Routes>
  )
}

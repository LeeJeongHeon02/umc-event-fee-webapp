import { Navigate, Outlet } from 'react-router-dom'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { useCurrentMember } from '../hooks/useCurrentMember'

// Account access does not require staff approval, so pending/suspended members can log out too.
export function AuthenticatedGate() {
  const me = useCurrentMember()
  if (me.isLoading) return <LoadingState label="회원 정보를 확인하는 중" />
  if (me.isError || !me.data) return <Navigate to="/login" replace />
  return <Outlet />
}

export function MemberGate() {
  const me = useCurrentMember()
  if (me.isLoading) return <LoadingState label="회원 정보를 확인하는 중" />
  if (me.isError) return <Navigate to="/login" replace />
  if (!me.data!.onboardingCompleted) return <Navigate to="/onboarding" replace />
  if (me.data!.status === 'PENDING') return <Navigate to="/pending" replace />
  if (me.data!.status !== 'ACTIVE') return <ErrorState title="이용할 수 없는 계정입니다" description="운영진에게 계정 상태를 문의해 주세요." />
  return <Outlet />
}

export function StaffGate() {
  const me = useCurrentMember()
  if (me.isLoading) return <LoadingState label="권한을 확인하는 중" />
  if (me.isError) return <Navigate to="/login" replace />
  if (!me.data!.onboardingCompleted) return <Navigate to="/onboarding" replace />
  if (me.data!.status === 'PENDING') return <Navigate to="/pending" replace />
  if (me.data!.status !== 'ACTIVE' || me.data!.role === 'MEMBER') return <Navigate to="/home" replace />
  return <Outlet />
}

import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { getMe } from '../services/api'

export function PendingPage() {
  const meQuery = useQuery({ queryKey: ['me'], queryFn: getMe })
  const me = meQuery.data

  return (
    <main className="auth-page auth-page--soft">
      <section className="auth-panel pending-panel">
        <div className="pending-illustration" aria-hidden="true">
          <span>✓</span>
        </div>
        <span className="eyebrow">ALMOST THERE</span>
        <h1>가입 신청이<br />운영진에게 전달됐어요.</h1>
        <p>동아리원 확인이 끝나면 모든 행사와 회비 정보를 볼 수 있습니다. 보통 하루 안에 승인돼요.</p>
        <div className="pending-profile">
          <div className="avatar avatar--large">{me?.name?.slice(0, 1) ?? 'D'}</div>
          <div>
            <strong>{me?.displayNickname ?? '동아리원 정보를 확인 중'}</strong>
            <span>승인 대기 중</span>
          </div>
        </div>
        <Link className="secondary-button secondary-button--block" to="/home">개발용 홈 미리보기</Link>
      </section>
    </main>
  )
}

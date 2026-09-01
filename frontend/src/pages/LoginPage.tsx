import { useNavigate } from 'react-router-dom'

export function LoginPage() {
  const navigate = useNavigate()

  function startKakaoLogin() {
    if (import.meta.env.DEV) {
      navigate('/onboarding')
      return
    }
    window.location.assign('/oauth2/authorization/kakao')
  }

  return (
    <main className="auth-page">
      <section className="auth-panel auth-panel--login">
        <div className="login-visual" aria-hidden="true">
          <div className="login-visual__orb login-visual__orb--one" />
          <div className="login-visual__orb login-visual__orb--two" />
          <div className="login-visual__card">
            <span>D:</span>
            <div>
              <strong>Make things</strong>
              <small>together.</small>
            </div>
          </div>
        </div>
        <div className="auth-copy">
          <span className="eyebrow">OUR CLUB, ONE PLACE</span>
          <h1>행사도, 회비도<br />가볍게 한곳에서.</h1>
          <p>동아리 활동에만 집중할 수 있도록 신청부터 납부 확인까지 연결해 드려요.</p>
        </div>
        <button className="kakao-button" type="button" onClick={startKakaoLogin}>
          <span className="kakao-symbol" aria-hidden="true">••</span>
          카카오로 계속하기
        </button>
        <p className="legal-copy">계속하면 서비스 이용약관과 개인정보 처리방침에 동의하게 됩니다.</p>
      </section>
    </main>
  )
}


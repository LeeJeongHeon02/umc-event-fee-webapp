import { Link } from 'react-router-dom'
import { LogoutButton } from '../components/LogoutButton'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { useCurrentMember } from '../hooks/useCurrentMember'
import type { MemberPart, MemberRole, MeResponse } from '../services/types'
import '../styles/mypage.css'

const partLabels: Record<MemberPart, string> = { PLAN: 'Plan', DESIGN: 'Design', PE_WEB: 'PE(Web)', PE_MOBILE: 'PE(Mobile)' }
const roleLabels: Record<MemberRole, string> = { MEMBER: '동아리원', STAFF: '운영진', ADMIN: '관리자' }
const statusLabels: Record<MeResponse['status'], string> = { ACTIVE: '활동 중', PENDING: '승인 대기', SUSPENDED: '이용 정지', WITHDRAWN: '탈퇴' }

export function MyPage() {
  const query = useCurrentMember()
  if (query.isLoading) return <LoadingState label="내 정보를 불러오는 중" />
  if (!query.data) return <ErrorState title="내 정보를 불러오지 못했어요" />
  const me = query.data
  const name = me.name || me.kakaoProfileName || me.loginId || '동아리원'
  const active = me.status === 'ACTIVE' && me.onboardingCompleted

  return <div className="page my-page">
    <header className="my-page__heading"><span className="eyebrow">MY ACCOUNT</span><h1>마이페이지</h1><p>내 정보와 동아리 활동을 한곳에서 확인하세요.</p></header>
    <section className="my-profile" aria-label="내 프로필">
      <div className="avatar avatar--large" aria-hidden="true">{name.slice(0, 1)}</div>
      <div><h2>{name}</h2><p>{me.displayNickname || '아직 프로필을 설정하지 않았어요.'}</p></div>
      <span className="my-profile__status">{statusLabels[me.status]}</span>
    </section>
    <section className="my-account" aria-labelledby="account-heading">
      <h2 id="account-heading">계정 정보</h2>
      <dl>
        <div><dt>이름</dt><dd>{me.name || '미설정'}</dd></div>
        <div><dt>파트</dt><dd>{me.part ? partLabels[me.part] : '미설정'}</dd></div>
        <div><dt>회원 권한</dt><dd>{roleLabels[me.role]}</dd></div>
        <div><dt>로그인 방식</dt><dd>{me.loginId ? '아이디 로그인' : '카카오 로그인'}</dd></div>
        {me.loginId && <div><dt>아이디</dt><dd>{me.loginId}</dd></div>}
        <div><dt>전화번호</dt><dd>{me.phoneNumber?.replace(/^(\d{3})(\d{3,4})(\d{4})$/, '$1-$2-$3') || '미등록'}</dd></div>
      </dl>
    </section>
    <section className="my-links" aria-label="내 활동 메뉴">
      {active ? <>
        <Link to="/events">행사 확인하기<span aria-hidden="true">↗</span></Link>
        <Link to="/payments">내 납부 내역<span aria-hidden="true">↗</span></Link>
        {me.role !== 'MEMBER' && <Link to="/admin">운영진 센터<span aria-hidden="true">↗</span></Link>}
      </> : !me.onboardingCompleted ? <Link to="/onboarding">프로필 설정하기<span aria-hidden="true">→</span></Link>
        : <p>{me.status === 'PENDING' ? '운영진 승인 후 행사와 납부 메뉴를 사용할 수 있어요.' : '이용 상태는 운영진에게 문의해 주세요.'}</p>}
    </section>
    <footer className="my-page__footer"><LogoutButton /><p>로그아웃해도 가입 정보와 참가 내역은 유지됩니다.</p></footer>
  </div>
}

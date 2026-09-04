import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { loginLocalMember, registerLocalMember } from '../services/api'

const loginSchema = z.object({
  loginId: z.string().trim().min(1, '아이디를 입력해 주세요.'),
  password: z.string().min(1, '비밀번호를 입력해 주세요.'),
})

const registerSchema = z.object({
  loginId: z.string().trim().regex(
    /^[a-z0-9._-]{4,30}$/,
    '영문 소문자, 숫자, 마침표, 밑줄, 하이픈으로 4~30자를 입력해 주세요.',
  ),
  password: z.string().min(8, '비밀번호는 8자 이상이어야 해요.').max(72, '비밀번호는 72자 이하여야 해요.'),
  phoneNumber: z.string().trim().regex(
    /^01[016789]-?[0-9]{3,4}-?[0-9]{4}$/,
    '휴대전화 번호 형식을 확인해 주세요.',
  ),
})

type LoginValues = z.infer<typeof loginSchema>
type RegisterValues = z.infer<typeof registerSchema>

export function LoginPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [registrationMessage, setRegistrationMessage] = useState('')
  const loginForm = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { loginId: '', password: '' },
  })
  const registerForm = useForm<RegisterValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: { loginId: '', password: '', phoneNumber: '' },
  })
  const loginMutation = useMutation({
    mutationFn: loginLocalMember,
    onSuccess: (result) => {
      queryClient.removeQueries()
      queryClient.setQueryData(['me'], result.member)
      navigate(result.redirectPath)
    },
  })
  const registerMutation = useMutation({
    mutationFn: registerLocalMember,
    onSuccess: (result) => {
      registerForm.reset()
      loginForm.setValue('loginId', result.loginId)
      setRegistrationMessage('회원가입이 완료됐어요. 만든 계정으로 로그인해 주세요.')
      setMode('login')
    },
  })

  function startKakaoLogin() {
    if (import.meta.env.DEV && import.meta.env.VITE_ENABLE_MOCKS !== 'false') {
      navigate('/onboarding')
      return
    }
    window.location.assign('/api/v1/oauth2/authorization/kakao')
  }

  function changeMode(nextMode: 'login' | 'register') {
    setMode(nextMode)
    setRegistrationMessage('')
    loginMutation.reset()
    registerMutation.reset()
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

        <div className="auth-divider"><span>또는</span></div>
        <div className="auth-tabs" aria-label="로컬 계정 메뉴">
          <button type="button" aria-label="로그인 화면 열기" aria-pressed={mode === 'login'} onClick={() => changeMode('login')}>로그인</button>
          <button type="button" aria-label="회원가입 화면 열기" aria-pressed={mode === 'register'} onClick={() => changeMode('register')}>회원가입</button>
        </div>

        {mode === 'login' ? (
          <form className="local-auth-form" onSubmit={loginForm.handleSubmit((values) => loginMutation.mutate(values))}>
            {registrationMessage && <p className="form-success" role="status">{registrationMessage}</p>}
            <label className="field-label" htmlFor="login-id">아이디</label>
            <input className="text-input" id="login-id" autoComplete="username" autoCapitalize="none" {...loginForm.register('loginId')} />
            {loginForm.formState.errors.loginId && <p className="field-error">{loginForm.formState.errors.loginId.message}</p>}
            <label className="field-label" htmlFor="login-password">비밀번호</label>
            <input className="text-input" id="login-password" type="password" autoComplete="current-password" {...loginForm.register('password')} />
            {loginForm.formState.errors.password && <p className="field-error">{loginForm.formState.errors.password.message}</p>}
            {loginMutation.isError && <p className="form-error" role="alert">{loginMutation.error.message}</p>}
            <button className="primary-button primary-button--block" disabled={loginMutation.isPending} type="submit">
              {loginMutation.isPending ? '로그인 중…' : '아이디로 로그인'}
            </button>
          </form>
        ) : (
          <form className="local-auth-form" onSubmit={registerForm.handleSubmit((values) => registerMutation.mutate(values))}>
            <label className="field-label" htmlFor="register-id">아이디</label>
            <input className="text-input" id="register-id" autoComplete="username" autoCapitalize="none" placeholder="영문 소문자 4자 이상" {...registerForm.register('loginId')} />
            {registerForm.formState.errors.loginId && <p className="field-error">{registerForm.formState.errors.loginId.message}</p>}
            <label className="field-label" htmlFor="register-password">비밀번호</label>
            <input className="text-input" id="register-password" type="password" autoComplete="new-password" placeholder="8자 이상" {...registerForm.register('password')} />
            {registerForm.formState.errors.password && <p className="field-error">{registerForm.formState.errors.password.message}</p>}
            <label className="field-label" htmlFor="phone-number">전화번호</label>
            <input className="text-input" id="phone-number" type="tel" inputMode="tel" autoComplete="tel" placeholder="010-1234-5678" {...registerForm.register('phoneNumber')} />
            {registerForm.formState.errors.phoneNumber && <p className="field-error">{registerForm.formState.errors.phoneNumber.message}</p>}
            {registerMutation.isError && <p className="form-error" role="alert">{registerMutation.error.message}</p>}
            <button className="primary-button primary-button--block" disabled={registerMutation.isPending} type="submit">
              {registerMutation.isPending ? '가입 중…' : '회원가입'}
            </button>
          </form>
        )}
        <p className="legal-copy">계속하면 서비스 이용약관과 개인정보 처리방침에 동의하게 됩니다.</p>
      </section>
    </main>
  )
}

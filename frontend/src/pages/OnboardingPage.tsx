import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { completeOnboarding } from '../services/api'
import type { MemberPart } from '../services/types'
import { useCurrentMember } from '../hooks/useCurrentMember'
import { LogoutButton } from '../components/LogoutButton'

const parts: { value: MemberPart; label: string; description: string }[] = [
  { value: 'PLAN', label: 'Plan', description: '문제를 정의하고 경험을 설계해요' },
  { value: 'DESIGN', label: 'Design', description: '브랜드와 인터페이스를 만들어요' },
  { value: 'PE_WEB', label: 'PE(Web)', description: '웹에서 아이디어를 구현해요' },
  { value: 'PE_MOBILE', label: 'PE(Mobile)', description: '모바일 경험을 구현해요' },
]

const schema = z.object({
  name: z.string().trim().min(1, '이름을 입력해 주세요.').max(50, '이름은 50자 이하여야 해요.'),
  part: z.enum(['PLAN', 'DESIGN', 'PE_WEB', 'PE_MOBILE']),
})

type FormValues = z.infer<typeof schema>

export function OnboardingPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const meQuery = useCurrentMember()
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', part: 'PE_WEB' },
  })
  useEffect(() => {
    if (meQuery.data?.kakaoProfileName) setValue('name', meQuery.data.kakaoProfileName)
  }, [meQuery.data?.kakaoProfileName, setValue])
  const selectedPart = watch('part')
  const name = watch('name')
  const selectedLabel = parts.find((part) => part.value === selectedPart)?.label ?? ''
  const mutation = useMutation({
    mutationFn: completeOnboarding,
    onSuccess: (member) => {
      queryClient.setQueryData(['me'], member)
      navigate(member.status === 'ACTIVE' ? '/home' : '/pending')
    },
  })

  return (
    <main className="auth-page auth-page--soft">
      <section className="auth-panel onboarding-panel">
        <div className="step-indicator" aria-label="가입 2단계 중 1단계">
          <span className="step-indicator__active" />
          <span />
        </div>
        <div className="auth-copy">
          <span className="eyebrow">WELCOME TO D:CLUB</span>
          <h1>어떤 파트에서<br />함께하고 있나요?</h1>
          <p>{meQuery.data?.kakaoProfileName
            ? '카카오 프로필 이름을 불러왔어요. 동아리에서 사용하는 이름으로 바꿀 수 있습니다.'
            : '동아리에서 사용할 이름과 활동 파트를 알려주세요.'}</p>
        </div>

        <form className="onboarding-form" onSubmit={handleSubmit((values) => mutation.mutate(values))}>
          <fieldset>
            <legend>파트 선택</legend>
            <div className="part-grid">
              {parts.map((part) => (
                <label className="part-option" key={part.value}>
                  <input type="radio" value={part.value} {...register('part')} />
                  <span>
                    <strong>{part.label}</strong>
                    <small>{part.description}</small>
                  </span>
                </label>
              ))}
            </div>
          </fieldset>

          <label className="field-label" htmlFor="name">이름</label>
          <input className="text-input" id="name" autoComplete="name" {...register('name')} />
          {errors.name && <p className="field-error">{errors.name.message}</p>}

          <div className="nickname-preview">
            <span>최종 닉네임</span>
            <strong>{selectedLabel} {name.trim() || '이름'}</strong>
          </div>

          {mutation.isError && <p className="form-error" role="alert">설정을 저장하지 못했어요. 다시 시도해 주세요.</p>}
          <button className="primary-button primary-button--block" disabled={mutation.isPending} type="submit">
            {mutation.isPending ? '저장 중…' : '이 이름으로 시작하기'}
          </button>
        </form>
        <LogoutButton />
      </section>
    </main>
  )
}

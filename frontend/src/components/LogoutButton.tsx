import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { logoutMember } from '../services/api'

export function LogoutButton() {
  const client = useQueryClient()
  const navigate = useNavigate()
  const logout = useMutation({
    mutationFn: logoutMember,
    onSuccess: async () => {
      // Stop in-flight member queries before clearing private data for the next account.
      await client.cancelQueries()
      client.clear()
      navigate('/login', { replace: true })
    },
  })

  return <div className="logout-action">
    <button type="button" className="secondary-button secondary-button--block" disabled={logout.isPending} onClick={() => logout.mutate()}>
      {logout.isPending ? '로그아웃 중…' : '로그아웃'}
    </button>
    {logout.isError && <p className="form-error" role="alert">로그아웃하지 못했어요. 연결 상태를 확인하고 다시 시도해 주세요.</p>}
  </div>
}

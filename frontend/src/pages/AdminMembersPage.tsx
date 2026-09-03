import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { approveAdminMember, changeAdminMemberRole, getAdminMembers, suspendAdminMember } from '../services/api'
import type { MemberRole } from '../services/types'

export function AdminMembersPage() {
  const client = useQueryClient()
  const members = useQuery({ queryKey: ['admin', 'members'], queryFn: getAdminMembers })
  const action = useMutation({
    mutationFn: (input: { kind: 'approve' | 'suspend' | 'role'; id: number; version: number; role?: MemberRole }) => {
      if (input.kind === 'approve') return approveAdminMember(input.id, input.version)
      if (input.kind === 'suspend') return suspendAdminMember(input.id, input.version)
      return changeAdminMemberRole(input.id, input.role!, input.version)
    },
    onSuccess: () => client.invalidateQueries({ queryKey: ['admin'] }),
  })
  if (members.isLoading) return <LoadingState label="회원 목록을 불러오는 중" />
  if (members.isError) return <ErrorState />
  return <div className="admin-page">
    <section className="admin-page-heading"><div><span className="eyebrow">MEMBERS</span><h1>회원 관리</h1><p>온보딩을 마친 회원을 승인하고 운영진 권한을 관리합니다.</p></div></section>
    {action.error && <p className="form-error" role="alert">{action.error.message}</p>}
    <section className="admin-list-panel"><div className="admin-table-wrap"><table className="admin-table">
      <thead><tr><th>회원</th><th>카카오 이름</th><th>상태</th><th>역할</th><th>관리</th></tr></thead>
      <tbody>{members.data!.map((member) => <tr key={member.id}>
        <td><div className="member-cell"><span>{(member.name ?? member.kakaoProfileName).slice(0, 1)}</span><div><strong>{member.displayNickname}</strong><small>{member.part?.replace('_', ' ') ?? '온보딩 전'}</small></div></div></td>
        <td>{member.kakaoProfileName}</td><td>{member.status}</td>
        <td><select className="select-input" aria-label={`${member.displayNickname} 역할`} value={member.role} disabled={member.status !== 'ACTIVE' || action.isPending} onChange={(event) => action.mutate({ kind: 'role', id: member.id, version: member.version, role: event.target.value as MemberRole })}><option value="MEMBER">MEMBER</option><option value="STAFF">STAFF</option><option value="ADMIN">ADMIN</option></select></td>
        <td>{member.status === 'PENDING' && member.onboardingCompleted ? <button className="table-button table-button--confirm" onClick={() => action.mutate({ kind: 'approve', id: member.id, version: member.version })}>가입 승인</button> : member.status === 'ACTIVE' ? <button className="table-button" onClick={() => action.mutate({ kind: 'suspend', id: member.id, version: member.version })}>이용 정지</button> : <span className="table-muted">처리 없음</span>}</td>
      </tr>)}</tbody>
    </table></div></section>
  </div>
}

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ErrorState, LoadingState } from '../components/AsyncState'
import { StatusBadge } from '../components/StatusBadge'
import { completeAdminRefund, getAdminRefunds } from '../services/api'
import { formatWon } from '../services/format'

export function AdminRefundsPage() {
  const client = useQueryClient()
  const refunds = useQuery({ queryKey: ['admin', 'refunds'], queryFn: getAdminRefunds })
  const complete = useMutation({ mutationFn: ({ id, version }: { id: number; version: number }) => completeAdminRefund(id, version, '운영진 환불 완료'), onSuccess: () => client.invalidateQueries({ queryKey: ['admin'] }) })
  if (refunds.isLoading) return <LoadingState label="환불 목록을 불러오는 중" />
  if (refunds.isError) return <ErrorState />
  return <div className="admin-page"><section className="admin-page-heading"><div><span className="eyebrow">REFUNDS</span><h1>환불 관리</h1><p>실제 계좌 환불 후 완료 처리하세요. 이 앱이 자동 이체를 수행하지는 않습니다.</p></div></section>
    {complete.error && <p className="form-error">{complete.error.message}</p>}<section className="admin-list-panel"><div className="admin-table-wrap"><table className="admin-table"><thead><tr><th>부원</th><th>금액</th><th>상태</th><th>관리</th></tr></thead><tbody>{refunds.data!.map((item) => <tr key={item.paymentId}><td>{item.nickname}</td><td>{formatWon(item.amount)}</td><td><StatusBadge status={item.status} /></td><td><button className="table-button table-button--confirm" disabled={complete.isPending} onClick={() => complete.mutate({ id: item.paymentId, version: item.version })}>환불 완료</button></td></tr>)}</tbody></table></div>{refunds.data!.length === 0 && <p className="admin-empty">환불 대기 건이 없습니다.</p>}</section>
  </div>
}

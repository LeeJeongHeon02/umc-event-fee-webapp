import { useQuery } from '@tanstack/react-query'
import { getMe } from '../services/api'

export function useCurrentMember() {
  return useQuery({ queryKey: ['me'], queryFn: getMe })
}

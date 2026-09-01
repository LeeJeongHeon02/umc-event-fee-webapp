const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
  weekday: 'short',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
  timeZone: 'Asia/Seoul',
})

const shortDateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: 'numeric',
  day: 'numeric',
  weekday: 'short',
  timeZone: 'Asia/Seoul',
})

const currencyFormatter = new Intl.NumberFormat('ko-KR')

export function formatDateTime(value: string): string {
  return dateFormatter.format(new Date(value))
}

export function formatShortDate(value: string): string {
  return shortDateFormatter.format(new Date(value))
}

export function formatWon(value: number): string {
  return value === 0 ? '무료' : `${currencyFormatter.format(value)}원`
}


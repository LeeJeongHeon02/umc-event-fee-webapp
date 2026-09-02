import { expect, test } from '@playwright/test'

test('실제 Spring API로 송금 신고·승인·참가 취소와 행사 생성을 처리한다', async ({ page }) => {
  await page.goto('/home')
  await expect(page.getByText('PE(Web) 김총무')).toBeVisible()
  await expect(page.locator('a[href="/events/42"]')).toBeVisible()

  await page.goto('/payments/311')
  await expect(page.getByLabel('송금자명')).toHaveValue('김총무')
  await page.getByLabel('실제로 송금을 완료했습니다.').check()
  await page.getByRole('button', { name: '송금했어요' }).click()
  await expect(page.getByRole('heading', { name: '송금 신고가 접수됐어요.' })).toBeVisible()

  await page.goto('/admin/events/42/participants')
  await page.getByRole('button', { name: '김총무 납부 승인' }).click()
  await expect(page.getByRole('row', { name: /김총무/ })).toContainText('납부 완료')

  await page.goto('/events/42')
  await page.getByRole('button', { name: '참가 취소' }).click()
  await page.getByRole('button', { name: '참가 취소 확정' }).click()
  await expect(page.getByRole('heading', { name: '참가 신청이 취소됐어요.' })).toBeVisible()
  await expect(page.getByText(/환불 대기 상태/)).toBeVisible()

  await page.goto('/admin/events')
  await page.getByRole('textbox', { name: '행사명' }).fill('E2E 네트워킹 행사')
  await page.getByRole('textbox', { name: '상세 내용' }).fill('실제 API로 생성하는 행사입니다.')
  await page.getByRole('button', { name: '초안 저장' }).click()
  await expect(page.getByText('행사 초안을 저장했습니다.')).toBeVisible()
  await page.getByRole('button', { name: '행사 공개' }).click()
  await expect(page.getByText('동아리원에게 행사를 공개했습니다.')).toBeVisible()
  await expect(page.getByText('공개').last()).toBeVisible()
})

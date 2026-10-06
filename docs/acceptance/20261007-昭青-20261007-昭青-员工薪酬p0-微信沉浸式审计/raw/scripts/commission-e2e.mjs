import fs from 'node:fs'
import path from 'node:path'

const API = 'http://127.0.0.1:8090/api/'
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261007-昭青-20261007-昭青-员工薪酬p0-微信沉浸式审计'
const EMP = '1790000000000000106'
const PROJECT = '232968907537776640'
const CONTRACT = 65406.10
const MANUAL_RATE = 2
const TEST_REMARK = '提成P0本地测试'
const RECEIVABLE_1 = '232968908057870336'
const RECEIVABLE_2 = '232968908057870337'
const RECEIVABLE_3 = '232968908062064640'
const RECEIPT_1_AMOUNT = 19621.83
const RECEIPT_2_AMOUNT = 26162.44
const RECEIPT_3_AMOUNT = 19621.83

const money = value => Number(Number(value).toFixed(2))
const enumKey = value => typeof value === 'object' && value !== null ? value.key : value
const pageRecords = page => page.records || page.content || []

async function call(uri, data = {}, token = '') {
  const response = await fetch(API + uri, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(data)
  })
  let body
  try {
    body = await response.json()
  } catch {
    throw new Error(`${uri}: HTTP ${response.status}, 响应不是 JSON`)
  }
  return { httpStatus: response.status, body }
}

async function ok(uri, data = {}, token = '') {
  const result = await call(uri, data, token)
  if (!result.body?.success) {
    const code = result.body?.errorCode?.code || `HTTP-${result.httpStatus}`
    const message = result.body?.errorCode?.message || '调用失败'
    throw new Error(`${uri}: ${code} ${message}`)
  }
  return result.body.data
}

function assert(condition, message) {
  if (!condition) throw new Error(`断言失败：${message}`)
}

function releaseCumulative(cumulativeReceipt, commissionAmount) {
  const amount = commissionAmount * cumulativeReceipt / CONTRACT
  return money(Math.min(commissionAmount, amount))
}

async function commissionInfo(commissionId, token) {
  return ok('payroll/commission/info', { commissionId }, token)
}

async function ensureReceipt(receiptPage, receivableId, amount, idempotencyKey, token) {
  return pageRecords(receiptPage).find(item => String(item.receivable?.receivableId || item.receivableId) === receivableId)
    || ok('finance/receipt/save', {
      projectId: PROJECT,
      receivableId,
      amount,
      account: 'CompanyBank',
      receivedAt: '2026-10-07',
      payerName: '提成P0本地测试客户',
      idempotencyKey,
      remark: TEST_REMARK
    }, token)
}

async function receiptPage(token) {
  return ok('finance/receipt/page', { projectId: PROJECT, current: 1, size: 100 }, token)
}

async function main() {
  const finance = (await ok('auth/session/password-login', {
    phone: '19900000002',
    password: 'Maihong2026'
  })).token
  assert(finance, '财务登录未返回 token')

  const commissionPage = await ok('payroll/commission/page', { current: 1, size: 100 }, finance)
  const existing = pageRecords(commissionPage)
    .filter(item => item.remark === TEST_REMARK)
    .sort((a, b) => Number(b.commissionId) - Number(a.commissionId))[0]

  if (existing) {
    const latest = await commissionInfo(existing.commissionId, finance)
    if (money(latest.releasedAmount) === money(latest.commissionAmount)
        && money(latest.paidAmount) === money(latest.commissionAmount)
        && enumKey(latest.status) === 'Paid') {
      const evidencePath = path.join(RUN, 'raw/api/commission-e2e.json')
      const evidence = JSON.parse(fs.readFileSync(evidencePath, 'utf8'))
      evidence.reverifiedAt = new Date().toISOString()
      evidence.reverifiedCommissionId = String(latest.commissionId)
      evidence.reverifiedFinalReleased = money(latest.releasedAmount)
      evidence.reverifiedFinalPaid = money(latest.paidAmount)
      evidence.reverifiedFinalStatus = enumKey(latest.status)
      fs.writeFileSync(evidencePath, JSON.stringify(evidence, null, 2))
      console.log(JSON.stringify(evidence, null, 2))
      return
    }
    throw new Error('检测到未完成的提成 P0 测试数据，请先执行 commission-e2e-reset.sql 后重跑')
  }

  const saved = await ok('payroll/commission/save', {
    empId: EMP,
    projectId: PROJECT,
    contractAmount: CONTRACT,
    manualRate: MANUAL_RATE,
    remark: TEST_REMARK
  }, finance)
  const commissionId = String(saved.commissionId)
  assert(money(saved.commissionAmount) === 1308.12, '提成计提金额应为 1308.12')
  assert(money(saved.releasedAmount) === 0, '签约计提时释放金额应为 0')
  assert(enumKey(saved.status) === 'Accrued', '签约计提后状态应为 Accrued')

  const payablePage = await ok('finance/payment/employee/page', { current: 1, size: 100 }, finance)
  const payable = pageRecords(payablePage)
    .find(item => String(item.payeeEmployee?.empId) === EMP && String(item.sourceId) === commissionId)
  assert(payable, '未找到新提成对应的员工应付')

  const beforeRelease = await call('finance/payment/employee/save', {
    payableIds: [payable.payableId],
    amount: money(saved.commissionAmount),
    account: 'CompanyBank',
    paidAt: '2026-10-07',
    batchNo: 'COMMISSION-P0-PRE',
    idempotencyKey: 'comm-p0-pre-release',
    remark: '未回款前置发放负例'
  }, finance)
  assert(!beforeRelease.body.success, '未回款提成不应允许发放')

  let receipts = await receiptPage(finance)
  await ensureReceipt(receipts, RECEIVABLE_1, RECEIPT_1_AMOUNT, 'comm-p0-receipt-1', finance)
  const afterFirstReceipt = await commissionInfo(commissionId, finance)
  const firstExpected = releaseCumulative(RECEIPT_1_AMOUNT, money(saved.commissionAmount))
  assert(money(afterFirstReceipt.releasedAmount) === firstExpected, `第一笔回款后应释放 ${firstExpected}`)

  const overRelease1 = await call('finance/payment/employee/save', {
    payableIds: [payable.payableId],
    amount: money(firstExpected + 0.01),
    account: 'CompanyBank',
    paidAt: '2026-10-07',
    batchNo: 'COMMISSION-P0-OVER-1',
    idempotencyKey: 'comm-p0-over-release-1',
    remark: '超过已释放金额负例'
  }, finance)
  assert(!overRelease1.body.success, '超过已释放金额的提成不应允许发放')

  const firstPayment = await ok('finance/payment/employee/save', {
    payableIds: [payable.payableId],
    amount: firstExpected,
    account: 'CompanyBank',
    paidAt: '2026-10-07',
    batchNo: 'COMMISSION-P0-1',
    idempotencyKey: 'comm-p0-pay-1',
    remark: '第一笔已释放提成发放'
  }, finance)
  assert(money(firstPayment.amount) === firstExpected, '第一笔发放金额应等于本次释放金额')

  receipts = await receiptPage(finance)
  await ensureReceipt(receipts, RECEIVABLE_2, RECEIPT_2_AMOUNT, 'comm-p0-receipt-2', finance)
  const cumulative2 = RECEIPT_1_AMOUNT + RECEIPT_2_AMOUNT
  const cumulativeRelease2 = releaseCumulative(cumulative2, money(saved.commissionAmount))
  const secondExpected = money(cumulativeRelease2 - firstExpected)
  const afterSecondReceipt = await commissionInfo(commissionId, finance)
  assert(money(afterSecondReceipt.releasedAmount) === cumulativeRelease2, `第二笔回款后累计释放应为 ${cumulativeRelease2}`)

  const secondPayload = {
    payableIds: [payable.payableId],
    amount: secondExpected,
    account: 'CompanyBank',
    paidAt: '2026-10-07',
    batchNo: 'COMMISSION-P0-2',
    idempotencyKey: 'comm-p0-pay-2',
    remark: '第二笔已释放提成发放'
  }
  const secondPayment = await ok('finance/payment/employee/save', secondPayload, finance)
  const secondRetry = await ok('finance/payment/employee/save', secondPayload, finance)
  assert(String(secondRetry.paymentId) === String(secondPayment.paymentId), '同 key 重试应返回同一 Payment')

  receipts = await receiptPage(finance)
  await ensureReceipt(receipts, RECEIVABLE_3, RECEIPT_3_AMOUNT, 'comm-p0-receipt-3', finance)
  const cumulative3 = cumulative2 + RECEIPT_3_AMOUNT
  const cumulativeRelease3 = releaseCumulative(cumulative3, money(saved.commissionAmount))
  const thirdExpected = money(cumulativeRelease3 - cumulativeRelease2)
  const afterThirdReceipt = await commissionInfo(commissionId, finance)
  assert(money(afterThirdReceipt.releasedAmount) === cumulativeRelease3, `第三笔回款后累计释放应为 ${cumulativeRelease3}`)

  const thirdPayment = await ok('finance/payment/employee/save', {
    payableIds: [payable.payableId],
    amount: thirdExpected,
    account: 'CompanyBank',
    paidAt: '2026-10-07',
    batchNo: 'COMMISSION-P0-3',
    idempotencyKey: 'comm-p0-pay-3',
    remark: '第三笔已释放提成发放'
  }, finance)
  assert(enumKey(thirdPayment.payable?.status) === 'Paid', '全部释放并发放后应付状态应为 Paid')

  const releasePage = await ok('payroll/commission/release/page', {
    commissionId,
    current: 1,
    size: 20
  }, finance)
  const final = await commissionInfo(commissionId, finance)
  assert(money(final.releasedAmount) === 1308.12, '最终累计释放应为 1308.12')
  assert(money(final.paidAmount) === 1308.12, '最终累计发放应为 1308.12')
  assert(enumKey(final.status) === 'Paid', '最终提成状态应为 Paid')
  assert(pageRecords(releasePage).length === 3, '应产生 3 条按回款释放记录')

  const evidence = {
    generatedAt: new Date().toISOString(),
    tenant: '昭青',
    projectId: PROJECT,
    commissionId,
    contractAmount: money(CONTRACT),
    commissionAmount: money(saved.commissionAmount),
    beforeRelease: {
      success: beforeRelease.body.success,
      code: beforeRelease.body.errorCode?.code,
      message: beforeRelease.body.errorCode?.message
    },
    firstReleased: firstExpected,
    overReleaseCents: {
      success: overRelease1.body.success,
      code: overRelease1.body.errorCode?.code,
      message: overRelease1.body.errorCode?.message
    },
    firstPaymentId: String(firstPayment.paymentId),
    firstPaymentAmount: money(firstPayment.amount),
    secondReleased: secondExpected,
    secondPaymentId: String(secondPayment.paymentId),
    secondPaymentAmount: money(secondPayment.amount),
    retrySamePayment: String(secondRetry.paymentId) === String(secondPayment.paymentId),
    thirdReleased: thirdExpected,
    thirdPaymentId: String(thirdPayment.paymentId),
    thirdPaymentAmount: money(thirdPayment.amount),
    finalReleased: money(final.releasedAmount),
    finalPaid: money(final.paidAmount),
    finalStatus: enumKey(final.status),
    releaseCount: pageRecords(releasePage).length
  }
  fs.writeFileSync(path.join(RUN, 'raw/api/commission-e2e.json'), JSON.stringify(evidence, null, 2))
  console.log(JSON.stringify(evidence, null, 2))
}

main().catch(error => {
  console.error(error.stack || error)
  process.exit(1)
})

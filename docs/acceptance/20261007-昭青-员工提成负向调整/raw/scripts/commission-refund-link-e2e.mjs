import fs from 'node:fs'
import path from 'node:path'

const API = 'http://127.0.0.1:8090/api/'
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261007-昭青-员工提成负向调整'
const PROJECT = '232968907537776640'
const EMP = '1790000000000000106'
const PERIOD = '2026-12'
const REFUND_AMOUNT = 10000
const REFUND_REASON = '客户退款联动E2E'
const REFUND_KEY = 'customer-refund-link-e2e-v1'
const OVER_REFUND_KEY = 'customer-refund-link-e2e-over-v1'

const money = value => Number(Number(value).toFixed(2))
const enumKey = value => typeof value === 'object' && value !== null ? value.key : value
const pageRecords = page => page.records || page.content || []

async function call(uri, data = {}, token = '') {
  const response = await fetch(API + uri, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(data)
  })
  const body = await response.json().catch(() => ({}))
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

async function main() {
  const finance = (await ok('auth/session/password-login', {
    phone: '19900000002',
    password: 'Maihong2026'
  })).token
  assert(finance, '财务登录未返回 token')

  const receiptPage = await ok('finance/receipt/page', { projectId: PROJECT, current: 1, size: 100 }, finance)
  const receipt = pageRecords(receiptPage)
    .filter(item => Number(item.amount) >= REFUND_AMOUNT)
    .sort((a, b) => Number(b.amount) - Number(a.amount))[0]
  assert(receipt, '未找到可用于退款的客户收款')

  const refundPayload = {
    receiptId: receipt.receiptId,
    amount: REFUND_AMOUNT,
    account: 'CompanyBank',
    refundAt: '2026-10-07',
    reason: REFUND_REASON,
    idempotencyKey: REFUND_KEY
  }
  const refund = await ok('finance/refund/save', refundPayload, finance)
  const refundRetry = await ok('finance/refund/save', refundPayload, finance)
  assert(String(refundRetry.refundId) === String(refund.refundId), '同 key 退款重试应返回同一退款')
  const refundConflict = await call('finance/refund/save', {
    ...refundPayload,
    amount: REFUND_AMOUNT - 1000
  }, finance)
  assert(!refundConflict.body.success && refundConflict.body.errorCode?.code === '409',
    '同 key 不同金额退款意图应返回 409')

  const commissionPage = await ok('payroll/commission/page', { current: 1, size: 100 }, finance)
  const commission = pageRecords(commissionPage)
    .filter(item => String(item.employee?.empId) === EMP)
    .filter(item => enumKey(item.status) === 'Paid')
    .sort((a, b) => Number(b.commissionId) - Number(a.commissionId))[0]
  assert(commission, '未找到已发放员工提成')
  const contractAmount = money(commission.contractAmount)
  const commissionAmount = money(commission.commissionAmount)
  const cumulativeAllowed = money(commissionAmount * (contractAmount - REFUND_AMOUNT) / contractAmount)
  const expectedDeduction = money(commission.paidAmount - cumulativeAllowed)

  let adjustmentPage = await ok('payroll/commission/adjustment/page', { current: 1, size: 100 }, finance)
  let adjustment = pageRecords(adjustmentPage)
    .find(item => item.refund && String(item.refund.refundId) === String(refund.refundId))
  assert(adjustment, '退款未自动生成提成负向调整')
  assert(money(adjustment.deductionAmount) === expectedDeduction, `自动负向调整应为 ${expectedDeduction}`)
  assert(enumKey(adjustment.status) === 'Pending', '新退款调整应为 Pending')

  const overRefund = await call('finance/refund/save', {
    receiptId: receipt.receiptId,
    amount: Number(receipt.amount) + 1,
    account: 'CompanyBank',
    refundAt: '2026-10-07',
    reason: '超额退款负例E2E',
    idempotencyKey: OVER_REFUND_KEY
  }, finance)
  assert(!overRefund.body.success, '超过原收款可退金额应被阻断')

  const sheet = await ok('payroll/sheet/build', { period: PERIOD }, finance)
  let itemPage = await ok('payroll/item/page', { sheetId: sheet.sheetId, empId: EMP, current: 1, size: 20 }, finance)
  let item = pageRecords(itemPage).find(row => row.attributionKey === 'COMPANY')
  assert(item, '未找到下期公司工资项')
  assert(money(item.commissionAmount) === -expectedDeduction, '退款调整未进入下期工资项')

  adjustmentPage = await ok('payroll/commission/adjustment/page', { current: 1, size: 100 }, finance)
  adjustment = pageRecords(adjustmentPage)
    .find(row => row.refund && String(row.refund.refundId) === String(refund.refundId))
  assert(money(adjustment.appliedAmount) === expectedDeduction, '退款调整应记录已抵扣金额')
  assert(enumKey(adjustment.status) === 'Included', '退款调整应标记已进入工资单')

  const rebuilt = await ok('payroll/sheet/build', { period: PERIOD }, finance)
  itemPage = await ok('payroll/item/page', { sheetId: rebuilt.sheetId, empId: EMP, current: 1, size: 20 }, finance)
  const rebuiltItem = pageRecords(itemPage).find(row => row.attributionKey === 'COMPANY')
  const rebuiltAdjustment = pageRecords(await ok('payroll/commission/adjustment/page', { current: 1, size: 100 }, finance))
    .find(row => row.refund && String(row.refund.refundId) === String(refund.refundId))
  assert(money(rebuiltItem.commissionAmount) === -expectedDeduction, '工资单重建不得重复抵扣退款调整')
  assert(money(rebuiltAdjustment.appliedAmount) === expectedDeduction, '工资单重建后调整金额不得重复')

  const projectReport = await ok('finance/project/report', { projectId: PROJECT }, finance)
  assert(money(projectReport.employeeCommissionAmount) === cumulativeAllowed,
    '退款应冲减原项目员工提成成本')

  const evidence = {
    generatedAt: new Date().toISOString(),
    tenant: '昭青',
    projectId: PROJECT,
    originalReceiptId: String(receipt.receiptId),
    originalReceiptAmount: money(receipt.amount),
    refundId: String(refund.refundId),
    refundAmount: REFUND_AMOUNT,
    refundRetrySameRecord: String(refundRetry.refundId) === String(refund.refundId),
    refundIdempotencyConflict: {
      success: refundConflict.body.success,
      code: refundConflict.body.errorCode?.code,
      message: refundConflict.body.errorCode?.message
    },
    overRefundBlocked: {
      success: overRefund.body.success,
      code: overRefund.body.errorCode?.code,
      message: overRefund.body.errorCode?.message
    },
    adjustmentId: String(adjustment.adjustmentId),
    adjustmentDeduction: expectedDeduction,
    adjustmentStatus: enumKey(rebuiltAdjustment.status),
    adjustmentApplied: money(rebuiltAdjustment.appliedAmount),
    nextPeriod: PERIOD,
    nextPeriodItemId: String(item.itemId),
    nextPeriodCommissionAmount: money(rebuiltItem.commissionAmount),
    projectEmployeeCommissionCost: money(projectReport.employeeCommissionAmount),
    rebuildIdempotent: true
  }
  fs.writeFileSync(path.join(RUN, 'raw/api/commission-refund-link-e2e.json'), JSON.stringify(evidence, null, 2))
  console.log(JSON.stringify(evidence, null, 2))
}

main().catch(error => {
  console.error(error.stack || error)
  process.exit(1)
})

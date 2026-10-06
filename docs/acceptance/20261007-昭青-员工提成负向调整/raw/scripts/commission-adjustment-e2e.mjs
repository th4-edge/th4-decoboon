import fs from 'node:fs'
import path from 'node:path'

const API = 'http://127.0.0.1:8090/api/'
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261007-昭青-员工提成负向调整'
const EMP = '1790000000000000106'
const PERIOD = '2026-12'
const REFUND_REMARK = '提成退款负向调整E2E'
const MANUAL_NEGATIVE_REMARK = '提成手工超额负例E2E'
const REFUND_AMOUNT = 10000
const REFUND_IDEMPOTENCY_KEY = 'commission-adjustment-e2e-refund'
const MANUAL_IDEMPOTENCY_KEY = 'commission-adjustment-e2e-manual-over'

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

  const commissionPage = await ok('payroll/commission/page', { current: 1, size: 100 }, finance)
  const commission = pageRecords(commissionPage)
    .filter(item => String(item.employee?.empId) === EMP)
    .filter(item => enumKey(item.status) === 'Paid')
    .sort((a, b) => Number(b.commissionId) - Number(a.commissionId))[0]
  assert(commission, '未找到已发放的员工提成')
  const commissionId = String(commission.commissionId)
  const paidAmount = money(commission.paidAmount)
  const contractAmount = money(commission.contractAmount)
  const commissionAmount = money(commission.commissionAmount)

  let adjustmentPage = await ok('payroll/commission/adjustment/page', { current: 1, size: 100 }, finance)
  let refundAdjustment = pageRecords(adjustmentPage)
    .find(item => item.originalCommission && String(item.originalCommission.commissionId) === commissionId
      && item.remark === REFUND_REMARK)
  const refundPayload = {
    originalCommissionId: commissionId,
    type: 'Refund',
    refundAmount: REFUND_AMOUNT,
    idempotencyKey: REFUND_IDEMPOTENCY_KEY,
    occurredAt: '2026-10-07',
    remark: REFUND_REMARK
  }
  const createdFresh = !refundAdjustment
  if (createdFresh) refundAdjustment = await ok('payroll/commission/adjustment/save', refundPayload, finance)
  const refundRetry = await ok('payroll/commission/adjustment/save', refundPayload, finance)
  assert(String(refundRetry.adjustmentId) === String(refundAdjustment.adjustmentId), '同 key 重试应返回同一调整')
  const refundConflict = await call('payroll/commission/adjustment/save', {
    ...refundPayload,
    refundAmount: REFUND_AMOUNT - 1000
  }, finance)
  assert(!refundConflict.body.success && refundConflict.body.errorCode?.code === '409',
    '同一幂等键复用为不同退款意图应返回 409 冲突')

  const effectiveReceipt = 65406.10 - REFUND_AMOUNT
  const cumulativeAllowed = money(Math.min(commissionAmount, commissionAmount * effectiveReceipt / contractAmount))
  const expectedDeduction = money(paidAmount - cumulativeAllowed)
  assert(money(refundAdjustment.deductionAmount) === expectedDeduction,
    `退款后负向调整应为 ${expectedDeduction}`)
  if (createdFresh) {
    assert(money(refundAdjustment.appliedAmount) === 0, '新负向调整进入下期前应为未抵扣')
    assert(enumKey(refundAdjustment.status) === 'Pending', '新负向调整应为 Pending')
  }

  const overManual = await call('payroll/commission/adjustment/save', {
    originalCommissionId: commissionId,
    type: 'CrossPeriod',
    deductionAmount: money(paidAmount - expectedDeduction + 1),
    idempotencyKey: MANUAL_IDEMPOTENCY_KEY,
    occurredAt: '2026-10-07',
    remark: MANUAL_NEGATIVE_REMARK
  }, finance)
  assert(!overManual.body.success, '超过原提成未调整已发金额的手工调整应被阻断')

  const sheet = await ok('payroll/sheet/build', { period: PERIOD }, finance)
  let itemPage = await ok('payroll/item/page', { sheetId: sheet.sheetId, empId: EMP, current: 1, size: 20 }, finance)
  let item = pageRecords(itemPage).find(row => row.attributionKey === 'COMPANY')
  assert(item, '未找到下期公司工资项')
  assert(money(item.commissionAmount) === -expectedDeduction, '下期工资项应包含负向提成调整')
  assert(money(item.netAmount) === money(Number(item.grossAmount) - Number(item.deductionAmount)), '工资项应发、扣款、实发应自洽')

  adjustmentPage = await ok('payroll/commission/adjustment/page', { current: 1, size: 100 }, finance)
  const included = pageRecords(adjustmentPage)
    .find(row => String(row.adjustmentId) === String(refundAdjustment.adjustmentId))
  assert(money(included.appliedAmount) === expectedDeduction, '调整应记录全额已抵扣')
  assert(enumKey(included.status) === 'Included', '调整应标记已进入工资单')
  assert(String(included.payrollItem?.itemId) === String(item.itemId), '调整应关联到下期工资项')

  const rebuilt = await ok('payroll/sheet/build', { period: PERIOD }, finance)
  itemPage = await ok('payroll/item/page', { sheetId: rebuilt.sheetId, empId: EMP, current: 1, size: 20 }, finance)
  const rebuiltItem = pageRecords(itemPage).find(row => row.attributionKey === 'COMPANY')
  adjustmentPage = await ok('payroll/commission/adjustment/page', { current: 1, size: 100 }, finance)
  const rebuiltAdjustment = pageRecords(adjustmentPage)
    .find(row => String(row.adjustmentId) === String(refundAdjustment.adjustmentId))
  assert(money(rebuiltItem.commissionAmount) === -expectedDeduction, '重生成下期工资单不得重复抵扣')
  assert(money(rebuiltAdjustment.appliedAmount) === expectedDeduction, '重生成后累计抵扣应保持不变')
  assert(enumKey(rebuiltAdjustment.status) === 'Included', '重生成后调整状态仍应为 Included')

  const projectReport = await ok('finance/project/report', { projectId: commission.project.projectId }, finance)
  assert(money(projectReport.employeeCommissionAmount) === cumulativeAllowed,
    '退款负向调整应从原项目提成成本中冲减')

  const evidence = {
    generatedAt: new Date().toISOString(),
    tenant: '昭青',
    originalCommissionId: commissionId,
    originalPaidAmount: paidAmount,
    refundAmount: REFUND_AMOUNT,
    effectiveReceipt: money(effectiveReceipt),
    cumulativeAllowedRelease: cumulativeAllowed,
    deductionAmount: expectedDeduction,
    refundRetrySameAdjustment: String(refundRetry.adjustmentId) === String(refundAdjustment.adjustmentId),
    idempotencyConflict: {
      success: refundConflict.body.success,
      code: refundConflict.body.errorCode?.code,
      message: refundConflict.body.errorCode?.message
    },
    manualOverAdjustmentBlocked: {
      success: overManual.body.success,
      code: overManual.body.errorCode?.code,
      message: overManual.body.errorCode?.message
    },
    nextPeriod: PERIOD,
    nextPeriodSheetId: String(sheet.sheetId),
    nextPeriodItemId: String(item.itemId),
    nextPeriodCommissionAmount: money(item.commissionAmount),
    nextPeriodNetAmount: money(item.netAmount),
    adjustmentStatus: enumKey(rebuiltAdjustment.status),
    adjustmentAppliedAmount: money(rebuiltAdjustment.appliedAmount),
    projectEmployeeCommissionCost: money(projectReport.employeeCommissionAmount),
    rebuildIdempotent: true
  }
  fs.writeFileSync(path.join(RUN, 'raw/api/commission-adjustment-e2e.json'), JSON.stringify(evidence, null, 2))
  console.log(JSON.stringify(evidence, null, 2))
}

main().catch(error => {
  console.error(error.stack || error)
  process.exit(1)
})

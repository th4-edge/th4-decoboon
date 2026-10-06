import fs from 'node:fs'
import path from 'node:path'

const API = 'http://127.0.0.1:8090/api/'
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261007-单BOSS一人装企全链路'
const PHONE = process.env.SINGLE_BOSS_PHONE || '18890000001'
const PASSWORD = process.env.SINGLE_BOSS_PASSWORD
const EMP_ID = '8900000000000000101'
const PROJECT_ID = '8900000000000000304'
const CONTRACT_AMOUNT = 100000

if (!PASSWORD) throw new Error('缺少 SINGLE_BOSS_PASSWORD')

async function post(uri, data = {}, token = '') {
  const response = await fetch(API + uri, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(data)
  })
  const body = await response.json().catch(() => ({}))
  if (!body.success) {
    const error = new Error(`${uri}: ${body.errorCode?.message || response.status}`)
    error.code = body.errorCode?.code
    throw error
  }
  return body.data
}

async function rawPost(uri, data, token) {
  const response = await fetch(API + uri, {
    method: 'POST',
    headers: { 'content-type': 'application/json', token },
    body: JSON.stringify(data)
  })
  return response.json()
}

const { token } = await post('auth/session/password-login', { phone: PHONE, password: PASSWORD })
const results = {}

const receivable = await post('finance/receivable/save', {
  projectId: PROJECT_ID,
  name: '单BOSS提成回款节点',
  stageName: '签约回款',
  type: 'Progress',
  amount: CONTRACT_AMOUNT,
  dueDate: '2026-10-07',
  remark: '单BOSS提成链验收'
}, token)
results.receivable = {
  receivableId: receivable.receivableId,
  amount: receivable.amount,
  receivedAmount: receivable.receivedAmount,
  status: receivable.status
}

const commission = await post('payroll/commission/save', {
  empId: EMP_ID,
  projectId: PROJECT_ID,
  contractAmount: CONTRACT_AMOUNT,
  manualRate: 2,
  remark: '单BOSS提成链验收'
}, token)
results.commissionAccrued = {
  commissionId: commission.commissionId,
  employee: commission.employee?.name,
  project: commission.project?.name,
  contractAmount: commission.contractAmount,
  manualRate: commission.manualRate,
  commissionAmount: commission.commissionAmount,
  accruedAmount: commission.accruedAmount,
  releasedAmount: commission.releasedAmount,
  paidAmount: commission.paidAmount,
  status: commission.status
}

const payablePageBefore = await post('finance/payment/employee/page', { current: 1, size: 20 }, token)
const payablesBefore = (payablePageBefore.records || payablePageBefore.content || [])
  .filter(item => String(item.payeeEmployee?.empId) === EMP_ID && item.type?.key === 'EmployeeCommission')
if (payablesBefore.length !== 1) throw new Error(`提成应付数量异常: ${payablesBefore.length}`)
const payable = payablesBefore[0]
results.payable = {
  payableId: payable.payableId,
  type: payable.type,
  amount: payable.amount,
  paidAmount: payable.paidAmount
}

const preReleasePayment = await rawPost('finance/payment/employee/save', {
  payableIds: [payable.payableId],
  amount: 800,
  account: 'CompanyBank',
  paidAt: '2026-10-07',
  batchNo: 'SINGLE-BOSS-COMMISSION',
  idempotencyKey: 'single-boss-commission-before-release',
  remark: '未释放提成发放阻断'
}, token)
results.preReleasePayment = {
  success: preReleasePayment.success,
  code: preReleasePayment.errorCode?.code,
  message: preReleasePayment.errorCode?.message
}

const firstReceipt = await post('finance/receipt/save', {
  projectId: PROJECT_ID,
  receivableId: receivable.receivableId,
  amount: 40000,
  account: 'CompanyBank',
  receivedAt: '2026-10-07',
  payerName: '单BOSS验收客户',
  idempotencyKey: 'single-boss-receipt-40',
  remark: '首笔回款'
}, token)
const firstReceiptRetry = await post('finance/receipt/save', {
  projectId: PROJECT_ID,
  receivableId: receivable.receivableId,
  amount: 40000,
  account: 'CompanyBank',
  receivedAt: '2026-10-07',
  payerName: '单BOSS验收客户',
  idempotencyKey: 'single-boss-receipt-40',
  remark: '首笔回款'
}, token)
const afterFirst = await post('payroll/commission/info', { commissionId: commission.commissionId }, token)
results.firstReceipt = {
  receiptId: firstReceipt.receiptId,
  amount: firstReceipt.amount,
  retrySameReceipt: String(firstReceiptRetry.receiptId) === String(firstReceipt.receiptId),
  releasedAmount: afterFirst.releasedAmount,
  paidAmount: afterFirst.paidAmount,
  status: afterFirst.status
}

const firstPaymentKey = 'single-boss-commission-pay-1'
const firstPaymentPayload = {
  payableIds: [payable.payableId],
  amount: 800,
  account: 'CompanyBank',
  paidAt: '2026-10-07',
  batchNo: 'SINGLE-BOSS-COMMISSION-1',
  idempotencyKey: firstPaymentKey,
  remark: '首批提成发放'
}
const firstPayment = await post('finance/payment/employee/save', firstPaymentPayload, token)
const firstPaymentRetry = await post('finance/payment/employee/save', firstPaymentPayload, token)
results.firstPayment = {
  paymentId: firstPayment.paymentId,
  amount: firstPayment.amount,
  retrySamePayment: String(firstPaymentRetry.paymentId) === String(firstPayment.paymentId)
}

const secondReceipt = await post('finance/receipt/save', {
  projectId: PROJECT_ID,
  receivableId: receivable.receivableId,
  amount: 60000,
  account: 'CompanyBank',
  receivedAt: '2026-10-07',
  payerName: '单BOSS验收客户',
  idempotencyKey: 'single-boss-receipt-60',
  remark: '第二笔回款'
}, token)
const afterSecond = await post('payroll/commission/info', { commissionId: commission.commissionId }, token)
const payablePageFinal = await post('finance/payment/employee/page', { current: 1, size: 20 }, token)
const commissionPayable = (payablePageFinal.records || payablePageFinal.content || [])
  .find(item => String(item.payableId) === String(payable.payableId))
const outstanding = Number(commissionPayable.amount) - Number(commissionPayable.paidAmount || 0)
results.secondReceipt = {
  receiptId: secondReceipt.receiptId,
  amount: secondReceipt.amount,
  releasedAmount: afterSecond.releasedAmount,
  paidAmount: afterSecond.paidAmount,
  outstanding,
  status: afterSecond.status
}

const secondPayment = await post('finance/payment/employee/save', {
  payableIds: [payable.payableId],
  amount: outstanding,
  account: 'CompanyBank',
  paidAt: '2026-10-07',
  batchNo: 'SINGLE-BOSS-COMMISSION-2',
  idempotencyKey: 'single-boss-commission-pay-2',
  remark: '尾批提成发放'
}, token)
const finalCommission = await post('payroll/commission/info', { commissionId: commission.commissionId }, token)
results.final = {
  secondPaymentId: secondPayment.paymentId,
  paidAmount: finalCommission.paidAmount,
  releasedAmount: finalCommission.releasedAmount,
  accruedAmount: finalCommission.accruedAmount,
  status: finalCommission.status
}

const evidence = {
  generatedAt: new Date().toISOString(),
  tenant: '单BOSS验收企业',
  projectId: PROJECT_ID,
  results
}
fs.writeFileSync(path.join(RUN, 'raw/api/single-boss-commission-e2e.json'), JSON.stringify(evidence, null, 2))
console.log(JSON.stringify(evidence, null, 2))

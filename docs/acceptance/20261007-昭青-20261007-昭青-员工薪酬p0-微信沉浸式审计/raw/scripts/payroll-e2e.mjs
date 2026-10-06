import fs from 'node:fs'
import path from 'node:path'

const API = 'http://127.0.0.1:8090/api/'
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261007-昭青-20261007-昭青-员工薪酬p0-微信沉浸式审计'
const credentials = {
  boss: ['19900000001', 'Maihong2026'],
  finance: ['19900000002', 'Maihong2026'],
  manager: ['19900000003', 'Maihong2026'],
  employee: ['19900000005', 'Maihong2026']
}
const results = {}

async function post(uri, data = {}, token = '') {
  const res = await fetch(API + uri, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(data)
  })
  const body = await res.json().catch(() => ({}))
  if (!body.success) throw new Error(`${uri}: ${body.errorCode?.message || res.status}`)
  return body.data
}

async function login(phone, password) {
  const data = await post('auth/session/password-login', { phone, password })
  return data.token
}

const tokens = {}
for (const [role, [phone, password]] of Object.entries(credentials)) {
  tokens[role] = await login(phone, password)
}
results.logins = Object.fromEntries(Object.keys(tokens).map(role => [role, 'PASS']))

const EMP_ID = process.env.EMP_ID || '1790000000000000105'
const PERIOD = process.env.PERIOD || '2026-10'

let compensationPage = await post('payroll/compensation/page', { current: 1, size: 100 }, tokens.finance)
let compensation = (compensationPage.records || compensationPage.content || []).find(item => String(item.employee?.empId) === EMP_ID)
if (!compensation) {
  compensation = await post('payroll/compensation/save', {
    empId: EMP_ID,
    baseSalary: 6000,
    effectiveFrom: '2026-10-01',
    remark: '员工薪酬P0本地测试'
  }, tokens.finance)
}
results.compensation = { id: compensation.schemeId, baseSalary: compensation.baseSalary, status: compensation.status }

let attendancePage = await post('attendance/summary/page', { month: PERIOD, current: 1, size: 100 }, tokens.finance)
let attendance = (attendancePage.records || attendancePage.content || []).find(item => String(item.employee?.empId) === EMP_ID)
if (!attendance) {
  attendance = await post('attendance/summary/save', {
    empId: EMP_ID,
    month: PERIOD,
    scheduledDays: 22,
    normalDays: 21,
    personalLeaveDays: 1,
    sickLeaveDays: 0,
    absenceDays: 0,
    externalDays: 0,
    annualLeaveDays: 0,
    compLeaveDays: 0,
    remark: '员工薪酬P0本地测试'
  }, tokens.finance)
}
results.attendanceCreated = { id: attendance.summaryId, status: attendance.status }

const attendanceConfirmed = await post('attendance/summary/update', {
  summaryId: attendance.summaryId,
  status: 'Confirmed'
}, tokens.manager)
results.attendanceConfirmed = { id: attendanceConfirmed.summaryId, status: attendanceConfirmed.status }

const sheetDraft = await post('payroll/sheet/build', { period: PERIOD }, tokens.finance)
results.sheetBuilt = {
  id: sheetDraft.sheetId,
  status: sheetDraft.status,
  attendanceComplete: sheetDraft.attendanceComplete,
  commissionComplete: sheetDraft.commissionComplete,
  totalGrossAmount: sheetDraft.totalGrossAmount,
  totalDeductionAmount: sheetDraft.totalDeductionAmount,
  totalNetAmount: sheetDraft.totalNetAmount
}

const sheetConfirmed = await post('payroll/sheet/update', { sheetId: sheetDraft.sheetId }, tokens.boss)
results.sheetConfirmed = { id: sheetConfirmed.sheetId, status: sheetConfirmed.status, totalNetAmount: sheetConfirmed.totalNetAmount }

const payablePage = await post('finance/payment/employee/page', { current: 1, size: 100 }, tokens.finance)
const payables = (payablePage.records || payablePage.content || []).filter(item => String(item.payeeEmployee?.empId) === EMP_ID)
if (!payables.length) throw new Error('未找到员工待发应付')
const outstandingAmount = payables.reduce((sum, item) => sum + Number(item.amount) - Number(item.paidAmount || 0), 0)
results.employeePayable = {
  ids: payables.map(item => item.payableId),
  types: payables.map(item => item.type),
  outstandingAmount
}

const paymentKey = `payroll-e2e-${sheetDraft.sheetId}-${Date.now()}`
const paymentPayload = {
  payableIds: payables.map(item => item.payableId),
  amount: outstandingAmount,
  account: 'CompanyBank',
  paidAt: '2026-10-07',
  batchNo: `PAYROLL-${PERIOD}`,
  idempotencyKey: paymentKey,
  remark: '员工薪酬P0本地测试发放'
}
const payment = await post('finance/payment/employee/save', paymentPayload, tokens.finance)
results.payment = { id: payment.paymentId, amount: payment.amount, payeeEmployee: payment.payeeEmployee?.name, batchNo: payment.batchNo }
const paymentRetry = await post('finance/payment/employee/save', paymentPayload, tokens.finance)
results.paymentRetry = { id: paymentRetry.paymentId, sameId: String(paymentRetry.paymentId) === String(payment.paymentId) }

const employeePayablesAfter = await post('finance/payment/employee/page', { current: 1, size: 20 }, tokens.finance)
results.employeePayableAfter = (employeePayablesAfter.records || employeePayablesAfter.content || []).length

const myPage = await post('payroll/payslip/my/page', { current: 1, size: 20 }, tokens.employee)
const mySlip = (myPage.records || myPage.content || [])[0]
if (!mySlip) throw new Error('员工未看到已发放工资条')
results.employeePayslip = {
  id: mySlip.payslipId,
  period: mySlip.period,
  status: mySlip.status,
  publishedAt: mySlip.publishedAt,
  netAmount: mySlip.netAmount
}

const storePage = await post('payroll/store-payroll/page', { current: 1, size: 20 }, tokens.manager)
const storeRows = storePage.records || storePage.content || []
results.storePayroll = { count: storeRows.length, firstEmployee: storeRows[0]?.employee?.name, firstNet: storeRows[0]?.netAmount }

const financeStoreDenied = await fetch(API + 'payroll/store-payroll/page', {
  method: 'POST', headers: { 'content-type': 'application/json', token: tokens.finance }, body: JSON.stringify({ current: 1, size: 20 })
}).then(r => r.json())
results.financeStoreNegative = { success: financeStoreDenied.success, code: financeStoreDenied.errorCode?.code }

const employeeWorkbenchDenied = await fetch(API + 'payroll/workbench/report', {
  method: 'POST', headers: { 'content-type': 'application/json', token: tokens.employee }, body: JSON.stringify({ period: PERIOD })
}).then(r => r.json())
results.employeeWorkbenchNegative = { success: employeeWorkbenchDenied.success, code: employeeWorkbenchDenied.errorCode?.code }

const sheetPage = await post('payroll/sheet/page', { current: 1, size: 20 }, tokens.boss)
const bossSheet = (sheetPage.records || sheetPage.content || [])[0]
results.bossSheet = { id: bossSheet.sheetId, status: bossSheet.status, totalNetAmount: bossSheet.totalNetAmount }

const evidence = {
  generatedAt: new Date().toISOString(),
  tenant: '昭青',
  period: PERIOD,
  employeeId: EMP_ID,
  results
}
fs.writeFileSync(path.join(RUN, 'raw/api/payroll-e2e.json'), JSON.stringify(evidence, null, 2))
console.log(JSON.stringify(evidence, null, 2))

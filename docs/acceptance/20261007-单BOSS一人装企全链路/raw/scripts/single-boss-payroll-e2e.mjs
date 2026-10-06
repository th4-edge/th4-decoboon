import fs from 'node:fs'
import path from 'node:path'

const API = 'http://127.0.0.1:8090/api/'
const RUN = '/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/docs/acceptance/20261007-单BOSS一人装企全链路'
const PHONE = process.env.SINGLE_BOSS_PHONE || '18890000001'
const PASSWORD = process.env.SINGLE_BOSS_PASSWORD
const EMP_ID = '8900000000000000101'
const PERIOD = '2027-01'

if (!PASSWORD) throw new Error('缺少 SINGLE_BOSS_PASSWORD')

async function post(uri, data = {}, token = '') {
  const response = await fetch(API + uri, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { token } : {}) },
    body: JSON.stringify(data)
  })
  const body = await response.json().catch(() => ({}))
  if (!body.success) {
    throw new Error(`${uri}: ${body.errorCode?.message || response.status}`)
  }
  return body.data
}

const { token } = await post('auth/session/password-login', { phone: PHONE, password: PASSWORD })
const session = await post('auth/session/info', {}, token)
const results = {
  session: {
    employeeId: String(session.employee?.empId),
    employeeName: session.employee?.name,
    role: session.employee?.role,
    companyId: String(session.employee?.company?.comId),
    companyName: session.employee?.company?.name,
    dataScope: session.dataScope
  }
}

const employeePage = await post('payroll/employee/page', { current: 1, size: 20 }, token)
const employees = employeePage.records || employeePage.content || []
results.employeeCount = employees.length
if (employees.length !== 1 || String(employees[0]?.empId) !== EMP_ID) {
  throw new Error(`单 BOSS 企业员工数异常: ${employees.length}`)
}

const compensation = await post('payroll/compensation/save', {
  empId: EMP_ID,
  baseSalary: 6000,
  effectiveFrom: '2027-01-01',
  remark: '单BOSS一人装企工资E2E'
}, token)
results.compensation = {
  schemeId: compensation.schemeId,
  employee: compensation.employee?.name,
  baseSalary: compensation.baseSalary,
  status: compensation.status,
  createdBy: compensation.createdBy?.name
}

const attendance = await post('attendance/summary/save', {
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
  remark: '单BOSS一人装企考勤E2E'
}, token)
const attendanceConfirmed = await post('attendance/summary/update', {
  summaryId: attendance.summaryId,
  status: 'Confirmed'
}, token)
results.attendance = {
  summaryId: attendanceConfirmed.summaryId,
  status: attendanceConfirmed.status,
  createdBy: attendanceConfirmed.createdBy?.name,
  confirmedBy: attendanceConfirmed.confirmedBy?.name,
  confirmedAt: attendanceConfirmed.confirmedAt
}

const sheet = await post('payroll/sheet/build', { period: PERIOD }, token)
results.sheetBuilt = {
  sheetId: sheet.sheetId,
  status: sheet.status,
  attendanceComplete: sheet.attendanceComplete,
  commissionComplete: sheet.commissionComplete,
  totalGrossAmount: sheet.totalGrossAmount,
  totalDeductionAmount: sheet.totalDeductionAmount,
  totalNetAmount: sheet.totalNetAmount,
  createdBy: sheet.createdBy?.name
}

const sheetConfirmed = await post('payroll/sheet/update', { sheetId: sheet.sheetId }, token)
results.sheetConfirmed = {
  sheetId: sheetConfirmed.sheetId,
  status: sheetConfirmed.status,
  totalNetAmount: sheetConfirmed.totalNetAmount,
  confirmedBy: sheetConfirmed.confirmedBy?.name,
  confirmedAt: sheetConfirmed.confirmedAt
}

const payablePage = await post('finance/payment/employee/page', { current: 1, size: 20 }, token)
const payables = (payablePage.records || payablePage.content || [])
  .filter(item => String(item.payeeEmployee?.empId) === EMP_ID)
if (payables.length !== 1) throw new Error(`单 BOSS 应付数量异常: ${payables.length}`)
const payable = payables[0]
const outstanding = Number(payable.amount) - Number(payable.paidAmount || 0)
results.payable = {
  payableId: payable.payableId,
  type: payable.type,
  amount: payable.amount,
  paidAmount: payable.paidAmount,
  outstanding,
  payeeEmployee: payable.payeeEmployee?.name
}

const idempotencyKey = `single-boss-payroll-${sheet.sheetId}`
const paymentPayload = {
  payableIds: [payable.payableId],
  amount: outstanding,
  account: 'CompanyBank',
  paidAt: '2026-10-07',
  batchNo: `SINGLE-BOSS-${PERIOD}`,
  idempotencyKey,
  remark: '单BOSS一人装企发放E2E'
}
const payment = await post('finance/payment/employee/save', paymentPayload, token)
const paymentRetry = await post('finance/payment/employee/save', paymentPayload, token)
results.payment = {
  paymentId: payment.paymentId,
  amount: payment.amount,
  payeeEmployee: payment.payeeEmployee?.name,
  batchNo: payment.batchNo,
  creator: payment.creator?.name,
  owner: payment.owner?.name
}
results.paymentRetry = {
  paymentId: paymentRetry.paymentId,
  samePayment: String(paymentRetry.paymentId) === String(payment.paymentId)
}

const payslipPage = await post('payroll/payslip/my/page', { current: 1, size: 20 }, token)
const payslip = (payslipPage.records || payslipPage.content || []).find(item => item.period === PERIOD)
if (!payslip) throw new Error('BOSS 未看到已发布工资条')
results.payslip = {
  payslipId: payslip.payslipId,
  period: payslip.period,
  status: payslip.status,
  netAmount: payslip.netAmount,
  publishedAt: payslip.publishedAt
}

const sheetInfo = await post('payroll/sheet/info', { sheetId: sheet.sheetId }, token)
results.sheetFinal = {
  sheetId: sheetInfo.sheetId,
  status: sheetInfo.status,
  totalNetAmount: sheetInfo.totalNetAmount
}

const evidence = {
  generatedAt: new Date().toISOString(),
  tenant: '单BOSS验收企业',
  period: PERIOD,
  employeeId: EMP_ID,
  results
}
fs.writeFileSync(path.join(RUN, 'raw/api/single-boss-payroll-e2e.json'), JSON.stringify(evidence, null, 2))
console.log(JSON.stringify(evidence, null, 2))

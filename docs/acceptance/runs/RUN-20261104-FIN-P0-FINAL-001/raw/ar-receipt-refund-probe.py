#!/usr/bin/env python3
import json, os, subprocess
BASE='http://127.0.0.1:8090/api'; PASSWORD=os.environ['TH4_TEST_PASSWORD']
FIN='13910002003'; PROJECT='242923438631550976'; RECEIVABLE='242923439214559232'
def post(route,body=None,token=None):
 cmd=['curl','-fsS','-X','POST',f'{BASE}/{route}','-H','Content-Type: application/json']
 if token: cmd+=['-H',f'token: {token}']
 cmd+=['--data-binary',json.dumps(body or {},ensure_ascii=False)]
 return json.loads(subprocess.check_output(cmd,text=True,timeout=20))
def login(phone): return post('auth/session/password-login',{'phone':phone,'password':PASSWORD})['data']['token']
def db(sql):
 env=dict(os.environ,MYSQL_PWD='mysqladmin')
 out=subprocess.check_output(['mysql','-h','127.0.0.1','-P','3306','-u','root','-D','decoboon','--default-character-set=utf8mb4','-N','-B','-e',sql],env=env,text=True)
 return [x.split('\t') for x in out.splitlines() if x]
t=login(FIN)
initial=db(f"SELECT amount,received_amount,status FROM db_finance_receivable WHERE receivable_id='{RECEIVABLE}'")[0]
r1=post('finance/receipt/save',{'projectId':PROJECT,'receivableId':RECEIVABLE,'amount':100,'account':'CompanyBank','receivedAt':'2026-11-04','payerName':'FIN-P0-003','remark':'AR idempotency','idempotencyKey':'FIN-P0-003-RCPT-001'},t); assert r1.get('success'),r1
r1dup=post('finance/receipt/save',{'projectId':PROJECT,'receivableId':RECEIVABLE,'amount':100,'account':'CompanyBank','receivedAt':'2026-11-04','payerName':'FIN-P0-003','remark':'AR idempotency','idempotencyKey':'FIN-P0-003-RCPT-001'},t); assert r1dup.get('success'),r1dup
receipt1=r1['data']['receiptId']
refund=post('finance/refund/save',{'receiptId':receipt1,'amount':40,'account':'CompanyBank','refundAt':'2026-11-04','reason':'FIN-P0-003 refund','idempotencyKey':'FIN-P0-003-REFUND-001'},t); assert refund.get('success'),refund
r2=post('finance/receipt/save',{'projectId':PROJECT,'receivableId':RECEIVABLE,'amount':50,'account':'CompanyBank','receivedAt':'2026-11-04','payerName':'FIN-P0-003','remark':'AR reversal','idempotencyKey':'FIN-P0-003-RCPT-002'},t); assert r2.get('success'),r2
receipt2=r2['data']['receiptId']
rev=post('finance/receipt/reverse',{'receiptId':receipt2,'reasonType':'Duplicate','reason':'FIN-P0-003 duplicate reversal','reversalAt':'2026-11-04','idempotencyKey':'FIN-P0-003-REV-001'},t); assert rev.get('success'),rev
final=db(f"SELECT amount,received_amount,status FROM db_finance_receivable WHERE receivable_id='{RECEIVABLE}'")[0]
receipts=db(f"SELECT receipt_id,amount,status FROM db_finance_receipt WHERE receivable_id='{RECEIVABLE}' AND payer_name='FIN-P0-003' ORDER BY receipt_id")
refunds=db(f"SELECT refund_id,receipt_id,amount FROM db_finance_refund WHERE receipt_id IN (SELECT receipt_id FROM db_finance_receipt WHERE receivable_id='{RECEIVABLE}' AND payer_name='FIN-P0-003') ORDER BY refund_id")
ledgers=db(f"SELECT ledger_id,source,source_id,amount,direction,voided FROM db_finance_ledger WHERE com_id=199500000000000001 AND source IN ('Receipt','ReceiptReversal','Refund') AND source_id IN (SELECT receipt_id FROM db_finance_receipt WHERE receivable_id='{RECEIVABLE}' AND payer_name='FIN-P0-003' UNION SELECT refund_id FROM db_finance_refund WHERE receipt_id IN (SELECT receipt_id FROM db_finance_receipt WHERE receivable_id='{RECEIVABLE}' AND payer_name='FIN-P0-003')) ORDER BY ledger_id")
report=post('finance/receipt/report',{'projectId':PROJECT},t)
refund_report=post('finance/refund/report',{'projectId':PROJECT},t)
print(json.dumps({'initial':initial,'receipt1':r1,'receipt1Duplicate':r1dup,'refund':refund,'receipt2':r2,'reversal':rev,'final':final,'receipts':receipts,'refunds':refunds,'ledgers':ledgers,'receiptReport':report,'refundReport':refund_report},ensure_ascii=False,indent=2))

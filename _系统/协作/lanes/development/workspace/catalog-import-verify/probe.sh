set -u
API=http://127.0.0.1:8090/api
DIR="$(cd "$(dirname "$0")" && pwd)"
login() { curl -s -X POST "$API/auth/session/password-login" -H 'Content-Type: application/json' -d "{\"phone\":\"$1\",\"password\":\"Maihong2026\"}" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])"; }
json() { python3 -c "import sys,json;r=json.load(sys.stdin);print('$1='+str(r.get('data',{}).get('$1'))+' status='+str((r.get('data') or {}).get('status')))"; }
BOSS=$(login 19900000001); FIN=$(login 19900000002)
echo "== P1 模板下载（BOSS）=="
curl -s -X POST "$API/catalog/import-template/build" -H 'Content-Type: application/json' -H "token: $BOSS" -d '{}' > tpl.json
python3 -c "
import json;s=json.load(open('tpl.json'))['data']
print(' name=',s.get('name'),'size=',s.get('size'),'path=',s.get('path'))
open('tpl.url','w').write(s.get('url',''))"
TPL_URL=$(cat tpl.url); curl -s "http://127.0.0.1:8090$TPL_URL" -o downloaded-template.xlsx; shasum -a 256 downloaded-template.xlsx | awk '{print " downloaded sha256="$1}'
echo "== P2 非法表头阻断（不创建批次）=="
curl -s -X POST "$API/catalog/import-batch/build" -H "token: $BOSS" -F "file=@$DIR/bad-header.xlsx" | head -c 300; echo
echo "== P3 非 BOSS 发起上传（期望 403）=="
curl -s -X POST "$API/catalog/import-batch/build" -H "token: $FIN" -F "file=@$DIR/clean-550.xlsx" | head -c 200; echo
echo "== P4 550 条真实模板预览 =="
curl -s -X POST "$API/catalog/import-batch/build" -H "token: $BOSS" -F "file=@$DIR/clean-550.xlsx" > clean1.json
python3 -c "
import json;d=json.load(open('clean1.json'))['data']
print(' batchId=',d['batchId'],'batchNo=',d['batchNo'],'status=',d['status']['key'],'total=',d['totalRows'],'ready=',d['readyRows'],'dup=',d['duplicateRows'],'susp=',d['suspectedDuplicateRows'],'error=',d['errorRows'],'costPending=',d['costPendingRows'])
open('clean.batch','w').write(str(d['batchId']))"
CLEAN=$(cat clean.batch)
echo "== P5 重复上传同一文件（指纹命中，返回原批次）=="
curl -s -X POST "$API/catalog/import-batch/build" -H "token: $BOSS" -F "file=@$DIR/clean-550.xlsx" | python3 -c "
import sys,json;d=json.load(sys.stdin)['data'];print(' batchId=',d['batchId'],'status=',d['status']['key'],'message=',d.get('message'))"
echo "== P6 确认导入 550 条（整批）=="
curl -s -X POST "$API/catalog/import-batch/save" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$CLEAN}" | python3 -c "
import sys,json;d=json.load(sys.stdin)['data'];print(' status=',d['status']['key'],'imported=',d['importedRows'],'ready=',d['readyRows'],'finished=',d['finishedAt'][:19])"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat(' catalog_items_with_batch=',count(*)) from catalog_item where import_batch_id=$CLEAN;" 2>/dev/null
echo "== P7 重复确认（幂等，不新增）=="
curl -s -X POST "$API/catalog/import-batch/save" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$CLEAN}" | python3 -c "
import sys,json;d=json.load(sys.stdin)['data'];print(' status=',d['status']['key'],'imported=',d['importedRows'])"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat(' catalog_items_with_batch=',count(*)) from catalog_item where import_batch_id=$CLEAN;" 2>/dev/null
echo "== P8 撤销批次（24h 内、无引用）=="
curl -s -X POST "$API/catalog/import-batch/update" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$CLEAN,\"status\":\"Revoked\"}" | python3 -c "
import sys,json;d=json.load(sys.stdin)['data'];print(' status=',d['status']['key'],'revokedAt=',d['revokedAt'][:19],'message=',d.get('message'))"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat(' catalog_items_with_batch=',count(*)) from catalog_item where import_batch_id=$CLEAN;" 2>/dev/null
echo "== P9 撤销后重新上传同一文件（新批次，不作为指纹命中）=="
curl -s -X POST "$API/catalog/import-batch/build" -H "token: $BOSS" -F "file=@$DIR/clean-550.xlsx" > clean2.json
python3 -c "
import json;d=json.load(open('clean2.json'))['data'];print(' batchId=',d['batchId'],'(与已撤销批次不同=',str(d['batchId'])!='$CLEAN','）status=',d['status']['key'],'ready=',d['readyRows'])"
echo "== P10 混合文件预览（重复/错误/成本待维护/固定总价）=="
curl -s -X POST "$API/catalog/import-batch/build" -H "token: $BOSS" -F "file=@$DIR/mixed.xlsx" > mixed.json
python3 -c "
import json;d=json.load(open('mixed.json'))['data']
print(' batchId=',d['batchId'],'total=',d['totalRows'],'ready=',d['readyRows'],'dup=',d['duplicateRows'],'susp=',d['suspectedDuplicateRows'],'error=',d['errorRows'],'costPending=',d['costPendingRows'])
open('mixed.batch','w').write(str(d['batchId']))"
MIXED=$(cat mixed.batch)
echo "== P11 有错误行时整批导入被拒绝 =="
curl -s -X POST "$API/catalog/import-batch/save" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$MIXED}" | head -c 240; echo
echo "== P12 二次确认仅导入合格行 =="
curl -s -X POST "$API/catalog/import-batch/save" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$MIXED,\"partialImport\":true}" | python3 -c "
import sys,json;d=json.load(sys.stdin)['data'];print(' status=',d['status']['key'],'imported=',d['importedRows'],'skipped=',d['skippedRows'],'partialImport=',d['partialImport'])"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat(' ',code,' cost=',ifnull(cost_price,'NULL'),' fixed=',ifnull(fixed_total_price,'NULL'),' material=',material_price,' manual=',manual_price) from catalog_item where import_batch_id=$MIXED order by code;" 2>/dev/null
echo "== P13 错误清单分页（字段+错误码+原因）=="
curl -s -X POST "$API/catalog/import-row/page" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$MIXED,\"status\":\"Error\",\"current\":1,\"size\":10}" | python3 -c "
import sys,json;d=json.load(sys.stdin)['data'];print(' total=',d['totalElements'])
[print('  行',r['rowNo'],r['errorField'],r['errorCode'],r['errorReason']) for r in d['content']]"
echo "== P14 撤销部分导入批次 =="
curl -s -X POST "$API/catalog/import-batch/update" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$MIXED,\"status\":\"Revoked\"}" | python3 -c "
import sys,json;d=json.load(sys.stdin)['data'];print(' status=',d['status']['key'])"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat(' catalog_items_with_batch=',count(*)) from catalog_item where import_batch_id=$MIXED;" 2>/dev/null
echo "== P15 批次列表（最近记录）=="
curl -s -X POST "$API/catalog/import-batch/page" -H 'Content-Type: application/json' -H "token: $BOSS" -d '{"current":1,"size":3}' | python3 -c "
import sys,json;d=json.load(sys.stdin)['data']
print(' total=',d['totalElements']);[print('  ',b['batchNo'],b['status']['key'],b['importedRows'],b['errorRows']) for b in d['content']]"

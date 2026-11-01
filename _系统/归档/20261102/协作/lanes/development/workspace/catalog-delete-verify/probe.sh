set -u
API=http://127.0.0.1:8090/api
DIR=/Users/zhangwei/FutureFiles/安徽昭青/家装装修/装客宝/后端代码/th4-decoboon/_系统/协作/lanes/development/workspace/catalog-import-verify
BOSS=$(curl -s -X POST "$API/auth/session/password-login" -H 'Content-Type: application/json' -d '{"phone":"19900000001","password":"Maihong2026"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
echo "== D1 新建无引用项目 =="
NEW=$(curl -s -X POST "$API/catalog/item/save" -H 'Content-Type: application/json' -H "token: $BOSS" -d '{"type":"General","category":"删除验证","name":"删除验证项目A","unit":"Item","pricingMode":"ManualOnly","manualPrice":1,"costPrice":null}' | python3 -c "import sys,json;d=json.load(sys.stdin)['data'];print(d['itemId'])")
echo " itemId=$NEW"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat('  db_before=',count(*)) from catalog_item where item_id=$NEW;" 2>/dev/null
echo "== D2 无引用删除成功 =="
curl -s -X POST "$API/catalog/item/delete" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"itemId\":$NEW}" | python3 -c "import sys,json;r=json.load(sys.stdin);print('  success=',r.get('success'),'name=',(r.get('data') or {}).get('name'))"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat('  db_after=',count(*)) from catalog_item where item_id=$NEW; select concat('  detail_after=',count(*)) from catalog_general_detail where item_id=$NEW;" 2>/dev/null
echo "== D3 重复删除稳定失败 =="
curl -s -X POST "$API/catalog/item/delete" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"itemId\":$NEW}" | head -c 200; echo
echo "== D4 被设计/报价引用的项目禁止删除 =="
curl -s -X POST "$API/catalog/item/delete" -H 'Content-Type: application/json' -H "token: $BOSS" -d '{"itemId":228668580093231104}' | head -c 300; echo
echo "== D5 不存在/跨企业项目统一 404 =="
curl -s -X POST "$API/catalog/item/delete" -H 'Content-Type: application/json' -H "token: $BOSS" -d '{"itemId":999999999999}' | head -c 200; echo
echo "== D6 导入批次行关联的受控解除 =="
curl -s -X POST "$API/catalog/import-batch/build" -H "token: $BOSS" -F "file=@$DIR/mixed.xlsx" -F "fileName=mixed.xlsx" > del-mixed.json
MB=$(python3 -c "import json;d=json.load(open('del-mixed.json'))['data'];print(d['batchId'])")
curl -s -X POST "$API/catalog/import-batch/save" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$MB,\"partialImport\":true}" >/dev/null
ITEM=$(mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select item_id from catalog_item where code='IMP-MIX-0001' and com_id=1790000000000000001 limit 1;" 2>/dev/null)
echo " imported item=$ITEM batch=$MB"
curl -s -X POST "$API/catalog/item/delete" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"itemId\":$ITEM}" | python3 -c "import sys,json;r=json.load(sys.stdin);print('  delete success=',r.get('success'))"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat('  row_item_null=',count(*),' reason=',min(coalesce(error_code,'-'))) from catalog_import_row where batch_id=$MB and item_id is null and error_code='DELETED'; select concat('  batch_rows_linked=',count(*)) from catalog_import_row where batch_id=$MB and item_id is not null;" 2>/dev/null
echo "== D7 撤销剩余批次清理 =="
curl -s -X POST "$API/catalog/import-batch/update" -H 'Content-Type: application/json' -H "token: $BOSS" -d "{\"batchId\":$MB,\"status\":\"Revoked\"}" | python3 -c "import sys,json;r=json.load(sys.stdin);print('  revoke success=',r.get('success'))"
mysql -h127.0.0.1 -uroot -pmysqladmin decoboon -N -B -e "select concat('  catalog_after_cleanup=',count(*)) from catalog_item where com_id=1790000000000000001;" 2>/dev/null

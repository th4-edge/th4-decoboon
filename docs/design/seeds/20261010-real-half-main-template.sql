-- Real half-package + main-material design template seed (dev/demo only).
-- Source: 半+主分项报价.xls; catalog references are existing company catalog items.
-- Re-runnable: deletes and recreates only the deterministic seed ids below.
start transaction;
delete from design_template_item where com_id=1790000000000000001 and version_id=234500000000000002;
delete from design_template_room where com_id=1790000000000000001 and version_id=234500000000000002;
delete from design_template_version where com_id=1790000000000000001 and version_id=234500000000000002;
delete from design_template where com_id=1790000000000000001 and template_id=234500000000000001;
insert into design_template (template_id,com_id,store_id,owner_id,scope,status,current_version_id,source_template_id,remark) values (234500000000000001,1790000000000000001,null,1790000000000000101,'Company','Draft',null,null,'真实半包+主材报价模板，来源 Excel 导入');
insert into design_template_version (version_id,com_id,template_id,version,status,name,house_type,area_min,area_max,tags,published_at,published_by) values (234500000000000002,1790000000000000001,234500000000000001,1,'Published','半包+主材 · 四室两厅标准模板（真实报价导入）','4室2厅1厨2卫',120,180,'半包,主材,简约,标准',now(),1790000000000000101);
update design_template set current_version_id=234500000000000002, status='Published' where template_id=234500000000000001;
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001000,1790000000000000001,234500000000000002,'living_dining','客厅、餐厅、过道','LivingRoom',10,1);
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001001,1790000000000000001,234500000000000002,'master_bed','主卧室','Bedroom',20,1);
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001002,1790000000000000001,234500000000000002,'bedroom_2','次卧1','Bedroom',30,1);
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001003,1790000000000000001,234500000000000002,'bedroom_3','次卧2','Bedroom',40,1);
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001004,1790000000000000001,234500000000000002,'bedroom_4','次卧3','Bedroom',50,1);
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001005,1790000000000000001,234500000000000002,'kitchen','厨房','Kitchen',60,1);
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001006,1790000000000000001,234500000000000002,'bath_public','公共卫生间','Bathroom',70,1);
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001007,1790000000000000001,234500000000000002,'bath_master','主卧卫生间','Bathroom',80,1);
insert into design_template_room (room_id,com_id,version_id,room_key,name,room_type,sort_no,room_count) values (234500000000001008,1790000000000000001,234500000000000002,'balcony','生活阳台','Balcony',90,1);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010000,1790000000000000001,234500000000000002,null,228668579988373504,null,1,1,'1.甲方自理');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010001,1790000000000000001,234500000000000002,null,228668580214865920,null,0,2,'1.人工费+辅材料费
2.水泥沙浆找平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010002,1790000000000000001,234500000000000002,null,228668580281974784,null,0,3,'1.人工费+辅材料费
2.水泥沙浆找平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010003,1790000000000000001,234500000000000002,null,228668583452868608,null,1,4,'1.75型轻钢龙骨+木方加固，泰山纸面石膏板厚0.9cm，专用防锈自攻螺钉，钉眼涂防锈漆,专用补缝腻子嵌平，再贴防裂绷带
2.竖龙骨中心距≦400mm');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010004,1790000000000000001,234500000000000002,null,228668583482228736,null,1,5,'1.75型轻钢龙骨+木方加固，泰山纸面石膏板厚0.9cm，专用防锈自攻螺钉，钉眼涂防锈漆,专用补缝腻子嵌平，再贴防裂绷带
2.竖龙骨中心距≦400mm');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010005,1790000000000000001,234500000000000002,null,228668583511588864,null,1,6,'1.75型轻钢龙骨+木方加固，泰山纸面石膏板厚0.9cm，专用防锈自攻螺钉，钉眼涂防锈漆,专用补缝腻子嵌平，再贴防裂绷带
2.竖龙骨中心距≦400mm');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010006,1790000000000000001,234500000000000002,null,228668583591280640,null,1,7,'1.轻钢龙骨，泰山纸面石膏板厚0.9cm');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010007,1790000000000000001,234500000000000002,null,228668583616446464,null,1,8,'1.轻钢龙骨，泰山纸面石膏板厚0.9cm');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010008,1790000000000000001,234500000000000002,null,228668583654195200,null,1,9,'1.轻钢龙骨，泰山纸面石膏板厚0.9cm');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010009,1790000000000000001,234500000000000002,null,233689437005938688,null,0,10,'1.红砖砌墙，水泥/黄沙双面粉平
2.不足1平方按1平方计算');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010010,1790000000000000001,234500000000000002,null,228668581565431808,null,0,11,'1.红砖砖砌墙，水泥/黄沙双面粉平。
2.不足1平方按1平方计算');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010011,1790000000000000001,234500000000000002,null,228668580122591232,null,1,12,'1.人工费+辅材料费
2.水泥沙浆找平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010012,1790000000000000001,234500000000000002,null,228668580160339968,null,1,13,'1.人工费+辅材料费
2.水泥沙浆找平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010013,1790000000000000001,234500000000000002,'living_dining',228668585168338944,'=WP',0,14,'1.墙面批刮倍得丽找平石膏+纤维网，做基层处理。
2.墙顶面误差大于2公分需二次找平并挂网或墙顶面墙体质量差须满挂网，费用另加3元每平。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010014,1790000000000000001,234500000000000002,'living_dining',228668585189310464,'=WP',0,15,'1.批刮倍得丽腻子二至三遍并打磨平整，喷刷面漆二遍。
2.墙/顶面乳胶漆为白色，彩色乳胶漆与墙纸费用另计。                                                                      
3.不含原有墙面铲除。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010015,1790000000000000001,234500000000000002,'living_dining',228668585214476288,'=WP',0,16,'1.三棵树系列，/如需加底漆加900元/户                                                                                                                                                                                                                           ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010016,1790000000000000001,234500000000000002,'living_dining',228668582714671104,'=A',0,17,'1.纯轻钢龙骨吊平顶，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010017,1790000000000000001,234500000000000002,'living_dining',228668582769197056,'=A',0,18,'1.轻钢龙骨+3*4木方加固，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010018,1790000000000000001,234500000000000002,'living_dining',228668582827917312,'=A',0,19,'1.9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010019,1790000000000000001,234500000000000002,'living_dining',228668590272806912,null,0,20,'1.石膏线规格6公分-9公分以内，周长绕边一圈。
2.异性结构另计（如：不规则形状）正常拐圆加60/个 /  拐圆加耳朵75/个。
3.异性结构另计（如：圆）正常圆弧另加60元/米，不足一米按150元/个。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010020,1790000000000000001,234500000000000002,'living_dining',228668583113129984,null,0,21,'1.石膏板内嵌
2.下口发光需是底面双层石膏板');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010021,1790000000000000001,234500000000000002,'living_dining',228668600569823232,null,1,22,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010022,1790000000000000001,234500000000000002,'living_dining',228668583150878720,null,0,23,'1.木工板基层，外贴石膏板，专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010023,1790000000000000001,234500000000000002,'living_dining',228668584371421184,null,1,24,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010024,1790000000000000001,234500000000000002,'living_dining',228668584409169920,null,1,25,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010025,1790000000000000001,234500000000000002,'living_dining',228668581108252672,'=A',0,26,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010026,1790000000000000001,234500000000000002,'living_dining',233689436464873472,'=A',0,27,'1.工程量按展开面积计算。                                                                                                                                                       2.表面平整.压实.收光。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010027,1790000000000000001,234500000000000002,'living_dining',228668582244909056,'=P-DW',0,28,'1.墙面清理扫净,用32.5#水泥砂浆铺贴。
2.人工+辅材
3.主材另计。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010028,1790000000000000001,234500000000000002,'master_bed',228668585168338944,'=WP',0,29,'1.墙面批刮倍得丽找平石膏+纤维网，做基层处理。
2.墙顶面误差大于2公分需二次找平并挂网或墙顶面墙体质量差须满挂网，费用另加3元每平。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010029,1790000000000000001,234500000000000002,'master_bed',228668585189310464,'=WP',0,30,'1.批刮倍得丽腻子二至三遍并打磨平整，喷刷面漆二遍。
2.墙/顶面乳胶漆为白色，彩色乳胶漆与墙纸费用另计。                                                                      
3.不含原有墙面铲除。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010030,1790000000000000001,234500000000000002,'master_bed',228668585214476288,'=WP',0,31,'1.三棵树系列，/如需加底漆加900元/户                                                                                                                                                                                                                           ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010031,1790000000000000001,234500000000000002,'master_bed',228668582714671104,'=A',0,32,'1.纯轻钢龙骨吊平顶，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010032,1790000000000000001,234500000000000002,'master_bed',228668582769197056,'=A',0,33,'1.轻钢龙骨+3*4木方加固，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010033,1790000000000000001,234500000000000002,'master_bed',228668582827917312,'=A',0,34,'1.9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010034,1790000000000000001,234500000000000002,'master_bed',228668590272806912,null,0,35,'1.石膏线规格6公分-9公分以内，周长绕边一圈。
2.异性结构另计（如：不规则形状）正常拐圆加60/个 /  拐圆加耳朵75/个。
3.异性结构另计（如：圆）正常圆弧另加60元/米，不足一米按150元/个。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010035,1790000000000000001,234500000000000002,'master_bed',228668583150878720,null,0,36,'1.木工板基层，外贴石膏板，专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010036,1790000000000000001,234500000000000002,'master_bed',228668584371421184,null,1,37,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010037,1790000000000000001,234500000000000002,'master_bed',228668584409169920,null,1,38,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010038,1790000000000000001,234500000000000002,'master_bed',228668581108252672,'=A',0,39,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010039,1790000000000000001,234500000000000002,'master_bed',233689436464873472,'=A',0,40,'1.工程量按展开面积计算。                                                                                                                                                       2.表面平整.压实.收光。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010040,1790000000000000001,234500000000000002,'master_bed',228668582244909056,'=P-DW',0,41,'1.墙面清理扫净,用32.5#水泥砂浆铺贴。
2.人工+辅材
3.主材另计。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010041,1790000000000000001,234500000000000002,'bedroom_2',228668585168338944,'=WP',0,42,'1.墙面批刮倍得丽找平石膏+纤维网，做基层处理。
2.墙顶面误差大于2公分需二次找平并挂网或墙顶面墙体质量差须满挂网，费用另加3元每平。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010042,1790000000000000001,234500000000000002,'bedroom_2',228668585189310464,'=WP',0,43,'1.批刮倍得丽腻子二至三遍并打磨平整，喷刷面漆二遍。
2.墙/顶面乳胶漆为白色，彩色乳胶漆与墙纸费用另计。                                                                      
3.不含原有墙面铲除。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010043,1790000000000000001,234500000000000002,'bedroom_2',228668585214476288,'=WP',0,44,'1.三棵树系列，/如需加底漆加900元/户                                                                                                                                                                                                                           ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010044,1790000000000000001,234500000000000002,'bedroom_2',228668582714671104,'=A',0,45,'1.纯轻钢龙骨吊平顶，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010045,1790000000000000001,234500000000000002,'bedroom_2',228668582769197056,'=A',0,46,'1.轻钢龙骨+3*4木方加固，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010046,1790000000000000001,234500000000000002,'bedroom_2',228668582827917312,'=A',0,47,'1.9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010047,1790000000000000001,234500000000000002,'bedroom_2',228668590272806912,null,0,48,'1.石膏线规格6公分-9公分以内，周长绕边一圈。
2.异性结构另计（如：不规则形状）正常拐圆加60/个 /  拐圆加耳朵75/个。
3.异性结构另计（如：圆）正常圆弧另加60元/米，不足一米按150元/个。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010048,1790000000000000001,234500000000000002,'bedroom_2',228668583113129984,null,0,49,'1.石膏板内嵌
2.下口发光需是底面双层石膏板');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010049,1790000000000000001,234500000000000002,'bedroom_2',228668600569823232,null,1,50,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010050,1790000000000000001,234500000000000002,'bedroom_2',228668583150878720,null,0,51,'1.木工板基层，外贴石膏板，专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010051,1790000000000000001,234500000000000002,'bedroom_2',228668584371421184,null,1,52,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010052,1790000000000000001,234500000000000002,'bedroom_2',228668584409169920,null,1,53,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010053,1790000000000000001,234500000000000002,'bedroom_2',228668581108252672,'=A',0,54,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010054,1790000000000000001,234500000000000002,'bedroom_2',233689436464873472,'=A',0,55,'1.工程量按展开面积计算。                                                                                                                                                       2.表面平整.压实.收光。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010055,1790000000000000001,234500000000000002,'bedroom_2',228668582244909056,'=P-DW',0,56,'1.墙面清理扫净,用32.5#水泥砂浆铺贴。
2.人工+辅材
3.主材另计。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010056,1790000000000000001,234500000000000002,'bedroom_3',228668585168338944,'=WP',0,57,'1.墙面批刮倍得丽找平石膏+纤维网，做基层处理。
2.墙顶面误差大于2公分需二次找平并挂网或墙顶面墙体质量差须满挂网，费用另加3元每平。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010057,1790000000000000001,234500000000000002,'bedroom_3',228668585189310464,'=WP',0,58,'1.批刮倍得丽腻子二至三遍并打磨平整，喷刷面漆二遍。
2.墙/顶面乳胶漆为白色，彩色乳胶漆与墙纸费用另计。                                                                      
3.不含原有墙面铲除。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010058,1790000000000000001,234500000000000002,'bedroom_3',228668585214476288,'=WP',0,59,'1.三棵树系列，/如需加底漆加900元/户                                                                                                                                                                                                                           ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010059,1790000000000000001,234500000000000002,'bedroom_3',228668582714671104,'=A',0,60,'1.纯轻钢龙骨吊平顶，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010060,1790000000000000001,234500000000000002,'bedroom_3',228668582769197056,'=A',0,61,'1.轻钢龙骨+3*4木方加固，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010061,1790000000000000001,234500000000000002,'bedroom_3',228668582827917312,'=A',0,62,'1.9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010062,1790000000000000001,234500000000000002,'bedroom_3',228668590272806912,null,0,63,'1.石膏线规格6公分-9公分以内，周长绕边一圈。
2.异性结构另计（如：不规则形状）正常拐圆加60/个 /  拐圆加耳朵75/个。
3.异性结构另计（如：圆）正常圆弧另加60元/米，不足一米按150元/个。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010063,1790000000000000001,234500000000000002,'bedroom_3',228668583113129984,null,0,64,'1.石膏板内嵌
2.下口发光需是底面双层石膏板');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010064,1790000000000000001,234500000000000002,'bedroom_3',228668600569823232,null,1,65,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010065,1790000000000000001,234500000000000002,'bedroom_3',228668583150878720,null,0,66,'1.木工板基层，外贴石膏板，专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010066,1790000000000000001,234500000000000002,'bedroom_3',228668584371421184,null,1,67,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010067,1790000000000000001,234500000000000002,'bedroom_3',228668584409169920,null,1,68,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010068,1790000000000000001,234500000000000002,'bedroom_3',228668581108252672,'=A',0,69,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010069,1790000000000000001,234500000000000002,'bedroom_3',233689436464873472,'=A',0,70,'1.工程量按展开面积计算。                                                                                                                                                       2.表面平整.压实.收光。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010070,1790000000000000001,234500000000000002,'bedroom_3',228668582244909056,'=P-DW',0,71,'1.墙面清理扫净,用32.5#水泥砂浆铺贴。
2.人工+辅材
3.主材另计。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010071,1790000000000000001,234500000000000002,'bedroom_4',228668585168338944,'=WP',0,72,'1.墙面批刮倍得丽找平石膏+纤维网，做基层处理。
2.墙顶面误差大于2公分需二次找平并挂网或墙顶面墙体质量差须满挂网，费用另加3元每平。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010072,1790000000000000001,234500000000000002,'bedroom_4',228668585189310464,'=WP',0,73,'1.批刮倍得丽腻子二至三遍并打磨平整，喷刷面漆二遍。
2.墙/顶面乳胶漆为白色，彩色乳胶漆与墙纸费用另计。                                                                      
3.不含原有墙面铲除。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010073,1790000000000000001,234500000000000002,'bedroom_4',228668585214476288,'=WP',0,74,'1.三棵树系列，/如需加底漆加900元/户                                                                                                                                                                                                                           ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010074,1790000000000000001,234500000000000002,'bedroom_4',228668582714671104,'=A',0,75,'1.纯轻钢龙骨吊平顶，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010075,1790000000000000001,234500000000000002,'bedroom_4',228668582769197056,'=A',0,76,'1.轻钢龙骨+3*4木方加固，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010076,1790000000000000001,234500000000000002,'bedroom_4',228668582827917312,'=A',0,77,'1.9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010077,1790000000000000001,234500000000000002,'bedroom_4',228668590272806912,null,0,78,'1.石膏线规格6公分-9公分以内，周长绕边一圈。
2.异性结构另计（如：不规则形状）正常拐圆加60/个 /  拐圆加耳朵75/个。
3.异性结构另计（如：圆）正常圆弧另加60元/米，不足一米按150元/个。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010078,1790000000000000001,234500000000000002,'bedroom_4',228668583113129984,null,0,79,'1.石膏板内嵌
2.下口发光需是底面双层石膏板');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010079,1790000000000000001,234500000000000002,'bedroom_4',228668600569823232,null,1,80,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010080,1790000000000000001,234500000000000002,'bedroom_4',228668583150878720,null,0,81,'1.木工板基层，外贴石膏板，专用防锈自攻螺钉，钉眼涂防锈漆
2.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010081,1790000000000000001,234500000000000002,'bedroom_4',228668584371421184,null,1,82,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010082,1790000000000000001,234500000000000002,'bedroom_4',228668584409169920,null,1,83,'1.木工板基层加固。                                                                                                                                                                                                                                      ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010083,1790000000000000001,234500000000000002,'bedroom_4',228668581108252672,'=A',0,84,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010084,1790000000000000001,234500000000000002,'bedroom_4',233689436464873472,'=A',0,85,'1.工程量按展开面积计算。                                                                                                                                                       2.表面平整.压实.收光。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010085,1790000000000000001,234500000000000002,'bedroom_4',228668582244909056,'=P-DW',0,86,'1.墙面清理扫净,用32.5#水泥砂浆铺贴。
2.人工+辅材
3.主材另计。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010086,1790000000000000001,234500000000000002,'kitchen',228668581108252672,'=A',0,87,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010087,1790000000000000001,234500000000000002,'kitchen',228668581192138752,'=WP',0,88,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010088,1790000000000000001,234500000000000002,'kitchen',233689437391814656,null,0,89,'1.人工（校对，切割），切割片
2.项目范围：墙砖.水管.窗户碰角，阳台地砖卷边碰角，干切工艺。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010089,1790000000000000001,234500000000000002,'kitchen',228668581779341312,null,1,90,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010090,1790000000000000001,234500000000000002,'kitchen',233689437219848192,null,1,91,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010091,1790000000000000001,234500000000000002,'kitchen',233689437240819712,null,1,92,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010092,1790000000000000001,234500000000000002,'kitchen',233689437257596928,null,1,93,'1.下水横管隔音棉处理');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010093,1790000000000000001,234500000000000002,'kitchen',233689437295345664,null,0,94,'1.下水竖管隔音棉处理');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010094,1790000000000000001,234500000000000002,'kitchen',233689436758474752,null,1,95,'人工费');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010095,1790000000000000001,234500000000000002,'bath_public',228668581108252672,'=A',0,96,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010096,1790000000000000001,234500000000000002,'bath_public',228668581192138752,'=WP',0,97,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010097,1790000000000000001,234500000000000002,'bath_public',233689437391814656,null,0,98,'1.人工（校对，切割），切割片
2.项目范围：墙砖.水管.窗户碰角，阳台地砖卷边碰角，干切工艺。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010098,1790000000000000001,234500000000000002,'bath_public',233689437555392512,'=A',0,99,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010099,1790000000000000001,234500000000000002,'bath_public',233689437219848192,null,1,100,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010100,1790000000000000001,234500000000000002,'bath_public',233689437240819712,null,1,101,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010101,1790000000000000001,234500000000000002,'bath_public',233689437257596928,null,1,102,'1.下水横管隔音棉处理');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010102,1790000000000000001,234500000000000002,'bath_public',233689437295345664,null,0,103,'1.下水竖管隔音棉处理');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010103,1790000000000000001,234500000000000002,'bath_public',228668582513344512,null,1,104,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平
3.含外表面装饰人工费用');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010104,1790000000000000001,234500000000000002,'bath_public',228668581871616000,'=A',0,105,'1.清理基层，无漏刷和鼓泡
2.管槽.下水口.浴缸.淋浴房及反面墙有家具的面须重点防水。（卫生间淋浴区防水高度必须达到1.8米）
3.冷涂两遍,做24-48小时闭水试验');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010105,1790000000000000001,234500000000000002,'bath_public',233689436758474752,null,1,106,'人工费');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010106,1790000000000000001,234500000000000002,'bath_master',228668581108252672,'=A',0,107,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010107,1790000000000000001,234500000000000002,'bath_master',228668581192138752,'=WP',0,108,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010108,1790000000000000001,234500000000000002,'bath_master',233689437391814656,null,0,109,'1.人工（校对，切割），切割片
2.项目范围：墙砖.水管.窗户碰角，阳台地砖卷边碰角，干切工艺。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010109,1790000000000000001,234500000000000002,'bath_master',233689437555392512,'=A',0,110,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010110,1790000000000000001,234500000000000002,'bath_master',233689437219848192,null,1,111,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010111,1790000000000000001,234500000000000002,'bath_master',233689437240819712,null,1,112,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010112,1790000000000000001,234500000000000002,'bath_master',233689437257596928,null,1,113,'1.下水横管隔音棉处理');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010113,1790000000000000001,234500000000000002,'bath_master',233689437295345664,null,0,114,'1.下水竖管隔音棉处理');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010114,1790000000000000001,234500000000000002,'bath_master',228668582513344512,null,1,115,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平
3.含外表面装饰人工费用');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010115,1790000000000000001,234500000000000002,'bath_master',228668581871616000,'=A',0,116,'1.清理基层，无漏刷和鼓泡
2.管槽.下水口.浴缸.淋浴房及反面墙有家具的面须重点防水。（卫生间淋浴区防水高度必须达到1.8米）
3.冷涂两遍,做24-48小时闭水试验');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010116,1790000000000000001,234500000000000002,'bath_master',233689436758474752,null,1,117,'人工费');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010117,1790000000000000001,234500000000000002,'balcony',228668581108252672,'=A',0,118,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010118,1790000000000000001,234500000000000002,'balcony',228668581192138752,'=WP',0,119,'1.水泥浆基底，不包括特殊基层处理。
2.水泥沙浆，强度32.5#普通硅酸盐水泥，水泥厚度不超过30mm，(水泥砂浆厚度超过10mm另加15元/平方。）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010119,1790000000000000001,234500000000000002,'balcony',233689437391814656,null,0,120,'1.人工（校对，切割），切割片
2.项目范围：墙砖.水管.窗户碰角，阳台地砖卷边碰角，干切工艺。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010120,1790000000000000001,234500000000000002,'balcony',233689437295345664,null,0,121,'1.下水竖管隔音棉处理');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010121,1790000000000000001,234500000000000002,'balcony',233689437219848192,null,1,122,'1.普通砖.32.5#普通硅酸盐水泥砂浆砌筑 
2.单面水泥砂浆打底.抹平');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010122,1790000000000000001,234500000000000002,'balcony',228668585168338944,'=WP',0,123,'1.墙面批刮倍得丽找平石膏+纤维网，做基层处理。
2.墙顶面误差大于2公分需二次找平并挂网或墙顶面墙体质量差须满挂网，费用另加3元每平。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010123,1790000000000000001,234500000000000002,'balcony',228668585189310464,'=WP',0,124,'1.批刮倍得丽腻子二至三遍并打磨平整，喷刷面漆二遍。
2.墙/顶面乳胶漆为白色，彩色乳胶漆与墙纸费用另计。                                                                      
3.不含原有墙面铲除。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010124,1790000000000000001,234500000000000002,'balcony',228668585214476288,'=WP',0,125,'1.三棵树系列，/如需加底漆加900元/户                                                                                                                                                                                                                           ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010125,1790000000000000001,234500000000000002,'balcony',228668581871616000,'=A',0,126,'1.清理基层，无漏刷和鼓泡
2.管槽.下水口.浴缸.淋浴房及反面墙有家具的面须重点防水。（卫生间淋浴区防水高度必须达到1.8米）
3.冷涂两遍,做24-48小时闭水试验');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010126,1790000000000000001,234500000000000002,'balcony',228668583708721152,'=A',0,127,'1.轻钢龙骨+3*4木方加固，9厘泰山石膏板.专用防锈自攻螺钉，钉眼涂防锈漆
2.轻钢龙骨间距≤400×400mm
3.石膏板之间留4～6mmV字缝处理，专用补缝腻子嵌平，再贴防裂绷带');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010127,1790000000000000001,234500000000000002,'balcony',233689436758474752,null,1,128,'人工费');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010128,1790000000000000001,234500000000000002,null,228668594953650176,null,0,129,'1.按建筑面积计算；                                                                                                                                        2.现场成品保护范围包括对原甲方室内设施及乙方所施工项目如地砖.窗台面等成品的保护；                                  3.绿色地固');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010129,1790000000000000001,234500000000000002,null,228668595142393856,null,0,130,'1.按建筑面积计算；                                                                                                                                    2.完工验收后由专业保洁公司进行保洁；');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010130,1790000000000000001,234500000000000002,null,228668594991398912,null,1,131,'1.按建筑面积计算。                                                                                                                                      2.无电梯，二楼（含）以上每平米每层加收5元。                                                                         ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010131,1790000000000000001,234500000000000002,null,228668595066896384,null,1,132,'1.按建筑面积计算；                                                                                                                                                2.本费用含日常垃圾清扫.袋装并运至小区指定地点。                                                             ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010132,1790000000000000001,234500000000000002,null,228668594710380544,null,1,133,'1.含开孔，局部找平.修补等');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010133,1790000000000000001,234500000000000002,null,228668594764906496,null,1,134,'1.含开孔，局部找平.修补等');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010134,1790000000000000001,234500000000000002,null,228668595029147648,null,0,135,'1.层数*建筑面积*10（架空层或者有台阶上一楼直接算一层）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010135,1790000000000000001,234500000000000002,null,228668594915901440,null,1,136,'1.需要脚手架等工具.人工费用（别墅.楼中楼.门面等超过3米以上空间）。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010136,1790000000000000001,234500000000000002,null,228668595175948288,null,0,137,'1.以天蚁装饰公司为起点，30公里范围外');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010137,1790000000000000001,234500000000000002,null,228668582442041344,null,1,138,'1.人工费。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010138,1790000000000000001,234500000000000002,null,228668580604936192,null,0,139,'1.按建筑面积计算
2.水管全换，品牌：保利管.进口原料皇家水管.冷水管25mm热水管20mm
3.电路增改。线管品牌：保利.鸿雁。电线：绿宝，金貂阻燃电线+3元/平方。
4.不含插座开关等面板。
5.三室常规点位50个，四室点位55个，超过一个点位加80元.水管走顶加25元/平方，全房换线加35/每平方。
6.本报价只含一个卫生间，一个阳台，多加一个卫生间加850元/ 多加一个阳台400元/以此类推。                                     ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010139,1790000000000000001,234500000000000002,null,228668580625907712,null,0,140,'1.按建筑面积计算
2.水管全换，品牌：保利管.进口原料皇家水管.冷水管25mm热水管20mm
3.电路全改。线管品牌：保利.鸿雁。电线：绿宝，金貂阻燃电线+3元/平方。
4.不含插座开关面板，强电箱断路器.单片等。
5.三室常规点位60个，四室点位65个，超过一个点位加80元.水管走顶加25元/平方。
6.本报价只含一个卫生间，一个阳台，多加一个卫生间加850元/ 多加一个阳台400元/以此类推。                                       ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010140,1790000000000000001,234500000000000002,null,228668580818845696,null,0,141,'1、中央空调布线配套施工费。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010141,1790000000000000001,234500000000000002,null,228668580646879232,null,0,142,'1.无主灯线路及人工费用。
2.按施工区域面积计算');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010142,1790000000000000001,234500000000000002,null,228668580701405184,null,1,143,'1.强电箱移位。（强电箱移位只适用于对调墙体正反面，其他移动均默认为全改，弱电改造以原始光纤长度为准）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010143,1790000000000000001,234500000000000002,null,228668580726571008,null,1,144,'1.弱电箱移位。（强电箱移位只适用于对调墙体正反面，其他移动均默认为全改，弱电改造以原始光纤长度为准）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010144,1790000000000000001,234500000000000002,null,228668580747542528,null,1,145,'1.PVC管道改造，(排水系统，透气管，下水管等）。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010145,1790000000000000001,234500000000000002,null,228668585533243392,'=A',0,146,'1、马可波罗(85.6元),大角鹿(74.9元), 利家福(64元)系列砖800*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010146,1790000000000000001,234500000000000002,null,228668585533243392,'=A',0,147,'1、马可波罗(85.6元),大角鹿(74.9元), 利家福(64元)系列砖800*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010147,1790000000000000001,234500000000000002,null,228668585533243392,'=A',0,148,'1、马可波罗(85.6元),大角鹿(74.9元), 利家福(64元)系列砖800*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010148,1790000000000000001,234500000000000002,null,228668585533243392,'=A',0,149,'1、马可波罗(85.6元),大角鹿(74.9元), 利家福(64元)系列砖800*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010149,1790000000000000001,234500000000000002,null,228668585533243392,'=A',0,150,'1、马可波罗(85.6元),大角鹿(74.9元), 利家福(64元)系列砖800*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010150,1790000000000000001,234500000000000002,null,228668585533243392,'=A',0,151,'1、马可波罗(85.6元),大角鹿(74.9元), 利家福(64元)系列砖800*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010151,1790000000000000001,234500000000000002,null,228668585633906688,'=A',0,152,'1、马可波罗(68.5元),大角鹿(59.9元), 利家福(53.9元)系列砖400*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010152,1790000000000000001,234500000000000002,null,228668585533243392,'=A',0,153,'1、马可波罗(85.6元),大角鹿(74.9元), 利家福(64元)系列砖800*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010153,1790000000000000001,234500000000000002,null,228668585633906688,'=A',0,154,'1、马可波罗(68.5元),大角鹿(59.9元), 利家福(53.9元)系列砖400*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010154,1790000000000000001,234500000000000002,null,228668585633906688,'=A',0,155,'1、马可波罗(68.5元),大角鹿(59.9元), 利家福(53.9元)系列砖400*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010155,1790000000000000001,234500000000000002,null,228668585633906688,'=A',0,156,'1、马可波罗(68.5元),大角鹿(59.9元), 利家福(53.9元)系列砖400*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010156,1790000000000000001,234500000000000002,null,228668585533243392,'=A',0,157,'1、马可波罗(85.6元),大角鹿(74.9元), 利家福(64元)系列砖800*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010157,1790000000000000001,234500000000000002,null,228668585633906688,'=A',0,158,'1、马可波罗(68.5元),大角鹿(59.9元), 利家福(53.9元)系列砖400*800');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010158,1790000000000000001,234500000000000002,null,228668582244909056,'=A',0,159,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010159,1790000000000000001,234500000000000002,null,228668590138589184,'=A',0,160,'1、可选型号：C201、C202、C203、C204、C205、C206、C301、C302、C303、C304、C306、C307
2、含安装，测量，损耗，防潮膜，实木多层踢脚线，收口条');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010160,1790000000000000001,234500000000000002,null,228668590205698048,'=P-DW',0,161,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010161,1790000000000000001,234500000000000002,null,228668592147660800,'=A',0,162,'1、300*600集成吊顶，含辅材安装。（天蚁专供点6烤漆板）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010162,1790000000000000001,234500000000000002,null,228668592210575360,null,1,163,'1、扣板300*600长灯，包安装');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010163,1790000000000000001,234500000000000002,null,228668592147660800,'=A',0,164,'1、300*600集成吊顶，含辅材安装。（天蚁专供点6烤漆板）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010164,1790000000000000001,234500000000000002,null,228668592244129792,null,1,165,'1、奥瑞奇双电机浴霸，2800w速热系列。（风暖+照明+换气+吹风）包安装');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010165,1790000000000000001,234500000000000002,null,228668587596840960,null,1,166,'1、实木复合烤漆/无漆木门（标准门洞2150*900），含配套门套.门锁.门吸
2、升级静音磁吸门锁加50元/樘
3、门扇高度大于2150mm加150元/樘。
4、型号：混油，科技木皮平板系列');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010166,1790000000000000001,234500000000000002,null,228668587726864384,null,0,167,'1.墙体厚度度300mm内，2.天然开放漆烤漆另加25元/米');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010167,1790000000000000001,234500000000000002,null,228668587752030208,null,0,168,'1.墙体厚度度300mm内，2.天然开放漆烤漆另加25元/米');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010168,1790000000000000001,234500000000000002,null,228668586833477632,null,0,169,'珐琅白 红酸枝 肌肤黑 水曲柳 香槟金 瓷泳灰  高光白 瓷泳灰
①灰玻，茶玻 ，普通长虹。每层玻璃另加28元/m²
②超白长虹，超白油沙，灰油沙，等...超白系列。双层玻璃每层玻璃另加55元/m²，单层玻璃另加85/㎡
③缓冲器另加100元/个，一般不建议做，属于易损件');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010169,1790000000000000001,234500000000000002,null,228668586925752320,null,1,170,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010170,1790000000000000001,234500000000000002,null,228668587005444096,null,0,171,'珐琅白 红酸枝 肌肤黑 水曲柳 香槟金 瓷泳灰  高光白 瓷泳灰
①灰玻，茶玻 ，普通长虹。每层玻璃另加28元/m²
②超白长虹，超白油沙，灰油沙，等...超白系列。双层玻璃每层玻璃另加55元/m²，单层玻璃另加85/㎡
③缓冲器另加100元/个，一般不建议做，属于易损件');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010171,1790000000000000001,234500000000000002,null,228668587005444096,null,0,172,'珐琅白 红酸枝 肌肤黑 水曲柳 香槟金 瓷泳灰  高光白 瓷泳灰
①灰玻，茶玻 ，普通长虹。每层玻璃另加28元/m²
②超白长虹，超白油沙，灰油沙，等...超白系列。双层玻璃每层玻璃另加55元/m²，单层玻璃另加85/㎡
③缓冲器另加100元/个，一般不建议做，属于易损件');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010172,1790000000000000001,234500000000000002,null,228668588863520768,null,1,173,'备注：                                                                                                                                                                                                     1.一字型淋浴房玻璃厚8mm                          ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010173,1790000000000000001,234500000000000002,null,228668588934823936,null,0,174,'备注：                                                                                                                                                                                                     1.一字型淋浴房玻璃厚8mm                          ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010174,1790000000000000001,234500000000000002,null,228668587290656768,null,0,175,'1、16*35铝合金型材，配5mm正常玻璃；
2、工艺玻璃额外
①.5mm灰玻.超白磨砂.超白玻另加30元/㎡; 
②.5mm普通长虹玻璃另加50元/㎡;
③.5mm超白长虹玻璃另加100元/㎡;
③.5mm超白长虹+5mm超白磨砂另加130元/㎡;
④.8mm灰油砂.白油砂.蓝油砂另加100元/㎡; ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010175,1790000000000000001,234500000000000002,null,228668587290656768,null,1,176,'1、边框颜色：珐琅白、红酸枝、肌肤黑、水曲柳、香槟金、瓷泳灰 、高光白、瓷泳灰
2、单层4mm钢化玻璃，灰玻、茶玻、长虹、油沙、灰油沙、水波纹、方格纹、石头纹等
3、1.5m²以内，不足1.5按1.5计算。含测量.运输.安装');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010176,1790000000000000001,234500000000000002,null,228668588079185920,null,1,177,'1.可选实木免漆.PVC材质800*800陶瓷盆，选择大理石台下盆加500元/套，其他款式见图册。                                                                                                        2.卫浴包含水龙头，五金配件。                                                                        ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010177,1790000000000000001,234500000000000002,null,228668588574113792,null,1,178,'1、柜体为实木多层板/门板为水晶单面板（展厅款），一体式盆、长度1.05-1.2m，超出每公分加长加15元，最大不得超过1.8m，超过1.8m后，收取额外加工费360元。深度60cm以内，超出15cm以内属于后飘，加360元。
2、平台切角140元/个。盆内切角415元/个（切角大小28cm以内）
3、含测量，运输及安装');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010178,1790000000000000001,234500000000000002,null,228668589249396736,'=A',0,179,'  1.可选样式： 莎安娜. 马蹄莲 .汉白玉 .银星白 .梨花白 .雅士白  .阿曼 .爵士白 . 圣米尔石 .金碧 .象牙米黄 .麦城金丝 .精品凤凰彩玉  .精品清水白玉 .精品金枝玉叶 .精品粉红玫瑰  .精品碧玺石 .精品奥特曼                                                                                                                         ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010179,1790000000000000001,234500000000000002,null,228668589220036608,'=A',0,180,'1.可选样式： 莎安娜. 马蹄莲 .汉白玉 .银星白 .梨花白 .雅士白  .阿曼 .爵士白 . 圣米尔石 .金碧 .象牙米黄 .麦城金丝 .精品凤凰彩玉  .精品清水白玉 .精品金枝玉叶 .精品粉红玫瑰  .精品碧玺石 .精品奥特曼                                                                                                                           ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010180,1790000000000000001,234500000000000002,null,228668589475889152,null,0,181,null);
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010181,1790000000000000001,234500000000000002,null,228668595314360320,'=A',0,182,'1.enf级欧松板柜体，5厘中纤板背板，机器收边，标配设计为两个抽屉，抽屉超过另加110元/个。
2.不含柜门。
3.深度小于600mm，超出1公分加10元。
4.不含五金（拉手.三节抽.锁等费用另计）。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010182,1790000000000000001,234500000000000002,null,228668595691847680,'=A',0,183,'1.双饰面板。注意：（如做衣柜双饰面板为颗粒板材质，其他板材不建议做，不建议做衣柜长门板）');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010183,1790000000000000001,234500000000000002,null,228668596912390144,null,0,184,'1.免漆板或多层板柜体，5厘板背板，采用机器收边 。
2.按展开长度计数，双饰面柜门，不含石材台面。
3.深度不超过600mm。超出1公分加10元 。
4.不含五金（拉手.三节抽.锁等费用另计）。
备注：柜体更换颗粒板550/每米，标配明拉手，隐形拉手另加15元/米。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010184,1790000000000000001,234500000000000002,null,228668597092745216,null,0,185,'1.展厅石英石台面单色，深度不超过60cm ，超过60CM1公分加8元 。                                                                                           
2.按展开长度计数，其它收边造型和挡水条另计。
3.升级双色石英石加70/米，升级普通不锈钢0.75厚加20元/每米/升级普通1.25加40元每米/升级304不锈钢200元每米，
4.如需造型收边需加70元/每米/厨房台面');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010185,1790000000000000001,234500000000000002,null,228668596958527488,null,0,186,'1.免漆板或多层板柜体，5厘板背板，采用机器收边 。
2.按展开长度计数，双饰面柜门。
3.深度不超过350mm。超出1公分加10元 。
4.不含五金（拉手.三节抽.锁等费用另计）。
备注：柜体更换颗粒板480/每米，标配明拉手，隐形拉手另加15元/米。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010186,1790000000000000001,234500000000000002,null,228668597197602816,null,1,187,'1.配套不锈钢双槽水槽。                                                                                                                                          2.可选展厅第一款或者手工盆，默认展厅第一款水槽。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010187,1790000000000000001,234500000000000002,null,228668598070018048,null,1,188,'1.配套不锈钢水龙头。                                                                                                                                            ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010188,1790000000000000001,234500000000000002,null,228668597285683200,null,1,189,'1.天蚁配套碗篮， 上下2层（720mm.760mm.800mm）3种箱体尺寸。                                                                                                                                           ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010189,1790000000000000001,234500000000000002,null,228668597377957888,null,1,190,'1.天蚁高低台工艺。                                                                                                                                            ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010190,1790000000000000001,234500000000000002,null,228668597377957888,null,1,191,'1.水槽台面下安装。                                                                                                                                            ');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010191,1790000000000000001,234500000000000002,null,228668597562507264,null,1,192,'1.天蚁定制配套板材包燃气管。石材包管另加150元/根。');
insert into design_template_item (item_id,com_id,version_id,room_key,catalog_item_id,quantity_formula,default_quantity,sort_no,remark) values (234500000000010192,1790000000000000001,234500000000000002,null,228668597608644608,null,1,193,'1.柜体及台面切角，常见于燃气管及包柱处橱柜');
commit;

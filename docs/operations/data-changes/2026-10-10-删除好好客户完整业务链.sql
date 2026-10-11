-- 2026-10-10 生产删除麦鸿装饰客户“好好”完整业务链
-- 目标：com_id=1790066191585976744 cust_id=234600090860257280
START TRANSACTION;
SET SESSION sql_safe_updates = 0;
SET SESSION transaction_isolation = 'READ-COMMITTED';

SELECT cust_id, name
  FROM cust_customer
 WHERE cust_id = 234600090860257280
   AND com_id = 1790066191585976744
 FOR UPDATE;

SELECT property_id
  FROM db_property
 WHERE property_id = 234600620802179072
   AND com_id = 1790066191585976744
   AND customer_id = 234600090860257280
 FOR UPDATE;

-- 1. 财务、采购、材料、库存、项目、验收等叶子数据
DELETE FROM db_finance_payment
 WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_finance_payment' AS step, ROW_COUNT() AS affected;

DELETE FROM db_finance_payable
 WHERE property_id = 234600620802179072
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_finance_payable' AS step, ROW_COUNT() AS affected;

DELETE FROM db_goods_receipt_images
 WHERE receipt_id IN (
   SELECT receipt_id FROM db_goods_receipt
    WHERE property_id = 234600620802179072
       OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
 );
SELECT 'db_goods_receipt_images' AS step, ROW_COUNT() AS affected;

DELETE FROM db_goods_receipt
 WHERE property_id = 234600620802179072
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_goods_receipt' AS step, ROW_COUNT() AS affected;

DELETE FROM db_purchase_order_item
 WHERE contract_id = 234601873519149056
    OR property_id = 234600620802179072
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_purchase_order_item' AS step, ROW_COUNT() AS affected;

DELETE FROM db_purchase_order
 WHERE contract_id = 234601873519149056
    OR property_id = 234600620802179072
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
    OR quote_line_id = 234601210328383488;
SELECT 'db_purchase_order' AS step, ROW_COUNT() AS affected;

DELETE FROM db_material_selection_attachments
 WHERE selection_id IN (
   SELECT selection_id FROM db_material_selection
    WHERE contract_id = 234601873519149056
       OR customer_id = 234600090860257280
       OR property_id = 234600620802179072
       OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
       OR source_contract_line_id = 234601873556897792
 );
SELECT 'db_material_selection_attachments' AS step, ROW_COUNT() AS affected;

DELETE FROM db_material_selection
 WHERE contract_id = 234601873519149056
    OR customer_id = 234600090860257280
    OR property_id = 234600620802179072
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
    OR source_contract_line_id = 234601873556897792;
SELECT 'db_material_selection' AS step, ROW_COUNT() AS affected;

DELETE FROM db_material_plan
 WHERE contract_id = 234601873519149056
    OR property_id = 234600620802179072
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
    OR contract_line_id = 234601873556897792
    OR quote_item_id = 234601210328383488;
SELECT 'db_material_plan' AS step, ROW_COUNT() AS affected;

DELETE FROM db_material_return
 WHERE property_id = 234600620802179072
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_material_return' AS step, ROW_COUNT() AS affected;

DELETE FROM db_stock_movement_image
 WHERE movement_id IN (
   SELECT movement_id FROM db_stock_movement
    WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
       OR quote_line_id = 234601210328383488
 );
SELECT 'db_stock_movement_image' AS step, ROW_COUNT() AS affected;

DELETE FROM db_stock_movement
 WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
    OR quote_line_id = 234601210328383488;
SELECT 'db_stock_movement' AS step, ROW_COUNT() AS affected;

DELETE FROM db_project_task_evidence
 WHERE task_id IN (
   SELECT task_id FROM db_project_task
    WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
       OR property_id = 234600620802179072
       OR contract_line_id = 234601873556897792
       OR quote_item_id = 234601210328383488
 );
SELECT 'db_project_task_evidence' AS step, ROW_COUNT() AS affected;

DELETE FROM db_project_task
 WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
    OR property_id = 234600620802179072
    OR contract_line_id = 234601873556897792
    OR quote_item_id = 234601210328383488;
SELECT 'db_project_task' AS step, ROW_COUNT() AS affected;

DELETE FROM db_project_stage
 WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
    OR property_id = 234600620802179072;
SELECT 'db_project_stage' AS step, ROW_COUNT() AS affected;

DELETE FROM db_project_report_image
 WHERE report_id IN (
   SELECT report_id FROM db_project_report
    WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
       OR property_id = 234600620802179072
 );
SELECT 'db_project_report_image' AS step, ROW_COUNT() AS affected;

DELETE FROM db_project_report
 WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
    OR property_id = 234600620802179072;
SELECT 'db_project_report' AS step, ROW_COUNT() AS affected;

DELETE FROM db_project_plan_deviation
 WHERE project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280)
    OR property_id = 234600620802179072
    OR contract_id = 234601873519149056;
SELECT 'db_project_plan_deviation' AS step, ROW_COUNT() AS affected;

DELETE FROM db_acceptance_round_issue
 WHERE round_id IN (SELECT round_id FROM db_acceptance_round WHERE property_id = 234600620802179072);
SELECT 'db_acceptance_round_issue' AS step, ROW_COUNT() AS affected;

DELETE FROM db_acceptance_round_evidence
 WHERE round_id IN (SELECT round_id FROM db_acceptance_round WHERE property_id = 234600620802179072);
SELECT 'db_acceptance_round_evidence' AS step, ROW_COUNT() AS affected;

DELETE FROM db_acceptance_round
 WHERE property_id = 234600620802179072;
SELECT 'db_acceptance_round' AS step, ROW_COUNT() AS affected;

DELETE FROM db_acceptance_evidence
 WHERE acceptance_id IN (SELECT acceptance_id FROM db_acceptance WHERE property_id = 234600620802179072);
SELECT 'db_acceptance_evidence' AS step, ROW_COUNT() AS affected;

DELETE FROM db_acceptance
 WHERE property_id = 234600620802179072;
SELECT 'db_acceptance' AS step, ROW_COUNT() AS affected;

DELETE FROM db_content_asset
 WHERE customer_id = 234600090860257280
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_content_asset' AS step, ROW_COUNT() AS affected;

DELETE FROM db_content_lead_touch
 WHERE customer_id = 234600090860257280;
SELECT 'db_content_lead_touch' AS step, ROW_COUNT() AS affected;

-- 2. 合同变更、施工计划、付款计划、合同行
DELETE FROM db_contract_change_image
 WHERE change_id IN (
   SELECT change_id FROM db_contract_change
    WHERE contract_id = 234601873519149056
       OR property_id = 234600620802179072
       OR source_line_id = 234601873556897792
 );
SELECT 'db_contract_change_image' AS step, ROW_COUNT() AS affected;

DELETE FROM db_contract_change_item
 WHERE change_id IN (
   SELECT change_id FROM db_contract_change
    WHERE contract_id = 234601873519149056
       OR property_id = 234600620802179072
       OR source_line_id = 234601873556897792
 )
    OR contract_line_id = 234601873556897792;
SELECT 'db_contract_change_item' AS step, ROW_COUNT() AS affected;

DELETE FROM db_contract_change
 WHERE contract_id = 234601873519149056
    OR property_id = 234600620802179072
    OR source_line_id = 234601873556897792;
SELECT 'db_contract_change' AS step, ROW_COUNT() AS affected;

DELETE FROM db_contract_construction_stage
 WHERE plan_id IN (
   SELECT plan_id FROM db_contract_construction_plan
    WHERE contract_id = 234601873519149056
 );
SELECT 'db_contract_construction_stage' AS step, ROW_COUNT() AS affected;

DELETE FROM db_contract_construction_plan
 WHERE contract_id = 234601873519149056;
SELECT 'db_contract_construction_plan' AS step, ROW_COUNT() AS affected;

DELETE FROM db_contract_payment_node
 WHERE plan_id IN (
   SELECT plan_id FROM db_contract_payment_plan
    WHERE contract_id = 234601873519149056
 );
SELECT 'db_contract_payment_node' AS step, ROW_COUNT() AS affected;

DELETE FROM db_contract_payment_plan
 WHERE contract_id = 234601873519149056;
SELECT 'db_contract_payment_plan' AS step, ROW_COUNT() AS affected;

DELETE FROM db_contract_line
 WHERE contract_id = 234601873519149056
    OR property_id = 234600620802179072
    OR source_quote_line_id = 234601210328383488;
SELECT 'db_contract_line' AS step, ROW_COUNT() AS affected;

-- 3. 应收、回款、退款、合同
DELETE FROM db_finance_refund
 WHERE customer_id = 234600090860257280
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_finance_refund' AS step, ROW_COUNT() AS affected;

DELETE FROM db_finance_receipt
 WHERE customer_id = 234600090860257280
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_finance_receipt' AS step, ROW_COUNT() AS affected;

DELETE FROM db_finance_receivable
 WHERE contract_id = 234601873519149056
    OR customer_id = 234600090860257280
    OR property_id = 234600620802179072
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_finance_receivable' AS step, ROW_COUNT() AS affected;

DELETE FROM db_project_contract
 WHERE contract_id = 234601873519149056
   AND customer_id = 234600090860257280
   AND property_id = 234600620802179072
   AND quote_id = 234601210319994880;
SELECT 'db_project_contract' AS step, ROW_COUNT() AS affected;

-- 4. 文档、通知、项目
DELETE FROM db_document_print
 WHERE document_id IN (
   SELECT document_id FROM db_document_record
    WHERE quote_id = 234601210319994880
       OR document_id = 234601652147978240
       OR reprint_of_id = 234601652147978240
 );
SELECT 'db_document_print' AS step, ROW_COUNT() AS affected;

DELETE FROM db_document_record
 WHERE quote_id = 234601210319994880
    OR document_id = 234601652147978240
    OR reprint_of_id = 234601652147978240;
SELECT 'db_document_record' AS step, ROW_COUNT() AS affected;

DELETE FROM db_notice
 WHERE customer_id = 234600090860257280
    OR quote_id = 234601210319994880
    OR project_id IN (SELECT project_id FROM db_project WHERE customer_id = 234600090860257280);
SELECT 'db_notice' AS step, ROW_COUNT() AS affected;

DELETE FROM db_project
 WHERE customer_id = 234600090860257280
    OR property_id = 234600620802179072
    OR quote_id = 234601210319994880;
SELECT 'db_project' AS step, ROW_COUNT() AS affected;

-- 5. 报价及设计讲解、设计项、设计空间和设计附属资料
DELETE FROM quote_line_components
 WHERE line_id = 234601210328383488;
SELECT 'quote_line_components' AS step, ROW_COUNT() AS affected;

DELETE FROM quote_line
 WHERE quote_id = 234601210319994880
    OR property_id = 234600620802179072
    OR item_id = 234601210328383488;
SELECT 'quote_line' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_talk_section_images
 WHERE section_id IN (
   SELECT section_id FROM db_design_talk_section
    WHERE talk_id IN (
      SELECT talk_id FROM db_design_talk
       WHERE customer_id = 234600090860257280
          OR design_id = 234600792223383552
          OR property_id = 234600620802179072
          OR quote_id = 234601210319994880
    )
 );
SELECT 'db_design_talk_section_images' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_talk_section
 WHERE talk_id IN (
   SELECT talk_id FROM db_design_talk
    WHERE customer_id = 234600090860257280
       OR design_id = 234600792223383552
       OR property_id = 234600620802179072
       OR quote_id = 234601210319994880
 );
SELECT 'db_design_talk_section' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_talk
 WHERE customer_id = 234600090860257280
    OR design_id = 234600792223383552
    OR property_id = 234600620802179072
    OR quote_id = 234601210319994880;
SELECT 'db_design_talk' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_item_images
 WHERE item_id IN (
   SELECT item_id FROM db_design_item
    WHERE design_id = 234600792223383552
       OR property_id = 234600620802179072
 );
SELECT 'db_design_item_images' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_item
 WHERE design_id = 234600792223383552
    OR property_id = 234600620802179072
    OR item_id = 234600834963341312;
SELECT 'db_design_item' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_space_images
 WHERE space_id IN (
   SELECT space_id FROM db_design_space
    WHERE design_id = 234600792223383552
       OR property_id = 234600620802179072
 );
SELECT 'db_design_space_images' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_space
 WHERE design_id = 234600792223383552
    OR property_id = 234600620802179072;
SELECT 'db_design_space' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_original_plan_images WHERE design_id = 234600792223383552;
SELECT 'db_design_original_plan_images' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_site_photos WHERE design_id = 234600792223383552;
SELECT 'db_design_site_photos' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_survey_photos WHERE design_id = 234600792223383552;
SELECT 'db_design_survey_photos' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_context_snapshot
 WHERE design_id = 234600792223383552
    OR customer_id = 234600090860257280
    OR property_id = 234600620802179072;
SELECT 'db_design_context_snapshot' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_generation_task
 WHERE design_id = 234600792223383552
    OR customer_id = 234600090860257280
    OR property_id = 234600620802179072;
SELECT 'db_design_generation_task' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_prompt_snapshot
 WHERE design_id = 234600792223383552
    OR customer_id = 234600090860257280
    OR property_id = 234600620802179072;
SELECT 'db_design_prompt_snapshot' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_render_version
 WHERE design_id = 234600792223383552
    OR customer_id = 234600090860257280
    OR property_id = 234600620802179072;
SELECT 'db_design_render_version' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design_requirement
 WHERE customer_id = 234600090860257280
    OR property_id = 234600620802179072;
SELECT 'db_design_requirement' AS step, ROW_COUNT() AS affected;

DELETE FROM quote
 WHERE quote_id = 234601210319994880
   AND customer_id = 234600090860257280
   AND property_id = 234600620802179072
   AND design_id = 234600792223383552;
SELECT 'quote' AS step, ROW_COUNT() AS affected;

DELETE FROM db_design
 WHERE design_id = 234600792223383552
   AND customer_id = 234600090860257280
   AND property_id = 234600620802179072;
SELECT 'db_design' AS step, ROW_COUNT() AS affected;

-- 6. 量房
DELETE FROM db_measure_attachments WHERE measure_id = 234600621271941120;
SELECT 'db_measure_attachments' AS step, ROW_COUNT() AS affected;

DELETE FROM db_measure_site_photos WHERE measure_id = 234600621271941120;
SELECT 'db_measure_site_photos' AS step, ROW_COUNT() AS affected;

DELETE FROM db_measure_structure_images WHERE measure_id = 234600621271941120;
SELECT 'db_measure_structure_images' AS step, ROW_COUNT() AS affected;

DELETE FROM db_measure
 WHERE measure_id = 234600621271941120
    OR customer_id = 234600090860257280
    OR property_id = 234600620802179072;
SELECT 'db_measure' AS step, ROW_COUNT() AS affected;

-- 7. 客户附属数据、房屋与客户
DELETE FROM cust_customer_follow_image
 WHERE follow_id IN (SELECT follow_id FROM cust_customer_follow WHERE customer_id = 234600090860257280);
SELECT 'cust_customer_follow_image' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_customer_follow WHERE customer_id = 234600090860257280;
SELECT 'cust_customer_follow' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_customer_feedback_image
 WHERE feedback_id IN (SELECT feedback_id FROM cust_customer_feedback WHERE cust_id = 234600090860257280 OR property_id = 234600620802179072);
SELECT 'cust_customer_feedback_image' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_customer_feedback
 WHERE cust_id = 234600090860257280
    OR property_id = 234600620802179072;
SELECT 'cust_customer_feedback' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_customer_access WHERE cust_id = 234600090860257280;
SELECT 'cust_customer_access' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_customer_invite WHERE customer_id = 234600090860257280;
SELECT 'cust_customer_invite' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_customer_status_log WHERE cust_id = 234600090860257280;
SELECT 'cust_customer_status_log' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_portrait_concern
 WHERE portrait_id IN (SELECT portrait_id FROM cust_portrait WHERE cust_id = 234600090860257280);
SELECT 'cust_portrait_concern' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_portrait WHERE cust_id = 234600090860257280;
SELECT 'cust_portrait' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_tag_link WHERE cust_id = 234600090860257280;
SELECT 'cust_tag_link' AS step, ROW_COUNT() AS affected;

DELETE FROM db_property
 WHERE property_id = 234600620802179072
   AND com_id = 1790066191585976744
   AND customer_id = 234600090860257280;
SELECT 'db_property' AS step, ROW_COUNT() AS affected;

DELETE FROM cust_customer
 WHERE cust_id = 234600090860257280
   AND com_id = 1790066191585976744
   AND name = '好好';
SELECT 'cust_customer' AS step, ROW_COUNT() AS affected;

COMMIT;

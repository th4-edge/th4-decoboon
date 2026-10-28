#!/usr/bin/env python3
"""Build INFO-D1 mapping from the business information registry to current code."""

from __future__ import annotations

import json
from collections import Counter
from pathlib import Path


REPO = Path(__file__).resolve().parents[1]
BASE = REPO.parents[1]
REGISTRY = BASE / "推广营销" / "装得清业务智能体" / "00-治理" / "业务信息项注册表-v0.1.json"
OUT_JSON = REPO / "docs" / "product" / "domains" / "product-system-baseline" / "INFO-D1-info-item-code-mapping.json"
OUT_MD = REPO / "docs" / "product" / "domains" / "product-system-baseline" / "INFO-D1-info-item-code-mapping.md"


FIELDS = {
    "INFO-ORG-001": "Company.comId/name/title/logo/color/addr/setting",
    "INFO-ORG-002": "Store.storeId/name/defaultStore; Team.teamId/name",
    "INFO-ORG-003": "Employee.empId/name/telNo/avatarUrl/role/status/store/team",
    "INFO-ORG-004": "EmployeeRole; DataScope; ProjectMember/参与关系",
    "INFO-ORG-005": "Subscription.plan/startTime/endTime/status; Plan.maxStores/maxSeats/modules",
    "INFO-CUSTOMER-001": "Customer.custId/name",
    "INFO-CUSTOMER-002": "Customer.telNo; CustomerAccess 手机号校验",
    "INFO-CUSTOMER-003": "Customer.source/owner/designer/store/team",
    "INFO-CUSTOMER-004": "Customer.status/idleDays/riskLevel/riskReason/nextAction/mainline*; CustomerTag",
    "INFO-CUSTOMER-005": "CustomerAccess.creId/customer/status; 项目码 scene",
    "INFO-CUSTOMER-006": "Customer.budget",
    "INFO-PROPERTY-001": "Property.propertyId/propertyNo/community/building/roomNo/address",
    "INFO-PROPERTY-002": "Property.area/layout/status",
    "INFO-PROPERTY-003": "Property.customer/owner/designer",
    "INFO-DEMAND-001": "CustomerFollow.way/content/nextFollowAt/images/emp/createTime",
    "INFO-DEMAND-002": "CustomerFollow.nextFollowAt; Customer.nextFollowAt",
    "INFO-DEMAND-003": "CustomerPortrait.decisionRole/purpose/houseCondition/startWindow/style/eco/household/payment/concerns/competitor",
    "INFO-DEMAND-004": "Requirement.type/spaceName/kind/category/priority/title/detail/source",
    "INFO-DEMAND-005": "Requirement.status/response/confirmedAt/owner",
    "INFO-MEASURE-001": "Measure.scheduledAt/executor/completedAt/status/designer",
    "INFO-MEASURE-002": "Measure.sitePhotos/structureImages/attachments",
    "INFO-MEASURE-003": "MeasureRegion.roomType/name/length/width/height/area/perimeter/areaSource/measuredBy/measuredAt",
    "INFO-MEASURE-004": "Measure.originalStructure/dimensions/customerRequirements/designerConclusion",
    "INFO-DESIGN-001": "Design.designId/no/name/version/status/customer/property/owner",
    "INFO-DESIGN-002": "Space.spaceId/type/name/length/width/height/area/perimeter/valueOrigin/sourceMeasureRegionId",
    "INFO-DESIGN-003": "DesignItem.type/category/catalogItem/name/brand/spec/unit/quantity/materialPrice/manualPrice/amount/cost",
    "INFO-DESIGN-004": "DesignItem.craft/criterion; QuoteLine.craft/criterion",
    "INFO-DESIGN-005": "Design.coverUrl/cadUrl/cadPreviewUrl/originalPlan*/demolitionUrl/masonryUrl/optimizationUrl/sitePhotos/surveyPhotos; Talk.*",
    "INFO-QUOTE-001": "Quote.quoteId/no/customer/property/design/createdBy/version/mode/status",
    "INFO-QUOTE-002": "QuoteLine.type/category/name/brand/spec/unit/quantity/price/amount/cost/source*",
    "INFO-QUOTE-003": "Quote.subtotal/discount/taxRate/taxAmount/amount/designFee/managementFee",
    "INFO-QUOTE-004": "Quote.status/submittedAt/acceptedAt/acceptedBy/rejectedAt/convertedAt",
    "INFO-CONTRACT-001": "ProjectContract.contractId/no/quote/project/customer/property/status/signedAt/effectiveAt/startDate/planEndDate",
    "INFO-CONTRACT-002": "ProjectContract.baselineAmount/signedAmount/currentAmount/signedCost/costComplete",
    "INFO-CONTRACT-003": "ContractLine.type/category/name/brand/spec/unit/quantity/materialPrice/manualPrice/amount/cost/spaceName",
    "INFO-CONTRACT-004": "ContractPaymentPlan/ContractPaymentNode; Receivable",
    "INFO-CONTRACT-005": "ContractConstructionPlan/ContractConstructionStage; ProjectStage.acceptanceMode",
    "INFO-CONTRACT-006": "ContractChange.type/reason/status/items/customerConfirmed*/evidenceImages/signatureUrl/approvedBy/conclusion",
    "INFO-PROJECT-001": "Project.projectId/no/name/customer/property/contract/designer/manager/support/status",
    "INFO-PROJECT-002": "ProjectStage.stageCode/name/status/progress/plannedStart/plannedEnd/acceptanceMode",
    "INFO-PROJECT-003": "Project.riskLevel/riskCount/mainRiskType/riskReason/nextAction/overdueDays",
    "INFO-TASK-001": "ProjectTask.taskId/name/zone/craft/spec/quantity/unit/status/source*",
    "INFO-TASK-002": "ProjectTask.owner/creator/priority/conceal/plannedStart/plannedEnd/actualStart/actualEnd",
    "INFO-TASK-003": "TaskNode.nodeKey/note/medias/confirmedBy; ProjectTask.evidenceUrls",
    "INFO-REPORT-001": "ProjectReport.type/title/content/customerSummary/images/author/authorRole/visibleToCustomer",
    "INFO-ACCEPTANCE-001": "Acceptance.stage/status/conclusion/issues/evidenceUrls/rectificationOwner/rectificationDueDate/confirmedBy/confirmedAt",
    "INFO-ACCEPTANCE-002": "AcceptanceRound.roundNo/result/issues/rectificationNote/rectificationEvidenceUrls/confirmedBy/confirmedAt",
    "INFO-FEEDBACK-001": "CustomerFeedback.kind/type/title/content/images/handler/response/status/dueAt/closedAt/satisfaction",
    "INFO-MATERIAL-001": "MaterialPlan.contractLine/changeItem/quoteItem/catalogItem/quantity/unit/selectedBrand/selectedSpec/selectedModel/requiredAt/arrivedAt/status",
    "INFO-MATERIAL-002": "MaterialSelection.contract/project/version/status/title/content/selectionJson/attachments/items",
    "INFO-MATERIAL-003": "MaterialOrderSheet.sheetType/category/supplier/measureAt/groupChatAt/deliverAt/installAt/signatures/status/priceState",
    "INFO-MATERIAL-004": "MaterialOrderSheetItem.spaceName/locationName/materialName/brand/model/spec/color/size/craft/glass/quantity/unit/purchasePrice/settlementPrice",
    "INFO-MATERIAL-005": "PurchaseOrder.supplier/deliveryMode/quantity/unitPrice/amount/expectedAt/priceState/status/source*",
    "INFO-MATERIAL-008": "GoodsReceipt.purchase/purchaseItem/materialPlan/quantity/batchNo/expiryDate/arrivedAt/inspector/images",
    "INFO-MATERIAL-006": "Inventory/StockMove/Return/Deduction 相关库存、退换和损耗字段",
    "INFO-MATERIAL-007": "Partner.name/type/contact/账期/settlementMode/enabled",
    "INFO-FINANCE-001": "Receivable.project/contract/stage/type/amount/receivedAmount/dueDate/status/invoice*",
    "INFO-FINANCE-002": "Receipt.project/receivable/amount/account/receivedAt/payerName/voucherUrl/status/reversal*",
    "INFO-FINANCE-003": "Payable.project/source*/payee*/amount/paidAmount/period/dueDate/status/estimated",
    "INFO-FINANCE-010": "Payment.payable/amount/account/paidAt/payeeName/partner/worker/payeeEmployee/batchNo/transactionNo/voucherUrl",
    "INFO-FINANCE-004": "Receivable.invoiceRequired/invoiceNo; Invoice 对象未落地",
    "INFO-FINANCE-005": "Ledger.direction/category/purpose/project/source/sourceId/amount/account/occurredAt/counterparty/summary/remark",
    "INFO-FINANCE-006": "VoucherRecord/Template/version/amount/party/date/PDF/重打记录",
    "INFO-FINANCE-007": "ProjectCostContext P1/P2/P3、costComplete、profitAmount/grossMargin",
    "INFO-FINANCE-008": "Settlement.contractAmount/finalAmount/finalCost/profitAmount/grossMargin/status",
    "INFO-FINANCE-009": "PayrollSheet/PayrollItem/Attendance/Commission/Payment/Voucher",
    "INFO-OWNER-001": "CustomerAccess; Project.ownerQrcodeScene/Path/Url/EnvVersion",
    "INFO-OWNER-002": "CustomerJourney.sections/actions/progress/currentStageName/vendor/team/previewMeta",
    "INFO-OWNER-003": "Acceptance.confirmedBy/confirmedAt/status; AcceptanceRound.confirmedBy/confirmedAt/result",
    "INFO-OWNER-004": "ShareJourneyView; CustomerFeedback; 售后/质保对象待建设",
}


STATUS = {
    "INFO-ORG-001": "ALIGNED", "INFO-ORG-002": "PARTIAL", "INFO-ORG-003": "ALIGNED", "INFO-ORG-004": "PARTIAL", "INFO-ORG-005": "ALIGNED",
    "INFO-CUSTOMER-001": "ALIGNED", "INFO-CUSTOMER-002": "ALIGNED", "INFO-CUSTOMER-003": "ALIGNED", "INFO-CUSTOMER-004": "PARTIAL", "INFO-CUSTOMER-005": "PARTIAL", "INFO-CUSTOMER-006": "PARTIAL",
    "INFO-PROPERTY-001": "ALIGNED", "INFO-PROPERTY-002": "PARTIAL", "INFO-PROPERTY-003": "ALIGNED",
    "INFO-DEMAND-001": "PARTIAL", "INFO-DEMAND-002": "ALIGNED", "INFO-DEMAND-003": "NO_OPERATIONAL_CONSUMER", "INFO-DEMAND-004": "PARTIAL", "INFO-DEMAND-005": "PARTIAL",
    "INFO-MEASURE-001": "ALIGNED", "INFO-MEASURE-002": "PARTIAL", "INFO-MEASURE-003": "ALIGNED", "INFO-MEASURE-004": "DUPLICATE_SOURCE",
    "INFO-DESIGN-001": "PARTIAL", "INFO-DESIGN-002": "ALIGNED", "INFO-DESIGN-003": "ALIGNED", "INFO-DESIGN-004": "SNAPSHOT_MISSING", "INFO-DESIGN-005": "PARTIAL",
    "INFO-QUOTE-001": "ALIGNED", "INFO-QUOTE-002": "PARTIAL", "INFO-QUOTE-003": "PARTIAL", "INFO-QUOTE-004": "ALIGNED",
    "INFO-CONTRACT-001": "PARTIAL", "INFO-CONTRACT-002": "ALIGNED", "INFO-CONTRACT-003": "SNAPSHOT_MISSING", "INFO-CONTRACT-004": "ALIGNED", "INFO-CONTRACT-005": "ALIGNED", "INFO-CONTRACT-006": "PARTIAL",
    "INFO-PROJECT-001": "ALIGNED", "INFO-PROJECT-002": "PARTIAL", "INFO-PROJECT-003": "PARTIAL",
    "INFO-TASK-001": "PARTIAL", "INFO-TASK-002": "PARTIAL", "INFO-TASK-003": "PARTIAL", "INFO-REPORT-001": "PARTIAL",
    "INFO-ACCEPTANCE-001": "PARTIAL", "INFO-ACCEPTANCE-002": "PARTIAL", "INFO-FEEDBACK-001": "PARTIAL",
    "INFO-MATERIAL-001": "PARTIAL", "INFO-MATERIAL-002": "PARTIAL", "INFO-MATERIAL-003": "PARTIAL", "INFO-MATERIAL-004": "SNAPSHOT_MISSING", "INFO-MATERIAL-005": "PARTIAL", "INFO-MATERIAL-008": "PARTIAL", "INFO-MATERIAL-006": "PARTIAL", "INFO-MATERIAL-007": "ALIGNED",
    "INFO-FINANCE-001": "PARTIAL", "INFO-FINANCE-002": "PARTIAL", "INFO-FINANCE-003": "PARTIAL", "INFO-FINANCE-010": "PARTIAL", "INFO-FINANCE-004": "UNMAPPED", "INFO-FINANCE-005": "PARTIAL", "INFO-FINANCE-006": "PARTIAL", "INFO-FINANCE-007": "PARTIAL", "INFO-FINANCE-008": "PARTIAL", "INFO-FINANCE-009": "PARTIAL",
    "INFO-OWNER-001": "PARTIAL", "INFO-OWNER-002": "PARTIAL", "INFO-OWNER-003": "PARTIAL", "INFO-OWNER-004": "PARTIAL",
}


APP_BY_DOMAIN = {
    "组织与权限": ["pages/mine/index", "subpackages/company/pages/members", "subpackages/company/pages/invite"],
    "客户经营": ["pages/customer/list", "subpackages/customer/pages/detail", "subpackages/customer/pages/edit"],
    "房屋资产": ["subpackages/customer/pages/property-edit", "subpackages/customer/pages/detail"],
    "需求与跟进": ["subpackages/customer/pages/follow", "subpackages/customer/pages/portrait-edit", "subpackages/design/pages/requirement"],
    "量房": ["subpackages/customer/pages/measure", "subpackages/customer/pages/measure-record"],
    "设计": ["subpackages/design/pages/scheme", "subpackages/design/pages/workbench", "subpackages/design/pages/requirements"],
    "报价": ["subpackages/quote/pages/workbench", "subpackages/quote/pages/detail", "subpackages/quote/pages/edit"],
    "合同": ["subpackages/project/pages/sign-workbench", "subpackages/project/pages/create", "subpackages/project/pages/contract"],
    "项目履约": ["subpackages/project/pages/detail", "subpackages/project/pages/plan"],
    "施工执行": ["subpackages/project/pages/plan", "subpackages/project/pages/task/detail"],
    "现场协同": ["subpackages/project/pages/report/edit", "subpackages/project/pages/report/daily"],
    "质量验收": ["subpackages/project/pages/accept/edit", "subpackages/project/pages/rectify-board"],
    "服务与投诉": ["subpackages/project/pages/feedback-edit", "subpackages/project/pages/feedback-list"],
    "材料供应链": ["subpackages/project/pages/material-plan", "subpackages/project/pages/selection-board", "subpackages/project/pages/material-sheet"],
    "财务经营": ["pages/finance/index", "subpackages/finance/pages/receivables", "subpackages/finance/pages/payments"],
    "业主服务": ["pages/index/index", "subpackages/project/pages/journey", "subpackages/project/pages/owner-acceptance"],
}

WEB_BY_DOMAIN = {
    "组织与权限": ["BossOrganizationView", "RoleAreaShell/session", "WorkbenchHomeView"],
    "客户经营": ["DesignerCustomersView", "DesignerCustomer360View", "DesignerFollowUpTasksView"],
    "房屋资产": ["DesignerCustomersView", "DesignerCustomer360View"],
    "需求与跟进": ["DesignerFollowUpTasksView", "DesignerCustomer360View", "DesignerDesignWorkspaceView"],
    "量房": ["DesignerMeasureTasksView", "DesignerCustomersView", "DesignerDesignWorkspaceView"],
    "设计": ["DesignerDesignWorkspaceView", "DesignerQuoteWorkspaceView", "DesignerCustomer360View"],
    "报价": ["DesignerQuoteListView", "DesignerQuoteWorkspaceView", "FinanceQuotesView", "FinanceQuoteDetailView"],
    "合同": ["DesignerContractHandoffView", "DesignerProjectContractView", "DesignerContractChangesView"],
    "项目履约": ["ProjectExecutionView", "DesignerProjectsView"],
    "施工执行": ["ProjectExecutionView"],
    "现场协同": ["ProjectExecutionView", "DesignerProjectsView"],
    "质量验收": ["DesignerAcceptanceView", "ProjectExecutionView"],
    "服务与投诉": ["FeedbackBoardView", "FeedbackDetailView", "ProjectExecutionView"],
    "材料供应链": ["FinanceMaterialOrderingView", "FinanceMaterialSettlementView", "DesignerMaterialSelectionBoardView"],
    "财务经营": ["FinanceWorkbenchView", "FinanceReceivablesView", "FinancePayablesView", "FinanceLedgerView", "FinanceVouchersView"],
    "业主服务": ["NO_WEB_OWNER_UI; App-only owner projection"],
}

OWNER_BY_DOMAIN = {
    "客户经营": ["项目码/邀请手机号校验后进入我的家"],
    "房屋资产": ["我的家/量房档案"],
    "需求与跟进": ["方案与档案中的授权摘要"],
    "量房": ["量房档案"],
    "设计": ["设计方案"],
    "报价": ["报价清单"],
    "合同": ["合同档案"],
    "项目履约": ["我的家/进度"],
    "施工执行": ["进度与现场动态摘要"],
    "现场协同": ["现场动态"],
    "质量验收": ["阶段验收/整改复验"],
    "服务与投诉": ["意见与投诉"],
    "材料供应链": ["选材记录/材料计划"],
    "财务经营": ["我的钱/付款计划"],
    "业主服务": ["我的家/装修历程/家档案/服务团队"],
}


TESTS_BY_DOMAIN = {
    "组织与权限": ["UserIdentityContextTest", "EmployeeRoleContractTest", "MembershipContextTest", "SubscriptionAccessFilterAcceptanceTest", "FinanceOrganizationRulesTest"],
    "客户经营": ["CustomerInsightContextTest", "CustomerOwnerAssignmentTest", "CustomerLossContextTest", "ScanEnterAccessTest", "ShareViewInfoTest"],
    "房屋资产": ["PropertyHandoffOwnershipTest", "PropertyPageDesignerVisibilityTest", "PropertyProjectManagerVisibilityTest"],
    "需求与跟进": ["CustomerFollowImageTest", "RequirementSaveUpdateTest", "TalkContextTest"],
    "量房": ["MeasureRegionSaveTest", "MeasureScheduleFactTest", "MeasureStateGuardTest", "CustomerMeasureStateGateTest"],
    "设计": ["DesignSchemeSaveTest", "DesignItemBatchSaveTest", "DesignSpaceSaveMeasuredTest", "DesignWritePermissionTest", "TalkPptContextTest"],
    "报价": ["QuoteCalcContextTest", "QuoteViewContractTest", "QuoteContextBusinessRulesTest", "QuoteWorkbookFileContextTest"],
    "合同": ["ProjectContractContextTest", "ContractSignRoleTest", "ContractChangeFulfillmentContextTest", "ContractFactTraceTest"],
    "项目履约": ["ProjectCreationScopeGuardTest", "ProjectOverviewContextTest", "ProjectBusinessStateContextTest", "ProjectFactTraceContextTest"],
    "施工执行": ["TaskNodeSaveAcceptanceTest", "ProjectPlanFromScheduleTest", "TaskUpdateTransitionPolicyTest"],
    "现场协同": ["ProjectReportPublisherAccessTest", "ProjectReportStoreManagerScopeTest", "BusinessEventContextTest"],
    "质量验收": ["ProjectProgressCompletionGateTest", "DecoboonClosedLoopTest", "CustomerJourneyOwnerPreviewModeTest"],
    "服务与投诉": ["ProjectFeedbackScopeTest", "CustomerJourneyOwnerPreviewModeTest"],
    "材料供应链": ["MaterialSelectionFlowTest", "MaterialSheetOrderFlowTest", "MaterialGoodsReceiptFlowTest", "MaterialSettlementFlowTest", "InventoryContextTest"],
    "财务经营": ["FinanceClosedLoopTest", "FinanceAgingContextTest", "PaymentIdempotencyTest", "ReceiptIdempotencyTest", "ReceiptReversalContextTest", "ProjectCostContextTest"],
    "业主服务": ["ScanEnterAccessTest", "CustomerJourneyEnrichmentTest", "CustomerJourneyOwnerPreviewModeTest", "ShareViewInfoTest"],
}


FINDING_MAP = {
    "INFO-01": ["INFO-DEMAND-001"], "INFO-02": ["INFO-CUSTOMER-006"], "INFO-03": ["INFO-DEMAND-003"], "INFO-04": ["INFO-DEMAND-003"],
    "INFO-05": ["INFO-DEMAND-003"], "INFO-06": ["INFO-DEMAND-003"], "INFO-07": ["INFO-DEMAND-003"], "INFO-08": ["INFO-DEMAND-003"],
    "INFO-09": ["INFO-DEMAND-003"], "INFO-10": ["INFO-MEASURE-004", "INFO-DEMAND-004"], "INFO-11": ["INFO-MEASURE-004", "INFO-DESIGN-004"],
    "INFO-12": ["INFO-DEMAND-005"], "INFO-13": ["INFO-DEMAND-003"], "INFO-14": ["INFO-DEMAND-003"], "INFO-15": ["INFO-DEMAND-003"],
    "INFO-16": ["INFO-MEASURE-004"], "INFO-17": ["INFO-MEASURE-004"], "INFO-18": ["INFO-CUSTOMER-001"], "INFO-19": ["INFO-CUSTOMER-001"],
    "INFO-20": ["INFO-CUSTOMER-001"], "INFO-21": ["INFO-PROPERTY-001"], "INFO-22": ["INFO-DEMAND-005"],
    "CHAIN-01": ["INFO-DESIGN-001"], "CHAIN-02": ["INFO-DESIGN-004", "INFO-QUOTE-002", "INFO-CONTRACT-003"], "CHAIN-03": ["INFO-DESIGN-004", "INFO-CONTRACT-003", "INFO-ACCEPTANCE-001"],
    "CHAIN-04": ["INFO-CONTRACT-006"], "CHAIN-05": ["INFO-MATERIAL-004", "INFO-MATERIAL-005"], "CHAIN-06": ["INFO-TASK-003", "INFO-ACCEPTANCE-001"],
    "CHAIN-07": ["INFO-ACCEPTANCE-002"], "CHAIN-08": ["INFO-DESIGN-005"], "CHAIN-09": ["INFO-QUOTE-003"], "CHAIN-10": ["INFO-CONTRACT-004"],
    "CHAIN-11": ["INFO-CONTRACT-006"], "CHAIN-12": ["INFO-MATERIAL-003"], "CHAIN-13": ["INFO-MATERIAL-003"], "CHAIN-14": ["INFO-REPORT-001"],
    "CHAIN-15": ["INFO-FINANCE-004"], "CHAIN-16": ["INFO-FINANCE-010"], "CHAIN-17": ["INFO-TASK-001"],
}

NEXT_ACTION = {
    "NO_OPERATIONAL_CONSUMER": "将画像字段接入设计约束、谈单任务、报价预警或跟进队列；未接入前不作为有效业务信息。",
    "DUPLICATE_SOURCE": "确定 MeasureRegion/Requirement 唯一主源，量房文字仅保留为证据摘要。",
    "SNAPSHOT_MISSING": "补齐来源 ID、版本、冻结点和下游快照消费，禁止回读当前主数据改写历史。",
    "UNMAPPED": "建立正式 Invoice 对象、状态、权限和来源链，或从正式表单移除。",
    "PARTIAL": "补齐唯一主源、下游动作、真实页面和闭环测试。",
    "ALIGNED": "保持同源，补充真实页面与独立验收证据。",
}


def main() -> int:
    payload = json.loads(REGISTRY.read_text(encoding="utf-8"))
    items = payload["items"]
    ids = [item["id"] for item in items]
    missing_fields = sorted(set(ids) - set(FIELDS))
    missing_status = sorted(set(ids) - set(STATUS))
    if missing_fields or missing_status:
        raise SystemExit(f"mapping missing fields={missing_fields} status={missing_status}")

    expected_findings = {f"INFO-{i:02d}" for i in range(1, 23)} | {f"CHAIN-{i:02d}" for i in range(1, 18)}
    if set(FINDING_MAP) != expected_findings:
        raise SystemExit(
            f"finding mapping mismatch missing={sorted(expected_findings - set(FINDING_MAP))} "
            f"extra={sorted(set(FINDING_MAP) - expected_findings)}"
        )
    unknown_finding_items = sorted({item_id for values in FINDING_MAP.values() for item_id in values if item_id not in ids})
    if unknown_finding_items:
        raise SystemExit(f"finding mapping references unknown items={unknown_finding_items}")

    findings_by_item: dict[str, list[str]] = {}
    for finding, item_ids in FINDING_MAP.items():
        for item_id in item_ids:
            findings_by_item.setdefault(item_id, []).append(finding)

    mapped = []
    for item in items:
        item_id = item["id"]
        status = STATUS[item_id]
        mapped.append({
            "id": item_id,
            "domain": item["domain"],
            "name": item["name"],
            "concept": item["concept"],
            "semantic": item["semantic"],
            "status": status,
            "backend": {"fields": FIELDS[item_id], "providerRoutes": item["source"]},
            "app": APP_BY_DOMAIN.get(item["domain"], ["UNMAPPED"]),
            "web": WEB_BY_DOMAIN.get(item["domain"], ["UNMAPPED"]),
            "owner": OWNER_BY_DOMAIN.get(item["domain"], ["不适用"]),
            "tests": TESTS_BY_DOMAIN.get(item["domain"], ["UNMAPPED"]),
            "findingIds": sorted(findings_by_item.get(item_id, [])),
            "nextAction": NEXT_ACTION[status],
        })

    counts = Counter(item["status"] for item in mapped)
    finding_ids = {fid for finding, ids_ in FINDING_MAP.items() for fid in [finding]}
    payload_out = {
        "schemaVersion": "1.0",
        "batch": "INFO-D1",
        "updated": "2026-10-28",
        "registry": str(REGISTRY),
        "summary": {"items": len(mapped), "statusCounts": dict(sorted(counts.items())), "findingsMapped": len(finding_ids)},
        "unregisteredCurrentFields": [
            {"field": "Customer.gender", "reason": "当前仅展示，未登记为正式业务信息项；候选删除或降为个人资料。"},
            {"field": "Customer.remark", "reason": "与跟进/画像/诉求职责重叠；候选降为备注或删除。"},
            {"field": "Customer.wechatNo", "reason": "未形成身份、触达或协作动作；候选仅保留微信已添加事实。"},
        ],
        "findingMappings": FINDING_MAP,
        "items": mapped,
    }
    OUT_JSON.write_text(json.dumps(payload_out, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    lines = [
        "# INFO-D1 业务信息项与代码映射",
        "",
        "> 状态：`DEV_VERIFIED / MAPPING_ONLY`；不修改业务代码，不替代独立验收。",
        "",
        f"- 信息项：{len(mapped)}",
        f"- 已关联审计问题：{len(finding_ids)}",
        "- 状态分布：" + "、".join(f"{k} {v}" for k, v in sorted(counts.items())),
        "",
        "## 信息项映射",
        "",
        "| ID | 信息项 | 概念/语义 | 状态 | 后端字段/Provider | App | Web | 测试 | 问题 |",
        "| --- | --- | --- | --- | --- | --- | --- | --- | --- |",
    ]
    for item in mapped:
        lines.append(
            f"| `{item['id']}` | {item['name']} | `{item['concept']}` / `{item['semantic']}` | **{item['status']}** | "
            f"{item['backend']['fields']}<br>`{item['backend']['providerRoutes']}` | {' / '.join(item['app'])} | "
            f"{' / '.join(item['web'])} | {' / '.join(item['tests'])} | {', '.join(item['findingIds']) or '—'} |"
        )
    lines.extend([
        "",
        "## 当前不应继续作为正式信息项的低价值字段",
        "",
        "| 当前字段 | 处理建议 |",
        "| --- | --- |",
        *[f"| `{item['field']}` | {item['reason']} |" for item in payload_out["unregisteredCurrentFields"]],
        "",
        "## 首批 P1 修复顺序",
        "",
        "1. `INFO-DESIGN-004` → `INFO-QUOTE-002` → `INFO-CONTRACT-003` → `INFO-TASK-001` → `INFO-ACCEPTANCE-001`：补齐工艺与验收标准快照。",
        "2. `INFO-MATERIAL-004` → `INFO-MATERIAL-005` → `INFO-MATERIAL-008`：补齐主材规格到采购和到场。",
        "3. `INFO-TASK-003` → `INFO-ACCEPTANCE-001/002`：复用节点证据并逐项整改。",
        "4. `INFO-DEMAND-001/003` 与 `INFO-CUSTOMER-006`：把跟进、画像和预算接入真实岗位动作。",
        "",
        "## 说明",
        "",
        "- App/Web 路径是当前主要消费面，不代表该信息项在所有页面可见。",
        "- 测试清单是关联测试入口，不代表每个字段已有完整分支覆盖。",
        "- `PARTIAL` 不等同于错误，表示信息存在但闭环或证据未完整。",
        "- `NO_OPERATIONAL_CONSUMER / UNMAPPED / SNAPSHOT_MISSING / DUPLICATE_SOURCE` 是首批修复对象。",
    ])
    OUT_MD.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"wrote {OUT_JSON}")
    print(f"wrote {OUT_MD}")
    print(json.dumps(payload_out["summary"], ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

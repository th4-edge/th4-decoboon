package com.th4.edge.decoboon.material.assistant;

import com.th4.edge.decoboon.material.configure.MaterialSheetStatus;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.context.MaterialHealthContext;
import com.th4.edge.decoboon.material.context.MaterialPlanWorkbenchContext;
import com.th4.edge.decoboon.material.context.MaterialSheetContext;
import com.th4.edge.decoboon.material.context.MaterialSheetTemplateContext;
import com.th4.edge.decoboon.material.model.MaterialAssistantAnswer;
import com.th4.edge.decoboon.material.model.MaterialAssistantCitation;
import com.th4.edge.decoboon.material.model.MaterialAssistantToolCall;
import com.th4.edge.decoboon.material.model.MaterialHealthReport;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbench;
import com.th4.edge.decoboon.material.model.MaterialPlanWorkbenchItem;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 只读材料业务助手。
 *
 * <p>回答只来自已发布蓝图、真实领域级 Provider 和确定性规则；不直接查库、不调用写 Provider、
 * 不做审批/付款/删除/权限变更，也不把模型推断写成业务事实。</p>
 */
@Component
public class MaterialAssistantContext {

    private static final int MAX_QUESTION_LENGTH = 500;
    private static final Set<EmployeeRole> COST_ROLES =
            Set.of(EmployeeRole.Boss, EmployeeRole.StoreManager, EmployeeRole.Finance);

    private final TenantContext tenantContext;
    private final MaterialAssistantKnowledge knowledge;
    private final MaterialAssistantIntentResolver resolver;
    private final ProjectAccessContext projectAccessContext;
    private final MaterialSheetContext sheetContext;
    private final MaterialSheetTemplateContext templateContext;
    private final MaterialHealthContext healthContext;
    private final MaterialPlanWorkbenchContext workbenchContext;

    public MaterialAssistantContext(TenantContext tenantContext,
                                    MaterialAssistantKnowledge knowledge,
                                    MaterialAssistantIntentResolver resolver,
                                    ProjectAccessContext projectAccessContext,
                                    MaterialSheetContext sheetContext,
                                    MaterialSheetTemplateContext templateContext,
                                    MaterialHealthContext healthContext,
                                    MaterialPlanWorkbenchContext workbenchContext) {
        this.tenantContext = tenantContext;
        this.knowledge = knowledge;
        this.resolver = resolver;
        this.projectAccessContext = projectAccessContext;
        this.sheetContext = sheetContext;
        this.templateContext = templateContext;
        this.healthContext = healthContext;
        this.workbenchContext = workbenchContext;
    }

    @Transactional(readOnly = true)
    public MaterialAssistantAnswer ask(Employee principal, String rawQuestion, Long projectId, Long sheetId) {
        Employee current = tenantContext.requireEmployee(principal);
        String question = rawQuestion == null ? "" : rawQuestion.trim();
        if (question.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "问题不能为空"));
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new DefinedException(new DefaultErrorCode("400", "问题过长，请控制在 500 字以内"));
        }
        MaterialAssistantIntent intent = resolver.resolve(question);
        LiveContext live = live(current, intent, projectId, sheetId);
        MaterialAssistantAnswer answer = switch (intent) {
            case SELECTION_GATE -> selection(question, live);
            case TIME_SEMANTICS -> timeSemantics(question, live);
            case SHEET_VS_PURCHASE -> sheetVsPurchase(question, live);
            case SHEET_STATUS -> sheetStatus(question, live);
            case PRICE_COST -> priceCost(question, current, live);
            case RECEIPT -> receipt(question, live);
            case CHANGE -> change(question, live);
            case SETTLEMENT -> settlement(question, live);
            case SUPPLIER -> supplier(question, live);
            case PROJECT_HEALTH -> projectHealth(question, current, live);
            case PERMISSION -> permission(question, current);
            case FIELD_DICTIONARY -> fieldDictionary(question);
            case OUT_OF_SCOPE -> refusal(question);
            case UNKNOWN -> unknown(question);
        };
        answer.setQuestion(question);
        answer.setIntent(intent.name());
        answer.setReadOnly(true);
        answer.setSuggestedQuestions(knowledge.suggestedQuestions(intent, 3));
        return answer;
    }

    private LiveContext live(Employee current, MaterialAssistantIntent intent, Long projectId, Long sheetId) {
        MaterialOrderSheet sheet = null;
        Long resolvedProjectId = projectId;
        List<String> missingFields = List.of();
        if (sheetId != null) {
            sheet = sheetContext.info(current, sheetId);
            Long sheetProjectId = sheet.getProject() == null ? null : sheet.getProject().getProjectId();
            if (resolvedProjectId != null && sheetProjectId != null && !resolvedProjectId.equals(sheetProjectId)) {
                throw new DefinedException(new DefaultErrorCode("404", "作业单不存在或无权访问"));
            }
            resolvedProjectId = resolvedProjectId == null ? sheetProjectId : resolvedProjectId;
            missingFields = missingTemplateFields(current, sheet);
        }
        Project project = resolvedProjectId == null ? null
                : projectAccessContext.requireReadableProject(resolvedProjectId, current);
        MaterialHealthReport health = null;
        MaterialPlanWorkbench workbench = null;
        if (resolvedProjectId != null && needsProjectData(intent)) {
            if (COST_ROLES.contains(current.getRole())) {
                health = healthContext.report(current, resolvedProjectId);
            } else {
                workbench = workbenchContext.summarize(current, resolvedProjectId, null).stream()
                        .findFirst().orElseGet(MaterialPlanWorkbench::new);
            }
        }
        return new LiveContext(project, sheet, health, workbench, missingFields);
    }

    private boolean needsProjectData(MaterialAssistantIntent intent) {
        return switch (intent) {
            case SELECTION_GATE, PRICE_COST, RECEIPT, CHANGE, SETTLEMENT, PROJECT_HEALTH -> true;
            default -> false;
        };
    }

    private List<String> missingTemplateFields(Employee current, MaterialOrderSheet sheet) {
        if (current.getCompany() == null || current.getCompany().getComId() == null) return List.of();
        List<MaterialSheetTemplateContext.FieldRequirement> required =
                templateContext.requiredHeaderFields(current.getCompany().getComId(), sheet.getSheetType());
        List<String> missing = new ArrayList<>();
        for (MaterialSheetTemplateContext.FieldRequirement field : required) {
            if (!hasHeaderValue(sheet, field.key())) missing.add(field.label());
        }
        return List.copyOf(missing);
    }

    private boolean hasHeaderValue(MaterialOrderSheet sheet, String key) {
        return switch (key) {
            case "manufacturerName" -> hasText(sheet.getManufacturerName());
            case "supplier" -> sheet.getSupplier() != null;
            case "measureAt" -> sheet.getMeasureAt() != null;
            case "groupChatAt" -> sheet.getGroupChatAt() != null;
            case "deliverAt" -> sheet.getDeliverAt() != null;
            case "installAt" -> sheet.getInstallAt() != null;
            case "remark" -> hasText(sheet.getRemark());
            default -> true;
        };
    }

    private MaterialAssistantAnswer selection(String question, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.ruleCitation("R-SELECTION-GATE"),
                knowledge.checkCitation("CHK-UNSELECTED"),
                knowledge.fieldCitation("MaterialSelection", "version/status"));
        String answer = "材料计划行进入作业单或采购前，合同选材来源必须有“已选材（Selected）”记录；进入采购后选材锁定，换材必须走增减项。"
                + liveSelection(live);
        return answer(question, "EXPLANATION", answer, 0.95, citations,
                List.of("未选材：由设计师完成选材并交付财务", "进入采购后不再改选材"), live);
    }

    private String liveSelection(LiveContext live) {
        if (live.health() != null) {
            return " 当前项目计划 " + live.health().getTotalItems() + " 条，已选材 "
                    + live.health().getSelectedItems() + " 条，未选材 " + live.health().getUnselectedItems()
                    + " 条；下一步：" + live.health().getNextAction() + "。";
        }
        if (live.workbench() != null) {
            int total = live.workbench().getTotalItems();
            int selected = (int) live.workbench().getItems().stream()
                    .filter(item -> "Selected".equals(item.getSelectionStatus())).count();
            return " 当前项目计划 " + total + " 条，已选材 " + selected + " 条，未选材 "
                    + Math.max(0, total - selected) + " 条。";
        }
        if (live.project() != null) {
            return " 当前项目已读取，但材料计划上下文暂不可确认；请在材料计划工作台查看选材状态。";
        }
        return " 如需判断某个项目，请提供 projectId 或在项目详情中提问。";
    }

    private MaterialAssistantAnswer timeSemantics(String question, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.fieldCitation("MaterialOrderSheet", "measureAt/groupChatAt/deliverAt/installAt"),
                knowledge.fieldCitation("PurchaseOrder", "expectedAt/orderedAt"),
                knowledge.fieldCitation("GoodsReceipt", "quantity/arrivedAt"),
                knowledge.gapCitation("GAP-MAT-TIME-SEMANTICS"));
        String answer = "测量日期是现场复尺/测量事实；拉群、配送、安装是现场的协作或计划日期；要求到场在材料计划/采购上，实际到场在 GoodsReceipt 上记录。"
                + " 建单时间使用 createTime，采购下单日期使用 orderedAt；是否新增独立“请购时间”仍是未决业务事项，当前不能写成已裁决字段。";
        if (live.sheet() != null) {
            MaterialOrderSheet sheet = live.sheet();
            answer += " 当前作业单：测量 " + date(sheet.getMeasureAt()) + "，配送 " + date(sheet.getDeliverAt())
                    + "，安装 " + date(sheet.getInstallAt()) + "。";
        }
        return answer(question, "EXPLANATION", answer, 0.95, citations,
                List.of("现场字段按当前品类模板显示", "要求到场/实际到场到材料计划或到场记录中确认"), live);
    }

    private MaterialAssistantAnswer sheetVsPurchase(String question, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.objectCitation("MaterialOrderSheet"),
                knowledge.ruleCitation("R-SHEET-NO-PRICE"),
                knowledge.ruleCitation("R-ONE-TO-MANY"),
                knowledge.objectCitation("PurchaseOrder"));
        String answer = "作业单对厂家，按品类承载安装位置、型号、颜色、尺寸、测量/配送/安装时间和签字；采购单对财务，承载供应商、价格、账期和应付。"
                + " 1 张作业单可以拆成 N 张采购单，作业单本身不带价格、账期和成本。";
        if (live.sheet() != null) {
            answer += " 当前作业单 " + live.sheet().getSheetNo() + " 的状态是 "
                    + statusName(live.sheet().getStatus()) + "，价格状态是 " + priceName(live.sheet().getPriceState()) + "。";
        }
        return answer(question, "EXPLANATION", answer, 0.97, citations,
                List.of("作业单继续由设计师/项目经理编排履约", "供应商与价格由财务在采购侧决定"), live);
    }

    private MaterialAssistantAnswer sheetStatus(String question, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.fieldCitation("MaterialOrderSheet", "status/remark"),
                knowledge.checkCitation("CHK-MEASURE-TEMPLATE"),
                knowledge.checkCitation("CHK-CANCEL-AFTER-ORDER"),
                knowledge.ruleCitation("R-SHEET-NO-PRICE"));
        if (live.sheet() == null) {
            return answer(question, "EXPLANATION",
                    "请提供当前作业单上下文（sheetId），我才能结合真实状态说明为什么不能继续。通用规则：未选材、模板必填字段缺失、超量会阻断提交请购；已进入采购后不能直接编辑或取消，换材走增减项。",
                    0.85, citations,
                    List.of("打开作业单详情重新提问", "检查选材、模板必填和采购状态"), live);
        }
        MaterialOrderSheet sheet = live.sheet();
        String missing = live.missingTemplateFields().isEmpty() ? "无"
                : String.join("、", live.missingTemplateFields());
        String answer = "当前作业单 " + sheet.getSheetNo() + " 状态为 " + statusName(sheet.getStatus())
                + "，价格状态为 " + priceName(sheet.getPriceState()) + "；测量 " + date(sheet.getMeasureAt())
                + "，配送 " + date(sheet.getDeliverAt()) + "，安装 " + date(sheet.getInstallAt()) + "。"
                + " 当前模板必填字段缺失：" + missing + "。"
                + " 下一步：" + sheetNextAction(sheet, live.missingTemplateFields());
        if (isLocked(sheet.getStatus())) {
            answer += " 该单已进入采购，材料和数量锁定；不能直接取消，换材走增减项，已到货扣减走退货。";
        }
        return answer(question, "LIVE_DATA", answer, 0.93, citations,
                List.of(sheetNextAction(sheet, live.missingTemplateFields())), live);
    }

    private MaterialAssistantAnswer priceCost(String question, Employee current, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.ruleCitation("R-PAYABLE-ON-ORDER"),
                knowledge.ruleCitation("R-P3-ACTUAL-COST"),
                knowledge.ruleCitation("R-COST-COMPLETE"),
                knowledge.checkCitation("CHK-UNPRICED"));
        String answer = "P1 报价成本价只用于预计成本；P2 采购价在下单时形成应付和暂估成本；P3 结算价是实际成本的唯一计价来源。"
                + " 无价下单允许，但会形成暂估应付；任一应计成本缺失时 costComplete=false，利润和毛利率必须为 null，不能用 0 成本或预计成本冒充实际利润。";
        if (live.health() != null) {
            MaterialHealthReport health = live.health();
            answer += " 当前项目材料应付 " + money(health.getMaterialCost()) + "，实际成本 " + money(health.getActualCost())
                    + "，成本状态：" + (health.isCostComplete() ? "完整" : "未完整（" + health.getCostIncompleteCount() + " 项）")
                    + "；利润：" + (health.getProfitAmount() == null ? "null（暂不确认）" : money(health.getProfitAmount())) + "。";
        } else if (live.project() != null) {
            answer += " 当前角色只能查看材料履约状态，成本和供应商价格仅对 Boss、店长、财务可见。";
        }
        List<String> next = live.health() != null && !live.health().isCostComplete()
                ? List.of("完成采购结算或补齐成本", "补齐前不展示利润和毛利率")
                : List.of("继续核对采购与结算金额");
        return answer(question, live.health() == null ? "EXPLANATION" : "LIVE_DATA", answer, 0.95, citations, next, live);
    }

    private MaterialAssistantAnswer receipt(String question, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.ruleCitation("R-RECEIPT-NO-PAYABLE"),
                knowledge.exceptionCitation("EX-PARTIAL-RECEIPT"),
                knowledge.fieldCitation("GoodsReceipt", "quantity/arrivedAt"));
        String answer = "到场确认按实际到场数量和日期登记，支持部分到场；到场不会新增应付，只回填账期并累计算实际成本。无价采购不阻断到场，成本仍保持不完整直到结算。";
        if (live.health() != null) {
            answer += " 当前项目待到货 " + live.health().getPendingArrival() + " 条，部分到场 "
                    + live.health().getPartiallyArrived() + " 条，逾期 " + live.health().getDelayed() + " 条。";
        } else if (live.workbench() != null) {
            answer += " 当前项目待下单 " + live.workbench().getReadyToOrder() + " 条，待到货 "
                    + live.workbench().getPendingArrival() + " 条，逾期 " + live.workbench().getDelayed() + " 条。";
        }
        return answer(question, live.health() == null ? "EXPLANATION" : "LIVE_DATA", answer, 0.94, citations,
                List.of("项目经理/设计师确认实际到场数量", "财务回填账期并在结算时收敛成本"), live);
    }

    private MaterialAssistantAnswer change(String question, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.ruleCitation("R-CHANGE-THREE-WAY"),
                knowledge.exceptionCitation("EX-CHANGE-AFTER-ORDER"),
                knowledge.objectCitation("MaterialReturn"));
        String answer = "减项按材料当前状态处理：未下单冲材料计划；已下单未到调整采购明细；已到货必须生成退货单，冲减应付、实际成本和库存。不得靠直接取消已下单作业单抹掉采购事实。";
        if (live.health() != null && live.health().getTotalItems() > 0) {
            answer += " 当前项目有 " + live.health().getTotalItems() + " 条材料计划，需按每条计划的采购/到场状态选择路径。";
        }
        return answer(question, "EXPLANATION", answer, 0.94, citations,
                List.of("先确认材料是否已下单/已到场", "已到货走退货，未到货调整采购或计划"), live);
    }

    private MaterialAssistantAnswer settlement(String question, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.ruleCitation("R-SETTLEMENT-EXCLUSIVE"),
                knowledge.ruleCitation("R-P3-ACTUAL-COST"));
        String answer = "逐单结算与月结并存，规则是互斥、逐单优先、月结兜底：同一采购明细只能被一种方式计入；月结只结算尚未逐单结算的差额。结算撤销走反向记录，释放明细后重新计算应付和实际成本，成本不完整时利润回到 null。";
        if (live.health() != null) {
            answer += " 当前项目结算状态：" + live.health().getSettlementStatus() + "。";
        }
        return answer(question, "EXPLANATION", answer, 0.94, citations,
                List.of("先逐单确认最终价", "期末只结未逐单结算差额"), live);
    }

    private MaterialAssistantAnswer supplier(String question, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.objectCitation("Partner"),
                knowledge.fieldCitation("PurchaseOrder", "supplier"),
                knowledge.ruleCitation("R-SHEET-NO-PRICE"));
        String answer = "系统里供应商和经销商统一是 Partner（合作方），按类型区分；作业单上的“厂家名称”是厂家文本或生产方，采购单上的 supplier 才是参与应付和账期的供应商。供应商与价格决策权在财务/采购，不在作业单上写死。";
        return answer(question, "EXPLANATION", answer, 0.95, citations,
                List.of("财务在合作方档案中选择供应商", "作业单只登记厂家和履约信息"), live);
    }

    private MaterialAssistantAnswer projectHealth(String question, Employee current, LiveContext live) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.checkCitation("CHK-UNSELECTED"),
                knowledge.checkCitation("CHK-UNPRICED"),
                knowledge.checkCitation("CHK-COST-INCOMPLETE"));
        if (live.project() == null) {
            return answer(question, "EXPLANATION",
                    "请提供 projectId，或在项目详情/项目账本打开材料体检；没有项目上下文时不能判断“这个项目缺什么”。",
                    0.8, citations, List.of("打开项目材料体检", "补充 projectId 后重试"), live);
        }
        if (live.health() != null) {
            MaterialHealthReport health = live.health();
            String issues = health.getIssues().isEmpty() ? "未命中已登记校核项"
                    : String.join("；", health.getIssues().stream().map(issue -> issue.getLabel() + " " + issue.getCount() + " 项").toList());
            String cost = " 成本状态：" + (health.isCostComplete() ? "完整" : "未完整（" + health.getCostIncompleteCount() + " 项）")
                    + "；利润：" + (health.getProfitAmount() == null ? "null（暂不确认）" : money(health.getProfitAmount())) + "。";
            String answer = "当前项目阶段：" + health.getStageLabel() + "。" + health.getStageSummary()
                    + " 材料计划 " + health.getTotalItems() + " 条，已选材 " + health.getSelectedItems()
                    + " 条，待下单 " + health.getReadyToOrder() + " 条，待到货 " + health.getPendingArrival()
                    + " 条，逾期 " + health.getDelayed() + " 条。校核项：" + issues + "。" + cost
                    + "下一步：" + health.getNextAction() + "。";
            return answer(question, "LIVE_DATA", answer, 0.94, citations, List.of(health.getNextAction()), live);
        }
        MaterialPlanWorkbench bench = live.workbench();
        String answer = "当前项目材料计划 " + bench.getTotalItems() + " 条，待下单 " + bench.getReadyToOrder()
                + " 条，待到货 " + bench.getPendingArrival() + " 条，逾期 " + bench.getDelayed()
                + " 条。当前角色可查看履约状态；成本和利润仅对 Boss、店长、财务展示。";
        return answer(question, "LIVE_DATA", answer, 0.9, citations, List.of("继续查看材料计划与到场状态"), live);
    }

    private MaterialAssistantAnswer permission(String question, Employee current) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.sourceCitation("BR-027", "权限与数据范围", "助手不扩大登录员工的数据范围，跨企业/跨项目请求拒绝。"),
                knowledge.exceptionCitation("EX-CROSS-TENANT"));
        String answer = "材料助手只读，并继承当前登录员工的数据范围，不信任前端传入的 companyId/empId/projectId。跨企业、跨项目和越权数据请求应返回 404/403；材料成本、供应商价格和利润仅 Boss、店长、财务可见。";
        return answer(question, "PERMISSION_DENIED", answer, 1.0, citations,
                List.of("仅查看当前员工有权访问的项目", "成本问题请由财务/负责人查看"), new LiveContext(null, null, null, null, List.of()));
    }

    private MaterialAssistantAnswer fieldDictionary(String question) {
        String normalized = question.toLowerCase();
        List<MaterialAssistantCitation> citations = new ArrayList<>();
        List<String> matches = new ArrayList<>();
        knowledge.blueprint().path("fields").forEach(field -> {
            String object = field.path("object").asText();
            String key = field.path("field").asText();
            String label = field.path("label").asText(key);
            if (normalized.contains(key.toLowerCase()) || normalized.contains(label.toLowerCase())
                    || (normalized.contains("status") && key.toLowerCase().contains("status"))
                    || (normalized.contains("pricestate") && key.toLowerCase().contains("pricestate"))) {
                citations.add(knowledge.fieldCitation(object, key));
                matches.add(label + "（" + object + "." + key + "）：" + field.path("meaning").asText(""));
            }
        });
        if (citations.isEmpty()) {
            return answer(question, "NO_EVIDENCE",
                    "已发布字段字典没有匹配到该字段。可以问“status、priceState、measureAt、lossRate 等字段是什么意思”。",
                    0.45, List.of(knowledge.sourceCitation("BR-044", "字段与模板边界", "字段含义必须先命中已发布字段字典。")),
                    List.of("换一个已登记字段名"), new LiveContext(null, null, null, null, List.of()));
        }
        return answer(question, "EXPLANATION",
                "字段字典：" + String.join("；", matches.stream().limit(3).toList()) + "。",
                0.9, citations.stream().limit(3).toList(),
                List.of("字段含义以蓝图字段字典为准"), new LiveContext(null, null, null, null, List.of()));
    }

    private MaterialAssistantAnswer refusal(String question) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.sourceCitation("READ-ONLY", "只读边界", "材料助手不执行审批、付款、删除或权限变更。"));
        return answer(question, "REFUSAL",
                "我只处理材料业务的解释、数据校核和下一步引导，不能自动审批、付款、删除数据或修改权限。需要这些动作时，请由有权限的人员在对应业务页面人工处理。",
                1.0, citations, List.of("打开对应业务页面由人工处理"), new LiveContext(null, null, null, null, List.of()));
    }

    private MaterialAssistantAnswer unknown(String question) {
        List<MaterialAssistantCitation> citations = List.of(
                knowledge.sourceCitation("KNOWLEDGE", "已发布蓝图", "当前问题没有命中已发布材料蓝图或实时业务上下文。"));
        return answer(question, "NO_EVIDENCE",
                "这个问题在已发布材料蓝图和当前项目上下文中没有足够证据，暂不可确认。请补充材料对象、项目或作业单上下文，或换一个材料业务问题。",
                0.35, citations, List.of("补充 projectId/sheetId", "换用材料计划、作业单、采购、到场或结算问题"),
                new LiveContext(null, null, null, null, List.of()));
    }

    private MaterialAssistantAnswer answer(String question, String type, String text, double confidence,
                                           List<MaterialAssistantCitation> citations,
                                           List<String> nextActions, LiveContext live) {
        MaterialAssistantAnswer answer = new MaterialAssistantAnswer();
        answer.setQuestion(question);
        answer.setAnswerType(type);
        answer.setAnswer(text);
        answer.setConfidence(confidence);
        answer.setCitations(citations);
        answer.setNextActions(nextActions);
        List<MaterialAssistantToolCall> tools = new ArrayList<>();
        tools.add(tool("blueprint.knowledge", "blueprints/material-v1.json", "OK", "published blueprint"));
        if (live.project() != null) tools.add(tool("project.access", "project/project/info", "OK", "current user/project"));
        if (live.sheet() != null) tools.add(tool("material.sheet.info", "material/sheet/info", "OK", "current user/sheet"));
        if (live.health() != null) tools.add(tool("business.check", "blueprint/check/report", "OK", "Boss/Store/Finance project"));
        if (live.workbench() != null) tools.add(tool("material.plan.workbench", "material/plan/workbench", "OK", "current user/project"));
        answer.setToolCalls(tools);
        return answer;
    }

    private MaterialAssistantToolCall tool(String name, String provider, String status, String scope) {
        MaterialAssistantToolCall call = new MaterialAssistantToolCall();
        call.setName(name);
        call.setProvider(provider);
        call.setStatus(status);
        call.setScope(scope);
        call.setReadOnly(true);
        return call;
    }

    private boolean isLocked(MaterialSheetStatus status) {
        return status == MaterialSheetStatus.Ordered || status == MaterialSheetStatus.PartiallyOrdered
                || status == MaterialSheetStatus.PartiallyArrived || status == MaterialSheetStatus.Arrived
                || status == MaterialSheetStatus.Settled || status == MaterialSheetStatus.Closed;
    }

    private String sheetNextAction(MaterialOrderSheet sheet, List<String> missing) {
        if (!missing.isEmpty()) return "先补齐模板必填字段：" + String.join("、", missing);
        return switch (sheet.getStatus()) {
            case Draft, Measuring, Measured -> "保存现场字段后提交请购";
            case Submitted, Pricing -> "等待财务核价并选择供应商下单";
            case PartiallyOrdered -> "继续完成剩余数量采购，已下单部分等到场";
            case Ordered -> "等待供应商送货，由项目经理/设计师确认到场";
            case PartiallyArrived -> "确认剩余到场数量，全部到齐后等待结算";
            case Arrived -> "等待财务结算和成本收敛";
            case Settled, Closed -> "材料已收口，仅查看历史";
            case Cancelled -> "已取消，保留历史；如需重新下单请从材料计划发起";
        };
    }

    private String statusName(MaterialSheetStatus status) {
        return status == null ? "未知" : status.getName();
    }

    private String priceName(SheetPriceState state) {
        return state == null ? "未知" : state.getName();
    }

    private String date(Object value) {
        return value == null ? "待确认" : value.toString();
    }

    private String money(BigDecimal value) {
        return value == null ? "暂不可确认" : "¥" + value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record LiveContext(Project project,
                               MaterialOrderSheet sheet,
                               MaterialHealthReport health,
                               MaterialPlanWorkbench workbench,
                               List<String> missingTemplateFields) {
    }
}

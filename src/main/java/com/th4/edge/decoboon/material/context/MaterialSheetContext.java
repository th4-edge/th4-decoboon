package com.th4.edge.decoboon.material.context;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.material.configure.MaterialSheetStatus;
import com.th4.edge.decoboon.material.configure.SheetItemSource;
import com.th4.edge.decoboon.material.configure.SheetPriceState;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetItemRepository;
import com.th4.edge.decoboon.material.repository.MaterialOrderSheetRepository;
import com.th4.edge.decoboon.project.configure.MaterialPlanStatus;
import com.th4.edge.decoboon.project.configure.PurchaseStatus;
import com.th4.edge.decoboon.project.context.MaterialPurchaseContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.context.ProjectPermissionContext;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.PurchaseOrder;
import com.th4.edge.decoboon.project.repository.MaterialPlanRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.PurchaseOrderRepository;
import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主材下单作业单：单据生命周期 + 门禁 + 下单编排（SOL-014 / OBJ-008 / BR-044 / BR-045 / SCN-021）。
 *
 * <p><b>单据边界（DEC-073）</b>：作业单对厂家（品类粒度，含测量/配送/安装时间与签字，**不带价格与账期**）；
 * 采购单对财务。1 张作业单 → N 张采购单。</p>
 *
 * <p><b>三道门禁</b>：① 行只能来自 {@code MaterialPlan}，且合同选材来源必须已选材（DEC-008 / DEC-056）；
 * ② 行数量不得超过"净用量 + 损耗"余额（DEC-MP-10）；③ 模板字段白名单（BR-044）。</p>
 *
 * <p><b>无价下单（DEC-072）</b>：价格不是下单前置条件；未定价行 {@code priceState=Unpriced}，
 * 价格留空（不是 0），形成暂估应付；实际成本在结算收敛前保持不完整。</p>
 */
@Component
public class MaterialSheetContext {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MaterialOrderSheetRepository sheetRepository;
    private final MaterialOrderSheetItemRepository itemRepository;
    private final MaterialPlanRepository materialPlanRepository;
    private final ProjectContractRepository contractRepository;
    private final PurchaseOrderRepository purchaseRepository;
    private final com.th4.edge.decoboon.supply.repository.PartnerRepository partnerRepository;
    private final ProjectAccessContext accessContext;
    private final ProjectPermissionContext permissionContext;
    private final ModelIdentityContext modelIdentityContext;
    private final TenantContext tenantContext;
    private final MaterialPurchaseContext materialPurchaseContext;
    private final MaterialSelectionContext materialSelectionContext;
    private final MaterialSheetTemplateContext templateContext;

    public MaterialSheetContext(MaterialOrderSheetRepository sheetRepository,
                                MaterialOrderSheetItemRepository itemRepository,
                                MaterialPlanRepository materialPlanRepository,
                                ProjectContractRepository contractRepository,
                                PurchaseOrderRepository purchaseRepository,
                                com.th4.edge.decoboon.supply.repository.PartnerRepository partnerRepository,
                                ProjectAccessContext accessContext,
                                ProjectPermissionContext permissionContext,
                                ModelIdentityContext modelIdentityContext,
                                TenantContext tenantContext,
                                MaterialPurchaseContext materialPurchaseContext,
                                MaterialSelectionContext materialSelectionContext,
                                MaterialSheetTemplateContext templateContext) {
        this.sheetRepository = sheetRepository;
        this.itemRepository = itemRepository;
        this.materialPlanRepository = materialPlanRepository;
        this.contractRepository = contractRepository;
        this.purchaseRepository = purchaseRepository;
        this.partnerRepository = partnerRepository;
        this.accessContext = accessContext;
        this.permissionContext = permissionContext;
        this.modelIdentityContext = modelIdentityContext;
        this.tenantContext = tenantContext;
        this.materialPurchaseContext = materialPurchaseContext;
        this.materialSelectionContext = materialSelectionContext;
        this.templateContext = templateContext;
    }

    // ------------------------------------------------------------------ 查询

    public List<MaterialOrderSheet> page(Employee principal, Long projectId, String sheetType, MaterialSheetStatus status) {
        Employee current = tenantContext.requireEmployee(principal);
        List<MaterialOrderSheet> rows = projectId == null
                ? sheetRepository.findByCompany_ComIdOrderBySheetIdDesc(current.getCompany().getComId())
                : sheetRepository.findByProject_ProjectIdOrderBySheetIdDesc(projectId).stream()
                        .filter(item -> item.getCompany().getComId().equals(current.getCompany().getComId()))
                        .toList();
        return rows.stream()
                .filter(item -> sheetType == null || sheetType.isBlank() || sheetType.equalsIgnoreCase(item.getSheetType()))
                .filter(item -> status == null || status == item.getStatus())
                .toList();
    }

    public MaterialOrderSheet info(Employee principal, Long sheetId) {
        Employee current = tenantContext.requireEmployee(principal);
        return requireSheet(current, sheetId);
    }

    public List<MaterialOrderSheetItem> items(Long sheetId) {
        return itemRepository.findBySheet_SheetIdOrderBySortAscItemIdAsc(sheetId);
    }

    /** 品类维度汇总（聚合口径，供工作台/报表使用）。 */
    public List<com.th4.edge.decoboon.material.model.MaterialSheetReportRow> report(Employee principal, Long projectId) {
        Employee current = tenantContext.requireEmployee(principal);
        List<MaterialOrderSheet> sheets = projectId == null
                ? sheetRepository.findByCompany_ComIdOrderBySheetIdDesc(current.getCompany().getComId())
                : sheetRepository.findByProject_ProjectIdOrderBySheetIdDesc(projectId);
        Map<String, com.th4.edge.decoboon.material.model.MaterialSheetReportRow> grouped = new LinkedHashMap<>();
        for (MaterialOrderSheet sheet : sheets) {
            if (sheet.getCompany() == null || !sheet.getCompany().getComId().equals(current.getCompany().getComId())) continue;
            com.th4.edge.decoboon.material.model.MaterialSheetReportRow row =
                    grouped.computeIfAbsent(sheet.getSheetType(), key -> {
                        com.th4.edge.decoboon.material.model.MaterialSheetReportRow created =
                                new com.th4.edge.decoboon.material.model.MaterialSheetReportRow();
                        created.setSheetType(key);
                        return created;
                    });
            row.setSheetCount(row.getSheetCount() + 1);
            if (sheet.getPriceState() == null || sheet.getPriceState() == SheetPriceState.Unpriced) {
                row.setUnpricedSheetCount(row.getUnpricedSheetCount() + 1);
            }
            if (sheet.getStatus() != null && sheet.getStatus().ordered()) {
                row.setOrderedSheetCount(row.getOrderedSheetCount() + 1);
            }
        }
        return new ArrayList<>(grouped.values());
    }

    // ------------------------------------------------------------------ 单据生命周期

    /** 新建作业单（草稿）：行只能挂材料计划行，且立即执行门禁校验。 */
    public MaterialOrderSheet save(Employee principal, Long projectId, String sheetType, String manufacturerName,
                                   Long supplierId, LocalDate groupChatAt, LocalDate installAt, String remark,
                                   List<String> attachments, List<MaterialOrderSheetItem> lines) {
        Employee current = requireDesignerOrManager(principal);
        if (projectId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少项目"));
        }
        if (sheetType == null || sheetType.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少单据类型（品类）"));
        }
        Project project = requireProject(current, projectId);
        ProjectContract contract = contractRepository.findByProject(project)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "主材下单必须来源已生效合同")));

        MaterialOrderSheet sheet = new MaterialOrderSheet();
        sheet.setCompany(project.getCompany());
        sheet.setProperty(project.getProperty());
        sheet.setProject(project);
        sheet.setContract(contract);
        sheet.setSheetType(sheetType.trim());
        sheet.setSheetNo(nextSheetNo(current, sheetType.trim()));
        MaterialSheetTemplate template = templateContext.templateForIssue(current.getCompany().getComId(), sheetType.trim());
        sheet.setTemplate(null);
        sheet.setTemplateVersion(template == null ? null : template.getVersion());
        permissionContext.apply(sheet, project);
        sheet.setManufacturerName(trimTo(manufacturerName, 128));
        sheet.setSupplier(requireSupplier(current, supplierId, null));
        sheet.setGroupChatAt(groupChatAt);
        sheet.setInstallAt(installAt);
        sheet.setRemark(trimTo(remark, 500));
        sheet.setStatus(MaterialSheetStatus.Draft);
        sheet.setPriceState(SheetPriceState.Unpriced);
        sheet.setCreatedBy(current);
        sheet.setAttachments(attachments == null || attachments.isEmpty() ? null : new ArrayList<>(attachments));
        MaterialOrderSheet saved = sheetRepository.save(modelIdentityContext.prepare(sheet));

        replaceLines(current, saved, lines);
        return saved;
    }

    /** 更新作业单：抬头 + 行整体替换；仅未进入采购的状态可改（DEC-008 / DEC-056）。 */
    public MaterialOrderSheet update(Employee principal, Long sheetId, String manufacturerName, Long supplierId,
                                     LocalDate groupChatAt, LocalDate deliverAt, LocalDate installAt, String remark,
                                     List<String> attachments, List<MaterialOrderSheetItem> lines) {
        Employee current = requireDesignerOrManager(principal);
        MaterialOrderSheet sheet = requireSheet(current, sheetId);
        requireEditable(sheet);
        if (manufacturerName != null) sheet.setManufacturerName(trimTo(manufacturerName, 128));
        if (supplierId != null) sheet.setSupplier(requireSupplier(current, supplierId, null));
        if (groupChatAt != null) sheet.setGroupChatAt(groupChatAt);
        if (deliverAt != null) sheet.setDeliverAt(deliverAt);
        if (installAt != null) sheet.setInstallAt(installAt);
        if (remark != null) sheet.setRemark(trimTo(remark, 500));
        if (attachments != null) sheet.setAttachments(attachments.isEmpty() ? null : new ArrayList<>(attachments));
        MaterialOrderSheet saved = sheetRepository.save(sheet);
        if (lines != null) {
            replaceLines(current, saved, lines);
        }
        return saved;
    }

    /** 删除作业单：仅 Draft / Cancelled 可删（已下单必须走采购调整或退货）。 */
    public void delete(Employee principal, Long sheetId) {
        Employee current = requireDesignerOrManager(principal);
        MaterialOrderSheet sheet = requireSheet(current, sheetId);
        if (sheet.getStatus() != MaterialSheetStatus.Draft && sheet.getStatus() != MaterialSheetStatus.Cancelled) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "已进入采购的作业单不能删除；请先取消（并走采购调整或退货）"));
        }
        itemRepository.deleteBySheet_SheetId(sheetId);
        sheetRepository.delete(sheet);
    }

    /** 提交请购：整单重新校验门禁与数量，然后进入材料员/财务队列。 */
    public MaterialOrderSheet submit(Employee principal, Long sheetId) {
        Employee current = requireDesignerOrManager(principal);
        MaterialOrderSheet sheet = requireSheet(current, sheetId);
        requireEditable(sheet);
        List<MaterialOrderSheetItem> lines = items(sheetId);
        if (lines.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "作业单还没有材料行"));
        }
        for (MaterialOrderSheetItem line : lines) {
            requireOrderablePlan(current, line.getMaterialPlan() == null ? null : line.getMaterialPlan().getPlanId());
        }
        sheet.setStatus(defaultAfter(sheet, MaterialSheetStatus.Submitted));
        return sheetRepository.save(sheet);
    }

    /** 测量与时间确认（项目经理/设计师，DEC-MP-07 任一方即可）。 */
    public MaterialOrderSheet measure(Employee principal, Long sheetId, LocalDate measureAt, LocalDate deliverAt,
                                      LocalDate installAt, String signatures) {
        Employee current = requireDesignerOrManager(principal);
        MaterialOrderSheet sheet = requireSheet(current, sheetId);
        if (measureAt != null) sheet.setMeasureAt(measureAt);
        if (deliverAt != null) sheet.setDeliverAt(deliverAt);
        if (installAt != null) sheet.setInstallAt(installAt);
        if (signatures != null && !signatures.isBlank()) sheet.setSignatures(signatures.trim());
        if (sheet.getStatus() == MaterialSheetStatus.Draft || sheet.getStatus() == MaterialSheetStatus.Measuring) {
            sheet.setStatus(sheet.getMeasureAt() == null ? MaterialSheetStatus.Measuring : MaterialSheetStatus.Measured);
        }
        return sheetRepository.save(sheet);
    }

    /** 取消作业单：已下单后取消必须走采购调整或退货（DEC-MP-02）。 */
    public MaterialOrderSheet cancel(Employee principal, Long sheetId, String reason) {
        Employee current = requireSheetOperator(principal);
        MaterialOrderSheet sheet = requireSheet(current, sheetId);
        if (sheet.getStatus() != null && sheet.getStatus().ordered()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "已进入采购的作业单不能直接取消；请走采购调整或退货"));
        }
        sheet.setStatus(MaterialSheetStatus.Cancelled);
        sheet.setRemark(reason == null || reason.isBlank() ? sheet.getRemark() : ("取消原因：" + reason.trim()));
        return sheetRepository.save(sheet);
    }

    // ------------------------------------------------------------------ 下单编排

    /**
     * 由作业单下单：一供应商一次调用；未定价行允许 {@code unitPrice} 为空（→ 暂估应付）。
     *
     * @param lines 至少要给 itemId 与数量；unitPrice 为空表示未定价
     */
    public MaterialOrderSheet order(Employee principal, Long sheetId, Long supplierId, String supplierName,
                                    com.th4.edge.decoboon.project.configure.DeliveryMode deliveryMode,
                                    LocalDate expectedAt, String remark, List<MaterialOrderSheetItem> lines) {
        Employee current = accessContext.requireEmployee(principal);
        materialPurchaseContext.requireOperator(current);
        MaterialOrderSheet sheet = requireSheet(current, sheetId);
        if (sheet.getStatus() == MaterialSheetStatus.Cancelled || sheet.getStatus() == MaterialSheetStatus.Closed) {
            throw new DefinedException(new DefaultErrorCode("400", "该作业单已取消或已关闭，不能下单"));
        }
        List<MaterialOrderSheetItem> sheetLines = items(sheetId);
        if (sheetLines.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "作业单还没有材料行"));
        }
        if (lines == null || lines.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择要下单的材料行"));
        }
        var supplier = requireSupplier(current, supplierId, supplierName);
        List<MaterialOrderSheetItem> targets = new ArrayList<>();
        for (MaterialOrderSheetItem command : lines) {
            MaterialOrderSheetItem line = sheetLines.stream()
                    .filter(item -> item.getItemId().equals(command.getItemId()))
                    .findFirst()
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "作业单行不存在或不属于本单")));
            BigDecimal quantity = command.getQuantity() == null ? line.getQuantity() : command.getQuantity();
            MaterialPlan plan = requireOrderablePlan(current, line.getMaterialPlan() == null ? null : line.getMaterialPlan().getPlanId());
            materialPurchaseContext.requireOrderQuantity(plan, quantity);

            BigDecimal unitPrice = command.getPurchasePrice();
            SheetPriceState priceState = unitPrice == null ? SheetPriceState.Unpriced : SheetPriceState.Priced;
            PurchaseOrder purchase = new PurchaseOrder();
            purchase.setCompany(plan.getProject().getCompany());
            purchase.setProperty(plan.getProject().getProperty());
            purchase.setProject(plan.getProject());
            purchase.setContract(sheet.getContract());
            purchase.setMaterialPlan(plan);
            purchase.setQuoteLine(plan.getQuoteItem());
            purchase.setSupplier(supplier);
            purchase.setSupplierName(supplier.getName());
            purchase.setDeliveryMode(deliveryMode == null
                    ? com.th4.edge.decoboon.project.configure.DeliveryMode.Direct : deliveryMode);
            purchase.setNo("PO" + System.currentTimeMillis() + "-" + line.getItemId());
            purchase.setQuantity(quantity);
            purchase.setUnit(line.getUnit());
            purchase.setUnitPrice(unitPrice == null ? BigDecimal.ZERO : unitPrice);
            purchase.setAmount(unitPrice == null ? BigDecimal.ZERO : quantity.multiply(unitPrice));
            purchase.setReceivedQuantity(BigDecimal.ZERO);
            purchase.setStatus(PurchaseStatus.Approved);
            purchase.setApprovedBy(current);
            purchase.setApprovedAt(LocalDateTime.now());
            purchase.setApprovalRemark(unitPrice == null ? "无价下单（暂估应付）" : "按作业单下单");
            purchase.setPriceState(priceState);
            purchase.setExpectedAt(expectedAt == null ? sheet.getDeliverAt() : expectedAt);
            purchase.setCreatedBy(current);
            purchase.setRemark(remark);
            PurchaseOrder savedPurchase = purchaseRepository.save(modelIdentityContext.prepare(purchase));
            materialPurchaseContext.confirmOrder(savedPurchase, current);

            line.setPurchasePrice(unitPrice);
            line.setPriceState(priceState);
            line.setOrderedQuantity(value(line.getOrderedQuantity()).add(quantity));
            itemRepository.save(line);
            targets.add(line);
        }
        refreshSheetPriceState(sheet, sheetLines);
        sheet.setStatus(sheetLines.stream().allMatch(line -> value(line.getOrderedQuantity()).signum() > 0)
                ? MaterialSheetStatus.Ordered : MaterialSheetStatus.PartiallyOrdered);
        return sheetRepository.save(sheet);
    }

    /**
     * 到场后回写作业单状态（OBJ-008 映射表：Ordered → PartiallyArrived → Arrived）。
     *
     * <p>**单向推进、只进不退**：由采购到场事实驱动，作业单不反向改写采购或计划事实。
     * 到场量达到该行已下单量即视为已到场；未下单的行不受影响。</p>
     */
    public void applyReceipt(MaterialPlan plan) {
        if (plan == null || plan.getPlanId() == null) {
            return;
        }
        List<com.th4.edge.decoboon.project.model.PurchaseOrder> orders =
                purchaseRepository.findByMaterialPlanOrderByPurchaseIdAsc(plan);
        BigDecimal received = orders.stream()
                .map(order -> value(order.getReceivedQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (received.signum() <= 0) {
            return;
        }
        for (MaterialOrderSheetItem line : itemRepository.findByMaterialPlan_PlanId(plan.getPlanId())) {
            MaterialOrderSheet sheet = line.getSheet();
            if (sheet == null) continue;
            MaterialSheetStatus status = sheet.getStatus();
            if (status != MaterialSheetStatus.Ordered && status != MaterialSheetStatus.PartiallyOrdered
                    && status != MaterialSheetStatus.PartiallyArrived) {
                continue;
            }
            BigDecimal ordered = value(line.getOrderedQuantity());
            if (ordered.signum() <= 0) continue;
            MaterialSheetStatus next = received.compareTo(ordered) >= 0
                    ? MaterialSheetStatus.Arrived : MaterialSheetStatus.PartiallyArrived;
            if (next == MaterialSheetStatus.PartiallyArrived && status == MaterialSheetStatus.PartiallyArrived) {
                continue;
            }
            sheet.setStatus(next);
            sheetRepository.save(sheet);
        }
    }

    // ------------------------------------------------------------------ 校验

    /** 单行校验：安装位置、数量、单位与损耗口径。 */
    public void validateLine(MaterialOrderSheetItem line) {
        if (line == null) {
            throw new DefinedException(new DefaultErrorCode("400", "作业单行不能为空"));
        }
        if (line.getMaterialPlan() == null || line.getMaterialPlan().getPlanId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "作业单行必须来源材料计划；合同外主材请先发起增减项"));
        }
        if (line.getQuantity() == null || line.getQuantity().signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "作业单行数量必须大于 0"));
        }
        if (line.getUnit() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "作业单行缺少单位"));
        }
        if (line.getLocationName() == null || line.getLocationName().isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "作业单行缺少安装位置"));
        }
    }

    /** 行所属计划必须可见且满足选材门禁。 */
    public MaterialPlan requireOrderablePlan(Employee current, Long planId) {
        if (planId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少材料计划"));
        }
        MaterialPlan plan = materialPlanRepository.findOne(
                        accessContext.visibleChild(current, MaterialPlan.class, "project")
                                .and((root, query, builder) -> builder.equal(root.get("planId"), planId)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "材料计划不存在或无权访问")));
        materialSelectionContext.requireOrderableSelection(plan);
        return plan;
    }

    // ------------------------------------------------------------------ 内部

    private void replaceLines(Employee current, MaterialOrderSheet sheet, List<MaterialOrderSheetItem> lines) {
        if (lines == null) return;
        itemRepository.deleteBySheet_SheetId(sheet.getSheetId());
        int sort = 0;
        for (MaterialOrderSheetItem line : lines) {
            MaterialPlan plan = requireOrderablePlan(current, line.getMaterialPlan() == null
                    ? null : line.getMaterialPlan().getPlanId());
            validateLine(line);
            materialPurchaseContext.requireOrderQuantity(plan, line.getQuantity());
            MaterialOrderSheetItem target = new MaterialOrderSheetItem();
            target.setCompany(sheet.getCompany());
            target.setProperty(sheet.getProperty());
            target.setProject(sheet.getProject());
            target.setSheet(sheet);
            permissionContext.apply(target, sheet.getProject());
            target.setSourceKind(SheetItemSource.Plan);
            target.setMaterialPlan(plan);
            target.setStandardIncluded(Boolean.TRUE.equals(line.getStandardIncluded()));
            target.setSpaceName(trimTo(line.getSpaceName(), 128));
            target.setLocationName(trimTo(line.getLocationName(), 128));
            target.setMaterialName(trimTo(line.getMaterialName(), 128));
            target.setBrand(trimTo(line.getBrand(), 128));
            target.setModel(trimTo(line.getModel(), 128));
            target.setSpec(trimTo(line.getSpec(), 256));
            target.setColor(trimTo(line.getColor(), 128));
            target.setSize(trimTo(line.getSize(), 128));
            target.setCraft(trimTo(line.getCraft(), 128));
            target.setGlass(trimTo(line.getGlass(), 128));
            target.setQuantity(line.getQuantity());
            target.setUnit(line.getUnit() == null ? plan.getUnit() : line.getUnit());
            target.setLossRate(value(plan.getLossRate()));
            target.setLossQuantity(value(plan.getLossQuantity()));
            target.setQuoteCostPrice(quoteCostUnitPrice(plan));
            target.setOrderedQuantity(BigDecimal.ZERO);
            target.setPriceState(SheetPriceState.Unpriced);
            target.setSort(sort++);
            target.setRemark(trimTo(line.getRemark(), 500));
            itemRepository.save(modelIdentityContext.prepare(target));
        }
        refreshSheetPriceState(sheet, itemRepository.findBySheet_SheetIdOrderBySortAscItemIdAsc(sheet.getSheetId()));
    }

    /**
     * 作业单行的"报价成本价快照"：取报价项目库标准成本（口径为报价侧成本，只读，用于预计成本对照）。
     * 缺报价项目库来源时留空，不臆造。
     */
    private BigDecimal quoteCostUnitPrice(MaterialPlan plan) {
        if (plan.getCatalogItem() != null && plan.getCatalogItem().getCostPrice() != null) {
            return plan.getCatalogItem().getCostPrice();
        }
        return null;
    }

    private void refreshSheetPriceState(MaterialOrderSheet sheet, List<MaterialOrderSheetItem> lines) {
        if (lines == null || lines.isEmpty()) {
            sheet.setPriceState(SheetPriceState.Unpriced);
            return;
        }
        long priced = lines.stream().filter(line -> line.getPriceState() == SheetPriceState.Priced
                || line.getPriceState() == SheetPriceState.Settled).count();
        if (priced == 0) {
            sheet.setPriceState(SheetPriceState.Unpriced);
        } else if (priced == lines.size()) {
            sheet.setPriceState(SheetPriceState.Priced);
        } else {
            sheet.setPriceState(SheetPriceState.Mixed);
        }
    }

    private MaterialSheetStatus defaultAfter(MaterialOrderSheet sheet, MaterialSheetStatus target) {
        return sheet.getMeasureAt() == null ? target : target;
    }

    private void requireEditable(MaterialOrderSheet sheet) {
        if (sheet.getStatus() == null || !sheet.getStatus().editable()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该作业单已进入采购（" + (sheet.getStatus() == null ? "未知" : sheet.getStatus().getName())
                            + "），不能再修改；如需更换材料请发起增减项"));
        }
    }

    private MaterialOrderSheet requireSheet(Employee current, Long sheetId) {
        if (sheetId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "缺少作业单索引"));
        }
        return sheetRepository.findOne(accessContext.visibleChild(current, MaterialOrderSheet.class, "project")
                        .and((root, query, builder) -> builder.equal(root.get("sheetId"), sheetId)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "作业单不存在或无权访问")));
    }

    private Project requireProject(Employee current, Long projectId) {
        return accessContext.requireProject(projectId, current);
    }

    /**
     * 解析供应商：供应商与价格的决策权在财务/采购（DEC-MP-03）。
     * 作业单下单只接受**已建档**供应商（按 id 或精确名称），不在下单时静默新建合作方。
     */
    private com.th4.edge.decoboon.supply.model.Partner requireSupplier(Employee current, Long supplierId, String supplierName) {
        Long comId = current.getCompany().getComId();
        if (supplierId != null) {
            return partnerRepository.findById(supplierId)
                    .filter(item -> item.getCompany() != null && item.getCompany().getComId().equals(comId))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "供应商不存在或无权访问")));
        }
        if (supplierName != null && !supplierName.isBlank()) {
            com.th4.edge.decoboon.supply.model.Partner partner =
                    partnerRepository.findByCompany_ComIdAndName(comId, supplierName.trim());
            if (partner != null) return partner;
            throw new DefinedException(new DefaultErrorCode("404", "供应商不存在，请先在合作方中建档：" + supplierName.trim()));
        }
        return null;
    }

    private Employee requireDesignerOrManager(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        EmployeeRole role = current.getRole();
        if (role != EmployeeRole.Designer && role != EmployeeRole.Boss && role != EmployeeRole.StoreManager) {
            throw new DefinedException(new DefaultErrorCode("403", "只有设计师、企业负责人或店长可以维护主材下单作业单"));
        }
        return current;
    }

    private Employee requireSheetOperator(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        EmployeeRole role = current.getRole();
        if (role != EmployeeRole.Designer && role != EmployeeRole.Boss
                && role != EmployeeRole.StoreManager && role != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "当前岗位不能操作主材下单作业单"));
        }
        return current;
    }

    private String nextSheetNo(Employee current, String sheetType) {
        String prefix = sheetType.toUpperCase() + "-" + LocalDate.now().format(DAY) + "-";
        return sheetRepository.findFirstByCompany_ComIdAndSheetNoStartingWithOrderBySheetNoDesc(
                        current.getCompany().getComId(), prefix)
                .map(last -> {
                    String tail = last.getSheetNo().substring(prefix.length());
                    int next = Integer.parseInt(tail) + 1;
                    return prefix + String.format("%03d", next);
                })
                .orElse(prefix + "001");
    }

    private String trimTo(String value, int max) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    private BigDecimal value(BigDecimal input) {
        return input == null ? BigDecimal.ZERO : input;
    }
}

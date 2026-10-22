package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.CatalogImportRowStatus;
import com.th4.edge.decoboon.catalog.configure.CatalogSourceType;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.catalog.model.CatalogImportRow;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 报价项目库批量导入校验上下文（PRD §5、§6）。
 *
 * <p>表头按名称识别，不按列顺序猜测；行级错误带字段、错误码和原值，持久化到
 * {@link CatalogImportRow} 供预览与结果页分页展示。成本空白保持 {@code null}
 * （成本待维护），0 表示明确零成本；固定总价写入独立字段，不落到材料/人工单价。</p>
 */
@Component
public class CatalogImportContext {

    /** 模板版本（V1.0 表头签名即版本标识）。 */
    public static final String TEMPLATE_VERSION = "V1.0";
    /** 校验版本：预览与确认必须一致，防止预览口径与写入口径漂移。 */
    public static final String VALIDATION_VERSION = "V1.0-r1";
    /** 预览过期时长（PRD §7：默认 24 小时）。 */
    public static final int EXPIRE_HOURS = 24;
    /** 预览摘要行数（PRD §4.3 前 20 条）。 */
    public static final int PREVIEW_LIMIT = 20;

    private static final Map<String, String> HEADERS = new LinkedHashMap<>();
    private static final Map<String, ItemType> TYPE_ALIAS = new HashMap<>();
    private static final Map<String, PricingMethod> MODE_ALIAS = new HashMap<>();
    private static final Map<String, Unit> UNIT_ALIAS = new HashMap<>();

    static {
        HEADERS.put("项目类型", "type");
        HEADERS.put("项目编码", "code");
        HEADERS.put("项目分类", "category");
        HEADERS.put("项目名称", "name");
        HEADERS.put("品牌", "brand");
        HEADERS.put("系列", "series");
        HEADERS.put("规格/型号", "spec");
        HEADERS.put("计量单位", "unit");
        HEADERS.put("计价方式", "pricingMode");
        HEADERS.put("材料单价", "materialPrice");
        HEADERS.put("人工单价", "manualPrice");
        HEADERS.put("固定总价", "fixedTotalPrice");
        HEADERS.put("标准成本单价", "costPrice");
        HEADERS.put("施工工期", "durationDays");
        HEADERS.put("损耗率", "lossRate");
        HEADERS.put("起订量", "minimumOrder");
        HEADERS.put("交期", "leadTimeDays");
        HEADERS.put("保修月数", "warrantyMonths");
        HEADERS.put("柜体板材", "board");
        HEADERS.put("台面", "countertop");
        HEADERS.put("施工工艺", "craft");
        HEADERS.put("验收标准", "criterion");
        HEADERS.put("项目说明", "description");

        for (ItemType type : ItemType.values()) {
            TYPE_ALIAS.put(type.getName(), type);
            TYPE_ALIAS.put(type.name(), type);
        }
        TYPE_ALIAS.put("软装", ItemType.Soft);
        TYPE_ALIAS.put("电器", ItemType.Appliance);
        TYPE_ALIAS.put("综合", ItemType.General);

        for (PricingMethod mode : PricingMethod.values()) {
            MODE_ALIAS.put(mode.getName(), mode);
            MODE_ALIAS.put(mode.name(), mode);
        }
        for (Unit unit : Unit.values()) {
            UNIT_ALIAS.put(unit.getName(), unit);
            UNIT_ALIAS.put(unit.name(), unit);
        }
        UNIT_ALIAS.put("㎡", Unit.Area);
        UNIT_ALIAS.put("m²", Unit.Area);
        UNIT_ALIAS.put("m2", Unit.Area);
        UNIT_ALIAS.put("平米", Unit.Area);
        UNIT_ALIAS.put("平方米", Unit.Area);
        UNIT_ALIAS.put("m", Unit.Metre);
        UNIT_ALIAS.put("米", Unit.Metre);
        UNIT_ALIAS.put("cm", Unit.Centimeter);
        UNIT_ALIAS.put("厘米", Unit.Centimeter);
        UNIT_ALIAS.put("只", Unit.Count);
    }

    private final CatalogItemRepository repository;
    private final TenantContext tenantContext;

    public CatalogImportContext(CatalogItemRepository repository, TenantContext tenantContext) {
        this.repository = repository;
        this.tenantContext = tenantContext;
    }

    /** 批量导入 P0 仅企业负责人可发起（历史入口保留）。 */
    public Employee requireBoss(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        if (current.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人可以批量导入报价项目库"));
        }
        return current;
    }

    /**
     * 报价项目库属于企业级主数据：BOSS、店长与财务均可批量导入。
     * 导入结果影响全企业，页面需提示影响范围并保留审计。
     */
    public Employee requireMaintainer(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        if (current.getRole() != EmployeeRole.Boss
                && current.getRole() != EmployeeRole.StoreManager
                && current.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "只有企业负责人、店长或财务可以批量导入报价项目库"));
        }
        return current;
    }

    /** 生成批次号：CI-时间戳-随机 4 位。 */
    public String nextBatchNo() {
        return "CI-" + java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now())
                + "-" + String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000));
    }

    /** 预览过期时间（默认 24 小时）。 */
    public java.time.LocalDateTime expireAt() {
        return java.time.LocalDateTime.now().plusHours(EXPIRE_HOURS);
    }

    /** 校验计划（内部值对象）：行记录 + 可导入项目 + 统计 + 摘要。 */
    public record Plan(List<CatalogImportRow> rows,
                       List<CatalogItem> readyItems,
                       int totalRows,
                       int readyRows,
                       int duplicateRows,
                       int suspectedDuplicateRows,
                       int errorRows,
                       int costPendingRows,
                       List<String> samples,
                       String message) { }

    /** 行级校验失败（内部异常）：字段 + 错误码 + 原因 + 原值。 */
    private static class RowRejected extends RuntimeException {
        private final String field;
        private final String code;
        private final String value;

        RowRejected(String field, String code, String message, String value) {
            super(message);
            this.field = field;
            this.code = code;
            this.value = value;
        }
    }

    /**
     * 解析并校验全部数据行；预览与确认共用本实现，保证两阶段口径一致。
     *
     * @param importSuspected 疑似重复是否按新增导入（批次级 duplicatePolicy=Import）
     */
    public Plan plan(Employee current, CatalogImportParser.ParsedSheet sheet, boolean importSuspected) {
        Map<String, Integer> columns = resolveColumns(sheet.header());
        Long comId = current.getCompany().getComId();
        Map<String, CatalogItem> existingByCode = new HashMap<>();
        Map<String, CatalogItem> existingByName = new HashMap<>();
        for (CatalogItem item : repository.findByCompany_ComId(comId)) {
            if (item.getType() != null && item.getCode() != null && !item.getCode().isBlank()) {
                existingByCode.putIfAbsent(codeKey(item.getType(), item.getCode()), item);
            }
            existingByName.putIfAbsent(nameKey(item), item);
        }

        List<CatalogImportRow> rows = new ArrayList<>();
        List<CatalogItem> readyItems = new ArrayList<>();
        List<String> samples = new ArrayList<>();
        Map<String, Integer> seenCodes = new HashMap<>();
        Map<String, Integer> seenNames = new HashMap<>();
        int duplicateRows = 0;
        int suspectedRows = 0;
        int errorRows = 0;
        int costPendingRows = 0;
        int ignoredTypeFields = 0;
        int rowNo = 1;

        for (List<String> cells : sheet.rows()) {
            rowNo++;
            CatalogImportRow row = new CatalogImportRow();
            row.setCompany(current.getCompany());
            row.setRowNo(rowNo);
            row.setRawData(String.join("｜", cells));
            try {
                CatalogItem item = parseRow(cells, columns, current);
                summarize(row, item);
                String codeKey = item.getCode() == null || item.getCode().isBlank()
                        ? null : codeKey(item.getType(), item.getCode());
                String nameKey = nameKey(item);
                CatalogItem dbSameCode = codeKey == null ? null : existingByCode.get(codeKey);
                CatalogItem dbSameName = existingByName.get(nameKey);

                // 去重键：有编码按“企业+类型+编码”；无编码才按“类型+名称+品牌+规格+单位”。
                if (codeKey != null) {
                    if (dbSameCode != null && !sameIdentity(dbSameCode, nameKey)) {
                        reject(row, "项目编码", "C-03",
                                "项目编码已存在：编码 " + dbSameCode.getCode() + " 属于已有项目「"
                                        + dbSameCode.getName() + "」", item.getCode());
                        errorRows++;
                        rows.add(row);
                        continue;
                    }
                    if (dbSameCode != null) {
                        row.setStatus(CatalogImportRowStatus.Duplicate);
                        rejectFields(row, "项目编码", "C-02",
                                "企业内已存在相同项目（" + dbSameCode.getName() + "），默认跳过", item.getCode());
                        duplicateRows++;
                        rows.add(row);
                        continue;
                    }
                    if (seenCodes.containsKey(codeKey)) {
                        row.setStatus(CatalogImportRowStatus.Duplicate);
                        rejectFields(row, "项目编码", "C-01",
                                "文件内重复：与第 " + seenCodes.get(codeKey) + " 行重复，保留第一条", item.getCode());
                        duplicateRows++;
                        rows.add(row);
                        continue;
                    }
                    seenCodes.put(codeKey, rowNo);
                    seenNames.putIfAbsent(nameKey, rowNo);
                } else {
                    if (seenNames.containsKey(nameKey)) {
                        row.setStatus(CatalogImportRowStatus.Duplicate);
                        rejectFields(row, "项目名称", "C-01",
                                "文件内重复：与第 " + seenNames.get(nameKey) + " 行重复，保留第一条", item.getName());
                        duplicateRows++;
                        rows.add(row);
                        continue;
                    }
                    if (dbSameName != null) {
                        boolean seriesDiffers = !equalsText(dbSameName.getSeries(), item.getSeries());
                        if (seriesDiffers) {
                            if (importSuspected) {
                                row.setStatus(CatalogImportRowStatus.Ready);
                                row.setErrorField(null);
                                row.setErrorCode(null);
                                row.setErrorReason("疑似重复（系列不同），已按批次策略作为新增导入");
                                if (item.getCostPrice() == null) {
                                    costPendingRows++;
                                }
                                readyItems.add(item);
                                seenNames.put(nameKey, rowNo);
                                rows.add(row);
                                continue;
                            }
                            row.setStatus(CatalogImportRowStatus.SuspectedDuplicate);
                            rejectFields(row, "系列", "C-01",
                                    "疑似重复：已有项目「" + dbSameName.getName() + "」系列为 "
                                            + textOrEmpty(dbSameName.getSeries()) + "，本行系列为 "
                                            + textOrEmpty(item.getSeries()), item.getSeries());
                            suspectedRows++;
                            rows.add(row);
                            continue;
                        }
                        row.setStatus(CatalogImportRowStatus.Duplicate);
                        rejectFields(row, "项目名称", "C-02",
                                "企业内已存在相同项目（" + dbSameName.getName() + "），默认跳过", item.getName());
                        duplicateRows++;
                        rows.add(row);
                        continue;
                    }
                    seenNames.put(nameKey, rowNo);
                    if (codeKey != null) {
                        seenCodes.put(codeKey, rowNo);
                    }
                }
                row.setStatus(CatalogImportRowStatus.Ready);
                if (item.getCostPrice() == null) {
                    costPendingRows++;
                }
                ignoredTypeFields += countIgnoredTypeFields(cells, columns, item.getType());
                readyItems.add(item);
                if (samples.size() < PREVIEW_LIMIT) {
                    samples.add(sampleLine(rowNo, item));
                }
            } catch (RowRejected rejected) {
                row.setStatus(CatalogImportRowStatus.Error);
                row.setErrorField(rejected.field);
                row.setErrorCode(rejected.code);
                row.setErrorReason(rejected.getMessage() + (rejected.value == null || rejected.value.isBlank()
                        ? "" : "（原值：" + rejected.value + "）"));
                errorRows++;
            }
            rows.add(row);
        }
        String message = ignoredTypeFields == 0 ? null
                : "已忽略 " + ignoredTypeFields + " 处与本行项目类型不匹配的专项字段";
        return new Plan(rows, readyItems, sheet.rows().size(), readyItems.size(), duplicateRows,
                suspectedRows, errorRows, costPendingRows, samples, message);
    }

    /** 仅写入错误/跳过字段，不改行状态（重复与疑似重复需要各自状态）。 */
    private void rejectFields(CatalogImportRow row, String field, String code, String reason, String value) {
        row.setErrorField(field);
        row.setErrorCode(code);
        row.setErrorReason(reason + (value == null || value.isBlank() ? "" : "（原值：" + value + "）"));
    }

    /** 预览摘要：名称、类型、单位、计价方式、售价与成本状态。 */
    private void summarize(CatalogImportRow row, CatalogItem item) {
        row.setItemName(item.getName());
        row.setItemType(item.getType());
        row.setUnit(item.getUnit());
        row.setPricingMode(item.getPricingMode());
        BigDecimal saleAmount = item.getPricingMode() == PricingMethod.FixedTotal
                ? item.getFixedTotalPrice()
                : value(item.getMaterialPrice()).add(value(item.getManualPrice()));
        row.setSaleAmount(saleAmount);
        row.setCostPending(item.getCostPrice() == null);
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private void reject(CatalogImportRow row, String field, String code, String reason, String value) {
        row.setStatus(CatalogImportRowStatus.Error);
        row.setErrorField(field);
        row.setErrorCode(code);
        row.setErrorReason(reason + (value == null || value.isBlank() ? "" : "（原值：" + value + "）"));
    }

    private boolean sameIdentity(CatalogItem existing, String nameKey) {
        return nameKey(existing).equals(nameKey);
    }

    /** 行解析（含价格、成本、类型专项字段与文本长度校验）。 */
    private CatalogItem parseRow(List<String> cells, Map<String, Integer> columns, Employee current) {
        CatalogItem item = new CatalogItem();
        item.setCompany(current.getCompany());
        item.setMaintainer(current);
        item.setActive(Boolean.TRUE);
        item.setSourceType(CatalogSourceType.Import);

        item.setType(alias(cells, columns, "type", "项目类型", "R-01", TYPE_ALIAS, ItemType.values()));
        item.setCode(text(cells, columns, "code", "项目编码", "R-15", 64, false));
        item.setCategory(text(cells, columns, "category", "项目分类", "R-02", 64, true));
        item.setName(text(cells, columns, "name", "项目名称", "R-03", 256, true));
        item.setBrand(text(cells, columns, "brand", "品牌", "R-15", 128, false));
        item.setSeries(text(cells, columns, "series", "系列", "R-15", 128, false));
        item.setSpec(text(cells, columns, "spec", "规格/型号", "R-15", 256, false));

        String unitText = raw(cells, columns, "unit");
        Unit unit = UNIT_ALIAS.get(unitText);
        if (unit == null) {
            throw new RowRejected("计量单位", "R-04", "计量单位不能识别，请使用模板下拉选项", unitText);
        }
        item.setUnit(unit);
        PricingMethod mode = alias(cells, columns, "pricingMode", "计价方式", "R-05", MODE_ALIAS, PricingMethod.values());
        item.setPricingMode(mode);

        BigDecimal material = money(cells, columns, "materialPrice", "材料单价");
        BigDecimal manual = money(cells, columns, "manualPrice", "人工单价");
        BigDecimal fixedTotal = money(cells, columns, "fixedTotalPrice", "固定总价");
        applyPricing(item, mode, material, manual, fixedTotal);

        // 标准成本：空白 = 待维护（null），0 = 明确零成本。
        item.setCostPrice(money(cells, columns, "costPrice", "标准成本单价"));

        Integer durationDays = positiveInteger(cells, columns, "durationDays", "施工工期");
        BigDecimal lossRate = decimal(cells, columns, "lossRate", "损耗率", BigDecimal.ZERO, new BigDecimal("100"));
        BigDecimal minimumOrder = decimal(cells, columns, "minimumOrder", "起订量", BigDecimal.ZERO, null);
        Integer leadTimeDays = positiveInteger(cells, columns, "leadTimeDays", "交期");
        Integer warrantyMonths = positiveInteger(cells, columns, "warrantyMonths", "保修月数");
        String board = text(cells, columns, "board", "柜体板材", "R-15", 128, false);
        String countertop = text(cells, columns, "countertop", "台面", "R-15", 128, false);

        switch (item.getType()) {
            case Construction -> item.setDurationDays(durationDays);
            case Material -> {
                item.setLossRate(lossRate);
                item.setMinimumOrder(minimumOrder);
            }
            case Soft -> item.setLeadTimeDays(leadTimeDays);
            case Appliance -> item.setWarrantyMonths(warrantyMonths);
            case Custom -> {
                item.setBoard(board);
                item.setCountertop(countertop);
            }
            case General -> { }
        }
        item.setCraft(text(cells, columns, "craft", "施工工艺", "R-15", 4000, false));
        item.setCriterion(text(cells, columns, "criterion", "验收标准", "R-15", 4000, false));
        item.setDescription(text(cells, columns, "description", "项目说明", "R-15", 4000, false));
        return item;
    }

    /** 售价口径：按计价方式校验；固定总价写入独立字段，不落材料/人工单价。 */
    private void applyPricing(CatalogItem item, PricingMethod mode,
                              BigDecimal material, BigDecimal manual, BigDecimal fixedTotal) {
        switch (mode) {
            case MaterialAndManual -> {
                if (material == null) {
                    throw new RowRejected("材料单价", "R-06", "材料单价不能为空（售价空白不得当作 0）", null);
                }
                if (manual == null) {
                    throw new RowRejected("人工单价", "R-06", "人工单价不能为空（售价空白不得当作 0）", null);
                }
                if (fixedTotal != null) {
                    throw new RowRejected("固定总价", "R-07", "非固定总价项目不能填写固定总价", text(fixedTotal));
                }
                item.setMaterialPrice(material);
                item.setManualPrice(manual);
            }
            case MaterialOnly -> {
                if (material == null) {
                    throw new RowRejected("材料单价", "R-07", "材料单价不能为空（售价空白不得当作 0）", null);
                }
                if (manual != null && manual.signum() > 0) {
                    throw new RowRejected("人工单价", "R-07", "仅材料项目不能填写大于 0 的人工单价", text(manual));
                }
                if (fixedTotal != null) {
                    throw new RowRejected("固定总价", "R-07", "非固定总价项目不能填写固定总价", text(fixedTotal));
                }
                item.setMaterialPrice(material);
                item.setManualPrice(BigDecimal.ZERO);
            }
            case ManualOnly -> {
                if (manual == null) {
                    throw new RowRejected("人工单价", "R-08", "人工单价不能为空（售价空白不得当作 0）", null);
                }
                if (material != null && material.signum() > 0) {
                    throw new RowRejected("材料单价", "R-08", "仅人工项目不能填写大于 0 的材料单价", text(material));
                }
                if (fixedTotal != null) {
                    throw new RowRejected("固定总价", "R-08", "非固定总价项目不能填写固定总价", text(fixedTotal));
                }
                item.setMaterialPrice(BigDecimal.ZERO);
                item.setManualPrice(manual);
            }
            case FixedTotal -> {
                if (fixedTotal == null) {
                    throw new RowRejected("固定总价", "R-09", "固定总价不能为空（售价空白不得当作 0）", null);
                }
                if (material != null && material.signum() > 0) {
                    throw new RowRejected("材料单价", "R-09", "固定总价项目不能填写材料单价，请保持为空", text(material));
                }
                if (manual != null && manual.signum() > 0) {
                    throw new RowRejected("人工单价", "R-09", "固定总价项目不能填写人工单价，请保持为空", text(manual));
                }
                item.setFixedTotalPrice(fixedTotal);
                item.setMaterialPrice(BigDecimal.ZERO);
                item.setManualPrice(BigDecimal.ZERO);
            }
        }
    }

    /** 表头按名称识别：必须包含全部 V1.0 表头，且不得出现模板之外的列。 */
    private Map<String, Integer> resolveColumns(List<String> header) {
        Map<String, Integer> columns = new LinkedHashMap<>();
        Set<String> unknown = new LinkedHashSet<>();
        for (int index = 0; index < header.size(); index++) {
            String name = normalizeHeader(header.get(index));
            if (name.isBlank()) {
                continue;
            }
            String key = HEADERS.get(name);
            if (key == null) {
                unknown.add(name);
                continue;
            }
            columns.putIfAbsent(key, index);
        }
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> entry : HEADERS.entrySet()) {
            if (!columns.containsKey(entry.getValue())) {
                missing.add(entry.getKey());
            }
        }
        if (!missing.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "表头与 V1.0 模板不一致，缺少列：" + String.join("、", missing) + "，请重新下载模板"));
        }
        if (!unknown.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "表头存在 V1.0 模板之外的列：" + String.join("、", unknown) + "，请重新下载模板"));
        }
        return columns;
    }

    /** 表头归一：去掉必填星号与括号提示（元/天/仅施工等）。 */
    private String normalizeHeader(String rawValue) {
        String value = rawValue == null ? "" : rawValue.trim();
        if (value.endsWith("*")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        int full = value.indexOf('（');
        int half = value.indexOf('(');
        int index = full < 0 ? half : (half < 0 ? full : Math.min(full, half));
        if (index > 0) {
            value = value.substring(0, index).trim();
        }
        return value;
    }

    private int countIgnoredTypeFields(List<String> cells, Map<String, Integer> columns, ItemType type) {
        int ignored = 0;
        int[] indexes = switch (type) {
            case Construction -> new int[]{index(columns, "lossRate"), index(columns, "minimumOrder"),
                    index(columns, "leadTimeDays"), index(columns, "warrantyMonths"),
                    index(columns, "board"), index(columns, "countertop")};
            case Material -> new int[]{index(columns, "durationDays"), index(columns, "leadTimeDays"),
                    index(columns, "warrantyMonths"), index(columns, "board"), index(columns, "countertop")};
            case Soft -> new int[]{index(columns, "durationDays"), index(columns, "lossRate"),
                    index(columns, "minimumOrder"), index(columns, "warrantyMonths"),
                    index(columns, "board"), index(columns, "countertop")};
            case Appliance -> new int[]{index(columns, "durationDays"), index(columns, "lossRate"),
                    index(columns, "minimumOrder"), index(columns, "leadTimeDays"),
                    index(columns, "board"), index(columns, "countertop")};
            case Custom -> new int[]{index(columns, "durationDays"), index(columns, "lossRate"),
                    index(columns, "minimumOrder"), index(columns, "leadTimeDays"), index(columns, "warrantyMonths")};
            case General -> new int[]{index(columns, "durationDays"), index(columns, "lossRate"),
                    index(columns, "minimumOrder"), index(columns, "leadTimeDays"),
                    index(columns, "warrantyMonths"), index(columns, "board"), index(columns, "countertop")};
        };
        for (int position : indexes) {
            if (position >= 0 && position < cells.size() && !cells.get(position).isBlank()) {
                ignored++;
            }
        }
        return ignored;
    }

    private int index(Map<String, Integer> columns, String key) {
        Integer index = columns.get(key);
        return index == null ? -1 : index;
    }

    private <E extends Enum<E> & com.th4.edge.framework.data.enums.Enumerable<String>> E alias(
            List<String> cells, Map<String, Integer> columns, String key, String label, String code,
            Map<String, E> aliases, E[] values) {
        String value = raw(cells, columns, key);
        if (value.isEmpty()) {
            throw new RowRejected(label, code, label + "不能为空", null);
        }
        E matched = aliases.get(value);
        if (matched == null) {
            StringBuilder options = new StringBuilder();
            for (E candidate : values) {
                if (options.length() > 0) {
                    options.append("、");
                }
                options.append(candidate.getName());
            }
            throw new RowRejected(label, code, label + "不能识别（可选：" + options + "）", value);
        }
        return matched;
    }

    private String text(List<String> cells, Map<String, Integer> columns, String key, String label,
                        String code, int max, boolean required) {
        String value = raw(cells, columns, key);
        if (value.isEmpty()) {
            if (required) {
                throw new RowRejected(label, code, label + "不能为空", null);
            }
            return null;
        }
        if (value.length() > max) {
            throw new RowRejected(label, code, label + "长度超过 " + max + " 字符", value.substring(0, Math.min(40, value.length())) + "…");
        }
        return value;
    }

    private String raw(List<String> cells, Map<String, Integer> columns, String key) {
        Integer index = columns.get(key);
        if (index == null || index >= cells.size()) {
            return "";
        }
        String value = cells.get(index);
        return value == null ? "" : value.trim();
    }

    private BigDecimal money(List<String> cells, Map<String, Integer> columns, String key, String label) {
        String value = raw(cells, columns, key);
        if (value.isEmpty()) {
            return null;
        }
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(value.replace(",", ""));
        } catch (NumberFormatException ex) {
            throw new RowRejected(label, "R-08", label + "必须是数字", value);
        }
        if (parsed.signum() < 0) {
            throw new RowRejected(label, "R-08", label + "不能为负数", value);
        }
        if (parsed.stripTrailingZeros().scale() > 2) {
            throw new RowRejected(label, "R-08", label + "最多保留两位小数", value);
        }
        return parsed.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal decimal(List<String> cells, Map<String, Integer> columns, String key, String label,
                               BigDecimal min, BigDecimal max) {
        BigDecimal value = money(cells, columns, key, label);
        if (value == null) {
            return null;
        }
        if (min != null && value.compareTo(min) < 0) {
            throw new RowRejected(label, "R-13", label + "不能小于 " + min.toPlainString(), text(value));
        }
        if (max != null && value.compareTo(max) > 0) {
            throw new RowRejected(label, "R-13", label + "不能大于 " + max.toPlainString(), text(value));
        }
        return value;
    }

    private Integer positiveInteger(List<String> cells, Map<String, Integer> columns, String key, String label) {
        String value = raw(cells, columns, key);
        if (value.isEmpty()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new RowRejected(label, "R-12", label + "不能为负数", value);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new RowRejected(label, "R-12", label + "必须是非负整数", value);
        }
    }

    private String codeKey(ItemType type, String code) {
        return type.name() + "|" + code.trim().toUpperCase(Locale.ROOT);
    }

    private String nameKey(CatalogItem item) {
        return item.getType().name() + "|" + lower(item.getName()) + "|" + lower(item.getBrand())
                + "|" + lower(item.getSpec()) + "|" + item.getUnit().name();
    }

    private boolean equalsText(String left, String right) {
        return lower(left).equals(lower(right));
    }

    private String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String sampleLine(int rowNo, CatalogItem item) {
        StringBuilder builder = new StringBuilder();
        builder.append("第").append(rowNo).append("行｜").append(item.getType().getName())
                .append("｜").append(item.getName()).append("｜").append(item.getUnit().getName())
                .append("｜").append(item.getPricingMode().getName());
        if (item.getPricingMode() == PricingMethod.FixedTotal) {
            builder.append("｜固定总价").append(text(item.getFixedTotalPrice()));
        } else {
            builder.append("｜材料").append(text(item.getMaterialPrice()))
                    .append("+人工").append(text(item.getManualPrice()));
        }
        builder.append("｜").append(item.getCostPrice() == null ? "成本待维护" : "成本" + text(item.getCostPrice()));
        return builder.toString();
    }

    private String text(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String textOrEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** 文件指纹（企业内重复上传识别）。 */
    public String fingerprint(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    public String fingerprint(String content) {
        return fingerprint(content.getBytes(StandardCharsets.UTF_8));
    }
}

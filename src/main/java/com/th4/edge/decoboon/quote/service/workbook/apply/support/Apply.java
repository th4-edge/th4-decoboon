package com.th4.edge.decoboon.quote.service.workbook.apply.support;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.context.QuoteRevisionContext;
import com.th4.edge.decoboon.quote.context.QuoteWorkbookImportContext;
import com.th4.edge.decoboon.quote.model.QuoteWorkbookImportRequest;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.quote.service.workbook.importer.ImportResult;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报价工作簿确认导入（落库）。
 *
 * 依据 ANS-BIZ-20261002-002 Q3：
 * - 仅草稿；设计师仅能操作本人创建的报价；BOSS 可按企业范围操作；
 * - 错误行**阻断整个导入**（不静默跳过）；
 * - 区域必须匹配当前报价所属设计方案的空间；
 * - 落库后由 `QuoteAdjustmentContext` 权威重算，并返回新的内容修订令牌。
 *
 * 模式：MERGE（按 itemId 合并）/ OVERWRITE（以工作簿行为准覆盖量价与公式）。
 */
@Note("报价工作簿确认导入")
@Properties(allow = {
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "title", "customer", "design", "version", "mode", "area",
                        "subtotal", "amount", "discount", "taxRate", "taxAmount", "status", "items",
                        "regionGroups", "typeGroups", "constructionGroups", "materialGroups", "otherGroups",
                        "createTime", "updateTime", "property", "revision"}),
        @AllowProperties(model = QuoteGroup.class,
                properties = {"key", "name", "groupMode", "type", "category", "unit", "quantity", "amount", "items"}),
        @AllowProperties(model = QuoteLine.class,
                properties = {"itemId", "catalogSourceId", "catalogSourceCode", "catalogSourceRevision", "space", "type", "category", "packageSectionNameSnapshot", "name", "brand", "spec", "unit", "quantity",
                        "quantityFormula", "materialPrice", "manualPrice", "fixedTotalPrice", "pricingMode",
                        "price", "amount", "role", "included", "discountAmount", "sort", "typeRowId", "sourceLineIds", "sourceRegionIds", "sourceRegionNames", "sourceCount"}),
        @AllowProperties(model = Space.class, properties = {"spaceId", "name", "type"})
})
@Service("quote/workbook/apply")
@Scope(ScopeTypes.PROTOTYPE)
public class Apply implements com.th4.edge.decoboon.quote.service.workbook.apply.Apply {

    @AutoInject
    @NotNull
    private Long quoteId;

    @AutoInject
    private String mode;

    @AutoInject
    private String fileName;

    @AutoInject
    private String contentBase64;

    @User
    private Employee employee;

    private final QuoteRepository quoteRepository;
    private final QuoteLineRepository lineRepository;
    private final SpaceRepository spaceRepository;
    private final QuoteWorkbookImportContext importContext;
    private final QuoteAdjustmentContext adjustmentContext;
    private final QuoteRevisionContext revisionContext;
    private final com.th4.edge.decoboon.quote.context.QuoteViewContext viewContext;
    private final DataScopeContext dataScopeContext;

    public Apply(QuoteRepository quoteRepository, QuoteLineRepository lineRepository,
                 SpaceRepository spaceRepository,
                 QuoteWorkbookImportContext importContext, QuoteAdjustmentContext adjustmentContext,
                 QuoteRevisionContext revisionContext,
                 com.th4.edge.decoboon.quote.context.QuoteViewContext viewContext,
                 DataScopeContext dataScopeContext) {
        this.quoteRepository = quoteRepository;
        this.lineRepository = lineRepository;
        this.spaceRepository = spaceRepository;
        this.importContext = importContext;
        this.adjustmentContext = adjustmentContext;
        this.revisionContext = revisionContext;
        this.viewContext = viewContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional
    public Quote call(Quote ignored) {
        Employee current = dataScopeContext.requireEmployee(employee);
        boolean boss = current.getRole() == EmployeeRole.Boss;
        if (!boss && current.getRole() != EmployeeRole.Designer) {
            throw new DefinedException(new DefaultErrorCode("403", "仅设计师或企业负责人可导入报价工作簿"));
        }
        DataScope scope = dataScopeContext.resolve(current);
        Specification<Quote> byId = (root, query, builder) -> builder.equal(root.get("quoteId"), quoteId);
        Quote quote = quoteRepository.findOne(byId.and(DataScopeSpecification.visible(Quote.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问")));
        if (!boss && (quote.getCreatedBy() == null
                || !quote.getCreatedBy().getEmpId().equals(current.getEmpId()))) {
            throw new DefinedException(new DefaultErrorCode("404", "报价不存在或无权访问"));
        }
        if (!QuoteStatus.Draft.equals(quote.getStatus())) {
            throw new DefinedException(new DefaultErrorCode("400", "只有草稿报价可以导入工作簿"));
        }

        List<QuoteLine> lines = lineRepository.findByQuote(quote);
        QuoteWorkbookImportRequest request = new QuoteWorkbookImportRequest();
        request.setQuoteId(quoteId);
        request.setFileName(fileName);
        request.setContentBase64(contentBase64);
        ImportResult parsed = importContext.parse(request, lines);

        // 错误行阻断：不静默跳过
        if (parsed.getFailed() > 0) {
            String detail = parsed.getRows().stream()
                    .filter(row -> !row.isImportable())
                    .limit(5)
                    .map(row -> "第" + row.getExcelRow() + "行：" + String.join("、", row.getErrors()))
                    .collect(Collectors.joining("；"));
            throw new DefinedException(new DefaultErrorCode("400",
                    "工作簿存在 " + parsed.getFailed() + " 行错误，已阻止导入：" + detail));
        }

        // 区域匹配校验：行区域必须属于当前报价已存在的区域
        Map<Long, QuoteLine> lineById = lines.stream()
                .collect(Collectors.toMap(QuoteLine::getItemId, line -> line, (a, b) -> a, LinkedHashMap::new));
        List<String> regionErrors = new ArrayList<>();
        for (ImportResult.Row row : parsed.getRows()) {
            QuoteLine target = lineById.get(row.getItemId());
            if (target == null) continue;
            String currentRegion = target.getSpace() == null ? null : target.getSpace().getName();
            if (row.getRegion() != null && !row.getRegion().isBlank()
                    && currentRegion != null && !row.getRegion().trim().equals(currentRegion)) {
                regionErrors.add("第" + row.getExcelRow() + "行：区域「" + row.getRegion().trim()
                        + "」与当前报价区域「" + currentRegion + "」不一致");
            }
        }
        if (!regionErrors.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "区域不匹配，已阻止导入：" + String.join("；", regionErrors)));
        }

        String normalizedMode = mode == null ? "MERGE" : mode.trim().toUpperCase();
        if (!"MERGE".equals(normalizedMode) && !"OVERWRITE".equals(normalizedMode)) {
            throw new DefinedException(new DefaultErrorCode("400", "不支持的导入模式：" + mode));
        }

        // OVERWRITE：工作簿有效行成为当前草稿的**有效集合**；
        // 工作簿未出现的原行不物理删除，改为 included=false 逻辑排除（保留审计数据）。
        // MERGE：只更新工作簿出现的 itemId，未出现的原行不动。
        if ("OVERWRITE".equals(normalizedMode)) {
            java.util.Set<Long> imported = parsed.getRows().stream()
                    .map(ImportResult.Row::getItemId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            for (QuoteLine line : lines) {
                boolean keep = imported.contains(line.getItemId());
                if (!Boolean.valueOf(keep).equals(line.getIncluded())) {
                    line.setIncluded(keep);
                    lineRepository.save(line);
                }
            }
        }

        // 区域列落地（USER-DECISION-20261013-002：区域为可选维度，不得因区域而阻断）：
        // 仅当「原行没有区域」且「工作簿区域名匹配到当前设计已存在的区域」时补写；
        // 不新建区域、不改动已有区域（两者不一致仍按上方校验阻断）、匹配不到则保持原样。
        Map<String, Space> spacesByName = new LinkedHashMap<>();
        Design design = quote.getDesign();
        if (design != null && design.getDesignId() != null) {
            for (Space space : spaceRepository.findByDesign_DesignIdOrderBySortAsc(design.getDesignId())) {
                if (space.getName() != null && !space.getName().isBlank()) {
                    spacesByName.putIfAbsent(space.getName().trim(), space);
                }
            }
        }

        // 落库：以工作簿为准更新量价与公式（未提供的字段保留原值）
        for (ImportResult.Row row : parsed.getRows()) {
            QuoteLine target = lineById.get(row.getItemId());
            if (target == null) continue;
            target.setIncluded(true);
            if (target.getSpace() == null && row.getRegion() != null && !row.getRegion().isBlank()) {
                Space matched = spacesByName.get(row.getRegion().trim());
                if (matched != null) target.setSpace(matched);
            }
            if (row.getQuantityFormula() != null && !row.getQuantityFormula().isBlank()) {
                target.setQuantityFormula(row.getQuantityFormula().trim());
            }
            if (row.getQuantity() != null && row.getQuantity().signum() > 0) {
                target.setQuantity(row.getQuantity());
            }
            if (row.getMaterialPrice() != null && row.getMaterialPrice().signum() >= 0) {
                target.setMaterialPrice(row.getMaterialPrice());
            }
            if (row.getManualPrice() != null && row.getManualPrice().signum() >= 0) {
                target.setManualPrice(row.getManualPrice());
            }
            lineRepository.save(target);
        }
        // 权威重算
        adjustmentContext.recalculate(quote);
        List<QuoteLine> saved = lineRepository.findByQuote(quote);
        Quote view = viewContext.apply(quote, saved);
        view.setRevision(revisionContext.token(quote, saved));
        return view;
    }
}

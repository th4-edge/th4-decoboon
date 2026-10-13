package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.context.QuoteAdjustmentContext;
import com.th4.edge.decoboon.quote.context.QuoteRevisionContext;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.quote.context.QuoteWorkbookImportContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.model.QuoteWorkbookImportRequest;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.quote.service.workbook.apply.support.Apply;
import com.th4.edge.decoboon.quote.service.workbook.importer.ImportResult;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 报价工作簿确认导入（quote/workbook/apply）定向测试。
 *
 * 覆盖：状态拒绝、错误行阻断、区域不匹配、MERGE 保留行、OVERWRITE 逻辑排除、
 * 落库后权威重算调用、返回新 revision。
 */
class QuoteWorkbookApplyTest {

    private final QuoteRepository quoteRepository = mock(QuoteRepository.class);
    private final QuoteLineRepository lineRepository = mock(QuoteLineRepository.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final QuoteWorkbookImportContext importContext = mock(QuoteWorkbookImportContext.class);
    private final QuoteAdjustmentContext adjustmentContext = mock(QuoteAdjustmentContext.class);
    private final QuoteRevisionContext revisionContext = new QuoteRevisionContext();
    private final QuoteViewContext viewContext = mock(QuoteViewContext.class);
    private final DataScopeContext dataScopeContext = mock(DataScopeContext.class);

    private final Apply apply = new Apply(quoteRepository, lineRepository, spaceRepository, importContext,
            adjustmentContext, revisionContext, viewContext, dataScopeContext);

    private Employee designer() {
        Employee e = new Employee();
        e.setRole(EmployeeRole.Designer);
        e.setEmpId(105L);
        return e;
    }

    private Quote draftQuote(QuoteStatus status) {
        Employee owner = new Employee();
        owner.setEmpId(105L);
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setStatus(status);
        quote.setCreatedBy(owner);
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxRate(new BigDecimal("0.05"));
        return quote;
    }

    private QuoteLine line(Long id, String region, boolean included) {
        Space space = new Space();
        space.setName(region);
        QuoteLine l = new QuoteLine();
        l.setItemId(id);
        l.setSpace(space);
        l.setIncluded(included);
        l.setQuantity(new BigDecimal("2"));
        l.setMaterialPrice(new BigDecimal("10.00"));
        l.setManualPrice(new BigDecimal("5.00"));
        l.setAmount(new BigDecimal("30.00"));
        l.setCost(new BigDecimal("8.00"));
        return l;
    }

    private ImportResult.Row row(long itemId, String region, boolean importable) {
        ImportResult.Row r = new ImportResult.Row();
        r.setExcelRow((int) itemId + 1);
        r.setItemId(itemId);
        r.setRegion(region);
        r.setQuantity(new BigDecimal("3"));
        r.setMaterialPrice(new BigDecimal("12.00"));
        r.setImportable(importable);
        if (!importable) r.getErrors().add("工程量必须大于零");
        return r;
    }

    private ImportResult result(ImportResult.Row... rows) {
        ImportResult r = new ImportResult();
        for (ImportResult.Row row : rows) r.getRows().add(row);
        r.setTotal(rows.length);
        int valid = (int) java.util.Arrays.stream(rows).filter(ImportResult.Row::isImportable).count();
        r.setValid(valid);
        r.setFailed(rows.length - valid);
        return r;
    }

    private void wire(Quote quote, List<QuoteLine> lines) {
        when(dataScopeContext.requireEmployee(any())).thenReturn(designer());
        when(quoteRepository.findOne(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(java.util.Optional.of(quote));
        when(lineRepository.findByQuote(quote)).thenReturn(lines);
        when(lineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(viewContext.apply(any(), anyList())).thenAnswer(inv -> quote);
    }

    private void setFields(Long quoteId, String mode) {
        ReflectionTestUtils.setField(apply, "quoteId", quoteId);
        ReflectionTestUtils.setField(apply, "mode", mode);
        ReflectionTestUtils.setField(apply, "fileName", "wb.xlsx");
        ReflectionTestUtils.setField(apply, "contentBase64", "UEsDBA==");
        ReflectionTestUtils.setField(apply, "employee", designer());
    }

    // ───────── 状态拒绝 ─────────

    @Test
    void shouldRejectNonDraftQuotes() {
        for (QuoteStatus status : List.of(QuoteStatus.Submitted, QuoteStatus.Accepted, QuoteStatus.Converted)) {
            Quote quote = draftQuote(status);
            wire(quote, List.of(line(2L, "客厅", true)));
            setFields(1L, "MERGE");
            assertThatThrownBy(() -> apply.call(new Quote()))
                    .hasMessageContaining("只有草稿报价可以导入工作簿");
        }
    }

    // ───────── 错误行阻断 ─────────

    @Test
    void shouldBlockWhenAnyRowHasError() {
        Quote quote = draftQuote(QuoteStatus.Draft);
        wire(quote, List.of(line(2L, "客厅", true)));
        when(importContext.parse(any(QuoteWorkbookImportRequest.class), anyList()))
                .thenReturn(result(row(2L, "客厅", false)));
        setFields(1L, "MERGE");

        assertThatThrownBy(() -> apply.call(new Quote()))
                .hasMessageContaining("行错误，已阻止导入");
        verify(adjustmentContext, never()).recalculate(any());
    }

    // ───────── 区域不匹配 ─────────

    @Test
    void shouldBlockWhenRegionMismatch() {
        Quote quote = draftQuote(QuoteStatus.Draft);
        wire(quote, List.of(line(2L, "客厅", true)));
        when(importContext.parse(any(QuoteWorkbookImportRequest.class), anyList()))
                .thenReturn(result(row(2L, "主卧", true)));
        setFields(1L, "MERGE");

        assertThatThrownBy(() -> apply.call(new Quote()))
                .hasMessageContaining("区域不匹配");
        verify(adjustmentContext, never()).recalculate(any());
    }

    // ───────── 区域可选：工作簿「区域列」落地（USER-DECISION-20261013-002）─────────

    @Test
    void shouldFillRegionFromWorkbookWhenLineHasNoSpace() {
        Quote quote = draftQuote(QuoteStatus.Draft);
        Design design = new Design();
        design.setDesignId(77L);
        quote.setDesign(design);
        Space master = new Space();
        master.setSpaceId(501L);
        master.setName("主卧");
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(77L)).thenReturn(List.of(master));

        QuoteLine noSpace = line(2L, null, true);
        noSpace.setSpace(null);
        wire(quote, List.of(noSpace));
        when(importContext.parse(any(QuoteWorkbookImportRequest.class), anyList()))
                .thenReturn(result(row(2L, "主卧", true)));
        setFields(1L, "MERGE");

        apply.call(new Quote());

        assertThat(noSpace.getSpace()).isNotNull();
        assertThat(noSpace.getSpace().getName()).isEqualTo("主卧");
        assertThat(noSpace.getSpace().getSpaceId()).isEqualTo(501L);
    }

    @Test
    void shouldKeepSpaceEmptyWhenWorkbookRegionDoesNotMatchAnySpace() {
        Quote quote = draftQuote(QuoteStatus.Draft);
        Design design = new Design();
        design.setDesignId(77L);
        quote.setDesign(design);
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(77L)).thenReturn(List.of());

        QuoteLine noSpace = line(2L, null, true);
        noSpace.setSpace(null);
        wire(quote, List.of(noSpace));
        when(importContext.parse(any(QuoteWorkbookImportRequest.class), anyList()))
                .thenReturn(result(row(2L, "阁楼", true)));
        setFields(1L, "MERGE");

        apply.call(new Quote());

        // 不新建区域、不因匹配不到而阻断：保持无区域
        assertThat(noSpace.getSpace()).isNull();
        verify(lineRepository).save(noSpace);
    }

    // ───────── MERGE 保留未出现行 ─────────

    @Test
    void shouldKeepUnlistedLinesInMergeMode() {
        Quote quote = draftQuote(QuoteStatus.Draft);
        QuoteLine kept = line(3L, "主卧", true);
        QuoteLine updated = line(2L, "客厅", true);
        wire(quote, List.of(kept, updated));
        when(importContext.parse(any(QuoteWorkbookImportRequest.class), anyList()))
                .thenReturn(result(row(2L, "客厅", true)));
        setFields(1L, "MERGE");

        apply.call(new Quote());

        assertThat(kept.getIncluded()).isTrue();          // 未被排除
        assertThat(updated.getQuantity()).isEqualByComparingTo("3");
        assertThat(updated.getMaterialPrice()).isEqualByComparingTo("12.00");
        verify(adjustmentContext).recalculate(quote);
    }

    // ───────── OVERWRITE 逻辑排除 ─────────

    @Test
    void shouldExcludeMissingLinesLogicallyInOverwriteMode() {
        Quote quote = draftQuote(QuoteStatus.Draft);
        QuoteLine missing = line(3L, "主卧", true);
        QuoteLine present = line(2L, "客厅", true);
        wire(quote, List.of(missing, present));
        when(importContext.parse(any(QuoteWorkbookImportRequest.class), anyList()))
                .thenReturn(result(row(2L, "客厅", true)));
        setFields(1L, "OVERWRITE");

        apply.call(new Quote());

        assertThat(missing.getIncluded()).isFalse();      // 逻辑排除，不物理删除
        assertThat(present.getIncluded()).isTrue();
        verify(adjustmentContext).recalculate(quote);
    }

    // ───────── 返回新 revision ─────────

    @Test
    void shouldReturnRevisionToken() {
        Quote quote = draftQuote(QuoteStatus.Draft);
        QuoteLine present = line(2L, "客厅", true);
        wire(quote, List.of(present));
        when(importContext.parse(any(QuoteWorkbookImportRequest.class), anyList()))
                .thenReturn(result(row(2L, "客厅", true)));
        setFields(1L, "MERGE");

        Quote view = apply.call(new Quote());

        assertThat(view.getRevision()).isNotBlank();
    }
}

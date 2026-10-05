package com.th4.edge.decoboon.trade.quote;

import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.quote.context.QuoteWorkbookFileContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.model.QuoteWorkbookFile;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 报价工作簿双版本导出契约测试。 */
class QuoteWorkbookFileContextTest {

    private final QuoteWorkbookFileContext context = new QuoteWorkbookFileContext();

    @Test
    void internalWorkbookKeepsQuoteFieldsAndNeverContainsCostOrProfit() throws Exception {
        QuoteWorkbookFile file = context.export(quote(), List.of(line()), "internal");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(Base64.getDecoder().decode(file.getContentBase64())))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(cell(sheet, 0, 6)).isEqualTo("工程量公式");
            assertThat(cell(sheet, 0, 7)).isEqualTo("材料价");
            assertThat(cell(sheet, 0, 8)).isEqualTo("人工价");
            assertThat(cell(sheet, 0, 9)).isEqualTo("单价");
            assertThat(cell(sheet, 0, 10)).isEqualTo("金额");
            assertThat(cell(sheet, 1, 0)).isEqualTo("101");
            assertThat(cell(sheet, 1, 6)).isEqualTo("=5*6");
            assertThat(cell(sheet, 1, 7)).isEqualTo("35.0");
            assertThat(cell(sheet, 1, 8)).isEqualTo("25.0");
            assertNoCostOrProfitHeaders(sheet);
            assertThat(file.getFileName()).contains("内部工作版");
        }
    }

    @Test
    void customerWorkbookOnlyContainsCustomerVisibleFieldsAndNeverContainsCostOrProfit() throws Exception {
        QuoteWorkbookFile file = context.export(quote(), List.of(line()), "customer");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(Base64.getDecoder().decode(file.getContentBase64())))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertThat(cell(sheet, 0, 0)).isEqualTo("序号");
            assertThat(cell(sheet, 0, 5)).isEqualTo("工程量");
            assertThat(cell(sheet, 0, 6)).isEqualTo("单价");
            assertThat(cell(sheet, 0, 7)).isEqualTo("金额");
            assertThat(headerText(sheet)).doesNotContain("工程量公式", "材料价", "人工价", "成本", "利润", "采购", "内部");
            assertThat(cell(sheet, 1, 6)).isEqualTo("60.0");
            assertThat(cell(sheet, 1, 7)).isEqualTo("600.0");
            assertNoCostOrProfitHeaders(sheet);
            assertThat(file.getFileName()).contains("客户版");
        }
    }

    @Test
    void unsupportedVariantIsRejectedExplicitly() {
        assertThatThrownBy(() -> context.export(quote(), List.of(line()), "unknown"))
                .hasMessageContaining("仅支持 internal/customer");
    }

    @Test
    void nullVariantKeepsBackwardCompatibleInternalWorkbook() {
        QuoteWorkbookFile file = context.export(quote(), List.of(line()), null);
        assertThat(file.getFileName()).contains("内部工作版");
    }

    private Quote quote() {
        Quote quote = new Quote();
        quote.setNo("Q-TEST");
        quote.setVersion(1);
        return quote;
    }

    private QuoteLine line() {
        Space space = new Space();
        space.setName("客厅");
        QuoteLine line = new QuoteLine();
        line.setItemId(101L);
        line.setSpace(space);
        line.setName("乳胶漆");
        line.setQuantity(new BigDecimal("10"));
        line.setQuantityFormula("=5*6");
        line.setMaterialPrice(new BigDecimal("35.00"));
        line.setManualPrice(new BigDecimal("25.00"));
        line.setPrice(new BigDecimal("60.00"));
        line.setAmount(new BigDecimal("600.00"));
        line.setCost(new BigDecimal("80.00"));
        return line;
    }

    private String cell(Sheet sheet, int row, int column) {
        Row r = sheet.getRow(row);
        return r == null || r.getCell(column) == null ? null : r.getCell(column).toString();
    }

    private String headerText(Sheet sheet) {
        StringBuilder builder = new StringBuilder();
        Row header = sheet.getRow(0);
        for (int i = 0; i < header.getLastCellNum(); i++) builder.append(cell(sheet, 0, i)).append('|');
        return builder.toString();
    }

    private void assertNoCostOrProfitHeaders(Sheet sheet) {
        assertThat(headerText(sheet)).doesNotContain("成本", "毛利", "利润", "采购价", "标准成本");
    }
}

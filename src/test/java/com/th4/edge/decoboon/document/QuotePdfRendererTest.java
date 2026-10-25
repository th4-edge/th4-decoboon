package com.th4.edge.decoboon.document;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.document.configure.DocumentAudience;
import com.th4.edge.decoboon.document.context.DocumentTemplateBaseline;
import com.th4.edge.decoboon.document.context.QuotePdfRenderer;
import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.quote.configure.QuoteViewMode;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.bim.configure.Unit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuotePdfRendererTest {

    @Test
    void rendersCustomerQuoteWithoutInternalCost() throws Exception {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setNo("BJ-20260920-001");
        quote.setTitle("现代简约整装报价");
        quote.setVersion(2);
        quote.setArea(new BigDecimal("118"));
        quote.setAmount(new BigDecimal("168000.00"));
        Customer customer = new Customer();
        customer.setCustId(11L);
        customer.setName("张女士");
        Property property = new Property();
        property.setPropertyId(21L);
        property.setAddress("合肥市蜀山区示例小区 8-1801");
        quote.setCustomer(customer);
        quote.setProperty(property);

        QuoteLine line = new QuoteLine() {
            @Override
            public BigDecimal getCost() {
                throw new AssertionError("客户版报价 PDF 不得读取成本字段");
            }
        };
        line.setItemId(31L);
        line.setCategory("主材");
        line.setName("客厅地砖");
        line.setBrand("示例品牌");
        line.setSpec("750x1500");
        line.setUnit(Unit.Area);
        line.setQuantity(new BigDecimal("32"));
        line.setPrice(new BigDecimal("200.00"));
        line.setAmount(new BigDecimal("6400.00"));

        DocumentProfile profile = new DocumentProfile();
        profile.setLegalName("合肥示例装饰工程有限公司");
        profile.setAddress("合肥市政务区示例大厦");
        profile.setServiceHotline("400-000-0000");
        QuotePdfRenderer renderer = new QuotePdfRenderer(new ObjectMapper());

        byte[] pdf = renderer.render(quote, List.of(line), new DocumentTemplateBaseline().template(
                com.th4.edge.decoboon.document.configure.DocumentType.Quote), profile, DocumentAudience.Customer, QuoteViewMode.REGION);

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
        PdfReader reader = new PdfReader(pdf);
        assertThat(reader.getNumberOfPages()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rendersTypeGroupingAndPrintsGroupMode() throws Exception {
        Quote quote = baseQuote();
        QuoteLine line = line(3L, "客厅", "水电开槽", "1800.00");
        line.setType(ItemType.Construction);
        line.setCategory("水电");

        QuotePdfRenderer renderer = new QuotePdfRenderer(new ObjectMapper());
        byte[] pdf = renderer.render(quote, List.of(line),
                new DocumentTemplateBaseline().template(
                        com.th4.edge.decoboon.document.configure.DocumentType.Quote),
                profile(), DocumentAudience.Customer, QuoteViewMode.TYPE);

        String text = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(text).contains("分组方式").contains("按类型");
        assertThat(text).contains("施工 · 水电");
    }

    @Test
    void embedsChineseFontAndGroupsLinesByRegionWithChineseUnits() throws Exception {
        Quote quote = baseQuote();
        QuoteLine livingRoom = line(1L, "客厅", "客厅地砖", "6400.00");
        QuoteLine bedroom = line(2L, "主卧", "主卧地板", "3200.00");

        QuotePdfRenderer renderer = new QuotePdfRenderer(new ObjectMapper());
        byte[] pdf = renderer.render(quote, List.of(livingRoom, bedroom),
                new DocumentTemplateBaseline().template(
                        com.th4.edge.decoboon.document.configure.DocumentType.Quote),
                profile(), DocumentAudience.Customer, QuoteViewMode.REGION);

        // 字体必须嵌入并带 ToUnicode，否则手机阅读器乱码、复制文本乱码
        String raw = new String(pdf, StandardCharsets.ISO_8859_1);
        assertThat(raw).contains("/FontFile2");
        assertThat(raw).contains("/ToUnicode");

        String text = new PdfTextExtractor(new PdfReader(pdf)).getTextFromPage(1);
        assertThat(text).contains("平方米");
        assertThat(text).doesNotContain("m²");
        assertThat(text).contains("客厅").contains("主卧").contains("小计");
        // 数字由 Roboto 提供，中文由 DroidSansFallback 提供；缺任一方金额会变成空白
        assertThat(text).contains("168000.00").contains("6400.00").contains("3200.00");
        assertThat(text).contains("118.00 平方米");
    }

    private Quote baseQuote() {
        Quote quote = new Quote();
        quote.setQuoteId(1L);
        quote.setNo("BJ-20260920-001");
        quote.setTitle("现代简约整装报价");
        quote.setVersion(2);
        quote.setArea(new BigDecimal("118"));
        quote.setAmount(new BigDecimal("168000.00"));
        Customer customer = new Customer();
        customer.setCustId(11L);
        customer.setName("张女士");
        Property property = new Property();
        property.setPropertyId(21L);
        property.setAddress("合肥市蜀山区示例小区 8-1801");
        quote.setCustomer(customer);
        quote.setProperty(property);
        return quote;
    }

    private DocumentProfile profile() {
        DocumentProfile profile = new DocumentProfile();
        profile.setLegalName("合肥示例装饰工程有限公司");
        profile.setAddress("合肥市政务区示例大厦");
        profile.setServiceHotline("400-000-0000");
        return profile;
    }

    private QuoteLine line(Long itemId, String region, String name, String amount) {
        Space space = new Space();
        space.setSpaceId(itemId);
        space.setName(region);
        space.setSort(itemId.intValue());

        QuoteLine line = new QuoteLine();
        line.setItemId(itemId);
        line.setSpace(space);
        line.setCategory("主材");
        line.setName(name);
        line.setBrand("示例品牌");
        line.setSpec("750x1500");
        line.setUnit(Unit.Area);
        line.setQuantity(new BigDecimal("32"));
        line.setPrice(new BigDecimal("200.00"));
        line.setAmount(new BigDecimal(amount));
        return line;
    }
}

package com.th4.edge.decoboon.document.context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.FontSelector;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.document.configure.DocumentAudience;
import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.quote.configure.QuoteViewMode;
import com.th4.edge.decoboon.quote.context.QuoteViewContext;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteGroup;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 报价单 PDF 渲染；客户版固定不输出成本、利润和内部结算字段。 */
@Component
public class QuotePdfRenderer {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    /**
     * 可嵌入中文字体。历史实现使用 BaseFont.NOT_EMBEDDED 的 STSong-Light，
     * 手机阅读器缺少该字体时会整段乱码或显示方框，且没有 ToUnicode 导致复制文本乱码。
     */
    private static final String CJK_FONT_RESOURCE = "/fonts/DroidSansFallback.ttf";
    private static final String CJK_FONT_NAME = "DroidSansFallback.ttf";
    /** DroidSansFallback 没有 ASCII 字形，金额数字必须由 Roboto 提供 */
    private static final String LATIN_FONT_RESOURCE = "/fonts/Roboto-Regular.ttf";
    private static final String LATIN_FONT_NAME = "Roboto-Regular.ttf";
    private static final byte[] CJK_FONT_BYTES = readFont(CJK_FONT_RESOURCE);
    private static final byte[] LATIN_FONT_BYTES = readFont(LATIN_FONT_RESOURCE);
    private final ObjectMapper objectMapper;
    private final QuoteViewContext quoteViewContext;

    public QuotePdfRenderer(ObjectMapper objectMapper) {
        this(objectMapper, new QuoteViewContext());
    }

    @Autowired
    public QuotePdfRenderer(ObjectMapper objectMapper, QuoteViewContext quoteViewContext) {
        this.objectMapper = objectMapper;
        this.quoteViewContext = quoteViewContext;
    }

    public byte[] render(Quote quote, List<QuoteLine> lines, DocumentTemplate template,
                         DocumentProfile profile, DocumentAudience audience, QuoteViewMode groupMode) {
        if (groupMode == null) {
            throw invalid("请选择报价分组方式");
        }
        if (quote == null) throw invalid("报价不存在");
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            RenderStyle style = style(template.getStyleConfig());
            FontKit fonts = fontKit();
            Font titleFont = fonts.font(18, Font.BOLD, style.primaryColor());
            Font headingFont = fonts.font(12, Font.BOLD, style.primaryColor());
            Font bodyFont = fonts.font(style.fontSize(), Font.NORMAL, Color.DARK_GRAY);
            Font smallFont = fonts.font(Math.max(8, style.fontSize() - 1), Font.NORMAL, Color.GRAY);

            Document document = new Document(PageSize.A4, 46, 42, 48, 42);
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(paragraph(fonts, profile.getLegalName(), titleFont, Element.ALIGN_CENTER, 0, 10));
            document.add(paragraph(fonts, "住宅装饰装修工程报价单", headingFont, Element.ALIGN_CENTER, 0, 8));
            if (enabled(template.getFieldConfig(), "showCompanyAddress") || enabled(template.getFieldConfig(), "showServiceHotline")) {
                String companyLine = join("  ", profile.getAddress(),
                        enabled(template.getFieldConfig(), "showServiceHotline") && profile.getServiceHotline() != null
                                ? "服务热线：" + profile.getServiceHotline() : null);
                if (!companyLine.isBlank()) document.add(paragraph(fonts, companyLine, smallFont, Element.ALIGN_CENTER, 0, 12));
            }

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            addMeta(fonts, meta, "报价编号", quote.getNo(), bodyFont);
            addMeta(fonts, meta, "报价版本", "V" + value(quote.getVersion(), 1), bodyFont);
            addMeta(fonts, meta, "分组方式", groupMode.getName(), bodyFont);
            addMeta(fonts, meta, "客户名称", quote.getCustomer() == null ? "" : quote.getCustomer().getName(), bodyFont);
            addMeta(fonts, meta, "报价名称", quote.getTitle(), bodyFont);
            if (enabled(template.getFieldConfig(), "showPropertyAddress")) {
                addMeta(fonts, meta, "工程地点", quote.getProperty() == null ? "" : quote.getProperty().getAddress(), bodyFont);
            }
            if (enabled(template.getFieldConfig(), "showPropertyArea")) {
                addMeta(fonts, meta, "计价面积", amount(quote.getArea()) + " 平方米", bodyFont);
            }
            document.add(meta);
            document.add(paragraph(fonts, " ", smallFont, Element.ALIGN_LEFT, 0, 8));

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{0.55f, 1.15f, 2.35f, 0.8f, 0.75f, 1.0f, 1.05f});
            addHeader(fonts, table, "序号", headingFont, style.primaryColor());
            addHeader(fonts, table, "分类", headingFont, style.primaryColor());
            addHeader(fonts, table, "项目名称", headingFont, style.primaryColor());
            addHeader(fonts, table, "单位", headingFont, style.primaryColor());
            addHeader(fonts, table, "数量", headingFont, style.primaryColor());
            addHeader(fonts, table, "单价", headingFont, style.primaryColor());
            addHeader(fonts, table, "金额", headingFont, style.primaryColor());

            Quote quoteView = quoteViewContext.applyForPrint(quote, lines);
            List<QuoteGroup> groups = groupMode == QuoteViewMode.TYPE
                    ? quoteView.getTypeGroups() : quoteView.getRegionGroups();
            int index = 1;
            if (groups != null) {
                for (QuoteGroup group : groups) {
                    addGroupRow(fonts, table, groupLabel(group), group, headingFont, style.primaryColor());
                    for (QuoteLine line : group.getItems()) {
                        addCell(fonts, table, String.valueOf(index++), bodyFont, Element.ALIGN_CENTER);
                        addCell(fonts, table, value(line.getCategory()), bodyFont, Element.ALIGN_LEFT);
                        addCell(fonts, table, lineName(line, template), bodyFont, Element.ALIGN_LEFT);
                        addCell(fonts, table, unitLabel(line.getUnit()), bodyFont, Element.ALIGN_CENTER);
                        addCell(fonts, table, amount(line.getQuantity()), bodyFont, Element.ALIGN_RIGHT);
                        addCell(fonts, table, money(line.getPrice()), bodyFont, Element.ALIGN_RIGHT);
                        addCell(fonts, table, money(line.getAmount()), bodyFont, Element.ALIGN_RIGHT);
                    }
                }
            }
            document.add(table);

            Paragraph total = new Paragraph(fonts.phrase("报价合计：￥" + money(quote.getAmount()), headingFont));
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(10);
            document.add(total);
            if (enabled(template.getFieldConfig(), "showTerms")) {
                document.add(paragraph(fonts, "说明：本报价单为线下打印稿，仅用于客户沟通和人工签署，不构成电子合同或电子签；最终价格及施工范围以双方线下签署文件为准。",
                        smallFont, Element.ALIGN_LEFT, 12, 0));
            }
            document.add(paragraph(fonts, "打印稿生成时间：" + DATE_TIME.format(LocalDateTime.now()), smallFont,
                    Element.ALIGN_RIGHT, 18, 0));
            document.close();
            return output.toByteArray();
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "报价PDF生成失败：" + ex.getMessage()));
        }
    }

    private String groupLabel(QuoteGroup group) {
        String category = value(group.getName());
        if (group.getType() == null) {
            return category;
        }
        return group.getType().getName() + " · " + category;
    }

    private void addGroupRow(FontKit fonts, PdfPTable table, String label, QuoteGroup group,
                             Font font, Color color) {
        String subtotal = group.getAmount() == null
                ? "暂不可确认"
                : "￥" + money(group.getAmount());
        PdfPCell cell = new PdfPCell(fonts.phrase(label + "（" + group.getItems().size() + " 项）    小计 " + subtotal, font));
        cell.setColspan(7);
        cell.setPadding(6);
        cell.setBackgroundColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 36));
        table.addCell(cell);
    }

    /** 单位统一输出中文，避免 m² 这类字形在部分阅读器缺字。 */
    private String unitLabel(Unit unit) {
        if (unit == null) return "";
        return switch (unit) {
            case Area -> "平方米";
            case Metre -> "米";
            case Centimeter -> "厘米";
            default -> clean(unit.getName());
        };
    }

    /** 历史数据存在 m²/㎡ 等写法，渲染前统一成中文字形。 */
    private static String clean(String text) {
        if (text == null) return "";
        return text.replace("m²", "平方米")
                .replace("M²", "平方米")
                .replace("㎡", "平方米")
                .replace("m2", "平方米");
    }

    /** 中文字形与西文字形分开嵌入，并用 FontSelector 按字符自动选字体。 */
    private FontKit fontKit() {
        return new FontKit(embeddedFont(CJK_FONT_BYTES, CJK_FONT_NAME), embeddedFont(LATIN_FONT_BYTES, LATIN_FONT_NAME));
    }

    private BaseFont embeddedFont(byte[] bytes, String name) {
        if (bytes != null) {
            try {
                return BaseFont.createFont(name, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, bytes, null);
            } catch (Exception ignored) {
                // 嵌入字体异常时退回 Adobe CJK 字体，保证打印稿仍可生成
            }
        }
        try {
            return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "PDF 字体初始化失败：" + ex.getMessage()));
        }
    }

    private static byte[] readFont(String resource) {
        try (InputStream input = QuotePdfRenderer.class.getResourceAsStream(resource)) {
            return input == null ? null : input.readAllBytes();
        } catch (Exception ex) {
            return null;
        }
    }

    private String lineName(QuoteLine line, DocumentTemplate template) {
        StringBuilder name = new StringBuilder(value(line.getName()));
        if (enabled(template.getFieldConfig(), "showLineBrand") && notBlank(line.getBrand())) {
            name.append(" / ").append(line.getBrand().trim());
        }
        if (enabled(template.getFieldConfig(), "showLineSpec") && notBlank(line.getSpec())) {
            name.append(" / ").append(line.getSpec().trim());
        }
        return name.toString();
    }

    private RenderStyle style(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return new RenderStyle(color(node.path("primaryColor").asText("#0E8A7D")),
                    node.path("fontSize").asInt(10), node.path("rowSpacing").asInt(2));
        } catch (Exception ex) {
            return new RenderStyle(new Color(14, 138, 125), 10, 2);
        }
    }

    private boolean enabled(String json, String key) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return !node.has(key) || node.path(key).asBoolean(true);
        } catch (Exception ex) {
            return true;
        }
    }

    private Paragraph paragraph(FontKit fonts, String text, Font font, int align,
                                float spacingBefore, float spacingAfter) {
        Paragraph paragraph = new Paragraph(fonts.phrase(value(text), font));
        paragraph.setAlignment(align);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private void addMeta(FontKit fonts, PdfPTable table, String label, String value, Font font) {
        addCell(fonts, table, label, font, Element.ALIGN_LEFT);
        addCell(fonts, table, value(value), font, Element.ALIGN_LEFT);
    }

    private void addHeader(FontKit fonts, PdfPTable table, String text, Font font, Color color) {
        PdfPCell cell = new PdfPCell(fonts.phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        cell.setBackgroundColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
        table.addCell(cell);
    }

    private void addCell(FontKit fonts, PdfPTable table, String text, Font font, int align) {
        PdfPCell cell = new PdfPCell(fonts.phrase(value(text), font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String join(String separator, String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (!notBlank(value)) continue;
            if (!result.isEmpty()) result.append(separator);
            result.append(value.trim());
        }
        return result.toString();
    }

    private String money(BigDecimal value) {
        return amount(value);
    }

    private String amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private int value(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Color color(String hex) {
        try {
            return new Color(Integer.parseInt(hex.substring(1), 16));
        } catch (Exception ex) {
            return new Color(14, 138, 125);
        }
    }

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }

    /** 一次渲染内的字体组合：中文缺西文字形、Roboto 缺中文字形，按字符分别取字体。 */
    private static final class FontKit {
        private final BaseFont cjk;
        private final BaseFont latin;

        private FontKit(BaseFont cjk, BaseFont latin) {
            this.cjk = cjk;
            this.latin = latin;
        }

        private Font font(int size, int style, Color color) {
            return new Font(cjk, size, style, color);
        }

        private Phrase phrase(String text, Font font) {
            FontSelector selector = new FontSelector();
            selector.addFont(new Font(cjk, font.getSize(), font.getStyle(), font.getColor()));
            selector.addFont(new Font(latin, font.getSize(), font.getStyle(), font.getColor()));
            Phrase phrase = selector.process(clean(text));
            return phrase == null ? new Phrase("", font) : phrase;
        }
    }

    private record RenderStyle(Color primaryColor, int fontSize, int rowSpacing) {}
}

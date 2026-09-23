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
import com.th4.edge.decoboon.document.configure.DocumentAudience;
import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.plan.model.ContractConstructionPlan;
import com.th4.edge.decoboon.plan.model.ContractConstructionStage;
import com.th4.edge.decoboon.plan.model.ContractPaymentNode;
import com.th4.edge.decoboon.plan.model.ContractPaymentPlan;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * 住宅施工合同打印稿渲染。
 *
 * <p>打印稿固定输出合同主体、金额、付款计划、施工计划与合同范围快照，
 * 不输出成本、利润和内部结算字段，避免内部口径随线下合同外流。</p>
 */
@Component
public class ContractPdfRenderer {

    private static final String CJK_FONT_RESOURCE = "/fonts/DroidSansFallback.ttf";
    private static final String CJK_FONT_NAME = "DroidSansFallback.ttf";
    private static final String LATIN_FONT_RESOURCE = "/fonts/Roboto-Regular.ttf";
    private static final String LATIN_FONT_NAME = "Roboto-Regular.ttf";
    private static final byte[] CJK_FONT_BYTES = readFont(CJK_FONT_RESOURCE);
    private static final byte[] LATIN_FONT_BYTES = readFont(LATIN_FONT_RESOURCE);

    private final ObjectMapper objectMapper;

    public ContractPdfRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] render(ProjectContract contract, List<ContractLine> lines,
                         DocumentTemplate template, DocumentProfile profile, DocumentAudience audience) {
        if (contract == null) {
            throw invalid("合同不存在");
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            RenderStyle style = style(template == null ? null : template.getStyleConfig());
            FontKit fonts = fontKit();
            Font titleFont = fonts.font(18, Font.BOLD, style.primaryColor());
            Font headingFont = fonts.font(12, Font.BOLD, style.primaryColor());
            Font bodyFont = fonts.font(style.fontSize(), Font.NORMAL, Color.DARK_GRAY);
            Font smallFont = fonts.font(Math.max(8, style.fontSize() - 1), Font.NORMAL, Color.GRAY);

            Document document = new Document(PageSize.A4, 46, 42, 48, 42);
            PdfWriter.getInstance(document, output);
            document.open();

            String company = profile == null ? null : profile.getLegalName();
            document.add(paragraph(fonts, company, titleFont, Element.ALIGN_CENTER, 0, 8));
            document.add(paragraph(fonts, "住宅装饰装修工程施工合同", headingFont, Element.ALIGN_CENTER, 0, 12));

            PdfPTable meta = new PdfPTable(2);
            meta.setWidthPercentage(100);
            addMeta(fonts, meta, "合同编号", contract.getNo(), bodyFont);
            addMeta(fonts, meta, "签约日期", date(contract.getSignedAt()), bodyFont);
            addMeta(fonts, meta, "甲方（客户）", contract.getCustomer() == null ? null : contract.getCustomer().getName(), bodyFont);
            addMeta(fonts, meta, "联系电话", contract.getCustomer() == null ? null : contract.getCustomer().getTelNo(), bodyFont);
            addMeta(fonts, meta, "工程地址", contract.getProperty() == null ? null : contract.getProperty().getAddress(), bodyFont);
            addMeta(fonts, meta, "建筑面积", contract.getProperty() == null ? null : amount(contract.getProperty().getArea()) + " 平方米", bodyFont);
            addMeta(fonts, meta, "乙方（装企）", company, bodyFont);
            addMeta(fonts, meta, "服务热线", profile == null ? null : profile.getServiceHotline(), bodyFont);
            meta.setSpacingAfter(12);
            document.add(meta);

            if (enabled(template, "showSignedAmount")) {
                BigDecimal signed = contract.getSignedAmount() == null ? contract.getBaselineAmount() : contract.getSignedAmount();
                document.add(paragraph(fonts, "合同金额（人民币）：" + amount(signed) + " 元", headingFont,
                        Element.ALIGN_LEFT, 6, 4));
                document.add(paragraph(fonts, "大写：" + VoucherAmountWords.of(signed), bodyFont,
                        Element.ALIGN_LEFT, 0, 12));
            }

            if (enabled(template, "showPaymentPlan")) {
                document.add(paragraph(fonts, "付款计划", headingFont, Element.ALIGN_LEFT, 6, 6));
                document.add(paymentTable(fonts, contract.getPaymentPlan(), bodyFont, style.primaryColor()));
                if (contract.getPaymentTerms() != null && !contract.getPaymentTerms().isBlank()) {
                    document.add(paragraph(fonts, "补充约定：" + contract.getPaymentTerms(), smallFont,
                            Element.ALIGN_LEFT, 4, 12));
                } else {
                    document.add(paragraph(fonts, "", smallFont, Element.ALIGN_LEFT, 0, 8));
                }
            }

            document.add(paragraph(fonts, "施工阶段计划", headingFont, Element.ALIGN_LEFT, 6, 6));
            document.add(constructionTable(fonts, contract.getConstructionPlan(), bodyFont, style.primaryColor()));

            if (lines != null && !lines.isEmpty()) {
                document.add(paragraph(fonts, "合同范围（签约冻结快照）", headingFont, Element.ALIGN_LEFT, 12, 6));
                document.add(lineTable(fonts, lines, bodyFont, style.primaryColor()));
            }

            if (enabled(template, "showBlankSealPlaceholder")) {
                document.add(paragraph(fonts, " ", bodyFont, Element.ALIGN_LEFT, 18, 0));
                PdfPTable seal = new PdfPTable(2);
                seal.setWidthPercentage(100);
                addSeal(fonts, seal, "甲方（签字/盖章）：", bodyFont);
                addSeal(fonts, seal, "乙方（签字/盖章）：", bodyFont);
                document.add(seal);
                document.add(paragraph(fonts, "本合同打印稿由系统按签约快照生成，金额以合同正文与双方签章件为准。",
                        smallFont, Element.ALIGN_LEFT, 12, 0));
            }
            document.close();
            return output.toByteArray();
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "合同打印稿生成失败：" + ex.getMessage()));
        }
    }

    private PdfPTable paymentTable(FontKit fonts, ContractPaymentPlan plan, Font font, Color color) {
        PdfPTable table = new PdfPTable(new float[]{3, 1.2f, 2, 3});
        table.setWidthPercentage(100);
        header(fonts, table, "节点", font, color);
        header(fonts, table, "比例", font, color);
        header(fonts, table, "金额(元)", font, color);
        header(fonts, table, "触发条件", font, color);
        if (plan == null || plan.getNodes() == null || plan.getNodes().isEmpty()) {
            cell(fonts, table, "暂无付款计划", font, Element.ALIGN_LEFT, 4);
            table.completeRow();
            return table;
        }
        for (ContractPaymentNode node : plan.getNodes()) {
            cell(fonts, table, node.getName(), font, Element.ALIGN_LEFT, 1);
            cell(fonts, table, ratio(node.getRatio()), font, Element.ALIGN_CENTER, 1);
            cell(fonts, table, amount(node.getAmount()), font, Element.ALIGN_RIGHT, 1);
            cell(fonts, table, trigger(node), font, Element.ALIGN_LEFT, 1);
        }
        return table;
    }

    private PdfPTable constructionTable(FontKit fonts, ContractConstructionPlan plan, Font font, Color color) {
        PdfPTable table = new PdfPTable(new float[]{3, 1.2f, 2});
        table.setWidthPercentage(100);
        header(fonts, table, "阶段", font, color);
        header(fonts, table, "计划工期(天)", font, color);
        header(fonts, table, "验收方式", font, color);
        if (plan == null || plan.getStages() == null || plan.getStages().isEmpty()) {
            cell(fonts, table, "暂无施工阶段计划", font, Element.ALIGN_LEFT, 4);
            table.completeRow();
            return table;
        }
        for (ContractConstructionStage stage : plan.getStages()) {
            cell(fonts, table, stage.getName(), font, Element.ALIGN_LEFT, 1);
            cell(fonts, table, stage.getDefaultDays() == null ? "" : String.valueOf(stage.getDefaultDays()),
                    font, Element.ALIGN_CENTER, 1);
            cell(fonts, table, stage.getAcceptanceMode() == null ? null : stage.getAcceptanceMode().getName(),
                    font, Element.ALIGN_LEFT, 1);
        }
        return table;
    }

    private PdfPTable lineTable(FontKit fonts, List<ContractLine> lines, Font font, Color color) {
        PdfPTable table = new PdfPTable(new float[]{3.2f, 2.4f, 1, 1.2f, 1.6f});
        table.setWidthPercentage(100);
        header(fonts, table, "项目名称", font, color);
        header(fonts, table, "品牌/规格", font, color);
        header(fonts, table, "单位", font, color);
        header(fonts, table, "数量", font, color);
        header(fonts, table, "金额(元)", font, color);
        BigDecimal total = BigDecimal.ZERO;
        for (ContractLine line : lines) {
            cell(fonts, table, line.getName(), font, Element.ALIGN_LEFT, 1);
            cell(fonts, table, join(" / ", line.getBrand(), line.getSpec()), font, Element.ALIGN_LEFT, 1);
            cell(fonts, table, line.getUnit() == null ? "" : line.getUnit().getName(), font, Element.ALIGN_CENTER, 1);
            cell(fonts, table, amount(line.getQuantity()), font, Element.ALIGN_RIGHT, 1);
            cell(fonts, table, amount(line.getAmount()), font, Element.ALIGN_RIGHT, 1);
            total = total.add(line.getAmount() == null ? BigDecimal.ZERO : line.getAmount());
        }
        cell(fonts, table, "合计", font, Element.ALIGN_RIGHT, 1);
        cell(fonts, table, "", font, Element.ALIGN_LEFT, 1);
        cell(fonts, table, "", font, Element.ALIGN_LEFT, 1);
        cell(fonts, table, "", font, Element.ALIGN_LEFT, 1);
        cell(fonts, table, amount(total), font, Element.ALIGN_RIGHT, 1);
        return table;
    }

    private void header(FontKit fonts, PdfPTable table, String text, Font font, Color color) {
        PdfPCell cell = new PdfPCell(fonts.phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        cell.setBackgroundColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 28));
        table.addCell(cell);
    }

    private void cell(FontKit fonts, PdfPTable table, String text, Font font, int align, int colspan) {
        PdfPCell cell = new PdfPCell(fonts.phrase(value(text), font));
        cell.setHorizontalAlignment(align);
        cell.setPadding(4);
        if (colspan > 1) {
            cell.setColspan(colspan);
        }
        table.addCell(cell);
    }

    private void addMeta(FontKit fonts, PdfPTable table, String label, String value, Font font) {
        cell(fonts, table, label, font, Element.ALIGN_LEFT, 1);
        cell(fonts, table, value, font, Element.ALIGN_LEFT, 1);
    }

    private void addSeal(FontKit fonts, PdfPTable table, String text, Font font) {
        cell(fonts, table, text + "\n\n\n", font, Element.ALIGN_LEFT, 1);
    }

    private Paragraph paragraph(FontKit fonts, String text, Font font, int align,
                                float spacingBefore, float spacingAfter) {
        Paragraph paragraph = new Paragraph(fonts.phrase(value(text), font));
        paragraph.setAlignment(align);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private String trigger(ContractPaymentNode node) {
        if (node.getTriggerStageName() != null && !node.getTriggerStageName().isBlank()) {
            String base = "阶段：" + node.getTriggerStageName();
            return node.getOffsetDays() == null || node.getOffsetDays() == 0
                    ? base : base + "（" + node.getOffsetDays() + "天）";
        }
        return node.getDueDate() == null ? "签约后" : date(node.getDueDate());
    }

    private String ratio(BigDecimal ratio) {
        if (ratio == null) {
            return "";
        }
        return ratio.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString() + "%";
    }

    private String date(LocalDate value) {
        return value == null ? "" : value.toString();
    }

    private String amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String join(String separator, String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(separator);
            }
            result.append(value.trim());
        }
        return result.toString();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private RenderStyle style(String json) {
        try {
            JsonNode node = objectMapper.readTree(json == null ? "{}" : json);
            return new RenderStyle(color(node.path("primaryColor").asText("#0E8A7D")),
                    node.path("fontSize").asInt(10));
        } catch (Exception ex) {
            return new RenderStyle(new Color(14, 138, 125), 10);
        }
    }

    private boolean enabled(DocumentTemplate template, String key) {
        if (template == null || template.getFieldConfig() == null) {
            return true;
        }
        try {
            JsonNode node = objectMapper.readTree(template.getFieldConfig());
            return !node.has(key) || node.path(key).asBoolean(true);
        } catch (Exception ex) {
            return true;
        }
    }

    private Color color(String hex) {
        try {
            return new Color(Integer.parseInt(hex.substring(1), 16));
        } catch (Exception ex) {
            return new Color(14, 138, 125);
        }
    }

    private static byte[] readFont(String resource) {
        try (InputStream input = ContractPdfRenderer.class.getResourceAsStream(resource)) {
            return input == null ? null : input.readAllBytes();
        } catch (Exception ex) {
            return null;
        }
    }

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

    private DefinedException invalid(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }

    private record RenderStyle(Color primaryColor, int fontSize) {
    }

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

        private static String clean(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("m²", "平方米").replace("M²", "平方米")
                    .replace("㎡", "平方米").replace("m2", "平方米");
        }
    }
}

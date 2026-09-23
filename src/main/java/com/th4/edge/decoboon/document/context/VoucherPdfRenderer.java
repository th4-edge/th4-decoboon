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
import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import com.th4.edge.decoboon.document.model.VoucherItem;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 凭证 PDF 渲染。
 *
 * <p>按《凭证套打与单据模板字段冻结清单》渲染：平台基线开箱即用，
 * L2 受控参数（字段显隐、企业抬头、字号、主色、签字栏）来自模板配置。
 * 不含会计科目与借贷分录。</p>
 */
@Component
public class VoucherPdfRenderer {

    private static final String CJK_FONT_RESOURCE = "/fonts/DroidSansFallback.ttf";
    private static final String CJK_FONT_NAME = "DroidSansFallback.ttf";
    private static final String LATIN_FONT_RESOURCE = "/fonts/Roboto-Regular.ttf";
    private static final String LATIN_FONT_NAME = "Roboto-Regular.ttf";
    private static final byte[] CJK_FONT_BYTES = readFont(CJK_FONT_RESOURCE);
    private static final byte[] LATIN_FONT_BYTES = readFont(LATIN_FONT_RESOURCE);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final ObjectMapper objectMapper;

    public VoucherPdfRenderer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public byte[] render(VoucherRecord record, List<VoucherItem> items, DocumentTemplate template,
                         DocumentProfile profile, Worker worker, Partner partner) {
        if (record == null) throw new DefinedException(new DefaultErrorCode("400", "凭证不存在"));
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            RenderStyle style = style(template == null ? null : template.getStyleConfig());
            FontKit fonts = fontKit();
            String config = template == null ? null : template.getFieldConfig();
            Font titleFont = fonts.font(style.fontSize() + 6, Font.BOLD, style.primaryColor());
            Font headFont = fonts.font(style.fontSize(), Font.BOLD, Color.DARK_GRAY);
            Font bodyFont = fonts.font(style.fontSize(), Font.NORMAL, Color.DARK_GRAY);
            Font smallFont = fonts.font(Math.max(8, style.fontSize() - 1), Font.NORMAL, Color.GRAY);

            Document document = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(document, output);
            document.open();

            if (show(config, "showCompanyName") || show(config, "showServiceHotline")) {
                String line = join(" · ", show(config, "showCompanyName") && profile != null ? profile.getLegalName() : null,
                        show(config, "showServiceHotline") && profile != null ? "服务热线 " + value(profile.getServiceHotline()) : null);
                if (!line.isBlank()) document.add(paragraph(fonts, line, smallFont, Element.ALIGN_CENTER, 0, 6));
            }
            document.add(paragraph(fonts, value(record.getTitle()), titleFont, Element.ALIGN_CENTER, 0, 12));

            PdfPTable meta = new PdfPTable(4);
            meta.setWidthPercentage(100);
            meta.setWidths(new float[]{18, 32, 18, 32});
            if (show(config, "showVoucherNo") || show(config, "showVoucherDate")) {
                addMeta(fonts, meta, "凭证编号", show(config, "showVoucherNo") ? record.getVoucherNo() : "", bodyFont);
                addMeta(fonts, meta, "开具日期",
                        show(config, "showVoucherDate") && record.getGeneratedAt() != null
                                ? record.getGeneratedAt().toLocalDate().format(DATE) : "", bodyFont);
            }
            if (show(config, "showPartyName") || show(config, "showWorkerPhone")) {
                addMeta(fonts, meta, partyLabel(record), show(config, "showPartyName") ? record.getPartyName() : "", bodyFont);
                addMeta(fonts, meta, "联系电话", show(config, "showWorkerPhone") ? partyPhone(worker, partner) : "", bodyFont);
            }
            if (show(config, "showWorkerIdCard") || show(config, "showWorkerTrade")) {
                addMeta(fonts, meta, "身份证号码",
                        show(config, "showWorkerIdCard") && worker != null ? value(worker.getIdCardNo()) : "", bodyFont);
                addMeta(fonts, meta, "工种", show(config, "showWorkerTrade") && worker != null ? value(worker.getTrade()) : "", bodyFont);
            }
            if (show(config, "showProject") || show(config, "showDetailTarget")) {
                addMeta(fonts, meta, "所属项目",
                        show(config, "showProject") && record.getProject() != null ? value(record.getProject().getName()) : "", bodyFont);
                addMeta(fonts, meta, "到账账户", show(config, "showDetailTarget") ? targetAccount(worker, partner) : "", bodyFont);
            }
            if (meta.size() > 0) document.add(meta);
            document.add(paragraph(fonts, " ", smallFont, Element.ALIGN_LEFT, 0, 6));

            if (show(config, "showAmount") || show(config, "showAmountWords")) {
                PdfPTable money = new PdfPTable(4);
                money.setWidthPercentage(100);
                money.setWidths(new float[]{18, 32, 18, 32});
                addMeta(fonts, money, "金额合计", show(config, "showAmount") ? "￥" + money(record.getTotalAmount()) : "", bodyFont);
                addMeta(fonts, money, "金额大写", show(config, "showAmountWords") ? value(record.getAmountWords()) : "", bodyFont);
                document.add(money);
                document.add(paragraph(fonts, " ", smallFont, Element.ALIGN_LEFT, 0, 6));
            }

            document.add(detailTable(fonts, config, items, headFont, bodyFont, style));

            if (show(config, "showSignBlock")) {
                document.add(paragraph(fonts, " ", smallFont, Element.ALIGN_LEFT, 12, 6));
                String sign = join("        ", "制单：" + value(record.getGeneratedBy() == null ? null : record.getGeneratedBy().getName()),
                        "审核：____________", "领款人签字：____________");
                document.add(paragraph(fonts, sign, bodyFont, Element.ALIGN_LEFT, 0, 0));
            }
            document.add(paragraph(fonts, "本凭证为款项收付单据打印稿，用于企业留档；不属于会计记账凭证，不替代会计做账。",
                    smallFont, Element.ALIGN_LEFT, 14, 0));
            document.close();
            return output.toByteArray();
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "凭证生成失败：" + ex.getMessage()));
        }
    }

    private PdfPTable detailTable(FontKit fonts, String config, List<VoucherItem> items,
                                  Font headFont, Font bodyFont, RenderStyle style) throws Exception {
        boolean showNo = show(config, "showDetailNo");
        boolean showDate = show(config, "showDetailDate");
        boolean showAccount = show(config, "showDetailAccount");
        boolean showAmount = show(config, "showDetailAmount");
        boolean showTarget = show(config, "showDetailTarget");
        boolean showCategory = show(config, "showDetailCategory");
        boolean showRemark = show(config, "showDetailRemark");
        int columns = (showNo ? 1 : 0) + (showDate ? 1 : 0) + (showAccount ? 1 : 0)
                + (showAmount ? 1 : 0) + (showTarget ? 1 : 0) + (showCategory ? 1 : 0) + (showRemark ? 1 : 0);
        if (columns == 0) columns = 1;
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        if (showNo) addHeader(fonts, table, "来源单号", headFont, style.primaryColor());
        if (showDate) addHeader(fonts, table, "日期", headFont, style.primaryColor());
        if (showAccount) addHeader(fonts, table, "支付账户", headFont, style.primaryColor());
        if (showAmount) addHeader(fonts, table, "金额", headFont, style.primaryColor());
        if (showTarget) addHeader(fonts, table, "到账账户", headFont, style.primaryColor());
        if (showCategory) addHeader(fonts, table, "款项类型", headFont, style.primaryColor());
        if (showRemark) addHeader(fonts, table, "备注", headFont, style.primaryColor());
        if (items == null || items.isEmpty()) {
            addCell(fonts, table, "无明细", bodyFont, Element.ALIGN_LEFT);
            for (int index = 1; index < columns; index++) addCell(fonts, table, "", bodyFont, Element.ALIGN_LEFT);
            return table;
        }
        for (VoucherItem item : items) {
            if (showNo) addCell(fonts, table, value(item.getSourceNo()), bodyFont, Element.ALIGN_LEFT);
            if (showDate) addCell(fonts, table,
                    item.getBusinessDate() == null ? "" : item.getBusinessDate().format(DATE), bodyFont, Element.ALIGN_LEFT);
            if (showAccount) addCell(fonts, table, value(item.getAccount() == null ? null : item.getAccount().getName()),
                    bodyFont, Element.ALIGN_LEFT);
            if (showAmount) addCell(fonts, table, money(item.getAmount()), bodyFont, Element.ALIGN_RIGHT);
            if (showTarget) addCell(fonts, table, "", bodyFont, Element.ALIGN_LEFT);
            if (showCategory) addCell(fonts, table, value(item.getCategory()), bodyFont, Element.ALIGN_LEFT);
            if (showRemark) addCell(fonts, table, value(item.getRemark()), bodyFont, Element.ALIGN_LEFT);
        }
        return table;
    }

    private String partyLabel(VoucherRecord record) {
        if (record.getWorker() != null) return "收款人";
        return record.getType() == com.th4.edge.decoboon.document.configure.DocumentType.Receipt ? "付款方" : "收款方";
    }

    private String partyPhone(Worker worker, Partner partner) {
        if (worker != null) return value(worker.getPhone());
        if (partner != null) return value(partner.getTelNo());
        return "";
    }

    private String targetAccount(Worker worker, Partner partner) {
        if (worker != null) return value(worker.getBankAccount());
        if (partner != null) return value(partner.getBankAccount());
        return "";
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

    private Paragraph paragraph(FontKit fonts, String text, Font font, int align,
                                float spacingBefore, float spacingAfter) {
        Paragraph paragraph = new Paragraph(fonts.phrase(text, font));
        paragraph.setAlignment(align);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        return paragraph;
    }

    private RenderStyle style(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            int size = node.path("fontSize").asInt(12);
            return new RenderStyle(color(node.path("primaryColor").asText("#0E8A7D")),
                    Math.max(9, Math.min(size, 14)));
        } catch (Exception ex) {
            return new RenderStyle(new Color(14, 138, 125), 12);
        }
    }

    /** 未配置视为显示；显式 false 才隐藏（与模板受控参数语义一致）。 */
    private boolean show(String json, String key) {
        if (json == null || json.isBlank()) return true;
        try {
            JsonNode node = objectMapper.readTree(json);
            return !node.has(key) || node.path(key).asBoolean(true);
        } catch (Exception ex) {
            return true;
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
                // 嵌入字体异常时退回 Adobe CJK 字体
            }
        }
        try {
            return BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "PDF 字体初始化失败：" + ex.getMessage()));
        }
    }

    private static byte[] readFont(String resource) {
        try (InputStream input = VoucherPdfRenderer.class.getResourceAsStream(resource)) {
            return input == null ? null : input.readAllBytes();
        } catch (Exception ex) {
            return null;
        }
    }

    private Color color(String hex) {
        try {
            return Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        } catch (Exception ex) {
            return new Color(14, 138, 125);
        }
    }

    private String join(String separator, String... values) {
        StringBuilder result = new StringBuilder();
        for (String item : values) {
            if (item == null || item.isBlank()) continue;
            if (!result.isEmpty()) result.append(separator);
            result.append(item.trim());
        }
        return result.toString();
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
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
            Phrase phrase = selector.process(text == null ? "" : text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", ""));
            return phrase == null ? new Phrase("", font) : phrase;
        }
    }
}

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
            LayoutSpec layout = layout(template == null ? null : template.getLayoutConfig());
            FontKit fonts = fontKit();
            String config = template == null ? null : template.getFieldConfig();
            Font titleFont = fonts.font(style.fontSize() + 6, Font.BOLD, style.primaryColor());
            Font headFont = fonts.font(style.fontSize(), Font.BOLD, Color.DARK_GRAY);
            Font bodyFont = fonts.font(style.fontSize(), Font.NORMAL, Color.DARK_GRAY);
            Font smallFont = fonts.font(Math.max(8, style.fontSize() - 1), Font.NORMAL, Color.GRAY);

            float margin = layout.marginMm() * 2.83465f;
            Document document = new Document(pageSize(layout), margin, margin, margin, margin);
            PdfWriter.getInstance(document, output);
            document.open();

            for (BlockSpec block : layout.blocks().stream()
                    .sorted(java.util.Comparator.comparingInt(BlockSpec::sort)).toList()) {
                if (!block.visible()) continue;
                switch (block.key()) {
                    case "company" -> renderCompany(document, fonts, smallFont, config, profile);
                    case "title" -> document.add(paragraph(fonts, value(record.getTitle()), titleFont,
                            Element.ALIGN_CENTER, 0, 12));
                    case "meta" -> renderMeta(document, fonts, config, record, worker, partner, bodyFont);
                    case "amount" -> renderAmount(document, fonts, config, record, bodyFont);
                    case "detail" -> document.add(detailTable(fonts, config, items, headFont, bodyFont, style));
                    case "sign" -> renderSign(document, fonts, config, record, bodyFont, smallFont, layout);
                    case "footer" -> renderFooter(document, fonts, smallFont, layout);
                    default -> { }
                }
            }

            document.close();
            return output.toByteArray();
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "凭证生成失败：" + ex.getMessage()));
        }
    }

    private void renderCompany(Document document, FontKit fonts, Font font, String config,
                               DocumentProfile profile) throws Exception {
        if (!show(config, "showCompanyName") && !show(config, "showServiceHotline")) return;
        String line = join(" · ", show(config, "showCompanyName") && profile != null ? profile.getLegalName() : null,
                show(config, "showServiceHotline") && profile != null ? "服务热线 " + value(profile.getServiceHotline()) : null);
        if (!line.isBlank()) document.add(paragraph(fonts, line, font, Element.ALIGN_CENTER, 0, 6));
    }

    private void renderMeta(Document document, FontKit fonts, String config, VoucherRecord record,
                            Worker worker, Partner partner, Font font) throws Exception {
        PdfPTable meta = new PdfPTable(4);
        meta.setWidthPercentage(100);
        meta.setWidths(new float[]{18, 32, 18, 32});
        if (show(config, "showVoucherNo") || show(config, "showVoucherDate")) {
            addMeta(fonts, meta, "凭证编号", show(config, "showVoucherNo") ? record.getVoucherNo() : "", font);
            addMeta(fonts, meta, "开具日期",
                    show(config, "showVoucherDate") && record.getGeneratedAt() != null
                            ? record.getGeneratedAt().toLocalDate().format(DATE) : "", font);
        }
        if (show(config, "showPartyName") || show(config, "showWorkerPhone")) {
            addMeta(fonts, meta, partyLabel(record), show(config, "showPartyName") ? record.getPartyName() : "", font);
            addMeta(fonts, meta, "联系电话", show(config, "showWorkerPhone") ? partyPhone(worker, partner) : "", font);
        }
        if (show(config, "showWorkerIdCard") || show(config, "showWorkerTrade")) {
            addMeta(fonts, meta, "身份证号码",
                    show(config, "showWorkerIdCard") && worker != null ? value(worker.getIdCardNo()) : "", font);
            addMeta(fonts, meta, "工种", show(config, "showWorkerTrade") && worker != null ? value(worker.getTrade()) : "", font);
        }
        if (show(config, "showProject") || show(config, "showDetailTarget")) {
            addMeta(fonts, meta, "所属项目",
                    show(config, "showProject") && record.getProject() != null ? value(record.getProject().getName()) : "", font);
            addMeta(fonts, meta, "到账账户", show(config, "showDetailTarget") ? targetAccount(worker, partner) : "", font);
        }
        if (VoucherRecord.SOURCE_EXPENSE.equals(record.getSourceType())) {
            addMeta(fonts, meta, "费用编号", value(record.getExpenseNo()), font);
            addMeta(fonts, meta, "费用分类", value(record.getExpenseCategory()), font);
            addMeta(fonts, meta, "提交人", value(record.getExpenseRequester()), font);
            addMeta(fonts, meta, "收款员工", value(record.getExpensePayee()), font);
            addMeta(fonts, meta, "审批版本",
                    record.getExpenseApprovalVersion() == null ? "" : "v" + record.getExpenseApprovalVersion(), font);
            addMeta(fonts, meta, "费用归属", value(record.getExpenseAttribution()), font);
        }
        if (meta.size() > 0) document.add(meta);
        document.add(paragraph(fonts, " ", font, Element.ALIGN_LEFT, 0, 6));
    }

    private void renderAmount(Document document, FontKit fonts, String config, VoucherRecord record,
                              Font font) throws Exception {
        if (!show(config, "showAmount") && !show(config, "showAmountWords")) return;
        PdfPTable money = new PdfPTable(4);
        money.setWidthPercentage(100);
        money.setWidths(new float[]{18, 32, 18, 32});
        addMeta(fonts, money, "金额合计", show(config, "showAmount") ? "￥" + money(record.getTotalAmount()) : "", font);
        addMeta(fonts, money, "金额大写", show(config, "showAmountWords") ? value(record.getAmountWords()) : "", font);
        document.add(money);
        document.add(paragraph(fonts, " ", font, Element.ALIGN_LEFT, 0, 6));
    }

    private void renderSign(Document document, FontKit fonts, String config, VoucherRecord record,
                            Font bodyFont, Font smallFont, LayoutSpec layout) throws Exception {
        if (!show(config, "showSignBlock")) return;
        document.add(paragraph(fonts, " ", smallFont, Element.ALIGN_LEFT, 12, 6));
        List<String> signatures = new java.util.ArrayList<>();
        for (String label : layout.signatures()) {
            if ("制单".equals(label) && record.getGeneratedBy() != null) {
                signatures.add(label + "：" + value(record.getGeneratedBy().getName()));
            } else {
                signatures.add(label + "：____________");
            }
        }
        document.add(paragraph(fonts, join("        ", signatures.toArray(String[]::new)),
                bodyFont, Element.ALIGN_LEFT, 0, 0));
    }

    private void renderFooter(Document document, FontKit fonts, Font font, LayoutSpec layout) throws Exception {
        if (layout.footer() == null || layout.footer().isBlank()) return;
        document.add(paragraph(fonts, layout.footer(), font, Element.ALIGN_LEFT, 14, 0));
    }

    private com.lowagie.text.Rectangle pageSize(LayoutSpec layout) {
        com.lowagie.text.Rectangle size = switch (layout.pageSize()) {
            case "A3" -> PageSize.A3;
            case "A5" -> PageSize.A5;
            default -> PageSize.A4;
        };
        return "landscape".equals(layout.orientation()) ? size.rotate() : size;
    }

    private LayoutSpec layout(String json) {
        try {
            JsonNode root = objectMapper.readTree(json == null || json.isBlank()
                    ? DocumentLayoutDefaults.JSON : json);
            String pageSize = root.path("pageSize").asText("A4").toUpperCase();
            if (!List.of("A3", "A4", "A5").contains(pageSize)) pageSize = "A4";
            String orientation = root.path("orientation").asText("portrait").toLowerCase();
            if (!List.of("portrait", "landscape").contains(orientation)) orientation = "portrait";
            int margin = Math.max(8, Math.min(root.path("marginMm").asInt(12), 20));
            List<BlockSpec> blocks = new java.util.ArrayList<>();
            int defaultSort = 10;
            for (String key : List.of("company", "title", "meta", "amount", "detail", "sign", "footer")) {
                JsonNode source = null;
                for (JsonNode item : root.path("blocks")) {
                    if (key.equals(item.path("key").asText(""))) {
                        source = item;
                        break;
                    }
                }
                boolean visible = source == null || !source.has("visible") || source.path("visible").asBoolean(true);
                int sort = source != null && source.has("sort") ? source.path("sort").asInt(defaultSort) : defaultSort;
                blocks.add(new BlockSpec(key, visible, Math.max(1, Math.min(sort, 999))));
                defaultSort += 10;
            }
            List<String> signatures = new java.util.ArrayList<>();
            for (JsonNode item : root.path("signatures")) {
                String label = item.asText("").trim();
                if (!label.isBlank() && signatures.size() < 4) signatures.add(label);
            }
            if (signatures.isEmpty()) signatures.addAll(List.of("制单", "审核", "领款/付款人"));
            String footer = root.path("footer").asText("").trim();
            return new LayoutSpec(pageSize, orientation, margin, List.copyOf(blocks), List.copyOf(signatures), footer);
        } catch (Exception ex) {
            return new LayoutSpec("A4", "portrait", 12,
                    List.of(new BlockSpec("company", true, 10), new BlockSpec("title", true, 20),
                            new BlockSpec("meta", true, 30), new BlockSpec("amount", true, 40),
                            new BlockSpec("detail", true, 50), new BlockSpec("sign", true, 60),
                            new BlockSpec("footer", true, 70)),
                    List.of("制单", "审核", "领款/付款人"),
                    "本凭证为款项收付单据打印稿，用于企业留档；不属于会计记账凭证，不替代会计做账。");
        }
    }

    private record BlockSpec(String key, boolean visible, int sort) {
    }

    private record LayoutSpec(String pageSize, String orientation, int marginMm,
                              List<BlockSpec> blocks, List<String> signatures, String footer) {
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

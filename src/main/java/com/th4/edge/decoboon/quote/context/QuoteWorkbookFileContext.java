package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.model.QuoteWorkbookFile;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.core.annotation.Note;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/** Web 报价工作簿导出。 */
@Note("报价工作簿导出")
@Component
public class QuoteWorkbookFileContext {

    private static final String[] INTERNAL_HEADERS = {
            "行ID（请勿修改）", "区域", "分类/项目", "品牌/规格", "单位",
            "工程量", "工程量公式", "材料价", "人工价", "单价", "金额", "备注"
    };

    private static final String[] CUSTOMER_HEADERS = {
            "序号", "区域", "分类/项目", "品牌/规格", "单位",
            "工程量", "单价", "金额", "备注"
    };

    public QuoteWorkbookFile export(Quote quote, List<QuoteLine> lines) {
        return export(quote, lines, "internal");
    }

    public QuoteWorkbookFile export(Quote quote, List<QuoteLine> lines, String variant) {
        String normalized = variant == null || variant.isBlank() ? "internal" : variant.trim().toLowerCase();
        if (!"internal".equals(normalized) && !"customer".equals(normalized)) {
            throw new DefinedException(new DefaultErrorCode("400", "报价工作簿导出类型仅支持 internal/customer"));
        }
        boolean customer = "customer".equals(normalized);
        String[] headers = customer ? CUSTOMER_HEADERS : INTERNAL_HEADERS;
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("报价明细");
            CellStyle head = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            head.setFont(font);

            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(head);
            }
            int rowIndex = 1;
            for (QuoteLine line : lines) {
                Row row = sheet.createRow(rowIndex);
                String region = line.getSpace() == null || line.getSpace().getName() == null ? "综合项目" : line.getSpace().getName();
                String unit = line.getUnit() == null ? "" : line.getUnit().getName();
                if (customer) {
                    row.createCell(0).setCellValue(rowIndex);
                    row.createCell(1).setCellValue(region);
                    row.createCell(2).setCellValue(nvl(line.getName()));
                    row.createCell(3).setCellValue(join(line.getBrand(), line.getSpec()));
                    row.createCell(4).setCellValue(unit);
                    row.createCell(5).setCellValue(number(line.getQuantity()));
                    row.createCell(6).setCellValue(number(line.getPrice()));
                    row.createCell(7).setCellValue(number(line.getAmount()));
                    row.createCell(8).setCellValue("");
                } else {
                    row.createCell(0).setCellValue(String.valueOf(line.getItemId()));
                    row.createCell(1).setCellValue(region);
                    row.createCell(2).setCellValue(nvl(line.getName()));
                    row.createCell(3).setCellValue(join(line.getBrand(), line.getSpec()));
                    row.createCell(4).setCellValue(unit);
                    row.createCell(5).setCellValue(number(line.getQuantity()));
                    row.createCell(6).setCellValue(nvl(line.getQuantityFormula()));
                    row.createCell(7).setCellValue(number(line.getMaterialPrice()));
                    row.createCell(8).setCellValue(number(line.getManualPrice()));
                    row.createCell(9).setCellValue(number(line.getPrice()));
                    row.createCell(10).setCellValue(number(line.getAmount()));
                    row.createCell(11).setCellValue("");
                }
                rowIndex++;
            }
            if (!customer) {
                sheet.setColumnHidden(0, true);
                sheet.setColumnWidth(0, 0);
            }
            sheet.createFreezePane(0, 1);
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(output);
            byte[] bytes = output.toByteArray();
            QuoteWorkbookFile result = new QuoteWorkbookFile();
            result.setFileName((quote.getNo() == null ? "报价" : quote.getNo()) + (customer ? "-客户版" : "-内部工作版") + "-V" + quote.getVersion() + ".xlsx");
            result.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            result.setContentBase64(Base64.getEncoder().encodeToString(bytes));
            result.setSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            return result;
        } catch (Exception ex) {
            throw new IllegalStateException("报价工作簿导出失败", ex);
        }
    }

    private String nvl(String value) { return value == null ? "" : value; }
    private String join(String left, String right) { return nvl(left) + (right == null || right.isBlank() ? "" : " / " + right); }
    private double number(BigDecimal value) { return value == null ? 0D : value.doubleValue(); }
}

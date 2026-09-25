package com.th4.edge.decoboon.catalog.context;

import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 报价项目库批量导入文件读取：只做文件级识别，不做业务校验。
 *
 * <p>V1.0 模板四张工作表（填写说明 / 报价项目库 / 填写示例 / 选项）中的「报价项目库」
 * 为唯一数据源，第 1 行为固定表头。文件级规则：仅 .xlsx/.csv、单个工作表、一行一个项目、
 * 数据区不得有合并单元格、不超过 2,000 行且不超过 10MB。</p>
 */
@Component
public class CatalogImportParser {

    /** 正式数据工作表名（V1.0）。 */
    public static final String DATA_SHEET = "报价项目库";
    /** 模板数据列数（A–W）。 */
    public static final int COLUMN_COUNT = 23;
    /** 首期数据行上限。 */
    public static final int MAX_ROWS = 2000;
    /** 首期文件大小上限。 */
    public static final long MAX_BYTES = 10L * 1024 * 1024;

    /** 解析结果：原始表头 + 有效数据行（已去除全空行）。 */
    public record ParsedSheet(List<String> header, List<List<String>> rows) { }

    public ParsedSheet parse(String fileName, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择要导入的模板文件"));
        }
        if (bytes.length > MAX_BYTES) {
            throw new DefinedException(new DefaultErrorCode("400", "文件超过 10MB，请拆分后重新上传"));
        }
        String name = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx")) {
            return parseXlsx(bytes);
        }
        if (name.endsWith(".csv")) {
            return parseCsv(bytes);
        }
        throw new DefinedException(new DefaultErrorCode("400", "仅支持 .xlsx 或 .csv 模板文件，请重新下载 V1.0 模板"));
    }

    private ParsedSheet parseXlsx(byte[] bytes) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            Sheet sheet = workbook.getSheet(DATA_SHEET);
            if (sheet == null) {
                throw new DefinedException(new DefaultErrorCode("400",
                        "未找到「" + DATA_SHEET + "」工作表，请重新下载 V1.0 模板并保持工作表名称不变"));
            }
            rejectMergedCells(sheet);
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            List<List<String>> all = new ArrayList<>();
            int lastRow = sheet.getLastRowNum();
            for (int index = 0; index <= lastRow; index++) {
                Row row = sheet.getRow(index);
                List<String> cells = new ArrayList<>(COLUMN_COUNT);
                for (int column = 0; column < COLUMN_COUNT; column++) {
                    cells.add(text(row == null ? null : row.getCell(column), formatter));
                }
                rejectExtraColumns(row, formatter);
                all.add(cells);
            }
            return normalize(all);
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "模板文件无法解析，请确认使用 V1.0 模板且未损坏：" + ex.getMessage()));
        }
    }

    private void rejectMergedCells(Sheet sheet) {
        for (CellRangeAddress region : sheet.getMergedRegions()) {
            // 表头或数据区存在合并单元格都会导致一行多值/列错位，直接阻断解析。
            throw new DefinedException(new DefaultErrorCode("400",
                    "数据区存在合并单元格（第 " + (region.getFirstRow() + 1) + " 行），请取消合并后重新上传"));
        }
    }

    private void rejectExtraColumns(Row row, DataFormatter formatter) {
        if (row == null) return;
        for (int column = COLUMN_COUNT; column <= row.getLastCellNum(); column++) {
            String value = text(row.getCell(column), formatter);
            if (!value.isBlank()) {
                throw new DefinedException(new DefaultErrorCode("400",
                        "第 " + (row.getRowNum() + 1) + " 行存在模板之外的列，请一行只保留一个报价项目并使用 V1.0 模板"));
            }
        }
    }

    private ParsedSheet parseCsv(byte[] bytes) {
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) {
            content = content.substring(1);
        }
        List<List<String>> all = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    field.append(ch);
                }
                continue;
            }
            switch (ch) {
                case '"' -> quoted = true;
                case ',' -> {
                    current.add(field.toString());
                    field.setLength(0);
                }
                case '\r' -> { }
                case '\n' -> {
                    current.add(field.toString());
                    field.setLength(0);
                    all.add(current);
                    current = new ArrayList<>();
                }
                default -> field.append(ch);
            }
        }
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            all.add(current);
        }
        for (List<String> row : all) {
            if (row.size() > COLUMN_COUNT
                    && row.subList(COLUMN_COUNT, row.size()).stream().anyMatch(value -> !value.isBlank())) {
                throw new DefinedException(new DefaultErrorCode("400",
                        "文件中存在模板之外的列，请一行只保留一个报价项目并使用 V1.0 模板"));
            }
            while (row.size() < COLUMN_COUNT) {
                row.add("");
            }
        }
        return normalize(all);
    }

    /** 统一裁剪到模板列数、去除全空行，并校验行数上限。 */
    private ParsedSheet normalize(List<List<String>> rawRows) {
        List<List<String>> cleaned = new ArrayList<>();
        for (List<String> row : rawRows) {
            List<String> cells = new ArrayList<>(COLUMN_COUNT);
            for (int column = 0; column < COLUMN_COUNT; column++) {
                cells.add(column < row.size() ? trim(row.get(column)) : "");
            }
            if (cells.stream().allMatch(String::isBlank)) {
                continue;
            }
            cleaned.add(cells);
        }
        if (cleaned.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "模板中没有可导入的数据行"));
        }
        List<String> header = cleaned.get(0);
        List<List<String>> rows = new ArrayList<>(cleaned.subList(1, cleaned.size()));
        if (rows.size() > MAX_ROWS) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "有效数据行 " + rows.size() + " 行，超过单次 " + MAX_ROWS + " 行上限，请拆分文件后分批导入"));
        }
        return new ParsedSheet(header, rows);
    }

    private String text(Cell cell, DataFormatter formatter) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.BLANK) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}

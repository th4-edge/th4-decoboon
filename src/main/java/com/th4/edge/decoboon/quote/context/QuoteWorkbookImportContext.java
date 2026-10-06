package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.model.QuoteWorkbookImportRequest;
import com.th4.edge.decoboon.quote.service.workbook.importer.ImportResult;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 报价工作簿导入预览。 */
@Note("报价工作簿导入预览")
@Component
public class QuoteWorkbookImportContext {

    private static final DataFormatter FORMATTER = new DataFormatter();

    public ImportResult parse(QuoteWorkbookImportRequest request, List<QuoteLine> lines) {
        if (request.getContentBase64() == null || request.getContentBase64().isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "请先选择报价工作簿文件"));
        }
        Map<Long, QuoteLine> lineById = lines.stream().collect(Collectors.toMap(QuoteLine::getItemId, Function.identity()));
        ImportResult result = new ImportResult();
        Map<Long, String> formulas = new HashMap<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(Base64.getDecoder().decode(request.getContentBase64())))) {
            Sheet sheet = workbook.getSheetAt(0);
            int last = sheet.getLastRowNum();
            for (int index = 1; index <= last; index++) {
                if (index > 201) throw new DefinedException(new DefaultErrorCode("400", "单次最多导入 200 行报价"));
                Row row = sheet.getRow(index);
                if (row == null || blank(row.getCell(0)) && blank(row.getCell(2))) continue;
                ImportResult.Row item = row(row, index + 1);
                result.getRows().add(item);
                if (item.getItemId() == null || !lineById.containsKey(item.getItemId())) {
                    item.getErrors().add("行ID不存在或不属于当前报价");
                    continue;
                }
                if (item.getQuantityFormula() != null && !item.getQuantityFormula().isBlank()) {
                    formulas.put(item.getItemId(), item.getQuantityFormula());
                }
                if (item.getQuantity() != null && item.getQuantity().signum() < 0) item.getErrors().add("工程量不能为负数");
                if (item.getMaterialPrice() != null && item.getMaterialPrice().signum() < 0) item.getErrors().add("材料价不能为负数");
                if (item.getManualPrice() != null && item.getManualPrice().signum() < 0) item.getErrors().add("人工价不能为负数");
            }
            Map<Long, BigDecimal> evaluated = Map.of();
            String formulaError = null;
            try {
                evaluated = formulaContext.evaluate(lines, formulas);
            } catch (DefinedException ex) {
                formulaError = ex.getMessage();
            }
            for (ImportResult.Row item : result.getRows()) {
                if (formulas.containsKey(item.getItemId())) {
                    if (formulaError != null) item.getErrors().add(formulaError);
                    else item.setQuantity(evaluated.get(item.getItemId()).setScale(3, RoundingMode.HALF_UP));
                }
                item.setImportable(item.getErrors().isEmpty());
            }
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("400", "报价工作簿无法解析，请使用系统导出的 .xlsx 模板"));
        }
        result.setTotal(result.getRows().size());
        result.setValid((int) result.getRows().stream().filter(ImportResult.Row::isImportable).count());
        result.setFailed(result.getTotal() - result.getValid());
        return result;
    }

    private final QuoteFormulaContext formulaContext;

    public QuoteWorkbookImportContext(QuoteFormulaContext formulaContext) {
        this.formulaContext = formulaContext;
    }

    private ImportResult.Row row(Row source, int excelRow) {
        ImportResult.Row row = new ImportResult.Row();
        row.setExcelRow(excelRow);
        row.setItemId(longValue(source.getCell(0)));
        row.setRegion(text(source.getCell(1)));
        row.setName(text(source.getCell(2)));
        row.setQuantity(decimal(source.getCell(5)));
        row.setQuantityFormula(text(source.getCell(6)));
        row.setMaterialPrice(decimal(source.getCell(7)));
        row.setManualPrice(decimal(source.getCell(8)));
        return row;
    }

    private boolean blank(Cell cell) { return cell == null || FORMATTER.formatCellValue(cell).trim().isEmpty(); }
    private String text(Cell cell) { return cell == null ? null : FORMATTER.formatCellValue(cell).trim(); }
    private Long longValue(Cell cell) {
        String value = text(cell);
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value).longValueExact(); }
        catch (Exception ex) { return null; }
    }
    private BigDecimal decimal(Cell cell) {
        String value = text(cell);
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.replace(",", "")); }
        catch (Exception ex) { return null; }
    }
}

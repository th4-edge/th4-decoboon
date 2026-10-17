package com.th4.edge.decoboon.material.context;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.material.model.MaterialSheetExportFile;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

/**
 * 主材下单作业单 Excel 单据导出（P2）。
 *
 * <p>目标：把系统里的品类作业单还原成装企熟悉的纸质单据形态（抬头 + 明细 + 签字），
 * 用于**发给厂家、打印签字留档、与既有 Excel 习惯衔接**。</p>
 *
 * <p><b>硬约束</b>：作业单对厂家，导出**不含价格、账期与成本**（`DEC-073`）；
 * 也不含供应商结算信息。导出按调用时快照生成，模板后续升级不影响已导出文件。</p>
 */
@Component
public class MaterialSheetExportContext {

    private static final String CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** 明细表头：与真实装企单据一致的"作业信息"，不含任何金额列。 */
    private static final String[] HEADERS = {
            "序号", "空间", "安装位置", "品牌", "型号", "规格", "颜色/花色", "尺寸", "数量", "单位", "标配含", "备注"
    };

    public MaterialSheetExportFile export(MaterialOrderSheet sheet, List<MaterialOrderSheetItem> items,
                                          MaterialSheetBaseline baseline) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            String title = titleOf(sheet, baseline);
            org.apache.poi.ss.usermodel.Sheet target = workbook.createSheet(sheet.getSheetType());
            CellStyle titleStyle = style(workbook, 16, true, HorizontalAlignment.CENTER, false);
            CellStyle labelStyle = style(workbook, 11, true, HorizontalAlignment.LEFT, false);
            CellStyle valueStyle = style(workbook, 11, false, HorizontalAlignment.LEFT, false);
            CellStyle headStyle = style(workbook, 11, true, HorizontalAlignment.CENTER, true);
            CellStyle bodyStyle = style(workbook, 11, false, HorizontalAlignment.CENTER, true);

            int rowIndex = 0;
            Row titleRow = target.createRow(rowIndex++);
            titleRow.setHeightInPoints(28);
            put(titleRow, 0, title, titleStyle);
            target.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            rowIndex = meta(target, rowIndex++, labelStyle, valueStyle, "设计师",
                    name(sheet.getCreatedBy()), "工程名称", projectName(sheet));
            rowIndex = meta(target, rowIndex++, labelStyle, valueStyle, "建单时间",
                    sheet.getCreateTime() == null ? "" : String.valueOf(sheet.getCreateTime().getDateTime()),
                    "厂家", nvl(sheet.getManufacturerName()));
            rowIndex = meta(target, rowIndex++, labelStyle, valueStyle, "单据编号",
                    nvl(sheet.getSheetNo()), "单据状态",
                    sheet.getStatus() == null ? "" : sheet.getStatus().getName());
            rowIndex = meta(target, rowIndex++, labelStyle, valueStyle, "测量时间",
                    str(sheet.getMeasureAt()), "配送时间", str(sheet.getDeliverAt()));
            rowIndex = meta(target, rowIndex++, labelStyle, valueStyle, "安装/送货时间",
                    str(sheet.getInstallAt()), "备注", nvl(sheet.getRemark()));

            Row header = target.createRow(rowIndex++);
            for (int i = 0; i < HEADERS.length; i++) {
                put(header, i, HEADERS[i], headStyle);
            }
            int index = 1;
            if (items != null) {
                for (MaterialOrderSheetItem item : items) {
                    Row row = target.createRow(rowIndex++);
                    put(row, 0, String.valueOf(index++), bodyStyle);
                    put(row, 1, nvl(item.getSpaceName()), bodyStyle);
                    put(row, 2, nvl(item.getLocationName()), bodyStyle);
                    put(row, 3, nvl(item.getBrand()), bodyStyle);
                    put(row, 4, nvl(item.getModel()), bodyStyle);
                    put(row, 5, nvl(item.getSpec()), bodyStyle);
                    put(row, 6, nvl(item.getColor()), bodyStyle);
                    put(row, 7, nvl(item.getSize()), bodyStyle);
                    put(row, 8, amount(item.getQuantity()), bodyStyle);
                    put(row, 9, item.getUnit() == null ? "" : item.getUnit().getName(), bodyStyle);
                    put(row, 10, Boolean.TRUE.equals(item.getStandardIncluded()) ? "是" : "否", bodyStyle);
                    put(row, 11, nvl(item.getRemark()), bodyStyle);
                }
            }
            Row sign = target.createRow(rowIndex + 1);
            put(sign, 0, "项目经理确认签字：", labelStyle);
            put(sign, 6, "设计师确认签字：", labelStyle);
            for (int i = 0; i < HEADERS.length; i++) {
                target.autoSizeColumn(i);
            }

            workbook.write(output);
            byte[] bytes = output.toByteArray();
            MaterialSheetExportFile file = new MaterialSheetExportFile();
            file.setFileName(nvl(sheet.getSheetNo()) + "-" + (sheet.getSheetType() == null ? "材料" : sheet.getSheetType()) + "下单表.xlsx");
            file.setContentType(CONTENT_TYPE);
            file.setContentBase64(Base64.getEncoder().encodeToString(bytes));
            file.setSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            return file;
        } catch (Exception ex) {
            throw new IllegalStateException("主材下单作业单导出失败", ex);
        }
    }

    // ------------------------------------------------------------------ 内部

    private String titleOf(MaterialOrderSheet sheet, MaterialSheetBaseline baseline) {
        Company company = sheet.getCompany();
        String companyName = company == null || company.getName() == null ? "" : company.getName();
        String typeName = sheet.getSheetType();
        if (baseline != null && sheet.getSheetType() != null) {
            MaterialSheetBaseline.Preset preset = baseline.find(sheet.getSheetType());
            if (preset != null) {
                typeName = preset.name();
            }
        }
        return companyName + typeName + "下单表";
    }

    private int meta(org.apache.poi.ss.usermodel.Sheet sheet, int rowIndex, CellStyle labelStyle,
                     CellStyle valueStyle, String leftLabel, String leftValue, String rightLabel, String rightValue) {
        Row row = sheet.createRow(rowIndex);
        put(row, 0, leftLabel + "：", labelStyle);
        put(row, 1, nvl(leftValue), valueStyle);
        put(row, 6, rightLabel + "：", labelStyle);
        put(row, 7, nvl(rightValue), valueStyle);
        return rowIndex + 1;
    }

    private CellStyle style(XSSFWorkbook workbook, int fontSize, boolean bold, HorizontalAlignment alignment,
                            boolean border) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) fontSize);
        font.setBold(bold);
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (border) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
        return style;
    }

    private void put(Row row, int index, String value, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private String projectName(MaterialOrderSheet sheet) {
        if (sheet.getProject() == null) {
            return "";
        }
        String name = sheet.getProject().getName();
        String address = sheet.getProperty() == null ? null : sheet.getProperty().getAddress();
        if (address == null || address.isBlank()) {
            return nvl(name);
        }
        return nvl(name) + "（" + address + "）";
    }

    private String name(Employee employee) {
        return employee == null ? "" : nvl(employee.getName());
    }

    private String str(java.time.LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private String amount(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String nvl(String value) {
        return value == null ? "" : value;
    }
}

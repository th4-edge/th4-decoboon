package com.th4.edge.decoboon.material;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.material.context.MaterialSheetBaseline;
import com.th4.edge.decoboon.material.context.MaterialSheetExportContext;
import com.th4.edge.decoboon.material.configure.MaterialSheetStatus;
import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialOrderSheetItem;
import com.th4.edge.decoboon.material.model.MaterialSheetExportFile;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 主材下单作业单 Excel 单据导出（P2）。
 *
 * <p>守卫重点：① 单据形态（抬头 + 明细表头 + 签字行）可打印留档；
 * ② **绝不带价格、账期与成本**——作业单对厂家，价格只存在于采购单侧（DEC-073）。</p>
 */
class MaterialSheetExportTest {

    private final MaterialSheetExportContext context = new MaterialSheetExportContext();
    private final MaterialSheetBaseline baseline = new MaterialSheetBaseline();

    private MaterialOrderSheet sheet() {
        Company company = new Company();
        company.setComId(1L);
        company.setName("麦鸿装饰");

        Employee designer = new Employee();
        designer.setEmpId(1L);
        designer.setName("王璐璐");

        Property property = new Property();
        property.setPropertyId(7L);
        property.setAddress("阳光里 12-1802");

        Project project = new Project();
        project.setProjectId(9L);
        project.setName("阳光里 12-1802");
        project.setProperty(property);

        MaterialOrderSheet sheet = new MaterialOrderSheet();
        sheet.setSheetId(1000L);
        sheet.setCompany(company);
        sheet.setProject(project);
        sheet.setProperty(property);
        sheet.setSheetType("Tile");
        sheet.setSheetNo("TILE-20261015-001");
        sheet.setStatus(MaterialSheetStatus.Submitted);
        sheet.setManufacturerName("诗尔曼");
        sheet.setRemark("含退补货");
        sheet.setCreatedBy(designer);
        return sheet;
    }

    private MaterialOrderSheetItem item(String location, String qty) {
        MaterialOrderSheetItem line = new MaterialOrderSheetItem();
        line.setSpaceName("客厅");
        line.setLocationName(location);
        line.setBrand("利家福");
        line.setModel("43021");
        line.setSpec("800*800");
        line.setColor("羊脂白玉");
        line.setSize("800*800");
        line.setQuantity(new BigDecimal(qty));
        line.setUnit(Unit.Sheet);
        line.setStandardIncluded(Boolean.FALSE);
        // 价格字段存在但**不得出现在导出结果中**
        line.setPurchasePrice(new BigDecimal("68"));
        line.setSettlementPrice(new BigDecimal("70"));
        return line;
    }

    private List<String> readAllCells(MaterialSheetExportFile file) throws Exception {
        List<String> values = new ArrayList<>();
        try (XSSFWorkbook workbook = new XSSFWorkbook(
                new ByteArrayInputStream(Base64.getDecoder().decode(file.getContentBase64())))) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                for (Cell cell : row) {
                    values.add(cell.toString());
                }
            }
        }
        return values;
    }

    @Test
    void exportShouldLookLikeAMaterialOrderDocument() throws Exception {
        MaterialSheetExportFile file = context.export(sheet(), List.of(item("客厅地砖", "12")), baseline);

        assertThat(file.getContentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(file.getFileName()).isEqualTo("TILE-20261015-001-Tile下单表.xlsx");
        byte[] bytes = Base64.getDecoder().decode(file.getContentBase64());
        assertThat(file.getSha256())
                .isEqualTo(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));

        List<String> cells = readAllCells(file);
        assertThat(cells).anyMatch(value -> value.contains("麦鸿装饰") && value.contains("下单表"));
        assertThat(cells).contains("项目经理确认签字：", "设计师确认签字：");
        assertThat(cells).contains("安装位置", "数量", "单位", "标配含");
        assertThat(cells)
                .as("建单时间必须与真正的采购下单时间区分")
                .anyMatch(value -> value.contains("建单时间"))
                .noneMatch(value -> value.contains("下单时间"));
        assertThat(cells).contains("客厅地砖", "12", "利家福", "43021", "羊脂白玉");
    }

    @Test
    void exportMustNotLeakPriceOrSettlement() throws Exception {
        List<String> cells = readAllCells(context.export(sheet(), List.of(item("客厅地砖", "12")), baseline));

        assertThat(cells)
                .as("作业单导出不得出现价格/账期/成本类列名（DEC-073）")
                .noneMatch(value -> value.contains("单价") || value.contains("金额") || value.contains("结算")
                        || value.contains("账期") || value.contains("成本") || value.contains("暂估"));
        assertThat(cells)
                .as("采购价 68 与结算价 70 都不得出现在导出内容中")
                .noneMatch(value -> value.equals("68") || value.equals("70") || value.contains("¥"));
    }
}

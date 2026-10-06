package com.th4.edge.decoboon.document;

import com.th4.edge.decoboon.document.configure.DocumentTemplateScope;
import com.th4.edge.decoboon.document.configure.DocumentType;
import com.th4.edge.decoboon.document.context.DocumentTemplateBaseline;
import com.th4.edge.decoboon.document.model.DocumentTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** 平台常用凭证模板库：预设数量、编码与字段差异。 */
class DocumentTemplateBaselineTest {

    private final DocumentTemplateBaseline baseline = new DocumentTemplateBaseline();

    @Test
    void shouldProvideCommonVoucherPresets() {
        assertThat(baseline.presets()).hasSize(28);
        assertThat(baseline.presets())
                .extracting(DocumentTemplate::getPresetCode)
                .contains("P-SALARY", "P-LABOR", "P-SUPPLIER", "P-EXPENSE",
                        "P-SUPPLIER-PREPAY", "P-SUPPLIER-ARRIVAL", "P-SUPPLIER-ACCEPTANCE",
                        "P-LABOR-PROGRESS", "P-LABOR-SETTLEMENT", "E-REIMBURSEMENT",
                        "I-PAYMENT-REQUEST", "I-PAYMENT-APPROVAL", "R-CUSTOMER-REFUND",
                        "F-MANUAL-LEDGER", "F-CORRECTION", "R-STANDARD", "R-DEPOSIT",
                        "R-CUSTOMER-PROGRESS", "R-CUSTOMER-FINAL", "R-CUSTOMER-CHANGE",
                        "R-CUSTOMER-WARRANTY");
        assertThat(baseline.presets()).allSatisfy(preset -> {
            assertThat(preset.getScope()).isEqualTo(DocumentTemplateScope.Platform);
            assertThat(preset.getName()).isNotBlank();
            assertThat(preset.getPresetNote()).isNotBlank();
            assertThat(preset.getLayoutConfig()).contains("A4").contains("portrait");
        });
    }

    @Test
    void defaultTemplateTakesFirstPresetOfItsType() {
        assertThat(baseline.template(DocumentType.Payment).getName()).isEqualTo("工资发放凭证");
        assertThat(baseline.template(DocumentType.Receipt).getName()).isEqualTo("客户收款收据");
        assertThat(baseline.template(DocumentType.Quote).getName()).contains("报价单");
        assertThat(baseline.template(DocumentType.Contract).getName()).contains("合同");
    }

    @Test
    void salaryPresetShowsWorkerIdentityWhileLaborTeamHidesIt() {
        DocumentTemplate salary = preset("P-SALARY");
        DocumentTemplate labor = preset("P-LABOR");
        DocumentTemplate supplier = preset("P-SUPPLIER");

        assertThat(salary.getFieldConfig()).contains("\"showWorkerIdCard\":true", "\"showWorkerTrade\":true");
        assertThat(labor.getFieldConfig()).contains("\"showWorkerIdCard\":false", "\"showWorkerTrade\":false");
        assertThat(supplier.getFieldConfig()).contains("\"showDetailCategory\":true");
        assertThat(salary.getFieldConfig()).contains("\"showDetailCategory\":false");
    }

    private DocumentTemplate preset(String code) {
        return baseline.presets().stream()
                .filter(item -> code.equals(item.getPresetCode()))
                .findFirst()
                .orElseThrow();
    }
}

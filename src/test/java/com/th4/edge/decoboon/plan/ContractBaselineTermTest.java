package com.th4.edge.decoboon.plan;

import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.context.ContractPlanContext;
import com.th4.edge.decoboon.plan.model.ConstructionPlanTemplate;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** DEC-048 CONTRACT-TERM-01/02/T-03：签约锁定合同基准工期（开工日期必填、基准完工日按模板自动生成）。 */
class ContractBaselineTermTest {

    private final CompanyPlanTemplateContext templateContext = mock(CompanyPlanTemplateContext.class);
    private final ContractPlanContext context = new ContractPlanContext(templateContext, null, null, null, null);

    @Test
    void signingMustRequirePlannedStartDate() {
        ProjectContract contract = contract(null, null);

        assertThatThrownBy(() -> context.lockBaselineTerm(contract, null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("确认签约必须填写计划开工日期");
    }

    @Test
    void baselineEndDateIsGeneratedFromTemplateTotalDays() {
        ConstructionPlanTemplate template = new ConstructionPlanTemplate();
        template.setTotalDays(120);
        when(templateContext.resolveConstructionPlan(eq(11L), any())).thenReturn(template);
        ProjectContract contract = contract(LocalDate.of(2026, 10, 20), null);

        context.lockBaselineTerm(contract, null);

        // 基准完工日 = 开工日 + 标准总工期 - 1（自然日，DEC-048 CONTRACT-TERM-02）
        assertThat(contract.getPlanEndDate()).isEqualTo(LocalDate.of(2027, 2, 16));
    }

    @Test
    void explicitPlanEndDateIsNotOverwritten() {
        LocalDate explicit = LocalDate.of(2026, 12, 31);
        ProjectContract contract = contract(LocalDate.of(2026, 10, 20), explicit);

        context.lockBaselineTerm(contract, null);

        assertThat(contract.getPlanEndDate()).isEqualTo(explicit);
    }

    private ProjectContract contract(LocalDate start, LocalDate planEnd) {
        Company company = new Company();
        company.setComId(11L);
        ProjectContract contract = new ProjectContract();
        contract.setCompany(company);
        contract.setStartDate(start);
        contract.setPlanEndDate(planEnd);
        return contract;
    }
}

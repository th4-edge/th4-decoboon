package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.context.DesignCostContext;
import com.th4.edge.decoboon.design.context.DesignPriceContext;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.service.schemes.info.support.Info;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 设计方案详情：计价公式说明与工程量公式必须分开投影。 */
class DesignSchemeInfoFormulaTest {

    private final DesignContext designContext = mock(DesignContext.class);
    private final SpaceRepository spaceRepository = mock(SpaceRepository.class);
    private final DesignItemRepository itemRepository = mock(DesignItemRepository.class);
    private final RequirementRepository requirementRepository = mock(RequirementRepository.class);
    private final DesignPriceContext priceContext = mock(DesignPriceContext.class);
    private final DesignCostContext costContext = mock(DesignCostContext.class);
    private final Info info = new Info(designContext, spaceRepository, itemRepository,
            requirementRepository, new DesignTemplateFormulaContext(), priceContext, costContext);

    @Test
    void pricingFormulaAloneMustNotBeMarkedForRevision() {
        Company company = new Company();
        company.setComId(1L);
        Customer customer = new Customer();
        customer.setCustId(2L);
        Design design = new Design();
        design.setDesignId(10L);
        design.setCompany(company);
        design.setCustomer(customer);

        DesignItem pricingOnly = item(100L, "(材料单价) × 工程量", null);
        DesignItem supportedQuantity = item(101L, "(材料单价) × 工程量", "A");
        DesignItem unsupportedQuantity = item(102L, "(材料单价) × 工程量", "(人工单价) × 工程量");

        when(designContext.requireReadableDesign(10L, null)).thenReturn(design);
        when(spaceRepository.findByDesign_DesignIdOrderBySortAsc(10L)).thenReturn(List.of());
        when(itemRepository.findByDesign_DesignIdOrderBySortAsc(10L))
                .thenReturn(List.of(pricingOnly, supportedQuantity, unsupportedQuantity));
        when(requirementRepository.findByCompany_ComIdAndCustomer_CustIdOrderByRequirementIdDesc(1L, 2L))
                .thenReturn(List.of());
        ReflectionTestUtils.setField(info, "designId", 10L);
        ReflectionTestUtils.setField(info, "employee", (Employee) null);

        info.call(new Design());

        assertThat(pricingOnly.getFormulaRevisionNeeded()).isFalse();
        assertThat(supportedQuantity.getFormulaRevisionNeeded()).isFalse();
        assertThat(unsupportedQuantity.getFormulaRevisionNeeded()).isTrue();
        assertThat(pricingOnly.getFormula()).isEqualTo("(材料单价) × 工程量");
        assertThat(pricingOnly.getQuantityFormula()).isNull();
    }

    private DesignItem item(Long id, String formula, String quantityFormula) {
        DesignItem item = new DesignItem();
        item.setItemId(id);
        item.setFormula(formula);
        item.setQuantityFormula(quantityFormula);
        return item;
    }
}

package com.th4.edge.decoboon.plan;

import com.th4.edge.decoboon.plan.context.CompanyPlanTemplateContext;
import com.th4.edge.decoboon.plan.model.PlanTemplateCatalog;
import com.th4.edge.decoboon.plan.service.template.picker.page.support.Page;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 计划选择器必须调用专用分页上下文。 */
class PlanPickerPageTest {

    @Test
    void planPickerDelegatesToPagedCatalog() {
        CompanyPlanTemplateContext context = mock(CompanyPlanTemplateContext.class);
        Employee boss = new Employee();
        PlanTemplateCatalog expected = new PlanTemplateCatalog();
        expected.setTotal(201);
        when(context.pickerCatalog(boss, "Payment", 1, 20, "月付")).thenReturn(expected);
        Page page = new Page(context);
        ReflectionTestUtils.setField(page, "employee", boss);
        ReflectionTestUtils.setField(page, "kind", "Payment");
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 20);
        ReflectionTestUtils.setField(page, "keyword", "月付");

        assertThat(page.call(boss)).isSameAs(expected);
        verify(context).pickerCatalog(boss, "Payment", 1, 20, "月付");
    }
}

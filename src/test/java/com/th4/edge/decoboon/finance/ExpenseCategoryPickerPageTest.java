package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.context.ExpenseAccessContext;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.repository.ExpenseCategoryRepository;
import com.th4.edge.decoboon.finance.service.expensecategories.page.support.Page;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 费用分类选择器必须使用数据库分页并限制单页大小。 */
class ExpenseCategoryPickerPageTest {

    @Test
    void expenseCategoryPickerCapsPageSize() {
        ExpenseCategoryRepository repository = mock(ExpenseCategoryRepository.class);
        ExpenseAccessContext access = mock(ExpenseAccessContext.class);
        Employee finance = employee();
        when(access.requireSubmitter(finance)).thenReturn(finance);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<ExpenseCategory>(List.of(), invocation.getArgument(1), 0));

        Page page = new Page(access, repository);
        ReflectionTestUtils.setField(page, "employee", finance);
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 200);
        ReflectionTestUtils.setField(page, "keyword", "差旅");

        assertThat(page.call(new ExpenseCategory()).getTotalElements()).isZero();
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    private static Employee employee() {
        Company company = new Company();
        company.setComId(1L);
        Employee employee = new Employee();
        employee.setEmpId(9L);
        employee.setCompany(company);
        return employee;
    }
}

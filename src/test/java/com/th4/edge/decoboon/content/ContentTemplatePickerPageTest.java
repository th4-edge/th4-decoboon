package com.th4.edge.decoboon.content;

import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.decoboon.content.repository.ContentTemplateRepository;
import com.th4.edge.decoboon.content.service.template.page.support.Page;
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

/** 内容模板选择器必须使用数据库分页并限制单页大小。 */
class ContentTemplatePickerPageTest {

    @Test
    void templatePickerCapsPageSize() {
        ContentTemplateRepository repository = mock(ContentTemplateRepository.class);
        ContentAccessContext access = mock(ContentAccessContext.class);
        Employee boss = employee();
        when(access.requireEmployee(boss)).thenReturn(boss);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<ContentTemplate>(List.of(), invocation.getArgument(1), 0));

        Page page = new Page(repository, access);
        ReflectionTestUtils.setField(page, "employee", boss);
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 200);
        ReflectionTestUtils.setField(page, "keyword", "口播");

        assertThat(page.call(new ContentTemplate()).getTotalElements()).isZero();
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

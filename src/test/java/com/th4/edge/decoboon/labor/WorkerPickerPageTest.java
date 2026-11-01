package com.th4.edge.decoboon.labor;

import com.th4.edge.decoboon.labor.configure.WorkerStatus;
import com.th4.edge.decoboon.labor.context.WorkerPrivacyContext;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.labor.repository.WorkerRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 工人选择器必须使用服务端分页与状态过滤。 */
class WorkerPickerPageTest {

    @Test
    void workerPickerPageUsesServerPaginationAndActiveFilter() {
        WorkerRepository repository = mock(WorkerRepository.class);
        WorkerPrivacyContext privacy = mock(WorkerPrivacyContext.class);
        Employee finance = new Employee();
        Company company = new Company();
        company.setComId(1L);
        finance.setCompany(company);
        Worker worker = new Worker();
        worker.setWorkerId(10L);
        worker.setName("张师傅");

        when(privacy.requireViewer(finance)).thenReturn(finance);
        when(privacy.mask(worker, finance)).thenReturn(worker);
        when(repository.findPickerPage(eq(1L), any(), eq("%张%"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(worker)));

        var page = new com.th4.edge.decoboon.labor.service.worker.page.support.Page(repository, privacy);
        ReflectionTestUtils.setField(page, "employee", finance);
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 20);
        ReflectionTestUtils.setField(page, "keyword", "张");
        ReflectionTestUtils.setField(page, "status", "Active");

        var result = page.call(new Worker());

        assertThat(result.getContent()).containsExactly(worker);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<WorkerStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(repository).findPickerPage(eq(1L), statuses.capture(), eq("%张%"), any(Pageable.class));
        assertThat(statuses.getValue()).containsExactly(WorkerStatus.Active);
    }
}

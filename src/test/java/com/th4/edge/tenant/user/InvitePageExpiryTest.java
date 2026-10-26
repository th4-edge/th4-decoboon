package com.th4.edge.tenant.user;

import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.repository.InviteRepository;
import com.th4.edge.tenant.user.service.invite.invitation.page.support.Page;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InvitePageExpiryTest {
    @Test
    void pageMarksTimedOutPendingInviteExpired() {
        TenantContext tenantContext = mock(TenantContext.class);
        InviteRepository inviteRepository = mock(InviteRepository.class);
        Company company = new Company();
        company.setComId(1L);
        Employee boss = new Employee();
        boss.setEmpId(1L);
        boss.setRole(EmployeeRole.Boss);
        boss.setCompany(company);
        Invite expired = new Invite();
        expired.setInviteId(51L);
        expired.setStatus(InviteStatus.Pending);
        expired.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(tenantContext.requireRole(any(Employee.class), eq(EmployeeRole.Boss),
                eq(EmployeeRole.StoreManager), eq(EmployeeRole.TeamLeader))).thenReturn(boss);
        when(inviteRepository.findByCompany_ComId(eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(expired)));
        Page page = new Page(tenantContext, inviteRepository);
        ReflectionTestUtils.setField(page, "employee", boss);
        ReflectionTestUtils.setField(page, "current", 1);
        ReflectionTestUtils.setField(page, "size", 20);

        page.call(new Employee());

        assertThat(expired.getStatus()).isEqualTo(InviteStatus.Expired);
        verify(inviteRepository).saveAll(List.of(expired));
    }
}

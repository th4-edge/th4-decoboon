package com.th4.edge.tenant.user;

import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EntryInviteType;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Invite;
import com.th4.edge.tenant.user.model.PendingEntryInvite;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.InviteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 登录后按已验证手机号发现员工/业主待接受邀请。 */
class PendingEntryInviteProviderTest {

    @Test
    void shouldReturnEmployeeInviteBeforeCustomerInviteWithMaskedPhone() {
        UserIdentityContext userContext = mock(UserIdentityContext.class);
        InviteRepository inviteRepository = mock(InviteRepository.class);
        CustomerInviteRepository customerInviteRepository = mock(CustomerInviteRepository.class);

        Credentials credential = credential("18660219918");
        when(userContext.requireUser(credential.getCreId())).thenReturn(credential);
        when(userContext.activeEmployee(credential)).thenReturn(null);

        Invite employeeInvite = employeeInvite();
        CustomerInvite customerInvite = customerInvite();
        when(inviteRepository.findByTelNoOrderByInviteIdDesc("18660219918")).thenReturn(List.of(employeeInvite));
        when(customerInviteRepository.findByTelNoOrderByInviteIdDesc("18660219918")).thenReturn(List.of(customerInvite));

        com.th4.edge.tenant.user.service.invite.invitation.pending.support.Pending provider =
                new com.th4.edge.tenant.user.service.invite.invitation.pending.support.Pending(
                        userContext, inviteRepository, customerInviteRepository);
        ReflectionTestUtils.setField(provider, "principal", credential);

        List<PendingEntryInvite> pending = provider.call(new Credentials());

        assertThat(pending).hasSize(2);
        assertThat(pending.get(0).getType()).isEqualTo(EntryInviteType.Employee);
        assertThat(pending.get(0).getRole()).isEqualTo(EmployeeRole.Finance);
        assertThat(pending.get(0).getMaskedPhone()).isEqualTo("186****9918");
        assertThat(pending.get(0).getCompanyName()).isEqualTo("昭青装饰");
        assertThat(pending.get(1).getType()).isEqualTo(EntryInviteType.Customer);
        assertThat(pending.get(1).getProjectName()).isEqualTo("测试项目");
    }

    @Test
    void shouldIgnoreExpiredInvitesAndActiveEmployee() {
        UserIdentityContext userContext = mock(UserIdentityContext.class);
        InviteRepository inviteRepository = mock(InviteRepository.class);
        CustomerInviteRepository customerInviteRepository = mock(CustomerInviteRepository.class);
        Credentials credential = credential("18660219918");
        when(userContext.requireUser(credential.getCreId())).thenReturn(credential);
        when(userContext.activeEmployee(credential)).thenReturn(new Employee());

        com.th4.edge.tenant.user.service.invite.invitation.pending.support.Pending provider =
                new com.th4.edge.tenant.user.service.invite.invitation.pending.support.Pending(
                        userContext, inviteRepository, customerInviteRepository);
        ReflectionTestUtils.setField(provider, "principal", credential);

        assertThat(provider.call(new Credentials())).isEmpty();
    }

    private Credentials credential(String phone) {
        Credentials credential = new Credentials();
        credential.setCreId(1001L);
        credential.setType(CredType.Wx);
        credential.setIdKey("openid");
        credential.setPhone(phone);
        return credential;
    }

    private Invite employeeInvite() {
        Company company = company();
        Store store = new Store();
        store.setStoreId(20L);
        store.setName("总店");
        Employee inviter = new Employee();
        inviter.setName("张伟");
        Invite invite = new Invite();
        invite.setInviteId(30L);
        invite.setCompany(company);
        invite.setStore(store);
        invite.setInviter(inviter);
        invite.setTelNo("18660219918");
        invite.setRole(EmployeeRole.Finance);
        invite.setStatus(InviteStatus.Pending);
        invite.setExpireTime(LocalDateTime.now().plusDays(1));
        return invite;
    }

    private CustomerInvite customerInvite() {
        Company company = company();
        Employee inviter = new Employee();
        inviter.setName("张伟");
        Property property = new Property();
        property.setAddress("合肥市测试小区 1 栋 101");
        Project project = new Project();
        project.setProjectId(40L);
        project.setName("测试项目");
        project.setProperty(property);
        CustomerInvite invite = new CustomerInvite();
        invite.setInviteId(41L);
        invite.setCompany(company);
        invite.setProject(project);
        invite.setInviter(inviter);
        invite.setCustomerName("张伟");
        invite.setTelNo("18660219918");
        invite.setStatus(InviteStatus.Pending);
        invite.setExpireTime(LocalDateTime.now().plusDays(1));
        return invite;
    }

    private Company company() {
        Company company = new Company();
        company.setComId(10L);
        company.setTitle("昭青装饰");
        company.setName("昭青装饰");
        company.setColor("#A65F46");
        return company;
    }
}

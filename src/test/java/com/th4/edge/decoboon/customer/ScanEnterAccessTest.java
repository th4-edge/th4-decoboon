package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.service.access.scanenter.support.ScanEnter;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 项目码扫码开通业主端（`USER-DECISION-20261015-001`）的门禁用例。
 *
 * <p>这是「业主可见性 + 权限」类变更，必须显式覆盖：手机号一致才开通、号码不符不泄露信息、
 * 员工微信不得降级为业主、客户档案无手机号时不得放开、项目不存在一律 404。</p>
 */
class ScanEnterAccessTest {

    private final CustomerAccessContext accessContext = mock(CustomerAccessContext.class);
    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ScanEnter provider = new ScanEnter(accessContext, projectRepository);

    private Credentials owner(String phone) {
        Credentials credential = new Credentials();
        credential.setCreId(9001L);
        credential.setPhone(phone);
        return credential;
    }

    private Project project(Customer propertyCustomer, Customer projectCustomer, String no) {
        Project project = new Project();
        project.setProjectId(7001L);
        project.setNo(no);
        project.setCustomer(projectCustomer);
        if (propertyCustomer != null) {
            Property property = new Property();
            property.setPropertyId(8001L);
            property.setCustomer(propertyCustomer);
            project.setProperty(property);
        }
        return project;
    }

    private Customer customer(String name, String telNo) {
        Customer customer = new Customer();
        customer.setCustId(6001L);
        customer.setName(name);
        customer.setTelNo(telNo);
        return customer;
    }

    private void prepare(Credentials credential, Project project) {
        ReflectionTestUtils.setField(provider, "scene", project == null ? "P-NOT-EXIST" : project.getNo());
        ReflectionTestUtils.setField(provider, "principal", credential);
        when(accessContext.requireCredential(any())).thenReturn(credential);
        if (project != null) {
            when(projectRepository.findByNo(project.getNo())).thenReturn(Optional.of(project));
        }
    }

    @Test
    void ownerWithMatchingPhoneGetsAccess() {
        Credentials credential = owner("13866729315");
        Customer owner = customer("李思远", "138 6672 9315");
        Project project = project(owner, owner, "P1792019925776");
        prepare(credential, project);

        Credentials result = provider.call(credential);

        assertThat(result).isSameAs(credential);
        verify(accessContext).grantByScan(credential, project, owner);
        verify(accessContext).attachSession(credential);
    }

    @Test
    void propertyCustomerWinsOverProjectCustomer() {
        Credentials credential = owner("13866729315");
        Customer propertyOwner = customer("李思远", "13866729315");
        Customer projectOwner = customer("其他客户", "13900000000");
        Project project = project(propertyOwner, projectOwner, "P1792019925777");
        prepare(credential, project);

        provider.call(credential);

        // 口径：以**房屋所属客户**为准
        verify(accessContext).grantByScan(credential, project, propertyOwner);
    }

    @Test
    void mismatchedPhoneIsRejectedAndNoAccessGranted() {
        Credentials credential = owner("13900000000");
        Customer owner = customer("李思远", "13866729315");
        Project project = project(owner, owner, "P1792019925778");
        prepare(credential, project);

        assertThatThrownBy(() -> provider.call(credential))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("该手机号不是本项目的业主手机号");
        verify(accessContext, never()).grantByScan(any(), any(), any());
    }

    @Test
    void employeeWechatCannotEnterOwnerEnd() {
        Credentials credential = owner("18660219918");
        credential.setEmployee(new Employee());
        Customer owner = customer("李思远", "18660219918");
        Project project = project(owner, owner, "P1792019925779");
        prepare(credential, project);

        assertThatThrownBy(() -> provider.call(credential))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("已绑定装修公司员工身份");
        verify(accessContext, never()).grantByScan(any(), any(), any());
    }

    @Test
    void blankCustomerPhoneCannotOpenAccess() {
        Credentials credential = owner("13866729315");
        Customer owner = customer("李思远", null);
        Project project = project(owner, owner, "P1792019925780");
        prepare(credential, project);

        assertThatThrownBy(() -> provider.call(credential))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("该手机号不是本项目的业主手机号");
        verify(accessContext, never()).grantByScan(any(), any(), any());
    }

    @Test
    void unknownSceneIsNotFound() {
        Credentials credential = owner("13866729315");
        ReflectionTestUtils.setField(provider, "scene", "P-NOT-EXIST");
        ReflectionTestUtils.setField(provider, "principal", credential);
        when(accessContext.requireCredential(any())).thenReturn(credential);
        when(projectRepository.findByNo(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.call(credential))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("项目码无效或项目不存在");
    }
}

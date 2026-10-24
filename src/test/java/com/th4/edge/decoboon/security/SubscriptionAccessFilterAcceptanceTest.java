package com.th4.edge.decoboon.security;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.RequestContext;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.provider.security.SecurityManager;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.billing.configure.PlanModule;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.filter.SubscriptionAccessRequestFilter;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** PP-07 订阅过滤器对写入、查询和续费路由的行为验收。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:subscription-filter-acceptance;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
class SubscriptionAccessFilterAcceptanceTest {

    @Autowired private ApplicationContext applicationContext;

    private SecurityManager securityManager;
    private TenantContext tenantContext;
    private SubscriptionContext subscriptionContext;
    private SubscriptionAccessRequestFilter filter;
    private Employee employee;
    private Company company;
    private RequestContext requestContext;
    private Provider<Object, ?> projectWrite;
    private Provider<Object, ?> projectPage;
    private Provider<Object, ?> orderPrepay;
    private Provider<Object, ?> fileGrant;
    private Provider<Object, ?> publicDownload;

    @BeforeEach
    void setUp() {
        securityManager = mock(SecurityManager.class);
        tenantContext = mock(TenantContext.class);
        subscriptionContext = mock(SubscriptionContext.class);
        requestContext = mock(RequestContext.class);
        company = new Company();
        company.setComId(700001L);
        employee = new Employee();
        employee.setEmpId(700002L);
        employee.setCompany(company);
        employee.setStatus(EmployeeStatus.Active);
        doReturn(employee).when(securityManager).authenticate(requestContext);
        when(tenantContext.requireEmployee(employee)).thenReturn(employee);
        filter = new SubscriptionAccessRequestFilter(securityManager, tenantContext, subscriptionContext);

        projectWrite = provider("project/project/update");
        projectPage = provider("project/project/page");
        orderPrepay = provider("order/prepay");
        fileGrant = provider("file/private/grant");
        publicDownload = provider("file/private/download");
    }

    @Test
    void writeRouteMustPassExpiryGateAndModuleGate() {
        filter.prepare((Provider<Object, ?>) projectWrite, new Object(), requestContext);

        verify(subscriptionContext).requireWritable(company);
        verify(subscriptionContext).requireModule(company, PlanModule.Project);
    }

    @Test
    void queryRouteMustKeepReadAccessButStillCheckModule() {
        filter.prepare((Provider<Object, ?>) projectPage, new Object(), requestContext);

        verify(subscriptionContext, never()).requireWritable(any());
        verify(subscriptionContext).requireModule(company, PlanModule.Project);
    }

    @Test
    void renewalAndFileRoutesMustStillPassLegacyRoleGateButSkipSubscriptionGates() {
        filter.prepare((Provider<Object, ?>) orderPrepay, new Object(), requestContext);
        filter.prepare((Provider<Object, ?>) fileGrant, new Object(), requestContext);

        verify(tenantContext, org.mockito.Mockito.times(2)).requireEmployee(employee);
        verifyNoInteractions(subscriptionContext);
    }

    @Test
    void frozenTeamLeaderMustBeRejectedBeforeExemptRouteReturns() {
        employee.setRole(EmployeeRole.TeamLeader);
        EmployeeRepository employeeRepository = mock(EmployeeRepository.class);
        when(employeeRepository.findByEmpId(employee.getEmpId())).thenReturn(employee);
        filter = new SubscriptionAccessRequestFilter(
                securityManager, new TenantContext(employeeRepository), subscriptionContext);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> filter.prepare((Provider<Object, ?>) orderPrepay, new Object(), requestContext))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> filter.prepare((Provider<Object, ?>) fileGrant, new Object(), requestContext))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> filter.prepare((Provider<Object, ?>) provider("subscription/info"), new Object(), requestContext))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("团队负责人角色已冻结");
        verifyNoInteractions(subscriptionContext);
    }

    @Test
    void supportMustRequireSecurityMarkerOnPublicRoutes() {
        assertThat(filter.support(projectWrite, new Object())).isTrue();
        assertThat(filter.support(publicDownload, new Object())).isFalse();
    }

    @SuppressWarnings("unchecked")
    private Provider<Object, ?> provider(String route) {
        Provider<Object, ?> provider = (Provider<Object, ?>) applicationContext.getBean(route);
        assertThat(provider).isNotNull();
        return provider;
    }
}

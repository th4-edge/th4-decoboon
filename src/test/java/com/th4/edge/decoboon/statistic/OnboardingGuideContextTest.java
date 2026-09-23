package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.statistic.configure.GuideStatus;
import com.th4.edge.decoboon.statistic.configure.GuideStepStatus;
import com.th4.edge.decoboon.statistic.context.OnboardingGuideContext;
import com.th4.edge.decoboon.statistic.model.GuideStep;
import com.th4.edge.decoboon.statistic.model.OnboardingGuide;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OnboardingGuideContextTest {

    @Mock private TenantContext tenantContext;
    @Mock private UserIdentityContext userIdentityContext;
    @Mock private CustomerAccessContext customerAccessContext;
    @Mock private CatalogItemRepository catalogItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private PartnerRepository partnerRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ReceivableRepository receivableRepository;
    @Mock private PayableRepository payableRepository;

    @InjectMocks private OnboardingGuideContext context;

    @Test
    void bossCustomerStepRequiresCustomerWithProperty() {
        Employee boss = employee(EmployeeRole.Boss);
        Customer customer = customer(11L, boss);
        mockBossBase(boss, List.of(catalog(true, "80")), List.of(customer), List.of());

        OnboardingGuide guide = context.guide(boss);

        assertThat(guide.getStatus()).isEqualTo(GuideStatus.ActionRequired);
        assertThat(guide.getRequiredTotal()).isEqualTo(3);
        assertThat(guide.getRequiredCompleted()).isEqualTo(2);
        assertThat(step(guide, "customer").getStatus()).isEqualTo(GuideStepStatus.Todo);
        assertThat(guide.getNextAction()).isEqualTo("boss.customer");
    }

    @Test
    void bossCustomerStepCompletesWhenPropertyExists() {
        Employee boss = employee(EmployeeRole.Boss);
        Customer customer = customer(11L, boss);
        Property property = property(21L, boss, customer);
        mockBossBase(boss, List.of(catalog(true, "80")), List.of(customer), List.of(property));

        OnboardingGuide guide = context.guide(boss);

        assertThat(guide.getStatus()).isEqualTo(GuideStatus.Completed);
        assertThat(guide.getRequiredCompleted()).isEqualTo(3);
        assertThat(step(guide, "customer").getStatus()).isEqualTo(GuideStepStatus.Done);
        assertThat(step(guide, "customer").getCount()).isEqualTo(1);
        assertThat(guide.getNextAction()).isNull();
    }

    @Test
    void requiredSourceFailureProducesUnknownWithoutStaleNextAction() {
        Employee boss = employee(EmployeeRole.Boss);
        mockEmployeeLookup(boss);
        when(catalogItemRepository.findByCompany_ComIdAndTypeAndActiveTrueOrderByCategoryAscBrandAscSeriesAscNameAsc(anyLong(), any()))
                .thenThrow(new RuntimeException("catalog unavailable"));

        OnboardingGuide guide = context.guide(boss);

        assertThat(guide.getStatus()).isEqualTo(GuideStatus.Unknown);
        assertThat(step(guide, "catalog").getStatus()).isEqualTo(GuideStepStatus.Unknown);
        assertThat(step(guide, "cost").getStatus()).isEqualTo(GuideStepStatus.Unknown);
        assertThat(guide.getNextAction()).isNull();
    }

    @Test
    void optionalSourceFailureDoesNotBlockBossCompletion() {
        Employee boss = employee(EmployeeRole.Boss);
        Customer customer = customer(11L, boss);
        Property property = property(21L, boss, customer);
        mockBossBase(boss, List.of(catalog(true, "80")), List.of(customer), List.of(property));
        when(partnerRepository.findByCompany_ComIdAndTypeAndStatusOrderByName(anyLong(), eq(PartnerType.Supplier), eq(PartnerStatus.Active)))
                .thenThrow(new RuntimeException("partner unavailable"));

        OnboardingGuide guide = context.guide(boss);

        assertThat(guide.getStatus()).isEqualTo(GuideStatus.Completed);
        assertThat(step(guide, "partner").getStatus()).isEqualTo(GuideStepStatus.Unknown);
        assertThat(guide.getRequiredCompleted()).isEqualTo(3);
    }

    @Test
    void projectManagerWithoutAssignedProjectCanSeePendingHandoff() {
        Employee manager = employee(EmployeeRole.Pm);
        Project pending = new Project();
        pending.setProjectId(88L);
        when(tenantContext.requireEmployee(manager)).thenReturn(manager);
        when(projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(100L, manager.getEmpId()))
                .thenReturn(List.of());
        when(projectRepository.findByCompany_ComIdOrderByProjectIdDesc(100L)).thenReturn(List.of(pending));

        OnboardingGuide guide = context.guide(manager);

        assertThat(guide.getStatus()).isEqualTo(GuideStatus.ActionRequired);
        assertThat(step(guide, "pm.project").getStatus()).isEqualTo(GuideStepStatus.Todo);
        assertThat(step(guide, "pm.project").getCount()).isEqualTo(1);
        assertThat(guide.getNextAction()).isEqualTo("pm.workbench");
    }

    @Test
    void customerGuideOnlyCountsActiveAccessesReturnedByAccessContext() {
        Credentials credential = new Credentials();
        credential.setCreId(9L);
        Project project = new Project();
        project.setProjectId(66L);
        CustomerAccess access = new CustomerAccess();
        access.setProject(project);
        when(userIdentityContext.requireUser(9L)).thenReturn(credential);
        when(customerAccessContext.activeAccesses(credential)).thenReturn(List.of(access));

        OnboardingGuide guide = context.guide(credential);

        assertThat(guide.getAudience()).isEqualTo("Customer");
        assertThat(guide.getStatus()).isEqualTo(GuideStatus.Completed);
        assertThat(step(guide, "customer.project").getStatus()).isEqualTo(GuideStepStatus.Done);
        assertThat(step(guide, "customer.project").getCount()).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(EmployeeRole.class)
    void everyEmployeeRoleProducesExplicitGuideContract(EmployeeRole role) {
        Employee employee = employee(role);
        when(tenantContext.requireEmployee(employee)).thenReturn(employee);
        when(catalogItemRepository.findByCompany_ComIdAndTypeAndActiveTrueOrderByCategoryAscBrandAscSeriesAscNameAsc(anyLong(), any()))
                .thenReturn(List.of());
        when(customerRepository.findByCompany_ComId(anyLong())).thenReturn(List.of());
        when(customerRepository.findByCompany_ComIdAndOwner_EmpId(anyLong(), anyLong())).thenReturn(List.of());
        when(customerRepository.findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(anyLong(), anyLong())).thenReturn(List.of());
        when(customerRepository.findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(anyLong(), anyLong())).thenReturn(List.of());
        when(propertyRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(anyLong(), anyLong())).thenReturn(List.of());
        when(propertyRepository.findByCompany_ComIdAndOwner_EmpIdOrderByPropertyIdDesc(anyLong(), anyLong())).thenReturn(List.of());
        when(partnerRepository.findByCompany_ComIdAndTypeAndStatusOrderByName(anyLong(), any(), any())).thenReturn(List.of());
        when(employeeRepository.countByCompany_ComIdAndStatus(anyLong(), any())).thenReturn(1L);
        when(projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(anyLong(), anyLong())).thenReturn(List.of());
        when(projectRepository.findByCompany_ComIdAndSupport_EmpIdOrderByProjectIdDesc(anyLong(), anyLong())).thenReturn(List.of());
        when(projectRepository.findByCompany_ComIdOrderByProjectIdDesc(anyLong())).thenReturn(List.of());
        when(receivableRepository.findByCompany_ComIdOrderByDueDateAsc(anyLong())).thenReturn(List.of());
        when(payableRepository.findByCompany_ComIdOrderByDueDateAsc(anyLong())).thenReturn(List.of());

        OnboardingGuide guide = context.guide(employee);

        assertThat(guide.getAudience()).isEqualTo(role.name());
        assertThat(guide.getStatus()).isNotNull();
        assertThat(guide.getSteps()).isNotEmpty();
        assertThat(guide.getSteps()).allMatch(item -> item.getStatus() != null);
    }

    private void mockBossBase(Employee boss, List<CatalogItem> catalog, List<Customer> customers, List<Property> properties) {
        mockEmployeeLookup(boss);
        for (ItemType type : ItemType.values()) {
            when(catalogItemRepository.findByCompany_ComIdAndTypeAndActiveTrueOrderByCategoryAscBrandAscSeriesAscNameAsc(100L, type))
                    .thenReturn(catalog);
        }
        when(customerRepository.findByCompany_ComId(100L)).thenReturn(customers);
        when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(eq(100L), any())).thenReturn(properties);
        when(partnerRepository.findByCompany_ComIdAndTypeAndStatusOrderByName(100L, PartnerType.Supplier, PartnerStatus.Active))
                .thenReturn(List.of());
        when(employeeRepository.countByCompany_ComIdAndStatus(100L, EmployeeStatus.Active)).thenReturn(1L);
    }

    private void mockEmployeeLookup(Employee employee) {
        when(tenantContext.requireEmployee(employee)).thenReturn(employee);
    }

    private Employee employee(EmployeeRole role) {
        Company company = new Company();
        company.setComId(100L);
        Employee employee = new Employee();
        employee.setEmpId(role == EmployeeRole.Boss ? 1L : 2L);
        employee.setCompany(company);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        return employee;
    }

    private Customer customer(Long id, Employee owner) {
        Customer customer = new Customer();
        customer.setCustId(id);
        customer.setCompany(owner.getCompany());
        customer.setOwner(owner);
        return customer;
    }

    private Property property(Long id, Employee owner, Customer customer) {
        Property property = new Property();
        property.setPropertyId(id);
        property.setCompany(owner.getCompany());
        property.setOwner(owner);
        property.setCustomer(customer);
        return property;
    }

    private CatalogItem catalog(boolean active, String cost) {
        CatalogItem item = new CatalogItem();
        item.setItemId(1L);
        item.setActive(active);
        item.setCostPrice(new BigDecimal(cost));
        return item;
    }

    private GuideStep step(OnboardingGuide guide, String key) {
        return guide.getSteps().stream().filter(item -> key.equals(item.getKey())).findFirst().orElseThrow();
    }
}

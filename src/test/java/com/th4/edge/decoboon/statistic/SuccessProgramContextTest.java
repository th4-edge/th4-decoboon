package com.th4.edge.decoboon.statistic;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.repository.MaterialSelectionRepository;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.configure.ActivationStatus;
import com.th4.edge.decoboon.statistic.configure.ChurnRiskLevel;
import com.th4.edge.decoboon.statistic.configure.MilestoneStatus;
import com.th4.edge.decoboon.statistic.context.SuccessProgramContext;
import com.th4.edge.decoboon.statistic.model.SuccessProgramReport;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SuccessProgramContextTest {

    @Mock private TenantContext tenantContext;
    @Mock private CatalogItemRepository catalogItemRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private ProjectContractRepository contractRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private MaterialSelectionRepository selectionRepository;
    @Mock private ReceivableRepository receivableRepository;
    @Mock private PayableRepository payableRepository;
    @Mock private AcceptanceRepository acceptanceRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks private SuccessProgramContext context;

    @Test
    void day14WithoutRealBusinessFactsIsHighRiskAndNotActivated() {
        Employee boss = employee(EmployeeRole.Boss, company());
        mockLookup(boss);
        mockCompanyResources(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        when(employeeRepository.findByCompany_ComId(100L)).thenReturn(List.of(boss));

        SuccessProgramReport report = context.report(boss);

        assertThat(report.getActivationStatus()).isEqualTo(ActivationStatus.Risk);
        assertThat(report.getRiskLevel()).isEqualTo(ChurnRiskLevel.High);
        assertThat(report.getNextAction()).isEqualTo("catalog");
        assertThat(report.getMilestones()).anyMatch(item -> item.getKey().equals("project")
                && item.getStatus() == MilestoneStatus.Overdue);
        assertThat(report.getRiskReasons()).contains("第 14 天仍没有报价", "第 14 天仍未创建项目");
    }

    @Test
    void missingCompanyCreateTimeFallsBackToTodayWithoutCrashing() {
        Company company = companyWithMissingCreateTime();
        Employee boss = employee(EmployeeRole.Boss, company);
        mockLookup(boss);
        mockCompanyResources(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        when(employeeRepository.findByCompany_ComId(100L)).thenReturn(List.of(boss));

        SuccessProgramReport report = context.report(boss);

        assertThat(report.getStartDate()).isEqualTo(LocalDate.now());
        assertThat(report.getDaysSinceCreated()).isZero();
    }

    @Test
    void completeCoreBusinessFactsProduceActivatedLowRisk() {
        Company company = company();
        Employee boss = employee(EmployeeRole.Boss, company);
        Employee marketer = employee(EmployeeRole.Marketer, company);
        Employee designer = employee(EmployeeRole.Designer, company);
        Employee pm = employee(EmployeeRole.Pm, company);
        Employee finance = employee(EmployeeRole.Finance, company);
        Employee storeManager = employee(EmployeeRole.StoreManager, company);
        mockLookup(boss);

        Customer customer = new Customer();
        customer.setCustId(11L);
        customer.setCompany(company);
        customer.setOwner(marketer);
        Property property = new Property();
        property.setPropertyId(21L);
        property.setCompany(company);
        property.setCustomer(customer);
        property.setDesigner(designer);
        Quote quote = new Quote();
        quote.setQuoteId(31L);
        ProjectContract contract = new ProjectContract();
        contract.setContractId(41L);
        contract.setStatus(ContractStatus.Effective);
        Project project = new Project();
        project.setProjectId(51L);
        project.setManager(pm);
        MaterialSelection selection = new MaterialSelection();
        selection.setSelectionId(61L);
        CatalogItem catalog = new CatalogItem();
        catalog.setItemId(71L);
        catalog.setActive(true);
        catalog.setCostPrice(new BigDecimal("100"));

        mockCompanyResources(List.of(catalog), List.of(customer), List.of(property), List.of(quote),
                List.of(contract), List.of(project), List.of(selection), List.of(mock(Payable.class)), List.of(mock(Acceptance.class)));
        when(employeeRepository.findByCompany_ComId(100L))
                .thenReturn(List.of(boss, marketer, designer, pm, finance, storeManager));

        SuccessProgramReport report = context.report(boss);

        assertThat(report.getActivationScore()).isEqualTo(100);
        assertThat(report.getActivationStatus()).isEqualTo(ActivationStatus.Activated);
        assertThat(report.getRiskLevel()).isEqualTo(ChurnRiskLevel.Low);
        assertThat(report.getRoleActivationRate()).isEqualTo(100);
        assertThat(report.getMilestones()).allMatch(item -> item.getStatus() == MilestoneStatus.Done);
    }

    private void mockLookup(Employee boss) {
        when(tenantContext.requireRole(boss, EmployeeRole.Boss)).thenReturn(boss);
    }

    private void mockCompanyResources(List<CatalogItem> catalog,
                                      List<Customer> customers,
                                      List<Property> properties,
                                      List<Quote> quotes,
                                      List<ProjectContract> contracts,
                                      List<Project> projects,
                                      List<MaterialSelection> selections,
                                      List<Payable> payables,
                                      List<Acceptance> acceptances) {
        for (ItemType type : ItemType.values()) {
            when(catalogItemRepository.findByCompany_ComIdAndTypeAndActiveTrueOrderByCategoryAscBrandAscSeriesAscNameAsc(
                    100L, type)).thenReturn(catalog);
        }
        when(customerRepository.findByCompany_ComId(100L)).thenReturn(customers);
        if (!customers.isEmpty()) {
            when(propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(anyLong(), any())).thenReturn(properties);
            when(selectionRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(anyLong(), any()))
                    .thenReturn(selections);
        }
        when(quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(100L)).thenReturn(quotes);
        when(contractRepository.findByCompany_ComIdOrderByContractIdDesc(100L)).thenReturn(contracts);
        when(projectRepository.findByCompany_ComIdOrderByProjectIdDesc(100L)).thenReturn(projects);
        when(receivableRepository.findByCompany_ComIdOrderByDueDateAsc(100L)).thenReturn(List.of());
        when(payableRepository.findByCompany_ComIdOrderByDueDateAsc(100L)).thenReturn(payables);
        when(acceptanceRepository.findByCompany_ComId(100L)).thenReturn(acceptances);
    }

    private Employee employee(EmployeeRole role, Company company) {
        Employee employee = new Employee();
        employee.setEmpId((long) role.ordinal() + 1);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setCompany(company);
        return employee;
    }

    private Company companyWithMissingCreateTime() {
        Company company = mock(Company.class);
        when(company.getComId()).thenReturn(100L);
        when(company.getTitle()).thenReturn("测试装企");
        DateTime createTime = mock(DateTime.class);
        when(createTime.toLocalDate()).thenReturn(null);
        when(company.getCreateTime()).thenReturn(createTime);
        return company;
    }

    private Company company() {
        Company company = mock(Company.class);
        when(company.getComId()).thenReturn(100L);
        when(company.getTitle()).thenReturn("测试装企");
        DateTime createTime = mock(DateTime.class);
        when(createTime.toLocalDate()).thenReturn(LocalDate.now().minusDays(14));
        when(company.getCreateTime()).thenReturn(createTime);
        return company;
    }
}

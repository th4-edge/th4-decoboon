package com.th4.edge.decoboon.project;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.configure.PricingMethod;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.ContractChangeStatus;
import com.th4.edge.decoboon.project.configure.ContractChangeType;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.ContractChange;
import com.th4.edge.decoboon.project.model.ContractChangeItem;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractChangeRepository;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 项目工地增减项窗口回归测试：未完工前允许发起增减项，完工/结算及合同关闭/终止后阻断。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contract-change-gate;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
@Transactional
class ContractChangeCompletionGateTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectContractRepository contractRepository;
    @Autowired private ContractChangeRepository changeRepository;
    @Autowired private com.th4.edge.decoboon.project.service.contractchange.save.Save contractChangeSave;
    @Autowired private com.th4.edge.decoboon.project.service.contractchange.batchsave.BatchSave batchSave;
    @Autowired private ContractLineRepository contractLineRepository;
    @Autowired private QuoteLineRepository quoteLineRepository;

    @BeforeEach
    void unwrapProviderProxy() {
        contractChangeSave = AopTestUtils.getTargetObject(contractChangeSave);
        batchSave = AopTestUtils.getTargetObject(batchSave);
    }

    @Test
    void shouldAllowChangeWhileProjectOngoing() {
        Fixture fixture = fixture("ongoing", ProjectStatus.Ongoing, ContractStatus.Effective);

        ContractChange change = request(fixture, new BigDecimal("1200.00"));

        assertThat(change.getChangeId()).isNotNull();
        assertThat(change.getType()).isEqualTo(ContractChangeType.Add);
        assertThat(change.getAmount()).isEqualByComparingTo("1200.00");
        assertThat(change.getProject().getProjectId()).isEqualTo(fixture.project().getProjectId());
        assertThat(changeRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldAllowChangeWhileProjectAccepting() {
        Fixture fixture = fixture("accepting", ProjectStatus.Accepting, ContractStatus.Changed);

        assertThat(request(fixture, new BigDecimal("800.00")).getChangeId()).isNotNull();
    }

    @Test
    void shouldRejectChangeAfterProjectFinished() {
        Fixture fixture = fixture("finished", ProjectStatus.Finished, ContractStatus.Completed);

        assertDefined(fixture, 400, "项目已完工，不能继续发起增减项");
        assertThat(changeRepository.count()).isZero();
        assertThat(fixture.contract().getCurrentAmount()).isEqualByComparingTo("100000.00");
    }

    @Test
    void shouldRejectChangeAfterProjectSettled() {
        Fixture fixture = fixture("settled", ProjectStatus.Settled, ContractStatus.Settled);

        assertDefined(fixture, 400, "项目已完工，不能继续发起增减项");
        assertThat(changeRepository.count()).isZero();
    }

    @Test
    void shouldRejectChangeWhenContractClosedOrTerminated() {
        Fixture closed = fixture("closed", ProjectStatus.Ongoing, ContractStatus.Closed);
        assertDefined(closed, 400, "合同已终止或关闭，不能继续发起增减项");

        Fixture terminated = fixture("terminated", ProjectStatus.Ongoing, ContractStatus.Terminated);
        assertDefined(terminated, 400, "合同已终止或关闭，不能继续发起增减项");
    }

    @Test
    void batchSaveMustRecomputeLineAmountOnServer() {
        Fixture fixture = fixture("batch-recompute", ProjectStatus.Ongoing, ContractStatus.Effective);
        QuoteLine quoteLine = new QuoteLine();
        quoteLine.setQuote(fixture.quote());
        quoteLine.setCompany(fixture.company());
        quoteLine.setProperty(fixture.property());
        quoteLine.setType(ItemType.Construction);
        quoteLine.setCategory("墙面施工");
        quoteLine.setName("墙面施工");
        quoteLine.setUnit(Unit.Area);
        quoteLine.setQuantity(new BigDecimal("10"));
        quoteLine.setMaterialPrice(new BigDecimal("100"));
        quoteLine.setManualPrice(BigDecimal.ZERO);
        quoteLine.setPricingMode(PricingMethod.MaterialAndManual);
        quoteLine.setAmount(new BigDecimal("1000"));
        quoteLine.setCost(new BigDecimal("600"));
        quoteLine = quoteLineRepository.save(quoteLine);

        ContractLine line = new ContractLine();
        line.setCompany(fixture.company());
        line.setProperty(fixture.property());
        line.setContract(fixture.contract());
        line.setSourceQuoteLine(quoteLine);
        line.setType(ItemType.Construction);
        line.setCategory("墙面施工");
        line.setName("墙面施工");
        line.setUnit(Unit.Area);
        line.setQuantity(new BigDecimal("10"));
        line.setMaterialPrice(new BigDecimal("100"));
        line.setManualPrice(BigDecimal.ZERO);
        line.setAmount(new BigDecimal("1000"));
        line.setCost(new BigDecimal("600"));
        line.setSort(0);
        line = contractLineRepository.save(line);

        ContractChangeItem item = new ContractChangeItem();
        item.setName("墙面施工调量");
        item.setChangeType(ContractChangeType.Quantity);
        item.setSourceLineId(line.getLineId());
        item.setNewQuantity(new BigDecimal("12"));
        assertThat(item.getNewQuantity()).isEqualByComparingTo("12");

        ReflectionTestUtils.setField(batchSave, "employee", fixture.pm());
        ReflectionTestUtils.setField(batchSave, "contractId", fixture.contract().getContractId());
        ReflectionTestUtils.setField(batchSave, "reason", "批量增减项（服务端重算回归）");
        ReflectionTestUtils.setField(batchSave, "type", ContractChangeType.Quantity);
        ReflectionTestUtils.setField(batchSave, "items", List.of(item));
        // 兼容回归：老版本客户端仍提交客户确认信息，服务端忽略、不校验、不落库（DEC-068）。
        ReflectionTestUtils.setField(batchSave, "evidenceImages", List.of("evidence.jpg"));
        ReflectionTestUtils.setField(batchSave, "signatureUrl", null);
        ReflectionTestUtils.setField(batchSave, "customerConfirmedAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(batchSave, "customerConfirmedBy", "业主李女士");
        ReflectionTestUtils.setField(batchSave, "customerConfirmMethod", "当面确认");

        ContractChange change = batchSave.call(new ContractChange());

        // 服务端按原合同行重算：(12-10)*100=200；(12-10)*60=120，不信任前端金额。
        assertThat(change.getAmount()).isEqualByComparingTo("200");
        assertThat(change.getCostDelta()).isEqualByComparingTo("120");
        assertThat(change.getStatus()).isEqualTo(ContractChangeStatus.Pending);
        // DEC-068：建单不再标记客户确认，仅留痕发起人与明细；审批通过即生效。
        assertThat(change.getCustomerConfirmed()).isNotEqualTo(Boolean.TRUE);
        assertThat(change.getItems()).hasSize(1);
        assertThat(change.getItems().get(0).getAmountDelta()).isEqualByComparingTo("200");
        assertThat(change.getItems().get(0).getSourceLine().getLineId()).isEqualTo(line.getLineId());
    }

    private ContractChange request(Fixture fixture, BigDecimal amount) {
        ReflectionTestUtils.setField(contractChangeSave, "employee", fixture.pm());
        ReflectionTestUtils.setField(contractChangeSave, "contractId", fixture.contract().getContractId());
        ReflectionTestUtils.setField(contractChangeSave, "reason", "工地增减项（窗口回归测试）");
        ReflectionTestUtils.setField(contractChangeSave, "type", ContractChangeType.Add);
        ReflectionTestUtils.setField(contractChangeSave, "amount", amount);
        ReflectionTestUtils.setField(contractChangeSave, "contractLineId", null);
        ReflectionTestUtils.setField(contractChangeSave, "newQuantity", null);
        ReflectionTestUtils.setField(contractChangeSave, "newPrice", null);
        ReflectionTestUtils.setField(contractChangeSave, "newCost", null);
        ReflectionTestUtils.setField(contractChangeSave, "costDelta", null);
        // 兼容回归：老版本客户端仍提交客户确认信息，服务端忽略、不校验、不落库（DEC-068）。
        ReflectionTestUtils.setField(contractChangeSave, "evidenceImages", List.of("evidence.jpg"));
        ReflectionTestUtils.setField(contractChangeSave, "signatureUrl", null);
        ReflectionTestUtils.setField(contractChangeSave, "customerConfirmedAt", LocalDateTime.now().minusDays(1));
        ReflectionTestUtils.setField(contractChangeSave, "customerConfirmedBy", "业主李女士");
        ReflectionTestUtils.setField(contractChangeSave, "customerConfirmMethod", "当面确认");
        ReflectionTestUtils.setField(contractChangeSave, "reportId", null);
        return contractChangeSave.call(new ContractChange());
    }

    private void assertDefined(Fixture fixture, int code, String messagePart) {
        assertThatThrownBy(() -> request(fixture, new BigDecimal("1000.00")))
                .isInstanceOfSatisfying(DefinedException.class, error -> {
                    assertThat(error.getErrorCode().getCode()).isEqualTo(String.valueOf(code));
                    assertThat(error.getMessage()).contains(messagePart);
                });
    }

    private record Fixture(Company company, Store store, Employee pm, Customer customer,
                           Property property, Quote quote, Project project, ProjectContract contract) {
    }

    private Fixture fixture(String prefix, ProjectStatus projectStatus, ContractStatus contractStatus) {
        return build(prefix, projectStatus, contractStatus);
    }

    private Fixture build(String prefix, ProjectStatus projectStatus, ContractStatus contractStatus) {
        String key = Long.toHexString(System.nanoTime());
        Company company = new Company();
        company.setTitle("企" + key);
        company.setName("C" + key);
        company.setSize(Size.A);
        company = companyRepository.save(company);

        Store store = new Store();
        store.setCompany(company);
        store.setName("S" + key);
        store.setDefaultStore(true);
        store = storeRepository.save(store);

        Employee pm = new Employee();
        pm.setCompany(company);
        pm.setStore(store);
        pm.setRole(EmployeeRole.Pm);
        pm.setName("E" + key);
        pm.setTelNo("1" + Math.abs(System.nanoTime() % 1_000_000_000L));
        pm = employeeRepository.save(pm);

        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(pm);
        customer.setName("客" + key);
        customer.setTelNo("2" + Math.abs(System.nanoTime() % 1_000_000_000L));
        customer.setCommunity("小" + key);
        customer.setAddr("地" + key);
        customer.setArea(new BigDecimal("100"));
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(pm);
        property.setCustomer(customer);
        property.setPropertyNo("P-" + key);
        property.setCommunity(customer.getCommunity());
        property.setBuilding("1");
        property.setRoomNo("101");
        property.setAddress(customer.getAddr());
        property.setArea(new BigDecimal("100"));
        property.setLayout("3室2厅");
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("Q-" + key);
        quote.setTitle("报" + key);
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(pm);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(property.getArea());
        quote.setSubtotal(new BigDecimal("100000"));
        quote.setDiscount(BigDecimal.ONE);
        quote.setTaxAmount(BigDecimal.ZERO);
        quote.setTaxRate(new BigDecimal("0.05"));
        quote.setAmount(new BigDecimal("100000"));
        quote.setCost(new BigDecimal("80000"));
        quote.setDesignFee(BigDecimal.ZERO);
        quote.setManagementFee(BigDecimal.ZERO);
        quote.setStatus(QuoteStatus.Accepted);
        quote = quoteRepository.save(quote);

        Project project = new Project();
        project.setNo("PJ-" + key);
        project.setName("项" + key);
        project.setCompany(company);
        project.setStore(store);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setOwner(customer.getOwner());
        project.setManager(pm);
        project.setSiteAddr(property.getAddress());
        project.setArea(property.getArea());
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(projectStatus);
        project.setProgress(30);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(60));
        project = projectRepository.save(project);

        ProjectContract contract = new ProjectContract();
        contract.setNo("C" + key);
        contract.setCompany(company);
        contract.setQuote(quote);
        contract.setCustomer(customer);
        contract.setProperty(property);
        contract.setProject(project);
        contract.setBaselineAmount(new BigDecimal("100000"));
        contract.setSignedAmount(new BigDecimal("100000"));
        contract.setCurrentAmount(new BigDecimal("100000"));
        contract.setStatus(contractStatus);
        contract.setCreatedBy(pm);
        contract.setSignedAt(LocalDate.now());
        contract = contractRepository.save(contract);
        return new Fixture(company, store, pm, customer, property, quote, project, contract);
    }
}

package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receipt;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.document.context.VoucherContext;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.labor.configure.WorkerStatus;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.labor.repository.WorkerRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import jakarta.persistence.EntityManager;
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

/** 财务应收收款、应付付款与流水归集闭环测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:finance-closed-loop;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "spring.main.banner-mode=off",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/finance-closed-loop-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
@Transactional
class FinanceClosedLoopTest {

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private QuoteRepository quoteRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private LedgerRepository ledgerRepository;
    @Autowired
    private WorkerRepository workerRepository;
    @Autowired
    private VoucherContext voucherContext;
    @Autowired
    private com.th4.edge.decoboon.document.service.frequentproject.list.List frequentProjects;
    @Autowired
    private com.th4.edge.decoboon.project.service.projects.page.Page projectPage;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private com.th4.edge.decoboon.finance.service.receivables.save.Save receivableSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.receipts.save.Save receiptSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.payables.save.Save payableSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.payments.save.Save paymentSave;

    @Test
    void shouldCloseReceivablePayableAndLedgerInOneFlow() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();

        Receivable receivable = createReceivable(boss, project);
        assertThat(receivable.getStatus()).isEqualTo(ReceivableStatus.Pending);

        Receipt receipt = createReceipt(boss, project, receivable);
        assertThat(receipt.getAmount()).isEqualByComparingTo("3000.00");
        Receivable reloaded = receivable.getReceivableId() == null ? receivable : receivable;
        assertThat(reloaded.getReceivedAmount()).isEqualByComparingTo("3000.00");
        assertThat(reloaded.getStatus()).isEqualTo(ReceivableStatus.Received);

        Payable payable = createPayable(boss, project);
        createPayment(boss, payable);
        assertThat(payable.getPaidAmount()).isEqualByComparingTo("800.00");
        assertThat(payable.getStatus()).isEqualTo(com.th4.edge.decoboon.finance.configure.PayableStatus.Paid);

        entityManager.flush();
        List<Ledger> ledgers = ledgerRepository.findAll().stream()
                .filter(item -> item.getProject() != null
                        && item.getProject().getProjectId().equals(project.getProjectId()))
                .toList();
        assertThat(ledgers).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldLinkWorkerAndUseWorkerNameOnSalaryPayment() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();

        Worker worker = new Worker();
        worker.setCompany(boss.getCompany());
        worker.setName("张伟");
        worker.setIdCardNo("341222198803305970");
        worker.setTrade("木工");
        worker.setBankAccount("662245678901266");
        worker.setStatus(WorkerStatus.Active);
        worker = workerRepository.save(worker);

        Payable payable = createSalaryPayable(boss, project);
        com.th4.edge.decoboon.finance.service.payments.save.support.Save save =
                AopTestUtils.getTargetObject(paymentSave);
        set(save, "employee", boss);
        set(save, "payableId", payable.getPayableId());
        set(save, "amount", new BigDecimal("40.00"));
        set(save, "account", FundAccount.CompanyBank);
        set(save, "paidAt", LocalDate.now());
        set(save, "payeeName", null);
        set(save, "workerId", worker.getWorkerId());
        set(save, "transactionNo", null);
        set(save, "voucherUrl", null);
        set(save, "remark", "工资发放");
        com.th4.edge.decoboon.finance.model.Payment payment =
                save.call(new com.th4.edge.decoboon.finance.model.Payment());

        assertThat(payment.getWorker()).isNotNull();
        assertThat(payment.getWorker().getWorkerId()).isEqualTo(worker.getWorkerId());
        assertThat(payment.getPayeeName()).isEqualTo("张伟");
    }

    @Test
    void shouldRankFrequentProjectAndSupportServerSideProjectSearch() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();
        Receivable receivable = createReceivable(boss, project);
        createReceipt(boss, project, receivable);
        entityManager.flush();

        List<Ledger> ledgers = ledgerRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(
                        boss.getCompany().getComId(), project.getProjectId());
        assertThat(ledgers).isNotEmpty();
        voucherContext.build(boss, List.of(ledgers.get(0).getLedgerId()));

        // 常用项目：按凭证出单频次返回该项目
        com.th4.edge.decoboon.document.service.frequentproject.list.support.List frequent =
                AopTestUtils.getTargetObject(frequentProjects);
        set(frequent, "employee", boss);
        set(frequent, "size", 5);
        assertThat(frequent.call(new Project()))
                .extracting(Project::getProjectId)
                .contains(project.getProjectId());

        // 服务端检索：关键词 + 状态集合 + 设计师
        com.th4.edge.decoboon.project.service.projects.page.support.Page page =
                AopTestUtils.getTargetObject(projectPage);
        set(page, "employee", boss);
        set(page, "keyword", project.getNo());
        set(page, "statuses", List.of(project.getStatus()));
        set(page, "designerId", boss.getEmpId());
        assertThat(page.call(new Project()).getContent())
                .extracting(Project::getProjectId)
                .contains(project.getProjectId());

        set(page, "designerId", -1L);
        assertThat(page.call(new Project()).getContent()).isEmpty();
    }

    @Test
    void shouldRejectReceiptOverReceivableBalance() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();
        Receivable receivable = createReceivable(boss, project);

        assertThatThrownBy(() -> {
            com.th4.edge.decoboon.finance.service.receipts.save.support.Save save = asReceiptSave();
            set(save, "employee", boss);
            set(save, "projectId", project.getProjectId());
            set(save, "receivableId", receivable.getReceivableId());
            set(save, "amount", new BigDecimal("3001.00"));
            set(save, "account", FundAccount.CompanyBank);
            set(save, "receivedAt", LocalDate.now());
            save.call(new Receipt());
        }).hasMessageContaining("不能超过应收余额");
    }

    private Receivable createReceivable(Employee boss, Project project) {
        com.th4.edge.decoboon.finance.service.receivables.save.support.Save save =
                AopTestUtils.getTargetObject(receivableSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "name", "自动化测试首期款");
        set(save, "stageName", "签订合同");
        set(save, "type", ReceivableType.First);
        set(save, "amount", new BigDecimal("3000.00"));
        set(save, "dueDate", LocalDate.now());
        set(save, "invoiceRequired", false);
        set(save, "invoiceNo", null);
        set(save, "remark", "transaction rollback");
        return save.call(new Receivable());
    }

    private Receipt createReceipt(Employee boss, Project project, Receivable receivable) {
        com.th4.edge.decoboon.finance.service.receipts.save.support.Save save = asReceiptSave();
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "receivableId", receivable.getReceivableId());
        set(save, "amount", new BigDecimal("3000.00"));
        set(save, "account", FundAccount.CompanyBank);
        set(save, "receivedAt", LocalDate.now());
        set(save, "payerName", project.getCustomer().getName());
        set(save, "voucherUrl", null);
        set(save, "remark", "transaction rollback");
        return save.call(new Receipt());
    }

    private Payable createPayable(Employee boss, Project project) {
        com.th4.edge.decoboon.finance.service.payables.save.support.Save save =
                AopTestUtils.getTargetObject(payableSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "materialPlanId", null);
        set(save, "name", "自动化测试材料款");
        set(save, "type", PayableType.Material);
        set(save, "payeeEmployeeId", null);
        set(save, "payeeName", "自动化测试供应商");
        set(save, "period", null);
        set(save, "amount", new BigDecimal("800.00"));
        set(save, "dueDate", LocalDate.now());
        set(save, "urgent", false);
        set(save, "sourceNo", null);
        set(save, "remark", "transaction rollback");
        return save.call(new Payable());
    }

    private Payable createSalaryPayable(Employee boss, Project project) {
        com.th4.edge.decoboon.finance.service.payables.save.support.Save save =
                AopTestUtils.getTargetObject(payableSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "materialPlanId", null);
        set(save, "name", "木工张伟工资");
        set(save, "type", PayableType.Salary);
        set(save, "payeeEmployeeId", null);
        set(save, "payeeName", "木工班组");
        set(save, "period", null);
        set(save, "amount", new BigDecimal("40.00"));
        set(save, "dueDate", LocalDate.now());
        set(save, "urgent", false);
        set(save, "sourceNo", null);
        set(save, "remark", "工资待发");
        return save.call(new Payable());
    }

    private void createPayment(Employee boss, Payable payable) {
        com.th4.edge.decoboon.finance.service.payments.save.support.Save save =
                AopTestUtils.getTargetObject(paymentSave);
        set(save, "employee", boss);
        set(save, "payableId", payable.getPayableId());
        set(save, "amount", new BigDecimal("800.00"));
        set(save, "account", FundAccount.CompanyBank);
        set(save, "paidAt", LocalDate.now());
        set(save, "payeeName", payable.getPayeeName());
        set(save, "transactionNo", null);
        set(save, "voucherUrl", null);
        set(save, "remark", "transaction rollback");
        save.call(new com.th4.edge.decoboon.finance.model.Payment());
    }

    private Fixture createFixture() {
        long seq = System.nanoTime();
        Company company = new Company();
        company.setTitle("财务闭环测试企业" + seq);
        company.setName("财务闭环企业");
        company.setSize(Size.A);
        company = companyRepository.save(company);

        Store store = new Store();
        store.setCompany(company);
        store.setName("财务测试门店");
        store.setDefaultStore(true);
        store = storeRepository.save(store);

        Employee boss = new Employee();
        boss.setCompany(company);
        boss.setStore(store);
        boss.setRole(EmployeeRole.Boss);
        boss.setStatus(EmployeeStatus.Active);
        boss.setName("财务测试负责人");
        boss.setTelNo("18" + String.format("%09d", Math.abs(seq % 1_000_000_000L)));
        boss.setJoinedAt(LocalDateTime.now());
        boss = employeeRepository.save(boss);

        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(boss);
        customer.setName("财务测试客户");
        customer.setTelNo("19" + String.format("%09d", Math.abs(seq % 1_000_000_000L)));
        customer.setArea(new BigDecimal("120"));
        customer.setAddr("财务测试地址");
        customer = customerRepository.save(customer);

        Property property = new Property();
        property.setCompany(company);
        property.setStore(store);
        property.setOwner(boss);
        property.setCustomer(customer);
        property.setPropertyNo("PROP-" + seq);
        property.setAddress("财务测试小区 1 栋 101");
        property.setArea(new BigDecimal("120"));
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = new Quote();
        quote.setNo("QUOTE-" + seq);
        quote.setTitle("财务闭环测试报价");
        quote.setCompany(company);
        quote.setStore(store);
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(boss);
        quote.setVersion(1);
        quote.setMode(QuoteMode.Custom);
        quote.setArea(new BigDecimal("120"));
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
        project.setNo("PROJECT-" + seq);
        project.setName("财务闭环测试项目");
        project.setCompany(company);
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setDesigner(boss);
        project.setManager(boss);
        project.setSupport(boss);
        project.setSiteAddr(property.getAddress());
        project.setArea(new BigDecimal("120"));
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(10);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(90));
        project = projectRepository.save(project);
        entityManager.flush();
        return new Fixture(boss, project);
    }

    private com.th4.edge.decoboon.finance.service.receipts.save.support.Save asReceiptSave() {
        return AopTestUtils.getTargetObject(receiptSave);
    }

    private void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }
    private record Fixture(Employee boss, Project project) {
    }

}

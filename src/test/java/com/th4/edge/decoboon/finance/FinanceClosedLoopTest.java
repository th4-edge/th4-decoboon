package com.th4.edge.decoboon.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.th4.edge.decoboon.common.configure.BusinessOrigin;
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
import org.springframework.core.annotation.AnnotationUtils;
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
    private com.th4.edge.decoboon.finance.repository.PayableRepository payableRepository;
    @Autowired
    private com.th4.edge.decoboon.finance.context.FinanceAccessContext financeAccessContext;
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
    private ObjectMapper objectMapper;
    @Autowired
    private com.th4.edge.decoboon.finance.service.receivables.save.Save receivableSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.receipts.save.Save receiptSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.payables.save.Save payableSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.payments.save.Save paymentSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgers.save.Save ledgerSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgers.update.Update ledgerUpdate;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgers.correct.Correct ledgerCorrect;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgers.delete.Delete ledgerDelete;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgercategories.tree.Tree ledgerCategoryTree;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgercategories.save.Save ledgerCategorySave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgercategories.update.Update ledgerCategoryUpdate;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgercategories.delete.Delete ledgerCategoryDelete;
    @Autowired
    private com.th4.edge.decoboon.finance.repository.LedgerRevisionRepository ledgerRevisionRepository;
    @Autowired
    private com.th4.edge.decoboon.finance.service.summary.report.Report summaryReport;
    @Autowired
    private com.th4.edge.decoboon.finance.service.openingbalances.save.Save openingBalanceSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.openingbalances.page.Page openingBalancePage;
    @Autowired
    private com.th4.edge.decoboon.finance.service.ledgers.report.Report ledgerReport;
    @Autowired
    private com.th4.edge.decoboon.finance.service.projects.report.Report financeProjectReport;
    @Autowired
    private com.th4.edge.decoboon.finance.service.projects.page.Page financeProjectPage;
    @Autowired
    private com.th4.edge.decoboon.finance.service.settlements.save.Save settlementSave;
    @Autowired
    private com.th4.edge.decoboon.finance.service.payables.report.Report payableReport;
    @Autowired
    private com.th4.edge.decoboon.document.service.voucher.report.Report voucherReport;
    @Autowired
    private com.th4.edge.decoboon.statistic.service.pm.report.Report pmReport;

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
    void shouldAllowAllManualReceivableTypes() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();
        com.th4.edge.decoboon.finance.service.receivables.save.support.Save save =
                AopTestUtils.getTargetObject(receivableSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "name", "手工应收类型门禁");
        set(save, "stageName", "补充款");
        set(save, "amount", new BigDecimal("10.00"));
        set(save, "dueDate", LocalDate.now());

        for (ReceivableType type : ReceivableType.values()) {
            set(save, "type", type);
            Receivable created = save.call(new Receivable());
            assertThat(created.getType()).isEqualTo(type);
        }
    }

    @Test
    void shouldAllowAllManualPayableTypes() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();
        com.th4.edge.decoboon.finance.service.payables.save.support.Save save =
                AopTestUtils.getTargetObject(payableSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "name", "手工应付类型门禁");
        set(save, "payeeName", "测试供应商");
        set(save, "amount", new BigDecimal("10.00"));
        set(save, "dueDate", LocalDate.now());

        for (PayableType type : PayableType.values()) {
            set(save, "type", type);
            Payable created = save.call(new Payable());
            assertThat(created.getType()).isEqualTo(type);
        }
    }

    @Test
    void shouldReuseExistingAutomaticLedgerForSameSource() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();
        Receivable receivable = createReceivable(boss, project);
        Receipt receipt = createReceipt(boss, project, receivable);
        Ledger original = ledgerRepository
                .findFirstByCompany_ComIdAndSourceAndSourceIdOrderByLedgerIdAsc(
                        boss.getCompany().getComId(),
                        com.th4.edge.decoboon.finance.configure.LedgerSource.Receipt,
                        receipt.getReceiptId())
                .orElseThrow();

        Ledger repeated = financeAccessContext.recordLedger(
                boss, project,
                com.th4.edge.decoboon.finance.configure.LedgerDirection.Income,
                receivable.getType().getName(),
                com.th4.edge.decoboon.finance.configure.LedgerSource.Receipt,
                receipt.getReceiptId(), receipt.getAmount(), receipt.getAccount(), receipt.getReceivedAt(),
                receipt.getPayerName(), "重复来源", "幂等核对");

        assertThat(repeated.getLedgerId()).isEqualTo(original.getLedgerId());
        assertThat(ledgerRepository.findAll().stream()
                .filter(item -> item.getCompany().getComId().equals(boss.getCompany().getComId()))
                .filter(item -> item.getSource() == com.th4.edge.decoboon.finance.configure.LedgerSource.Receipt)
                .filter(item -> receipt.getReceiptId().equals(item.getSourceId()))
                .count()).isEqualTo(1);
        assertThatThrownBy(() -> financeAccessContext.recordLedger(
                boss, project,
                com.th4.edge.decoboon.finance.configure.LedgerDirection.Income,
                receivable.getType().getName(),
                com.th4.edge.decoboon.finance.configure.LedgerSource.Receipt,
                receipt.getReceiptId(), new BigDecimal("1.00"), receipt.getAccount(), receipt.getReceivedAt(),
                receipt.getPayerName(), "冲突来源", "金额不一致"))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("该来源已存在不同资金流水");
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

        // 项目类型：新签与接管不能混淆
        set(page, "designerId", null);
        set(page, "businessOrigin", BusinessOrigin.Carryover);
        assertThat(page.call(new Project()).getContent()).isEmpty();
        set(page, "businessOrigin", BusinessOrigin.New);
        assertThat(page.call(new Project()).getContent())
                .extracting(Project::getProjectId)
                .contains(project.getProjectId());
    }

    @Test
    void shouldAllowBossToOpenCompanyFulfillmentWorkbench() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();

        com.th4.edge.decoboon.statistic.service.pm.report.support.Report report =
                AopTestUtils.getTargetObject(pmReport);
        com.th4.edge.decoboon.statistic.model.PmReport result = report.call(boss);

        assertThat(result.getProjectCount()).isGreaterThanOrEqualTo(1);
        assertThat(result.getFocusProjects())
                .extracting(Project::getProjectId)
                .contains(project.getProjectId());
    }

    @Test
    void shouldReportAllFilteredLedgerAndFinancialTotalsOnServer() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();
        Receivable receivable = createReceivable(boss, project);
        createReceipt(boss, project, receivable);
        Payable payable = createPayable(boss, project);
        createPayment(boss, payable);
        entityManager.flush();

        com.th4.edge.decoboon.finance.service.ledgers.report.support.Report ledger =
                AopTestUtils.getTargetObject(ledgerReport);
        set(ledger, "employee", boss);
        set(ledger, "projectId", project.getProjectId());
        set(ledger, "startDate", LocalDate.now());
        set(ledger, "endDate", LocalDate.now());
        var ledgerResult = ledger.call(new com.th4.edge.decoboon.finance.model.Ledger());
        assertThat(ledgerResult.getCount()).isEqualTo(2);
        assertThat(ledgerResult.getIncomeAmount()).isEqualByComparingTo("3000.00");
        assertThat(ledgerResult.getExpenseAmount()).isEqualByComparingTo("800.00");
        assertThat(ledgerResult.getNetCashFlowAmount()).isEqualByComparingTo("2200.00");
        assertThat(ledgerResult.getTotalAmount()).isEqualByComparingTo("3800.00");

        com.th4.edge.decoboon.finance.service.payables.report.support.Report payables =
                AopTestUtils.getTargetObject(payableReport);
        set(payables, "employee", boss);
        set(payables, "projectId", project.getProjectId());
        var payableResult = payables.call(new com.th4.edge.decoboon.finance.model.Payable());
        assertThat(payableResult.getCount()).isEqualTo(1);
        assertThat(payableResult.getTotalAmount()).isEqualByComparingTo("800.00");
        assertThat(payableResult.getPaidAmount()).isEqualByComparingTo("800.00");
        assertThat(payableResult.getOutstandingAmount()).isEqualByComparingTo("0.00");

        com.th4.edge.decoboon.finance.service.summary.report.support.Report summary =
                AopTestUtils.getTargetObject(summaryReport);
        set(summary, "employee", boss);
        var summaryResult = summary.call(new Project());
        assertThat(summaryResult.getReceiptCount()).isEqualTo(1);
        assertThat(summaryResult.getPaymentCount()).isEqualTo(1);
        assertThat(summaryResult.getMonthReceiptCount()).isEqualTo(1);
        assertThat(summaryResult.getMonthPaymentCount()).isEqualTo(1);
        assertThat(summaryResult.getReceiptAmount()).isEqualByComparingTo("3000.00");
        assertThat(summaryResult.getPaymentAmount()).isEqualByComparingTo("800.00");
        assertThat(summaryResult.getMonthReceiptAmount()).isEqualByComparingTo("3000.00");
        assertThat(summaryResult.getMonthPaymentAmount()).isEqualByComparingTo("800.00");
        assertThat(summaryResult.getMonthNetCashFlowAmount()).isEqualByComparingTo("2200.00");

        Ledger receiptLedger = ledgerRepository.findAll().stream()
                .filter(item -> item.getDirection() == com.th4.edge.decoboon.finance.configure.LedgerDirection.Income)
                .findFirst().orElseThrow();
        voucherContext.build(boss, List.of(receiptLedger.getLedgerId()));
        entityManager.flush();

        com.th4.edge.decoboon.document.service.voucher.report.support.Report vouchers =
                AopTestUtils.getTargetObject(voucherReport);
        set(vouchers, "employee", boss);
        set(vouchers, "type", "Receipt");
        set(vouchers, "startDate", LocalDate.now());
        set(vouchers, "endDate", LocalDate.now());
        var voucherResult = vouchers.call(new com.th4.edge.decoboon.document.model.VoucherRecord());
        assertThat(voucherResult.getCount()).isEqualTo(1);
        assertThat(voucherResult.getTotalAmount()).isEqualByComparingTo("3000.00");
        assertThat(voucherResult.getReceiptAmount()).isEqualByComparingTo("3000.00");
        assertThat(voucherResult.getPaymentAmount()).isEqualByComparingTo("0.00");
        assertThat(voucherResult.getMonthlyAmounts()).hasSize(1);
        assertThat(voucherResult.getMonthlyAmounts().get(0).getAmount()).isEqualByComparingTo("3000.00");
    }

    @Test
    void shouldIsolateHistoricalSettledCarryoverFromCurrentSettlementWorklist() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();
        project.setBusinessOrigin(BusinessOrigin.Carryover);
        project.setStatus(ProjectStatus.Settled);
        projectRepository.save(project);
        entityManager.flush();

        com.th4.edge.decoboon.finance.service.projects.page.support.Page page =
                AopTestUtils.getTargetObject(financeProjectPage);
        set(page, "employee", boss);
        set(page, "current", 1);
        set(page, "size", 20);
        set(page, "keyword", null);
        set(page, "status", null);
        set(page, "scope", null);
        assertThat(page.call(new Project()).getContent())
                .extracting(item -> item.getProject().getProjectId())
                .doesNotContain(project.getProjectId());

        set(page, "scope", "archive");
        var archived = page.call(new Project()).getContent().stream()
                .filter(item -> project.getProjectId().equals(item.getProject().getProjectId()))
                .findFirst()
                .orElseThrow();
        assertThat(archived.getHistoricalArchive()).isTrue();
        assertThat(archived.getArchiveLabel()).isEqualTo("历史接管项目 / 历史归档");
        assertThat(archived.getSettlementStatusLabel()).isEqualTo("历史已结算，明细未接管");
        assertThat(archived.getSettlementActionable()).isFalse();

        com.th4.edge.decoboon.finance.service.settlements.save.support.Save save =
                AopTestUtils.getTargetObject(settlementSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        assertThatThrownBy(() -> save.call(new com.th4.edge.decoboon.finance.model.Settlement()))
                .hasMessageContaining("历史接管项目只作归档展示");
    }

    @Test
    void shouldExposeFlatOtherCategoriesAndLinkManualLedgerByCategoryId() throws Exception {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();

        com.th4.edge.decoboon.finance.service.ledgercategories.tree.support.Tree tree =
                AopTestUtils.getTargetObject(ledgerCategoryTree);
        set(tree, "employee", boss);
        List<com.th4.edge.decoboon.finance.model.LedgerCategory> categories =
                tree.call(new com.th4.edge.decoboon.finance.model.LedgerCategory());
        assertThat(categories).allMatch(item -> item.getParent() == null);
        assertThat(categories).allMatch(item -> item.getChildren().isEmpty());
        assertThat(categories).extracting(com.th4.edge.decoboon.finance.model.LedgerCategory::getName)
                .contains("项目零星其他支出", "企业其他费用", "其他收入", "资金往来收入")
                .doesNotContain("工程款", "材料采购", "工人工资", "项目款");
        assertThat(categories).extracting(com.th4.edge.decoboon.finance.model.LedgerCategory::getDescription)
                .allMatch(value -> value != null && !value.isBlank());

        com.th4.edge.decoboon.finance.service.ledgercategories.save.support.Save categorySave =
                AopTestUtils.getTargetObject(ledgerCategorySave);
        set(categorySave, "employee", boss);
        set(categorySave, "name", "展会费用");
        set(categorySave, "description", "用于展会摊位、活动物料等企业其他费用；不进入项目成本。");
        set(categorySave, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(categorySave, "parentId", null);
        var custom = categorySave.call(new com.th4.edge.decoboon.finance.model.LedgerCategory());
        assertThat(custom.getBuiltIn()).isFalse();
        assertThat(custom.getParent()).isNull();
        assertThat(custom.getDescription()).contains("展会摊位", "不进入项目成本");

        com.th4.edge.decoboon.finance.service.ledgercategories.update.support.Update categoryUpdate =
                AopTestUtils.getTargetObject(ledgerCategoryUpdate);
        set(categoryUpdate, "employee", boss);
        set(categoryUpdate, "categoryId", custom.getCategoryId());
        set(categoryUpdate, "name", "展会费用");
        set(categoryUpdate, "description", "用于展会摊位、活动物料等企业其他费用；由企业按当前管理口径维护。");
        set(categoryUpdate, "active", true);
        var updatedCustom = categoryUpdate.call(new com.th4.edge.decoboon.finance.model.LedgerCategory());
        assertThat(updatedCustom.getDescription()).contains("由企业按当前管理口径维护");

        com.th4.edge.decoboon.finance.service.ledgers.save.support.Save save =
                AopTestUtils.getTargetObject(ledgerSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(save, "category", custom.getName());
        set(save, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.ProjectIncidentalCost);
        set(save, "categoryId", custom.getCategoryId());
        set(save, "amount", new BigDecimal("120.00"));
        set(save, "account", FundAccount.CompanyBank);
        set(save, "occurredAt", LocalDate.now());
        set(save, "counterparty", "会展中心");
        set(save, "summary", "展会摊位费");
        set(save, "remark", null);
        Ledger ledger = save.call(new Ledger());
        assertThat(ledger.getCategoryNode().getCategoryId()).isEqualTo(custom.getCategoryId());
        assertThat(ledger.getProject().getProjectId()).isEqualTo(project.getProjectId());
        com.th4.edge.decoboon.finance.service.projects.report.support.Report projectReport =
                AopTestUtils.getTargetObject(financeProjectReport);
        set(projectReport, "employee", boss);
        set(projectReport, "projectId", project.getProjectId());
        assertThat(projectReport.call(new Project()).getRecentLedgers())
                .extracting(Ledger::getLedgerId)
                .contains(ledger.getLedgerId());

        com.th4.edge.decoboon.finance.service.ledgercategories.delete.support.Delete delete =
                AopTestUtils.getTargetObject(ledgerCategoryDelete);
        set(delete, "employee", boss);
        set(delete, "categoryId", custom.getCategoryId());
        delete.call(new com.th4.edge.decoboon.finance.model.LedgerCategory());
        assertThat(custom.getActive()).isFalse();

        // 分类停用后，负责人仍应能更正历史流水元数据，并保留原分类关联和回显文本。
        com.th4.edge.decoboon.finance.service.ledgers.update.support.Update update =
                AopTestUtils.getTargetObject(ledgerUpdate);
        set(update, "employee", boss);
        set(update, "ledgerId", ledger.getLedgerId());
        set(update, "categoryId", custom.getCategoryId());
        set(update, "counterparty", "会展中心");
        set(update, "summary", "展会摊位费（更正）");
        set(update, "remark", "分类描述更正");
        set(update, "reason", "分类选择错误");
        update.call(new Ledger());
        Ledger afterInactiveUpdate = ledgerRepository.findByCompany_ComIdAndLedgerId(
                boss.getCompany().getComId(), ledger.getLedgerId()).orElseThrow();
        assertThat(afterInactiveUpdate.getSummary()).isEqualTo("展会摊位费（更正）");
        assertThat(afterInactiveUpdate.getCategoryNode().getCategoryId()).isEqualTo(custom.getCategoryId());
        assertThat(afterInactiveUpdate.getCategory()).isEqualTo("展会费用");
        assertThat(ledgerRevisionRepository.findByCompany_ComIdAndLedger_LedgerIdOrderByRevisionIdDesc(
                boss.getCompany().getComId(), ledger.getLedgerId())).hasSize(1);
        assertBoundedLedgerProjection(ledgerUpdate, afterInactiveUpdate);

        // 冲正重记沿用已停用的原分类时，也应保留历史分类，而不是把关联断开。
        com.th4.edge.decoboon.finance.service.ledgers.correct.support.Correct correct =
                AopTestUtils.getTargetObject(ledgerCorrect);
        set(correct, "employee", boss);
        set(correct, "ledgerId", ledger.getLedgerId());
        set(correct, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(correct, "category", custom.getName());
        set(correct, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.ProjectIncidentalCost);
        set(correct, "categoryId", custom.getCategoryId());
        set(correct, "amount", new BigDecimal("150.00"));
        set(correct, "account", FundAccount.CompanyBank);
        set(correct, "occurredAt", LocalDate.now());
        set(correct, "counterparty", "会展中心");
        set(correct, "summary", "展会摊位费（重记）");
        set(correct, "remark", null);
        set(correct, "projectId", project.getProjectId());
        set(correct, "reason", "金额录入错误");
        Ledger replacement = correct.call(new Ledger());
        assertThat(replacement.getAmount()).isEqualByComparingTo("150.00");
        assertThat(replacement.getCategoryNode().getCategoryId()).isEqualTo(custom.getCategoryId());
        assertThat(replacement.getCategory()).isEqualTo("展会费用");
        assertThat(replacement.getOriginalLedger().getLedgerId()).isEqualTo(ledger.getLedgerId());
        assertBoundedLedgerProjection(ledgerCorrect, replacement);
        assertThat(ledger.getCorrectionGroupId()).isNotBlank();
        assertThat(ledgerRepository.findByCompany_ComIdAndLedgerId(boss.getCompany().getComId(), ledger.getLedgerId())
                .orElseThrow().getCorrectionGroupId()).isEqualTo(replacement.getCorrectionGroupId());
        assertThat(ledgerRevisionRepository.findByCompany_ComIdAndLedger_LedgerIdOrderByRevisionIdDesc(
                boss.getCompany().getComId(), ledger.getLedgerId())).hasSize(2);

        Employee finance = new Employee();
        finance.setCompany(boss.getCompany());
        finance.setStore(boss.getStore());
        finance.setRole(EmployeeRole.Finance);
        finance.setStatus(EmployeeStatus.Active);
        finance.setName("财务测试");
        finance.setTelNo("17" + String.format("%09d", Math.abs(System.nanoTime() % 1_000_000_000L)));
        finance.setJoinedAt(LocalDateTime.now());
        finance = employeeRepository.save(finance);
        set(correct, "employee", finance);
        assertThatThrownBy(() -> correct.call(new Ledger()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("无权");
    }

    @Test
    void shouldEnforceManualLedgerBoundaryAndIncludeProjectManualAmounts() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();

        com.th4.edge.decoboon.finance.service.ledgers.save.support.Save save =
                AopTestUtils.getTargetObject(ledgerSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(save, "amount", new BigDecimal("100.00"));
        set(save, "account", FundAccount.CompanyBank);
        set(save, "occurredAt", LocalDate.now());
        set(save, "counterparty", "临时保洁");
        set(save, "summary", "现场临时保洁");
        set(save, "remark", null);

        // 项目零星成本必须关联项目。
        set(save, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.ProjectIncidentalCost);
        set(save, "category", "临时保洁");
        set(save, "projectId", null);
        assertThatThrownBy(() -> save.call(new Ledger()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("项目零星成本必须关联项目");

        // 工程款、材料款等已有应收/应付闭环的业务禁止在手工记账登记。
        set(save, "projectId", project.getProjectId());
        set(save, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Income);
        set(save, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.OtherIncome);
        set(save, "category", "工程款");
        assertThatThrownBy(() -> save.call(new Ledger()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("应收计划");

        set(save, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(save, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.CompanyExpense);
        set(save, "category", "材料采购");
        assertThatThrownBy(() -> save.call(new Ledger()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("应付");

        // 项目零星成本：计入项目实际成本。
        set(save, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(save, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.ProjectIncidentalCost);
        set(save, "category", "临时保洁");
        Ledger costLedger = save.call(new Ledger());
        assertThat(costLedger.getPurpose())
                .isEqualTo(com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.ProjectIncidentalCost);
        assertThat(costLedger.getCategory()).isEqualTo("临时保洁");

        // 其他收入 + 关联项目：计入项目收入。
        set(save, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Income);
        set(save, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.OtherIncome);
        set(save, "category", "设计费");
        set(save, "summary", "设计费收入");
        Ledger incomeLedger = save.call(new Ledger());
        assertThat(incomeLedger.getPurpose())
                .isEqualTo(com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.OtherIncome);

        // 项目财务报告同源纳入手工成本与收入。
        com.th4.edge.decoboon.finance.service.projects.report.support.Report projectReport =
                AopTestUtils.getTargetObject(financeProjectReport);
        set(projectReport, "employee", boss);
        set(projectReport, "projectId", project.getProjectId());
        var report = projectReport.call(new Project());
        assertThat(report.getManualCostAmount()).isEqualByComparingTo("100.00");
        assertThat(report.getManualIncomeAmount()).isEqualByComparingTo("100.00");
        assertThat(report.getTotalCost()).isGreaterThanOrEqualTo(new BigDecimal("100.00"));

        // 历史客户端未传 purpose：默认“其他”，不进入项目成本/收入。
        set(save, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Income);
        set(save, "purpose", null);
        set(save, "category", "补贴收入");
        set(save, "summary", "政府补贴");
        Ledger legacyDefault = save.call(new Ledger());
        assertThat(legacyDefault.getPurpose())
                .isEqualTo(com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.Other);
    }

    @Test
    void shouldAllowBossToVoidManualLedgerOnly() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();
        Project project = fixture.project();

        com.th4.edge.decoboon.finance.service.ledgercategories.tree.support.Tree tree =
                AopTestUtils.getTargetObject(ledgerCategoryTree);
        set(tree, "employee", boss);
        var projectOther = tree.call(new com.th4.edge.decoboon.finance.model.LedgerCategory()).stream()
                .filter(item -> item.getDirection() == com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense)
                .filter(item -> "项目零星其他支出".equals(item.getName()))
                .findFirst().orElseThrow();

        com.th4.edge.decoboon.finance.service.ledgers.save.support.Save save =
                AopTestUtils.getTargetObject(ledgerSave);
        set(save, "employee", boss);
        set(save, "projectId", project.getProjectId());
        set(save, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(save, "category", projectOther.getName());
        set(save, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.ProjectIncidentalCost);
        set(save, "categoryId", projectOther.getCategoryId());
        set(save, "amount", new BigDecimal("88.00"));
        set(save, "account", FundAccount.CompanyBank);
        set(save, "occurredAt", LocalDate.now());
        set(save, "counterparty", "作废测试供应商");
        set(save, "summary", "作废测试流水");
        set(save, "remark", null);
        Ledger manual = save.call(new Ledger());

        com.th4.edge.decoboon.finance.service.ledgers.report.support.Report report =
                AopTestUtils.getTargetObject(ledgerReport);
        set(report, "employee", boss);
        set(report, "projectId", project.getProjectId());
        set(report, "startDate", LocalDate.now());
        set(report, "endDate", LocalDate.now());
        assertThat(report.call(new Ledger()).getCount()).isEqualTo(1);

        // 财务不可作废；仅企业负责人可作废。
        Employee finance = new Employee();
        finance.setCompany(boss.getCompany());
        finance.setStore(boss.getStore());
        finance.setRole(EmployeeRole.Finance);
        finance.setStatus(EmployeeStatus.Active);
        finance.setName("财务作废测试");
        finance.setTelNo("19" + String.format("%09d", Math.abs(System.nanoTime() % 1_000_000_000L)));
        finance.setJoinedAt(LocalDateTime.now());
        finance = employeeRepository.save(finance);

        com.th4.edge.decoboon.finance.service.ledgers.delete.support.Delete delete =
                AopTestUtils.getTargetObject(ledgerDelete);
        set(delete, "employee", finance);
        set(delete, "ledgerId", manual.getLedgerId());
        set(delete, "reason", "录入错误");
        assertThatThrownBy(() -> delete.call(new Ledger()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("无权");

        set(delete, "employee", boss);
        Ledger voided = delete.call(new Ledger());
        assertThat(voided.getVoided()).isTrue();
        assertThat(voided.getVoidReason()).isEqualTo("录入错误");
        assertThat(voided.getVoidedBy().getEmpId()).isEqualTo(boss.getEmpId());

        // 原记录保留，但台账/汇总不再返回。
        Ledger stored = ledgerRepository.findByCompany_ComIdAndLedgerId(boss.getCompany().getComId(), manual.getLedgerId())
                .orElseThrow();
        assertThat(stored.getVoided()).isTrue();
        assertThat(report.call(new Ledger()).getCount()).isZero();
        assertThat(ledgerRevisionRepository
                .findByCompany_ComIdAndLedger_LedgerIdOrderByRevisionIdDesc(boss.getCompany().getComId(), manual.getLedgerId()))
                .hasSize(1)
                .extracting(item -> item.getType().name())
                .contains("Void");

        // 自动流水不可作废。
        Receivable receivable = createReceivable(boss, project);
        Receipt receipt = createReceipt(boss, project, receivable);
        Ledger receiptLedger = ledgerRepository
                .findByCompany_ComIdAndProject_ProjectIdOrderByOccurredAtDescLedgerIdDesc(
                        boss.getCompany().getComId(), project.getProjectId())
                .stream()
                .filter(item -> item.getSource() == com.th4.edge.decoboon.finance.configure.LedgerSource.Receipt)
                .findFirst().orElseThrow();
        set(delete, "ledgerId", receiptLedger.getLedgerId());
        set(delete, "reason", "自动流水测试");
        assertThatThrownBy(() -> delete.call(new Ledger()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("仅手工记账流水允许作废");

        // 已冲正重记的流水不可作废。
        set(save, "summary", "冲正后再作废测试");
        Ledger second = save.call(new Ledger());
        com.th4.edge.decoboon.finance.service.ledgers.correct.support.Correct correct =
                AopTestUtils.getTargetObject(ledgerCorrect);
        set(correct, "employee", boss);
        set(correct, "ledgerId", second.getLedgerId());
        set(correct, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(correct, "category", "冲正重记分类");
        set(correct, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.ProjectIncidentalCost);
        set(correct, "categoryId", projectOther.getCategoryId());
        set(correct, "amount", new BigDecimal("99.00"));
        set(correct, "account", FundAccount.CompanyBank);
        set(correct, "occurredAt", LocalDate.now());
        set(correct, "counterparty", "作废测试供应商");
        set(correct, "summary", "冲正重记后的流水");
        set(correct, "remark", null);
        set(correct, "projectId", project.getProjectId());
        set(correct, "reason", "冲正测试");
        Ledger replacement = correct.call(new Ledger());
        set(delete, "ledgerId", replacement.getLedgerId());
        set(delete, "reason", "冲正后作废测试");
        assertThatThrownBy(() -> delete.call(new Ledger()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("已冲正重记，不能作废");
    }

    @Test
    void shouldSaveOpeningBalancesAndKeepThemOutOfIncome() {
        Fixture fixture = createFixture();
        Employee boss = fixture.boss();

        com.th4.edge.decoboon.finance.service.openingbalances.save.support.Save save =
                AopTestUtils.getTargetObject(openingBalanceSave);
        set(save, "employee", boss);
        set(save, "account", FundAccount.CompanyBank);
        set(save, "amount", new BigDecimal("1000.00"));
        set(save, "asOfDate", LocalDate.now());
        set(save, "remark", "接管日对公余额");
        com.th4.edge.decoboon.finance.model.OpeningBalance bank = save.call(
                new com.th4.edge.decoboon.finance.model.OpeningBalance());

        set(save, "account", FundAccount.Cash);
        set(save, "amount", new BigDecimal("200.00"));
        set(save, "remark", "接管日现金");
        com.th4.edge.decoboon.finance.model.OpeningBalance cash = save.call(
                new com.th4.edge.decoboon.finance.model.OpeningBalance());
        assertThat(cash.getOpeningId()).isNotEqualTo(bank.getOpeningId());

        // 同一企业 + 同一账户 + 同一接管日重复保存为更正余额。
        set(save, "account", FundAccount.CompanyBank);
        set(save, "amount", new BigDecimal("1200.00"));
        set(save, "remark", "接管日对公余额更正");
        com.th4.edge.decoboon.finance.model.OpeningBalance updated = save.call(
                new com.th4.edge.decoboon.finance.model.OpeningBalance());
        assertThat(updated.getOpeningId()).isEqualTo(bank.getOpeningId());
        assertThat(updated.getAmount()).isEqualByComparingTo("1200.00");

        com.th4.edge.decoboon.finance.service.openingbalances.page.support.Page page =
                AopTestUtils.getTargetObject(openingBalancePage);
        set(page, "employee", boss);
        set(page, "current", 1);
        set(page, "size", 20);
        assertThat(page.call(new com.th4.edge.decoboon.finance.model.OpeningBalance()).getContent())
                .hasSize(2);

        // 接管后发生一笔手续费支出：期初余额不进收入，现金流 = 期初 + 接管后净流量。
        com.th4.edge.decoboon.finance.service.ledgers.save.support.Save ledgerSave =
                AopTestUtils.getTargetObject(this.ledgerSave);
        set(ledgerSave, "employee", boss);
        set(ledgerSave, "projectId", null);
        set(ledgerSave, "direction", com.th4.edge.decoboon.finance.configure.LedgerDirection.Expense);
        set(ledgerSave, "purpose", com.th4.edge.decoboon.finance.configure.LedgerManualPurpose.CompanyExpense);
        set(ledgerSave, "category", "银行手续费");
        set(ledgerSave, "amount", new BigDecimal("100.00"));
        set(ledgerSave, "account", FundAccount.CompanyBank);
        set(ledgerSave, "occurredAt", LocalDate.now());
        set(ledgerSave, "counterparty", "银行");
        set(ledgerSave, "summary", "银行手续费");
        set(ledgerSave, "remark", null);
        ledgerSave.call(new Ledger());
        entityManager.flush();

        com.th4.edge.decoboon.finance.service.summary.report.support.Report summary =
                AopTestUtils.getTargetObject(summaryReport);
        set(summary, "employee", boss);
        com.th4.edge.decoboon.finance.model.FinanceSummary result = summary.call(new Project());
        assertThat(result.getOpeningBalanceAmount()).isEqualByComparingTo("1400.00");
        assertThat(result.getCashBalance()).isEqualByComparingTo("1300.00");
        assertThat(result.getMonthIncome()).isEqualByComparingTo("0.00");
        assertThat(result.getMonthExpense()).isEqualByComparingTo("100.00");

        // 设计师不可维护期初资金余额。
        Employee designer = new Employee();
        designer.setCompany(boss.getCompany());
        designer.setStore(boss.getStore());
        designer.setRole(EmployeeRole.Designer);
        designer.setStatus(EmployeeStatus.Active);
        designer.setName("设计师期初测试");
        designer.setTelNo("16" + String.format("%09d", Math.abs(System.nanoTime() % 1_000_000_000L)));
        designer.setJoinedAt(LocalDateTime.now());
        designer = employeeRepository.save(designer);
        set(save, "employee", designer);
        assertThatThrownBy(() -> save.call(new com.th4.edge.decoboon.finance.model.OpeningBalance()))
                .isInstanceOf(com.th4.edge.framework.exception.DefinedException.class)
                .hasMessageContaining("无权");
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
        set(save, "name", "自动化测试其他应收");
        set(save, "stageName", "补充款");
        set(save, "type", ReceivableType.Other);
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
        set(save, "name", "自动化测试费用支出");
        set(save, "type", PayableType.Expense);
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
        Payable payable = new Payable();
        payable.setCompany(boss.getCompany());
        payable.setProject(project);
        payable.setProperty(project.getProperty());
        payable.setStore(project.getStore());
        payable.setTeam(project.getTeam());
        payable.setOwner(boss);
        payable.setName("木工张伟工资");
        payable.setType(PayableType.Salary);
        payable.setPayeeName("木工班组");
        payable.setAmount(new BigDecimal("40.00"));
        payable.setPaidAmount(BigDecimal.ZERO);
        payable.setDueDate(LocalDate.now());
        payable.setStatus(com.th4.edge.decoboon.finance.configure.PayableStatus.Pending);
        payable.setUrgent(false);
        payable.setRemark("工资待发");
        payable.setCreatedBy(boss);
        return payableRepository.save(payable);
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

    private void assertBoundedLedgerProjection(Object provider, Ledger ledger) throws Exception {
        Object target = AopTestUtils.getTargetObject(provider);
        com.th4.edge.framework.data.jackson.annotation.Properties properties =
                AnnotationUtils.findAnnotation(target.getClass(), com.th4.edge.framework.data.jackson.annotation.Properties.class);
        assertThat(properties).as("Provider must declare @Properties").isNotNull();
        com.th4.edge.framework.data.jackson.context.MixInModelPropertiesHolder.setAspectProperties(properties);
        try {
            String json = objectMapper.writeValueAsString(ledger);
            assertThat(json.length())
                    .as("更正响应必须使用最小项目投影，不能把整棵项目对象返回给 App")
                    .isLessThan(8000);
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(json);
            assertThat(node.path("project").path("projectId").asLong()).isPositive();
            assertThat(node.path("project").has("company")).isFalse();
            assertThat(node.path("project").has("quote")).isFalse();
        } finally {
            com.th4.edge.framework.data.jackson.context.MixInModelPropertiesHolder.removeAspectProperties();
        }
    }

    private void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }
    private record Fixture(Employee boss, Project project) {
    }

}

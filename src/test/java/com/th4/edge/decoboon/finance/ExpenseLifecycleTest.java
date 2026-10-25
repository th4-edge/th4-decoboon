package com.th4.edge.decoboon.finance;

import com.th4.edge.decoboon.finance.configure.ExpenseDocumentType;
import com.th4.edge.decoboon.finance.configure.FundAccount;
import com.th4.edge.decoboon.finance.configure.ExpensePaymentMode;
import com.th4.edge.decoboon.finance.configure.ExpenseStatus;
import com.th4.edge.decoboon.finance.context.ExpenseCategoryContext;
import com.th4.edge.decoboon.finance.model.Expense;
import com.th4.edge.decoboon.finance.model.ExpenseAttachment;
import com.th4.edge.decoboon.finance.model.ExpenseCategory;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Payment;
import com.th4.edge.decoboon.finance.repository.ExpenseRepository;
import com.th4.edge.decoboon.finance.repository.LedgerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.PaymentRepository;
import com.th4.edge.decoboon.finance.repository.ExpenseRevisionRepository;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
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
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 费用草稿、提交版本、审批权限和企业/本人范围回归测试。 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:expense-lifecycle;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "th4.edge.components.storage.provider=local",
        "th4.edge.framework.upload-dir=${java.io.tmpdir}/expense-lifecycle-upload",
        "logging.level.root=ERROR",
        "logging.level.org.hibernate.SQL=OFF"
})
class ExpenseLifecycleTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ExpenseRepository expenseRepository;
    @Autowired private PayableRepository payableRepository;
    @Autowired private PaymentRepository paymentRepository;
    @Autowired private LedgerRepository ledgerRepository;
    @Autowired private ExpenseRevisionRepository revisionRepository;
    @Autowired private ExpenseCategoryContext categoryContext;
    @Autowired private ObjectProvider<com.th4.edge.decoboon.finance.service.expenses.save.support.Save> saveProvider;
    @Autowired private ObjectProvider<com.th4.edge.decoboon.finance.service.expenses.update.support.Update> updateProvider;
    @Autowired private ObjectProvider<com.th4.edge.decoboon.finance.service.expenses.info.support.Info> infoProvider;
    @Autowired private ObjectProvider<com.th4.edge.decoboon.finance.service.expenses.page.support.Page> pageProvider;
    @Autowired private ObjectProvider<com.th4.edge.decoboon.finance.service.expensecategories.update.support.Update> categoryUpdateProvider;
    @Autowired private ObjectProvider<com.th4.edge.decoboon.finance.service.payments.save.support.Save> paymentSaveProvider;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private final AtomicLong sequence = new AtomicLong(System.nanoTime());

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void shouldCreateSubmitApproveEmployeeReimbursementWithOneRevisionAndIdempotency() {
        Fixture fixture = fixture();
        ExpenseCategory category = category(fixture.boss());
        Expense first = createExpense(fixture.marketer(), category, ExpensePaymentMode.Reimbursement,
                "expense-idem-1", List.of(attachment(fixture, "receipt-1.png")));
        Expense repeated = createExpense(fixture.marketer(), category, ExpensePaymentMode.Reimbursement,
                "expense-idem-1", List.of(attachment(fixture, "receipt-1.png")));

        assertThat(first.getStatus()).isEqualTo(ExpenseStatus.Draft);
        assertThat(first.getAllowedActions()).contains("submit", "edit", "delete");
        assertThat(repeated.getExpenseId()).isEqualTo(first.getExpenseId());
        assertThat(expenseRepository.findAll().stream()
                .filter(item -> item.getCompany().getComId().equals(fixture.company().getComId()))
                .count()).isEqualTo(1);

        Expense submitted = transition(fixture.marketer(), first.getExpenseId(), "Submitted", null);
        assertThat(submitted.getStatus()).isEqualTo(ExpenseStatus.Submitted);
        assertThat(submitted.getAllowedActions()).contains("withdraw");
        assertThat(submitted.getVersion()).isEqualTo(1);
        assertThat(revisionRepository.findByCompany_ComIdAndExpense_ExpenseIdOrderByVersionDesc(
                fixture.company().getComId(), first.getExpenseId())).hasSize(1);
        assertThat(info(fixture.marketer(), first.getExpenseId()).getRevisions()).hasSize(1);

        Expense approved = transition(fixture.manager(), first.getExpenseId(), "Approved", "同意报销");
        assertThat(approved.getStatus()).isEqualTo(ExpenseStatus.Approved);
        assertThat(approved.getApprovedBy().getEmpId()).isEqualTo(fixture.manager().getEmpId());
        assertThat(approved.getSelfApproved()).isFalse();
        assertThat(approved.getAllowedActions()).contains("pay");
    }

    @Test
    void shouldRejectStoreManagerSelfApprovalAndAllowBossApproval() {
        Fixture fixture = fixture();
        ExpenseCategory category = category(fixture.manager());
        Expense expense = createExpense(fixture.manager(), category, ExpensePaymentMode.Reimbursement,
                "manager-self-1", List.of(attachment(fixture, "manager.png")));
        transition(fixture.manager(), expense.getExpenseId(), "Submitted", null);

        assertThatThrownBy(() -> transition(fixture.manager(), expense.getExpenseId(), "Approved", "自己批准"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("店长不能审批本人报销");

        Expense approved = transition(fixture.boss(), expense.getExpenseId(), "Approved", "负责人审批");
        assertThat(approved.getStatus()).isEqualTo(ExpenseStatus.Approved);

        Expense bossExpense = createExpense(fixture.boss(), category(fixture.boss()),
                ExpensePaymentMode.Reimbursement, "boss-self-1", List.of(attachment(fixture, "boss.png")));
        transition(fixture.boss(), bossExpense.getExpenseId(), "Submitted", null);
        Expense bossApproved = transition(fixture.boss(), bossExpense.getExpenseId(), "Approved", "一人企业自审");
        assertThat(bossApproved.getStatus()).isEqualTo(ExpenseStatus.Approved);
        assertThat(bossApproved.getSelfApproved()).isTrue();
    }

    @Test
    void shouldAllowFinanceReturnButForbidFinanceApprovalAndSelfReturn() {
        Fixture fixture = fixture();
        ExpenseCategory category = category(fixture.marketer());
        Expense expense = createExpense(fixture.marketer(), category, ExpensePaymentMode.Direct,
                "finance-return-1", List.of(attachment(fixture, "finance.png")));
        transition(fixture.marketer(), expense.getExpenseId(), "Submitted", null);

        Expense returned = transition(fixture.finance(), expense.getExpenseId(), "Returned", "请补充发票");
        assertThat(returned.getStatus()).isEqualTo(ExpenseStatus.Returned);
        assertThat(returned.getApprovalNote()).isEqualTo("请补充发票");

        Expense resubmitted = transition(fixture.marketer(), expense.getExpenseId(), "Submitted", null);
        assertThat(resubmitted.getVersion()).isEqualTo(2);
        assertThat(revisionRepository.findByCompany_ComIdAndExpense_ExpenseIdOrderByVersionDesc(
                fixture.company().getComId(), expense.getExpenseId())).hasSize(2);

        assertThatThrownBy(() -> transition(fixture.finance(), expense.getExpenseId(), "Approved", "财务批准"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权执行");

        Expense financeOwn = createExpense(fixture.finance(), category(fixture.finance()),
                ExpensePaymentMode.Reimbursement, "finance-self-return", List.of(attachment(fixture, "finance-self.png")));
        transition(fixture.finance(), financeOwn.getExpenseId(), "Submitted", null);
        assertThatThrownBy(() -> transition(fixture.finance(), financeOwn.getExpenseId(), "Returned", "本人退回"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("本人不能退回");
    }

    @Test
    void shouldEnforceRequiredEvidenceAndSelfDataScope() {
        Fixture fixture = fixture();
        ExpenseCategory category = category(fixture.marketer());
        Expense draftWithoutEvidence = createExpense(fixture.marketer(), category, ExpensePaymentMode.Direct,
                "missing-evidence-1", List.of());
        assertThat(draftWithoutEvidence.getStatus()).isEqualTo(ExpenseStatus.Draft);
        assertThatThrownBy(() -> transition(fixture.marketer(), draftWithoutEvidence.getExpenseId(), "Submitted", null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("证据附件");

        Expense expense = createExpense(fixture.marketer(), category, ExpensePaymentMode.Direct,
                "scope-expense-1", List.of(attachment(fixture, "scope.png")));
        transition(fixture.marketer(), expense.getExpenseId(), "Submitted", null);

        assertThatThrownBy(() -> info(fixture.otherMarketer(), expense.getExpenseId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权访问");

        Fixture otherCompany = fixture();
        assertThatThrownBy(() -> info(otherCompany.marketer(), expense.getExpenseId()))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("无权访问");

        assertThat(page(fixture.marketer()).getContent()).extracting(Expense::getExpenseId)
                .contains(expense.getExpenseId());
        assertThat(page(fixture.otherMarketer()).getContent()).isEmpty();
        assertThat(page(fixture.manager()).getContent()).extracting(Expense::getExpenseId)
                .contains(expense.getExpenseId());

        assertThatThrownBy(() -> {
            com.th4.edge.decoboon.finance.service.expenses.update.support.Update update =
                    AopTestUtils.getTargetObject(updateProvider.getObject());
            ReflectionTestUtils.setField(update, "employee", fixture.marketer());
            ReflectionTestUtils.setField(update, "expenseId", expense.getExpenseId());
            ReflectionTestUtils.setField(update, "amount", new BigDecimal("999.00"));
            transactionTemplate.executeWithoutResult(status -> update.call(new Expense()));
        }).isInstanceOf(DefinedException.class).hasMessageContaining("不能修改");
    }

    @Test
    void shouldCreateSingleExpensePayableAndNoPaymentBeforePay() {
        Fixture fixture = fixture();
        Project project = createProject(fixture);
        ExpenseCategory category = category(fixture.marketer());
        Expense expense = createExpense(fixture.marketer(), category, ExpensePaymentMode.Direct,
                "project-expense-1", List.of(attachment(fixture, project, "project.png")), project.getProjectId());

        assertThat(expense.getProject().getProjectId()).isEqualTo(project.getProjectId());
        assertThat(expense.getStore().getStoreId()).isEqualTo(fixture.store().getStoreId());
        Expense approved = transition(fixture.marketer(), expense.getExpenseId(), "Submitted", null);
        assertThat(approved.getStatus()).isEqualTo(ExpenseStatus.Submitted);
        approved = transition(fixture.boss(), expense.getExpenseId(), "Approved", "项目费用通过");
        assertThat(approved.getStatus()).isEqualTo(ExpenseStatus.Approved);
        Payable payable = approved.getPayable();
        assertThat(payable).isNotNull();
        assertThat(payable.getAmount()).isEqualByComparingTo("680.00");
        assertThat(payable.getType().name()).isEqualTo("Expense");
        assertThat(payable.getSourceType()).isEqualTo("Expense");
        assertThat(payable.getSourceId()).isEqualTo(expense.getExpenseId());
        assertThat(paymentRepository.findByCompany_ComIdOrderByPaidAtDesc(
                fixture.company().getComId())).isEmpty();
    }

    @Test
    void shouldPayExpensePartiallyThenCloseWithoutDuplicatePayable() {
        Fixture fixture = fixture();
        ExpenseCategory category = category(fixture.marketer());
        Expense expense = createExpense(fixture.marketer(), category, ExpensePaymentMode.Reimbursement,
                "expense-payment-1", List.of(attachment(fixture, "payment.pdf")));
        transition(fixture.marketer(), expense.getExpenseId(), "Submitted", null);
        Expense approved = transition(fixture.manager(), expense.getExpenseId(), "Approved", "同意付款");
        Long payableId = approved.getPayable().getPayableId();

        Payment first = pay(fixture.finance(), payableId, new BigDecimal("200.00"), "expense-payment-p1");
        assertThat(first.getAmount()).isEqualByComparingTo("200.00");
        Expense partial = info(fixture.marketer(), expense.getExpenseId());
        assertThat(partial.getStatus()).isEqualTo(ExpenseStatus.Approved);
        assertThat(partial.getPaidAmount()).isEqualByComparingTo("200.00");
        assertThat(partial.getUnpaidAmount()).isEqualByComparingTo("480.00");

        pay(fixture.finance(), payableId, new BigDecimal("480.00"), "expense-payment-p2");
        Expense closed = info(fixture.marketer(), expense.getExpenseId());
        assertThat(closed.getStatus()).isEqualTo(ExpenseStatus.Closed);
        assertThat(closed.getPaidAmount()).isEqualByComparingTo("680.00");
        assertThat(closed.getUnpaidAmount()).isEqualByComparingTo("0.00");
        assertThat(expenseRepository.findAll().stream()
                .filter(item -> item.getExpenseId().equals(expense.getExpenseId()))
                .count()).isEqualTo(1);
        assertThat(payableRepository.findByCompany_ComIdAndSourceTypeAndSourceId(
                fixture.company().getComId(), "Expense", expense.getExpenseId())).isPresent();
        assertThat(ledgerRepository.findByCompany_ComIdOrderByOccurredAtDescLedgerIdDesc(
                fixture.company().getComId()).stream()
                .filter(item -> item.getSource() == com.th4.edge.decoboon.finance.configure.LedgerSource.Payment)
                .count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shouldRejectBuiltInCategoryMutationAndDeactivatedCategorySubmission() {
        Fixture fixture = fixture();
        ExpenseCategory builtIn = category(fixture.boss());
        assertThatThrownBy(() -> {
            com.th4.edge.decoboon.finance.service.expensecategories.update.support.Update update =
                    AopTestUtils.getTargetObject(categoryUpdateProvider.getObject());
            ReflectionTestUtils.setField(update, "employee", fixture.finance());
            ReflectionTestUtils.setField(update, "categoryId", builtIn.getCategoryId());
            ReflectionTestUtils.setField(update, "name", "被篡改的分类");
            transactionTemplate.executeWithoutResult(status -> update.call(new ExpenseCategory()));
        }).isInstanceOf(DefinedException.class).hasMessageContaining("平台基础分类名称不可修改");

        ExpenseCategory custom = Objects.requireNonNull(transactionTemplate.execute(status ->
                categoryContext.saveCustom(fixture.finance(), "验收临时分类", "验收停用场景", 999)));
        Expense draft = createExpense(fixture.marketer(), custom, ExpensePaymentMode.Direct,
                "deactivated-category-1", List.of(attachment(fixture, "deactivated.pdf")));
        transactionTemplate.executeWithoutResult(status -> categoryContext.deactivate(fixture.finance(), custom.getCategoryId()));
        assertThatThrownBy(() -> transition(fixture.marketer(), draft.getExpenseId(), "Submitted", null))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("费用分类已停用");
    }

    @Test
    void shouldRejectForeignEvidenceAndAmountThatRoundsToZero() {
        Fixture fixture = fixture();
        ExpenseCategory category = category(fixture.marketer());
        ExpenseAttachment foreign = attachment(fixture, "foreign.pdf");
        foreign.setFileUrl("companies/999999999/files/expense/2026/10/foreign.pdf");
        assertThatThrownBy(() -> createExpense(fixture.marketer(), category, ExpensePaymentMode.Direct,
                "foreign-evidence-1", List.of(foreign)))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("不属于当前企业或项目");

        assertThatThrownBy(() -> createExpenseWithAmount(fixture.marketer(), category, "rounding-zero-1",
                new BigDecimal("0.004")))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("费用金额必须大于 0");
    }

    @Test
    void shouldRejectDifferentIdempotencyIntentAndPreserveReturnAuditInRevision() {
        Fixture fixture = fixture();
        Project project = createProject(fixture);
        ExpenseCategory category = category(fixture.marketer());
        Expense first = createExpense(fixture.marketer(), category, ExpensePaymentMode.Direct,
                "intent-key-1", List.of(attachment(fixture, project, "intent.pdf")), project.getProjectId());
        assertThatThrownBy(() -> createExpenseWithProjectAndDescription(fixture, category,
                "intent-key-1", project.getProjectId(), "不同业务说明"))
                .isInstanceOf(DefinedException.class)
                .hasMessageContaining("该幂等键已用于其他费用提交");

        Expense submitted = transition(fixture.marketer(), first.getExpenseId(), "Submitted", null);
        Expense returned = transition(fixture.boss(), submitted.getExpenseId(), "Returned", "补充行程单");
        assertThat(returned.getApprovalNote()).isEqualTo("补充行程单");
        Expense resubmitted = transition(fixture.marketer(), returned.getExpenseId(), "Submitted", null);
        assertThat(resubmitted.getApprovalNote()).isNull();
        var revisions = info(fixture.marketer(), resubmitted.getExpenseId()).getRevisions();
        assertThat(revisions).hasSize(2);
        assertThat(revisions.get(0).getReturnReason()).isEqualTo("补充行程单");
        assertThat(revisions.get(0).getReturnedBy().getEmpId()).isEqualTo(fixture.boss().getEmpId());
        assertThat(revisions.get(0).getReturnedAt()).isNotNull();
        assertThat(revisions.get(0).getSnapshotJson()).contains("补充行程单");
    }

    private Payment pay(Employee actor, Long payableId, BigDecimal amount, String idempotencyKey) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            com.th4.edge.decoboon.finance.service.payments.save.support.Save save =
                    AopTestUtils.getTargetObject(paymentSaveProvider.getObject());
            ReflectionTestUtils.setField(save, "employee", actor);
            ReflectionTestUtils.setField(save, "payableId", payableId);
            ReflectionTestUtils.setField(save, "payableIds", null);
            ReflectionTestUtils.setField(save, "amount", amount);
            ReflectionTestUtils.setField(save, "account", FundAccount.CompanyBank);
            ReflectionTestUtils.setField(save, "paidAt", LocalDate.now());
            ReflectionTestUtils.setField(save, "payeeName", null);
            ReflectionTestUtils.setField(save, "workerId", null);
            ReflectionTestUtils.setField(save, "transactionNo", null);
            ReflectionTestUtils.setField(save, "voucherUrl", null);
            ReflectionTestUtils.setField(save, "remark", "费用付款验收");
            ReflectionTestUtils.setField(save, "idempotencyKey", idempotencyKey);
            return save.call(new Payment());
        }));
    }

    private Expense createExpense(Employee actor, ExpenseCategory category, ExpensePaymentMode mode,
                                  String idempotencyKey, List<ExpenseAttachment> attachments) {
        return createExpense(actor, category, mode, idempotencyKey, attachments, null);
    }

    private Expense createExpenseWithAmount(Employee actor, ExpenseCategory category,
                                             String idempotencyKey, BigDecimal amount) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            com.th4.edge.decoboon.finance.service.expenses.save.support.Save save =
                    AopTestUtils.getTargetObject(saveProvider.getObject());
            ReflectionTestUtils.setField(save, "employee", actor);
            ReflectionTestUtils.setField(save, "paymentMode", ExpensePaymentMode.Direct);
            ReflectionTestUtils.setField(save, "amount", amount);
            ReflectionTestUtils.setField(save, "expenseDate", LocalDate.now());
            ReflectionTestUtils.setField(save, "description", "金额边界验收");
            ReflectionTestUtils.setField(save, "categoryId", category.getCategoryId());
            ReflectionTestUtils.setField(save, "projectId", null);
            ReflectionTestUtils.setField(save, "storeId", null);
            ReflectionTestUtils.setField(save, "employeeId", null);
            ReflectionTestUtils.setField(save, "payeeEmployeeId", null);
            ReflectionTestUtils.setField(save, "idempotencyKey", idempotencyKey);
            ReflectionTestUtils.setField(save, "attachments", List.of());
            return save.call(new Expense());
        }));
    }

    private Expense createExpenseWithProjectAndDescription(Fixture fixture, ExpenseCategory category,
                                                            String idempotencyKey, Long projectId,
                                                            String description) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            com.th4.edge.decoboon.finance.service.expenses.save.support.Save save =
                    AopTestUtils.getTargetObject(saveProvider.getObject());
            ReflectionTestUtils.setField(save, "employee", fixture.marketer());
            ReflectionTestUtils.setField(save, "paymentMode", ExpensePaymentMode.Direct);
            ReflectionTestUtils.setField(save, "amount", new BigDecimal("680.00"));
            ReflectionTestUtils.setField(save, "expenseDate", LocalDate.now());
            ReflectionTestUtils.setField(save, "description", description);
            ReflectionTestUtils.setField(save, "categoryId", category.getCategoryId());
            ReflectionTestUtils.setField(save, "projectId", projectId);
            ReflectionTestUtils.setField(save, "storeId", null);
            ReflectionTestUtils.setField(save, "employeeId", null);
            ReflectionTestUtils.setField(save, "payeeEmployeeId", null);
            ReflectionTestUtils.setField(save, "idempotencyKey", idempotencyKey);
            Project scopeProject = new Project();
            scopeProject.setProjectId(projectId);
            ReflectionTestUtils.setField(save, "attachments", List.of(attachment(fixture, scopeProject, "intent.pdf")));
            return save.call(new Expense());
        }));
    }

    private Expense createExpense(Employee actor, ExpenseCategory category, ExpensePaymentMode mode,
                                  String idempotencyKey, List<ExpenseAttachment> attachments, Long projectId) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            com.th4.edge.decoboon.finance.service.expenses.save.support.Save save =
                    AopTestUtils.getTargetObject(saveProvider.getObject());
            ReflectionTestUtils.setField(save, "employee", actor);
            ReflectionTestUtils.setField(save, "paymentMode", mode);
            ReflectionTestUtils.setField(save, "amount", new BigDecimal("680.00"));
            ReflectionTestUtils.setField(save, "expenseDate", LocalDate.now());
            ReflectionTestUtils.setField(save, "description", "项目现场临时支出");
            ReflectionTestUtils.setField(save, "categoryId", category.getCategoryId());
            ReflectionTestUtils.setField(save, "projectId", projectId);
            ReflectionTestUtils.setField(save, "storeId", null);
            ReflectionTestUtils.setField(save, "employeeId", null);
            ReflectionTestUtils.setField(save, "payeeEmployeeId", null);
            ReflectionTestUtils.setField(save, "idempotencyKey", idempotencyKey);
            ReflectionTestUtils.setField(save, "attachments", attachments);
            return save.call(new Expense());
        }));
    }

    private Expense transition(Employee actor, Long expenseId, String status, String note) {
        return Objects.requireNonNull(transactionTemplate.execute(tx -> {
            com.th4.edge.decoboon.finance.service.expenses.update.support.Update update =
                    AopTestUtils.getTargetObject(updateProvider.getObject());
            ReflectionTestUtils.setField(update, "employee", actor);
            ReflectionTestUtils.setField(update, "expenseId", expenseId);
            ReflectionTestUtils.setField(update, "status", status);
            ReflectionTestUtils.setField(update, "approvalNote", note);
            return update.call(new Expense());
        }));
    }

    private Expense info(Employee actor, Long expenseId) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            com.th4.edge.decoboon.finance.service.expenses.info.support.Info info =
                    AopTestUtils.getTargetObject(infoProvider.getObject());
            ReflectionTestUtils.setField(info, "employee", actor);
            ReflectionTestUtils.setField(info, "expenseId", expenseId);
            return info.call(new Expense());
        }));
    }

    private org.springframework.data.domain.Page<Expense> page(Employee actor) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            com.th4.edge.decoboon.finance.service.expenses.page.support.Page page =
                    AopTestUtils.getTargetObject(pageProvider.getObject());
            ReflectionTestUtils.setField(page, "employee", actor);
            ReflectionTestUtils.setField(page, "current", 1);
            ReflectionTestUtils.setField(page, "size", 20);
            ReflectionTestUtils.setField(page, "status", null);
            ReflectionTestUtils.setField(page, "keyword", null);
            ReflectionTestUtils.setField(page, "projectId", null);
            ReflectionTestUtils.setField(page, "payeeEmployeeId", null);
            ReflectionTestUtils.setField(page, "storeId", null);
            return page.call(new Expense());
        }));
    }

    private ExpenseCategory category(Employee employee) {
        return Objects.requireNonNull(transactionTemplate.execute(status ->
                categoryContext.list(employee).stream()
                        .filter(item -> Boolean.TRUE.equals(item.getActive()))
                        .findFirst()
                        .orElseThrow()));
    }

    private ExpenseAttachment attachment(Fixture fixture, String fileName) {
        return attachment(fixture, null, fileName);
    }

    private ExpenseAttachment attachment(Fixture fixture, Project project, String fileName) {
        ExpenseAttachment attachment = new ExpenseAttachment();
        attachment.setDocumentType(ExpenseDocumentType.Receipt);
        String projectPart = project == null ? "" : "projects/" + project.getProjectId() + "/";
        attachment.setFileUrl("companies/" + fixture.company().getComId() + "/" + projectPart
                + "files/expense/2026/10/" + fileName);
        attachment.setFileName(fileName);
        attachment.setSize(1024L);
        return attachment;
    }

    private Project createProject(Fixture fixture) {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            long seq = sequence.incrementAndGet();
            Customer customer = new Customer();
            customer.setCompany(fixture.company());
            customer.setStore(fixture.store());
            customer.setOwner(fixture.marketer());
            customer.setName("费用项目客户");
            customer.setTelNo("16" + String.format("%09d", Math.abs(seq % 1_000_000_000L)));
            customer.setArea(new BigDecimal("90"));
            customer.setAddr("费用测试项目地址");
            customer = customerRepository.save(customer);

            Property property = new Property();
            property.setCompany(fixture.company());
            property.setStore(fixture.store());
            property.setOwner(fixture.marketer());
            property.setCustomer(customer);
            property.setPropertyNo("EXP-PROP-" + seq);
            property.setAddress("费用测试小区 2 栋 202");
            property.setArea(new BigDecimal("90"));
            property.setStatus(PropertyStatus.Serving);
            property = propertyRepository.save(property);

            Quote quote = new Quote();
            quote.setNo("EXP-QUOTE-" + seq);
            quote.setTitle("费用测试项目报价");
            quote.setCompany(fixture.company());
            quote.setStore(fixture.store());
            quote.setCustomer(customer);
            quote.setProperty(property);
            quote.setCreatedBy(fixture.marketer());
            quote.setVersion(1);
            quote.setMode(QuoteMode.Custom);
            quote.setArea(new BigDecimal("90"));
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
            project.setNo("EXP-PROJECT-" + seq);
            project.setName("费用测试项目");
            project.setCompany(fixture.company());
            project.setStore(fixture.store());
            project.setOwner(fixture.marketer());
            project.setCustomer(customer);
            project.setProperty(property);
            project.setQuote(quote);
            project.setDesigner(fixture.marketer());
            project.setManager(fixture.marketer());
            project.setSupport(fixture.finance());
            project.setSiteAddr(property.getAddress());
            project.setArea(new BigDecimal("90"));
            project.setContractAmount(new BigDecimal("100000"));
            project.setCost(new BigDecimal("80000"));
            project.setStatus(ProjectStatus.Ongoing);
            project.setProgress(10);
            project.setStartDate(LocalDate.now());
            project.setPlanEndDate(LocalDate.now().plusDays(90));
            return projectRepository.save(project);
        }));
    }

    private Fixture fixture() {
        return Objects.requireNonNull(transactionTemplate.execute(status -> {
            long seq = sequence.incrementAndGet();
            Company company = new Company();
            company.setTitle("费用测试企业" + seq);
            company.setName("费用测试企业");
            company.setSize(Size.A);
            company = companyRepository.save(company);

            Store store = new Store();
            store.setCompany(company);
            store.setName("费用测试门店");
            store.setDefaultStore(true);
            store = storeRepository.save(store);

            Employee boss = employee(company, store, EmployeeRole.Boss, "费用负责人", seq, 1);
            Employee manager = employee(company, store, EmployeeRole.StoreManager, "费用店长", seq, 2);
            Employee finance = employee(company, store, EmployeeRole.Finance, "费用财务", seq, 3);
            Employee marketer = employee(company, store, EmployeeRole.Marketer, "费用业务员", seq, 4);
            Employee other = employee(company, store, EmployeeRole.Marketer, "其他业务员", seq, 5);
            boss = employeeRepository.save(boss);
            manager = employeeRepository.save(manager);
            finance = employeeRepository.save(finance);
            marketer = employeeRepository.save(marketer);
            other = employeeRepository.save(other);
            return new Fixture(company, store, boss, manager, finance, marketer, other);
        }));
    }

    private Employee employee(Company company, Store store, EmployeeRole role,
                              String name, long seq, int offset) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setStatus(EmployeeStatus.Active);
        employee.setName(name);
        employee.setTelNo("17" + String.format("%09d", Math.abs((seq + offset) % 1_000_000_000L)));
        employee.setJoinedAt(LocalDateTime.now());
        return employee;
    }

    private record Fixture(Company company, Store store, Employee boss, Employee manager,
                           Employee finance, Employee marketer, Employee otherMarketer) { }
}

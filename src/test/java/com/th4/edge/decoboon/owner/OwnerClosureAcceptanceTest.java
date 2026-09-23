package com.th4.edge.decoboon.owner;

import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackKind;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus;
import com.th4.edge.decoboon.customer.configure.CustomerFeedbackType;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.configure.ReceivableStatus;
import com.th4.edge.decoboon.finance.configure.ReceivableType;
import com.th4.edge.decoboon.finance.configure.SettlementStatus;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.model.Receivable;
import com.th4.edge.decoboon.finance.model.Settlement;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.finance.repository.SettlementRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.AcceptanceRectificationType;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.AcceptanceRound;
import com.th4.edge.decoboon.project.model.ContractWorkbench;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.AcceptanceRoundRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectStageRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteMode;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.CredType;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.InviteStatus;
import com.th4.edge.tenant.user.configure.SessionType;
import com.th4.edge.tenant.user.configure.Size;
import com.th4.edge.tenant.user.configure.UserStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 业主闭环 P0 独立验收：邀请、身份授权、owner-only 读取和验收状态机。
 *
 * <p>本测试调用实际 Provider/Context 实现，不使用开发自测生成的会话状态。H2 用于业务边界和状态机验证；
 * MySQL 并发锁行为不在本测试的证明范围内。</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:owner-closure-acceptance;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
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
class OwnerClosureAcceptanceTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CredentialsRepository credentialsRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private PropertyRepository propertyRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private ProjectStageRepository stageRepository;
    @Autowired private ProjectTaskRepository taskRepository;
    @Autowired private AcceptanceRepository acceptanceRepository;
    @Autowired private AcceptanceRoundRepository acceptanceRoundRepository;
    @Autowired private ReceivableRepository receivableRepository;
    @Autowired private PayableRepository payableRepository;
    @Autowired private SettlementRepository settlementRepository;
    @Autowired private ProjectReportRepository reportRepository;
    @Autowired private ProjectContractRepository contractRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private CustomerInviteRepository inviteRepository;
    @Autowired private CustomerAccessRepository accessRepository;
    @Autowired private CustomerAccessContext accessContext;
    @Autowired private ProjectContext projectContext;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private com.th4.edge.decoboon.project.service.acceptances.round.page.Page acceptanceRoundPage;
    @Autowired private com.th4.edge.decoboon.project.service.acceptances.update.Update projectAcceptanceUpdate;

    @Autowired private com.th4.edge.decoboon.customer.service.invite.save.Save inviteSave;
    @Autowired private com.th4.edge.decoboon.customer.service.invite.info.Info inviteInfo;
    @Autowired private com.th4.edge.decoboon.customer.service.invite.accept.Accept inviteAccept;
    @Autowired private com.th4.edge.decoboon.customer.service.invite.cancel.Cancel inviteCancel;
    @Autowired private com.th4.edge.decoboon.customer.service.project.page.Page ownerProjectPage;
    @Autowired private com.th4.edge.decoboon.customer.service.project.info.Info ownerProjectInfo;
    @Autowired private com.th4.edge.decoboon.customer.service.property.info.Info ownerPropertyInfo;
    @Autowired private com.th4.edge.decoboon.customer.service.stage.list.List ownerStageList;
    @Autowired private com.th4.edge.decoboon.customer.service.report.page.Page ownerReportPage;
    @Autowired private com.th4.edge.decoboon.customer.service.acceptance.page.Page ownerAcceptancePage;
    @Autowired private com.th4.edge.decoboon.customer.service.acceptance.info.Info ownerAcceptanceInfo;
    @Autowired private com.th4.edge.decoboon.customer.service.acceptance.update.Update ownerAcceptanceUpdate;
    @Autowired private com.th4.edge.decoboon.customer.service.acceptance.round.page.Page ownerAcceptanceRoundPage;
    @Autowired private com.th4.edge.decoboon.customer.service.contract.info.Info ownerContractInfo;
    @Autowired private com.th4.edge.decoboon.customer.service.contract.workbench.Workbench ownerContractWorkbench;
    @Autowired private com.th4.edge.decoboon.customer.service.contractlines.page.Page ownerContractLinePage;
    @Autowired private com.th4.edge.decoboon.customer.service.feedback.save.Save ownerFeedbackSave;
    @Autowired private com.th4.edge.decoboon.customer.service.feedback.page.Page ownerFeedbackPage;
    @Autowired private com.th4.edge.decoboon.customer.service.feedback.close.Close ownerFeedbackClose;
    @Autowired private com.th4.edge.decoboon.project.service.feedback.page.Page projectFeedbackPage;
    @Autowired private com.th4.edge.decoboon.project.service.feedback.update.Update projectFeedbackUpdate;
    @Autowired private com.th4.edge.decoboon.customer.service.project.journey.info.Info ownerJourneyInfo;
    @Autowired private com.th4.edge.tenant.user.service.auth.session.info.Info sessionInfo;

    @BeforeEach
    void unwrapProviderProxies() {
        inviteSave = AopTestUtils.getTargetObject(inviteSave);
        inviteInfo = AopTestUtils.getTargetObject(inviteInfo);
        inviteAccept = AopTestUtils.getTargetObject(inviteAccept);
        inviteCancel = AopTestUtils.getTargetObject(inviteCancel);
        ownerProjectPage = AopTestUtils.getTargetObject(ownerProjectPage);
        ownerProjectInfo = AopTestUtils.getTargetObject(ownerProjectInfo);
        ownerPropertyInfo = AopTestUtils.getTargetObject(ownerPropertyInfo);
        ownerStageList = AopTestUtils.getTargetObject(ownerStageList);
        ownerReportPage = AopTestUtils.getTargetObject(ownerReportPage);
        ownerAcceptancePage = AopTestUtils.getTargetObject(ownerAcceptancePage);
        ownerAcceptanceInfo = AopTestUtils.getTargetObject(ownerAcceptanceInfo);
        ownerAcceptanceUpdate = AopTestUtils.getTargetObject(ownerAcceptanceUpdate);
        ownerAcceptanceRoundPage = AopTestUtils.getTargetObject(ownerAcceptanceRoundPage);
        ownerContractInfo = AopTestUtils.getTargetObject(ownerContractInfo);
        ownerContractWorkbench = AopTestUtils.getTargetObject(ownerContractWorkbench);
        ownerContractLinePage = AopTestUtils.getTargetObject(ownerContractLinePage);
        ownerFeedbackSave = AopTestUtils.getTargetObject(ownerFeedbackSave);
        ownerFeedbackPage = AopTestUtils.getTargetObject(ownerFeedbackPage);
        ownerFeedbackClose = AopTestUtils.getTargetObject(ownerFeedbackClose);
        projectFeedbackPage = AopTestUtils.getTargetObject(projectFeedbackPage);
        projectFeedbackUpdate = AopTestUtils.getTargetObject(projectFeedbackUpdate);
        ownerJourneyInfo = AopTestUtils.getTargetObject(ownerJourneyInfo);
        sessionInfo = AopTestUtils.getTargetObject(sessionInfo);
        acceptanceRoundPage = AopTestUtils.getTargetObject(acceptanceRoundPage);
        projectAcceptanceUpdate = AopTestUtils.getTargetObject(projectAcceptanceUpdate);
    }

    @Test
    void shouldRegisterOwnerRoutesAndSeparateEmployeePrincipal() {
        assertThat(inviteAccept).isNotNull();
        assertThat(ownerProjectInfo).isNotNull();
        Fixture fixture = createFixture("\u52a0\u5bc6");

        setField(ownerProjectInfo, "principal", fixture.bossA());
        setField(ownerProjectInfo, "projectId", fixture.projectA1().getProjectId());
        assertDefined(() -> ownerProjectInfo.call(new Project()), "403");
    }

    @Test
    void shouldQueryInviteByTokenAndRejectWrongPhoneWithoutLeakingFullPhone() {
        Fixture fixture = createFixture("invite-info");
        CustomerInvite invite = createInvite(fixture, fixture.bossA(), fixture.projectA1(),
                fixture.customerA1().getName(), "13800138000");

        setField(inviteInfo, "token", invite.getToken());
        setField(inviteInfo, "inviteId", null);
        setField(inviteInfo, "phone", null);
        CustomerInvite found = inviteInfo.call(new CustomerInvite());
        assertThat(found.getMaskedTelNo()).isEqualTo("138****8000");
        assertThat(found.getTelNo()).isEqualTo("13800138000");

        setField(inviteInfo, "token", null);
        setField(inviteInfo, "inviteId", invite.getInviteId());
        setField(inviteInfo, "phone", "13800138000");
        assertThat(inviteInfo.call(new CustomerInvite()).getInviteId()).isEqualTo(invite.getInviteId());

        setField(inviteInfo, "token", null);
        setField(inviteInfo, "inviteId", invite.getInviteId());
        setField(inviteInfo, "phone", "13900139000");
        assertDefined(() -> inviteInfo.call(new CustomerInvite()), "404");

        setField(inviteInfo, "token", null);
        setField(inviteInfo, "inviteId", invite.getInviteId());
        setField(inviteInfo, "phone", null);
        assertDefined(() -> inviteInfo.call(new CustomerInvite()), "404");
    }

    @Test
    void shouldAcceptInviteOnceAndRejectReplayAndPhoneMismatch() {
        Fixture fixture = createFixture("invite-accept");
        Credentials owner = credential("wx-owner-accept", "13800138111");
        CustomerInvite invite = createInvite(fixture, fixture.bossA(), fixture.projectA1(),
                "业主甲", owner.getPhone());

        setField(inviteAccept, "principal", owner);
        setField(inviteAccept, "inviteId", invite.getInviteId());
        setField(inviteAccept, "token", invite.getToken());
        Credentials accepted = (Credentials) inviteAccept.call(new Credentials());

        assertThat(accepted.getSessionType()).isEqualTo(SessionType.Customer);
        assertThat(accepted.getCustomerAccesses()).extracting(CustomerAccess::getProject)
                .extracting(Project::getProjectId).containsExactly(fixture.projectA1().getProjectId());
        assertThat(inviteRepository.findById(invite.getInviteId()).orElseThrow().getStatus())
                .isEqualTo(InviteStatus.Accepted);
        assertThat(accessContext.activeAccesses(owner)).hasSize(1);

        setField(inviteAccept, "principal", owner);
        setField(inviteAccept, "inviteId", invite.getInviteId());
        setField(inviteAccept, "token", invite.getToken());
        assertDefined(() -> inviteAccept.call(new Credentials()), "400");
        assertThat(accessContext.activeAccesses(owner)).hasSize(1);

        Credentials wrongPhone = credential("wx-owner-wrong-phone", "13900139111");
        CustomerInvite mismatch = createInvite(fixture, fixture.bossA(), fixture.projectA1(),
                "业主乙", "13800138222");
        setField(inviteAccept, "principal", wrongPhone);
        setField(inviteAccept, "inviteId", mismatch.getInviteId());
        setField(inviteAccept, "token", mismatch.getToken());
        assertDefined(() -> inviteAccept.call(new Credentials()), "400");
        assertThat(accessContext.activeAccesses(wrongPhone)).isEmpty();
    }

    @Test
    void shouldRejectExpiredAndCancelledInvitesWithoutGrantingAccess() {
        Fixture fixture = createFixture("invite-state");
        Credentials owner = credential("wx-owner-state", "13800138333");
        CustomerInvite expired = createInviteRecord(fixture, fixture.projectA1(), owner.getPhone(), "x-state-expired");
        expired.setExpireTime(LocalDateTime.now().minusMinutes(1));
        inviteRepository.save(expired);

        setField(inviteAccept, "principal", owner);
        setField(inviteAccept, "inviteId", expired.getInviteId());
        setField(inviteAccept, "token", expired.getToken());
        assertDefined(() -> inviteAccept.call(new Credentials()), "400");
        assertThat(inviteRepository.findById(expired.getInviteId()).orElseThrow().getStatus())
                .isEqualTo(InviteStatus.Expired);
        assertThat(accessContext.activeAccesses(owner)).isEmpty();

        CustomerInvite cancelled = createInvite(fixture, fixture.bossA(), fixture.projectA2(),
                "业主丙", owner.getPhone());
        setField(inviteCancel, "employee", fixture.bossB());
        setField(inviteCancel, "inviteId", cancelled.getInviteId());
        assertDefined(() -> inviteCancel.call(new CustomerInvite()), "404");
        assertThat(inviteRepository.findById(cancelled.getInviteId()).orElseThrow().getStatus())
                .isEqualTo(InviteStatus.Pending);

        setField(inviteCancel, "employee", fixture.bossA());
        setField(inviteCancel, "inviteId", cancelled.getInviteId());
        CustomerInvite cancelledResult = inviteCancel.call(new CustomerInvite());
        assertThat(cancelledResult.getStatus()).isEqualTo(InviteStatus.Cancelled);

        setField(inviteAccept, "principal", owner);
        setField(inviteAccept, "inviteId", cancelled.getInviteId());
        setField(inviteAccept, "token", cancelled.getToken());
        assertDefined(() -> inviteAccept.call(new Credentials()), "400");
        assertThat(accessContext.activeAccesses(owner)).isEmpty();
    }

    @Test
    void shouldSupportMultipleProjectsAndExplicitMultipleOwners() {
        Fixture fixture = createFixture("invite-multi");
        Credentials owner1 = credential("wx-owner-multi-1", "13800138441");
        Credentials owner2 = credential("wx-owner-multi-2", "13800138442");

        accept(fixture, owner1, fixture.projectA1(), "业主一");
        accept(fixture, owner1, fixture.projectA2(), "业主一");
        accept(fixture, owner2, fixture.projectA1(), "共同业主");

        assertThat(accessContext.activeAccesses(owner1)).extracting(CustomerAccess::getProject)
                .extracting(Project::getProjectId)
                .containsExactlyInAnyOrder(fixture.projectA1().getProjectId(), fixture.projectA2().getProjectId());
        assertThat(accessRepository.findByCredential_CreIdAndProject_ProjectId(
                owner1.getCreId(), fixture.projectA1().getProjectId())).isPresent();
        assertThat(accessRepository.findByCredential_CreIdAndProject_ProjectId(
                owner2.getCreId(), fixture.projectA1().getProjectId())).isPresent();

        setField(ownerProjectPage, "principal", owner1);
        setField(ownerProjectPage, "current", 1);
        setField(ownerProjectPage, "size", 20);
        Page<Project> page = ownerProjectPage.call(new Project());
        assertThat(page.getContent()).extracting(Project::getProjectId)
                .containsExactlyInAnyOrder(fixture.projectA1().getProjectId(), fixture.projectA2().getProjectId());
    }

    @Test
    void shouldIsolateOwnerReadsAcrossProjectsAndCompanies() {
        Fixture fixture = createFixture("read-isolation");
        Credentials owner = credential("wx-owner-read", "13800138555");
        accept(fixture, owner, fixture.projectA1(), "业主读取");

        setField(ownerProjectPage, "principal", owner);
        setField(ownerProjectPage, "current", 1);
        setField(ownerProjectPage, "size", 20);
        assertThat(ownerProjectPage.call(new Project()).getContent())
                .extracting(Project::getProjectId).containsExactly(fixture.projectA1().getProjectId());

        setField(ownerProjectInfo, "principal", owner);
        setField(ownerProjectInfo, "projectId", fixture.projectA1().getProjectId());
        assertThat(ownerProjectInfo.call(new Project()).getProjectId()).isEqualTo(fixture.projectA1().getProjectId());

        assertOwnerNotFound(owner, ownerProjectInfo, "projectId", fixture.projectA2().getProjectId(),
                () -> ownerProjectInfo.call(new Project()));
        assertOwnerNotFound(owner, ownerProjectInfo, "projectId", fixture.projectB1().getProjectId(),
                () -> ownerProjectInfo.call(new Project()));
        assertOwnerNotFound(owner, ownerPropertyInfo, "projectId", fixture.projectA2().getProjectId(),
                () -> ownerPropertyInfo.call(new Property()));
        assertOwnerNotFound(owner, ownerStageList, "projectId", fixture.projectA2().getProjectId(),
                () -> ownerStageList.call(new ProjectStage()));
        assertOwnerNotFound(owner, ownerReportPage, "projectId", fixture.projectA2().getProjectId(),
                () -> ownerReportPage.call(new ProjectReport()));
        assertOwnerNotFound(owner, ownerAcceptancePage, "projectId", fixture.projectA2().getProjectId(),
                () -> ownerAcceptancePage.call(new Acceptance()));
        assertOwnerNotFound(owner, ownerContractInfo, "projectId", fixture.projectA2().getProjectId(),
                () -> ownerContractInfo.call(new ProjectContract()));

        setField(ownerPropertyInfo, "principal", owner);
        setField(ownerPropertyInfo, "projectId", fixture.projectA1().getProjectId());
        assertThat(ownerPropertyInfo.call(new Property()).getPropertyId())
                .isEqualTo(fixture.projectA1().getProperty().getPropertyId());
    }

    @Test
    void shouldFilterCustomerReportsAndKeepContractSummaryAllowListMinimal() {
        Fixture fixture = createFixture("read-fields");
        Credentials owner = credential("wx-owner-fields", "13800138666");
        accept(fixture, owner, fixture.projectA1(), "业主字段");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        report(fixture.projectA1(), stage, "公开动态", true);
        report(fixture.projectA1(), stage, "内部动态", false);
        report(fixture.projectA2(), null, "其他项目动态", true);
        ProjectContract contract = createContract(fixture.projectA1(), fixture.marketerA(), "C-summary");

        setField(ownerReportPage, "principal", owner);
        setField(ownerReportPage, "projectId", fixture.projectA1().getProjectId());
        setField(ownerReportPage, "current", 1);
        setField(ownerReportPage, "size", 20);
        Page<ProjectReport> reports = ownerReportPage.call(new ProjectReport());
        assertThat(reports.getContent()).extracting(ProjectReport::getTitle).containsExactly("公开动态");

        setField(ownerContractInfo, "principal", owner);
        setField(ownerContractInfo, "projectId", fixture.projectA1().getProjectId());
        ProjectContract returned = ownerContractInfo.call(new ProjectContract());
        assertThat(returned.getContractId()).isEqualTo(contract.getContractId());

        Properties properties = com.th4.edge.decoboon.customer.service.contract.info.support.Info.class
                .getAnnotation(Properties.class);
        Set<String> allowed = Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(ProjectContract.class))
                .flatMap(item -> Arrays.stream(item.properties()))
                .collect(Collectors.toSet());
        assertThat(allowed).contains("contractId", "no", "currentAmount", "status", "paymentTerms");
        assertThat(allowed).doesNotContain("quote", "cost", "remark", "createdBy", "receivableAmount", "receivedAmount");
    }

    @Test
    void shouldExposeOwnerContractWorkbenchWithoutCostAndRejectUnauthorizedContract() {
        Fixture fixture = createFixture("owner-contract-workbench");
        Credentials owner = credential("wx-owner-contract-workbench", "13800138688");
        accept(fixture, owner, fixture.projectA1(), "合同工作台业主");
        ProjectContract contract = createContract(fixture.projectA1(), fixture.marketerA(), "C-owner-workbench");
        ProjectContract otherContract = createContract(fixture.projectA2(), fixture.marketerA(), "C-owner-other");

        setField(ownerContractWorkbench, "principal", owner);
        setField(ownerContractWorkbench, "projectId", fixture.projectA1().getProjectId());
        setField(ownerContractWorkbench, "contractId", null);
        ContractWorkbench view = ownerContractWorkbench.call(new ProjectContract());
        assertThat(view.getContract().getContractId()).isEqualTo(contract.getContractId());
        assertThat(view.getCost()).isNull();

        setField(ownerContractWorkbench, "projectId", null);
        setField(ownerContractWorkbench, "contractId", contract.getContractId());
        assertThat(ownerContractWorkbench.call(new ProjectContract()).getContract().getContractId())
                .isEqualTo(contract.getContractId());

        Properties properties = com.th4.edge.decoboon.customer.service.contract.workbench.support.Workbench.class
                .getAnnotation(Properties.class);
        Set<String> workbenchAllowed = Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(ContractWorkbench.class))
                .flatMap(item -> Arrays.stream(item.properties()))
                .collect(Collectors.toSet());
        Set<String> contractAllowed = Arrays.stream(properties.allow())
                .filter(item -> item.model().equals(ProjectContract.class))
                .flatMap(item -> Arrays.stream(item.properties()))
                .collect(Collectors.toSet());
        assertThat(workbenchAllowed).doesNotContain("cost");
        assertThat(contractAllowed).doesNotContain("signedCost", "quote", "createdBy", "remark");

        assertOwnerNotFound(owner, ownerContractWorkbench, "contractId", otherContract.getContractId(),
                () -> ownerContractWorkbench.call(new ProjectContract()));

        setField(ownerContractLinePage, "principal", owner);
        setField(ownerContractLinePage, "projectId", null);
        setField(ownerContractLinePage, "contractId", contract.getContractId());
        assertThat(ownerContractLinePage.call(new com.th4.edge.decoboon.project.model.ContractLine()).getContent())
                .isEmpty();
        setField(ownerContractLinePage, "contractId", otherContract.getContractId());
        assertDefined(() -> ownerContractLinePage.call(new com.th4.edge.decoboon.project.model.ContractLine()), "404");

        setField(ownerContractWorkbench, "principal", fixture.bossA());
        setField(ownerContractWorkbench, "contractId", contract.getContractId());
        assertDefined(() -> ownerContractWorkbench.call(new ProjectContract()), "403");
    }

    @Test
    void shouldReturnOwnerAcceptanceRoundsAndRejectUnauthorizedAcceptance() {
        Fixture fixture = createFixture("owner-acceptance-rounds");
        Credentials owner = credential("wx-owner-acceptance-rounds", "13800138689");
        accept(fixture, owner, fixture.projectA1(), "轮次时间线业主");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance acceptance = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "轮次提交", List.of(), List.of("round-site.jpg"));
        Acceptance rejected = decide(owner, acceptance.getAcceptanceId(), false, "轮次驳回");

        setField(ownerAcceptanceRoundPage, "principal", owner);
        setField(ownerAcceptanceRoundPage, "acceptanceId", rejected.getAcceptanceId());
        List<AcceptanceRound> rounds = ownerAcceptanceRoundPage.call(new Acceptance());
        assertThat(rounds).hasSize(1);
        assertThat(rounds.get(0).getRectificationType()).isEqualTo(AcceptanceRectificationType.Construction);
        assertThat(rounds.get(0).getRectificationDueDate()).isEqualTo(LocalDate.now().plusDays(3));

        Acceptance otherAcceptance = acceptance(fixture.projectA2(), createStageAndTask(
                fixture.projectA2(), fixture.marketerA()), fixture.marketerA());
        setField(ownerAcceptanceRoundPage, "acceptanceId", otherAcceptance.getAcceptanceId());
        assertDefined(() -> ownerAcceptanceRoundPage.call(new Acceptance()), "404");

        setField(ownerAcceptanceRoundPage, "principal", fixture.marketerA());
        setField(ownerAcceptanceRoundPage, "acceptanceId", rejected.getAcceptanceId());
        assertDefined(() -> ownerAcceptanceRoundPage.call(new Acceptance()), "403");
    }

    @Test
    void shouldPersistOwnerRectificationImagesOnAuthorizedAcceptance() {
        Fixture fixture = createFixture("owner-rectification-images");
        Credentials owner = credential("wx-owner-rectification-images", "13800138690");
        accept(fixture, owner, fixture.projectA1(), "整改图片业主");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance acceptance = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "图片验收提交", List.of(), List.of("rectification-site.jpg"));

        setField(ownerAcceptanceUpdate, "principal", owner);
        setField(ownerAcceptanceUpdate, "acceptanceId", acceptance.getAcceptanceId());
        setField(ownerAcceptanceUpdate, "confirmed", false);
        setField(ownerAcceptanceUpdate, "conclusion", "提交整改图片");
        setField(ownerAcceptanceUpdate, "issues", List.of("墙面收口不平"));
        setField(ownerAcceptanceUpdate, "rectificationType", AcceptanceRectificationType.Construction);
        setField(ownerAcceptanceUpdate, "rectificationIssueImages", List.of("issue-1.jpg", "issue-2.jpg"));

        Acceptance rejected = ownerAcceptanceUpdate.call(new Acceptance());
        assertThat(rejected.getRectificationIssueImages()).containsExactly("issue-1.jpg", "issue-2.jpg");
        AcceptanceRound round = acceptanceRoundRepository.findByAcceptanceOrderByRoundNoDesc(rejected).get(0);
        assertThat(round.getRectificationIssueImages()).containsExactly("issue-1.jpg", "issue-2.jpg");
    }

    @Test
    void shouldCloseOwnerFeedbackWithEmployeeResponseAndExposeJourney() {
        Fixture fixture = createFixture("owner-feedback");
        Credentials owner = credential("wx-owner-feedback", "13800138691");
        accept(fixture, owner, fixture.projectA1(), "意见投诉业主");

        setField(ownerFeedbackSave, "principal", owner);
        setField(ownerFeedbackSave, "projectId", fixture.projectA1().getProjectId());
        setField(ownerFeedbackSave, "kind", CustomerFeedbackKind.Complaint);
        setField(ownerFeedbackSave, "type", CustomerFeedbackType.Service);
        setField(ownerFeedbackSave, "title", "现场无人沟通");
        setField(ownerFeedbackSave, "content", "本周多次联系项目经理未得到回复");
        setField(ownerFeedbackSave, "images", List.of("feedback-1.jpg"));
        CustomerFeedback feedback = ownerFeedbackSave.call(new CustomerFeedback());
        assertThat(feedback.getStatus()).isEqualTo(CustomerFeedbackStatus.Submitted);
        assertThat(feedback.getHandler().getEmpId()).isEqualTo(fixture.projectA1().getManager().getEmpId());
        assertThat(feedback.getDueAt()).isNotNull();

        setField(ownerFeedbackPage, "principal", owner);
        setField(ownerFeedbackPage, "projectId", fixture.projectA1().getProjectId());
        assertThat(ownerFeedbackPage.call(new CustomerFeedback())).extracting(CustomerFeedback::getFeedbackId)
                .containsExactly(feedback.getFeedbackId());

        setField(projectFeedbackPage, "employee", fixture.projectA1().getManager());
        setField(projectFeedbackPage, "projectId", fixture.projectA1().getProjectId());
        assertThat(projectFeedbackPage.call(new CustomerFeedback())).extracting(CustomerFeedback::getFeedbackId)
                .containsExactly(feedback.getFeedbackId());

        setField(projectFeedbackUpdate, "employee", fixture.projectA1().getManager());
        setField(projectFeedbackUpdate, "feedbackId", feedback.getFeedbackId());
        setField(projectFeedbackUpdate, "status", CustomerFeedbackStatus.Resolved);
        setField(projectFeedbackUpdate, "response", "已联系业主并说明处理计划");
        CustomerFeedback resolved = projectFeedbackUpdate.call(new CustomerFeedback());
        assertThat(resolved.getStatus()).isEqualTo(CustomerFeedbackStatus.Resolved);
        assertThat(resolved.getResponse()).contains("处理计划");

        setField(ownerFeedbackClose, "principal", owner);
        setField(ownerFeedbackClose, "feedbackId", feedback.getFeedbackId());
        setField(ownerFeedbackClose, "satisfaction", 5);
        CustomerFeedback closed = ownerFeedbackClose.call(new CustomerFeedback());
        assertThat(closed.getStatus()).isEqualTo(CustomerFeedbackStatus.Closed);
        assertThat(closed.getSatisfaction()).isEqualTo(5);

        setField(ownerJourneyInfo, "principal", owner);
        setField(ownerJourneyInfo, "projectId", fixture.projectA1().getProjectId());
        var journey = ownerJourneyInfo.call(new Project());
        assertThat(journey.getProjectId()).isEqualTo(fixture.projectA1().getProjectId());
        assertThat(journey.getSections()).extracting(item -> item.getKey())
                .contains("measure", "design", "quote", "contract", "change", "material", "construction",
                        "acceptance", "payment", "feedback");
        assertThat(journey.getSections().stream().filter(item -> "quote".equals(item.getKey()))
                .findFirst().orElseThrow().getItems()).isNotEmpty();
        assertThat(journey.getOpenFeedbackCount()).isZero();
    }

    @Test
    void shouldConfirmAcceptanceIdempotentlyAndRejectConflictingDecision() {
        Fixture fixture = createFixture("accept-confirm");
        Credentials owner = credential("wx-owner-confirm", "13800138777");
        accept(fixture, owner, fixture.projectA1(), "业主确认");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance acceptance = acceptance(fixture.projectA1(), stage, fixture.marketerA());

        Acceptance confirmed = decide(owner, acceptance.getAcceptanceId(), true, "同意");
        assertThat(confirmed.getStatus()).isEqualTo(AcceptStatus.Confirmed);
        assertThat(confirmed.getConfirmedBy().getCreId()).isEqualTo(owner.getCreId());
        assertThat(confirmed.getConfirmedAt()).isNotNull();
        assertThat(stageRepository.findById(stage.getStageId()).orElseThrow().getStatus()).isEqualTo(StageStatus.Done);
        assertThat(projectRepository.findById(fixture.projectA1().getProjectId()).orElseThrow().getProgress()).isEqualTo(100);
        LocalDateTime firstConfirmedAt = confirmed.getConfirmedAt();

        Acceptance repeated = decide(owner, acceptance.getAcceptanceId(), true, "再次确认");
        assertThat(repeated.getStatus()).isEqualTo(AcceptStatus.Confirmed);
        assertThat(repeated.getConfirmedAt()).isEqualTo(firstConfirmedAt);
        assertDefined(() -> decide(owner, acceptance.getAcceptanceId(), false, "改为驳回"), "400");
    }

    @Test
    void shouldRejectThenAllowNewSubmissionAndKeepStageOrderingRule() {
        Fixture fixture = createFixture("accept-reject");
        Credentials owner = credential("wx-owner-reject", "13800138888");
        accept(fixture, owner, fixture.projectA1(), "业主驳回");
        Employee manager = employee(fixture.companyA(), fixture.storeA(), EmployeeRole.Pm, "整改项目经理");
        fixture.projectA1().setManager(manager);
        projectRepository.save(fixture.projectA1());
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance first = acceptance(fixture.projectA1(), stage, fixture.marketerA());

        Acceptance rejected = decide(owner, first.getAcceptanceId(), false, "整改");
        assertThat(rejected.getStatus()).isEqualTo(AcceptStatus.Rejected);
        assertThat(rejected.getRectificationOwner().getEmpId()).isEqualTo(manager.getEmpId());
        assertThat(stageRepository.findById(stage.getStageId()).orElseThrow().getStatus()).isEqualTo(StageStatus.Doing);

        Acceptance resubmitted = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "整改完成", List.of(), List.of("https://example.test/reaccept.jpg"));
        assertThat(resubmitted.getStatus()).isEqualTo(AcceptStatus.Pending);
        assertThat(stageRepository.findById(stage.getStageId()).orElseThrow().getStatus()).isEqualTo(StageStatus.Accepting);

        Acceptance confirmed = decide(owner, resubmitted.getAcceptanceId(), true, "整改确认");
        assertThat(confirmed.getStatus()).isEqualTo(AcceptStatus.Confirmed);
        assertThat(stageRepository.findById(stage.getStageId()).orElseThrow().getStatus()).isEqualTo(StageStatus.Done);
    }

    @Test
    void shouldBlockOwnerRejectionWhenProjectManagerIsMissing() {
        Fixture fixture = createFixture("reject-without-manager");
        Credentials owner = credential("wx-reject-without-manager", "13800138904");
        accept(fixture, owner, fixture.projectA1(), "无项目经理整改");
        fixture.projectA1().setManager(null);
        projectRepository.save(fixture.projectA1());
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance acceptance = acceptance(fixture.projectA1(), stage, fixture.marketerA());

        assertDefined(() -> decide(owner, acceptance.getAcceptanceId(), false, "缺少项目经理",
                List.of("瓷砖空鼓")), "400");
    }

    @Test
    void shouldRejectEmployeeImpersonationOnBothAcceptanceDecisionRoutes() {
        Fixture fixture = createFixture("accept-employee-decision");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance acceptance = acceptance(fixture.projectA1(), stage, fixture.marketerA());

        setField(projectAcceptanceUpdate, "employee", fixture.marketerA());
        setField(projectAcceptanceUpdate, "acceptanceId", acceptance.getAcceptanceId());
        setField(projectAcceptanceUpdate, "confirmed", true);
        assertDefined(() -> projectAcceptanceUpdate.call(new Acceptance()), "403");

        setField(ownerAcceptanceUpdate, "principal", fixture.marketerA());
        setField(ownerAcceptanceUpdate, "acceptanceId", acceptance.getAcceptanceId());
        setField(ownerAcceptanceUpdate, "confirmed", true);
        assertDefined(() -> ownerAcceptanceUpdate.call(new Acceptance()), "403");
    }

    @Test
    void shouldReturnExistingPendingAcceptanceOnDuplicateSubmission() {
        Fixture fixture = createFixture("duplicate-pending");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());

        Acceptance first = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "首次提交", List.of(), List.of("duplicate-site-1.jpg"));
        AcceptanceRound firstRound = acceptanceRoundRepository
                .findByAcceptanceOrderByRoundNoDesc(first).get(0);
        RoundSnapshot before = snapshot(firstRound);

        Acceptance repeated = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "重复点击", List.of(), List.of("duplicate-site-2.jpg"));

        assertThat(repeated.getAcceptanceId()).isEqualTo(first.getAcceptanceId());
        assertThat(acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(
                fixture.projectA1().getProjectId())).hasSize(1);
        List<AcceptanceRound> rounds = acceptanceRoundRepository.findByAcceptanceOrderByRoundNoDesc(first);
        assertThat(rounds).hasSize(1);
        assertThat(snapshot(rounds.get(0))).isEqualTo(before);
        assertThat(rounds.get(0).getEvidenceUrls()).containsExactly("duplicate-site-1.jpg");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldSerializeConcurrentFirstSubmissionWithoutUndefinedErrors() throws Exception {
        Fixture fixture = createFixture("concurrent-pending");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Acceptance> submit = () -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("concurrent start timeout");
                }
                return projectContext.submitAcceptance(
                        fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                        fixture.marketerA(), "并发提交", List.of(), List.of("concurrent-site.jpg"));
            };
            Future<Acceptance> left = pool.submit(submit);
            Future<Acceptance> right = pool.submit(submit);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            Acceptance leftResult = left.get(20, TimeUnit.SECONDS);
            Acceptance rightResult = right.get(20, TimeUnit.SECONDS);
            assertThat(leftResult.getAcceptanceId()).isEqualTo(rightResult.getAcceptanceId());
            assertThat(leftResult.getStatus()).isEqualTo(AcceptStatus.Pending);
            assertThat(rightResult.getStatus()).isEqualTo(AcceptStatus.Pending);

            List<Acceptance> acceptances = acceptanceRepository
                    .findByProject_ProjectIdOrderByAcceptanceIdDesc(fixture.projectA1().getProjectId());
            assertThat(acceptances).hasSize(1);
            assertThat(acceptanceRoundRepository.findByAcceptanceOrderByRoundNoDesc(acceptances.get(0)))
                    .singleElement()
                    .extracting(AcceptanceRound::getRoundNo)
                    .isEqualTo(1);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void shouldRequireCompleteRectificationRequirementsOnOwnerRejection() {
        Fixture fixture = createFixture("reject-requirements");
        Credentials owner = credential("wx-reject-requirements", "13800138903");
        accept(fixture, owner, fixture.projectA1(), "驳回校验业主");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance acceptance = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "首轮提交", List.of(), List.of("reject-requirements-site.jpg"));
        LocalDate today = LocalDate.now();

        assertDefined(() -> decide(owner, acceptance.getAcceptanceId(), false, "缺问题",
                List.of(), null), "400");
        assertDefined(() -> decide(owner, acceptance.getAcceptanceId(), false, "缺类型",
                List.of("瓷砖空鼓"), null), "400");

        Acceptance rejected = decide(owner, acceptance.getAcceptanceId(), false, "完整驳回",
                List.of("瓷砖空鼓"), AcceptanceRectificationType.Construction);
        AcceptanceRound round = acceptanceRoundRepository.findByAcceptanceOrderByRoundNoDesc(rejected).get(0);
        assertThat(rejected.getStatus()).isEqualTo(AcceptStatus.Rejected);
        assertThat(rejected.getIssues()).containsExactly("瓷砖空鼓");
        assertThat(rejected.getRectificationOwner().getEmpId()).isEqualTo(fixture.marketerA().getEmpId());
        assertThat(rejected.getRectificationType()).isEqualTo(AcceptanceRectificationType.Construction);
        assertThat(rejected.getRectificationDueDate()).isEqualTo(today.plusDays(3));
        assertThat(round.getIssues()).containsExactly("瓷砖空鼓");
        assertThat(round.getRectificationOwner().getEmpId()).isEqualTo(fixture.marketerA().getEmpId());
        assertThat(round.getRectificationType()).isEqualTo(AcceptanceRectificationType.Construction);
        assertThat(round.getRectificationDueDate()).isEqualTo(today.plusDays(3));

        ProjectStage designStage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance designAcceptance = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), designStage.getStageId(),
                fixture.marketerA(), "设计验收提交", List.of(), List.of("design-site.jpg"));
        Acceptance designRejected = decide(owner, designAcceptance.getAcceptanceId(), false, "设计整改",
                List.of("柜体颜色与确认方案不一致"), AcceptanceRectificationType.Design);
        assertThat(designRejected.getRectificationType()).isEqualTo(AcceptanceRectificationType.Design);
        assertThat(designRejected.getRectificationDueDate()).isEqualTo(today.plusDays(5));
    }

    @Test
    void shouldBackfillLegacyAcceptanceRoundsIdempotently() throws Exception {
        Fixture fixture = createFixture("legacy-backfill");
        ProjectStage pendingStage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance pending = acceptance(fixture.projectA1(), pendingStage, fixture.marketerA());

        ProjectStage rejectedStage = createStageAndTask(fixture.projectA2(), fixture.marketerA());
        Acceptance rejected = acceptance(fixture.projectA2(), rejectedStage, fixture.marketerA());
        rejected.setStatus(AcceptStatus.Rejected);
        rejected.setConclusion("遗留驳回");
        rejected.setIssues(new ArrayList<>(List.of("遗留问题")));
        rejected.setEvidenceUrls(new ArrayList<>(List.of("legacy-evidence.jpg")));
        rejected.setRectificationOwner(fixture.marketerA());
        rejected.setRectificationDueDate(LocalDate.now().plusDays(5));
        final Acceptance rejectedSaved = acceptanceRepository.saveAndFlush(rejected);

        executeRectificationBackfill();
        executeRectificationBackfill();

        List<AcceptanceRound> pendingRounds = acceptanceRoundRepository
                .findByAcceptanceOrderByRoundNoDesc(pending);
        List<AcceptanceRound> rejectedRounds = acceptanceRoundRepository
                .findByAcceptanceOrderByRoundNoDesc(rejectedSaved);
        assertThat(pendingRounds).singleElement()
                .satisfies(round -> {
                    assertThat(round.getRoundId()).isEqualTo(pending.getAcceptanceId());
                    assertThat(round.getRoundNo()).isEqualTo(1);
                    assertThat(round.getResult()).isEqualTo(AcceptStatus.Pending);
                    assertThat(round.getEvidenceUrls()).containsExactly("https://example.test/acceptance.jpg");
                });
        assertThat(rejectedRounds).singleElement()
                .satisfies(round -> {
                    assertThat(round.getRoundId()).isEqualTo(rejectedSaved.getAcceptanceId());
                    assertThat(round.getRoundNo()).isEqualTo(1);
                    assertThat(round.getResult()).isEqualTo(AcceptStatus.Rejected);
                    assertThat(round.getIssues()).containsExactly("遗留问题");
                    assertThat(round.getEvidenceUrls()).containsExactly("legacy-evidence.jpg");
                    assertThat(round.getRectificationOwner().getEmpId()).isEqualTo(fixture.marketerA().getEmpId());
                    assertThat(round.getRectificationDueDate()).isEqualTo(rejectedSaved.getRectificationDueDate());
                });
    }

    @Test
    void shouldHideAcceptanceRoundsFromUnrelatedEmployee() {
        Fixture fixture = createFixture("round-scope");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance acceptance = acceptance(fixture.projectA1(), stage, fixture.marketerA());
        Employee unrelated = employee(fixture.companyA(), fixture.storeA(), EmployeeRole.Designer, "无关设计师");
        Employee otherCompanyBoss = fixture.bossB();

        setField(acceptanceRoundPage, "employee", unrelated);
        setField(acceptanceRoundPage, "acceptanceId", acceptance.getAcceptanceId());
        assertDefined(() -> acceptanceRoundPage.call(new Acceptance()), "404");

        setField(acceptanceRoundPage, "employee", otherCompanyBoss);
        assertDefined(() -> acceptanceRoundPage.call(new Acceptance()), "404");
    }

    @Test
    void shouldAppendRectificationRoundsAndPreserveHistoryAndFinanceFacts() {
        Fixture fixture = createFixture("rectify-rounds");
        Credentials owner = credential("wx-owner-rounds", "13800138901");
        accept(fixture, owner, fixture.projectA1(), "整改多轮业主");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        createFinanceFacts(fixture);
        FinanceFacts before = financeFacts(fixture.projectA1());

        Acceptance first = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "第 1 轮提交", List.of(), List.of("r1-site.jpg"),
                null, null, null, null);
        decide(owner, first.getAcceptanceId(), false, "第 1 轮驳回", List.of("瓷砖空鼓"));
        AcceptanceRound round1 = acceptanceRoundRepository
                .findByAcceptanceOrderByRoundNoDesc(first).stream()
                .filter(item -> item.getRoundNo() == 1).findFirst().orElseThrow();
        RoundSnapshot firstSnapshot = snapshot(round1);

        Acceptance second = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "第 2 轮复验", List.of(), List.of("r2-site.jpg"),
                null, null, "已重新铺贴并复检", List.of("r2-fix.jpg"));
        decide(owner, second.getAcceptanceId(), false, "第 2 轮驳回", List.of("局部收口不平"));
        Acceptance third = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "第 3 轮复验", List.of(), List.of("r3-site.jpg"),
                null, null, "已完成二次整改", List.of("r3-fix.jpg"));
        decide(owner, third.getAcceptanceId(), false, "第 3 轮驳回", List.of("胶缝不均匀"));
        Acceptance fourth = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "第 4 轮复验", List.of(), List.of("r4-site.jpg"),
                null, null, "全部问题完成整改", List.of("r4-fix.jpg"));
        Acceptance confirmed = decide(owner, fourth.getAcceptanceId(), true, "复验通过", List.of());

        List<AcceptanceRound> rounds = acceptanceRoundRepository.findByAcceptanceOrderByRoundNoDesc(confirmed);
        assertThat(rounds).extracting(AcceptanceRound::getRoundNo)
                .containsExactly(4, 3, 2, 1);
        AcceptanceRound reloadedFirst = rounds.stream()
                .filter(item -> item.getRoundNo() == 1).findFirst().orElseThrow();
        assertThat(snapshot(reloadedFirst)).isEqualTo(firstSnapshot);
        assertThat(reloadedFirst.getRectificationNote()).isNull();
        assertThat(confirmed.getStatus()).isEqualTo(AcceptStatus.Confirmed);
        assertThat(stageRepository.findById(stage.getStageId()).orElseThrow().getStatus()).isEqualTo(StageStatus.Done);
        assertThat(acceptanceRepository.findFirstByStageOrderByAcceptanceIdDesc(stage).orElseThrow().getAcceptanceId())
                .isEqualTo(first.getAcceptanceId());
        assertThat(financeFacts(fixture.projectA1())).isEqualTo(before);
    }

    @Test
    void shouldRequireRectificationEvidenceOnResubmitAndRejectPostConfirmedSubmission() {
        Fixture fixture = createFixture("rectify-guard");
        Credentials owner = credential("wx-owner-guard", "13800138902");
        accept(fixture, owner, fixture.projectA1(), "整改校验业主");
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());

        Acceptance first = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "首轮提交", List.of(), List.of("guard-site-1.jpg"),
                null, null, null, null);
        decide(owner, first.getAcceptanceId(), false, "首轮驳回", List.of("阳角破损"));

        assertDefined(() -> projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "缺整改证据", List.of(), List.of("guard-site-2.jpg"),
                null, null, null, null), "400");
        assertThat(acceptanceRoundRepository.findByAcceptanceOrderByRoundNoDesc(first)).hasSize(1);

        Acceptance resubmitted = projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "完整复验", List.of(), List.of("guard-site-3.jpg"),
                null, null, "已完成整改", List.of("guard-fix.jpg"));
        decide(owner, resubmitted.getAcceptanceId(), true, "复验通过", List.of());

        assertDefined(() -> projectContext.submitAcceptance(
                fixture.companyA().getComId(), fixture.projectA1().getProjectId(), stage.getStageId(),
                fixture.marketerA(), "通过后再次提交", List.of(), List.of("guard-site-4.jpg"),
                null, null, "再次整改", List.of("guard-fix-2.jpg")), "400");
    }

    @Test
    void shouldRejectCrossProjectAcceptanceAndRevocationMustRemoveReadAccess() {
        Fixture fixture = createFixture("accept-cross");
        Credentials owner = credential("wx-owner-cross", "13800138999");
        CustomerInvite invite = accept(fixture, owner, fixture.projectA1(), "业主越权");
        ProjectStage stageA2 = createStageAndTask(fixture.projectA2(), fixture.marketerA());
        Acceptance otherProjectAcceptance = acceptance(fixture.projectA2(), stageA2, fixture.marketerA());

        setField(ownerAcceptanceInfo, "principal", owner);
        setField(ownerAcceptanceInfo, "acceptanceId", otherProjectAcceptance.getAcceptanceId());
        assertDefined(() -> ownerAcceptanceInfo.call(new Acceptance()), "404");

        setField(ownerAcceptanceUpdate, "principal", owner);
        setField(ownerAcceptanceUpdate, "acceptanceId", otherProjectAcceptance.getAcceptanceId());
        setField(ownerAcceptanceUpdate, "confirmed", true);
        setField(ownerAcceptanceUpdate, "conclusion", "越权");
        assertDefined(() -> ownerAcceptanceUpdate.call(new Acceptance()), "404");

        setField(inviteCancel, "employee", fixture.bossA());
        setField(inviteCancel, "inviteId", invite.getInviteId());
        inviteCancel.call(new CustomerInvite());
        assertThat(accessContext.activeAccesses(owner)).isEmpty();
        assertOwnerNotFound(owner, ownerProjectInfo, "projectId", fixture.projectA1().getProjectId(),
                () -> ownerProjectInfo.call(new Project()));
    }

    @Test
    void shouldReturnCustomerSessionIdentityAndExposeOnlyActiveAccesses() {
        Fixture fixture = createFixture("session");
        Credentials owner = credential("wx-owner-session", "13800138001");
        accept(fixture, owner, fixture.projectA1(), "会话业主");

        setField(sessionInfo, "principal", owner);
        Credentials session = sessionInfo.call(null);
        assertThat(session.getSessionType()).isEqualTo(SessionType.Customer);
        assertThat(session.getCustomerAccesses()).extracting(CustomerAccess::getStatus)
                .containsOnly(CustomerAccessStatus.Active);
        assertThat(session.getCustomerAccesses()).extracting(CustomerAccess::getProject)
                .extracting(Project::getProjectId).containsExactly(fixture.projectA1().getProjectId());
    }

    private Fixture createFixture(String prefix) {
        String suffix = prefix + "-" + System.nanoTime();
        Company companyA = company("企业 A " + suffix);
        Company companyB = company("企业 B " + suffix);
        Store storeA = store(companyA, "门店 A " + suffix);
        Store storeB = store(companyB, "门店 B " + suffix);
        Employee bossA = employee(companyA, storeA, EmployeeRole.Boss, "老板 A " + suffix);
        Employee marketerA = employee(companyA, storeA, EmployeeRole.Marketer, "成员 A " + suffix);
        Employee bossB = employee(companyB, storeB, EmployeeRole.Boss, "老板 B " + suffix);
        Customer customerA1 = customer(companyA, storeA, marketerA, "客户 A1 " + suffix);
        Customer customerA2 = customer(companyA, storeA, marketerA, "客户 A2 " + suffix);
        Customer customerB1 = customer(companyB, storeB, bossB, "客户 B1 " + suffix);
        Project projectA1 = project(customerA1, marketerA, "项目 A1 " + suffix);
        Project projectA2 = project(customerA2, marketerA, "项目 A2 " + suffix);
        Project projectB1 = project(customerB1, bossB, "项目 B1 " + suffix);
        return new Fixture(companyA, companyB, storeA, storeB, bossA, marketerA, bossB,
                customerA1, customerA2, customerB1, projectA1, projectA2, projectB1);
    }

    private Company company(String name) {
        Company company = new Company();
        company.setTitle(name);
        company.setName("C" + Long.toHexString(System.nanoTime()));
        company.setSize(Size.A);
        return companyRepository.save(company);
    }

    private Store store(Company company, String name) {
        Store store = new Store();
        store.setCompany(company);
        store.setName("S" + Long.toHexString(System.nanoTime()));
        store.setDefaultStore(true);
        return storeRepository.save(store);
    }

    private Employee employee(Company company, Store store, EmployeeRole role, String name) {
        Employee employee = new Employee();
        employee.setCompany(company);
        employee.setStore(store);
        employee.setRole(role);
        employee.setName("E" + Long.toHexString(System.nanoTime()));
        employee.setTelNo("1" + Math.abs(System.nanoTime() % 1_000_000_000L));
        return employeeRepository.save(employee);
    }

    private Customer customer(Company company, Store store, Employee owner, String name) {
        Customer customer = new Customer();
        customer.setCompany(company);
        customer.setStore(store);
        customer.setOwner(owner);
        customer.setName("X" + Long.toHexString(System.nanoTime()));
        customer.setTelNo("2" + Math.abs(System.nanoTime() % 1_000_000_000L));
        customer.setCommunity("小区" + Long.toHexString(System.nanoTime()));
        customer.setAddr("地址" + Long.toHexString(System.nanoTime()));
        customer.setArea(new BigDecimal("100"));
        return customerRepository.save(customer);
    }

    private Project project(Customer customer, Employee owner, String name) {
        Property property = new Property();
        property.setCompany(customer.getCompany());
        property.setStore(customer.getStore());
        property.setTeam(customer.getTeam());
        property.setOwner(owner);
        property.setCustomer(customer);
        property.setPropertyNo("P-" + name);
        property.setCommunity(customer.getCommunity());
        property.setBuilding("1");
        property.setRoomNo("101");
        property.setAddress(customer.getAddr());
        property.setArea(new BigDecimal("100"));
        property.setLayout("3室2厅");
        property.setStatus(PropertyStatus.Serving);
        property = propertyRepository.save(property);

        Quote quote = quote(customer, property, owner, name);
        Project project = new Project();
        project.setNo("PJ-" + name);
        project.setName(name);
        project.setCompany(customer.getCompany());
        project.setCustomer(customer);
        project.setProperty(property);
        project.setQuote(quote);
        project.setStore(customer.getStore());
        project.setTeam(customer.getTeam());
        project.setOwner(owner);
        project.setDesigner(owner);
        project.setManager(owner);
        project.setSupport(owner);
        project.setSiteAddr(property.getAddress());
        project.setArea(property.getArea());
        project.setContractAmount(new BigDecimal("100000"));
        project.setCost(new BigDecimal("80000"));
        project.setStatus(ProjectStatus.Ongoing);
        project.setProgress(10);
        project.setStartDate(LocalDate.now());
        project.setPlanEndDate(LocalDate.now().plusDays(90));
        return projectRepository.save(project);
    }

    private Quote quote(Customer customer, Property property, Employee owner, String key) {
        Quote quote = new Quote();
        quote.setNo("Q-" + key);
        quote.setTitle("报价-" + key);
        quote.setCompany(customer.getCompany());
        quote.setStore(customer.getStore());
        quote.setTeam(customer.getTeam());
        quote.setCustomer(customer);
        quote.setProperty(property);
        quote.setCreatedBy(owner);
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
        return quoteRepository.save(quote);
    }

    private Credentials credential(String idKey, String phone) {
        Credentials credential = new Credentials();
        credential.setType(CredType.Wx);
        credential.setIdKey(idKey + "-" + System.nanoTime());
        credential.setIdSecret("union-" + idKey + "-" + System.nanoTime());
        credential.setPhone(phone);
        credential.setStatus(UserStatus.Active);
        return credentialsRepository.save(credential);
    }

    private CustomerInvite createInvite(Fixture fixture, Employee inviter, Project project,
                                        String name, String phone) {
        setField(inviteSave, "employee", inviter);
        setField(inviteSave, "projectId", project.getProjectId());
        setField(inviteSave, "customerName", name);
        setField(inviteSave, "telNo", phone);
        return inviteSave.call(new CustomerInvite());
    }

    private CustomerInvite createInviteRecord(Fixture fixture, Project project, String phone, String token) {
        CustomerInvite invite = new CustomerInvite();
        invite.setCompany(project.getCompany());
        invite.setProject(project);
        invite.setCustomer(project.getCustomer());
        invite.setInviter(fixture.bossA());
        invite.setCustomerName("直接夹具业主");
        invite.setTelNo(phone);
        invite.setToken(token + "-" + System.nanoTime());
        invite.setStatus(InviteStatus.Pending);
        invite.setExpireTime(LocalDateTime.now().plusDays(30));
        return inviteRepository.save(invite);
    }

    private CustomerInvite accept(Fixture fixture, Credentials owner, Project project, String name) {
        CustomerInvite invite = createInvite(fixture, fixture.bossA(), project, name, owner.getPhone());
        setField(inviteAccept, "principal", owner);
        setField(inviteAccept, "inviteId", invite.getInviteId());
        setField(inviteAccept, "token", invite.getToken());
        inviteAccept.call(new Credentials());
        return invite;
    }

    private ProjectStage createStageAndTask(Project project, Employee owner) {
        ProjectStage stage = new ProjectStage();
        stage.setCompany(project.getCompany());
        stage.setStore(project.getStore());
        stage.setTeam(project.getTeam());
        stage.setOwner(owner);
        stage.setProject(project);
        stage.setProperty(project.getProperty());
        stage.setName("阶段-" + project.getProjectId());
        stage.setSort(1);
        stage.setWeight(BigDecimal.ONE);
        stage.setStatus(StageStatus.Doing);
        stage.setProgress(100);
        stage = stageRepository.save(stage);

        ProjectTask task = new ProjectTask();
        task.setCompany(project.getCompany());
        task.setStore(project.getStore());
        task.setTeam(project.getTeam());
        task.setProject(project);
        task.setProperty(project.getProperty());
        task.setStage(stage);
        task.setOwner(owner);
        task.setCreator(owner);
        task.setName("任务-" + project.getProjectId());
        task.setStatus(TaskStatus.Done);
        task.setProgress(100);
        task.setQuantity(BigDecimal.ONE);
        task.setSalePrice(new BigDecimal("1000"));
        task.setCost(new BigDecimal("600"));
        taskRepository.save(task);
        return stage;
    }

    private Acceptance acceptance(Project project, ProjectStage stage, Employee submittedBy) {
        Acceptance acceptance = new Acceptance();
        acceptance.setCompany(project.getCompany());
        acceptance.setStore(project.getStore());
        acceptance.setTeam(project.getTeam());
        acceptance.setOwner(project.getOwner());
        acceptance.setProject(project);
        acceptance.setProperty(project.getProperty());
        acceptance.setStage(stage);
        acceptance.setSubmittedBy(submittedBy);
        acceptance.setStatus(AcceptStatus.Pending);
        acceptance.setConclusion("待业主确认");
        acceptance.setIssues(new ArrayList<>());
        acceptance.setEvidenceUrls(new ArrayList<>(List.of("https://example.test/acceptance.jpg")));
        return acceptanceRepository.save(acceptance);
    }

    private Acceptance decide(Credentials owner, Long acceptanceId, boolean confirmed, String conclusion) {
        if (confirmed) {
            return decide(owner, acceptanceId, true, conclusion, List.of(), null);
        }
        return decide(owner, acceptanceId, false, conclusion, List.of("验收问题"),
                AcceptanceRectificationType.Construction);
    }

    private Acceptance decide(Credentials owner, Long acceptanceId, boolean confirmed, String conclusion,
                              List<String> issues) {
        if (confirmed) {
            return decide(owner, acceptanceId, true, conclusion, issues, null);
        }
        return decide(owner, acceptanceId, false, conclusion, issues,
                AcceptanceRectificationType.Construction);
    }

    private Acceptance decide(Credentials owner, Long acceptanceId, boolean confirmed, String conclusion,
                              List<String> issues, AcceptanceRectificationType rectificationType) {
        setField(ownerAcceptanceUpdate, "principal", owner);
        setField(ownerAcceptanceUpdate, "acceptanceId", acceptanceId);
        setField(ownerAcceptanceUpdate, "confirmed", confirmed);
        setField(ownerAcceptanceUpdate, "conclusion", conclusion);
        setField(ownerAcceptanceUpdate, "issues", issues);
        setField(ownerAcceptanceUpdate, "rectificationType", rectificationType);
        return ownerAcceptanceUpdate.call(new Acceptance());
    }

    private void executeRectificationBackfill() throws Exception {
        String script = Files.readString(Path.of(
                "docs/design/migrations/47-阶段验收整改轮次.sql"));
        String lower = script.toLowerCase();
        int roundStart = lower.indexOf("insert into db_acceptance_round (");
        int issueStart = lower.indexOf("insert into db_acceptance_round_issue", roundStart);
        int evidenceStart = lower.indexOf("insert into db_acceptance_round_evidence", issueStart);
        int rollbackStart = lower.indexOf("-- 回滚", evidenceStart);
        assertThat(roundStart).isGreaterThanOrEqualTo(0);
        assertThat(issueStart).isGreaterThan(roundStart);
        assertThat(evidenceStart).isGreaterThan(issueStart);
        assertThat(rollbackStart).isGreaterThan(evidenceStart);
        String roundSql = script.substring(roundStart, issueStart).strip();
        // H2 不支持 MySQL 的 STR_TO_DATE；仅替换时间表达式，其他回填 SQL 原样执行。
        roundSql = roundSql.replace(
                "str_to_date(concat(a.create_date, lpad(coalesce(a.create_time, 0), 6, '0')),\n"
                        + "                             '%Y%m%d%H%i%s')",
                "coalesce(a.confirmed_at, current_timestamp)");
        jdbcTemplate.execute(roundSql);
        jdbcTemplate.execute(script.substring(issueStart, evidenceStart).strip());
        jdbcTemplate.execute(script.substring(evidenceStart, rollbackStart).strip());
    }

    private RoundSnapshot snapshot(AcceptanceRound round) {
        return new RoundSnapshot(round.getRoundId(), round.getRoundNo(), round.getResult(),
                round.getRectificationNote(), round.getRectificationEvidenceUrls(), round.getEvidenceUrls(),
                round.getIssues(), round.getConclusion(), round.getSubmittedAt(), round.getConfirmedAt(),
                round.getRectificationOwner() == null ? null : round.getRectificationOwner().getEmpId(),
                round.getRectificationDueDate(), round.getRectificationType(),
                round.getRectificationIssueImages());
    }

    private void createFinanceFacts(Fixture fixture) {
        Receivable receivable = new Receivable();
        receivable.setCompany(fixture.companyA());
        receivable.setStore(fixture.storeA());
        receivable.setTeam(fixture.projectA1().getTeam());
        receivable.setOwner(fixture.bossA());
        receivable.setProject(fixture.projectA1());
        receivable.setProperty(fixture.projectA1().getProperty());
        receivable.setCustomer(fixture.customerA1());
        receivable.setName("整改验收冻结应收");
        receivable.setType(ReceivableType.Other);
        receivable.setAmount(new BigDecimal("1000.00"));
        receivable.setReceivedAmount(new BigDecimal("200.00"));
        receivable.setDueDate(LocalDate.now().plusDays(10));
        receivable.setStatus(ReceivableStatus.Partial);
        receivable.setCreatedBy(fixture.bossA());
        receivableRepository.save(receivable);

        Payable payable = new Payable();
        payable.setCompany(fixture.companyA());
        payable.setStore(fixture.storeA());
        payable.setTeam(fixture.projectA1().getTeam());
        payable.setOwner(fixture.bossA());
        payable.setProject(fixture.projectA1());
        payable.setProperty(fixture.projectA1().getProperty());
        payable.setName("整改验收冻结应付");
        payable.setType(PayableType.Other);
        payable.setAmount(new BigDecimal("600.00"));
        payable.setPaidAmount(new BigDecimal("100.00"));
        payable.setDueDate(LocalDate.now().plusDays(10));
        payable.setStatus(PayableStatus.Partial);
        payable.setCreatedBy(fixture.bossA());
        payableRepository.save(payable);

        Settlement settlement = new Settlement();
        settlement.setCompany(fixture.companyA());
        settlement.setStore(fixture.storeA());
        settlement.setTeam(fixture.projectA1().getTeam());
        settlement.setOwner(fixture.bossA());
        settlement.setProject(fixture.projectA1());
        settlement.setContractAmount(new BigDecimal("100000.00"));
        settlement.setFinalAmount(new BigDecimal("100000.00"));
        settlement.setReceivableAmount(new BigDecimal("1000.00"));
        settlement.setReceivedAmount(new BigDecimal("200.00"));
        settlement.setPayableAmount(new BigDecimal("600.00"));
        settlement.setPaidAmount(new BigDecimal("100.00"));
        settlement.setTotalCost(new BigDecimal("80000.00"));
        settlement.setProfitAmount(new BigDecimal("20000.00"));
        settlement.setGrossMargin(new BigDecimal("20.00"));
        settlement.setStatus(SettlementStatus.Draft);
        settlement.setCreatedBy(fixture.bossA());
        settlementRepository.save(settlement);
    }

    private FinanceFacts financeFacts(Project project) {
        List<String> receivables = receivableRepository.findByProjectOrderByDueDateAsc(project).stream()
                .map(item -> item.getReceivableId() + "|" + money(item.getAmount()) + "|"
                        + money(item.getReceivedAmount()) + "|" + item.getStatus()).toList();
        List<String> payables = payableRepository.findByProjectOrderByDueDateAsc(project).stream()
                .map(item -> item.getPayableId() + "|" + money(item.getAmount()) + "|"
                        + money(item.getPaidAmount()) + "|" + item.getStatus()).toList();
        String settlement = settlementRepository.findByProject_ProjectId(project.getProjectId())
                .map(item -> item.getSettlementId() + "|" + money(item.getContractAmount()) + "|"
                        + money(item.getFinalAmount()) + "|" + money(item.getReceivableAmount()) + "|"
                        + money(item.getReceivedAmount()) + "|" + money(item.getPayableAmount()) + "|"
                        + money(item.getPaidAmount()) + "|" + money(item.getTotalCost()) + "|"
                        + money(item.getProfitAmount()) + "|" + money(item.getGrossMargin()) + "|" + item.getStatus())
                .orElse(null);
        return new FinanceFacts(money(project.getContractAmount()), money(project.getCost()), receivables, payables, settlement);
    }

    private String money(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private ProjectReport report(Project project, ProjectStage stage, String title, boolean visible) {
        ProjectReport report = new ProjectReport();
        report.setCompany(project.getCompany());
        report.setStore(project.getStore());
        report.setTeam(project.getTeam());
        report.setOwner(project.getOwner());
        report.setProject(project);
        report.setProperty(project.getProperty());
        report.setStage(stage);
        report.setType(ReportType.Progress);
        report.setTitle(title);
        report.setContent(title + " 内容");
        report.setImages(List.of("https://example.test/" + title + ".jpg"));
        report.setAuthor(project.getOwner());
        report.setAuthorRole(EmployeeRole.Marketer);
        report.setVisibleToCustomer(visible);
        return reportRepository.save(report);
    }

    private ProjectContract createContract(Project project, Employee creator, String no) {
        Quote quote = project.getQuote();
        ProjectContract contract = new ProjectContract();
        contract.setCompany(project.getCompany());
        contract.setStore(project.getStore());
        contract.setTeam(project.getTeam());
        contract.setOwner(project.getOwner());
        contract.setProject(project);
        contract.setQuote(quote);
        contract.setCustomer(project.getCustomer());
        contract.setProperty(project.getProperty());
        contract.setNo(no);
        contract.setBaselineAmount(new BigDecimal("100000"));
        contract.setCurrentAmount(new BigDecimal("100000"));
        contract.setStatus(ContractStatus.Effective);
        contract.setSignedAt(LocalDate.now());
        contract.setStartDate(project.getStartDate());
        contract.setPlanEndDate(project.getPlanEndDate());
        contract.setPaymentTerms("test-terms");
        contract.setCreatedBy(creator);
        contract.setRemark("internal-only");
        return contractRepository.save(contract);
    }

    private void assertOwnerNotFound(Credentials owner, Object provider, String fieldName, Object value,
                                     Supplier<?> call) {
        setField(provider, "principal", owner);
        setField(provider, fieldName, value);
        assertDefined(call::get, "404");
    }

    private void assertDefined(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String code) {
        assertThatThrownBy(callable).isInstanceOfSatisfying(DefinedException.class,
                ex -> assertThat(ex.getErrorCode().getCode()).isEqualTo(code));
    }

    private static void setField(Object target, String name, Object value) {
        ReflectionTestUtils.setField(target, name, value);
    }

    private record RoundSnapshot(Long roundId, Integer roundNo, AcceptStatus result, String rectificationNote,
                                 List<String> rectificationEvidence, List<String> evidence,
                                 List<String> issues, String conclusion, LocalDateTime submittedAt,
                                 LocalDateTime confirmedAt, Long rectificationOwnerId, LocalDate rectificationDueDate,
                                 AcceptanceRectificationType rectificationType,
                                 List<String> rectificationIssueImages) {
    }

    private record FinanceFacts(String contractAmount, String cost, List<String> receivables,
                                List<String> payables, String settlement) {
    }

    private record Fixture(Company companyA, Company companyB, Store storeA, Store storeB,
                           Employee bossA, Employee marketerA, Employee bossB,
                           Customer customerA1, Customer customerA2, Customer customerB1,
                           Project projectA1, Project projectA2, Project projectB1) {
    }
}

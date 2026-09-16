package com.th4.edge.decoboon.owner;

import com.th4.edge.decoboon.customer.configure.CustomerAccessStatus;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.decoboon.customer.repository.CustomerAccessRepository;
import com.th4.edge.decoboon.customer.repository.CustomerInviteRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.ContractStatus;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.ReportType;
import com.th4.edge.decoboon.project.configure.StageStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.context.ProjectContext;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.model.ProjectStage;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
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
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
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
    @Autowired private ProjectReportRepository reportRepository;
    @Autowired private ProjectContractRepository contractRepository;
    @Autowired private QuoteRepository quoteRepository;
    @Autowired private CustomerInviteRepository inviteRepository;
    @Autowired private CustomerAccessRepository accessRepository;
    @Autowired private CustomerAccessContext accessContext;
    @Autowired private ProjectContext projectContext;

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
    @Autowired private com.th4.edge.decoboon.customer.service.contract.info.Info ownerContractInfo;
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
        ownerContractInfo = AopTestUtils.getTargetObject(ownerContractInfo);
        sessionInfo = AopTestUtils.getTargetObject(sessionInfo);
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
        ProjectStage stage = createStageAndTask(fixture.projectA1(), fixture.marketerA());
        Acceptance first = acceptance(fixture.projectA1(), stage, fixture.marketerA());

        Acceptance rejected = decide(owner, first.getAcceptanceId(), false, "整改");
        assertThat(rejected.getStatus()).isEqualTo(AcceptStatus.Rejected);
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
        setField(ownerAcceptanceUpdate, "principal", owner);
        setField(ownerAcceptanceUpdate, "acceptanceId", acceptanceId);
        setField(ownerAcceptanceUpdate, "confirmed", confirmed);
        setField(ownerAcceptanceUpdate, "conclusion", conclusion);
        return ownerAcceptanceUpdate.call(new Acceptance());
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

    private record Fixture(Company companyA, Company companyB, Store storeA, Store storeB,
                           Employee bossA, Employee marketerA, Employee bossB,
                           Customer customerA1, Customer customerA2, Customer customerB1,
                           Project projectA1, Project projectA2, Project projectB1) {
    }
}

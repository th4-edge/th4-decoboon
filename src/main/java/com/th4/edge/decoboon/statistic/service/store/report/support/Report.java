package com.th4.edge.decoboon.statistic.service.store.report.support;

import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.collab.repository.NoticeRepository;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.ProjectStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.statistic.context.QuoteAcceptanceContext;
import com.th4.edge.decoboon.statistic.model.DesignerPerformance;
import com.th4.edge.decoboon.statistic.model.StoreManagerReport;
import com.th4.edge.decoboon.statistic.model.StoreScopeInfo;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.repository.StoreRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** 店长经营报告，所有数据严格限定在登录店长所属企业、门店。 */
@Description("聚合门店成员、客户、报价、项目、施工风险、通知与设计师工作量")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "gender", "source", "community", "addr",
                        "area", "budget", "status", "owner", "designer", "wechatAdded", "visitCount",
                        "lastFollowAt", "nextFollowAt", "pendingMeasureAt", "measureAt", "handedOffAt", "createTime"}),
        @AllowProperties(model = Quote.class,
                properties = {"quoteId", "no", "customer", "createdBy", "area", "amount", "cost",
                        "discount", "status", "createTime"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "property", "customer", "designer", "manager", "siteAddr",
                        "area", "contractAmount", "cost", "status", "progress", "startDate", "planEndDate", "createTime" }),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class,
                properties = {"empId", "name", "telNo", "role", "store"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"}),
        // 门店维度范围标注（DEC-067 BOSS-STORE-03 / P0B-04 同构）
        @AllowProperties(model = StoreScopeInfo.class,
                properties = {"scopeType", "storeId", "storeName", "unassignedCount"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.StoreManagerReport.class, properties = {"storeScope", "storeName", "memberCount", "designerCount", "marketerCount", "projectManagerCount", "customerCount", "dormantCustomerCount", "newCustomerCount", "followingCustomerCount", "pendingMeasureCount", "unassignedDesignerCount", "quoteCount", "pendingQuoteCount", "acceptedQuoteCount", "quoteAmount", "signedAmount", "conversionRate", "submittedQuoteCount", "pendingDecisionQuoteCount", "decidedOpportunityCount", "acceptedOpportunityCount", "finalQuoteAcceptanceRate", "firstQuoteAcceptanceRate", "projectCount", "preparingProjectCount", "ongoingProjectCount", "acceptingProjectCount", "suspendedProjectCount", "finishedProjectCount", "taskTodoCount", "taskOverdueCount", "reportCount", "noticeUnreadCount", "designerPerformances", "recentCustomers", "recentQuotes", "recentProjects"}),
        @AllowProperties(model = com.th4.edge.decoboon.statistic.model.DesignerPerformance.class, properties = {"designer", "customerCount", "pendingMeasureCount", "quoteCount", "pendingQuoteCount", "acceptedQuoteCount", "quoteAmount", "signedAmount", "projectCount", "ongoingProjectCount", "taskTodoCount", "taskOverdueCount", "reportCount", "conversionRate", "submittedQuoteCount", "pendingDecisionQuoteCount", "decidedOpportunityCount", "acceptedOpportunityCount", "finalQuoteAcceptanceRate", "firstQuoteAcceptanceRate"})
})
@Note("门店经营报告（BOSS / 总部财务 / 店长 / 门店财务）")
@Service("statistic/store/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.store.report.Report {

    /**
     * 门店维度（`DESIGN-BIZ-20261013-003` P0D-02 / `GAP-048`）：框架只对 provider 实例的
     * {@code @AutoInject} 字段做请求绑定，请求模型不承载 HTTP 参数，因此门店维度必须落在字段上。
     */
    @AutoInject
    private Long storeId;

    private final TenantContext tenantContext;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final ProjectReportRepository projectReportRepository;
    private final NoticeRepository noticeRepository;
    private final StoreRepository storeRepository;
    private final StoreFilterContext storeFilterContext;

    public Report(TenantContext tenantContext,
                  EmployeeRepository employeeRepository,
                  CustomerRepository customerRepository,
                  QuoteRepository quoteRepository,
                  ProjectRepository projectRepository,
                  ProjectTaskRepository taskRepository,
                  ProjectReportRepository projectReportRepository,
                  NoticeRepository noticeRepository,
                  StoreRepository storeRepository,
                  StoreFilterContext storeFilterContext) {
        this.tenantContext = tenantContext;
        this.employeeRepository = employeeRepository;
        this.customerRepository = customerRepository;
        this.quoteRepository = quoteRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.projectReportRepository = projectReportRepository;
        this.noticeRepository = noticeRepository;
        this.storeRepository = storeRepository;
        this.storeFilterContext = storeFilterContext;
    }

    @Override
    @Transactional(readOnly = true)
    public StoreManagerReport call(@User Employee employee) {
        // 角色门禁（DESIGN-BIZ-20261013-003 P0D-02）：BOSS / 总部财务 / 店长 / 门店财务。
        // 店长与门店财务由 StoreFilterContext 锁定本店（传他店 403）；BOSS 与总部财务可按门店收窄。
        Employee current = tenantContext.requireRole(employee,
                EmployeeRole.Boss, EmployeeRole.Finance, EmployeeRole.StoreManager);
        if (current.getCompany() == null) {
            throw new DefinedException(new DefaultErrorCode("403", "当前账号未归属企业"));
        }

        Long comId = current.getCompany().getComId();
        // 门店维度：解析 + 越权校验（他企业 / 不存在的门店 → 403）。
        Long scopeStoreId = storeFilterContext.resolve(current, storeId);
        if (StoreScope.isAllStores(scopeStoreId)) {
            // 「全部门店」视图：本页是门店经营概览（含人员结构与比率），**不提供全部门店合计**
            // （比率与人员结构不可简单相加，避免与经营总览/财务口径重叠造成误读）。
            // 只回范围标注，由前端给「请先选择门店」引导。
            StoreManagerReport guide = new StoreManagerReport();
            StoreScopeInfo allStores = new StoreScopeInfo();
            allStores.setScopeType("AllStores");
            guide.setStoreScope(allStores);
            return guide;
        }
        Store store = storeRepository.findByStoreId(scopeStoreId);
        if (store == null) {
            throw new DefinedException(new DefaultErrorCode("403", "门店不存在或无权访问"));
        }
        List<Employee> members = employeeRepository
                .findByCompany_ComIdAndStore_StoreIdOrderByName(comId, scopeStoreId);
        List<Customer> customers = customerRepository
                .findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(comId, scopeStoreId);
        List<Quote> quotes = quoteRepository
                .findByCompany_ComIdAndCustomer_Store_StoreIdOrderByQuoteIdDesc(comId, scopeStoreId);
        List<Project> projects = projectRepository
                .findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(comId, scopeStoreId);

        StoreManagerReport report = new StoreManagerReport();
        report.setStoreName(store.getName());
        report.setMemberCount(members.size());
        report.setDesignerCount(countRole(members, EmployeeRole.Designer));
        report.setMarketerCount(countRole(members, EmployeeRole.Marketer));
        report.setProjectManagerCount(countRole(members, EmployeeRole.Pm));
        report.setCustomerCount(customers.size());
        report.setDormantCustomerCount(countCustomers(customers, CustStatus.Dormant));
        report.setNewCustomerCount(countCustomers(customers, CustStatus.New));
        report.setFollowingCustomerCount(countCustomers(customers,
                CustStatus.Following, CustStatus.Quoted));
        report.setPendingMeasureCount(countCustomers(customers, CustStatus.PendingMeasure));
        report.setUnassignedDesignerCount((int) customers.stream()
                .filter(customer -> customer.getStatus() == CustStatus.PendingMeasure
                        && customer.getMeasureAt() != null
                        && customer.getDesigner() == null)
                .count());
        report.setQuoteCount(quotes.size());
        report.setPendingQuoteCount((int) quotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Draft
                        || quote.getStatus() == QuoteStatus.Submitted)
                .count());
        report.setAcceptedQuoteCount((int) quotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Accepted
                        || quote.getStatus() == QuoteStatus.Converted)
                .count());
        report.setQuoteAmount(quotes.stream()
                .filter(quote -> quote.getStatus() != QuoteStatus.Rejected && quote.getStatus() != QuoteStatus.Superseded)
                .map(Quote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setSignedAmount(projects.stream()
                .map(Project::getContractAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        QuoteAcceptanceContext.Result acceptance = QuoteAcceptanceContext.analyze(quotes);
        report.setSubmittedQuoteCount(acceptance.submittedQuoteCount());
        report.setPendingDecisionQuoteCount(acceptance.pendingDecisionQuoteCount());
        report.setDecidedOpportunityCount(acceptance.decidedOpportunityCount());
        report.setAcceptedOpportunityCount(acceptance.acceptedOpportunityCount());
        report.setFinalQuoteAcceptanceRate(acceptance.finalAcceptanceRate());
        report.setFirstQuoteAcceptanceRate(acceptance.firstAcceptanceRate());
        report.setConversionRate(acceptance.finalAcceptanceRate());

        report.setProjectCount(projects.size());
        report.setPreparingProjectCount(countProjects(projects, ProjectStatus.Preparing));
        report.setOngoingProjectCount(countProjects(projects, ProjectStatus.Ongoing));
        report.setAcceptingProjectCount(countProjects(projects, ProjectStatus.Accepting));
        report.setSuspendedProjectCount(countProjects(projects, ProjectStatus.Suspended));
        report.setFinishedProjectCount(countProjects(projects, ProjectStatus.Finished, ProjectStatus.Settled));

        int todo = 0;
        int overdue = 0;
        int reportCount = 0;
        // DEC-036：循环外一次批量取数（原为每项目「任务 + 日报计数」两条）
        List<Long> projectIds = projects.stream().map(Project::getProjectId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        java.util.Map<Long, List<ProjectTask>> tasksByProject = projectIds.isEmpty() ? java.util.Map.of()
                : taskRepository.findByProject_ProjectIdIn(projectIds).stream()
                        .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                        .collect(java.util.stream.Collectors.groupingBy(item -> item.getProject().getProjectId()));
        java.util.Map<Long, Integer> reportCountByProject = new java.util.HashMap<>();
        if (!projectIds.isEmpty()) {
            for (Object[] row : projectReportRepository.countGroupByProject(current.getCompany().getComId(), projectIds)) {
                reportCountByProject.put((Long) row[0], ((Number) row[1]).intValue());
            }
        }
        for (Project project : projects) {
            for (ProjectTask task : tasksByProject.getOrDefault(project.getProjectId(), List.of())) {
                if (task.getStatus() == TaskStatus.Todo || task.getStatus() == TaskStatus.Doing) {
                    todo++;
                }
                if (task.getPlannedEnd() != null
                        && task.getPlannedEnd().isBefore(LocalDate.now())
                        && task.getStatus() != TaskStatus.Done
                        && task.getStatus() != TaskStatus.Cancelled) {
                    overdue++;
                }
            }
            reportCount += reportCountByProject.getOrDefault(project.getProjectId(), 0);
        }
        report.setTaskTodoCount(todo);
        report.setTaskOverdueCount(overdue);
        report.setReportCount(reportCount);
        report.setNoticeUnreadCount(noticeRepository.countByReceiver_EmpIdAndReadFalse(current.getEmpId()));

        List<DesignerPerformance> performances = members.stream()
                .filter(member -> member.getRole() == EmployeeRole.Designer)
                .map(designer -> performance(designer, customers, quotes, projects))
                .sorted(Comparator.comparing(DesignerPerformance::getTaskOverdueCount).reversed()
                        .thenComparing(DesignerPerformance::getSignedAmount, Comparator.reverseOrder()))
                .toList();
        report.setDesignerPerformances(performances);
        report.setRecentCustomers(customers.stream().limit(8).toList());
        report.setRecentQuotes(quotes.stream().limit(8).toList());
        report.setRecentProjects(projects.stream().limit(8).toList());
        StoreScopeInfo scopeInfo = new StoreScopeInfo();
        scopeInfo.setScopeType("Store");
        scopeInfo.setStoreId(scopeStoreId);
        scopeInfo.setStoreName(store.getName());
        // 单店视图不含未归属门店（未归属只在「全部门店」口径下出现，DEC-067 BOSS-STORE-03）。
        scopeInfo.setUnassignedCount(0);
        report.setStoreScope(scopeInfo);
        return report;
    }

    private DesignerPerformance performance(Employee designer,
                                            List<Customer> customers,
                                            List<Quote> quotes,
                                            List<Project> projects) {
        Long designerId = designer.getEmpId();
        List<Customer> ownedCustomers = customers.stream()
                .filter(customer -> customer.getDesigner() != null
                        && designerId.equals(customer.getDesigner().getEmpId()))
                .toList();
        List<Quote> designerQuotes = quotes.stream()
                .filter(quote -> quote.getCreatedBy() != null
                        && designerId.equals(quote.getCreatedBy().getEmpId()))
                .toList();
        List<Project> designerProjects = projects.stream()
                .filter(project -> project.getDesigner() != null
                        && designerId.equals(project.getDesigner().getEmpId()))
                .toList();

        DesignerPerformance performance = new DesignerPerformance();
        performance.setDesigner(designer);
        performance.setCustomerCount(ownedCustomers.size());
        performance.setPendingMeasureCount((int) ownedCustomers.stream()
                .filter(customer -> customer.getStatus() == CustStatus.PendingMeasure)
                .count());
        performance.setQuoteCount(designerQuotes.size());
        performance.setPendingQuoteCount((int) designerQuotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Draft
                        || quote.getStatus() == QuoteStatus.Submitted)
                .count());
        performance.setAcceptedQuoteCount((int) designerQuotes.stream()
                .filter(quote -> quote.getStatus() == QuoteStatus.Accepted
                        || quote.getStatus() == QuoteStatus.Converted)
                .count());
        performance.setQuoteAmount(designerQuotes.stream()
                .filter(quote -> quote.getStatus() != QuoteStatus.Rejected && quote.getStatus() != QuoteStatus.Superseded)
                .map(Quote::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        performance.setSignedAmount(designerProjects.stream()
                .map(Project::getContractAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        performance.setProjectCount(designerProjects.size());
        performance.setOngoingProjectCount((int) designerProjects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.Preparing
                        || project.getStatus() == ProjectStatus.Ongoing
                        || project.getStatus() == ProjectStatus.Accepting)
                .count());
        QuoteAcceptanceContext.Result acceptance = QuoteAcceptanceContext.analyze(quotes, designer);
        performance.setSubmittedQuoteCount(acceptance.submittedQuoteCount());
        performance.setPendingDecisionQuoteCount(acceptance.pendingDecisionQuoteCount());
        performance.setDecidedOpportunityCount(acceptance.decidedOpportunityCount());
        performance.setAcceptedOpportunityCount(acceptance.acceptedOpportunityCount());
        performance.setFinalQuoteAcceptanceRate(acceptance.finalAcceptanceRate());
        performance.setFirstQuoteAcceptanceRate(acceptance.firstAcceptanceRate());
        performance.setConversionRate(acceptance.finalAcceptanceRate());

        int todo = 0;
        int overdue = 0;
        int reports = 0;
        // DEC-036：同上，设计师维度也改循环外批量
        List<Long> designerProjectIds = designerProjects.stream().map(Project::getProjectId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        java.util.Map<Long, List<ProjectTask>> tasksByDesignerProject = designerProjectIds.isEmpty() ? java.util.Map.of()
                : taskRepository.findByProject_ProjectIdIn(designerProjectIds).stream()
                        .filter(item -> item.getProject() != null && item.getProject().getProjectId() != null)
                        .collect(java.util.stream.Collectors.groupingBy(item -> item.getProject().getProjectId()));
        java.util.Map<Long, Integer> reportCountByDesignerProject = new java.util.HashMap<>();
        if (!designerProjectIds.isEmpty()) {
            for (Object[] row : projectReportRepository.countGroupByProject(
                    designer.getCompany().getComId(), designerProjectIds)) {
                reportCountByDesignerProject.put((Long) row[0], ((Number) row[1]).intValue());
            }
        }
        for (Project project : designerProjects) {
            for (ProjectTask task : tasksByDesignerProject.getOrDefault(project.getProjectId(), List.of())) {
                if (task.getStatus() == TaskStatus.Todo || task.getStatus() == TaskStatus.Doing) {
                    todo++;
                }
                if (task.getPlannedEnd() != null
                        && task.getPlannedEnd().isBefore(LocalDate.now())
                        && task.getStatus() != TaskStatus.Done
                        && task.getStatus() != TaskStatus.Cancelled) {
                    overdue++;
                }
            }
            reports += reportCountByDesignerProject.getOrDefault(project.getProjectId(), 0);
        }
        performance.setTaskTodoCount(todo);
        performance.setTaskOverdueCount(overdue);
        performance.setReportCount(reports);
        return performance;
    }

    private int countRole(List<Employee> members, EmployeeRole role) {
        return (int) members.stream().filter(member -> member.getRole() == role).count();
    }

    private int countCustomers(List<Customer> customers, CustStatus... statuses) {
        return (int) customers.stream().filter(customer -> contains(statuses, customer.getStatus())).count();
    }

    private int countProjects(List<Project> projects, ProjectStatus... statuses) {
        return (int) projects.stream().filter(project -> contains(statuses, project.getStatus())).count();
    }

    private <T> boolean contains(T[] values, T target) {
        for (T value : values) {
            if (value == target) return true;
        }
        return false;
    }

}

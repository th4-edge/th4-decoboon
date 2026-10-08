package com.th4.edge.decoboon.collab.context;

import com.th4.edge.decoboon.collab.configure.NoticeType;
import com.th4.edge.decoboon.collab.model.Notice;
import com.th4.edge.decoboon.collab.repository.NoticeRepository;
import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.context.CustomerInsightContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.configure.AcceptStatus;
import com.th4.edge.decoboon.project.configure.TaskStatus;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectTask;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.project.repository.ProjectTaskRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Note("业务通知生成上下文")
@Component
public class NoticeContext {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CustomerRepository customerRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository taskRepository;
    private final NoticeRepository noticeRepository;
    private final QuoteRepository quoteRepository;
    private final EmployeeRepository employeeRepository;
    private final SubscribeMessageContext subscribeMessageContext;

    public NoticeContext(CustomerRepository customerRepository,
                         ProjectRepository projectRepository,
                         ProjectTaskRepository taskRepository,
                         NoticeRepository noticeRepository,
                         QuoteRepository quoteRepository,
                         EmployeeRepository employeeRepository,
                         SubscribeMessageContext subscribeMessageContext) {
        this.customerRepository = customerRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.noticeRepository = noticeRepository;
        this.quoteRepository = quoteRepository;
        this.employeeRepository = employeeRepository;
        this.subscribeMessageContext = subscribeMessageContext;
    }

    @Transactional
    public void measureRequested(Customer customer, Employee marketer) {
        Long comId = marketer.getCompany().getComId();
        List<Employee> managers = customer.getStore() == null ? List.of()
                : employeeRepository.findByCompany_ComIdAndStore_StoreIdAndRoleOrderByName(
                        comId, customer.getStore().getStoreId(), EmployeeRole.StoreManager);
        if (managers.isEmpty()) {
            managers = employeeRepository.findByCompany_ComIdAndRoleOrderByName(comId, EmployeeRole.Boss);
        }
        for (Employee manager : managers) {
            upsert(manager, null, customer, null, "measure:" + customer.getCustId(), NoticeType.Project,
                    "待指定设计师", customer.getName() + " · 约定 " + formatTime(customer.getMeasureAt())
                            + " · 业务员 " + marketer.getName());
        }
    }

    @Transactional
    public void designerSubmitted(Customer customer, Employee marketer, Employee designer) {
        upsert(designer, null, customer, null, "assign-designer:" + customer.getCustId(), NoticeType.Customer,
                "新客户已分配", customer.getName() + " · 约定量房 " + formatTime(customer.getMeasureAt())
                        + " · 业务员 " + marketer.getName());
        if (customer.getOwner() != null
                && !customer.getOwner().getEmpId().equals(designer.getEmpId())) {
            upsert(customer.getOwner(), null, customer, null, "designer-assigned:" + customer.getCustId(),
                    NoticeType.Customer, marketer.getRole().getName() + "已指定设计师",
                    customer.getName() + " · 设计师 " + designer.getName()
                            + " · 约定量房 " + formatTime(customer.getMeasureAt()));
        }
    }

    @Transactional
    public void refresh(Employee employee) {
        Long comId = employee.getCompany().getComId();
        noticeRepository.deleteByReceiver_EmpIdAndQuoteIsNotNull(employee.getEmpId());
        Long empId = employee.getEmpId();
        if (employee.getRole() == EmployeeRole.StoreManager) {
            refreshStoreManager(employee, comId);
            return;
        }
        if (employee.getRole() == EmployeeRole.TeamLeader) {
            refreshTeamLeader(employee, comId);
            return;
        }
        Map<Long, Customer> customerMap = new LinkedHashMap<>();
        customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, empId)
                .forEach(customer -> customerMap.put(customer.getCustId(), customer));
        List<Project> managedProjects = employee.getRole() == EmployeeRole.Pm
                ? projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(comId, empId)
                : projectRepository.findByCompany_ComIdAndDesigner_EmpIdOrderByProjectIdDesc(comId, empId);
        managedProjects.forEach(project -> customerMap.put(project.getCustomer().getCustId(), project.getCustomer()));
        List<Customer> customers = List.copyOf(customerMap.values());

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        for (Customer customer : customers) {
            if (customer.getStatus() == CustStatus.Lost
                    || customer.getStatus() == CustStatus.Signed
                    || customer.getStatus() == CustStatus.Serving
                    || customer.getStatus() == CustStatus.Finished
                    || customer.getStatus() == CustStatus.Dormant) {
                continue;
            }
            LocalDate nextDate = customer.getNextFollowAt() == null ? null : customer.getNextFollowAt().toLocalDate();
            // 跟进逾期只在预约量房前成立；预约量房后不再生成逾期通知（今日/超期未跟进另按原口径）。
            if (nextDate != null && nextDate.isBefore(today)
                    && CustomerInsightContext.followOverdueEligible(customer)) {
                upsert(employee, null, customer, null,
                        "follow-overdue:" + customer.getCustId() + ":" + today,
                        NoticeType.Overdue, "客户跟进已逾期",
                        customer.getName() + " · 原计划 " + nextDate + " · 请尽快联系");
            } else if (nextDate != null && nextDate.isEqual(today)) {
                upsert(employee, null, customer, null,
                        "follow-today:" + customer.getCustId() + ":" + today,
                        NoticeType.Customer, "今日待跟进",
                        customer.getName() + " · " + (customer.getCommunity() == null ? "未填小区" : customer.getCommunity())
                                + " · 请完成今日跟进");
            } else if (nextDate == null && customer.getLastFollowAt() == null) {
                upsert(employee, null, customer, null, "customer:" + customer.getCustId(),
                        NoticeType.Customer, "新客户待首联",
                        customer.getName() + " · 请完成首次联系并设置下次跟进时间");
            } else if (nextDate == null && customer.getLastFollowAt().isBefore(now.minusDays(3))) {
                upsert(employee, null, customer, null,
                        "follow-stale:" + customer.getCustId() + ":" + today,
                        NoticeType.Customer, "客户超过3天未跟进",
                        customer.getName() + " · 上次跟进 " + customer.getLastFollowAt().toLocalDate()
                                + " · 建议今天联系");
            }
        }

        for (Quote quote : quoteRepository.findByCompany_ComIdAndCreatedBy_EmpIdOrderByQuoteIdDesc(comId, empId)) {
            if ((quote.getStatus() == QuoteStatus.Draft || quote.getStatus() == QuoteStatus.Submitted)
                    && quote.getCustomer() != null
                    && quote.getCustomer().getStatus() != CustStatus.Dormant) {
                upsert(employee, null, quote.getCustomer(), quote, "quote:" + quote.getQuoteId(), NoticeType.Quote,
                        "报价待推进", quote.getNo() + " · " + quote.getCustomer().getName()
                                + " · ¥" + quote.getAmount());
            }
        }

        List<Project> projects = managedProjects.isEmpty()
                ? projectRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId,
                        customers.stream().map(Customer::getCustId).toList())
                : managedProjects;
        for (Project project : projects) {
            upsert(employee, project, project.getCustomer(), null, "project:" + project.getProjectId(), NoticeType.Project,
                    "项目进度提醒", project.getName() + " · 当前进度 " + project.getProgress() + "%");
            for (ProjectTask task : taskRepository.findByProject(project)) {
                boolean overdue = task.getPlannedEnd() != null
                        && task.getPlannedEnd().isBefore(LocalDate.now())
                        && task.getStatus() != TaskStatus.Done
                        && task.getStatus() != TaskStatus.Cancelled;
                if (overdue) {
                    upsert(employee, project, project.getCustomer(), null, "task:" + task.getTaskId(), NoticeType.Overdue,
                            "施工任务已逾期", task.getName() + " · 计划完成 " + task.getPlannedEnd());
                }
            }
        }
    }

    /** 店长通知只关注门店待指派、报价推进、项目停工和施工逾期风险。 */
    private void refreshStoreManager(Employee manager, Long comId) {
        if (manager.getStore() == null || manager.getStore().getStoreId() == null) {
            return;
        }
        Long storeId = manager.getStore().getStoreId();
        List<Customer> customers = customerRepository
                .findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(comId, storeId);
        for (Customer customer : customers) {
            if (customer.getStatus() == CustStatus.PendingMeasure
                    && customer.getMeasureAt() != null
                    && customer.getDesigner() == null) {
                upsert(manager, null, customer, null, "manager-assign:" + customer.getCustId(),
                        NoticeType.Project, "客户待指派设计师",
                        customer.getName() + " · 量房 " + customer.getMeasureAt()
                                + " · 请按设计师负荷安排接手");
            }
        }

        List<Quote> quotes = quoteRepository
                .findByCompany_ComIdAndCustomer_Store_StoreIdOrderByQuoteIdDesc(comId, storeId);
        for (Quote quote : quotes) {
            if ((quote.getStatus() == QuoteStatus.Draft || quote.getStatus() == QuoteStatus.Submitted)
                    && quote.getCustomer() != null
                    && quote.getCustomer().getStatus() != CustStatus.Dormant) {
                upsert(manager, null, quote.getCustomer(), quote, "manager-quote:" + quote.getQuoteId(),
                        NoticeType.Quote, quote.getStatus() == QuoteStatus.Draft ? "门店报价未提交" : "客户报价待确认",
                        quote.getNo() + " · " + quote.getCustomer().getName() + " · ¥" + quote.getAmount()
                                + " · 负责人 " + (quote.getCreatedBy() == null ? "未指定" : quote.getCreatedBy().getName()));
            }
        }

        List<Project> projects = projectRepository
                .findByCompany_ComIdAndCustomer_Store_StoreIdOrderByProjectIdDesc(comId, storeId);
        for (Project project : projects) {
            if (project.getStatus() == com.th4.edge.decoboon.project.configure.ProjectStatus.Suspended) {
                upsert(manager, project, project.getCustomer(), null, "manager-project:" + project.getProjectId(),
                        NoticeType.Project, "门店项目停工",
                        project.getName() + " · 当前进度 " + project.getProgress() + "% · 请核查停工原因");
            }
            for (ProjectTask task : taskRepository.findByProject(project)) {
                boolean overdue = task.getPlannedEnd() != null
                        && task.getPlannedEnd().isBefore(LocalDate.now())
                        && task.getStatus() != TaskStatus.Done
                        && task.getStatus() != TaskStatus.Cancelled;
                if (overdue) {
                    upsert(manager, project, project.getCustomer(), null, "manager-task:" + task.getTaskId(),
                            NoticeType.Overdue, "门店施工任务逾期",
                            project.getName() + " · " + task.getName() + " · 计划完成 " + task.getPlannedEnd());
                }
            }
        }
    }

    /** 团队负责人通知聚焦团队待派单、报价推进、停工和任务逾期。 */
    private void refreshTeamLeader(Employee leader, Long comId) {
        if (leader.getTeam() == null || leader.getTeam().getTeamId() == null) {
            return;
        }
        Long teamId = leader.getTeam().getTeamId();
        List<Customer> customers = customerRepository
                .findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(comId, teamId);
        for (Customer customer : customers) {
            if (customer.getStatus() == CustStatus.PendingMeasure
                    && customer.getMeasureAt() != null
                    && customer.getDesigner() == null) {
                upsert(leader, null, customer, null, "team-assign:" + customer.getCustId(),
                        NoticeType.Project, "团队客户待指派设计师",
                        customer.getName() + " · 量房 " + customer.getMeasureAt()
                                + " · 请按团队设计师负荷安排接手");
            }
        }

        List<Quote> quotes = quoteRepository
                .findByCompany_ComIdAndCustomer_Team_TeamIdOrderByQuoteIdDesc(comId, teamId);
        for (Quote quote : quotes) {
            if ((quote.getStatus() == QuoteStatus.Draft || quote.getStatus() == QuoteStatus.Submitted)
                    && quote.getCustomer() != null
                    && quote.getCustomer().getStatus() != CustStatus.Dormant) {
                upsert(leader, null, quote.getCustomer(), quote, "team-quote:" + quote.getQuoteId(),
                        NoticeType.Quote, quote.getStatus() == QuoteStatus.Draft ? "团队报价未提交" : "团队报价待确认",
                        quote.getNo() + " · " + quote.getCustomer().getName() + " · ¥" + quote.getAmount()
                                + " · 负责人 " + (quote.getCreatedBy() == null ? "未指定" : quote.getCreatedBy().getName()));
            }
        }

        List<Project> projects = projectRepository
                .findByCompany_ComIdAndCustomer_Team_TeamIdOrderByProjectIdDesc(comId, teamId);
        for (Project project : projects) {
            if (project.getStatus() == com.th4.edge.decoboon.project.configure.ProjectStatus.Suspended) {
                upsert(leader, project, project.getCustomer(), null, "team-project:" + project.getProjectId(),
                        NoticeType.Project, "团队项目停工",
                        project.getName() + " · 当前进度 " + project.getProgress() + "% · 请核查停工原因");
            }
            for (ProjectTask task : taskRepository.findByProject(project)) {
                boolean overdue = task.getPlannedEnd() != null
                        && task.getPlannedEnd().isBefore(LocalDate.now())
                        && task.getStatus() != TaskStatus.Done
                        && task.getStatus() != TaskStatus.Cancelled;
                if (overdue) {
                    upsert(leader, project, project.getCustomer(), null, "team-task:" + task.getTaskId(),
                            NoticeType.Overdue, "团队施工任务逾期",
                            project.getName() + " · " + task.getName() + " · 计划完成 " + task.getPlannedEnd());
                }
            }
        }
    }

    /** 业主确认或驳回后，通知项目内部履约人员。 */
    @Transactional
    public void notifyAcceptanceDecision(Acceptance acceptance) {
        if (acceptance == null || acceptance.getProject() == null) {
            return;
        }
        Project project = acceptance.getProject();
        var receivers = new LinkedHashMap<Long, Employee>();
        addReceiver(receivers, project.getManager());
        addReceiver(receivers, project.getDesigner());
        addReceiver(receivers, project.getSupport());
        addReceiver(receivers, acceptance.getSubmittedBy());
        if (project.getProperty() != null) {
            addReceiver(receivers, project.getProperty().getOwner());
        }
        if (project.getCustomer() != null) {
            addReceiver(receivers, project.getCustomer().getOwner());
        }
        boolean confirmed = acceptance.getStatus() == AcceptStatus.Confirmed;
        String status = confirmed ? "确认" : "驳回";
        for (Employee receiver : new LinkedHashSet<>(receivers.values())) {
            upsert(receiver, project, project.getCustomer(), null,
                    "acceptance:" + acceptance.getAcceptanceId() + ":" + acceptance.getStatus(),
                    NoticeType.Project,
                    "业主已" + status + "阶段验收",
                    project.getName() + " · " + acceptance.getStage().getName() + " · 业主已" + status);
            // 站内通知与微信订阅消息同时发；订阅消息未授权时在触达上下文内静默跳过。
            subscribeMessageContext.acceptanceDecision(receiver, acceptance, confirmed);
        }
    }

    private void addReceiver(Map<Long, Employee> receivers, Employee employee) {
        if (employee != null && employee.getEmpId() != null) {
            receivers.putIfAbsent(employee.getEmpId(), employee);
        }
    }

    private void upsert(Employee employee, Project project, Customer customer, Quote quote,
                        String bizKey, NoticeType type, String title, String content) {
        Notice notice = noticeRepository.findByReceiver_EmpIdAndBizKey(employee.getEmpId(), bizKey)
                .orElseGet(Notice::new);
        boolean existing = notice.getNoticeId() != null;
        notice.setCompany(employee.getCompany());
        notice.setReceiver(employee);
        notice.setProject(project);
        notice.setCustomer(customer);
        notice.setQuote(quote);
        notice.setBizKey(bizKey);
        notice.setType(type);
        notice.setTitle(title);
        notice.setContent(content);
        if (!existing) {
            notice.setRead(false);
        }
        noticeRepository.save(notice);
    }

    private String formatTime(LocalDateTime value) {
        return value == null ? "待确认" : DATE_TIME_FORMATTER.format(value);
    }
}

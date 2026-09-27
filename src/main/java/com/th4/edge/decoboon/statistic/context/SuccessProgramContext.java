package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
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
import com.th4.edge.decoboon.statistic.model.RoleActivation;
import com.th4.edge.decoboon.statistic.model.SuccessMilestone;
import com.th4.edge.decoboon.statistic.model.SuccessProgramReport;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 企业用户成功只读投影。
 *
 * <p>只根据已有业务事实推导激活与流失风险，不新增业务状态，不改变任何客户、项目、金额、
 * 权限或状态机。激活分数只用于运营支持，不作为对外经营指标。</p>
 */
@Component
public class SuccessProgramContext {

    private final TenantContext tenantContext;
    private final CatalogItemRepository catalogItemRepository;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;
    private final ProjectRepository projectRepository;
    private final MaterialSelectionRepository selectionRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;
    private final AcceptanceRepository acceptanceRepository;
    private final EmployeeRepository employeeRepository;

    public SuccessProgramContext(TenantContext tenantContext,
                                 CatalogItemRepository catalogItemRepository,
                                 CustomerRepository customerRepository,
                                 PropertyRepository propertyRepository,
                                 QuoteRepository quoteRepository,
                                 ProjectContractRepository contractRepository,
                                 ProjectRepository projectRepository,
                                 MaterialSelectionRepository selectionRepository,
                                 ReceivableRepository receivableRepository,
                                 PayableRepository payableRepository,
                                 AcceptanceRepository acceptanceRepository,
                                 EmployeeRepository employeeRepository) {
        this.tenantContext = tenantContext;
        this.catalogItemRepository = catalogItemRepository;
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
        this.projectRepository = projectRepository;
        this.selectionRepository = selectionRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
        this.acceptanceRepository = acceptanceRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional(readOnly = true)
    public SuccessProgramReport report(Employee principal) {
        Employee current = tenantContext.requireRole(principal, EmployeeRole.Boss);
        Company company = current.getCompany();
        if (company == null || company.getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "当前员工未归属企业"));
        }
        Long companyId = company.getComId();
        LocalDate startDate = company.getCreateTime() == null
                ? LocalDate.now() : company.getCreateTime().toLocalDate();
        long days = Math.max(0, ChronoUnit.DAYS.between(startDate, LocalDate.now()));

        List<CatalogItem> catalogItems = catalogItems(companyId);
        List<Employee> employees = activeEmployees(companyId);
        List<Customer> customers = customerRepository.findByCompany_ComId(companyId);
        List<Property> properties = properties(companyId, customers);
        List<Quote> quotes = quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(companyId);
        List<ProjectContract> contracts = contractRepository.findByCompany_ComIdOrderByContractIdDesc(companyId);
        List<Project> projects = projectRepository.findByCompany_ComIdOrderByProjectIdDesc(companyId);
        List<?> selections = selections(companyId, properties);
        int financeCount = receivableRepository.findByCompany_ComIdOrderByDueDateAsc(companyId).size()
                + payableRepository.findByCompany_ComIdOrderByDueDateAsc(companyId).size();
        List<Acceptance> acceptances = acceptanceRepository.findByCompany_ComId(companyId);

        int catalogCount = catalogItems.size();
        int costCount = (int) catalogItems.stream()
                .map(CatalogItem::getCostPrice)
                .filter(Objects::nonNull)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .count();
        int customerPropertyCount = (int) properties.stream()
                .map(Property::getCustomer)
                .filter(Objects::nonNull)
                .map(Customer::getCustId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        int memberCount = (int) employees.stream()
                .filter(employee -> employee.getRole() != EmployeeRole.Boss)
                .count();
        int effectiveContractCount = (int) contracts.stream()
                .filter(this::contractEffective)
                .count();

        List<SuccessMilestone> milestones = List.of(
                milestone("catalog", "维护报价基础", "至少建立 1 个有效报价项目。", 7,
                        catalogCount, days, "维护报价项目库", "/subpackages/quote/pages/catalog/manage"),
                milestone("customerProperty", "建立首位客户和房屋", "客户必须有具体房屋，后续量房和报价才能推进。", 7,
                        customerPropertyCount, days, "新建客户", "/subpackages/customer/pages/edit"),
                milestone("members", "邀请关键成员", "至少邀请 1 名非企业负责人的员工。", 7,
                        memberCount, days, "邀请员工", "/subpackages/company/pages/invite"),
                milestone("quote", "完成首份报价", "形成第一份真实报价，连接设计与商务。", 14,
                        quotes.size(), days, "报价工作台", "/subpackages/quote/pages/workbench"),
                milestone("contract", "完成首份合同生效", "报价确认后完成合同签约和生效。", 14,
                        effectiveContractCount, days, "查看合同", "/subpackages/project/pages/contract"),
                milestone("project", "创建首个项目", "合同生效后进入交付，形成项目主线。", 14,
                        projects.size(), days, "查看项目", "/pages/project/list"),
                milestone("selection", "完成主材选材确认", "至少有一条主材选材记录。", 30,
                        selections.size(), days, "选材工作台", "/subpackages/project/pages/selection-board"),
                milestone("finance", "形成首笔应收或应付", "合同或采购后形成财务事实。", 30,
                        financeCount, days, "财务工作台", "/pages/finance/index"),
                milestone("acceptance", "完成首次验收留痕", "阶段验收或业主确认形成交付证据。", 30,
                        acceptances.size(), days, "履约工作台", "/subpackages/team/pages/workbench")
        );

        int score = score(catalogCount, costCount, customerPropertyCount, quotes.size(),
                effectiveContractCount, projects.size(), selections.size(), financeCount);
        RoleSummary roles = roles(employees, customers, properties, quotes, projects, financeCount);
        ChurnRisk risk = risk(days, customerPropertyCount, quotes.size(), effectiveContractCount,
                projects.size(), selections.size(), financeCount, membersConfigured(employees), roles.rate());
        ActivationStatus activationStatus = activationStatus(score, projects.size(), risk.level());

        SuccessMilestone next = nextMilestone(milestones);
        SuccessProgramReport report = new SuccessProgramReport();
        report.setCompanyName(company.getTitle() == null || company.getTitle().isBlank()
                ? company.getName() : company.getTitle());
        report.setStartDate(startDate);
        report.setDaysSinceCreated(days);
        report.setActivationStatus(activationStatus);
        report.setActivationScore(score);
        report.setRoleActivationRate(roles.rate());
        report.setRiskLevel(risk.level());
        report.setRiskReasons(risk.reasons());
        report.setMilestones(milestones);
        report.setRoleActivations(roles.items());
        if (next != null) {
            report.setNextAction(next.getKey());
            report.setNextActionLabel(next.getActionLabel());
            report.setNextActionPath(next.getActionPath());
        }
        report.setRefreshedAt(LocalDateTime.now());
        return report;
    }

    private List<CatalogItem> catalogItems(Long companyId) {
        List<CatalogItem> result = new ArrayList<>();
        for (ItemType type : ItemType.values()) {
            result.addAll(catalogItemRepository
                    .findByCompany_ComIdAndTypeAndActiveTrueOrderByCategoryAscBrandAscSeriesAscNameAsc(companyId, type));
        }
        return result;
    }

    private List<Property> properties(Long companyId, List<Customer> customers) {
        List<Long> customerIds = customers.stream()
                .map(Customer::getCustId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return customerIds.isEmpty() ? List.of()
                : propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(companyId, customerIds);
    }

    private List<?> selections(Long companyId, List<Property> properties) {
        List<Long> propertyIds = properties.stream()
                .map(Property::getPropertyId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return propertyIds.isEmpty() ? List.of()
                : selectionRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(companyId, propertyIds);
    }

    private List<Employee> activeEmployees(Long companyId) {
        return employeeRepository.findByCompany_ComId(companyId).stream()
                .filter(employee -> employee.getStatus() == EmployeeStatus.Active)
                .toList();
    }

    private SuccessMilestone milestone(String key, String label, String description, int targetDay,
                                       int count, long days, String actionLabel, String actionPath) {
        SuccessMilestone milestone = new SuccessMilestone();
        milestone.setKey(key);
        milestone.setLabel(label);
        milestone.setDescription(description);
        milestone.setTargetDay(targetDay);
        milestone.setCount(count);
        milestone.setActionLabel(actionLabel);
        milestone.setActionPath(actionPath);
        milestone.setStatus(count > 0 ? MilestoneStatus.Done
                : days >= targetDay ? MilestoneStatus.Overdue : MilestoneStatus.Todo);
        return milestone;
    }

    private SuccessMilestone nextMilestone(List<SuccessMilestone> milestones) {
        return milestones.stream()
                .filter(item -> item.getStatus() == MilestoneStatus.Overdue)
                .findFirst()
                .orElseGet(() -> milestones.stream()
                        .filter(item -> item.getStatus() == MilestoneStatus.Todo)
                        .findFirst()
                        .orElse(null));
    }

    private int score(int catalog, int cost, int customerProperty, int quote, int contract,
                      int project, int selection, int finance) {
        int score = 0;
        score += catalog > 0 ? 10 : 0;
        score += cost > 0 ? 10 : 0;
        score += customerProperty > 0 ? 15 : 0;
        score += quote > 0 ? 15 : 0;
        score += contract > 0 ? 15 : 0;
        score += project > 0 ? 15 : 0;
        score += selection > 0 ? 10 : 0;
        score += finance > 0 ? 10 : 0;
        return Math.min(100, score);
    }

    private ActivationStatus activationStatus(int score, int projects, ChurnRiskLevel risk) {
        if (risk == ChurnRiskLevel.High) return ActivationStatus.Risk;
        if (score >= 75 && projects > 0) return ActivationStatus.Activated;
        if (score >= 35) return ActivationStatus.InProgress;
        return ActivationStatus.NotActivated;
    }

    private RoleSummary roles(List<Employee> employees,
                              List<Customer> customers,
                              List<Property> properties,
                              List<Quote> quotes,
                              List<Project> projects,
                              int financeCount) {
        Map<EmployeeRole, Integer> configured = new EnumMap<>(EmployeeRole.class);
        for (Employee employee : employees) {
            configured.merge(employee.getRole(), 1, Integer::sum);
        }
        List<RoleActivation> items = new ArrayList<>();
        int active = 0;
        for (EmployeeRole role : EmployeeRole.values()) {
            int configuredCount = configured.getOrDefault(role, 0);
            if (configuredCount == 0) continue;
            RoleEvidence evidence = evidence(role, customers, properties, quotes, projects, financeCount);
            if (evidence.activated()) active++;
            RoleActivation item = new RoleActivation();
            item.setRole(role.name());
            item.setLabel(role.getName());
            item.setConfiguredCount(configuredCount);
            item.setActiveCount(evidence.activeCount());
            item.setActivated(evidence.activated());
            item.setEvidence(evidence.text());
            items.add(item);
        }
        int rate = configured.isEmpty() ? 0 : (int) Math.round(active * 100.0 / configured.size());
        return new RoleSummary(items, rate);
    }

    private RoleEvidence evidence(EmployeeRole role, List<Customer> customers, List<Property> properties,
                                  List<Quote> quotes, List<Project> projects, int financeCount) {
        return switch (role) {
            case Boss -> new RoleEvidence(hasBusiness(customers, properties, quotes, projects, financeCount),
                    hasBusiness(customers, properties, quotes, projects, financeCount) ? 1 : 0,
                    "报价、客户或项目存在业务事实");
            case StoreManager -> new RoleEvidence(!customers.isEmpty() || !projects.isEmpty(),
                    !customers.isEmpty() || !projects.isEmpty() ? 1 : 0, "门店存在客户或项目责任");
            case TeamLeader -> new RoleEvidence(!customers.isEmpty() || !projects.isEmpty(),
                    !customers.isEmpty() || !projects.isEmpty() ? 1 : 0, "团队存在客户或项目责任");
            case Marketer -> {
                long count = customers.stream().filter(customer -> customer.getOwner() != null
                        && customer.getOwner().getRole() == EmployeeRole.Marketer).count();
                yield new RoleEvidence(count > 0, (int) count, "业务员已形成客户归属");
            }
            case Designer -> {
                long count = properties.stream().filter(property -> property.getDesigner() != null
                        && property.getDesigner().getRole() == EmployeeRole.Designer).count();
                yield new RoleEvidence(count > 0, (int) count, "设计师已接手房屋");
            }
            case Pm -> {
                long count = projects.stream().filter(project -> project.getManager() != null
                        && project.getManager().getRole() == EmployeeRole.Pm).count();
                yield new RoleEvidence(count > 0, (int) count, "项目经理已接手项目");
            }
            case Finance -> new RoleEvidence(financeCount > 0, financeCount > 0 ? 1 : 0,
                    "财务侧已有应收或应付事实");
        };
    }

    private boolean hasBusiness(List<Customer> customers, List<Property> properties,
                                List<Quote> quotes, List<Project> projects, int financeCount) {
        return !customers.isEmpty() || !properties.isEmpty() || !quotes.isEmpty()
                || !projects.isEmpty() || financeCount > 0;
    }

    private int membersConfigured(List<Employee> employees) {
        return (int) employees.stream().filter(item -> item.getRole() != EmployeeRole.Boss).count();
    }

    private ChurnRisk risk(long days, int customerProperty, int quotes, int contracts,
                           int projects, int selections, int finance, int members, int roleRate) {
        List<String> reasons = new ArrayList<>();
        boolean high = false;
        boolean medium = false;
        if (days >= 7 && customerProperty == 0) {
            reasons.add("第 7 天仍未建立客户和房屋");
            high = true;
        }
        if (days >= 7 && members == 0) {
            reasons.add("除企业负责人外尚未邀请关键成员");
            medium = true;
        }
        if (days >= 14 && quotes == 0) {
            reasons.add("第 14 天仍没有报价");
            high = true;
        }
        if (days >= 14 && contracts == 0) {
            reasons.add("第 14 天仍没有生效合同");
            high = true;
        }
        if (days >= 14 && projects == 0) {
            reasons.add("第 14 天仍未创建项目");
            high = true;
        }
        if (days >= 30 && selections == 0) {
            reasons.add("第 30 天仍没有主材选材记录");
            medium = true;
        }
        if (days >= 30 && finance == 0) {
            reasons.add("第 30 天仍没有应收或应付事实");
            medium = true;
        }
        if (days >= 7 && roleRate < 50) {
            reasons.add("已配置岗位的业务激活率低于 50%");
            medium = true;
        }
        ChurnRiskLevel level = high ? ChurnRiskLevel.High : medium ? ChurnRiskLevel.Medium : ChurnRiskLevel.Low;
        return new ChurnRisk(level, reasons);
    }

    private boolean contractEffective(ProjectContract contract) {
        ContractStatus status = contract.getStatus();
        return status == ContractStatus.Effective || status == ContractStatus.Active
                || status == ContractStatus.Changing || status == ContractStatus.Changed
                || status == ContractStatus.Completed || status == ContractStatus.Settled;
    }

    private record RoleSummary(List<RoleActivation> items, int rate) {}
    private record RoleEvidence(boolean activated, int activeCount, String text) {}
    private record ChurnRisk(ChurnRiskLevel level, List<String> reasons) {}
}

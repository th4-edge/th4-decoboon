package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.finance.repository.ReceivableRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.statistic.configure.GuideStatus;
import com.th4.edge.decoboon.statistic.configure.GuideStepStatus;
import com.th4.edge.decoboon.statistic.model.GuideStep;
import com.th4.edge.decoboon.statistic.model.OnboardingGuide;
import com.th4.edge.decoboon.supply.configure.PartnerStatus;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.context.UserIdentityContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.function.Supplier;

/** 全角色启动引导状态推导。 */
@Component
public class OnboardingGuideContext {

    private static final Logger log = LoggerFactory.getLogger(OnboardingGuideContext.class);

    private final TenantContext tenantContext;
    private final UserIdentityContext userIdentityContext;
    private final CustomerAccessContext customerAccessContext;
    private final CatalogItemRepository catalogItemRepository;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final PartnerRepository partnerRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectRepository projectRepository;
    private final ReceivableRepository receivableRepository;
    private final PayableRepository payableRepository;

    public OnboardingGuideContext(TenantContext tenantContext,
                                  UserIdentityContext userIdentityContext,
                                  CustomerAccessContext customerAccessContext,
                                  CatalogItemRepository catalogItemRepository,
                                  CustomerRepository customerRepository,
                                  PropertyRepository propertyRepository,
                                  PartnerRepository partnerRepository,
                                  EmployeeRepository employeeRepository,
                                  ProjectRepository projectRepository,
                                  ReceivableRepository receivableRepository,
                                  PayableRepository payableRepository) {
        this.tenantContext = tenantContext;
        this.userIdentityContext = userIdentityContext;
        this.customerAccessContext = customerAccessContext;
        this.catalogItemRepository = catalogItemRepository;
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.partnerRepository = partnerRepository;
        this.employeeRepository = employeeRepository;
        this.projectRepository = projectRepository;
        this.receivableRepository = receivableRepository;
        this.payableRepository = payableRepository;
    }

    /** 当前登录主体只能是员工或微信业主身份。 */
    public OnboardingGuide guide(User<?> principal) {
        if (principal instanceof Employee employee) {
            return employeeGuide(tenantContext.requireEmployee(employee));
        }
        if (principal == null || principal.getUserId() == null) {
            throw new DefinedException(new DefaultErrorCode("401", "登录状态已失效"));
        }
        Credentials credentials = userIdentityContext.requireUser(Long.valueOf(principal.getUserId().toString()));
        return customerGuide(credentials);
    }

    private OnboardingGuide employeeGuide(Employee employee) {
        return switch (employee.getRole()) {
            case Boss -> bossGuide(employee);
            case StoreManager -> storeManagerGuide(employee);
            case TeamLeader -> teamLeaderGuide(employee);
            case Marketer -> marketerGuide(employee);
            case Designer -> designerGuide(employee);
            case Pm -> projectManagerGuide(employee);
            case Finance -> financeGuide(employee);
        };
    }

    private OnboardingGuide bossGuide(Employee employee) {
        Long comId = companyId(employee);
        List<GuideStep> steps = new ArrayList<>();
        steps.add(safeStep("catalog", "维护报价项目库", "建立可报价项目，填写单位、计价方式和销售价。",
                true, "catalog/item/page", "当前企业至少存在 1 个 active 报价项目",
                "boss.catalog", "维护报价项目库", () -> positiveStep(activeCatalogCount(comId))));
        steps.add(safeStep("cost", "维护标准成本价", "成本价直接维护在报价项目，不要求先建供应商。",
                true, "catalog/item/page", "当前企业至少存在 1 个 active 且 costPrice > 0 的报价项目",
                "boss.cost", "维护标准成本价", () -> positiveStep(activeCostItemCount(comId))));
        steps.add(safeStep("customer", "创建第一个客户及房屋", "客户建档后继续建立房屋，客户没有房屋时步骤不会完成。",
                true, "customer/customer/page + property/property/page", "至少 1 个客户且该客户关联至少 1 套房屋",
                "boss.customer", "建立客户及房屋", () -> positiveStep(customerWithPropertyCount(comId))));
        steps.add(safeStep("partner", "维护供应商品牌关系", "需要采购时再维护；供应商未关联品牌时不计完成。",
                false, "partner/page", "active 材料供应商至少 1 个且存在非空品牌",
                "boss.partner", "维护供应商品牌", () -> positiveStep(partnerBrandCount(comId))));
        steps.add(safeStep("member", "邀请关键成员", "按需要邀请店长、业务员、设计师、项目经理、材料和财务。",
                false, "company/member/page", "至少 1 个非 Boss 的 active 成员",
                "boss.member", "邀请关键成员", () -> positiveStep(nonBossMemberCount(comId))));
        return aggregate("Boss", "企业负责人", "开始使用装得清", "先完成报价基础和第一个客户，再按需要补充供应商。", steps);
    }

    private OnboardingGuide storeManagerGuide(Employee employee) {
        if (employee.getStore() == null) {
            return aggregate("StoreManager", "店长", "门店岗位启动", "当前账号尚未绑定门店。",
                    List.of(waitingStep("store", "确认门店经营范围", "等待门店配置后再分配客户和成员。", "store.scope", "查看客户", "门店绑定")));
        }
        Long comId = companyId(employee);
        Long storeId = employee.getStore().getStoreId();
        return aggregate("StoreManager", "店长", "门店岗位启动", "从本门店真实客户责任开始。",
                List.of(safeStep("store", "确认门店经营范围", "建立或接手本门店的第一个客户，形成责任事实。",
                        true, "customer/customer/page", "本门店至少存在 1 个客户责任事实",
                        "store.customers", "查看门店客户", () -> positiveStep(customerRepository
                                .findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(comId, storeId).size()))));
    }

    private OnboardingGuide teamLeaderGuide(Employee employee) {
        if (employee.getTeam() == null) {
            return aggregate("TeamLeader", "团队负责人", "团队岗位启动", "当前账号尚未绑定团队。",
                    List.of(waitingStep("team", "确认团队经营范围", "等待团队配置后再分配客户和成员。", "team.scope", "查看团队", "团队绑定")));
        }
        Long comId = companyId(employee);
        Long teamId = employee.getTeam().getTeamId();
        return aggregate("TeamLeader", "团队负责人", "团队岗位启动", "从本团队真实客户责任开始。",
                List.of(safeStep("team", "确认团队经营范围", "建立或接手本团队的第一个客户，形成责任事实。",
                        true, "customer/customer/page", "本团队至少存在 1 个客户责任事实",
                        "team.customers", "查看团队客户", () -> positiveStep(customerRepository
                                .findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(comId, teamId).size()))));
    }

    private OnboardingGuide marketerGuide(Employee employee) {
        Long comId = companyId(employee);
        return aggregate("Marketer", "业务员", "岗位首任务", "先建立自己的客户和房屋，再进入跟进与量房。",
                List.of(safeStep("marketer.customer", "建立首个客户及房屋", "完成客户建档并至少建立一套房屋。",
                        true, "customer/customer/page + property/property/page", "本人至少 1 个客户且该客户关联至少 1 套房屋",
                        "marketer.customer", "新建客户", () -> positiveStep(ownedCustomerWithPropertyCount(comId, employee.getEmpId())))));
    }

    private OnboardingGuide designerGuide(Employee employee) {
        Long comId = companyId(employee);
        return aggregate("Designer", "设计师", "岗位首任务", "等待设计调度或继续处理已分配房屋。",
                List.of(safeStep("designer.property", "确认设计调度", "被指派房屋后进入设计工作台处理诉求、方案和报价。",
                        true, "property/property/page", "本人至少被分配 1 套房屋",
                        "designer.workbench", "进入设计工作台", () -> positiveOrWaitingStep(propertyRepository
                                .findByCompany_ComIdAndDesigner_EmpIdOrderByPropertyIdDesc(comId, employee.getEmpId()).size()))));
    }

    private OnboardingGuide projectManagerGuide(Employee employee) {
        Long comId = companyId(employee);
        return aggregate("Pm", "项目经理", "岗位首任务", "接单后确认计划并推进首个阶段。",
                List.of(safeStep("pm.project", "接收并确认项目", "有负责项目时进入 PM 工作台；存在已签约待接单项目时及时接收。",
                        true, "project/project/page", "本人至少负责 1 个项目，或存在待接单项目",
                        "pm.workbench", "进入 PM 工作台", () -> {
                            int assigned = projectRepository.findByCompany_ComIdAndManager_EmpIdOrderByProjectIdDesc(
                                    comId, employee.getEmpId()).size();
                            if (assigned > 0) return new StepFact(assigned, GuideStepStatus.Done);
                            int pending = (int) projectRepository.findByCompany_ComIdOrderByProjectIdDesc(comId).stream()
                                    .filter(project -> project.getManager() == null)
                                    .count();
                            return pending > 0
                                    ? new StepFact(pending, GuideStepStatus.Todo)
                                    : new StepFact(0, GuideStepStatus.Waiting);
                        })));
    }

    private OnboardingGuide financeGuide(Employee employee) {
        Long comId = companyId(employee);
        return aggregate("Finance", "财务", "岗位首任务", "从当前企业真实应收应付和项目履约协同开始处理。",
                List.of(safeStep("finance.liability", "确认首笔应收应付", "合同生效、采购到货等业务事实形成后自动进入财务台账。",
                        true, "finance/receivable/page + finance/payable/page", "当前企业至少存在 1 条应收或应付事实",
                        "finance.home", "进入财务工作台", () -> positiveStep(
                                receivableRepository.findByCompany_ComIdOrderByDueDateAsc(comId).size()
                                        + payableRepository.findByCompany_ComIdOrderByDueDateAsc(comId).size()))));
    }

    private OnboardingGuide customerGuide(Credentials credentials) {
        return aggregate("Customer", "业主", "我的家", "仅展示本人已授权项目；有项目后查看动态和验收。", List.of(
                safeStep("customer.project", "确认项目授权", "接受有效邀请后查看本人项目、施工动态和待确认验收。",
                        true, "customer/project/page", "当前微信身份至少存在 1 条 active 项目授权",
                        "customer.projects", "查看我的家", () -> {
                            List<CustomerAccess> accesses = customerAccessContext.activeAccesses(credentials);
                            long count = accesses.stream().map(CustomerAccess::getProject)
                                    .filter(Objects::nonNull).map(Project::getProjectId).distinct().count();
                            return count > 0
                                    ? new StepFact((int) count, GuideStepStatus.Done)
                                    : new StepFact(0, GuideStepStatus.Waiting);
                        })));
    }

    private OnboardingGuide aggregate(String audience, String audienceLabel, String title, String subtitle,
                                      List<GuideStep> steps) {
        int requiredTotal = (int) steps.stream().filter(GuideStep::isRequired).count();
        int requiredCompleted = (int) steps.stream()
                .filter(GuideStep::isRequired)
                .filter(step -> step.getStatus() == GuideStepStatus.Done)
                .count();
        boolean requiredUnknown = steps.stream()
                .anyMatch(step -> step.isRequired() && step.getStatus() == GuideStepStatus.Unknown);
        boolean optionalUnknown = steps.stream()
                .anyMatch(step -> !step.isRequired() && step.getStatus() == GuideStepStatus.Unknown);
        boolean requiredWaiting = requiredCompleted < requiredTotal && steps.stream()
                .filter(GuideStep::isRequired)
                .filter(step -> step.getStatus() != GuideStepStatus.Done)
                .allMatch(step -> step.getStatus() == GuideStepStatus.Waiting);

        GuideStatus status;
        String nextAction = null;
        String message;
        if (requiredUnknown) {
            status = GuideStatus.Unknown;
            message = "部分关键状态暂不可确认，请稍后重试。";
        } else if (requiredCompleted >= requiredTotal) {
            status = GuideStatus.Completed;
            message = optionalUnknown ? "启动引导已完成；部分可选状态暂不可确认。" : "启动引导已完成。";
        } else if (requiredWaiting) {
            status = GuideStatus.WaitingAssignment;
            nextAction = nextAction(steps);
            message = "当前岗位还没有可执行责任对象，请等待分配。";
        } else {
            status = GuideStatus.ActionRequired;
            nextAction = nextAction(steps);
            message = "按当前下一步继续即可，完成后返回首页会自动刷新。";
        }

        OnboardingGuide guide = new OnboardingGuide();
        guide.setAudience(audience);
        guide.setAudienceLabel(audienceLabel);
        guide.setTitle(title);
        guide.setSubtitle(subtitle);
        guide.setStatus(status);
        guide.setRequiredTotal(requiredTotal);
        guide.setRequiredCompleted(requiredCompleted);
        guide.setNextAction(nextAction);
        guide.setMessage(message);
        guide.setSteps(steps);
        guide.setRefreshedAt(LocalDateTime.now());
        return guide;
    }

    private String nextAction(List<GuideStep> steps) {
        return steps.stream()
                .filter(GuideStep::isRequired)
                .filter(step -> step.getStatus() == GuideStepStatus.Todo || step.getStatus() == GuideStepStatus.Waiting)
                .map(GuideStep::getAction)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private GuideStep safeStep(String key, String label, String description, boolean required,
                               String sourceProvider, String completionPredicate,
                               String action, String actionLabel, Supplier<StepFact> supplier) {
        try {
            StepFact fact = supplier.get();
            return step(key, label, description, required, sourceProvider, completionPredicate,
                    action, actionLabel, fact.count(), fact.status());
        } catch (RuntimeException exception) {
            log.debug("启动引导数据源暂不可用: {}", sourceProvider, exception);
            return step(key, label, description, required, sourceProvider, completionPredicate,
                    action, actionLabel, null, GuideStepStatus.Unknown);
        }
    }

    private GuideStep waitingStep(String key, String label, String description,
                                  String action, String actionLabel, String predicate) {
        return step(key, label, description, true, "tenant/context", predicate,
                action, actionLabel, 0, GuideStepStatus.Waiting);
    }

    private GuideStep step(String key, String label, String description, boolean required,
                           String sourceProvider, String completionPredicate,
                           String action, String actionLabel, Integer count, GuideStepStatus status) {
        GuideStep step = new GuideStep();
        step.setKey(key);
        step.setLabel(label);
        step.setDescription(description);
        step.setRequired(required);
        step.setSourceProvider(sourceProvider);
        step.setCompletionPredicate(completionPredicate);
        step.setAction(action);
        step.setActionLabel(actionLabel);
        step.setCount(count);
        step.setStatus(status);
        return step;
    }

    private StepFact positiveStep(long count) {
        int value = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        return new StepFact(value, count > 0 ? GuideStepStatus.Done : GuideStepStatus.Todo);
    }

    private StepFact positiveOrWaitingStep(long count) {
        int value = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
        return new StepFact(value, count > 0 ? GuideStepStatus.Done : GuideStepStatus.Waiting);
    }

    private long activeCatalogCount(Long comId) {
        return Arrays.stream(ItemType.values())
                .map(type -> catalogItemRepository.findByCompany_ComIdAndTypeAndActiveTrueOrderByCategoryAscBrandAscSeriesAscNameAsc(comId, type))
                .flatMap(List::stream)
                .count();
    }

    private long activeCostItemCount(Long comId) {
        return Arrays.stream(ItemType.values())
                .map(type -> catalogItemRepository.findByCompany_ComIdAndTypeAndActiveTrueOrderByCategoryAscBrandAscSeriesAscNameAsc(comId, type))
                .flatMap(List::stream)
                .map(CatalogItem::getCostPrice)
                .filter(Objects::nonNull)
                .filter(value -> value.compareTo(BigDecimal.ZERO) > 0)
                .count();
    }

    private long customerWithPropertyCount(Long comId) {
        List<Customer> customers = customerRepository.findByCompany_ComId(comId);
        List<Long> customerIds = customers.stream().map(Customer::getCustId).filter(Objects::nonNull).distinct().toList();
        if (customerIds.isEmpty()) return 0;
        return propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds).stream()
                .map(Property::getCustomer)
                .filter(Objects::nonNull)
                .map(Customer::getCustId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private long ownedCustomerWithPropertyCount(Long comId, Long empId) {
        List<Long> customerIds = customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, empId).stream()
                .map(Customer::getCustId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (customerIds.isEmpty()) return 0;
        Set<Long> customerIdSet = customerIds.stream().collect(Collectors.toSet());
        return propertyRepository.findByCompany_ComIdAndOwner_EmpIdOrderByPropertyIdDesc(comId, empId).stream()
                .map(Property::getCustomer)
                .filter(Objects::nonNull)
                .map(Customer::getCustId)
                .filter(customerIdSet::contains)
                .distinct()
                .count();
    }

    private long partnerBrandCount(Long comId) {
        List<Partner> partners = partnerRepository.findByCompany_ComIdAndTypeAndStatusOrderByName(
                comId, PartnerType.Supplier, PartnerStatus.Active);
        return partners.stream()
                .filter(partner -> partner.getBrands() != null && partner.getBrands().stream().anyMatch(value -> value != null && !value.isBlank()))
                .count();
    }

    private long nonBossMemberCount(Long comId) {
        long active = employeeRepository.countByCompany_ComIdAndStatus(comId, EmployeeStatus.Active);
        return Math.max(0, active - 1);
    }

    private Long companyId(Employee employee) {
        if (employee.getCompany() == null || employee.getCompany().getComId() == null) {
            throw new DefinedException(new DefaultErrorCode("400", "当前员工未归属企业"));
        }
        return employee.getCompany().getComId();
    }

    private record StepFact(Integer count, GuideStepStatus status) {}
}

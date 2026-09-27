package com.th4.edge.platform.integration.tenant;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.platform.model.FunnelMetric;
import com.th4.edge.platform.model.PlatformAction;
import com.th4.edge.platform.model.PlatformMetrics;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.platform.model.PlatformOperationLog;
import com.th4.edge.platform.model.PlatformTenantOnboarding;
import com.th4.edge.platform.model.PlatformTenantOwner;
import com.th4.edge.platform.model.PlatformTenantOwnerContact;
import com.th4.edge.platform.model.PlatformPlan;
import com.th4.edge.platform.model.PlatformSubscription;
import com.th4.edge.platform.model.PlatformTenant;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.model.RevenueTrend;
import com.th4.edge.platform.repository.PlatformOperationLogRepository;
import com.th4.edge.tenant.billing.configure.OrderStatus;
import com.th4.edge.tenant.billing.configure.SubStatus;
import com.th4.edge.tenant.billing.context.SubscriptionContext;
import com.th4.edge.tenant.billing.model.Order;
import com.th4.edge.tenant.billing.model.Plan;
import com.th4.edge.tenant.billing.model.PlanVersion;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.billing.repository.OrderRepository;
import com.th4.edge.tenant.billing.repository.PlanRepository;
import com.th4.edge.tenant.billing.repository.PlanVersionRepository;
import com.th4.edge.tenant.billing.repository.SubscriptionRepository;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.CredentialsRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户业务只读适配器。
 *
 * <p>平台持久化模型不引用 Company、Employee、Subscription 等租户实体。
 * 该适配器是唯一允许接触租户仓储的边界，只向平台域返回标量快照。</p>
 */
@Note("租户业务平台适配器")
@Component
public class TenantDataAdapter {

    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final StoreRepository storeRepository;
    private final CredentialsRepository credentialsRepository;
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository projectContractRepository;
    private final ProjectRepository projectRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PlanVersionRepository planVersionRepository;
    private final OrderRepository orderRepository;
    private final SubscriptionContext subscriptionContext;
    private final PlatformOperationLogRepository operationLogRepository;

    public TenantDataAdapter(CompanyRepository companyRepository,
                             EmployeeRepository employeeRepository,
                             StoreRepository storeRepository,
                             CredentialsRepository credentialsRepository,
                             CustomerRepository customerRepository,
                             QuoteRepository quoteRepository,
                             ProjectContractRepository projectContractRepository,
                             ProjectRepository projectRepository,
                             SubscriptionRepository subscriptionRepository,
                             PlanRepository planRepository,
                             PlanVersionRepository planVersionRepository,
                             OrderRepository orderRepository,
                             SubscriptionContext subscriptionContext,
                             PlatformOperationLogRepository operationLogRepository) {
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.storeRepository = storeRepository;
        this.credentialsRepository = credentialsRepository;
        this.customerRepository = customerRepository;
        this.quoteRepository = quoteRepository;
        this.projectContractRepository = projectContractRepository;
        this.projectRepository = projectRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.planVersionRepository = planVersionRepository;
        this.orderRepository = orderRepository;
        this.subscriptionContext = subscriptionContext;
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional(readOnly = true)
    public Page<PlatformTenant> pageTenants(String keyword, SubStatus status, Pageable pageable) {
        return companyRepository.pageTenants(keyword, status, pageable).map(this::tenant);
    }

    @Transactional(readOnly = true)
    public PlatformTenant tenant(Long comId) {
        Company company = companyRepository.findByComId(comId);
        if (company == null) throw new DefinedException(new DefaultErrorCode("404", "企业不存在"));
        return tenant(company);
    }

    @Transactional(readOnly = true)
    public List<PlatformTenantOnboarding> onboarding(String keyword, SubStatus status) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        List<Company> companies = companyRepository.findForOnboarding(normalized, status, EmployeeRole.Boss);
        if (companies.isEmpty()) return List.of();
        List<Long> companyIds = companies.stream().map(Company::getComId).toList();
        List<Employee> owners = employeeRepository
                .findByCompany_ComIdInAndRoleOrderByNameAsc(companyIds, EmployeeRole.Boss);
        List<Long> empIds = owners.stream().map(Employee::getEmpId).toList();
        List<Credentials> credentials = empIds.isEmpty() ? List.of()
                : credentialsRepository.findByEmployee_EmpIdIn(empIds);
        return companies.stream().map(company -> onboarding(company, owners, credentials)).toList();
    }

    @Transactional(readOnly = true)
    public PlatformTenantOnboarding onboarding(Long comId) {
        Company company = companyRepository.findByComId(comId);
        if (company == null) throw new DefinedException(new DefaultErrorCode("404", "企业不存在"));
        List<Employee> owners = employeeRepository
                .findByCompany_ComIdInAndRoleOrderByNameAsc(List.of(comId), EmployeeRole.Boss);
        List<Long> empIds = owners.stream().map(Employee::getEmpId).toList();
        List<Credentials> credentials = empIds.isEmpty() ? List.of()
                : credentialsRepository.findByEmployee_EmpIdIn(empIds);
        return onboarding(company, owners, credentials);
    }

    @Transactional(readOnly = true)
    public PlatformTenantOwnerContact ownerContact(Long comId, Long empId) {
        Employee owner = employeeRepository.findByEmpIdAndCompany_ComId(empId, comId);
        if (owner == null || owner.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("404", "企业负责人不存在或无权访问"));
        }
        if (owner.getTelNo() == null || owner.getTelNo().isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "企业负责人未维护手机号"));
        }
        PlatformTenantOwnerContact contact = new PlatformTenantOwnerContact();
        contact.setCompanyId(comId);
        contact.setEmpId(owner.getEmpId());
        contact.setOwnerName(owner.getName());
        contact.setPhone(owner.getTelNo());
        return contact;
    }

    @Transactional(readOnly = true)
    public List<PlatformPlan> plans() {
        subscriptionContext.initializePlans();
        return subscriptionContext.availablePlans().stream().map(this::plan).toList();
    }

    @Transactional(readOnly = true)
    public PlatformMetrics metrics() {
        List<Company> companies = companyRepository.findAll();
        List<Subscription> subscriptions = subscriptionRepository.findAllByOrderBySubIdDesc();
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);

        PlatformMetrics metrics = new PlatformMetrics();
        metrics.setTotalCompanies((long) companies.size());
        metrics.setTrialSubscriptions(subscriptions.stream().filter(item -> item.getStatus() == SubStatus.Trial).count());
        metrics.setActiveSubscriptions(subscriptions.stream().filter(item -> item.getStatus() == SubStatus.Active).count());
        metrics.setExpiringSoon(subscriptions.stream()
                .filter(item -> item.getStatus() == SubStatus.Active || item.getStatus() == SubStatus.Trial)
                .filter(item -> {
                    long days = remainingDays(item);
                    return days >= 0 && days <= 7;
                }).count());
        metrics.setRiskCompanies(subscriptions.stream().filter(item -> {
            long days = remainingDays(item);
            return item.getStatus() == SubStatus.Expired
                    || item.getStatus() == SubStatus.Frozen
                    || item.getStatus() == SubStatus.Closed
                    || (days >= 0 && days <= 7);
        }).count());
        metrics.setNewCompaniesThisMonth(companies.stream()
                .filter(item -> item.getCreateTime() != null && item.getCreateTime().toLocalDateTime() != null)
                .filter(item -> YearMonth.from(item.getCreateTime().toLocalDateTime()).equals(currentMonth))
                .count());

        List<Order> successfulOrders = orderRepository.findAll().stream()
                .filter(item -> item.getStatus() == OrderStatus.Granted || item.getStatus() == OrderStatus.Paid)
                .toList();
        metrics.setPaidAmountLast30Days(successfulOrders.stream()
                .filter(item -> item.getPaidTime() != null && item.getPaidTime().isAfter(now.minusDays(30)))
                .mapToLong(item -> item.getAmount() == null ? 0L : item.getAmount()).sum());
        metrics.setMrr(successfulOrders.stream()
                .filter(item -> item.getPaidTime() != null && item.getPaidTime().isAfter(now.minusDays(365)))
                .mapToLong(item -> item.getAmount() == null ? 0L : item.getAmount() / 12L).sum());
        metrics.setSeatUsageRate(usageRate(employeeRepository.count(),
                subscriptions.stream().mapToLong(this::seatCapacity).sum()));
        metrics.setStoreUsageRate(usageRate(storeRepository.count(),
                subscriptions.stream().mapToLong(this::storeCapacity).sum()));

        Map<String, Long> statusDistribution = new LinkedHashMap<>();
        for (SubStatus status : SubStatus.values()) {
            statusDistribution.put(status.name(), subscriptions.stream().filter(item -> item.getStatus() == status).count());
        }
        metrics.setSubscriptionStatus(statusDistribution);
        Map<String, Long> planDistribution = new LinkedHashMap<>();
        subscriptions.forEach(item -> planDistribution.merge(planName(item), 1L, Long::sum));
        metrics.setPlanDistribution(planDistribution);
        metrics.setFunnel(buildFunnel(companies.size(), subscriptions));
        metrics.setRevenueTrend(buildTrend(companies, successfulOrders, now));
        return metrics;
    }

    @Transactional
    public PlatformSubscription updateSubscription(PlatformUser operator,
                                                   Long subId,
                                                   Integer days,
                                                   String planCode,
                                                   SubStatus status,
                                                   Integer extraSeats,
                                                   Integer extraStores,
                                                   String reason) {
        subscriptionContext.initializePlans();
        Subscription subscription = subscriptionRepository.findBySubIdForUpdate(subId);
        if (subscription == null || subscription.getCompany() == null) {
            throw new DefinedException(new DefaultErrorCode("404", "订阅不存在"));
        }
        if (days == null && (planCode == null || planCode.isBlank()) && status == null
                && extraSeats == null && extraStores == null) {
            throw new DefinedException(new DefaultErrorCode("400", "至少提交一项订阅调整"));
        }
        Map<String, Object> before = snapshot(subscription);
        PlatformAction action = action(days, planCode, extraSeats, extraStores);
        if (days != null) {
            LocalDateTime base = subscription.getServiceEndTime() != null
                    && subscription.getServiceEndTime().toLocalDateTime().isAfter(LocalDateTime.now())
                    ? subscription.getServiceEndTime().toLocalDateTime() : LocalDateTime.now();
            subscription.setServiceEndTime(new DateTime(base.plusDays(days)));
            subscription.setLastRenewTime(LocalDateTime.now());
            if (subscription.getStatus() == null || subscription.getStatus() == SubStatus.Expired) {
                subscription.setStatus(SubStatus.Active);
            }
        }
        if (planCode != null && !planCode.isBlank()) {
            Plan plan = planRepository.findByCode(planCode.trim());
            if (plan == null || !Boolean.TRUE.equals(plan.getEnabled())) {
                throw new DefinedException(new DefaultErrorCode("400", "套餐不存在或已下架"));
            }
            PlanVersion version = planVersionRepository.findTopByPlan_PlanIdOrderByVersionNoDesc(plan.getPlanId());
            if (version == null) throw new DefinedException(new DefaultErrorCode("400", "套餐版本不存在"));
            subscription.setPlan(plan);
            subscription.setPlanVersion(version);
        }
        if (extraSeats != null) subscription.setExtraSeats(extraSeats);
        if (extraStores != null) subscription.setExtraStores(extraStores);
        if (status != null) {
            if (status == SubStatus.Active && !hasFutureService(subscription)) {
                throw new DefinedException(new DefaultErrorCode("400", "恢复为使用中前必须确保服务期未到期"));
            }
            subscription.setStatus(status);
            if (status == SubStatus.Frozen) action = PlatformAction.Freeze;
            if (status == SubStatus.Active) action = PlatformAction.Unfreeze;
        }

        Subscription saved = subscriptionRepository.save(subscription);
        PlatformOperationLog log = new PlatformOperationLog();
        log.setOrg(operator.getOrg());
        log.setOperator(operator);
        log.setAction(action);
        log.setTargetType("Subscription");
        log.setTargetKey(String.valueOf(saved.getSubId()));
        log.setTargetComId(saved.getCompany().getComId());
        log.setTargetCompanyTitle(saved.getCompany().getTitle());
        log.setTargetSubId(saved.getSubId());
        log.setSummary("人工调整企业订阅");
        log.setBeforeValue(before.toString());
        log.setAfterValue(snapshot(saved).toString());
        log.setReason(reason.trim());
        operationLogRepository.save(log);
        return subscription(saved);
    }

    private PlatformTenantOnboarding onboarding(Company company,
                                                  List<Employee> allOwners,
                                                  List<Credentials> allCredentials) {
        List<Employee> owners = allOwners.stream()
                .filter(owner -> company.getComId().equals(owner.getCompany() == null ? null : owner.getCompany().getComId()))
                .toList();
        List<Long> ownerIds = owners.stream().map(Employee::getEmpId).toList();
        List<Credentials> credentials = allCredentials.stream()
                .filter(item -> item.getEmployee() != null && ownerIds.contains(item.getEmployee().getEmpId()))
                .toList();
        Map<Long, List<Credentials>> credentialsByOwner = new LinkedHashMap<>();
        for (Credentials credential : credentials) {
            credentialsByOwner.computeIfAbsent(credential.getEmployee().getEmpId(), key -> new ArrayList<>()).add(credential);
        }

        List<Employee> activeOwners = owners.stream()
                .filter(owner -> owner.getStatus() == EmployeeStatus.Active)
                .sorted((left, right) -> {
                    LocalDateTime leftLogin = lastLogin(credentialsByOwner.get(left.getEmpId()));
                    LocalDateTime rightLogin = lastLogin(credentialsByOwner.get(right.getEmpId()));
                    if (leftLogin != null && rightLogin != null) {
                        int compared = rightLogin.compareTo(leftLogin);
                        if (compared != 0) return compared;
                    } else if (leftLogin != null) {
                        return -1;
                    } else if (rightLogin != null) {
                        return 1;
                    }
                    return Long.compare(right.getEmpId(), left.getEmpId());
                })
                .toList();
        Employee primary = activeOwners.isEmpty() ? null : activeOwners.get(0);

        PlatformTenantOnboarding value = new PlatformTenantOnboarding();
        Subscription subscription = subscriptionContext.current(company);
        value.setComId(company.getComId());
        value.setCompanyTitle(company.getTitle());
        value.setCompanyName(company.getName());
        value.setCompanySize(company.getSize() == null ? null : company.getSize().name());
        value.setCompanyAddress(company.getAddr());
        value.setCompanyCreateTime(company.getCreateTime());
        value.setPlanName(planName(subscription));
        value.setSubscriptionStatus(subscription == null || subscription.getStatus() == null ? "None" : subscription.getStatus().name());
        value.setRemainingDays(subscription == null ? null : subscriptionContext.remainingDays(subscription));
        value.setOwnerCount(activeOwners.size());
        value.setBusinessStage(businessStage(company.getComId()));
        value.setOwners(owners.stream().map(owner -> owner(owner, credentialsByOwner.get(owner.getEmpId()))).toList());
        if (primary != null) {
            List<Credentials> primaryCredentials = credentialsByOwner.getOrDefault(primary.getEmpId(), List.of());
            LocalDateTime lastLogin = lastLogin(primaryCredentials);
            value.setOwnerEmpId(primary.getEmpId());
            value.setOwnerName(primary.getName());
            value.setOwnerPhoneMasked(mask(primary.getTelNo()));
            value.setOwnerStatus(primary.getStatus() == null ? null : primary.getStatus().name());
            value.setOwnerJoinedAt(primary.getJoinedAt() == null
                    ? dateTime(primary.getCreateTime()) : primary.getJoinedAt());
            value.setAccountCreateTime(firstCredentialCreateTime(primaryCredentials));
            value.setLastLoginAt(lastLogin);
            value.setLastLoginType(lastLoginType(primaryCredentials));
            value.setDaysSinceLastLogin(lastLogin == null ? null
                    : Math.max(0L, ChronoUnit.DAYS.between(lastLogin.toLocalDate(), LocalDateTime.now().toLocalDate())));
            value.setLoginStatus(loginStatus(lastLogin));
        } else {
            value.setOwnerPhoneMasked("");
            value.setLoginStatus("未维护负责人");
        }
        value.setNextAction(defaultNextAction(value));
        return value;
    }

    private PlatformTenantOwner owner(Employee owner, List<Credentials> credentials) {
        PlatformTenantOwner item = new PlatformTenantOwner();
        item.setEmpId(owner.getEmpId());
        item.setName(owner.getName());
        item.setPhoneMasked(mask(owner.getTelNo()));
        item.setStatus(owner.getStatus() == null ? null : owner.getStatus().name());
        item.setJoinedAt(owner.getJoinedAt() == null ? dateTime(owner.getCreateTime()) : owner.getJoinedAt());
        item.setAccountCreateTime(firstCredentialCreateTime(credentials));
        item.setLastLoginAt(lastLogin(credentials));
        item.setLastLoginType(lastLoginType(credentials));
        return item;
    }

    private String businessStage(Long comId) {
        if (customerRepository.countByCompany_ComId(comId) == 0) return "未建客户";
        if (quoteRepository.countByCompany_ComId(comId) == 0) return "已建客户";
        if (projectContractRepository.countByCompany_ComId(comId) == 0) return "已报价";
        if (projectRepository.countByCompany_ComId(comId) == 0) return "已签约";
        return "已立项";
    }

    private String defaultNextAction(PlatformTenantOnboarding value) {
        if ("从未登录".equals(value.getLoginStatus())) return "首次联系并确认登录方式";
        if (value.getDaysSinceLastLogin() != null && value.getDaysSinceLastLogin() >= 30) return "30日未登录，核实使用问题";
        if (value.getOwnerCount() == 0) return "确认负责人并补全联系信息";
        return switch (value.getBusinessStage()) {
            case "未建客户" -> "引导建立第一个客户和房屋";
            case "已建客户" -> "引导完成首次报价";
            case "已报价" -> "跟进报价决策和签约";
            case "已签约" -> "确认项目立项和施工计划";
            default -> "回访业务使用情况";
        };
    }

    private LocalDateTime lastLogin(List<Credentials> credentials) {
        if (credentials == null) return null;
        return credentials.stream().map(Credentials::getLastTime).filter(item -> item != null)
                .max(LocalDateTime::compareTo).orElse(null);
    }

    private LocalDateTime firstCredentialCreateTime(List<Credentials> credentials) {
        if (credentials == null) return null;
        return credentials.stream().map(item -> dateTime(item.getCreateTime())).filter(item -> item != null)
                .min(LocalDateTime::compareTo).orElse(null);
    }

    private String lastLoginType(List<Credentials> credentials) {
        if (credentials == null) return null;
        return credentials.stream().filter(item -> item.getLastTime() != null)
                .max(Comparator.comparing(Credentials::getLastTime))
                .map(item -> item.getType() == null ? null : item.getType().name()).orElse(null);
    }

    private String loginStatus(LocalDateTime lastLogin) {
        if (lastLogin == null) return "从未登录";
        long days = Math.max(0L, ChronoUnit.DAYS.between(lastLogin.toLocalDate(), LocalDateTime.now().toLocalDate()));
        if (days >= 30) return "30日未登录";
        if (days >= 7) return "7日未登录";
        if (days == 0) return "今日已登录";
        return "7日内登录";
    }

    private String mask(String phone) {
        if (phone == null || phone.isBlank()) return "";
        String normalized = phone.trim();
        if (normalized.length() < 7) return "******";
        return normalized.substring(0, 3) + "****" + normalized.substring(normalized.length() - 4);
    }

    private LocalDateTime dateTime(DateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private PlatformTenant tenant(Company company) {
        Subscription subscription = subscriptionContext.current(company);
        PlatformTenant value = new PlatformTenant();
        value.setComId(company.getComId());
        value.setTitle(company.getTitle());
        value.setName(company.getName());
        value.setLogo(company.getLogo());
        value.setColor(company.getColor());
        value.setSize(company.getSize() == null ? null : company.getSize().name());
        value.setAddr(company.getAddr());
        value.setCreateTime(company.getCreateTime());
        value.setEnableStore(company.getSetting() != null ? company.getSetting().getEnableStore() : false);
        value.setPlanName(planName(subscription));
        value.setSubscriptionStatus(subscription == null || subscription.getStatus() == null ? "None" : subscription.getStatus().name());
        value.setServiceEndTime(subscription == null ? null : subscription.getServiceEndTime());
        value.setRemainingDays(subscription == null ? null : subscriptionContext.remainingDays(subscription));
        value.setUsedSeats(employeeRepository.countByCompany_ComId(company.getComId()));
        value.setEffectiveMaxSeats(subscription == null ? null : subscriptionContext.effectiveMaxSeats(subscription));
        value.setUsedStores(storeRepository.countByCompany_ComId(company.getComId()));
        value.setEffectiveMaxStores(subscription == null ? null : subscriptionContext.effectiveMaxStores(subscription));
        return value;
    }

    private PlatformSubscription subscription(Subscription subscription) {
        PlatformSubscription value = new PlatformSubscription();
        value.setSubId(subscription.getSubId());
        value.setComId(subscription.getCompany() == null ? null : subscription.getCompany().getComId());
        value.setCompanyTitle(subscription.getCompany() == null ? null : subscription.getCompany().getTitle());
        value.setPlanCode(subscription.getPlanVersion() == null
                ? subscription.getPlan() == null ? null : subscription.getPlan().getCode()
                : subscription.getPlanVersion().getPlanCode());
        value.setPlanName(planName(subscription));
        value.setStatus(subscription.getStatus() == null ? null : subscription.getStatus().name());
        value.setServiceEndTime(subscription.getServiceEndTime());
        value.setRemainingDays(subscriptionContext.remainingDays(subscription));
        value.setExtraSeats(subscription.getExtraSeats());
        value.setExtraStores(subscription.getExtraStores());
        value.setUsedSeats(subscription.getCompany() == null ? 0L : employeeRepository.countByCompany_ComId(subscription.getCompany().getComId()));
        value.setUsedStores(subscription.getCompany() == null ? 0L : storeRepository.countByCompany_ComId(subscription.getCompany().getComId()));
        value.setEffectiveMaxSeats(subscriptionContext.effectiveMaxSeats(subscription));
        value.setEffectiveMaxStores(subscriptionContext.effectiveMaxStores(subscription));
        value.setWritable(subscriptionContext.isWritable(subscription.getCompany()));
        value.setExpired(subscriptionContext.isExpired(subscription.getCompany()));
        return value;
    }

    private PlatformPlan plan(Plan plan) {
        PlatformPlan value = new PlatformPlan();
        value.setPlanId(plan.getPlanId());
        value.setCode(plan.getCode());
        value.setName(plan.getName());
        value.setDays(plan.getDays());
        value.setMaxStores(plan.getMaxStores());
        value.setMaxSeats(plan.getMaxSeats());
        value.setModules(plan.getModules());
        value.setPrice(plan.getPrice());
        value.setOriginalPrice(plan.getOriginalPrice());
        value.setDescription(plan.getDescription());
        value.setTagline(plan.getTagline());
        value.setHighlights(plan.getHighlights());
        value.setBadge(plan.getBadge());
        value.setRecommended(plan.getRecommended());
        value.setPriority(plan.getPriority());
        return value;
    }

    private List<FunnelMetric> buildFunnel(long companies, List<Subscription> subscriptions) {
        long activated = subscriptions.size();
        long trial = subscriptions.stream().filter(item -> item.getStatus() == SubStatus.Trial).count();
        long paid = subscriptions.stream().filter(item -> item.getStatus() == SubStatus.Active).count();
        List<FunnelMetric> funnel = new ArrayList<>();
        funnel.add(new FunnelMetric("企业注册", companies, 100D));
        funnel.add(new FunnelMetric("订阅激活", activated, rate(activated, companies)));
        funnel.add(new FunnelMetric("试用中", trial, rate(trial, companies)));
        funnel.add(new FunnelMetric("付费转化", paid, rate(paid, companies)));
        return funnel;
    }

    private List<RevenueTrend> buildTrend(List<Company> companies, List<Order> orders, LocalDateTime now) {
        List<RevenueTrend> result = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) {
            YearMonth month = YearMonth.from(now.minusMonths(offset));
            long amount = orders.stream()
                    .filter(item -> item.getPaidTime() != null && YearMonth.from(item.getPaidTime()).equals(month))
                    .mapToLong(item -> item.getAmount() == null ? 0L : item.getAmount()).sum();
            long newCompanies = companies.stream()
                    .filter(item -> item.getCreateTime() != null && item.getCreateTime().toLocalDateTime() != null)
                    .filter(item -> YearMonth.from(item.getCreateTime().toLocalDateTime()).equals(month)).count();
            long renewals = orders.stream()
                    .filter(item -> item.getPaidTime() != null && YearMonth.from(item.getPaidTime()).equals(month)).count();
            result.add(new RevenueTrend(month.toString(), amount, newCompanies, renewals));
        }
        return result;
    }

    private String planName(Subscription subscription) {
        if (subscription == null) return "未订阅";
        if (subscription.getPlanVersion() != null) return subscription.getPlanVersion().getPlanName();
        if (subscription.getPlan() != null) return subscription.getPlan().getName();
        return "未配置套餐";
    }

    private long remainingDays(Subscription subscription) {
        return subscription == null || subscription.getServiceEndTime() == null ? -1L
                : subscriptionContext.remainingDays(subscription);
    }

    private long seatCapacity(Subscription subscription) {
        if (subscription.getPlanVersion() == null) return 0L;
        return safe(subscription.getPlanVersion().getMaxSeats()) + safe(subscription.getExtraSeats());
    }

    private long storeCapacity(Subscription subscription) {
        if (subscription.getPlanVersion() == null) return 0L;
        return safe(subscription.getPlanVersion().getMaxStores()) + safe(subscription.getExtraStores());
    }

    private long safe(Number value) { return value == null ? 0L : Math.max(0L, value.longValue()); }
    private double usageRate(long used, long capacity) { return capacity <= 0 ? 0D : Math.round(Math.min(100D, used * 100D / capacity) * 100D) / 100D; }
    private double rate(long numerator, long denominator) { return denominator <= 0 ? 0D : Math.round(numerator * 10000D / denominator) / 100D; }

    private PlatformAction action(Integer days, String planCode, Integer extraSeats, Integer extraStores) {
        if (days != null) return PlatformAction.Renew;
        if (planCode != null && !planCode.isBlank()) return PlatformAction.PlanChange;
        if (extraSeats != null) return PlatformAction.SeatAdjust;
        if (extraStores != null) return PlatformAction.StoreAdjust;
        return PlatformAction.StatusChange;
    }

    private boolean hasFutureService(Subscription subscription) {
        return subscription.getServiceEndTime() != null
                && subscription.getServiceEndTime().toLocalDateTime() != null
                && subscription.getServiceEndTime().toLocalDateTime().isAfter(LocalDateTime.now());
    }

    private Map<String, Object> snapshot(Subscription subscription) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("subId", subscription.getSubId());
        value.put("companyId", subscription.getCompany() == null ? null : subscription.getCompany().getComId());
        value.put("planCode", subscription.getPlanVersion() == null ? null : subscription.getPlanVersion().getPlanCode());
        value.put("planName", planName(subscription));
        value.put("status", subscription.getStatus() == null ? null : subscription.getStatus().name());
        value.put("serviceEndTime", subscription.getServiceEndTime() == null ? null : subscription.getServiceEndTime().toLocalDateTime());
        value.put("extraSeats", subscription.getExtraSeats());
        value.put("extraStores", subscription.getExtraStores());
        return value;
    }
}

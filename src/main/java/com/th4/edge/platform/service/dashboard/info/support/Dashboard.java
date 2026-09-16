package com.th4.edge.platform.service.dashboard.info.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.platform.context.PlatformAdminContext;
import com.th4.edge.platform.model.FunnelMetric;
import com.th4.edge.platform.model.PlatformMetrics;
import com.th4.edge.platform.model.RevenueTrend;
import com.th4.edge.tenant.billing.configure.OrderStatus;
import com.th4.edge.tenant.billing.configure.SubStatus;
import com.th4.edge.tenant.billing.model.Order;
import com.th4.edge.tenant.billing.model.Subscription;
import com.th4.edge.tenant.billing.repository.OrderRepository;
import com.th4.edge.tenant.billing.repository.SubscriptionRepository;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.CompanyRepository;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import com.th4.edge.tenant.user.repository.StoreRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 平台运营总览实现。 */
@Note("平台运营总览")
@Service("platform/dashboard/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Dashboard implements com.th4.edge.platform.service.dashboard.info.Dashboard {

    @User
    private Employee employee;

    private final PlatformAdminContext platformContext;
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;
    private final StoreRepository storeRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;

    public Dashboard(PlatformAdminContext platformContext,
                     CompanyRepository companyRepository,
                     EmployeeRepository employeeRepository,
                     StoreRepository storeRepository,
                     SubscriptionRepository subscriptionRepository,
                     OrderRepository orderRepository) {
        this.platformContext = platformContext;
        this.companyRepository = companyRepository;
        this.employeeRepository = employeeRepository;
        this.storeRepository = storeRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformMetrics call(Company request) {
        platformContext.requireAdmin(employee);
        List<Company> companies = companyRepository.findByPlatformFalseOrderByComIdDesc();
        List<Subscription> subscriptions = subscriptionRepository.findAllByOrderBySubIdDesc().stream()
                .filter(item -> item.getCompany() != null && !Boolean.TRUE.equals(item.getCompany().getPlatform()))
                .toList();
        LocalDateTime now = LocalDateTime.now();
        YearMonth currentMonth = YearMonth.from(now);

        PlatformMetrics metrics = new PlatformMetrics();
        metrics.setTotalCompanies((long) companies.size());
        metrics.setTrialSubscriptions(subscriptions.stream().filter(item -> item.getStatus() == SubStatus.Trial).count());
        metrics.setActiveSubscriptions(subscriptions.stream().filter(item -> item.getStatus() == SubStatus.Active).count());
        metrics.setExpiringSoon(subscriptions.stream()
                .filter(item -> item.getStatus() == SubStatus.Active || item.getStatus() == SubStatus.Trial)
                .filter(item -> {
                    long days = platformRemainingDays(item);
                    return days >= 0 && days <= 7;
                }).count());
        metrics.setRiskCompanies(subscriptions.stream().filter(item -> {
            long days = platformRemainingDays(item);
            return item.getStatus() == SubStatus.Expired
                    || item.getStatus() == SubStatus.Frozen
                    || item.getStatus() == SubStatus.Closed
                    || (days >= 0 && days <= 7);
        }).count());
        metrics.setNewCompaniesThisMonth(companies.stream()
                .filter(item -> item.getCreateTime() != null && item.getCreateTime().toLocalDateTime() != null)
                .filter(item -> YearMonth.from(item.getCreateTime().toLocalDateTime()).equals(currentMonth))
                .count());

        List<Order> orders = orderRepository.findAll();
        List<Order> successfulOrders = orders.stream()
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
        subscriptions.forEach(item -> {
            String planName = item.getPlanVersion() == null ? "未配置套餐" : item.getPlanVersion().getPlanName();
            planDistribution.merge(planName, 1L, Long::sum);
        });
        metrics.setPlanDistribution(planDistribution);
        metrics.setFunnel(buildFunnel(companies.size(), subscriptions));
        metrics.setRevenueTrend(buildTrend(companies, successfulOrders, now));
        return metrics;
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
                    .filter(item -> item.getPaidTime() != null && YearMonth.from(item.getPaidTime()).equals(month))
                    .filter(item -> item.getItems() != null && item.getItems().stream()
                            .anyMatch(orderItem -> orderItem.getItemType() != null
                                    && "Plan".equals(orderItem.getItemType().name())))
                    .count();
            result.add(new RevenueTrend(month.toString(), amount, newCompanies, renewals));
        }
        return result;
    }

    private long platformRemainingDays(Subscription subscription) {
        if (subscription.getServiceEndTime() == null || subscription.getServiceEndTime().toLocalDateTime() == null) return -1L;
        return java.time.Duration.between(LocalDateTime.now(), subscription.getServiceEndTime().toLocalDateTime()).toDays();
    }

    private long seatCapacity(Subscription subscription) {
        if (subscription.getPlanVersion() == null) return 0L;
        return safe(subscription.getPlanVersion().getMaxSeats()) + safe(subscription.getExtraSeats());
    }

    private long storeCapacity(Subscription subscription) {
        if (subscription.getPlanVersion() == null) return 0L;
        return safe(subscription.getPlanVersion().getMaxStores()) + safe(subscription.getExtraStores());
    }

    private long safe(Number value) {
        return value == null ? 0L : Math.max(0L, value.longValue());
    }

    private double usageRate(long used, long capacity) {
        if (capacity <= 0) return 0D;
        return Math.round(Math.min(100D, used * 100D / capacity) * 100D) / 100D;
    }

    private double rate(long numerator, long denominator) {
        if (denominator <= 0) return 0D;
        return Math.round(numerator * 10000D / denominator) / 100D;
    }
}

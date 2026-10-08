package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerRiskLevel;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.decoboon.customer.model.CustomerMetricCard;
import com.th4.edge.decoboon.customer.model.CustomerMetricPeriod;
import com.th4.edge.decoboon.customer.model.CustomerOwnerGroup;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.model.CustomerStageCount;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 企业负责人客户经营聚合。 */
@Component
public class CustomerOverviewContext {

    private final TenantContext tenantContext;
    private final CustomerRepository customerRepository;
    private final CustomerFollowRepository followRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;
    private final MeasureRepository measureRepository;
    private final CustomerInsightContext insight;

    public CustomerOverviewContext(TenantContext tenantContext,
                                   CustomerRepository customerRepository,
                                   CustomerFollowRepository followRepository,
                                   QuoteRepository quoteRepository,
                                   ProjectContractRepository contractRepository,
                                   MeasureRepository measureRepository,
                                   CustomerInsightContext insight) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.followRepository = followRepository;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
        this.measureRepository = measureRepository;
        this.insight = insight;
    }

    @Transactional(readOnly = true)
    public CustomerOverviewReport report(Employee principal,
                                         Integer current,
                                         Integer size,
                                         String keyword,
                                         String stage,
                                         String sort,
                                         Long storeId) {
        return report(principal, current, size, keyword, stage, sort, storeId, null);
    }

    @Transactional(readOnly = true)
    public CustomerOverviewReport report(Employee principal,
                                         Integer current,
                                         Integer size,
                                         String keyword,
                                         String stage,
                                         String sort,
                                         Long storeId,
                                         Long ownerId) {
        Employee boss = tenantContext.requireBoss(principal);
        Long comId = boss.getCompany().getComId();
        LocalDate today = LocalDate.now();
        LocalDate month = today.withDayOfMonth(1);

        List<Customer> customers = customerRepository.findByCompany_ComId(comId).stream()
                .filter(item -> storeId == null || item.getStore() != null && storeId.equals(item.getStore().getStoreId()))
                .toList();
        List<CustomerFollow> follows = followRepository.findByCompany_ComId(comId);
        List<ProjectContract> contracts = contractRepository.findByCompany_ComIdOrderByContractIdDesc(comId);
        List<Long> customerIds = customers.stream().map(Customer::getCustId).toList();
        List<Measure> measures = measureRepository == null || customerIds.isEmpty() ? List.of()
                : measureRepository.findByCompany_ComIdAndCustomer_CustIdInOrderByMeasureIdDesc(comId, customerIds);
        Map<Long, Quote> latestQuotes = latestQuotes(quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(comId));
        Map<Long, ProjectContract> latestContracts = latestContracts(contracts);
        customers.forEach(customer -> enrich(customer, today, latestQuotes.get(customer.getCustId()),
                latestContracts.get(customer.getCustId())));

        CustomerOverviewReport report = new CustomerOverviewReport();
        report.setAsOf(today);
        report.setMonth(month);
        report.setMetrics(metrics(customers, follows, contracts, measures, today, month));
        report.setStageCounts(stageCounts(customers));
        applyPage(report, customers, current, size, keyword, stage, sort, ownerId);
        return report;
    }

    private void enrich(Customer customer, LocalDate today, Quote quote, ProjectContract contract) {
        insight.enrich(customer, today, quote, contract);
        insight.mainline(customer, false, customer.getMeasureAt() != null);
    }

    private boolean openStatus(CustStatus status) {
        return CustomerInsightContext.openStatus(status);
    }

    private List<CustomerMetricCard> metrics(List<Customer> customers,
                                             List<CustomerFollow> follows,
                                             List<ProjectContract> contracts,
                                             List<Measure> measures,
                                             LocalDate today,
                                             LocalDate month) {
        LocalDate yesterday = today.minusDays(1);
        LocalDate last7Start = today.minusDays(6);
        LocalDateTime monthStart = month.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime last7StartTime = last7Start.atStartOfDay();

        List<Customer> monthNew = customers.stream().filter(item -> inRange(created(item), monthStart, now)).toList();
        long todayNew = customers.stream().filter(item -> inRange(created(item), todayStart, now)).count();
        long yesterdayNew = customers.stream().filter(item -> inRange(created(item), yesterdayStart, todayStart)).count();
        long last7New = customers.stream().filter(item -> inRange(created(item), last7StartTime, now)).count();
        YearMonth previous = YearMonth.from(month).minusMonths(1);
        LocalDate previousStart = previous.atDay(1);
        LocalDate previousEnd = previous.atDay(Math.min(today.getDayOfMonth(), previous.lengthOfMonth()));
        long previousNew = customers.stream().filter(item -> {
            LocalDate date = created(item) == null ? null : created(item).toLocalDate();
            return date != null && !date.isBefore(previousStart) && !date.isAfter(previousEnd);
        }).count();

        Set<Long> todayActiveIds = activeCustomerIds(customers, follows, todayStart, now);
        Set<Long> yesterdayActiveIds = activeCustomerIds(customers, follows, yesterdayStart, todayStart);
        Set<Long> last7ActiveIds = activeCustomerIds(customers, follows, last7StartTime, now);
        Set<Long> monthActiveIds = activeCustomerIds(customers, follows, monthStart, now);
        long dueToday = customers.stream().filter(item -> openStatus(item.getStatus()) && item.getNextFollowAt() != null
                && item.getNextFollowAt().toLocalDate().equals(today)).count();
        long overdue = customers.stream().filter(item -> insight.overdue(item, today)).count();

        List<ProjectContract> signedContracts = contracts.stream()
                .filter(item -> item.getSignedAt() != null && YearMonth.from(item.getSignedAt()).equals(YearMonth.from(month)))
                .toList();
        long signedCustomers = signedContracts.stream().filter(item -> item.getCustomer() != null)
                .map(item -> item.getCustomer().getCustId()).distinct().count();
        long todaySigned = signedCustomers(contracts, today, today.plusDays(1));
        long yesterdaySigned = signedCustomers(contracts, yesterday, today);
        long last7Signed = signedCustomers(contracts, last7Start, today.plusDays(1));
        BigDecimal signedAmount = signedContracts.stream().map(item -> {
            BigDecimal signed = value(item.getSignedAmount());
            return signed.signum() == 0 ? value(item.getCurrentAmount()) : signed;
        }).reduce(BigDecimal.ZERO, BigDecimal::add);
        long quoting = customers.stream().filter(item -> item.getStatus() == CustStatus.Quoted).count();

        List<Measure> activeMeasures = measures.stream()
                .filter(item -> item.getStatus() != MeasureStatus.Cancelled)
                .toList();
        Set<Long> measureTodayIds = measureCustomerIds(activeMeasures, todayStart, now);
        Set<Long> measureYesterdayIds = measureCustomerIds(activeMeasures, yesterdayStart, todayStart);
        Set<Long> measureLast7Ids = measureCustomerIds(activeMeasures, last7StartTime, now);
        Set<Long> measureMonthIds = measureCustomerIds(activeMeasures, monthStart, now);
        Set<Long> measureCompletedIds = activeMeasures.stream()
                .filter(item -> inRange(item.getCompletedAt(), monthStart, now))
                .filter(item -> item.getCustomer() != null)
                .map(item -> item.getCustomer().getCustId())
                .collect(Collectors.toSet());
        Set<Long> measureUnassignedIds = activeMeasures.stream()
                .filter(item -> inRange(effectiveScheduledAt(item), monthStart, now))
                .filter(item -> item.getCompletedAt() == null && item.getDesigner() == null)
                .filter(item -> item.getCustomer() != null)
                .map(item -> item.getCustomer().getCustId())
                .collect(Collectors.toSet());

        CustomerMetricCard newCustomers = metric("newCustomer", "新增客户", monthNew.size(), "normal",
                "本月新增客户 · 按客户录入日期统计", rate(monthNew.size(), previousNew));
        newCustomers.setPeriods(periods(todayNew, yesterdayNew, last7New, monthNew.size()));

        CustomerMetricCard activeFollow = metric("activeFollow", "有效跟进", monthActiveIds.size(),
                overdue > 0 ? "attention" : "normal", "今日待跟进 " + dueToday + " · 逾期 " + overdue, null);
        activeFollow.setPeriods(periods(todayActiveIds.size(), yesterdayActiveIds.size(),
                last7ActiveIds.size(), monthActiveIds.size()));

        CustomerMetricCard signedCustomer = metric("signedCustomer", "签约客户", signedCustomers, "normal",
                "签约额 " + signedAmount.stripTrailingZeros().toPlainString() + " · 报价中 " + quoting, null);
        signedCustomer.setPeriods(periods(todaySigned, yesterdaySigned, last7Signed, signedCustomers));

        CustomerMetricCard measureCustomer = metric("measureCustomer", "预约量房客户", measureMonthIds.size(),
                measureUnassignedIds.isEmpty() ? "normal" : "attention",
                "本月完成 " + measureCompletedIds.size() + " 户 · 待派单 " + measureUnassignedIds.size() + " 户", null);
        measureCustomer.setPeriods(periods(measureTodayIds.size(), measureYesterdayIds.size(),
                measureLast7Ids.size(), measureMonthIds.size()));

        return List.of(newCustomers, activeFollow, signedCustomer, measureCustomer);
    }

    private Set<Long> measureCustomerIds(List<Measure> measures, LocalDateTime start, LocalDateTime end) {
        return measures.stream()
                .filter(item -> inRange(effectiveScheduledAt(item), start, end))
                .filter(item -> item.getCustomer() != null)
                .map(item -> item.getCustomer().getCustId())
                .collect(Collectors.toSet());
    }

    private LocalDateTime effectiveScheduledAt(Measure measure) {
        return measure.getScheduledAt() != null ? measure.getScheduledAt() : measure.getMeasureTime();
    }

    private Set<Long> activeCustomerIds(List<Customer> customers,
                                        List<CustomerFollow> follows,
                                        LocalDateTime start,
                                        LocalDateTime end) {
        Set<Long> ids = follows.stream()
                .filter(item -> item.getCustomer() != null && item.getCreateTime() != null
                        && inRange(item.getCreateTime().toLocalDateTime(), start, end))
                .map(item -> item.getCustomer().getCustId())
                .collect(Collectors.toSet());
        customers.stream().filter(item -> inRange(item.getLastVisitAt(), start, end)
                        || inRange(item.getMeasureAt(), start, end)
                        || inRange(item.getHandedOffAt(), start, end))
                .map(Customer::getCustId).forEach(ids::add);
        return ids;
    }

    private long signedCustomers(List<ProjectContract> contracts, LocalDate start, LocalDate endExclusive) {
        return contracts.stream()
                .filter(item -> item.getSignedAt() != null
                        && !item.getSignedAt().isBefore(start)
                        && item.getSignedAt().isBefore(endExclusive))
                .filter(item -> item.getCustomer() != null)
                .map(item -> item.getCustomer().getCustId())
                .distinct()
                .count();
    }

    private List<CustomerMetricPeriod> periods(long today, long yesterday, long last7Days, long month) {
        return List.of(
                period("today", "日增", today),
                period("yesterday", "昨日", yesterday),
                period("last7Days", "近7日增", last7Days),
                period("month", "月增", month)
        );
    }

    private CustomerMetricPeriod period(String key, String label, long value) {
        CustomerMetricPeriod period = new CustomerMetricPeriod();
        period.setKey(key);
        period.setLabel(label);
        period.setValue(BigDecimal.valueOf(value));
        period.setValueType("count");
        return period;
    }

    private CustomerMetricCard metric(String key, String label, long value, String status,
                                      String subText, BigDecimal trendRate) {
        CustomerMetricCard card = new CustomerMetricCard();
        card.setKey(key);
        card.setLabel(label);
        card.setValue(BigDecimal.valueOf(value));
        card.setValueType("count");
        card.setStatus(status);
        card.setSubText(subText);
        card.setTrendRate(trendRate);
        return card;
    }

    private List<CustomerStageCount> stageCounts(List<Customer> customers) {
        List<CustomerStageCount> result = new ArrayList<>();
        result.add(new CustomerStageCount("all", "全部", customers.size()));
        result.add(new CustomerStageCount("assign", "待指派设计", Math.toIntExact(customers.stream()
                .filter(item -> item.getStatus() == CustStatus.PendingMeasure && item.getMeasureAt() != null && item.getDesigner() == null).count())));
        result.add(new CustomerStageCount("intention", "待首联", count(customers, CustStatus.New)));
        result.add(new CustomerStageCount("following", "跟进中", count(customers, CustStatus.Following)));
        result.add(new CustomerStageCount("pendingMeasure", "待量房", count(customers, CustStatus.PendingMeasure)));
        result.add(new CustomerStageCount("quote", "报价", count(customers, CustStatus.Quoted)));
        result.add(new CustomerStageCount("sign", "签约", count(customers, CustStatus.Signed)));
        result.add(new CustomerStageCount("construction", "施工", count(customers, CustStatus.Serving)));
        result.add(new CustomerStageCount("done", "完工", count(customers, CustStatus.Finished)));
        result.add(new CustomerStageCount("lost", "流失", count(customers, CustStatus.Lost)));
        return result;
    }

    private int count(List<Customer> customers, CustStatus status) {
        return Math.toIntExact(customers.stream().filter(item -> item.getStatus() == status).count());
    }

    private void applyPage(CustomerOverviewReport report,
                           List<Customer> customers,
                           Integer requestedCurrent,
                           Integer requestedSize,
                           String keyword,
                           String stage,
                           String sort,
                           Long ownerId) {
        int current = requestedCurrent == null || requestedCurrent < 1 ? 1 : requestedCurrent;
        int size = requestedSize == null || requestedSize < 1 ? 20 : Math.min(requestedSize, 100);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<Customer> narrowed = customers.stream()
                .filter(item -> matchesStage(item, stage))
                .filter(item -> kw.isBlank() || (item.getName() + item.getTelNo() + (item.getCommunity() == null ? "" : item.getCommunity()))
                        .toLowerCase().contains(kw))
                .toList();
        report.setOwnerGroups(ownerGroups(narrowed));
        List<Customer> filtered = narrowed.stream()
                .filter(item -> matchesOwner(item, ownerId))
                .sorted(comparator(sort))
                .toList();
        int from = Math.min((current - 1) * size, filtered.size());
        int to = Math.min(from + size, filtered.size());
        report.setCustomers(filtered.subList(from, to));
        report.setCurrent(current);
        report.setSize(size);
        report.setTotal((long) filtered.size());
        report.setHasMore(to < filtered.size());
    }

    private boolean matchesOwner(Customer customer, Long ownerId) {
        if (ownerId == null) return true;
        Long actual = customer.getOwner() == null ? null : customer.getOwner().getEmpId();
        return ownerId == 0L ? actual == null : ownerId.equals(actual);
    }

    private List<CustomerOwnerGroup> ownerGroups(List<Customer> customers) {
        Map<String, List<Customer>> grouped = new LinkedHashMap<>();
        for (Customer customer : customers) {
            Long ownerId = customer.getOwner() == null ? null : customer.getOwner().getEmpId();
            grouped.computeIfAbsent(ownerId == null ? "unassigned" : String.valueOf(ownerId), key -> new ArrayList<>())
                    .add(customer);
        }
        return grouped.values().stream().map(items -> {
            Customer first = items.get(0);
            Employee owner = first.getOwner();
            CustomerOwnerGroup group = new CustomerOwnerGroup();
            group.setOwnerId(owner == null ? null : owner.getEmpId());
            group.setOwnerName(owner == null ? "未分配负责人" : owner.getName());
            group.setStoreName(owner == null || owner.getStore() == null ? null : owner.getStore().getName());
            group.setCustomerCount(items.size());
            group.setRiskCustomerCount(Math.toIntExact(items.stream()
                    .filter(item -> item.getRiskLevel() != CustomerRiskLevel.Normal).count()));
            group.setPendingMeasureCount(Math.toIntExact(items.stream()
                    .filter(item -> item.getStatus() == CustStatus.PendingMeasure).count()));
            group.setSignedCustomerCount(Math.toIntExact(items.stream()
                    .filter(item -> item.getStatus() == CustStatus.Signed
                            || item.getStatus() == CustStatus.Serving
                            || item.getStatus() == CustStatus.Finished).count()));
            group.setQuoteAmount(items.stream().map(item -> value(item.getQuoteAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            group.setSignedAmount(items.stream().map(item -> value(item.getSignedAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            return group;
        }).sorted(Comparator.comparing((CustomerOwnerGroup item) -> item.getOwnerId() == null ? 1 : 0)
                .thenComparing(CustomerOwnerGroup::getCustomerCount, Comparator.reverseOrder())
                .thenComparing(CustomerOwnerGroup::getOwnerName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private boolean matchesStage(Customer customer, String stage) {
        if (stage == null || stage.isBlank() || "all".equals(stage)) return true;
        if ("assign".equals(stage)) return customer.getStatus() == CustStatus.PendingMeasure
                && customer.getMeasureAt() != null && customer.getDesigner() == null;
        return stage.equals(stageKey(customer.getStatus()));
    }

    private String stageKey(CustStatus status) {
        return switch (status) {
            case New -> "intention";
            case Following -> "following";
            case PendingMeasure -> "pendingMeasure";
            case Quoted -> "quote";
            case Signed -> "sign";
            case Serving -> "construction";
            case Finished -> "done";
            case Lost -> "lost";
        };
    }

    private Comparator<Customer> comparator(String sort) {
        if ("idle".equals(sort)) return Comparator.comparing(Customer::getIdleDays, Comparator.nullsLast(Comparator.reverseOrder()));
        if ("value".equals(sort)) return Comparator.comparing(this::businessValue, Comparator.reverseOrder());
        if ("newest".equals(sort)) return Comparator.comparing(this::created, Comparator.nullsLast(Comparator.reverseOrder()));
        return Comparator.comparingInt((Customer item) -> switch (item.getRiskLevel()) {
            case High -> 0;
            case Attention -> 1;
            case Normal -> 2;
        }).thenComparing(Customer::getIdleDays, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private BigDecimal businessValue(Customer customer) {
        return value(customer.getSignedAmount()).max(value(customer.getQuoteAmount()));
    }

    private Map<Long, Quote> latestQuotes(List<Quote> quotes) {
        Map<Long, Quote> result = new HashMap<>();
        for (Quote quote : quotes) {
            if (quote.getCustomer() == null) continue;
            Quote current = result.get(quote.getCustomer().getCustId());
            if (current == null || quote.getQuoteId() > current.getQuoteId()) result.put(quote.getCustomer().getCustId(), quote);
        }
        return result;
    }

    private Map<Long, ProjectContract> latestContracts(List<ProjectContract> contracts) {
        Map<Long, ProjectContract> result = new HashMap<>();
        for (ProjectContract contract : contracts) {
            if (contract.getCustomer() == null) continue;
            ProjectContract current = result.get(contract.getCustomer().getCustId());
            if (current == null || contract.getContractId() > current.getContractId()) {
                result.put(contract.getCustomer().getCustId(), contract);
            }
        }
        return result;
    }

    private LocalDateTime latest(LocalDateTime... values) {
        LocalDateTime result = null;
        for (LocalDateTime value : values) {
            if (value != null && (result == null || value.isAfter(result))) result = value;
        }
        return result;
    }

    private LocalDateTime created(Customer customer) {
        return customer.getCreateTime() == null ? null : customer.getCreateTime().toLocalDateTime();
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && !value.isAfter(end);
    }

    private BigDecimal rate(long current, long previous) {
        if (previous == 0) return current == 0 ? BigDecimal.ZERO : null;
        return BigDecimal.valueOf(current - previous)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

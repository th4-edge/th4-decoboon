package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerMetricCard;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.model.CustomerPortrait;
import com.th4.edge.decoboon.customer.model.CustomerStageCount;
import com.th4.edge.decoboon.customer.model.CustomerTag;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerPortraitRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 设计师「我的客户」聚合。
 *
 * <p>与 {@link CustomerOverviewContext}（企业负责人口径）的区别只有两处：</p>
 * <ol>
 *   <li><b>数据范围</b>：本人负责 ∪ 指派给我设计（与 {@code customer/customer/page} 一致）；</li>
 *   <li><b>取数方式</b>：阶段计数与经营时钟用聚合 SQL，客户列表只对当前页做有界投影组装，
 *       不再全量回捞实体后循环（DEC-036 / BR-031）。</li>
 * </ol>
 *
 * <p>风险等级、风险原因、下一步动作、停留天数与最近金额统一走
 * {@link CustomerInsightContext}，与企业负责人视角同源同口径。</p>
 */
@Component
public class MyCustomerOverviewContext {

    /** 参与"进行中"判定的状态（与 CustomerInsightContext.openStatus 保持一致）。 */
    private static final List<CustStatus> OPEN_STATUSES =
            List.of(CustStatus.New, CustStatus.Following, CustStatus.PendingMeasure, CustStatus.Quoted);

    /** 参与"跟进逾期"统计的状态：仅预约量房前（与 CustomerInsightContext.followOverdueEligible 一致），
     * 仓储查询另排除已写入量房约定时间的客户作为事实兜底。 */
    private static final List<CustStatus> FOLLOW_OVERDUE_STATUSES =
            List.of(CustStatus.New, CustStatus.Following);

    private final CustomerRepository customerRepository;
    private final CustomerPortraitRepository portraitRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;
    private final MeasureRepository measureRepository;
    private final CustomerInsightContext insight;
    private final CustomerFollowRepository followRepository;
    private final CustomerTagContext tagContext;
    private final CustomerGradeContext gradeContext;
    private final CustomerLossContext lossContext;

    public MyCustomerOverviewContext(CustomerRepository customerRepository,
                                     CustomerPortraitRepository portraitRepository,
                                     QuoteRepository quoteRepository,
                                     ProjectContractRepository contractRepository,
                                     MeasureRepository measureRepository,
                                     CustomerInsightContext insight,
                                     CustomerFollowRepository followRepository,
                                     CustomerTagContext tagContext,
                                     CustomerGradeContext gradeContext,
                                     CustomerLossContext lossContext) {
        this.customerRepository = customerRepository;
        this.portraitRepository = portraitRepository;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
        this.measureRepository = measureRepository;
        this.insight = insight;
        this.followRepository = followRepository;
        this.tagContext = tagContext;
        this.gradeContext = gradeContext;
        this.lossContext = lossContext;
    }

    @Transactional(readOnly = true)
    public CustomerOverviewReport report(Employee principal,
                                         Integer requestedCurrent,
                                         Integer requestedSize,
                                         String keyword,
                                         String stage,
                                         String sort,
                                         String followFilter) {
        Long comId = principal.getCompany().getComId();
        Long empId = principal.getEmpId();
        LocalDate today = LocalDate.now();
        int current = requestedCurrent == null || requestedCurrent < 1 ? 1 : requestedCurrent;
        int size = requestedSize == null || requestedSize < 1 ? 20 : Math.min(requestedSize, 100);

        CustomerOverviewReport report = new CustomerOverviewReport();
        report.setAsOf(today);
        report.setMonth(today.withDayOfMonth(1));

        Map<CustStatus, Long> statusCounts = statusCounts(comId, empId);
        report.setStageCounts(stageCounts(comId, empId, statusCounts, today));
        report.setFollowCounts(followCounts(comId, empId, today));
        report.setMetrics(metrics(comId, empId, statusCounts, today));

        Page<Customer> page = customerRepository.findPageForDesigner(
                scopeSpecification(comId, empId, normalizeStatus(stage), keyword, sort, followFilter, today),
                PageRequest.of(current - 1, size));
        List<Customer> rows = page.getContent();
        assemblePage(rows, comId, today);

        report.setCustomers(rows);
        report.setCurrent(current);
        report.setSize(size);
        report.setTotal(page.getTotalElements());
        report.setHasMore(page.hasNext());
        return report;
    }

    // ── 计数：全部走聚合 SQL ────────────────────────────────────────────────

    private Map<CustStatus, Long> statusCounts(Long comId, Long empId) {
        return customerRepository.countStatusForDesigner(comId, empId).stream()
                .collect(Collectors.toMap(row -> (CustStatus) row[0], row -> ((Number) row[1]).longValue()));
    }

    private List<CustomerStageCount> stageCounts(Long comId,
                                                 Long empId,
                                                 Map<CustStatus, Long> counts,
                                                 LocalDate today) {
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long pendingAssign = customerRepository.countPendingAssignForDesigner(comId, empId);
        List<CustomerStageCount> result = new ArrayList<>();
        result.add(new CustomerStageCount("all", "全部", Math.toIntExact(total)));
        result.add(new CustomerStageCount("assign", "待指派设计", Math.toIntExact(pendingAssign)));
        result.add(new CustomerStageCount("intention", "待首联", count(counts, CustStatus.New)));
        result.add(new CustomerStageCount("following", "跟进中", count(counts, CustStatus.Following)));
        result.add(new CustomerStageCount("pendingMeasure", "待量房", count(counts, CustStatus.PendingMeasure)));
        result.add(new CustomerStageCount("quote", "报价", count(counts, CustStatus.Quoted)));
        result.add(new CustomerStageCount("sign", "签约", count(counts, CustStatus.Signed)));
        result.add(new CustomerStageCount("construction", "施工", count(counts, CustStatus.Serving)));
        result.add(new CustomerStageCount("done", "完工", count(counts, CustStatus.Finished)));
        result.add(new CustomerStageCount("lost", "流失", count(counts, CustStatus.Lost)));
        return result;
    }

    private List<CustomerStageCount> followCounts(Long comId, Long empId, LocalDate today) {
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime soonEnd = today.plusDays(4).atStartOfDay();
        long overdue = customerRepository.countFollowBeforeForDesigner(
                comId, empId, FOLLOW_OVERDUE_STATUSES, todayStart);
        long current = customerRepository.countFollowBetweenForDesigner(
                comId, empId, FOLLOW_OVERDUE_STATUSES, todayStart, tomorrowStart);
        long soon = customerRepository.countFollowBetweenForDesigner(
                comId, empId, FOLLOW_OVERDUE_STATUSES, tomorrowStart, soonEnd);
        long unplanned = customerRepository.countFollowUnplannedForDesigner(
                comId, empId, FOLLOW_OVERDUE_STATUSES);
        long all = overdue + current + soon + unplanned;
        return List.of(
                new CustomerStageCount("all", "全部待办", Math.toIntExact(all)),
                new CustomerStageCount("overdue", "已逾期", Math.toIntExact(overdue)),
                new CustomerStageCount("today", "今日跟进", Math.toIntExact(current)),
                new CustomerStageCount("soon", "3 天内", Math.toIntExact(soon)),
                new CustomerStageCount("unplanned", "未安排", Math.toIntExact(unplanned))
        );
    }

    private int count(Map<CustStatus, Long> counts, CustStatus status) {
        return Math.toIntExact(counts.getOrDefault(status, 0L));
    }

    private List<CustomerMetricCard> metrics(Long comId,
                                             Long empId,
                                             Map<CustStatus, Long> counts,
                                             LocalDate today) {
        long inProgress = counts.entrySet().stream()
                .filter(entry -> CustomerInsightContext.openStatus(entry.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum();

        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
        LocalDateTime soonEnd = today.plusDays(4).atStartOfDay();
        LocalDateTime idleThreshold = today.minusDays(15).atStartOfDay();
        Long idleThresholdMillis = idleThreshold.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        long overdue = customerRepository.countFollowBeforeForDesigner(comId, empId, FOLLOW_OVERDUE_STATUSES, todayStart);
        long dueToday = customerRepository.countFollowBetweenForDesigner(comId, empId, OPEN_STATUSES, todayStart, tomorrowStart);
        long dueSoon = customerRepository.countFollowBetweenForDesigner(comId, empId, OPEN_STATUSES, tomorrowStart, soonEnd);
        long idle = customerRepository.countIdleForDesigner(comId, empId, OPEN_STATUSES, idleThreshold, idleThresholdMillis);

        List<CustomerMetricCard> cards = new ArrayList<>();
        cards.add(metric("designerInProgressCustomerCount", "在途客户", inProgress, "量房/设计/报价阶段客户"));
        cards.add(metric("designerPendingMeasureCustomerCount", "待量房", count(counts, CustStatus.PendingMeasure), "已约定或待安排量房"));
        cards.add(metric("designerQuotedCustomerCount", "报价推进中", count(counts, CustStatus.Quoted), "等待客户确认或调整"));
        cards.add(metric("designerSignedCustomerCount", "已签约", count(counts, CustStatus.Signed), "已签约客户"));
        cards.add(metric("designerClockOverdueCount", "逾期跟进", overdue, "跟进时间已过，需立即处理"));
        cards.add(metric("designerClockTodayCount", "今日跟进", dueToday, "今天到期的跟进"));
        cards.add(metric("designerClockSoonCount", "3 天内跟进", dueSoon, "明后三天到期的跟进"));
        cards.add(metric("designerClockIdleCount", "沉睡风险", idle, "连续 15 天未推进"));
        return cards;
    }

    private CustomerMetricCard metric(String key, String label, long value, String subText) {
        CustomerMetricCard card = new CustomerMetricCard();
        card.setKey(key);
        card.setLabel(label);
        card.setValue(BigDecimal.valueOf(value));
        card.setValueType("count");
        card.setStatus("normal");
        card.setSubText(subText);
        return card;
    }

    // ── 列表：范围 + 关键词 + 阶段 + 排序全部下推 SQL ────────────────────────

    private Specification<Customer> scopeSpecification(Long comId,
                                                      Long empId,
                                                      CustStatus status,
                                                      String keyword,
                                                      String sort,
                                                      String followFilter,
                                                      LocalDate today) {
        String kw = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("comId"), comId));
            predicates.add(cb.or(
                    cb.equal(root.get("owner").get("empId"), empId),
                    cb.equal(root.get("designer").get("empId"), empId)));
            String queue = normalizeFollowFilter(followFilter);
            if (queue != null) {
                predicates.add(root.get("status").in(FOLLOW_OVERDUE_STATUSES));
                predicates.add(cb.isNull(root.get("measureRequestedAt")));
                predicates.add(cb.isNull(root.get("pendingMeasureAt")));
                predicates.add(cb.isNull(root.get("measureAt")));
                LocalDateTime todayStart = today.atStartOfDay();
                LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();
                LocalDateTime soonEnd = today.plusDays(4).atStartOfDay();
                switch (queue) {
                    case "overdue" -> {
                        predicates.add(cb.isNotNull(root.get("nextFollowAt")));
                        predicates.add(cb.lessThan(root.get("nextFollowAt"), todayStart));
                    }
                    case "today" -> {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("nextFollowAt"), todayStart));
                        predicates.add(cb.lessThan(root.get("nextFollowAt"), tomorrowStart));
                    }
                    case "soon" -> {
                        predicates.add(cb.greaterThanOrEqualTo(root.get("nextFollowAt"), tomorrowStart));
                        predicates.add(cb.lessThan(root.get("nextFollowAt"), soonEnd));
                    }
                    case "unplanned" -> predicates.add(cb.isNull(root.get("nextFollowAt")));
                    default -> predicates.add(cb.or(
                            cb.isNull(root.get("nextFollowAt")),
                            cb.lessThan(root.get("nextFollowAt"), soonEnd)));
                }
            } else if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (!kw.isBlank()) {
                String like = "%" + kw + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("telNo")), like),
                        cb.like(cb.lower(cb.coalesce(root.<String>get("community"), "")), like)));
            }
            applyOrder(query, cb, root, sort);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 排序下推 SQL。Criteria 不支持 NULLS LAST，这里用 CASE 把「无跟进时间」排到末尾，
     * 避免把排序拉回 Java 内存。
     */
    private void applyOrder(jakarta.persistence.criteria.CriteriaQuery<?> query,
                            jakarta.persistence.criteria.CriteriaBuilder cb,
                            jakarta.persistence.criteria.Root<Customer> root,
                            String sort) {
        if (query == null || query.getResultType() == Long.class || query.getResultType() == long.class) {
            return;
        }
        if ("newest".equalsIgnoreCase(sort)) {
            query.orderBy(cb.desc(root.get("custId")));
            return;
        }
        // 风险优先：进行中的客户排在前面，再按跟进到期早晚，最后是「无跟进时间」。
        var openRank = cb.selectCase()
                .when(root.get("status").in(OPEN_STATUSES), 0)
                .otherwise(1);
        var noFollowRank = cb.selectCase()
                .when(cb.isNull(root.get("nextFollowAt")), 1)
                .otherwise(0);
        query.orderBy(cb.asc(openRank), cb.asc(noFollowRank), cb.asc(root.get("nextFollowAt")),
                cb.desc(root.get("custId")));
    }

    private String normalizeFollowFilter(String followFilter) {
        if (followFilter == null || followFilter.isBlank() || "all".equalsIgnoreCase(followFilter)) {
            return followFilter == null || followFilter.isBlank() ? null : "all";
        }
        return switch (followFilter.trim()) {
            case "overdue", "today", "soon", "unplanned" -> followFilter.trim();
            default -> null;
        };
    }

    private CustStatus normalizeStatus(String stage) {
        if (stage == null || stage.isBlank()) {
            return null;
        }
        return switch (stage) {
            case "intention" -> CustStatus.New;
            case "following" -> CustStatus.Following;
            case "pendingMeasure" -> CustStatus.PendingMeasure;
            case "quote" -> CustStatus.Quoted;
            case "sign" -> CustStatus.Signed;
            case "construction" -> CustStatus.Serving;
            case "done" -> CustStatus.Finished;
            case "lost" -> CustStatus.Lost;
            default -> null;
        };
    }

    // ── 投影组装：只对当前页客户做有界查询 ─────────────────────────────────

    private void assemblePage(List<Customer> rows, Long comId, LocalDate today) {
        if (rows.isEmpty()) {
            return;
        }
        lossContext.enrich(rows);
        Set<Long> ids = rows.stream().map(Customer::getCustId).collect(Collectors.toSet());
        Map<Long, Quote> latestQuotes = latest(quoteRepository
                .findByCompany_ComIdAndCustomer_CustIdInOrderByQuoteIdDesc(comId, ids), Quote::getCustomer);
        Map<Long, ProjectContract> latestContracts = latest(contractRepository
                .findByCompany_ComIdAndCustomer_CustIdInOrderByContractIdDesc(comId, ids), ProjectContract::getCustomer);
        Map<Long, com.th4.edge.decoboon.customer.model.CustomerPortrait> portraits =
                portraitRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, ids).stream()
                        .collect(Collectors.toMap(item -> item.getCustomer().getCustId(), item -> item, (left, right) -> left));
        Map<Long, List<Measure>> measuresByCustomer = measureRepository
                .findByCompany_ComIdAndCustomer_CustIdInOrderByMeasureIdDesc(comId, ids).stream()
                .collect(Collectors.groupingBy(item -> item.getCustomer().getCustId()));
        Set<Long> measuredCustomerIds = measuresByCustomer.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(item -> item.getStatus() == MeasureStatus.Completed))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Set<Long> scheduledCustomerIds = measuresByCustomer.entrySet().stream()
                .filter(entry -> entry.getValue().stream().anyMatch(item -> item.getStatus() == MeasureStatus.Scheduled))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        Map<Long, List<CustomerTag>> tagsByCustomer = tagContext.assembleBatch(
                comId, rows, measuredCustomerIds, scheduledCustomerIds);
        Map<Long, Integer> followCounts = followRepository.countGroupByCustomer(comId, ids).stream()
                .collect(Collectors.toMap(CustomerFollowRepository.CustomerFollowCount::getCustomerId,
                        row -> row.getTotal() == null ? 0 : row.getTotal().intValue()));
        for (Customer customer : rows) {
            CustomerPortrait portrait = portraits.get(customer.getCustId());
            customer.setPortrait(portrait);
            List<CustomerTag> tags = tagsByCustomer.getOrDefault(customer.getCustId(), List.of());
            customer.setTags(tags);
            Set<String> tagKeys = tags.stream()
                    .map(CustomerTag::getKey)
                    .filter(key -> key != null && !key.isBlank())
                    .collect(Collectors.toSet());
            insight.enrich(customer, today, latestQuotes.get(customer.getCustId()),
                    latestContracts.get(customer.getCustId()));
            List<Measure> measures = measuresByCustomer.getOrDefault(customer.getCustId(), List.of());
            boolean measured = measures.stream().anyMatch(item -> item.getStatus() == MeasureStatus.Completed);
            boolean scheduled = measures.stream().anyMatch(item -> item.getStatus() == MeasureStatus.Scheduled);
            customer.setGrade(gradeContext.compute(
                    customer, portrait, measured, followCounts.getOrDefault(customer.getCustId(), 0),
                    customer.getLastFollowAt(), tagKeys));
            insight.mainline(customer, measured, scheduled || customer.getMeasureAt() != null);
        }
    }

    private <T> Map<Long, T> latest(List<T> rows, Function<T, Customer> customerOf) {
        Map<Long, T> result = new java.util.LinkedHashMap<>();
        for (T row : rows) {
            Customer customer = customerOf.apply(row);
            if (customer == null) {
                continue;
            }
            result.putIfAbsent(customer.getCustId(), row);
        }
        return result;
    }
}

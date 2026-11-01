package com.th4.edge.decoboon.customer.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerMetricCard;
import com.th4.edge.decoboon.customer.model.CustomerOwnerGroup;
import com.th4.edge.decoboon.customer.model.CustomerOverviewReport;
import com.th4.edge.decoboon.customer.model.CustomerStageCount;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;

/** 经营角色客户聚合；客户范围始终由登录员工的数据范围重新解析。 */
@Component
public class CustomerOverviewContext {

    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;
    private final CustomerRepository customerRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;
    private final CustomerInsightContext insight;
    private final CustomerLossContext lossContext;

    public CustomerOverviewContext(TenantContext tenantContext,
                                   DataScopeContext dataScopeContext,
                                   CustomerRepository customerRepository,
                                   QuoteRepository quoteRepository,
                                   ProjectContractRepository contractRepository,
                                   CustomerInsightContext insight,
                                   CustomerLossContext lossContext) {
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
        this.customerRepository = customerRepository;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
        this.insight = insight;
        this.lossContext = lossContext;
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
        Employee operator = tenantContext.requireEmployee(principal);
        if (operator.getRole() != EmployeeRole.Boss
                && operator.getRole() != EmployeeRole.StoreManager
                && operator.getRole() != EmployeeRole.Finance) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色无权查看客户经营分组"));
        }
        DataScope scope = dataScopeContext.requireScope(operator);
        Long comId = scope.comId();
        boolean allStores = StoreScope.isAllStores(storeId);
        Long effectiveStoreId = allStores ? null : storeId;

        List<CustomerOwnerGroup> groups = ownerGroupsByScope(comId, allStores, effectiveStoreId);
        Map<CustStatus, Long> statusCounts = statusCountsByScope(comId, allStores, effectiveStoreId);
        long pendingAssign = customerRepository.countPendingAssignByStoreScope(comId, allStores, effectiveStoreId);
        long followDue = customerRepository.countFollowDueByStoreScope(
                comId, allStores, effectiveStoreId, LocalDate.now().plusDays(1).atStartOfDay());
        long total = 0;
        for (CustomerOwnerGroup group : groups) total += group.getCustomerCount();
        long pendingMeasure = statusCounts.getOrDefault(CustStatus.PendingMeasure, 0L);
        long signed = statusCounts.getOrDefault(CustStatus.Signed, 0L)
                + statusCounts.getOrDefault(CustStatus.Serving, 0L)
                + statusCounts.getOrDefault(CustStatus.Finished, 0L);

        CustomerOverviewReport report = new CustomerOverviewReport();
        LocalDate today = LocalDate.now();
        report.setAsOf(today);
        report.setMonth(today.withDayOfMonth(1));
        report.setMetrics(List.of(
                metric("newCustomer", "客户总数", total, "normal", "当前负责人分组范围", null),
                metric("activeFollow", "待跟进", followDue, followDue > 0 ? "attention" : "normal",
                        "含今日到期与逾期客户", null),
                metric("measureCustomer", "待量房", pendingMeasure, pendingAssign > 0 ? "attention" : "normal",
                        "待指派设计 " + pendingAssign + " 户", null),
                metric("signedCustomer", "已签约", signed, "normal", "含施工中与已完工客户", null)));
        report.setStageCounts(stageCounts(statusCounts, pendingAssign));
        report.setOwnerGroups(groups);

        int pageNumber = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        Specification<Customer> pageSpec = (root, query, builder) -> {
            var predicate = builder.equal(root.get("company").get("comId"), comId);
            if (!allStores) {
                predicate = builder.and(predicate,
                        builder.equal(root.get("store").get("storeId"), effectiveStoreId));
            }
            if (ownerId != null) {
                predicate = builder.and(predicate, ownerId == 0L
                        ? builder.isNull(root.get("owner"))
                        : builder.equal(root.get("owner").get("empId"), ownerId));
            }
            if (stage != null && !stage.isBlank() && !"all".equals(stage)) {
                if ("assign".equals(stage)) {
                    predicate = builder.and(predicate,
                            builder.equal(root.get("status"), CustStatus.PendingMeasure),
                            builder.isNotNull(root.get("measureAt")),
                            builder.isNull(root.get("designer")));
                } else {
                    CustStatus status = statusOf(stage);
                    if (status != null) predicate = builder.and(predicate, builder.equal(root.get("status"), status));
                }
            }
            String kw = keyword == null ? "" : keyword.trim().toLowerCase();
            if (!kw.isBlank()) {
                String pattern = "%" + kw + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(builder.lower(root.get("telNo")), pattern),
                        builder.like(builder.lower(root.get("community")), pattern),
                        builder.like(builder.lower(root.get("addr")), pattern)));
            }
            return predicate;
        };
        var page = customerRepository.findAll(pageSpec,
                org.springframework.data.domain.PageRequest.of(pageNumber, pageSize, sortOf(sort)));
        List<Customer> customers = new ArrayList<>(page.getContent());
        enrichPage(comId, customers, today);
        report.setCustomers(customers);
        report.setCurrent(pageNumber + 1);
        report.setSize(pageSize);
        report.setTotal(page.getTotalElements());
        report.setHasMore(pageNumber + 1 < page.getTotalPages());
        return report;
    }

    private List<CustomerOwnerGroup> ownerGroupsByScope(Long comId, boolean allStores, Long storeId) {
        return customerRepository.countOwnerGroupsByStoreScope(comId, allStores, storeId).stream()
                .map(row -> {
                    CustomerOwnerGroup group = new CustomerOwnerGroup();
                    group.setOwnerId(row[0] == null ? null : ((Number) row[0]).longValue());
                    group.setOwnerName(row[1] == null ? "未分配负责人" : String.valueOf(row[1]));
                    group.setCustomerCount(((Number) row[2]).intValue());
                    group.setPendingMeasureCount(((Number) row[3]).intValue());
                    group.setSignedCustomerCount(((Number) row[4]).intValue());
                    group.setRiskCustomerCount(0);
                    return group;
                })
                .sorted(Comparator.comparing((CustomerOwnerGroup item) -> item.getOwnerId() == null ? 1 : 0)
                        .thenComparing(CustomerOwnerGroup::getCustomerCount, Comparator.reverseOrder())
                        .thenComparing(CustomerOwnerGroup::getOwnerName, Comparator.nullsLast(String::compareTo)))
                .toList();
    }

    private Map<CustStatus, Long> statusCountsByScope(Long comId, boolean allStores, Long storeId) {
        Map<CustStatus, Long> result = new HashMap<>();
        for (Object[] row : customerRepository.countStatusByStoreScope(comId, allStores, storeId)) {
            result.put((CustStatus) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    private List<CustomerStageCount> stageCounts(Map<CustStatus, Long> counts, long pendingAssign) {
        List<CustomerStageCount> result = new ArrayList<>();
        result.add(stageCount("assign", "待指派设计", pendingAssign));
        for (CustStatus status : CustStatus.values()) {
            result.add(stageCount(stageKey(status), stageLabel(status), counts.getOrDefault(status, 0L)));
        }
        return result;
    }

    private CustomerStageCount stageCount(String key, String label, long count) {
        CustomerStageCount item = new CustomerStageCount();
        item.setKey(key);
        item.setLabel(label);
        item.setCount(Math.toIntExact(count));
        return item;
    }

    private void enrichPage(Long comId, List<Customer> customers, LocalDate today) {
        if (customers.isEmpty()) return;
        List<Long> customerIds = customers.stream().map(Customer::getCustId).toList();
        lossContext.enrich(customers);
        Map<Long, Quote> quotes = latestQuotes(
                quoteRepository.findByCompany_ComIdAndCustomer_CustIdInOrderByQuoteIdDesc(comId, customerIds));
        Map<Long, ProjectContract> contracts = latestContracts(
                contractRepository.findByCompany_ComIdAndCustomer_CustIdInOrderByContractIdDesc(comId, customerIds));
        customers.forEach(customer -> enrich(customer, today,
                quotes.get(customer.getCustId()), contracts.get(customer.getCustId())));
    }

    private org.springframework.data.domain.Sort sortOf(String sort) {
        if ("idle".equals(sort)) {
            return org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.asc("lastFollowAt"),
                    org.springframework.data.domain.Sort.Order.desc("custId"));
        }
        if ("value".equals(sort)) {
            return org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Order.desc("budget"),
                    org.springframework.data.domain.Sort.Order.desc("custId"));
        }
        if ("newest".equals(sort)) {
            return org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Order.desc("custId"));
        }
        return org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Order.asc("nextFollowAt"),
                org.springframework.data.domain.Sort.Order.desc("custId"));
    }

    private CustStatus statusOf(String stage) {
        for (CustStatus status : CustStatus.values()) {
            if (stageKey(status).equals(stage)) return status;
        }
        return null;
    }

    private String stageLabel(CustStatus status) {
        return switch (status) {
            case New -> "待首联";
            case Following -> "跟进中";
            case PendingMeasure -> "待量房";
            case Quoted -> "报价";
            case Signed -> "签约";
            case Serving -> "施工";
            case Finished -> "完工";
            case Lost -> "流失";
            case Dormant -> "休眠";
        };
    }

    private void enrich(Customer customer, LocalDate today, Quote quote, ProjectContract contract) {
        insight.enrich(customer, today, quote, contract);
        insight.mainline(customer, false, customer.getMeasureAt() != null);
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
            case Dormant -> "dormant";
        };
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

}

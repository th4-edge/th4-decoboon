package com.th4.edge.decoboon.ai.context;

import com.th4.edge.decoboon.ai.configure.AiBillingStatus;
import com.th4.edge.decoboon.ai.configure.AiStoreAllocation;
import com.th4.edge.decoboon.ai.configure.AiUsageStatus;
import com.th4.edge.decoboon.ai.model.AiUsageCall;
import com.th4.edge.decoboon.ai.model.AiUsageQuery;
import com.th4.edge.decoboon.ai.model.AiUsageReport;
import com.th4.edge.decoboon.ai.repository.AiUsageCallRepository;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** AI 用量只读统计上下文。 */
@Component
public class AiUsageReportContext {

    private final TenantContext tenantContext;
    private final AiUsageCallRepository repository;

    public AiUsageReportContext(TenantContext tenantContext, AiUsageCallRepository repository) {
        this.tenantContext = tenantContext;
        this.repository = repository;
    }

    public AiUsageReport report(Employee principal, AiUsageQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        LocalDate from = query == null || query.getFrom() == null
                ? LocalDate.now().withDayOfMonth(1) : query.getFrom();
        LocalDate to = query == null || query.getTo() == null ? LocalDate.now() : query.getTo();
        if (from.isAfter(to)) {
            throw new DefinedException(new DefaultErrorCode("400", "统计开始日期不能晚于结束日期"));
        }
        List<AiUsageCall> calls = repository.findAll(specification(current, query, from, to));
        AiUsageReport report = new AiUsageReport();
        report.setAsOf(LocalDate.now());
        report.setFrom(from);
        report.setTo(to);
        report.setRequestCount((long) calls.size());

        long valid = 0L;
        long success = 0L;
        long failure = 0L;
        long resourceUnits = 0L;
        long shadowUnits = 0L;
        long refundedUnits = 0L;
        Map<LocalDate, AiUsageReport.AiUsageTrendPoint> trend = new LinkedHashMap<>();
        Map<String, AiUsageReport.AiUsageStoreSummary> stores = new LinkedHashMap<>();
        Map<String, AiUsageReport.AiUsageCapabilitySummary> capabilities = new LinkedHashMap<>();

        for (AiUsageCall call : calls) {
            AiUsageStatus status = call.getStatus();
            boolean validCall = status != null && status.validCall();
            boolean succeeded = status != null && status.success();
            boolean failed = status != null && status.failure();
            if (validCall) valid++;
            if (succeeded) success++;
            if (failed) failure++;
            resourceUnits += nvl(call.getResourceUnits());
            if (succeeded && call.getBillingStatus() == AiBillingStatus.Calculated) {
                shadowUnits += nvl(call.getShadowBillableUnits());
                refundedUnits += nvl(call.getRefundedUnits());
            }

            LocalDate date = call.getStartedAt() == null ? null : call.getStartedAt().toLocalDate();
            if (date != null) {
                AiUsageReport.AiUsageTrendPoint point = trend.computeIfAbsent(date,
                        key -> new AiUsageReport.AiUsageTrendPoint(key, 0L, 0L, 0L));
                point.setRequestCount(point.getRequestCount() + 1);
                if (succeeded) point.setSuccessCount(point.getSuccessCount() + 1);
                if (succeeded && call.getBillingStatus() == AiBillingStatus.Calculated) {
                    point.setShadowBillableUnits(point.getShadowBillableUnits() + nvl(call.getShadowBillableUnits()));
                }
            }

            Store store = call.getAllocationStore();
            String storeKey = store == null ? "enterprise-public" : "store-" + store.getStoreId();
            AiUsageReport.AiUsageStoreSummary storeSummary = stores.computeIfAbsent(storeKey, key -> {
                AiUsageReport.AiUsageStoreSummary item = new AiUsageReport.AiUsageStoreSummary();
                item.setStoreId(store == null ? null : store.getStoreId());
                item.setStoreName(store == null ? "企业公共" : store.getName());
                item.setAllocationType(call.getAllocationType());
                return item;
            });
            storeSummary.setRequestCount(storeSummary.getRequestCount() + 1);
            if (succeeded) storeSummary.setSuccessCount(storeSummary.getSuccessCount() + 1);
            if (failed) storeSummary.setFailureCount(storeSummary.getFailureCount() + 1);
            storeSummary.setResourceUnits(storeSummary.getResourceUnits() + nvl(call.getResourceUnits()));
            if (succeeded && call.getBillingStatus() == AiBillingStatus.Calculated) {
                storeSummary.setShadowBillableUnits(storeSummary.getShadowBillableUnits() + nvl(call.getShadowBillableUnits()));
            }

            String capabilityCode = call.getCapabilityCode() == null ? "unknown" : call.getCapabilityCode();
            AiUsageReport.AiUsageCapabilitySummary capability = capabilities.computeIfAbsent(capabilityCode, key -> {
                AiUsageReport.AiUsageCapabilitySummary item = new AiUsageReport.AiUsageCapabilitySummary();
                item.setCapabilityCode(key);
                return item;
            });
            capability.setRequestCount(capability.getRequestCount() + 1);
            if (succeeded) capability.setSuccessCount(capability.getSuccessCount() + 1);
            if (failed) capability.setFailureCount(capability.getFailureCount() + 1);
            if (succeeded && call.getBillingStatus() == AiBillingStatus.Calculated) {
                capability.setShadowBillableUnits(capability.getShadowBillableUnits() + nvl(call.getShadowBillableUnits()));
            }
        }

        long netUnits = Math.max(0L, shadowUnits - refundedUnits);
        report.setValidCallCount(valid);
        report.setSuccessCount(success);
        report.setFailureCount(failure);
        report.setResourceUnits(resourceUnits);
        report.setShadowBillableUnits(shadowUnits);
        report.setRefundedUnits(refundedUnits);
        report.setNetBillableUnits(netUnits);
        report.setSuccessRate(rate(success, valid));
        report.setTrend(new ArrayList<>(trend.values()));
        report.getTrend().sort(Comparator.comparing(AiUsageReport.AiUsageTrendPoint::getDate));
        report.setStores(new ArrayList<>(stores.values()));
        report.getStores().sort(Comparator.comparing(AiUsageReport.AiUsageStoreSummary::getRequestCount).reversed()
                .thenComparing(AiUsageReport.AiUsageStoreSummary::getStoreName,
                        Comparator.nullsLast(String::compareTo)));
        report.setCapabilities(new ArrayList<>(capabilities.values()));
        report.getCapabilities().sort(Comparator.comparing(AiUsageReport.AiUsageCapabilitySummary::getRequestCount).reversed()
                .thenComparing(AiUsageReport.AiUsageCapabilitySummary::getCapabilityCode));
        return report;
    }

    public Page<AiUsageCall> page(Employee principal, AiUsageQuery query) {
        Employee current = tenantContext.requireBoss(principal);
        LocalDate from = query == null || query.getFrom() == null
                ? LocalDate.now().withDayOfMonth(1) : query.getFrom();
        LocalDate to = query == null || query.getTo() == null ? LocalDate.now() : query.getTo();
        if (from.isAfter(to)) {
            throw new DefinedException(new DefaultErrorCode("400", "查询开始日期不能晚于结束日期"));
        }
        int currentPage = query == null || query.getCurrent() == null ? 1 : Math.max(1, query.getCurrent());
        int size = query == null || query.getSize() == null ? 20 : Math.max(1, Math.min(200, query.getSize()));
        return repository.findAll(specification(current, query, from, to),
                PageRequest.of(currentPage - 1, size, Sort.by(Sort.Direction.DESC, "startedAt")));
    }

    private Specification<AiUsageCall> specification(Employee current, AiUsageQuery query,
                                                     LocalDate from, LocalDate to) {
        Long companyId = current.getCompany().getComId();
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("company").get("comId"), companyId));
            predicates.add(cb.greaterThanOrEqualTo(root.get("startedAt"), from.atStartOfDay()));
            predicates.add(cb.lessThan(root.get("startedAt"), to.plusDays(1).atStartOfDay()));
            if (query != null && query.getStoreId() != null) {
                predicates.add(cb.equal(root.get("allocationStore").get("storeId"), query.getStoreId()));
            }
            if (query != null && hasText(query.getCapabilityCode())) {
                predicates.add(cb.equal(root.get("capabilityCode"), query.getCapabilityCode().trim()));
            }
            if (query != null && hasText(query.getBusinessModule())) {
                predicates.add(cb.equal(root.get("businessModule"), query.getBusinessModule().trim()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private long nvl(Long value) {
        return value == null ? 0L : value;
    }

    private BigDecimal rate(long numerator, long denominator) {
        if (denominator == 0L) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

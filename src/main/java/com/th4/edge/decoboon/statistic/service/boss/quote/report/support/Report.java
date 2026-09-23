package com.th4.edge.decoboon.statistic.service.boss.quote.report.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.BossQuoteCustomer;
import com.th4.edge.decoboon.statistic.model.BossQuoteDesignerGroup;
import com.th4.edge.decoboon.statistic.model.BossQuoteHistory;
import com.th4.edge.decoboon.statistic.model.BossQuoteProperty;
import com.th4.edge.decoboon.statistic.model.BossQuoteQuery;
import com.th4.edge.decoboon.statistic.model.BossQuoteReport;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 企业负责人报价谈单聚合实现，按客户而非报价单展示经营事实。 */
@Note("企业负责人报价谈单报告")
@Properties(allow = {
        @AllowProperties(model = BossQuoteReport.class,
                properties = {"asOf", "customerCount", "activeCustomerCount", "pendingConfirmCustomerCount",
                        "confirmedCustomerCount", "riskCustomerCount", "talkGeneratedCustomerCount",
                        "quoteCount", "latestQuoteAmount", "acceptedAmount", "customers", "designerGroups"}),
        @AllowProperties(model = BossQuoteCustomer.class,
                properties = {"custId", "customerName", "telNo", "propertyCount", "quoteCount",
                        "draftPropertyCount", "submittedPropertyCount", "rejectedPropertyCount",
                        "confirmedPropertyCount", "talkPropertyCount", "riskPropertyCount",
                        "latestQuoteAmount", "acceptedAmount", "designerId", "designerName",
                        "designerStoreName", "nextAction", "riskLevel", "lastActivityAt", "properties"}),
        @AllowProperties(model = BossQuoteDesignerGroup.class,
                properties = {"designerId", "designerName", "storeName", "customerCount",
                        "riskCustomerCount", "pendingConfirmCustomerCount", "confirmedCustomerCount",
                        "quoteCount", "quoteAmount", "acceptedAmount", "lastActivityAt", "customers"}),
        @AllowProperties(model = BossQuoteProperty.class,
                properties = {"propertyId", "propertyNo", "community", "addr", "area", "designerName",
                        "quoteCount", "latestQuoteId", "latestQuoteNo", "latestQuoteVersion",
                        "latestQuoteAmount", "latestQuoteStatus", "latestQuoteStatusName",
                        "latestQuoteCreatedAt", "latestQuoteSubmittedAt", "latestQuoteAcceptedAt",
                        "talkCount", "latestTalkId", "latestTalkStatus", "latestTalkStatusName",
                        "latestTalkBuiltAt", "latestTalkOutputUrl", "customerConfirmed",
                        "customerConfirmedAt", "nextAction", "stayDays", "riskLevel", "history"}),
        @AllowProperties(model = BossQuoteHistory.class,
                properties = {"quoteId", "quoteNo", "version", "amount", "status", "statusName",
                        "createdAt", "submittedAt", "acceptedAt"})
})
@Service("statistic/boss/quote/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.boss.quote.report.Report {

    private static final int HISTORY_LIMIT = 10;
    private static final Set<QuoteStatus> CONFIRMED_STATUSES = Set.of(QuoteStatus.Accepted, QuoteStatus.Converted);

    @User
    private Employee employee;

    private final TenantContext tenantContext;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final QuoteRepository quoteRepository;
    private final TalkRepository talkRepository;

    public Report(TenantContext tenantContext,
                  CustomerRepository customerRepository,
                  PropertyRepository propertyRepository,
                  QuoteRepository quoteRepository,
                  TalkRepository talkRepository) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.quoteRepository = quoteRepository;
        this.talkRepository = talkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BossQuoteReport call(BossQuoteQuery request) {
        Employee current = tenantContext.requireBoss(employee);
        Long comId = current.getCompany().getComId();
        LocalDateTime now = LocalDateTime.now();

        List<Customer> customers = customerRepository.findByCompany_ComId(comId);
        List<Long> customerIds = customers.stream().map(Customer::getCustId).toList();
        List<Property> properties = customerIds.isEmpty()
                ? List.of()
                : propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds);
        Map<Long, Property> propertyById = properties.stream()
                .filter(property -> property.getPropertyId() != null)
                .collect(Collectors.toMap(Property::getPropertyId, Function.identity(), (left, right) -> left));

        Map<Long, List<Quote>> quotesByProperty = quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(comId).stream()
                .filter(quote -> quote.getProperty() != null && quote.getProperty().getPropertyId() != null)
                .collect(Collectors.groupingBy(quote -> quote.getProperty().getPropertyId()));
        Map<Long, List<Talk>> talksByProperty = talkRepository.findByCompany_ComIdOrderByTalkIdDesc(comId).stream()
                .filter(talk -> talk.getProperty() != null && talk.getProperty().getPropertyId() != null)
                .collect(Collectors.groupingBy(talk -> talk.getProperty().getPropertyId()));

        List<BossQuoteCustomer> customerReports = new ArrayList<>();
        for (Map.Entry<Long, List<Quote>> entry : quotesByProperty.entrySet()) {
            Property property = propertyById.get(entry.getKey());
            if (property == null || property.getCustomer() == null) {
                continue;
            }
            Customer customer = property.getCustomer();
            BossQuoteProperty item = buildProperty(property, entry.getValue(),
                    talksByProperty.getOrDefault(property.getPropertyId(), List.of()), now);
            BossQuoteCustomer existing = customerReports.stream()
                    .filter(report -> Objects.equals(report.getCustId(), customer.getCustId()))
                    .findFirst()
                    .orElse(null);
            if (existing == null) {
                existing = newCustomer(customer);
                existing.setProperties(new ArrayList<>());
                customerReports.add(existing);
            }
            existing.getProperties().add(item);
        }

        Long customerId = request == null ? null : request.getCustomerId();
        customerReports = customerReports.stream()
                .filter(report -> customerId == null || customerId.equals(report.getCustId()))
                .peek(this::summarizeCustomer)
                .sorted(Comparator
                        .comparingInt((BossQuoteCustomer item) -> riskOrder(item.getRiskLevel()))
                        .thenComparing(BossQuoteCustomer::getLastActivityAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(BossQuoteCustomer::getCustId, Comparator.reverseOrder()))
                .collect(Collectors.toCollection(ArrayList::new));

        BossQuoteReport report = new BossQuoteReport();
        report.setAsOf(LocalDate.now());
        report.setCustomers(customerReports);
        summarizeReport(report, customerReports);
        return report;
    }

    private BossQuoteProperty buildProperty(Property property,
                                            List<Quote> quotes,
                                            List<Talk> talks,
                                            LocalDateTime now) {
        List<Quote> sortedQuotes = quotes.stream()
                .filter(quote -> quote.getQuoteId() != null)
                .sorted(Comparator.comparing(Quote::getQuoteId).reversed())
                .toList();
        Quote latest = latestQuote(sortedQuotes);
        Talk latestTalk = talks.stream()
                .filter(talk -> talk.getTalkId() != null)
                .max(Comparator.comparing(Talk::getTalkId))
                .orElse(null);

        BossQuoteProperty item = new BossQuoteProperty();
        item.setPropertyId(property.getPropertyId());
        item.setPropertyNo(property.getPropertyNo());
        item.setCommunity(firstNonBlank(property.getCommunity(), property.getCustomer().getCommunity()));
        item.setAddr(firstNonBlank(property.getAddress(), property.getCustomer().getAddr()));
        item.setArea(property.getArea() == null ? property.getCustomer().getArea() : property.getArea());
        item.setDesignerName(resolveDesignerName(property, latest));
        item.setQuoteCount(sortedQuotes.size());

        if (latest != null) {
            item.setLatestQuoteId(latest.getQuoteId());
            item.setLatestQuoteNo(latest.getNo());
            item.setLatestQuoteVersion(latest.getVersion());
            item.setLatestQuoteAmount(latest.getAmount());
            item.setLatestQuoteStatus(latest.getStatus().name());
            item.setLatestQuoteStatusName(latest.getStatus().getName());
            item.setLatestQuoteCreatedAt(createTime(latest));
            item.setLatestQuoteSubmittedAt(submittedAt(latest));
            item.setLatestQuoteAcceptedAt(acceptedAt(latest));
            item.setCustomerConfirmed(CONFIRMED_STATUSES.contains(latest.getStatus()));
            item.setCustomerConfirmedAt(item.getCustomerConfirmed() ? acceptedAt(latest) : null);
        } else {
            item.setCustomerConfirmed(false);
        }

        item.setTalkCount(talks.size());
        if (latestTalk != null) {
            item.setLatestTalkId(latestTalk.getTalkId());
            item.setLatestTalkStatus(latestTalk.getStatus().name());
            item.setLatestTalkStatusName(latestTalk.getStatus().getName());
            item.setLatestTalkBuiltAt(latestTalk.getBuiltAt());
            item.setLatestTalkOutputUrl(latestTalk.getOutputUrl());
        }

        String stage = stage(latest);
        LocalDateTime stageStart = stageStart(stage, latest, now);
        long stayDays = Math.max(0, Duration.between(stageStart, now).toDays());
        item.setStayDays(stayDays);
        item.setRiskLevel(riskLevel(stage, stayDays));
        item.setNextAction(nextAction(stage));
        item.setHistory(sortedQuotes.stream().limit(HISTORY_LIMIT).map(this::history).toList());
        return item;
    }

    private BossQuoteCustomer newCustomer(Customer customer) {
        BossQuoteCustomer item = new BossQuoteCustomer();
        item.setCustId(customer.getCustId());
        item.setCustomerName(customer.getName());
        item.setTelNo(customer.getTelNo());
        Employee designer = customer.getDesigner();
        if (designer != null) {
            item.setDesignerId(designer.getEmpId());
            item.setDesignerName(designer.getName());
            item.setDesignerStoreName(designer.getStore() == null ? null : designer.getStore().getName());
        }
        return item;
    }

    private void summarizeCustomer(BossQuoteCustomer customer) {
        List<BossQuoteProperty> properties = customer.getProperties();
        properties.sort(Comparator
                .comparingInt((BossQuoteProperty item) -> riskOrder(item.getRiskLevel()))
                .thenComparing(this::lastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BossQuoteProperty::getPropertyId, Comparator.nullsLast(Comparator.reverseOrder())));
        customer.setPropertyCount(properties.size());
        customer.setQuoteCount(properties.stream().mapToInt(item -> value(item.getQuoteCount())).sum());
        customer.setDraftPropertyCount(countLatest(properties, QuoteStatus.Draft));
        customer.setSubmittedPropertyCount(countLatest(properties, QuoteStatus.Submitted));
        customer.setRejectedPropertyCount(countLatest(properties, QuoteStatus.Rejected));
        customer.setConfirmedPropertyCount((int) properties.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCustomerConfirmed()))
                .count());
        customer.setTalkPropertyCount((int) properties.stream().filter(item -> value(item.getTalkCount()) > 0).count());
        customer.setRiskPropertyCount((int) properties.stream().filter(item -> !"Normal".equals(item.getRiskLevel())).count());
        customer.setLatestQuoteAmount(properties.stream()
                .filter(item -> item.getLatestQuoteAmount() != null)
                .filter(item -> !QuoteStatus.Rejected.name().equals(item.getLatestQuoteStatus()))
                .filter(item -> !QuoteStatus.Superseded.name().equals(item.getLatestQuoteStatus()))
                .map(BossQuoteProperty::getLatestQuoteAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        customer.setAcceptedAmount(properties.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCustomerConfirmed()))
                .map(BossQuoteProperty::getLatestQuoteAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        customer.setRiskLevel(properties.stream()
                .map(BossQuoteProperty::getRiskLevel)
                .min(Comparator.comparingInt(this::riskOrder))
                .orElse("Normal"));
        customer.setLastActivityAt(properties.stream()
                .map(this::lastActivityAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null));
        customer.setNextAction(nextAction(properties));
    }

    private void summarizeReport(BossQuoteReport report, List<BossQuoteCustomer> customers) {
        report.setCustomerCount(customers.size());
        report.setActiveCustomerCount((int) customers.stream()
                .filter(item -> item.getDraftPropertyCount() > 0
                        || item.getSubmittedPropertyCount() > 0
                        || item.getRejectedPropertyCount() > 0)
                .count());
        report.setPendingConfirmCustomerCount((int) customers.stream()
                .filter(item -> item.getSubmittedPropertyCount() > 0)
                .count());
        report.setConfirmedCustomerCount((int) customers.stream()
                .filter(item -> item.getConfirmedPropertyCount() > 0)
                .count());
        report.setRiskCustomerCount((int) customers.stream()
                .filter(item -> !"Normal".equals(item.getRiskLevel()))
                .count());
        report.setTalkGeneratedCustomerCount((int) customers.stream()
                .filter(item -> item.getTalkPropertyCount() > 0)
                .count());
        report.setQuoteCount(customers.stream().mapToInt(item -> value(item.getQuoteCount())).sum());
        report.setLatestQuoteAmount(customers.stream()
                .map(BossQuoteCustomer::getLatestQuoteAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setAcceptedAmount(customers.stream()
                .map(BossQuoteCustomer::getAcceptedAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setDesignerGroups(buildDesignerGroups(customers));
    }

    private Quote latestQuote(List<Quote> quotes) {
        Quote confirmed = quotes.stream()
                .filter(quote -> CONFIRMED_STATUSES.contains(quote.getStatus()))
                .max(Comparator.comparing(Quote::getQuoteId))
                .orElse(null);
        if (confirmed != null) return confirmed;
        return quotes.stream()
                .filter(quote -> quote.getStatus() != QuoteStatus.Superseded)
                .max(Comparator.comparing(Quote::getQuoteId))
                .orElse(quotes.isEmpty() ? null : quotes.get(0));
    }

    private BossQuoteHistory history(Quote quote) {
        BossQuoteHistory item = new BossQuoteHistory();
        item.setQuoteId(quote.getQuoteId());
        item.setQuoteNo(quote.getNo());
        item.setVersion(quote.getVersion());
        item.setAmount(quote.getAmount());
        item.setStatus(quote.getStatus().name());
        item.setStatusName(quote.getStatus().getName());
        item.setCreatedAt(createTime(quote));
        item.setSubmittedAt(submittedAt(quote));
        item.setAcceptedAt(acceptedAt(quote));
        return item;
    }

    private String stage(Quote quote) {
        if (quote == null) return "Empty";
        return switch (quote.getStatus()) {
            case Draft -> "Draft";
            case Submitted -> "Submitted";
            case Rejected -> "Rejected";
            case Accepted -> "Confirmed";
            case Converted -> "Converted";
            case Superseded -> "History";
        };
    }

    private String nextAction(String stage) {
        return switch (stage) {
            case "Draft" -> "完善并提交报价";
            case "Submitted" -> "推动客户确认";
            case "Rejected" -> "按反馈调整并重报";
            case "Confirmed" -> "推进合同签约";
            case "Converted" -> "查看施工项目";
            default -> "补充报价方案";
        };
    }

    private String nextAction(List<BossQuoteProperty> properties) {
        if (properties.stream().anyMatch(item -> "Submitted".equals(item.getLatestQuoteStatus()))) {
            return "推动客户确认";
        }
        if (properties.stream().anyMatch(item -> "Rejected".equals(item.getLatestQuoteStatus()))) {
            return "处理驳回报价";
        }
        if (properties.stream().anyMatch(item -> "Draft".equals(item.getLatestQuoteStatus()))) {
            return "完善并提交报价";
        }
        if (properties.stream().anyMatch(item -> "Confirmed".equals(item.getLatestQuoteStatus()))) {
            return "推进合同签约";
        }
        if (properties.stream().anyMatch(item -> "Converted".equals(item.getLatestQuoteStatus()))) {
            return "查看施工项目";
        }
        return "补充报价方案";
    }

    private String riskLevel(String stage, long stayDays) {
        if (Set.of("Submitted", "Confirmed").contains(stage)) {
            if (stayDays >= 7) return "Danger";
            if (stayDays >= 3) return "Warning";
            return "Normal";
        }
        if ("Rejected".equals(stage)) {
            if (stayDays >= 3) return "Danger";
            if (stayDays >= 1) return "Warning";
            return "Normal";
        }
        if ("Draft".equals(stage)) {
            if (stayDays >= 14) return "Danger";
            if (stayDays >= 7) return "Warning";
        }
        return "Normal";
    }

    private LocalDateTime stageStart(String stage, Quote quote, LocalDateTime now) {
        if (quote == null) return now;
        if (Set.of("Confirmed", "Converted").contains(stage)) {
            return firstNonNull(acceptedAt(quote), quote.getUpdateTime() == null ? null : quote.getUpdateTime().toLocalDateTime(),
                    createTime(quote), now);
        }
        if ("Submitted".equals(stage)) {
            return firstNonNull(submittedAt(quote), createTime(quote), now);
        }
        return firstNonNull(quote.getUpdateTime() == null ? null : quote.getUpdateTime().toLocalDateTime(),
                createTime(quote), now);
    }

    private String resolveDesignerName(Property property, Quote latest) {
        if (property.getCustomer() != null && property.getCustomer().getDesigner() != null) {
            return property.getCustomer().getDesigner().getName();
        }
        if (property.getDesigner() != null) return property.getDesigner().getName();
        if (latest != null && latest.getDesign() != null && latest.getDesign().getOwner() != null) {
            return latest.getDesign().getOwner().getName();
        }
        return latest != null && latest.getCreatedBy() != null ? latest.getCreatedBy().getName() : null;
    }

    private List<BossQuoteDesignerGroup> buildDesignerGroups(List<BossQuoteCustomer> customers) {
        Map<Long, BossQuoteDesignerGroup> grouped = new LinkedHashMap<>();
        for (BossQuoteCustomer customer : customers) {
            Long key = customer.getDesignerId() == null ? 0L : customer.getDesignerId();
            BossQuoteDesignerGroup group = grouped.computeIfAbsent(key, ignored -> {
                BossQuoteDesignerGroup created = new BossQuoteDesignerGroup();
                created.setDesignerId(customer.getDesignerId());
                created.setDesignerName(customer.getDesignerId() == null ? "待指派设计" : customer.getDesignerName());
                created.setStoreName(customer.getDesignerStoreName());
                created.setCustomers(new ArrayList<>());
                return created;
            });
            group.getCustomers().add(customer);
        }
        List<BossQuoteDesignerGroup> groups = new ArrayList<>(grouped.values());
        for (BossQuoteDesignerGroup group : groups) {
            List<BossQuoteCustomer> members = group.getCustomers();
            group.setCustomerCount(members.size());
            group.setRiskCustomerCount((int) members.stream().filter(item -> !"Normal".equals(item.getRiskLevel())).count());
            group.setPendingConfirmCustomerCount((int) members.stream().filter(item -> value(item.getSubmittedPropertyCount()) > 0).count());
            group.setConfirmedCustomerCount((int) members.stream().filter(item -> value(item.getConfirmedPropertyCount()) > 0).count());
            group.setQuoteCount(members.stream().mapToInt(item -> value(item.getQuoteCount())).sum());
            group.setQuoteAmount(members.stream()
                    .map(BossQuoteCustomer::getLatestQuoteAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            group.setAcceptedAmount(members.stream()
                    .map(BossQuoteCustomer::getAcceptedAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            group.setLastActivityAt(members.stream()
                    .map(BossQuoteCustomer::getLastActivityAt)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null));
        }
        groups.sort(Comparator
                .comparingInt((BossQuoteDesignerGroup group) -> group.getDesignerId() == null ? 0 : 1)
                .thenComparing(BossQuoteDesignerGroup::getRiskCustomerCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BossQuoteDesignerGroup::getPendingConfirmCustomerCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BossQuoteDesignerGroup::getQuoteAmount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BossQuoteDesignerGroup::getLastActivityAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return groups;
    }

    private int countLatest(List<BossQuoteProperty> properties, QuoteStatus status) {
        return (int) properties.stream()
                .filter(item -> status.name().equals(item.getLatestQuoteStatus()))
                .count();
    }

    private LocalDateTime lastActivityAt(BossQuoteProperty item) {
        return latest(
                item.getLatestQuoteAcceptedAt(),
                item.getLatestQuoteSubmittedAt(),
                item.getLatestQuoteCreatedAt(),
                item.getLatestTalkBuiltAt());
    }

    private LocalDateTime createTime(Quote quote) {
        return quote == null || quote.getCreateTime() == null ? null : quote.getCreateTime().toLocalDateTime();
    }

    private LocalDateTime submittedAt(Quote quote) {
        if (quote == null) return null;
        if (quote.getSubmittedAt() != null) return quote.getSubmittedAt();
        if (quote.getStatus() == QuoteStatus.Draft) return null;
        return quote.getUpdateTime() == null ? createTime(quote) : quote.getUpdateTime().toLocalDateTime();
    }

    private LocalDateTime acceptedAt(Quote quote) {
        if (quote == null || !CONFIRMED_STATUSES.contains(quote.getStatus())) return null;
        if (quote.getAcceptedAt() != null) return quote.getAcceptedAt();
        if (quote.getUpdateTime() != null) return quote.getUpdateTime().toLocalDateTime();
        return createTime(quote);
    }

    private int riskOrder(String level) {
        if ("Danger".equals(level)) return 0;
        if ("Warning".equals(level)) return 1;
        return 2;
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private LocalDateTime latest(LocalDateTime... values) {
        LocalDateTime result = null;
        for (LocalDateTime value : values) {
            if (value != null && (result == null || value.isAfter(result))) result = value;
        }
        return result;
    }

    private LocalDateTime firstNonNull(LocalDateTime... values) {
        for (LocalDateTime value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }
}

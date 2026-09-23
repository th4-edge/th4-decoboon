package com.th4.edge.decoboon.statistic.service.boss.design.report.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.configure.DesignStatus;
import com.th4.edge.decoboon.design.configure.TalkStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.design.repository.TalkRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.configure.QuoteStatus;
import com.th4.edge.decoboon.quote.model.Quote;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.decoboon.statistic.model.BossDesignItem;
import com.th4.edge.decoboon.statistic.model.BossDesignQuery;
import com.th4.edge.decoboon.statistic.model.BossDesignReport;
import com.th4.edge.decoboon.statistic.model.BossDesignerLoad;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.configure.EmployeeStatus;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.repository.EmployeeRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 企业负责人视角的设计、报价、谈单与客户确认经营报告。 */
@Note("企业负责人设计经营报告")
@Properties(allow = {
        @AllowProperties(model = BossDesignReport.class,
                properties = {"asOf", "pipelineCount", "unassignedCount", "requirementPendingCount",
                        "schemePendingCount", "designingCount", "quoteDraftCount", "quoteSubmittedCount",
                        "confirmedCount", "contractedCount", "talkGeneratedCount", "riskCount", "quoteAmount",
                        "acceptedAmount", "designers", "items"}),
        @AllowProperties(model = BossDesignerLoad.class,
                properties = {"designerId", "designerName", "storeName", "customerCount", "schemeCount",
                        "quoteCount", "talkCount", "confirmedCount", "riskCount", "quoteAmount",
                        "acceptedAmount", "lastUpdateAt"}),
        @AllowProperties(model = BossDesignItem.class,
                properties = {"custId", "propertyId", "customerName", "telNo", "community", "addr", "area",
                        "designerId", "designerName", "designerStoreName", "measureAt", "pendingMeasureAt",
                        "stage", "stageName", "nextAction", "stayDays", "riskLevel", "requirementCount",
                        "latestRequirementTitle", "schemeCount", "latestSchemeId", "latestSchemeNo",
                        "latestSchemeName", "latestSchemeStatus", "latestSchemeStatusName",
                        "latestSchemeSpaceCount", "latestSchemeItemCount", "latestDesignAt", "quoteCount",
                        "latestQuoteId", "latestQuoteNo", "latestQuoteVersion", "latestQuoteAmount",
                        "latestQuoteStatus", "latestQuoteStatusName", "latestQuoteSubmittedAt",
                        "latestQuoteAcceptedAt", "talkCount", "latestTalkId", "latestTalkNo",
                        "latestTalkStatus", "latestTalkStatusName", "latestTalkBuiltAt", "latestTalkOutputUrl",
                        "customerConfirmed", "customerConfirmedAt"})
})
@Service("statistic/boss/design/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.boss.design.report.Report {

    private static final Set<QuoteStatus> CONFIRMED_QUOTE_STATUSES = EnumSet.of(
            QuoteStatus.Accepted, QuoteStatus.Converted);

    @User
    private Employee employee;

    private final TenantContext tenantContext;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final DesignRepository designRepository;
    private final RequirementRepository requirementRepository;
    private final SpaceRepository spaceRepository;
    private final DesignItemRepository designItemRepository;
    private final QuoteRepository quoteRepository;
    private final TalkRepository talkRepository;
    private final MeasureRepository measureRepository;
    private final EmployeeRepository employeeRepository;

    public Report(TenantContext tenantContext,
                  CustomerRepository customerRepository,
                  PropertyRepository propertyRepository,
                  DesignRepository designRepository,
                  RequirementRepository requirementRepository,
                  SpaceRepository spaceRepository,
                  DesignItemRepository designItemRepository,
                  QuoteRepository quoteRepository,
                  TalkRepository talkRepository,
                  MeasureRepository measureRepository,
                  EmployeeRepository employeeRepository) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.designRepository = designRepository;
        this.requirementRepository = requirementRepository;
        this.spaceRepository = spaceRepository;
        this.designItemRepository = designItemRepository;
        this.quoteRepository = quoteRepository;
        this.talkRepository = talkRepository;
        this.measureRepository = measureRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BossDesignReport call(BossDesignQuery request) {
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

        List<Design> designs = designRepository.findByCompany_ComIdOrderByDesignIdDesc(comId);
        List<Requirement> requirements = requirementRepository.findByCompany_ComIdOrderByRequirementIdDesc(comId);
        List<Quote> quotes = quoteRepository.findByCompany_ComIdOrderByQuoteIdDesc(comId);
        List<Talk> talks = talkRepository.findByCompany_ComIdOrderByTalkIdDesc(comId);

        List<Long> propertyIds = properties.stream()
                .map(Property::getPropertyId)
                .filter(Objects::nonNull)
                .toList();
        List<Measure> measures = propertyIds.isEmpty()
                ? List.of()
                : measureRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(comId, propertyIds);

        Map<Long, List<Design>> designsByProperty = groupDesignsByProperty(designs);
        Map<Long, List<Requirement>> requirementsByProperty = groupRequirementsByProperty(requirements);
        Map<Long, List<Quote>> quotesByProperty = groupQuotesByProperty(quotes);
        Map<Long, List<Talk>> talksByProperty = groupTalksByProperty(talks);
        Map<Long, List<Measure>> measuresByProperty = groupMeasuresByProperty(measures);

        Set<Long> designIds = designs.stream().map(Design::getDesignId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> spaceCountByDesign = countSpacesByDesign(designIds);
        Map<Long, Long> itemCountByDesign = countItemsByDesign(designIds);

        Set<Long> candidatePropertyIds = new HashSet<>();
        candidatePropertyIds.addAll(designsByProperty.keySet());
        candidatePropertyIds.addAll(requirementsByProperty.keySet());
        candidatePropertyIds.addAll(quotesByProperty.keySet());
        candidatePropertyIds.addAll(talksByProperty.keySet());
        properties.stream()
                .filter(property -> property.getDesigner() != null)
                .map(Property::getPropertyId)
                .filter(Objects::nonNull)
                .forEach(candidatePropertyIds::add);

        List<BossDesignItem> items = new ArrayList<>();
        for (Long propertyId : candidatePropertyIds) {
            Property property = propertyById.get(propertyId);
            if (property == null || property.getCustomer() == null) {
                continue;
            }
            items.add(buildItem(comId, property,
                    designsByProperty.getOrDefault(propertyId, List.of()),
                    requirementsByProperty.getOrDefault(propertyId, List.of()),
                    quotesByProperty.getOrDefault(propertyId, List.of()),
                    talksByProperty.getOrDefault(propertyId, List.of()),
                    measuresByProperty.getOrDefault(propertyId, List.of()),
                    spaceCountByDesign,
                    itemCountByDesign,
                    now));
        }

        Long designerId = request == null ? null : request.getDesignerId();
        if (designerId != null) {
            items = items.stream()
                    .filter(item -> designerId.equals(item.getDesignerId()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
        items.sort(Comparator
                .comparingInt((BossDesignItem item) -> riskOrder(item.getRiskLevel()))
                .thenComparing(BossDesignItem::getStayDays,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BossDesignItem::getPropertyId, Comparator.nullsLast(Comparator.reverseOrder())));

        int unassignedCount = designerId == null ? unassignedCustomerCount(customers, properties, measures, now) : 0;
        List<BossDesignerLoad> designerLoads = buildDesignerLoads(comId, items);
        BossDesignReport report = new BossDesignReport();
        report.setAsOf(LocalDate.now());
        report.setDesigners(designerLoads);
        report.setItems(items);
        summarize(report, items, unassignedCount);
        return report;
    }

    private BossDesignItem buildItem(Long comId,
                                     Property property,
                                     List<Design> designs,
                                     List<Requirement> requirements,
                                     List<Quote> quotes,
                                     List<Talk> talks,
                                     List<Measure> measures,
                                     Map<Long, Long> spaceCountByDesign,
                                     Map<Long, Long> itemCountByDesign,
                                     LocalDateTime now) {
        Customer customer = property.getCustomer();
        Design latestDesign = latestDesign(designs);
        Requirement latestRequirement = latestRequirement(requirements);
        Quote latestQuote = latestQuote(quotes);
        Talk latestTalk = latestTalk(talks);
        Employee designer = resolveDesigner(property, customer, latestDesign, latestRequirement, latestTalk, latestQuote);
        LocalDateTime measureAt = completedMeasureAt(measures, property.getLatestMeasureAt());
        LocalDateTime pendingMeasureAt = scheduledMeasureAt(measures, now);
        LocalDateTime latestDesignAt = latestDesignAt(latestDesign);

        String stage = resolveStage(designer, requirements, latestDesign, latestQuote);
        LocalDateTime stageStart = stageStart(stage, measureAt, pendingMeasureAt, latestDesignAt,
                latestQuote, property, now);
        long stayDays = Math.max(0, Duration.between(stageStart, now).toDays());

        BossDesignItem item = new BossDesignItem();
        item.setCustId(customer.getCustId());
        item.setPropertyId(property.getPropertyId());
        item.setCustomerName(customer.getName());
        item.setTelNo(customer.getTelNo());
        item.setCommunity(firstNonBlank(property.getCommunity(), customer.getCommunity()));
        item.setAddr(firstNonBlank(property.getAddress(), customer.getAddr()));
        item.setArea(property.getArea() == null ? customer.getArea() : property.getArea());
        item.setDesignerId(designer == null ? null : designer.getEmpId());
        item.setDesignerName(designer == null ? null : designer.getName());
        item.setDesignerStoreName(designer == null || designer.getStore() == null ? null : designer.getStore().getName());
        item.setMeasureAt(measureAt);
        item.setPendingMeasureAt(pendingMeasureAt);
        item.setStage(stage);
        item.setStageName(stageName(stage));
        item.setNextAction(nextAction(stage));
        item.setStayDays(stayDays);
        item.setRiskLevel(riskLevel(stage, stayDays));

        item.setRequirementCount(requirements.size());
        item.setLatestRequirementTitle(latestRequirement == null ? null : latestRequirement.getTitle());
        item.setSchemeCount(designs.size());
        if (latestDesign != null) {
            item.setLatestSchemeId(latestDesign.getDesignId());
            item.setLatestSchemeNo(latestDesign.getNo());
            item.setLatestSchemeName(latestDesign.getName());
            item.setLatestSchemeStatus(latestDesign.getStatus().name());
            item.setLatestSchemeStatusName(latestDesign.getStatus().getName());
            item.setLatestSchemeSpaceCount(spaceCountByDesign.getOrDefault(latestDesign.getDesignId(), 0L).intValue());
            item.setLatestSchemeItemCount(itemCountByDesign.getOrDefault(latestDesign.getDesignId(), 0L).intValue());
        }
        item.setLatestDesignAt(latestDesignAt);

        item.setQuoteCount(quotes.size());
        if (latestQuote != null) {
            item.setLatestQuoteId(latestQuote.getQuoteId());
            item.setLatestQuoteNo(latestQuote.getNo());
            item.setLatestQuoteVersion(latestQuote.getVersion());
            item.setLatestQuoteAmount(latestQuote.getAmount());
            item.setLatestQuoteStatus(latestQuote.getStatus().name());
            item.setLatestQuoteStatusName(latestQuote.getStatus().getName());
            item.setLatestQuoteSubmittedAt(submittedAt(latestQuote));
            item.setLatestQuoteAcceptedAt(acceptedAt(latestQuote));
        }

        item.setTalkCount(talks.size());
        if (latestTalk != null) {
            item.setLatestTalkId(latestTalk.getTalkId());
            item.setLatestTalkNo(latestTalk.getNo());
            item.setLatestTalkStatus(latestTalk.getStatus().name());
            item.setLatestTalkStatusName(latestTalk.getStatus().getName());
            item.setLatestTalkBuiltAt(latestTalk.getBuiltAt());
            item.setLatestTalkOutputUrl(latestTalk.getOutputUrl());
        }

        boolean confirmed = latestQuote != null && CONFIRMED_QUOTE_STATUSES.contains(latestQuote.getStatus());
        item.setCustomerConfirmed(confirmed);
        item.setCustomerConfirmedAt(confirmed ? acceptedAt(latestQuote) : null);
        return item;
    }

    private void summarize(BossDesignReport report, List<BossDesignItem> items, int unassignedCount) {
        report.setPipelineCount(items.size());
        report.setUnassignedCount(unassignedCount);
        report.setRequirementPendingCount(countStage(items, "RequirementPending"));
        report.setSchemePendingCount(countStage(items, "SchemePending"));
        report.setDesigningCount(items.size()
                - countStage(items, "Confirmed")
                - countStage(items, "Contracted"));
        report.setQuoteDraftCount(countStage(items, "QuoteDraft"));
        report.setQuoteSubmittedCount(countStage(items, "QuoteSubmitted"));
        report.setConfirmedCount(countStage(items, "Confirmed"));
        report.setContractedCount(countStage(items, "Contracted"));
        report.setTalkGeneratedCount((int) items.stream().filter(item -> value(item.getTalkCount()) > 0).count());
        report.setRiskCount((int) items.stream().filter(item -> !"Normal".equals(item.getRiskLevel())).count());
        report.setQuoteAmount(items.stream()
                .filter(item -> item.getLatestQuoteAmount() != null)
                .filter(item -> !QuoteStatus.Rejected.name().equals(item.getLatestQuoteStatus()))
                .filter(item -> !QuoteStatus.Superseded.name().equals(item.getLatestQuoteStatus()))
                .map(BossDesignItem::getLatestQuoteAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setAcceptedAmount(items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getCustomerConfirmed()))
                .map(BossDesignItem::getLatestQuoteAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private List<BossDesignerLoad> buildDesignerLoads(Long comId, List<BossDesignItem> items) {
        Map<Long, BossDesignerLoad> loads = new LinkedHashMap<>();
        employeeRepository.findByCompany_ComIdAndRoleOrderByName(comId, EmployeeRole.Designer).stream()
                .filter(employee -> employee.getStatus() == EmployeeStatus.Active)
                .forEach(employee -> loads.put(employee.getEmpId(), emptyLoad(employee)));

        for (BossDesignItem item : items) {
            if (item.getDesignerId() == null) {
                continue;
            }
            BossDesignerLoad load = loads.computeIfAbsent(item.getDesignerId(), id -> {
                BossDesignerLoad created = new BossDesignerLoad();
                created.setDesignerId(id);
                created.setDesignerName(item.getDesignerName());
                created.setStoreName(item.getDesignerStoreName());
                return created;
            });
            addLoad(load, item);
        }

        List<BossDesignerLoad> result = new ArrayList<>(loads.values());
        result.sort(Comparator
                .comparingInt((BossDesignerLoad load) -> value(load.getRiskCount())).reversed()
                .thenComparing(BossDesignerLoad::getCustomerCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BossDesignerLoad::getDesignerName, Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private BossDesignerLoad emptyLoad(Employee employee) {
        BossDesignerLoad load = new BossDesignerLoad();
        load.setDesignerId(employee.getEmpId());
        load.setDesignerName(employee.getName());
        load.setStoreName(employee.getStore() == null ? null : employee.getStore().getName());
        load.setCustomerCount(0);
        load.setSchemeCount(0);
        load.setQuoteCount(0);
        load.setTalkCount(0);
        load.setConfirmedCount(0);
        load.setRiskCount(0);
        load.setQuoteAmount(BigDecimal.ZERO);
        load.setAcceptedAmount(BigDecimal.ZERO);
        return load;
    }

    private void addLoad(BossDesignerLoad load, BossDesignItem item) {
        load.setCustomerCount(value(load.getCustomerCount()) + 1);
        load.setSchemeCount(value(load.getSchemeCount()) + value(item.getSchemeCount()));
        load.setQuoteCount(value(load.getQuoteCount()) + value(item.getQuoteCount()));
        load.setTalkCount(value(load.getTalkCount()) + value(item.getTalkCount()));
        load.setConfirmedCount(value(load.getConfirmedCount()) + (Boolean.TRUE.equals(item.getCustomerConfirmed()) ? 1 : 0));
        load.setRiskCount(value(load.getRiskCount()) + (!"Normal".equals(item.getRiskLevel()) ? 1 : 0));
        if (item.getLatestQuoteAmount() != null
                && !QuoteStatus.Rejected.name().equals(item.getLatestQuoteStatus())
                && !QuoteStatus.Superseded.name().equals(item.getLatestQuoteStatus())) {
            load.setQuoteAmount(value(load.getQuoteAmount()).add(item.getLatestQuoteAmount()));
        }
        if (Boolean.TRUE.equals(item.getCustomerConfirmed()) && item.getLatestQuoteAmount() != null) {
            load.setAcceptedAmount(value(load.getAcceptedAmount()).add(item.getLatestQuoteAmount()));
        }
        LocalDateTime latest = latest(
                item.getLatestDesignAt(),
                item.getLatestQuoteAcceptedAt(),
                item.getLatestQuoteSubmittedAt(),
                item.getLatestTalkBuiltAt());
        if (latest != null && (load.getLastUpdateAt() == null || latest.isAfter(load.getLastUpdateAt()))) {
            load.setLastUpdateAt(latest);
        }
    }

    private int unassignedCustomerCount(List<Customer> customers,
                                        List<Property> properties,
                                        List<Measure> measures,
                                        LocalDateTime now) {
        Map<Long, List<Measure>> byProperty = groupMeasuresByProperty(measures);
        Set<Long> measuredCustomerIds = new HashSet<>();
        for (Property property : properties) {
            Customer customer = property.getCustomer();
            if (customer == null || property.getDesigner() != null || customer.getDesigner() != null) {
                continue;
            }
            boolean measured = completedMeasureAt(byProperty.getOrDefault(property.getPropertyId(), List.of()),
                    property.getLatestMeasureAt()) != null;
            if (measured) {
                measuredCustomerIds.add(customer.getCustId());
            }
        }
        return measuredCustomerIds.size();
    }

    private String resolveStage(Employee designer,
                                List<Requirement> requirements,
                                Design latestDesign,
                                Quote latestQuote) {
        if (latestQuote != null) {
            if (latestQuote.getStatus() == QuoteStatus.Converted) {
                return "Contracted";
            }
            if (latestQuote.getStatus() == QuoteStatus.Accepted) {
                return "Confirmed";
            }
            if (latestQuote.getStatus() == QuoteStatus.Submitted) {
                return "QuoteSubmitted";
            }
            return "QuoteDraft";
        }
        if (latestDesign != null) {
            return latestDesign.getStatus() == DesignStatus.Draft ? "Designing" : "SchemeReady";
        }
        if (!requirements.isEmpty()) {
            return "SchemePending";
        }
        if (designer != null) {
            return "RequirementPending";
        }
        return "PendingAssign";
    }

    private String stageName(String stage) {
        return switch (stage) {
            case "PendingAssign" -> "待指派设计师";
            case "RequirementPending" -> "待录设计诉求";
            case "SchemePending" -> "待出设计方案";
            case "Designing" -> "方案设计中";
            case "SchemeReady" -> "方案已完成";
            case "QuoteDraft" -> "报价编制中";
            case "QuoteSubmitted" -> "报价待确认";
            case "Confirmed" -> "客户已确认";
            case "Contracted" -> "已签约转项目";
            default -> "设计推进中";
        };
    }

    private String nextAction(String stage) {
        return switch (stage) {
            case "PendingAssign" -> "指派设计师";
            case "RequirementPending" -> "录入客户诉求";
            case "SchemePending" -> "创建设计方案";
            case "Designing" -> "完善方案区域与分项";
            case "SchemeReady" -> "生成报价与谈单PPT";
            case "QuoteDraft" -> "提交客户报价";
            case "QuoteSubmitted" -> "推动客户确认";
            case "Confirmed" -> "推进合同签约";
            case "Contracted" -> "查看施工项目";
            default -> "查看设计进度";
        };
    }

    private String riskLevel(String stage, long stayDays) {
        if (stage.equals("PendingAssign")) {
            return stayDays >= 2 ? "Danger" : "Normal";
        }
        if (Set.of("RequirementPending", "SchemePending", "Designing", "SchemeReady").contains(stage)) {
            if (stayDays >= 14) return "Danger";
            if (stayDays >= 7) return "Warning";
            return "Normal";
        }
        if (stage.equals("QuoteSubmitted") || stage.equals("Confirmed")) {
            if (stayDays >= 7) return "Danger";
            if (stayDays >= 3) return "Warning";
            return "Normal";
        }
        return "Normal";
    }

    private int riskOrder(String level) {
        if ("Danger".equals(level)) return 0;
        if ("Warning".equals(level)) return 1;
        return 2;
    }

    private LocalDateTime stageStart(String stage,
                                     LocalDateTime measureAt,
                                     LocalDateTime pendingMeasureAt,
                                     LocalDateTime latestDesignAt,
                                     Quote latestQuote,
                                     Property property,
                                     LocalDateTime now) {
        if (Set.of("Confirmed", "Contracted").contains(stage)) {
            return firstNonNull(acceptedAt(latestQuote), propertyCreateTime(property), now);
        }
        if ("QuoteSubmitted".equals(stage)) {
            return firstNonNull(submittedAt(latestQuote), propertyCreateTime(property), now);
        }
        if ("QuoteDraft".equals(stage)) {
            return firstNonNull(quoteCreateTime(latestQuote), latestDesignAt, propertyCreateTime(property), now);
        }
        if (Set.of("Designing", "SchemeReady").contains(stage) && latestDesignAt != null) {
            return latestDesignAt;
        }
        return firstNonNull(measureAt, pendingMeasureAt, latestDesignAt, propertyCreateTime(property), now);
    }

    private Employee resolveDesigner(Property property,
                                     Customer customer,
                                     Design latestDesign,
                                     Requirement latestRequirement,
                                     Talk latestTalk,
                                     Quote latestQuote) {
        if (property.getDesigner() != null) return property.getDesigner();
        if (customer.getDesigner() != null) return customer.getDesigner();
        if (latestDesign != null && latestDesign.getOwner() != null) return latestDesign.getOwner();
        if (latestRequirement != null && latestRequirement.getOwner() != null) return latestRequirement.getOwner();
        if (latestTalk != null && latestTalk.getOwner() != null) return latestTalk.getOwner();
        return latestQuote == null || latestQuote.getDesign() == null ? null : latestQuote.getDesign().getOwner();
    }

    private Design latestDesign(List<Design> designs) {
        return designs.stream()
                .filter(design -> design.getDesignId() != null)
                .max(Comparator.comparing(Design::getDesignId))
                .orElse(null);
    }

    private Requirement latestRequirement(List<Requirement> requirements) {
        return requirements.stream()
                .filter(requirement -> requirement.getRequirementId() != null)
                .max(Comparator.comparing(Requirement::getRequirementId))
                .orElse(null);
    }

    private Talk latestTalk(List<Talk> talks) {
        return talks.stream()
                .filter(talk -> talk.getTalkId() != null)
                .max(Comparator.comparing(Talk::getTalkId))
                .orElse(null);
    }

    private Quote latestQuote(List<Quote> quotes) {
        Quote confirmed = quotes.stream()
                .filter(quote -> CONFIRMED_QUOTE_STATUSES.contains(quote.getStatus()))
                .max(Comparator.comparing(Quote::getQuoteId))
                .orElse(null);
        if (confirmed != null) {
            return confirmed;
        }
        return quotes.stream()
                .filter(quote -> quote.getStatus() != QuoteStatus.Superseded)
                .max(Comparator.comparing(Quote::getQuoteId))
                .orElse(null);
    }

    private LocalDateTime latestDesignAt(Design design) {
        if (design == null) return null;
        if (design.getUpdateTime() != null) return design.getUpdateTime().toLocalDateTime();
        if (design.getCreateTime() != null) return design.getCreateTime().toLocalDateTime();
        return null;
    }

    private LocalDateTime completedMeasureAt(List<Measure> measures, LocalDateTime fallback) {
        return measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Completed)
                .map(Measure::getMeasureTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(fallback);
    }

    private LocalDateTime scheduledMeasureAt(List<Measure> measures, LocalDateTime now) {
        List<LocalDateTime> scheduled = measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Scheduled)
                .map(Measure::getMeasureTime)
                .filter(Objects::nonNull)
                .toList();
        return scheduled.stream()
                .filter(measureTime -> !measureTime.isBefore(now))
                .min(LocalDateTime::compareTo)
                .orElseGet(() -> scheduled.stream().max(LocalDateTime::compareTo).orElse(null));
    }

    private LocalDateTime submittedAt(Quote quote) {
        if (quote == null) return null;
        if (quote.getSubmittedAt() != null) return quote.getSubmittedAt();
        if (quote.getStatus() == QuoteStatus.Draft) return null;
        return quote.getUpdateTime() == null ? quoteCreateTime(quote) : quote.getUpdateTime().toLocalDateTime();
    }

    private LocalDateTime acceptedAt(Quote quote) {
        if (quote == null || !CONFIRMED_QUOTE_STATUSES.contains(quote.getStatus())) return null;
        if (quote.getAcceptedAt() != null) return quote.getAcceptedAt();
        if (quote.getUpdateTime() != null) return quote.getUpdateTime().toLocalDateTime();
        return quoteCreateTime(quote);
    }

    private LocalDateTime quoteCreateTime(Quote quote) {
        return quote == null || quote.getCreateTime() == null ? null : quote.getCreateTime().toLocalDateTime();
    }

    private LocalDateTime propertyCreateTime(Property property) {
        return property == null || property.getCreateTime() == null ? null : property.getCreateTime().toLocalDateTime();
    }

    private Map<Long, List<Design>> groupDesignsByProperty(List<Design> designs) {
        return designs.stream()
                .filter(design -> design.getProperty() != null && design.getProperty().getPropertyId() != null)
                .collect(Collectors.groupingBy(design -> design.getProperty().getPropertyId()));
    }

    private Map<Long, List<Requirement>> groupRequirementsByProperty(List<Requirement> requirements) {
        return requirements.stream()
                .filter(requirement -> requirement.getProperty() != null && requirement.getProperty().getPropertyId() != null)
                .collect(Collectors.groupingBy(requirement -> requirement.getProperty().getPropertyId()));
    }

    private Map<Long, List<Quote>> groupQuotesByProperty(List<Quote> quotes) {
        return quotes.stream()
                .filter(quote -> quote.getProperty() != null && quote.getProperty().getPropertyId() != null)
                .collect(Collectors.groupingBy(quote -> quote.getProperty().getPropertyId()));
    }

    private Map<Long, List<Talk>> groupTalksByProperty(List<Talk> talks) {
        return talks.stream()
                .filter(talk -> talk.getProperty() != null && talk.getProperty().getPropertyId() != null)
                .collect(Collectors.groupingBy(talk -> talk.getProperty().getPropertyId()));
    }

    private Map<Long, List<Measure>> groupMeasuresByProperty(List<Measure> measures) {
        return measures.stream()
                .filter(measure -> measure.getProperty() != null && measure.getProperty().getPropertyId() != null)
                .collect(Collectors.groupingBy(measure -> measure.getProperty().getPropertyId()));
    }

    private Map<Long, Long> countSpacesByDesign(Collection<Long> designIds) {
        if (designIds.isEmpty()) return Map.of();
        Map<Long, Long> counts = new HashMap<>();
        for (Space space : spaceRepository.findByDesign_DesignIdIn(designIds)) {
            if (space.getDesign() != null && space.getDesign().getDesignId() != null) {
                counts.merge(space.getDesign().getDesignId(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private Map<Long, Long> countItemsByDesign(Collection<Long> designIds) {
        if (designIds.isEmpty()) return Map.of();
        Map<Long, Long> counts = new HashMap<>();
        for (DesignItem item : designItemRepository.findByDesign_DesignIdIn(designIds)) {
            if (item.getDesign() != null && item.getDesign().getDesignId() != null) {
                counts.merge(item.getDesign().getDesignId(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private int countStage(List<BossDesignItem> items, String stage) {
        return (int) items.stream().filter(item -> stage.equals(item.getStage())).count();
    }

    private int value(Integer number) {
        return number == null ? 0 : number;
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private LocalDateTime latest(LocalDateTime... values) {
        LocalDateTime result = null;
        for (LocalDateTime value : values) {
            if (value != null && (result == null || value.isAfter(result))) {
                result = value;
            }
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

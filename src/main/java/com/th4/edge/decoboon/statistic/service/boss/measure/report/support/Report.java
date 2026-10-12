package com.th4.edge.decoboon.statistic.service.boss.measure.report.support;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.configure.CustomerSource;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.statistic.model.BossMeasureDesignerGroup;
import com.th4.edge.decoboon.statistic.model.BossMeasureItem;
import com.th4.edge.decoboon.statistic.model.BossMeasureMarketerGroup;
import com.th4.edge.decoboon.statistic.model.BossMeasurePeriod;
import com.th4.edge.decoboon.statistic.model.BossMeasureQuery;
import com.th4.edge.decoboon.statistic.model.BossMeasureReport;
import com.th4.edge.decoboon.statistic.model.BossMeasureSourceGroup;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;

/** 企业负责人量房经营与预约转化聚合。 */
@Note("企业负责人量房经营报告")
@Properties(allow = {
        @AllowProperties(model = BossMeasureReport.class,
                properties = {"asOf", "itemCount", "todayCount", "riskCount", "scheduledCount",
                        "appointmentRate", "completedCount", "completionRate", "overdue4hCount",
                        "overdueRate", "handoffCount", "handoffRate", "unassignedCount", "handoff48hCount",
                        "periods", "items", "marketerGroups", "sourceGroups", "designerGroups"}),
        @AllowProperties(model = BossMeasurePeriod.class,
                properties = {"key", "label", "scheduledCount", "completedCount"}),
        @AllowProperties(model = BossMeasureItem.class,
                properties = {"measureId", "customerId", "propertyId", "customerName", "telNo",
                        "customerStatus", "customerStatusName", "source", "sourceName", "marketerId",
                        "marketerName", "marketerStoreName", "community", "building", "roomNo", "address",
                        "area", "layout", "propertyStatus", "propertyStatusName", "designerId", "designerName",
                        "executorId", "executorName", "scheduledAt", "completedAt", "measureStatus",
                        "derivedState", "derivedStateName", "riskLevel", "nextAction", "sitePhotoCount",
                        "structureImageCount", "attachmentCount", "handoffStarted", "handoffStartedAt", "stayHours"}),
        @AllowProperties(model = BossMeasureMarketerGroup.class,
                properties = {"marketerId", "marketerName", "storeName", "itemCount", "notScheduledCount",
                        "scheduledCount", "completedCount", "overdue4hCount", "unassignedCount",
                        "appointmentRate", "completionRate", "overdueRate", "items"}),
        @AllowProperties(model = BossMeasureSourceGroup.class,
                properties = {"source", "sourceName", "itemCount", "scheduledCount", "completedCount",
                        "handoffCount", "appointmentRate", "completionRate", "handoffRate", "items"}),
        @AllowProperties(model = BossMeasureDesignerGroup.class,
                properties = {"designerId", "designerName", "storeName", "scheduledCount",
                        "completedCount", "overdue4hCount", "items"})
})
@Service("statistic/boss/measure/report")
@Scope(ScopeTypes.PROTOTYPE)
public class Report implements com.th4.edge.decoboon.statistic.service.boss.measure.report.Report {

    private static final long OVERDUE_HOURS = 4L;
    private static final long HANDOFF_HOURS = 48L;
    @User private Employee employee;

    /**
     * 门店维度（DEC-067 / `GAP-048`）。框架只对 provider 实例的 {@code @AutoInject} 字段做请求绑定，
     * 请求模型不承载 HTTP 参数；因此门店维度落在字段上，解析+越权校验后在本 Provider 内使用。
     */
    @AutoInject
    private Long storeId;

    /**
     * 其余筛选参数（`GAP-048`：请求模型不承载 HTTP 参数，必须落在 provider 的 @AutoInject 字段上）。
     * 修复前 `month/marketerId/source/keyword/status/designerId` 经 HTTP 全部不生效
     * （App 考核页的月份与筛选项形同虚设）。
     */
    @AutoInject
    private LocalDate month;
    @AutoInject
    private String status;
    @AutoInject
    private Long marketerId;
    @AutoInject
    private Long designerId;
    @AutoInject
    private CustomerSource source;
    @AutoInject
    private String keyword;


    private final TenantContext tenantContext;
    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;
    private final RequirementRepository requirementRepository;
    private final DesignRepository designRepository;
    private final StoreFilterContext storeFilterContext;

    public Report(TenantContext tenantContext,
                  CustomerRepository customerRepository,
                  PropertyRepository propertyRepository,
                  MeasureRepository measureRepository,
                  RequirementRepository requirementRepository,
                  DesignRepository designRepository,
                  StoreFilterContext storeFilterContext) {
        this.tenantContext = tenantContext;
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.requirementRepository = requirementRepository;
        this.designRepository = designRepository;
        this.storeFilterContext = storeFilterContext;
    }

    @Override
    @Transactional(readOnly = true)
    public BossMeasureReport call(BossMeasureQuery request) {
        Employee current = tenantContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        LocalDateTime now = LocalDateTime.now();
        // 门店维度（DEC-067）：经 @AutoInject 绑定（GAP-048），解析 + 越权校验
        Long storeFilter = storeFilterContext.resolve(employee, storeId);

        List<Customer> customers = scopedCustomers(comId, current).stream()
                // 全企业日期窗口必须保留已签约/服务中/已完工客户的历史量房事实，
                // 不能先按客户当前状态截断，否则 periods 与人员绩效会漏记真实事件。
                // 归属基准（DEC-067 BOSS-STORE-09）：量房经营客户按 `Customer.store`，不按负责人门店
                .filter(customer -> StoreScope.inStore(StoreScope.storeIdOf(customer.getStore()), storeFilter))
                .filter(customer -> marketerId == null
                        || (customer.getOwner() != null && marketerId.equals(customer.getOwner().getEmpId())))
                .filter(customer -> source == null || source == customer.getSource())
                .filter(customer -> blank(keyword) || matches(customer, keyword))
                .toList();
        List<Long> customerIds = customers.stream().map(Customer::getCustId).toList();
        if (customerIds.isEmpty()) {
            return empty(now);
        }

        List<Property> properties = propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds);
        List<Long> propertyIds = properties.stream().map(Property::getPropertyId).toList();
        Map<Long, List<Measure>> measuresByProperty = propertyIds.isEmpty() ? Map.of()
                : measureRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(comId, propertyIds)
                        .stream().collect(Collectors.groupingBy(measure -> measure.getProperty().getPropertyId()));
        Map<Long, List<Requirement>> requirementsByProperty = propertyIds.isEmpty() ? Map.of()
                : requirementRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(comId, propertyIds)
                        .stream().collect(Collectors.groupingBy(requirement -> requirement.getProperty().getPropertyId()));
        Map<Long, List<Design>> designsByProperty = propertyIds.isEmpty() ? Map.of()
                : designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(comId, propertyIds)
                        .stream().collect(Collectors.groupingBy(design -> design.getProperty().getPropertyId()));

        List<BossMeasureItem> items = new ArrayList<>();
        for (Property property : properties) {
            Customer customer = property.getCustomer();
            if (customer == null) continue;
            List<Measure> measures = measuresByProperty.getOrDefault(property.getPropertyId(), List.of());
            boolean handoff = !requirementsByProperty.getOrDefault(property.getPropertyId(), List.of()).isEmpty()
                    || !designsByProperty.getOrDefault(property.getPropertyId(), List.of()).isEmpty();
            BossMeasureItem item = buildItem(property, customer, measures, handoff, now);
            if (matchesStatus(item, status)
                    && (month == null || inMonth(item, month))
                    && (designerId == null || Objects.equals(designerId, item.getDesignerId()))) {
                items.add(item);
            }
        }
        items.sort(itemComparator());

        BossMeasureReport report = new BossMeasureReport();
        report.setAsOf(now);
        report.setItems(items);
        report.setItemCount(items.size());
        report.setTodayCount((int) items.stream().filter(this::isToday).count());
        report.setScheduledCount((int) items.stream().filter(item -> item.getScheduledAt() != null).count());
        report.setCompletedCount((int) items.stream().filter(item -> item.getCompletedAt() != null).count());
        report.setOverdue4hCount((int) items.stream().filter(this::isOverdue4h).count());
        report.setUnassignedCount((int) items.stream().filter(this::isUnassigned).count());
        report.setHandoff48hCount((int) items.stream().filter(this::isHandoff48h).count());
        report.setHandoffCount((int) items.stream().filter(item -> "DesignStarted".equals(item.getDerivedState())).count());
        report.setRiskCount((int) items.stream().filter(item -> !"Normal".equals(item.getRiskLevel())).count());
        report.setPeriods(periods(items, now));
        report.setAppointmentRate(rate(report.getScheduledCount(), report.getItemCount()));
        report.setCompletionRate(rate(report.getCompletedCount(), report.getScheduledCount()));
        report.setOverdueRate(rate(report.getOverdue4hCount(), report.getScheduledCount()));
        report.setHandoffRate(rate(report.getHandoffCount(), report.getCompletedCount()));
        report.setMarketerGroups(marketerGroups(items));
        report.setSourceGroups(sourceGroups(items));
        report.setDesignerGroups(designerGroups(items));
        return report;
    }

    private List<Customer> scopedCustomers(Long comId, Employee current) {
        return switch (current.getRole()) {
            case Boss -> customerRepository.findByCompany_ComId(comId);
            case StoreManager -> current.getStore() == null ? List.of()
                    : customerRepository.findByCompany_ComIdAndStore_StoreIdOrderByCustIdDesc(
                            comId, current.getStore().getStoreId());
            case TeamLeader -> current.getTeam() == null ? List.of()
                    : customerRepository.findByCompany_ComIdAndTeam_TeamIdOrderByCustIdDesc(
                            comId, current.getTeam().getTeamId());
            case Marketer -> customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, current.getEmpId());
            default -> throw new DefinedException(new DefaultErrorCode("403", "当前角色无权访问量房经营工作台"));
        };
    }

    private BossMeasureItem buildItem(Property property, Customer customer, List<Measure> measures,
                                      boolean handoff, LocalDateTime now) {
        Measure scheduled = measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Scheduled)
                .max(Comparator.comparing(this::effectiveScheduledAt, Comparator.nullsFirst(LocalDateTime::compareTo)))
                .orElse(null);
        Measure completed = measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Completed)
                .max(Comparator.comparing(this::effectiveCompletedAt, Comparator.nullsFirst(LocalDateTime::compareTo)))
                .orElse(null);
        Measure latest = measures.isEmpty() ? null : measures.get(0);

        LocalDateTime scheduledAt = scheduled == null ? (completed == null ? null : effectiveScheduledAt(completed))
                : effectiveScheduledAt(scheduled);
        LocalDateTime completedAt = completed == null ? null : effectiveCompletedAt(completed);
        Employee designer = scheduled != null && scheduled.getDesigner() != null ? scheduled.getDesigner()
                : (property.getDesigner() != null ? property.getDesigner() : customer.getDesigner());
        Employee executor = completed == null ? null : completed.getExecutor();

        String state;
        String stateName;
        String risk;
        String next;
        if (completedAt != null) {
            if (handoff) {
                state = "DesignStarted"; stateName = "已交设计"; risk = "Normal"; next = "查看设计进度";
            } else if (completedAt.plusHours(HANDOFF_HOURS).isBefore(now)) {
                state = "HandoffRisk"; stateName = "待交设计"; risk = "Danger"; next = "提醒设计责任人录入诉求或方案";
            } else {
                state = "CompletedPendingHandoff"; stateName = "量房完成·待交设计"; risk = "Normal"; next = "等待或提醒设计责任人";
            }
        } else if (scheduledAt != null) {
            if (designer == null) {
                state = "Unassigned"; stateName = "已预约·待指派"; risk = "Danger"; next = "指派客户级设计责任人";
            } else if (scheduledAt.plusHours(OVERDUE_HOURS).isBefore(now)) {
                state = "Overdue"; stateName = "预约逾期"; risk = "Danger"; next = "立即联系客户并改约";
            } else {
                state = "Scheduled"; stateName = "已预约·待上门"; risk = "Normal"; next = "确认客户上门时间";
            }
        } else if (latest != null && latest.getStatus() == MeasureStatus.Cancelled) {
            state = "Cancelled"; stateName = "已取消"; risk = "Normal"; next = "查看客户后续安排";
        } else {
            state = "NotScheduled"; stateName = "未预约量房"; risk = "Warning"; next = "联系客户并预约量房";
        }

        BossMeasureItem item = new BossMeasureItem();
        item.setMeasureId(latest == null ? null : latest.getMeasureId());
        item.setCustomerId(customer.getCustId());
        item.setPropertyId(property.getPropertyId());
        item.setCustomerName(customer.getName());
        item.setTelNo(customer.getTelNo());
        item.setCustomerStatus(customer.getStatus().name());
        item.setCustomerStatusName(customer.getStatus().getName());
        CustomerSource source = customer.getSource() == null ? CustomerSource.Other : customer.getSource();
        item.setSource(source.name());
        item.setSourceName(source.getName());
        if (customer.getOwner() != null) {
            item.setMarketerId(customer.getOwner().getEmpId());
            item.setMarketerName(customer.getOwner().getName());
            item.setMarketerStoreName(customer.getOwner().getStore() == null ? null : customer.getOwner().getStore().getName());
        }
        item.setCommunity(property.getCommunity());
        item.setBuilding(property.getBuilding());
        item.setRoomNo(property.getRoomNo());
        item.setAddress(property.getAddress());
        item.setArea(property.getArea() == null ? customer.getArea() : property.getArea());
        item.setLayout(property.getLayout());
        item.setPropertyStatus(property.getStatus() == null ? null : property.getStatus().name());
        item.setPropertyStatusName(property.getStatus() == null ? null : property.getStatus().getName());
        if (designer != null) {
            item.setDesignerId(designer.getEmpId());
            item.setDesignerName(designer.getName());
        }
        if (executor != null) {
            item.setExecutorId(executor.getEmpId());
            item.setExecutorName(executor.getName());
        }
        item.setScheduledAt(scheduledAt);
        item.setCompletedAt(completedAt);
        item.setMeasureStatus(latest == null ? null : latest.getStatus().name());
        item.setDerivedState(state);
        item.setDerivedStateName(stateName);
        item.setRiskLevel(risk);
        item.setNextAction(next);
        item.setSitePhotoCount(size(latest == null ? null : latest.getSitePhotos()));
        item.setStructureImageCount(size(latest == null ? null : latest.getStructureImages()));
        item.setAttachmentCount(size(latest == null ? null : latest.getAttachments()));
        item.setHandoffStarted(handoff);
        item.setHandoffStartedAt(null);
        item.setStayHours(stayHours(item, now));
        return item;
    }

    private List<BossMeasureMarketerGroup> marketerGroups(List<BossMeasureItem> items) {
        Map<String, List<BossMeasureItem>> groups = items.stream().collect(Collectors.groupingBy(
                item -> item.getMarketerId() == null ? "unassigned" : String.valueOf(item.getMarketerId()),
                LinkedHashMap::new, Collectors.toList()));
        List<BossMeasureMarketerGroup> result = new ArrayList<>();
        groups.forEach((key, list) -> {
            BossMeasureItem first = list.get(0);
            BossMeasureMarketerGroup group = new BossMeasureMarketerGroup();
            group.setMarketerId(first.getMarketerId());
            group.setMarketerName(first.getMarketerName() == null ? "未分配业务员" : first.getMarketerName());
            group.setStoreName(first.getMarketerStoreName());
            group.setItems(list);
            group.setItemCount(list.size());
            group.setNotScheduledCount(countState(list, "NotScheduled"));
            group.setScheduledCount((int) list.stream().filter(item -> item.getScheduledAt() != null).count());
            group.setCompletedCount((int) list.stream().filter(item -> item.getCompletedAt() != null).count());
            group.setOverdue4hCount((int) list.stream().filter(this::isOverdue4h).count());
            group.setUnassignedCount((int) list.stream().filter(this::isUnassigned).count());
            group.setAppointmentRate(rate(group.getScheduledCount(), group.getItemCount()));
            group.setCompletionRate(rate(group.getCompletedCount(), group.getScheduledCount()));
            group.setOverdueRate(rate(group.getOverdue4hCount(), group.getScheduledCount()));
            result.add(group);
        });
        result.sort(Comparator.comparing(BossMeasureMarketerGroup::getOverdue4hCount, Comparator.reverseOrder())
                .thenComparing(BossMeasureMarketerGroup::getNotScheduledCount, Comparator.reverseOrder())
                .thenComparing(BossMeasureMarketerGroup::getItemCount, Comparator.reverseOrder()));
        return result;
    }

    private List<BossMeasureSourceGroup> sourceGroups(List<BossMeasureItem> items) {
        Map<String, List<BossMeasureItem>> groups = items.stream().collect(Collectors.groupingBy(
                item -> item.getSource() == null ? "Other" : item.getSource(),
                LinkedHashMap::new, Collectors.toList()));
        List<BossMeasureSourceGroup> result = new ArrayList<>();
        groups.forEach((source, list) -> {
            BossMeasureSourceGroup group = new BossMeasureSourceGroup();
            group.setSource(source);
            group.setSourceName(list.get(0).getSourceName());
            group.setItems(list);
            group.setItemCount(list.size());
            group.setScheduledCount((int) list.stream().filter(item -> item.getScheduledAt() != null).count());
            group.setCompletedCount((int) list.stream().filter(item -> item.getCompletedAt() != null).count());
            group.setHandoffCount((int) list.stream().filter(item -> Boolean.TRUE.equals(item.getHandoffStarted())).count());
            group.setAppointmentRate(rate(group.getScheduledCount(), group.getItemCount()));
            group.setCompletionRate(rate(group.getCompletedCount(), group.getScheduledCount()));
            group.setHandoffRate(rate(group.getHandoffCount(), group.getCompletedCount()));
            result.add(group);
        });
        result.sort(Comparator.comparing(BossMeasureSourceGroup::getScheduledCount, Comparator.reverseOrder())
                .thenComparing(BossMeasureSourceGroup::getItemCount, Comparator.reverseOrder()));
        return result;
    }

    private List<BossMeasureDesignerGroup> designerGroups(List<BossMeasureItem> items) {
        Map<String, List<BossMeasureItem>> groups = items.stream()
                .filter(item -> item.getDesignerId() != null)
                .collect(Collectors.groupingBy(item -> String.valueOf(item.getDesignerId()), LinkedHashMap::new, Collectors.toList()));
        List<BossMeasureDesignerGroup> result = new ArrayList<>();
        groups.forEach((key, list) -> {
            BossMeasureItem first = list.get(0);
            BossMeasureDesignerGroup group = new BossMeasureDesignerGroup();
            group.setDesignerId(first.getDesignerId());
            group.setDesignerName(first.getDesignerName());
            group.setStoreName(null);
            group.setItems(list);
            group.setScheduledCount((int) list.stream().filter(item -> item.getScheduledAt() != null).count());
            group.setCompletedCount((int) list.stream().filter(item -> item.getCompletedAt() != null).count());
            group.setOverdue4hCount((int) list.stream().filter(this::isOverdue4h).count());
            result.add(group);
        });
        result.sort(Comparator.comparing(BossMeasureDesignerGroup::getOverdue4hCount, Comparator.reverseOrder())
                .thenComparing(BossMeasureDesignerGroup::getScheduledCount, Comparator.reverseOrder()));
        return result;
    }

    private Comparator<BossMeasureItem> itemComparator() {
        Map<String, Integer> rank = Map.of("Overdue", 0, "Unassigned", 1, "HandoffRisk", 2,
                "Scheduled", 3, "NotScheduled", 4, "CompletedPendingHandoff", 5,
                "DesignStarted", 6, "Cancelled", 7);
        return Comparator.comparing((BossMeasureItem item) -> rank.getOrDefault(item.getDerivedState(), 99))
                .thenComparing(BossMeasureItem::getScheduledAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(BossMeasureItem::getCustomerId, Comparator.nullsLast(Long::compareTo));
    }

    private boolean matchesStatus(BossMeasureItem item, String status) {
        if (status == null || status.isBlank() || "all".equals(status)) return true;
        return switch (status) {
            case "notScheduled" -> "NotScheduled".equals(item.getDerivedState());
            case "scheduled" -> item.getScheduledAt() != null;
            case "today" -> isToday(item);
            case "overdue4h" -> isOverdue4h(item);
            case "completed" -> item.getCompletedAt() != null;
            case "handoff48h" -> "HandoffRisk".equals(item.getDerivedState());
            case "unassigned" -> isUnassigned(item);
            default -> true;
        };
    }

    private boolean matches(Customer customer, String keyword) {
        String key = keyword.toLowerCase(Locale.ROOT);
        return contains(customer.getName(), key) || contains(customer.getTelNo(), key)
                || contains(customer.getCommunity(), key) || contains(customer.getAddr(), key);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
    private int countState(List<BossMeasureItem> items, String state) {
        return (int) items.stream().filter(item -> state.equals(item.getDerivedState())).count();
    }
    private boolean isOverdue4h(BossMeasureItem item) {
        return item.getCompletedAt() == null && item.getScheduledAt() != null
                && item.getScheduledAt().plusHours(OVERDUE_HOURS).isBefore(LocalDateTime.now());
    }
    private boolean isUnassigned(BossMeasureItem item) {
        return item.getCompletedAt() == null && item.getScheduledAt() != null && item.getDesignerId() == null;
    }
    private boolean isHandoff48h(BossMeasureItem item) {
        return item.getCompletedAt() != null && !Boolean.TRUE.equals(item.getHandoffStarted())
                && item.getCompletedAt().plusHours(HANDOFF_HOURS).isBefore(LocalDateTime.now());
    }
    private int size(List<?> values) { return values == null ? 0 : values.size(); }
    private boolean isToday(BossMeasureItem item) {
        // “今日预约”按预约自然日统计，今天已完成也必须计入；
        // completedAt 是否为空只影响完成/逾期指标，不影响预约口径。
        return item.getScheduledAt() != null
                && LocalDate.now().equals(item.getScheduledAt().toLocalDate());
    }

    private boolean inMonth(BossMeasureItem item, LocalDate month) {
        LocalDate start = month.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1);
        return inDayRange(item.getScheduledAt(), start, end)
                || inDayRange(item.getCompletedAt(), start, end);
    }

    private boolean inDayRange(LocalDateTime value, LocalDate start, LocalDate end) {
        if (value == null) return false;
        LocalDate date = value.toLocalDate();
        return !date.isBefore(start) && date.isBefore(end);
    }
    private LocalDateTime effectiveScheduledAt(Measure measure) {
        return measure.getScheduledAt() != null ? measure.getScheduledAt() : measure.getMeasureTime();
    }
    private LocalDateTime effectiveCompletedAt(Measure measure) {
        return measure.getCompletedAt() != null ? measure.getCompletedAt() : measure.getMeasureTime();
    }
    private Long stayHours(BossMeasureItem item, LocalDateTime now) {
        LocalDateTime start = item.getCompletedAt() != null ? item.getCompletedAt() : item.getScheduledAt();
        return start == null ? null : Math.max(0, Duration.between(start, now).toHours());
    }
    private BigDecimal rate(Integer numerator, Integer denominator) {
        if (numerator == null || denominator == null || denominator == 0) return null;
        return BigDecimal.valueOf(numerator * 100.0 / denominator).setScale(1, RoundingMode.HALF_UP);
    }
    private BossMeasureReport empty(LocalDateTime now) {
        BossMeasureReport report = new BossMeasureReport();
        report.setAsOf(now);
        report.setItemCount(0);
        report.setTodayCount(0);
        report.setRiskCount(0);
        report.setScheduledCount(0);
        report.setCompletedCount(0);
        report.setOverdue4hCount(0);
        report.setUnassignedCount(0);
        report.setHandoff48hCount(0);
        report.setHandoffCount(0);
        report.setPeriods(periods(List.of(), now));
        return report;
    }

    private List<BossMeasurePeriod> periods(List<BossMeasureItem> items, LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        return List.of(
                period("today", "日增", items, today.atStartOfDay(), today.plusDays(1).atStartOfDay()),
                period("yesterday", "昨日", items, today.minusDays(1).atStartOfDay(), today.atStartOfDay()),
                period("last7Days", "近7日增", items, today.minusDays(6).atStartOfDay(), now),
                period("month", "月增", items, today.withDayOfMonth(1).atStartOfDay(), now)
        );
    }

    private BossMeasurePeriod period(String key, String label, List<BossMeasureItem> items,
                                     LocalDateTime start, LocalDateTime end) {
        BossMeasurePeriod period = new BossMeasurePeriod();
        period.setKey(key);
        period.setLabel(label);
        period.setScheduledCount((int) items.stream()
                .filter(item -> inRange(item.getScheduledAt(), start, end)).count());
        period.setCompletedCount((int) items.stream()
                .filter(item -> inRange(item.getCompletedAt(), start, end)).count());
        return period;
    }

    private boolean inRange(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }
}

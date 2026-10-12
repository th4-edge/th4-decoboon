package com.th4.edge.decoboon.statistic.context;

import com.th4.edge.decoboon.customer.configure.CustStatus;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Requirement;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureQuery;
import com.th4.edge.decoboon.statistic.model.DesignerMeasureTask;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 设计师待量房任务投影：按客户 + 房屋返回可执行任务，不在前端推断状态。 */
@Component
public class DesignerMeasureTaskContext {

    private static final long OVERDUE_HOURS = 4L;
    private static final long HANDOFF_HOURS = 48L;
    private static final Set<CustStatus> OPEN_STATUSES = Set.of(
            CustStatus.New, CustStatus.Following, CustStatus.PendingMeasure, CustStatus.Quoted);

    private final CustomerRepository customerRepository;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;
    private final RequirementRepository requirementRepository;
    private final DesignRepository designRepository;

    public DesignerMeasureTaskContext(CustomerRepository customerRepository,
                                      PropertyRepository propertyRepository,
                                      MeasureRepository measureRepository,
                                      RequirementRepository requirementRepository,
                                      DesignRepository designRepository) {
        this.customerRepository = customerRepository;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.requirementRepository = requirementRepository;
        this.designRepository = designRepository;
    }

    @Transactional(readOnly = true)
    public List<DesignerMeasureTask> list(Employee current, DesignerMeasureQuery request) {
        Long comId = current.getCompany().getComId();
        Long empId = current.getEmpId();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, Customer> customerMap = new LinkedHashMap<>();
        if (current.getRole() == EmployeeRole.Boss) {
            // DEC-032 / BR-028 / ANS-BIZ-20261011-002：BOSS 为内部岗位能力超集，数据范围为 Company，
            // 因此量房队列按企业全量客户呈现（不等于把他人待办归到本人名下，仍按责任人与状态展示）。
            customerRepository.findByCompany_ComId(comId)
                    .forEach(customer -> customerMap.put(customer.getCustId(), customer));
        } else {
            customerRepository.findByCompany_ComIdAndOwner_EmpId(comId, empId)
                    .forEach(customer -> customerMap.put(customer.getCustId(), customer));
            customerRepository.findByCompany_ComIdAndDesigner_EmpId(comId, empId)
                    .forEach(customer -> customerMap.put(customer.getCustId(), customer));
        }

        List<Customer> customers = customerMap.values().stream()
                .filter(customer -> customer.getStatus() != null && OPEN_STATUSES.contains(customer.getStatus()))
                .toList();
        if (customers.isEmpty()) return List.of();

        List<Long> customerIds = customers.stream().map(Customer::getCustId).toList();
        List<Property> properties = propertyRepository.findByCompany_ComIdAndCustomer_CustIdIn(comId, customerIds);
        Map<Long, List<Property>> propertiesByCustomer = properties.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        property -> property.getCustomer().getCustId(),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()));

        List<Long> propertyIds = properties.stream().map(Property::getPropertyId).toList();
        Map<Long, List<Measure>> measuresByProperty = propertyIds.isEmpty() ? Map.of()
                : measureRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByMeasureTimeDesc(comId, propertyIds)
                        .stream().collect(java.util.stream.Collectors.groupingBy(
                                measure -> measure.getProperty().getPropertyId(),
                                LinkedHashMap::new,
                                java.util.stream.Collectors.toList()));
        Map<Long, List<Requirement>> requirementsByProperty = propertyIds.isEmpty() ? Map.of()
                : requirementRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(comId, propertyIds)
                        .stream().collect(java.util.stream.Collectors.groupingBy(
                                requirement -> requirement.getProperty().getPropertyId(),
                                LinkedHashMap::new,
                                java.util.stream.Collectors.toList()));
        Map<Long, List<Design>> designsByProperty = propertyIds.isEmpty() ? Map.of()
                : designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(comId, propertyIds)
                        .stream().collect(java.util.stream.Collectors.groupingBy(
                                design -> design.getProperty().getPropertyId(),
                                LinkedHashMap::new,
                                java.util.stream.Collectors.toList()));

        List<DesignerMeasureTask> tasks = new ArrayList<>();
        for (Customer customer : customers) {
            boolean measureIntent = customer.getStatus() == CustStatus.PendingMeasure
                    || customer.getMeasureAt() != null
                    || customer.getMeasureRequestedAt() != null
                    || customer.getPendingMeasureAt() != null;
            List<Property> customerProperties = propertiesByCustomer.getOrDefault(customer.getCustId(), List.of());
            if (customerProperties.isEmpty()) {
                if (measureIntent) {
                    tasks.add(missingPropertyTask(customer));
                }
                continue;
            }
            for (Property property : customerProperties) {
                List<Measure> measures = measuresByProperty.getOrDefault(property.getPropertyId(), List.of());
                int requirementCount = requirementsByProperty.getOrDefault(property.getPropertyId(), List.of()).size();
                int designCount = designsByProperty.getOrDefault(property.getPropertyId(), List.of()).size();
                DesignerMeasureTask task = buildTask(customer, property, measures, requirementCount, designCount, now);
                if (task != null) tasks.add(task);
            }
        }

        String keyword = request == null || request.getKeyword() == null
                ? "" : request.getKeyword().trim().toLowerCase(Locale.ROOT);
        String state = request == null ? "" : String.valueOf(request.getState() == null ? "" : request.getState());
        return tasks.stream()
                .filter(task -> state.isBlank() || state.equals(task.getStateKey()))
                .filter(task -> matchesKeyword(task, keyword))
                .sorted(taskComparator())
                .limit(200)
                .toList();
    }

    private DesignerMeasureTask missingPropertyTask(Customer customer) {
        DesignerMeasureTask task = base(customer, null);
        task.setStateKey("missing_property");
        task.setStateLabel("待建房屋");
        task.setRiskLevel("warning");
        task.setRiskReason("客户还没有房屋档案，量房必须绑定明确房屋。");
        task.setMainActionKey("create_property");
        task.setMainActionLabel("新增房屋");
        task.setNextActionKey("create_property");
        task.setNextActionLabel("建立房屋档案");
        task.setCompletionHint("房屋建档后重新进入待量房队列，再安排量房。");
        task.setAllowed(Boolean.TRUE);
        task.setBlockReason("先建立房屋档案，不能以客户地址代替房屋。");
        task.setScheduleStateKey("none");
        task.setScheduleStateName("未预约");
        task.setPriority(20);
        return task;
    }

    private DesignerMeasureTask buildTask(Customer customer,
                                          Property property,
                                          List<Measure> measures,
                                          int requirementCount,
                                          int designCount,
                                          LocalDateTime now) {
        Measure scheduled = measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Scheduled)
                .max(Comparator.comparing(this::effectiveScheduledAt, Comparator.nullsFirst(LocalDateTime::compareTo)))
                .orElse(null);
        Measure completed = measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Completed)
                .max(Comparator.comparing(this::effectiveCompletedAt, Comparator.nullsFirst(LocalDateTime::compareTo)))
                .orElse(null);
        Measure cancelled = measures.stream()
                .filter(measure -> measure.getStatus() == MeasureStatus.Cancelled)
                .max(Comparator.comparing(this::effectiveScheduledAt, Comparator.nullsFirst(LocalDateTime::compareTo)))
                .orElse(null);

        boolean handoffStarted = designCount > 0;
        boolean measureIntent = customer.getStatus() == CustStatus.PendingMeasure
                || customer.getMeasureAt() != null
                || customer.getMeasureRequestedAt() != null
                || customer.getPendingMeasureAt() != null;
        boolean propertyInMeasure = property.getStatus() == PropertyStatus.PendingMeasure
                || property.getStatus() == PropertyStatus.Measuring;
        if (scheduled == null && completed == null && cancelled == null && !measureIntent && !propertyInMeasure) {
            return null;
        }
        if (completed != null && handoffStarted) {
            return null;
        }

        DesignerMeasureTask task = base(customer, property);
        task.setRequirementCount(requirementCount);
        task.setDesignCount(designCount);
        task.setMeasureId(completed != null ? completed.getMeasureId()
                : scheduled != null ? scheduled.getMeasureId() : cancelled == null ? null : cancelled.getMeasureId());
        task.setMeasureStatus(completed != null ? "Completed" : scheduled != null ? "Scheduled"
                : cancelled == null ? null : "Cancelled");
        task.setCompletedAt(completed == null ? null : effectiveCompletedAt(completed));
        applyScheduleState(task, completed, scheduled, cancelled);

        if (completed != null) {
            LocalDateTime completedAt = effectiveCompletedAt(completed);
            boolean overdueHandoff = completedAt != null && completedAt.plusHours(HANDOFF_HOURS).isBefore(now);
            task.setStateKey("completed_pending_handoff");
            task.setStateLabel(overdueHandoff ? "量房完成·待交设计" : "量房完成·待交设计");
            task.setRiskLevel(overdueHandoff ? "high" : "normal");
            task.setRiskReason(overdueHandoff
                    ? "量房完成超过 48 小时，尚未形成设计方案。"
                    : "量房事实已完成，下一步要形成设计输入。");
            task.setMainActionKey("create_scheme");
            task.setMainActionLabel("新建方案");
            task.setNextActionKey("design/create");
            task.setNextActionLabel("进入方案设计");
            task.setCompletionHint("方案建立后，本次量房任务完成并进入设计推进。");
            task.setPriority(overdueHandoff ? 5 : 10);
            task.setSitePhotoCount(sizeOf(completed.getSitePhotos()));
            task.setStructureImageCount(sizeOf(completed.getStructureImages()));
            task.setAttachmentCount(sizeOf(completed.getAttachments()));
            return task;
        }

        if (scheduled != null) {
            LocalDateTime scheduledAt = effectiveScheduledAt(scheduled);
            boolean overdue = scheduledAt != null && scheduledAt.plusHours(OVERDUE_HOURS).isBefore(now);
            task.setStateKey(overdue ? "overdue" : "scheduled");
            task.setStateLabel(overdue ? "预约逾期" : "已预约·待上门");
            task.setRiskLevel(overdue ? "high" : "normal");
            task.setRiskReason(overdue
                    ? "预约时间已超过 4 小时，量房仍未完成；现场已量请直接登记量房结论，未上门请先联系客户改约。"
                    : "已约定量房时间，按预约完成现场事实。");
            // 逾期只是风险提示，不是「不能录入」：已预约未完成时主行动始终是登记量房结论，
            // 改约降级为并列动作（中栏「调整预约时间」），避免设计师已上门却无处录入（2026-10-12 修复）。
            task.setMainActionKey("complete_measure");
            task.setMainActionLabel("登记量房结论");
            task.setNextActionKey(overdue ? "measure/reschedule" : "measure/complete");
            task.setNextActionLabel(overdue ? "联系客户改约" : "录入量房事实");
            task.setCompletionHint(overdue
                    ? "登记量房结论后进入待交设计；未上门时先联系客户改约，再用「调整预约时间」重新约定。"
                    : "量房完成后进入待交设计，形成诉求或方案。");
            task.setPriority(overdue ? 1 : 8);
            return task;
        }

        if (cancelled != null) {
            task.setStateKey("cancelled");
            task.setStateLabel("已取消");
            task.setRiskLevel("warning");
            task.setRiskReason("量房已取消，客户仍需要继续推进量房。");
            task.setMainActionKey("reschedule_measure");
            task.setMainActionLabel("重新安排量房");
            task.setNextActionKey("measure/reschedule");
            task.setNextActionLabel("重新安排量房");
            task.setCompletionHint("重新预约后进入已预约待上门。");
            task.setPriority(12);
            return task;
        }

        task.setStateKey("not_scheduled");
        task.setStateLabel("未预约量房");
        task.setRiskLevel("warning");
        task.setRiskReason("客户已进入量房阶段，但尚未形成预约时间。");
        task.setMainActionKey("schedule_measure");
        task.setMainActionLabel("安排量房");
        task.setNextActionKey("measure/schedule");
        task.setNextActionLabel("约定量房时间");
        task.setCompletionHint("安排后进入已预约待上门。");
        task.setPriority(15);
        return task;
    }

    private void applyScheduleState(DesignerMeasureTask task,
                                    Measure completed,
                                    Measure scheduled,
                                    Measure cancelled) {
        if (completed != null) {
            LocalDateTime completedAt = effectiveCompletedAt(completed);
            LocalDateTime completedScheduledAt = effectiveScheduledAt(completed);
            LocalDateTime plannedScheduledAt = scheduled == null ? null : effectiveScheduledAt(scheduled);
            LocalDateTime rawScheduledAt = completedScheduledAt != null ? completedScheduledAt : plannedScheduledAt;
            if (rawScheduledAt != null && completedAt != null && rawScheduledAt.isAfter(completedAt)) {
                task.setScheduledAt(null);
                task.setScheduleStateKey("invalid_history");
                task.setScheduleStateName("预约记录异常，暂不可确认");
                return;
            }
            if (rawScheduledAt == null) {
                task.setScheduledAt(null);
                task.setScheduleStateKey("completed_without_schedule");
                task.setScheduleStateName("未预约（直接登记量房）");
                return;
            }
            task.setScheduledAt(rawScheduledAt);
            task.setScheduleStateKey("scheduled");
            task.setScheduleStateName("已预约");
            return;
        }
        if (scheduled != null) {
            task.setScheduledAt(effectiveScheduledAt(scheduled));
            task.setScheduleStateKey("scheduled");
            task.setScheduleStateName("已预约");
            return;
        }
        if (cancelled != null) {
            task.setScheduledAt(effectiveScheduledAt(cancelled));
            task.setScheduleStateKey("cancelled");
            task.setScheduleStateName("已取消");
            return;
        }
        task.setScheduledAt(null);
        task.setScheduleStateKey("none");
        task.setScheduleStateName("未预约");
    }

    private DesignerMeasureTask base(Customer customer, Property property) {
        DesignerMeasureTask task = new DesignerMeasureTask();
        task.setTaskKey((property == null ? "customer:" + customer.getCustId() : "property:" + property.getPropertyId())
                + ":measure");
        task.setCustomerId(customer.getCustId());
        task.setCustomerName(customer.getName());
        task.setTelNo(customer.getTelNo());
        task.setCustomerStatus(customer.getStatus() == null ? null : customer.getStatus().name());
        task.setCustomerStatusName(customer.getStatus() == null ? null : customer.getStatus().getName());
        task.setPropertyId(property == null ? null : property.getPropertyId());
        task.setPropertyLabel(propertyLabel(property));
        if (property != null) {
            task.setCommunity(property.getCommunity());
            task.setBuilding(property.getBuilding());
            task.setRoomNo(property.getRoomNo());
            task.setAddress(property.getAddress());
            task.setArea(property.getArea());
            task.setLayout(property.getLayout());
            task.setPropertyStatus(property.getStatus() == null ? null : property.getStatus().name());
            task.setPropertyStatusName(property.getStatus() == null ? null : property.getStatus().getName());
        }
        task.setAllowed(Boolean.TRUE);
        return task;
    }

    private boolean matchesKeyword(DesignerMeasureTask task, String keyword) {
        if (keyword.isBlank()) return true;
        return contains(task.getCustomerName(), keyword)
                || contains(task.getTelNo(), keyword)
                || contains(task.getCommunity(), keyword)
                || contains(task.getBuilding(), keyword)
                || contains(task.getRoomNo(), keyword)
                || contains(task.getAddress(), keyword)
                || contains(task.getPropertyLabel(), keyword);
    }

    private Comparator<DesignerMeasureTask> taskComparator() {
        return Comparator
                .comparingInt((DesignerMeasureTask task) -> task.getPriority() == null ? 99 : task.getPriority())
                .thenComparing(DesignerMeasureTask::getScheduledAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(DesignerMeasureTask::getCustomerId, Comparator.nullsLast(Long::compareTo));
    }

    private LocalDateTime effectiveScheduledAt(Measure measure) {
        return measure.getScheduledAt() != null ? measure.getScheduledAt() : measure.getMeasureTime();
    }

    private LocalDateTime effectiveCompletedAt(Measure measure) {
        return measure.getCompletedAt() != null ? measure.getCompletedAt() : measure.getMeasureTime();
    }

    private int sizeOf(List<String> values) {
        return values == null ? 0 : values.size();
    }

    private String propertyLabel(Property property) {
        if (property == null) return "房屋待建立";
        List<String> parts = new ArrayList<>();
        if (property.getCommunity() != null && !property.getCommunity().isBlank()) parts.add(property.getCommunity());
        String unit = (property.getBuilding() == null ? "" : property.getBuilding())
                + (property.getRoomNo() == null ? "" : property.getRoomNo());
        if (!unit.isBlank()) parts.add(unit);
        if (parts.isEmpty() && property.getAddress() != null && !property.getAddress().isBlank()) parts.add(property.getAddress());
        return parts.isEmpty() ? "房屋待完善" : String.join(" ", parts);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }
}

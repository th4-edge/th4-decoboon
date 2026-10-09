package com.th4.edge.decoboon.property.service.measures.save.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.configure.PropertyStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** 保存并确认房屋量房记录。 */
@Note("量房记录保存")
@Description("确认客户-房屋绑定，保存尺寸、诉求、现场照片和设计师结论")
@Properties(allow = {
        @AllowProperties(model = Measure.class,
                properties = {"measureId", "customer", "property", "designer", "executor", "scheduledAt", "completedAt", "measureTime", "status", "sitePhotos",
                        "originalStructure", "structureImages", "attachments", "dimensions", "customerRequirements", "designerConclusion", "createTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/measure/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.property.service.measures.save.Save {

    @AutoInject @NotNull private Long propertyId;
    @AutoInject private LocalDateTime scheduledAt;
    @AutoInject private LocalDateTime completedAt;
    @AutoInject private LocalDateTime measureTime;
    @AutoInject private MeasureStatus status;
    @AutoInject private List<String> sitePhotos;
    @AutoInject private List<String> structureImages;
    @AutoInject private List<String> attachments;
    @AutoInject private String originalStructure;
    @AutoInject private String dimensions;
    @AutoInject private String customerRequirements;
    @AutoInject private String designerConclusion;
    @User private Employee employee;
    private final PropertyContext propertyContext;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;

    public Save(PropertyContext propertyContext, PropertyRepository propertyRepository,
                MeasureRepository measureRepository) {
        this.propertyContext = propertyContext;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
    }

    @Override
    @Transactional
    public Measure call(Measure request) {
        // JWT 主体是框架用 BeanUtils 新建的 Employee 占位对象（只有 empId，
        // role 为字段默认值 Marketer、name 为空），因此执行人必须使用重新加载的完整员工实体，
        // 否则响应中的 executor.role/name 会与数据库不一致。
        Employee current = propertyContext.requireEmployee(employee);
        Property property = propertyContext.require(propertyId, current);
        MeasureStatus measureStatus = status == null ? MeasureStatus.Completed : status;
        var existing = measureRepository.findByPropertyOrderByMeasureTimeDesc(property);
        Measure scheduled = existing.stream()
                .filter(item -> item.getStatus() == MeasureStatus.Scheduled)
                .findFirst()
                .orElse(null);
        Measure completed = existing.stream()
                .filter(item -> item.getStatus() == MeasureStatus.Completed)
                .findFirst()
                .orElse(null);

        // 设计师是设计责任事实：只取已指派的设计师，绝不把当前操作人写成设计师。
        Employee assignedDesigner = resolveDesigner(property, scheduled == null ? request : scheduled, current);
        if (measureStatus == MeasureStatus.Completed) {
            if (assignedDesigner == null) {
                if (property.getCustomer() != null) {
                    throw new DefinedException(new DefaultErrorCode("400",
                            "请先由企业负责人、店长或团队负责人指定设计师，再完成量房"));
                }
            } else if (current == null || !assignedDesigner.getEmpId().equals(current.getEmpId())) {
                // SCN-001：已指派设计师后，量房完成只能由该指定设计师执行，
                // 其他角色（业务员/其他设计师/管理者）不得代录，失败且不落库。
                throw new DefinedException(new DefaultErrorCode("403",
                        "该房屋已指定设计师，量房完成必须由指定设计师执行"));
            }
        }
        // 幂等：房屋已有 Completed 量房事实且没有待完成的预约时，直接返回原记录，
        // 不重写 measureTime / property.latestMeasureAt / executor / designer / status / 附件。
        if (measureStatus == MeasureStatus.Completed && scheduled == null && completed != null) {
            return completed;
        }
        Measure target = scheduled == null ? request : scheduled;
        target.setCompany(property.getCompany());
        target.setStore(property.getStore());
        target.setTeam(property.getTeam());
        target.setOwner(property.getOwner());
        target.setCustomer(property.getCustomer());
        target.setProperty(property);
        target.setDesigner(assignedDesigner);
        if (assignedDesigner != null && property.getDesigner() == null) {
            property.setDesigner(assignedDesigner);
        }
        if (assignedDesigner != null && property.getCustomer() != null
                && property.getCustomer().getDesigner() == null) {
            property.getCustomer().setDesigner(assignedDesigner);
        }
        // 量房执行人单独记录在 executor，与设计师语义分离，并保持完整员工实体。
        target.setExecutor(current);
        LocalDateTime legacyTime = measureTime;
        if (measureStatus == MeasureStatus.Scheduled) {
            LocalDateTime scheduledValue = scheduledAt != null ? scheduledAt : legacyTime;
            target.setScheduledAt(scheduledValue == null ? LocalDateTime.now() : scheduledValue);
            target.setCompletedAt(null);
            target.setMeasureTime(target.getScheduledAt());
        } else if (measureStatus == MeasureStatus.Completed) {
            LocalDateTime completedValue = completedAt != null ? completedAt : legacyTime;
            if (completedValue == null) {
                completedValue = LocalDateTime.now();
            }
            // 历史坑：量房时间兼容字段不能回填为预约时间；直接登记完成必须保留“无预约”事实。
            LocalDateTime scheduledValue = target.getScheduledAt() != null
                    ? target.getScheduledAt() : scheduledAt;
            if (scheduledValue != null && scheduledValue.isAfter(completedValue)) {
                throw new DefinedException(new DefaultErrorCode("400", "量房完成时间不能早于预约时间"));
            }
            target.setScheduledAt(scheduledValue);
            target.setCompletedAt(completedValue);
            target.setMeasureTime(target.getCompletedAt());
        } else {
            LocalDateTime scheduledValue = target.getScheduledAt() != null
                    ? target.getScheduledAt() : (scheduledAt != null ? scheduledAt : legacyTime);
            if (scheduledValue != null) {
                target.setScheduledAt(scheduledValue);
                target.setMeasureTime(scheduledValue);
            }
        }
        target.setStatus(measureStatus);
        target.setSitePhotos(sitePhotos);
        target.setStructureImages(structureImages);
        target.setAttachments(attachments);
        target.setOriginalStructure(originalStructure);
        target.setDimensions(dimensions);
        target.setCustomerRequirements(customerRequirements);
        target.setDesignerConclusion(designerConclusion);
        Measure saved = measureRepository.save(target);
        if (measureStatus == MeasureStatus.Completed) {
            if (property.getStatus() == PropertyStatus.PendingMeasure
                    || property.getStatus() == PropertyStatus.Measuring) {
                property.setStatus(PropertyStatus.Measured);
            }
            property.setLatestMeasureAt(saved.getCompletedAt() == null ? saved.getMeasureTime() : saved.getCompletedAt());
            // 量房完成只更新房屋量房事实，不因为量房动作自动补齐设计师或完成设计节点。
            propertyRepository.save(property);
        } else if (measureStatus == MeasureStatus.Scheduled
                && property.getStatus() == PropertyStatus.PendingMeasure) {
            property.setStatus(PropertyStatus.PendingMeasure);
            propertyRepository.save(property);
        }
        return saved;
    }

    /** 解析当前房屋已指派的设计师；设计师或 BOSS 本人录入时默认由本人承接设计。 */
    private Employee resolveDesigner(Property property, Measure target, Employee current) {
        if (property.getDesigner() != null) {
            return property.getDesigner();
        }
        if (target.getDesigner() != null) {
            return target.getDesigner();
        }
        if (property.getCustomer() != null && property.getCustomer().getDesigner() != null) {
            return property.getCustomer().getDesigner();
        }
        if (current == null || (current.getRole() != EmployeeRole.Designer
                && current.getRole() != EmployeeRole.Boss)) {
            return null;
        }
        if (sameEmployee(property.getOwner(), current)
                || sameEmployee(property.getSalesman(), current)
                || (property.getCustomer() != null && sameEmployee(property.getCustomer().getOwner(), current))) {
            return current;
        }
        return null;
    }

    private boolean sameEmployee(Employee left, Employee right) {
        return left != null && right != null && left.getEmpId() != null
                && left.getEmpId().equals(right.getEmpId());
    }
}

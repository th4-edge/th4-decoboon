package com.th4.edge.decoboon.property.service.measures.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;

/** 房屋量房记录分页。 */
@Note("量房记录分页")
@Properties(allow = {
        @AllowProperties(model = Measure.class,
                properties = {"measureId", "customer", "property", "designer", "executor", "measureTime", "status", "sitePhotos",
                        "originalStructure", "structureImages", "attachments", "dimensions", "customerRequirements", "designerConclusion", "createTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/measure/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.property.service.measures.page.Page {

    @AutoInject @NotNull private Long propertyId;
    @User private Employee employee;
    private final PropertyContext propertyContext;
    private final PropertyRepository propertyRepository;
    private final ProjectAccessContext projectAccessContext;
    private final MeasureRepository measureRepository;

    public Page(PropertyContext propertyContext,
                PropertyRepository propertyRepository,
                ProjectAccessContext projectAccessContext,
                MeasureRepository measureRepository) {
        this.propertyContext = propertyContext;
        this.propertyRepository = propertyRepository;
        this.projectAccessContext = projectAccessContext;
        this.measureRepository = measureRepository;
    }

    @Override
    public org.springframework.data.domain.Page<Measure> call(Measure request) {
        Employee current = projectAccessContext.requireEmployee(employee);
        Property property = propertyRepository.findById(propertyId)
                .filter(item -> item.getCompany() != null && current.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(
                        new DefaultErrorCode("404", "量房记录不存在或无权访问")));
        if (!propertyContext.canView(property, current)) {
            // 项目参与人（项目经理、设计师、履约、报价创建人）按显式参与关系只读量房事实，
            // 使 PM 不因 property/measure/page 的房屋数据范围而无法开工；越权仍按 404 失败关闭。
            projectAccessContext.requireParticipantProperty(propertyId, current);
        }
        return new PageImpl<>(measureRepository.findByPropertyOrderByMeasureTimeDesc(property));
    }
}

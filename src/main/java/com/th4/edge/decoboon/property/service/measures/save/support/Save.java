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
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
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
                properties = {"measureId", "customer", "property", "designer", "measureTime", "status", "sitePhotos",
                        "originalStructure", "dimensions", "customerRequirements", "designerConclusion", "createTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/measure/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.property.service.measures.save.Save {

    @AutoInject @NotNull private Long propertyId;
    @AutoInject private LocalDateTime measureTime;
    @AutoInject private MeasureStatus status;
    @AutoInject private List<String> sitePhotos;
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
        Property property = propertyContext.require(propertyId, employee);
        request.setCompany(property.getCompany());
        request.setStore(property.getStore());
        request.setTeam(property.getTeam());
        request.setOwner(property.getOwner());
        request.setCustomer(property.getCustomer());
        request.setProperty(property);
        request.setDesigner(property.getDesigner() == null ? employee : property.getDesigner());
        request.setMeasureTime(measureTime == null ? LocalDateTime.now() : measureTime);
        MeasureStatus measureStatus = status == null ? MeasureStatus.Completed : status;
        request.setStatus(measureStatus);
        request.setSitePhotos(sitePhotos);
        request.setOriginalStructure(originalStructure);
        request.setDimensions(dimensions);
        request.setCustomerRequirements(customerRequirements);
        request.setDesignerConclusion(designerConclusion);
        Measure saved = measureRepository.save(request);
        if (measureStatus == MeasureStatus.Completed) {
            if (property.getStatus() == PropertyStatus.PendingMeasure
                    || property.getStatus() == PropertyStatus.Measuring) {
                property.setStatus(PropertyStatus.Measured);
            }
            property.setLatestMeasureAt(saved.getMeasureTime());
            property.setDesigner(saved.getDesigner());
            propertyRepository.save(property);
        } else if (measureStatus == MeasureStatus.Scheduled
                && property.getStatus() == PropertyStatus.PendingMeasure) {
            property.setStatus(PropertyStatus.PendingMeasure);
            propertyRepository.save(property);
        }
        return saved;
    }
}

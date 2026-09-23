package com.th4.edge.decoboon.property.service.measures.info.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 量房记录详情。 */
@Note("量房记录详情")
@Properties(allow = {
        @AllowProperties(model = Measure.class,
                properties = {"measureId", "customer", "property", "designer", "executor", "scheduledAt", "completedAt", "measureTime", "status", "sitePhotos",
                        "originalStructure", "structureImages", "attachments", "dimensions", "customerRequirements", "designerConclusion", "createTime"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "telNo"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/measure/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.property.service.measures.info.Info {

    @AutoInject @NotNull private Long measureId;
    @User private Employee employee;
    private final MeasureRepository measureRepository;
    private final DataScopeContext dataScopeContext;

    public Info(MeasureRepository measureRepository, DataScopeContext dataScopeContext) {
        this.measureRepository = measureRepository;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public Measure call(Measure request) {
        DataScope scope = dataScopeContext.requireScope(employee);
        org.springframework.data.jpa.domain.Specification<Measure> byId =
                (root, query, builder) -> builder.equal(root.get("measureId"), measureId);
        return measureRepository.findOne(byId.and(DataScopeSpecification.visible(Measure.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "量房记录不存在或无权访问")));
    }
}

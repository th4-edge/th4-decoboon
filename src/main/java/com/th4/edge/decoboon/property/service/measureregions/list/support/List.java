package com.th4.edge.decoboon.property.service.measureregions.list.support;

import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRegionRepository;
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
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/** 量房原始区域列表实现。 */
@Note("量房原始区域列表")
@Properties(allow = {
        @AllowProperties(model = MeasureRegion.class,
                properties = {"regionId", "roomKey", "roomType", "name", "length", "width", "height", "area",
                        "perimeter", "areaSource", "measuredBy", "measuredAt", "sort", "remark", "createTime",
                        "updateTime"}),
        @AllowProperties(model = Measure.class,
                properties = {"measureId", "status", "measureTime", "completedAt", "scheduledAt"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area",
                        "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/measure/region/list")
@Scope(ScopeTypes.PROTOTYPE)
public class List implements com.th4.edge.decoboon.property.service.measureregions.list.List {

    @AutoInject
    @NotNull
    private Long measureId;

    @User
    private Employee employee;

    private final PropertyContext propertyContext;
    private final MeasureRepository measureRepository;
    private final MeasureRegionRepository regionRepository;

    public List(PropertyContext propertyContext, MeasureRepository measureRepository,
                MeasureRegionRepository regionRepository) {
        this.propertyContext = propertyContext;
        this.measureRepository = measureRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    public java.util.List<MeasureRegion> call(MeasureRegion request) {
        Employee current = propertyContext.requireEmployee(employee);
        Measure measure = requireMeasure(current);
        return regionRepository.findByCompany_ComIdAndMeasure_MeasureIdOrderBySortAscRegionIdAsc(
                measure.getCompany().getComId(), measure.getMeasureId());
    }

    private Measure requireMeasure(Employee current) {
        Measure measure = measureRepository.findById(measureId)
                .filter(item -> item.getCompany() != null && current.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "量房记录不存在或无权访问")));
        if (measure.getProperty() != null && !propertyContext.canView(measure.getProperty(), current)) {
            throw new DefinedException(new DefaultErrorCode("404", "量房记录不存在或无权访问"));
        }
        return measure;
    }
}

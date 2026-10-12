package com.th4.edge.decoboon.property.service.measureregions.delete.support;

import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Measure;
import com.th4.edge.decoboon.property.model.MeasureRegion;
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
import org.springframework.transaction.annotation.Transactional;

/** 量房原始区域删除（量房完成后冻结，DEC-066）。 */
@Note("量房原始区域删除")
@Properties(allow = {
        @AllowProperties(model = MeasureRegion.class,
                properties = {"regionId", "roomKey", "roomType", "name", "area", "perimeter", "areaSource", "sort"}),
        @AllowProperties(model = Measure.class, properties = {"measureId", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/measure/region/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.property.service.measureregions.delete.Delete {

    @AutoInject
    @NotNull
    private Long regionId;

    @User
    private Employee employee;

    private final PropertyContext propertyContext;
    private final MeasureRepository measureRepository;
    private final MeasureRegionRepository regionRepository;

    public Delete(PropertyContext propertyContext, MeasureRepository measureRepository,
                  MeasureRegionRepository regionRepository) {
        this.propertyContext = propertyContext;
        this.measureRepository = measureRepository;
        this.regionRepository = regionRepository;
    }

    @Override
    @Transactional
    public MeasureRegion call(MeasureRegion request) {
        Employee current = propertyContext.requireEmployee(employee);
        MeasureRegion region = regionRepository.findById(regionId)
                .filter(item -> item.getCompany() != null && current.getCompany() != null
                        && item.getCompany().getComId().equals(current.getCompany().getComId()))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "量房原始区域不存在或无权访问")));
        Measure measure = region.getMeasure();
        if (measure == null || measure.getProperty() == null || !propertyContext.canView(measure.getProperty(), current)) {
            throw new DefinedException(new DefaultErrorCode("404", "量房原始区域不存在或无权访问"));
        }
        if (measure.getStatus() == MeasureStatus.Completed) {
            throw new DefinedException(new DefaultErrorCode("400",
                    "该量房已完成，原始结构不可再修改；如需修正请重新量房（复尺）"));
        }
        regionRepository.delete(region);
        return region;
    }
}

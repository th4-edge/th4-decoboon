package com.th4.edge.decoboon.property.service.regions.candidates.support;

import com.th4.edge.decoboon.design.configure.AreaSource;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
import com.th4.edge.decoboon.property.context.PropertyContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.model.PropertyRegion;
import com.th4.edge.decoboon.property.repository.PropertyRegionRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 历史方案区域实测值 → 房屋区域实测档案**候选清单**（只读）。
 *
 * <p>{@code DEC-064}：已有方案区域实测值可汇聚为房屋档案候选值，但**只出候选、不自动写入**，
 * 由设计师在「房屋区域实测」里人工确认后保存；历史估算值（{@code Estimated}）不参与候选，
 * 避免把不可用作基数的估算值升级成实测事实（{@code DEC-062} 第 8 条）。</p>
 */
@Note("房屋区域实测档案候选清单")
@Properties(allow = {
        @AllowProperties(model = PropertyRegion.class,
                properties = {"roomKey", "roomType", "name", "length", "width", "height", "area", "perimeter",
                        "areaSource", "remark"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "propertyNo", "address"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/region/candidates")
@Scope(ScopeTypes.PROTOTYPE)
public class Candidates implements com.th4.edge.decoboon.property.service.regions.candidates.Candidates {

    @AutoInject
    @NotNull
    private Long propertyId;

    @User
    private Employee employee;

    private final PropertyContext propertyContext;
    private final PropertyRegionRepository regionRepository;
    private final SpaceRepository spaceRepository;

    public Candidates(PropertyContext propertyContext, PropertyRegionRepository regionRepository,
                      SpaceRepository spaceRepository) {
        this.propertyContext = propertyContext;
        this.regionRepository = regionRepository;
        this.spaceRepository = spaceRepository;
    }

    @Override
    public List<PropertyRegion> call(PropertyRegion request) {
        Employee current = propertyContext.requireEmployee(employee);
        Property property = propertyContext.require(propertyId, current);
        Long comId = property.getCompany().getComId();

        // 已建档的区域不再作为候选（避免重复建档）
        Map<String, PropertyRegion> existing = new LinkedHashMap<>();
        for (PropertyRegion region : regionRepository
                .findByCompany_ComIdAndProperty_PropertyIdOrderBySortAscRegionIdAsc(comId, property.getPropertyId())) {
            existing.put(key(region.getRoomType() == null ? null : region.getRoomType().name(), region.getName()), region);
        }

        // 候选：该房屋各方案里已录入**实测**面积/周长的区域，按「区域类型 + 标准化名称」去重取最新（designId 降序先行）
        Map<String, PropertyRegion> candidates = new LinkedHashMap<>();
        List<Space> spaces = new ArrayList<>(spaceRepository.findByProperty_PropertyId(property.getPropertyId()));
        spaces.sort((left, right) -> {
            Long leftId = left.getDesign() == null ? 0L : left.getDesign().getDesignId();
            Long rightId = right.getDesign() == null ? 0L : right.getDesign().getDesignId();
            return Long.compare(rightId == null ? 0L : rightId, leftId == null ? 0L : leftId);
        });
        for (Space space : spaces) {
            if (space.getAreaSource() != AreaSource.Measured) {
                continue;
            }
            if (space.getArea() == null && space.getPerimeter() == null) {
                continue;
            }
            String key = key(space.getType() == null ? null : space.getType().name(), space.getName());
            if (existing.containsKey(key) || candidates.containsKey(key)) {
                continue;
            }
            PropertyRegion candidate = new PropertyRegion();
            candidate.setRoomType(space.getType());
            candidate.setName(space.getName());
            candidate.setLength(space.getLength());
            candidate.setWidth(space.getWidth());
            candidate.setHeight(space.getHeight());
            candidate.setArea(space.getArea());
            candidate.setPerimeter(space.getPerimeter());
            candidate.setAreaSource(space.getAreaSource());
            Design design = space.getDesign();
            candidate.setRemark("来自方案 " + (design == null ? "—" : design.getNo())
                    + "（" + (design == null || design.getStatus() == null ? "—" : design.getStatus().getName()) + "）");
            candidates.put(key, candidate);
        }
        return new ArrayList<>(candidates.values());
    }

    private String key(String roomType, String name) {
        return (roomType == null ? "" : roomType) + "|" + (name == null ? "" : name.replaceAll("\\s+", ""));
    }
}

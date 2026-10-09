package com.th4.edge.decoboon.designtemplate.service.templates.copy.support;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateVersionRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 复制设计模版。
 *
 * <p>复制结果始终是当前员工的**个人级草稿**，避免非管理者复制企业/门店模版造成共享范围漂移；
 * 来源记录在 {@code sourceTemplate}。可复制任意可见范围的模版（含已停用模版）。</p>
 */
@Note("设计模版复制")
@Description("复制可见模版为当前员工的个人级草稿")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                properties = {"templateId", "scope", "status", "currentVersion", "sourceTemplate", "remark",
                        "rooms", "items", "owner", "store"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax",
                        "tags", "publishedAt", "publishedBy"}),
        @AllowProperties(model = DesignTemplateRoom.class,
                properties = {"roomId", "roomKey", "name", "roomType", "sortNo", "length", "width", "height",
                        "doorWidthTotal", "doorAreaTotal", "windowAreaTotal", "wallRise", "count", "remark"}),
        @AllowProperties(model = DesignTemplateItem.class,
                properties = {"itemId", "roomKey", "quantityFormula", "defaultQuantity", "sortNo", "remark",
                        "catalogItem"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "pricingMode", "category", "brand", "spec",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = Unit.class, properties = {"name", "value"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/template/copy")
@Scope(ScopeTypes.PROTOTYPE)
public class Copy implements com.th4.edge.decoboon.designtemplate.service.templates.copy.Copy {

    @AutoInject
    private Long templateId;

    @AutoInject
    private Long versionId;

    @User
    private Employee employee;

    private final DesignTemplateRepository templateRepository;
    private final DesignTemplateVersionRepository versionRepository;
    private final DesignTemplateRoomRepository roomRepository;
    private final DesignTemplateItemRepository itemRepository;
    private final DesignTemplateAccessContext accessContext;

    public Copy(DesignTemplateRepository templateRepository,
                DesignTemplateVersionRepository versionRepository,
                DesignTemplateRoomRepository roomRepository,
                DesignTemplateItemRepository itemRepository,
                DesignTemplateAccessContext accessContext) {
        this.templateRepository = templateRepository;
        this.versionRepository = versionRepository;
        this.roomRepository = roomRepository;
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public DesignTemplate call(DesignTemplate request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        if (templateId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "templateId不能为空"));
        }
        DesignTemplate source = templateRepository.findByTemplateIdAndCompany_ComId(templateId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版不存在")));
        if (!accessContext.visible(me, source)) {
            throw new DefinedException(new DefaultErrorCode("403", "无权查看该模版，不能复制"));
        }
        DesignTemplateVersion sourceVersion = resolveSourceVersion(source, comId);

        DesignTemplate copy = new DesignTemplate();
        copy.setCompany(me.getCompany());
        copy.setStore(null);
        copy.setOwner(me);
        copy.setScope(DesignTemplate.Scope.Personal);
        copy.setStatus(DesignTemplate.Status.Draft);
        copy.setSourceTemplate(source);
        copy.setRemark(source.getRemark());
        templateRepository.save(copy);

        DesignTemplateVersion targetVersion = new DesignTemplateVersion();
        targetVersion.setCompany(me.getCompany());
        targetVersion.setTemplate(copy);
        targetVersion.setVersion(1);
        targetVersion.setStatus(DesignTemplate.Status.Draft);
        targetVersion.setName(sourceVersion.getName());
        targetVersion.setHouseType(sourceVersion.getHouseType());
        targetVersion.setAreaMin(sourceVersion.getAreaMin());
        targetVersion.setAreaMax(sourceVersion.getAreaMax());
        targetVersion.setTags(sourceVersion.getTags());
        versionRepository.save(targetVersion);

        copyRooms(me, sourceVersion, targetVersion, comId);
        copyItems(me, sourceVersion, targetVersion, comId);

        copy.setCurrentVersion(targetVersion);
        templateRepository.save(copy);
        copy.setRooms(roomRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(targetVersion.getVersionId(), comId));
        copy.setItems(itemRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(targetVersion.getVersionId(), comId));
        return copy;
    }

    private DesignTemplateVersion resolveSourceVersion(DesignTemplate source, Long comId) {
        if (versionId != null) {
            DesignTemplateVersion version = versionRepository.findByVersionIdAndCompany_ComId(versionId, comId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版版本不存在")));
            if (!version.getTemplate().getTemplateId().equals(source.getTemplateId())) {
                throw new DefinedException(new DefaultErrorCode("400", "模版版本不属于当前模版"));
            }
            return version;
        }
        DesignTemplateVersion current = source.getCurrentVersion();
        if (current == null) {
            throw new DefinedException(new DefaultErrorCode("400", "模版没有可复制的版本"));
        }
        return current;
    }

    private void copyRooms(Employee me, DesignTemplateVersion source, DesignTemplateVersion target, Long comId) {
        for (DesignTemplateRoom room : roomRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(source.getVersionId(), comId)) {
            DesignTemplateRoom copy = new DesignTemplateRoom();
            copy.setCompany(me.getCompany());
            copy.setVersion(target);
            copy.setRoomKey(room.getRoomKey());
            copy.setName(room.getName());
            copy.setRoomType(room.getRoomType());
            copy.setSortNo(room.getSortNo());
            copy.setLength(room.getLength());
            copy.setWidth(room.getWidth());
            copy.setHeight(room.getHeight());
            copy.setDoorWidthTotal(room.getDoorWidthTotal());
            copy.setDoorAreaTotal(room.getDoorAreaTotal());
            copy.setWindowAreaTotal(room.getWindowAreaTotal());
            copy.setWallRise(room.getWallRise());
            copy.setCount(room.getCount());
            copy.setRemark(room.getRemark());
            roomRepository.save(copy);
        }
    }

    private void copyItems(Employee me, DesignTemplateVersion source, DesignTemplateVersion target, Long comId) {
        for (DesignTemplateItem item : itemRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(source.getVersionId(), comId)) {
            DesignTemplateItem copy = new DesignTemplateItem();
            copy.setCompany(me.getCompany());
            copy.setVersion(target);
            copy.setRoomKey(item.getRoomKey());
            copy.setCatalogItem(item.getCatalogItem());
            copy.setQuantityFormula(item.getQuantityFormula());
            copy.setDefaultQuantity(item.getDefaultQuantity());
            copy.setSortNo(item.getSortNo());
            copy.setRemark(item.getRemark());
            itemRepository.save(copy);
        }
    }
}

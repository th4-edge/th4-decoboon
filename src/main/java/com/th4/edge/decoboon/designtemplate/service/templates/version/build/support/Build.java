package com.th4.edge.decoboon.designtemplate.service.templates.version.build.support;

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

import java.util.List;

/**
 * 已发布模版新建草稿版本。
 *
 * <p>BR-040 §5：模版发布后不可变；修改已发布模版必须新建版本。
 * 本 Provider 从指定版本（默认当前版本）复制内容快照到新的草稿版本，并把
 * 模版主档的 currentVersion 指向新草稿；旧的已发布版本保持不动。
 *
 * <p>幂等：同一模版已存在草稿版本时直接返回该草稿，不重复创建版本。
 * 模版主档 status 保持「已发布」（企业内仍有已发布版本可被使用），
 * 当前版本 status = Draft 由编辑器用于判断可写。</p>
 */
@Note("设计模版新建版本")
@Description("从已发布版本复制内容生成新的草稿版本；已有草稿时幂等返回")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                properties = {"templateId", "scope", "status", "currentVersion", "sourceTemplate", "remark",
                        "rooms", "items", "owner", "store"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax",
                        "tags", "publishedAt", "publishedBy"}),
        @AllowProperties(model = DesignTemplateRoom.class,
                properties = {"roomId", "roomKey", "name", "roomType", "sortNo", "length", "width", "height",
                        "doorWidthTotal", "doorAreaTotal", "windowAreaTotal", "wallRise",
                        "referenceArea", "referencePerimeter", "count", "remark"}),
        @AllowProperties(model = DesignTemplateItem.class,
                properties = {"itemId", "roomKey", "quantityFormula", "defaultQuantity", "sortNo", "remark",
                        "catalogItem"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "pricingMode", "category", "brand", "spec",
                        "formula", "craft", "criterion"}),
        @AllowProperties(model = Unit.class, properties = {"name", "value"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = com.th4.edge.tenant.user.model.Store.class, properties = {"storeId", "name", "status"})
})
@Service("design/template/version/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.designtemplate.service.templates.version.build.Build {

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

    public Build(DesignTemplateRepository templateRepository,
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
        DesignTemplate template = templateRepository.findByTemplateIdAndCompany_ComId(templateId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版不存在")));
        accessContext.requireMaintain(me, template);
        if (template.getStatus() == DesignTemplate.Status.Disabled) {
            throw new DefinedException(new DefaultErrorCode("400", "模版已停用，不能新建版本"));
        }

        // 幂等：已有草稿版本直接返回，避免重复点击“编辑”生成多个草稿。
        DesignTemplateVersion existingDraft = versionRepository
                .findByTemplate_TemplateIdAndCompany_ComIdAndStatus(templateId, comId, DesignTemplate.Status.Draft)
                .orElse(null);
        if (existingDraft != null) {
            template.setCurrentVersion(existingDraft);
            fillContent(template, existingDraft, comId);
            return template;
        }

        DesignTemplateVersion source = resolveSource(template, comId);
        if (source.getStatus() != DesignTemplate.Status.Published) {
            throw new DefinedException(new DefaultErrorCode("400", "只有已发布版本可以新建版本"));
        }

        DesignTemplateVersion draft = new DesignTemplateVersion();
        draft.setCompany(me.getCompany());
        draft.setTemplate(template);
        draft.setVersion(nextVersion(templateId, comId));
        draft.setStatus(DesignTemplate.Status.Draft);
        draft.setName(source.getName());
        draft.setHouseType(source.getHouseType());
        draft.setAreaMin(source.getAreaMin());
        draft.setAreaMax(source.getAreaMax());
        draft.setTags(source.getTags());
        versionRepository.save(draft);

        copyRooms(me, source, draft, comId);
        copyItems(me, source, draft, comId);

        template.setCurrentVersion(draft);
        // 模版主档仍保持“已发布”：企业内仍有已发布版本可被使用；当前版本为草稿。
        templateRepository.save(template);
        fillContent(template, draft, comId);
        return template;
    }

    private DesignTemplateVersion resolveSource(DesignTemplate template, Long comId) {
        if (versionId != null) {
            DesignTemplateVersion source = versionRepository.findByVersionIdAndCompany_ComId(versionId, comId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版版本不存在")));
            if (!source.getTemplate().getTemplateId().equals(template.getTemplateId())) {
                throw new DefinedException(new DefaultErrorCode("400", "模版版本不属于当前模版"));
            }
            return source;
        }
        DesignTemplateVersion current = template.getCurrentVersion();
        if (current == null) {
            throw new DefinedException(new DefaultErrorCode("400", "模版没有可复制的版本"));
        }
        return current;
    }

    private int nextVersion(Long templateId, Long comId) {
        List<DesignTemplateVersion> versions = versionRepository
                .findByTemplate_TemplateIdAndCompany_ComIdOrderByVersionDesc(templateId, comId);
        if (versions.isEmpty() || versions.get(0).getVersion() == null) {
            return 1;
        }
        return versions.get(0).getVersion() + 1;
    }

    private void copyRooms(Employee me, DesignTemplateVersion source, DesignTemplateVersion draft, Long comId) {
        for (DesignTemplateRoom room : roomRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(source.getVersionId(), comId)) {
            DesignTemplateRoom copy = new DesignTemplateRoom();
            copy.setCompany(me.getCompany());
            copy.setVersion(draft);
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
            copy.setReferenceArea(room.getReferenceArea());
            copy.setReferencePerimeter(room.getReferencePerimeter());
            copy.setCount(room.getCount());
            copy.setRemark(room.getRemark());
            roomRepository.save(copy);
        }
    }

    private void copyItems(Employee me, DesignTemplateVersion source, DesignTemplateVersion draft, Long comId) {
        for (DesignTemplateItem item : itemRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(source.getVersionId(), comId)) {
            DesignTemplateItem copy = new DesignTemplateItem();
            copy.setCompany(me.getCompany());
            copy.setVersion(draft);
            copy.setRoomKey(item.getRoomKey());
            copy.setCatalogItem(item.getCatalogItem());
            copy.setQuantityFormula(item.getQuantityFormula());
            copy.setDefaultQuantity(item.getDefaultQuantity());
            copy.setSortNo(item.getSortNo());
            copy.setRemark(item.getRemark());
            itemRepository.save(copy);
        }
    }

    private void fillContent(DesignTemplate template, DesignTemplateVersion version, Long comId) {
        template.setRooms(roomRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(version.getVersionId(), comId));
        template.setItems(itemRepository
                .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(version.getVersionId(), comId));
    }
}

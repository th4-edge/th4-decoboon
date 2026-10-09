package com.th4.edge.decoboon.designtemplate.service.templates.info.support;

import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateRoomRepository;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateVersionRepository;
import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 设计模版详情：返回模版主档 + 指定版本（默认当前版本）的头部快照、区域与分项。 */
@Note("设计模版详情")
@Properties(allow = {
        @AllowProperties(model = DesignTemplate.class,
                properties = {"templateId", "scope", "status", "currentVersion", "sourceTemplate", "remark",
                        "rooms", "items", "createTime", "owner", "store"}),
        @AllowProperties(model = DesignTemplateVersion.class,
                properties = {"versionId", "version", "status", "name", "houseType", "areaMin", "areaMax",
                        "tags", "publishedAt", "publishedBy"}),
        @AllowProperties(model = DesignTemplateRoom.class,
                properties = {"roomId", "roomKey", "name", "roomType", "sortNo", "length", "width", "height",
                        "doorWidthTotal", "doorAreaTotal", "windowAreaTotal", "wallRise", "count", "remark"}),
        @AllowProperties(model = DesignTemplateItem.class,
                properties = {"itemId", "roomKey", "quantityFormula", "defaultQuantity", "sortNo", "remark",
                        "catalogItem"}),
        // 名称/单位/计价方式/价格来自项目库；必须显式白名单且不含 costPrice（BR-003 / BR-037）
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "pricingMode", "category", "brand", "spec",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = Unit.class, properties = {"name", "value"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("design/template/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.designtemplate.service.templates.info.Info {

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

    public Info(DesignTemplateRepository templateRepository,
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
    @Transactional(readOnly = true)
    public DesignTemplate call(DesignTemplate request) {
        Employee me = accessContext.requireEmployee(employee);
        DesignTemplate template = requireTemplate(me, templateId);
        DesignTemplateVersion version = versionId != null
                ? requireVersion(me, template, versionId)
                : template.getCurrentVersion();
        if (version != null) {
            List<DesignTemplateRoom> rooms = roomRepository
                    .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(version.getVersionId(), me.getCompany().getComId());
            List<DesignTemplateItem> items = itemRepository
                    .findByVersion_VersionIdAndCompany_ComIdOrderBySortNoAsc(version.getVersionId(), me.getCompany().getComId());
            template.setRooms(rooms);
            template.setItems(items);
            template.setCurrentVersion(version);
        }
        return template;
    }

    private DesignTemplate requireTemplate(Employee me, Long id) {
        if (id == null) {
            throw new DefinedException(new DefaultErrorCode("400", "templateId不能为空"));
        }
        DesignTemplate template = templateRepository.findByTemplateIdAndCompany_ComId(id, me.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版不存在")));
        if (!accessContext.visible(me, template)) {
            throw new DefinedException(new DefaultErrorCode("403", "无权查看该模版"));
        }
        return template;
    }

    private DesignTemplateVersion requireVersion(Employee me, DesignTemplate template, Long id) {
        DesignTemplateVersion version = versionRepository.findByVersionIdAndCompany_ComId(id, me.getCompany().getComId())
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版版本不存在")));
        if (!version.getTemplate().getTemplateId().equals(template.getTemplateId())) {
            throw new DefinedException(new DefaultErrorCode("400", "版本不属于该模版"));
        }
        return version;
    }
}

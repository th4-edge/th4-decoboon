package com.th4.edge.decoboon.designtemplate.service.templates.item.save.support;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateFormulaContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
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

import java.math.BigDecimal;

/**
 * 设计模版分项保存（按 itemId 新增或更新）。
 *
 * <p>只写草稿版本；分项只接收「引用哪个项目库条目 + 归属区域 + 工程量公式 + 固定数量」，
 * <b>不接收名称、单位、计价方式与任何价格字段</b>——它们一律来自 catalogItem（`EV-160`）。</p>
 */
@Note("设计模版分项保存")
@Description("在草稿版本上新增/更新分项，不接收任何价格字段")
@Properties(allow = {
        @AllowProperties(model = DesignTemplateItem.class,
                properties = {"itemId", "roomKey", "quantityFormula", "defaultQuantity", "sortNo", "remark",
                        "catalogItem"}),
        // 模板分项不返回价格或成本，只保留渲染和公式所需元数据（BR-040 / DEC-084）
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "pricingMode", "category", "brand", "spec",
                        "formula", "craft", "criterion"}),
        @AllowProperties(model = Unit.class, properties = {"name", "value"})
})
@Service("design/template/item/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.designtemplate.service.templates.item.save.Save {

    @AutoInject
    private Long versionId;

    @AutoInject
    private Long itemId;

    @AutoInject
    private String roomKey;

    @AutoInject
    private Long catalogItemId;

    @AutoInject
    private String quantityFormula;

    @AutoInject
    private BigDecimal defaultQuantity;

    @AutoInject
    private Integer sortNo;

    @AutoInject
    private String remark;

    @User
    private Employee employee;

    private final DesignTemplateVersionRepository versionRepository;
    private final DesignTemplateItemRepository itemRepository;
    private final DesignTemplateRoomRepository roomRepository;
    private final CatalogItemRepository catalogItemRepository;
    private final DesignTemplateFormulaContext formulaContext;
    private final DesignTemplateAccessContext accessContext;

    public Save(DesignTemplateVersionRepository versionRepository,
                DesignTemplateItemRepository itemRepository,
                DesignTemplateRoomRepository roomRepository,
                CatalogItemRepository catalogItemRepository,
                DesignTemplateFormulaContext formulaContext,
                DesignTemplateAccessContext accessContext) {
        this.versionRepository = versionRepository;
        this.itemRepository = itemRepository;
        this.roomRepository = roomRepository;
        this.catalogItemRepository = catalogItemRepository;
        this.formulaContext = formulaContext;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public DesignTemplateItem call(DesignTemplateItem request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        if (versionId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "versionId不能为空"));
        }
        DesignTemplateVersion version = versionRepository.findByVersionIdAndCompany_ComId(versionId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版版本不存在")));
        DesignTemplate template = version.getTemplate();
        accessContext.requireMaintain(me, template);
        if (version.getStatus() != DesignTemplate.Status.Draft) {
            throw new DefinedException(new DefaultErrorCode("400", "已发布版本不可修改，请新建版本"));
        }

        if (catalogItemId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "必须选择报价项目库条目"));
        }
        CatalogItem catalogItem = catalogItemRepository.findByItemIdAndCompany_ComId(catalogItemId, comId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "报价项目库条目不存在")));
        if (!Boolean.TRUE.equals(catalogItem.getActive())) {
            throw new DefinedException(new DefaultErrorCode("400", "报价项目库条目已停用，请更换后保存"));
        }

        boolean hasFormula = quantityFormula != null && !quantityFormula.isBlank();
        if (hasFormula) {
            formulaContext.validate(quantityFormula);
            formulaContext.requireDimension(catalogItem.getUnit(), quantityFormula);
        } else if (defaultQuantity == null || defaultQuantity.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "分项必须填写工程量公式或大于 0 的固定数量"));
        }

        if (roomKey != null && !roomKey.isBlank()) {
            DesignTemplateRoom room = roomRepository
                    .findByVersion_VersionIdAndCompany_ComIdAndRoomKey(versionId, comId, roomKey)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("400", "归属区域不存在：" + roomKey)));
        }

        DesignTemplateItem item;
        if (itemId == null) {
            item = new DesignTemplateItem();
            item.setCompany(me.getCompany());
            item.setVersion(version);
        } else {
            item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "分项不存在")));
            if (!item.getVersion().getVersionId().equals(versionId)
                    || !item.getCompany().getComId().equals(comId)) {
                throw new DefinedException(new DefaultErrorCode("400", "分项不属于该模版版本"));
            }
        }
        item.setRoomKey(roomKey == null || roomKey.isBlank() ? null : roomKey);
        item.setCatalogItem(catalogItem);
        item.setQuantityFormula(hasFormula ? quantityFormula.trim() : null);
        item.setDefaultQuantity(hasFormula ? null : defaultQuantity);
        item.setSortNo(sortNo == null ? 0 : sortNo);
        item.setRemark(remark);
        return itemRepository.save(item);
    }
}

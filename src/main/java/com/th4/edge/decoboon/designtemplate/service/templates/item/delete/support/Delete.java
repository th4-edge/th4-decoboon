package com.th4.edge.decoboon.designtemplate.service.templates.item.delete.support;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.designtemplate.context.DesignTemplateAccessContext;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplate;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateItem;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateVersion;
import com.th4.edge.decoboon.designtemplate.repository.DesignTemplateItemRepository;
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
 * 设计模版分项删除。
 *
 * <p>只允许删除草稿版本上的分项；已发布版本不可修改，必须先新建版本（BR-040 §5）。</p>
 */
@Note("设计模版分项删除")
@Description("删除草稿版本上的模版分项")
@Properties(allow = {
        @AllowProperties(model = DesignTemplateItem.class,
                properties = {"itemId", "roomKey", "quantityFormula", "defaultQuantity", "sortNo", "remark",
                        "catalogItem"}),
        @AllowProperties(model = CatalogItem.class,
                properties = {"itemId", "type", "name", "unit", "pricingMode", "category", "brand", "spec",
                        "materialPrice", "manualPrice", "fixedTotalPrice", "formula", "craft", "criterion"}),
        @AllowProperties(model = Unit.class, properties = {"name", "value"})
})
@Service("design/template/item/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.designtemplate.service.templates.item.delete.Delete {

    @AutoInject
    private Long itemId;

    @User
    private Employee employee;

    private final DesignTemplateItemRepository itemRepository;
    private final DesignTemplateAccessContext accessContext;

    public Delete(DesignTemplateItemRepository itemRepository,
                  DesignTemplateAccessContext accessContext) {
        this.itemRepository = itemRepository;
        this.accessContext = accessContext;
    }

    @Override
    @Transactional
    public DesignTemplateItem call(DesignTemplateItem request) {
        Employee me = accessContext.requireEmployee(employee);
        Long comId = me.getCompany().getComId();
        if (itemId == null) {
            throw new DefinedException(new DefaultErrorCode("400", "itemId不能为空"));
        }
        DesignTemplateItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "模版分项不存在")));
        if (!item.getCompany().getComId().equals(comId)) {
            throw new DefinedException(new DefaultErrorCode("404", "模版分项不存在"));
        }
        DesignTemplateVersion version = item.getVersion();
        DesignTemplate template = version.getTemplate();
        accessContext.requireMaintain(me, template);
        if (version.getStatus() != DesignTemplate.Status.Draft) {
            throw new DefinedException(new DefaultErrorCode("400", "已发布版本不可修改，请新建版本"));
        }
        itemRepository.delete(item);
        return item;
    }
}

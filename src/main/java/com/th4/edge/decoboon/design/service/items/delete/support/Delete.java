package com.th4.edge.decoboon.design.service.items.delete.support;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.DesignItem;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.quote.repository.QuoteLineRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** 设计分项删除实现。 */
@Note("设计分项删除")
@Properties(allow = {
        @AllowProperties(model = DesignItem.class, properties = {"itemId", "name"})
})
@Service("design/item/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.design.service.items.delete.Delete {

    @AutoInject
    @NotNull
    private Long itemId;

    @User
    private Employee employee;

    private final DesignItemRepository itemRepository;
    private final QuoteLineRepository quoteItemRepository;
    private final DataScopeContext dataScopeContext;
    private final DesignContext designContext;

    public Delete(DesignItemRepository itemRepository,
                  QuoteLineRepository quoteItemRepository,
                  DataScopeContext dataScopeContext,
                  DesignContext designContext) {
        this.itemRepository = itemRepository;
        this.quoteItemRepository = quoteItemRepository;
        this.dataScopeContext = dataScopeContext;
        this.designContext = designContext;
    }

    @Override
    public DesignItem call(DesignItem request) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<DesignItem> byId = (root, query, builder) -> builder.equal(root.get("itemId"), itemId);
        DesignItem item = itemRepository.findOne(byId.and(DataScopeSpecification.visible(DesignItem.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "设计分项不存在或无权访问")));
        designContext.requireEditable(item.getDesign());
        if (quoteItemRepository.existsByDesignItem_ItemId(itemId)) {
            throw new DefinedException(new DefaultErrorCode("400", "分项已生成报价快照，归档报价后才能删除"));
        }
        itemRepository.delete(item);
        return item;
    }
}

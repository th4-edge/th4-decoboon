package com.th4.edge.decoboon.design.service.spaces.delete.support;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;

import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.design.model.Space;
import com.th4.edge.decoboon.design.repository.DesignItemRepository;
import com.th4.edge.decoboon.design.repository.SpaceRepository;
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

/** 方案区域删除实现。区域下有分项时禁止删除。 */
@Note("方案区域删除")
@Properties(allow = {
        @AllowProperties(model = Space.class, properties = {"spaceId", "name"})
})
@Service("design/space/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.design.service.spaces.delete.Delete {

    @AutoInject
    @NotNull
    private Long spaceId;

    @User
    private Employee employee;

    private final SpaceRepository spaceRepository;
    private final DesignItemRepository itemRepository;
    private final DataScopeContext dataScopeContext;
    private final DesignContext designContext;

    public Delete(SpaceRepository spaceRepository, DesignItemRepository itemRepository,
                  DataScopeContext dataScopeContext, DesignContext designContext) {
        this.spaceRepository = spaceRepository;
        this.itemRepository = itemRepository;
        this.dataScopeContext = dataScopeContext;
        this.designContext = designContext;
    }

    @Override
    public Space call(Space request) {
        DataScope scope = dataScopeContext.requireScope(employee);
        Specification<Space> byId = (root, query, builder) -> builder.equal(root.get("spaceId"), spaceId);
        Space space = spaceRepository.findOne(byId.and(DataScopeSpecification.visible(Space.class, scope)))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "方案区域不存在或无权访问")));
        designContext.requireEditable(space.getDesign());
        if (itemRepository.findByDesign_DesignIdAndSpace_SpaceIdOrderBySortAsc(
                space.getDesign().getDesignId(), spaceId).size() > 0) {
            throw new DefinedException(new DefaultErrorCode("400", "区域下存在分项，请先删除分项"));
        }
        spaceRepository.delete(space);
        return space;
    }
}

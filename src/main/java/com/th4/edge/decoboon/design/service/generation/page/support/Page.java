package com.th4.edge.decoboon.design.service.generation.page.support;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.design.configure.DesignGenerationStatus;
import com.th4.edge.decoboon.design.model.Design;
import com.th4.edge.decoboon.design.model.DesignGenerationTask;
import com.th4.edge.decoboon.design.repository.DesignGenerationTaskRepository;
import com.th4.edge.decoboon.design.context.DesignContext;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScope;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/** AI设计生成任务分页实现。 */
@Note("AI设计生成任务分页")
@Properties(allow = {
        @AllowProperties(model = DesignGenerationTask.class,
                properties = {"generationId", "no", "design", "customer", "property", "owner", "mode", "status",
                        "version", "failureCode", "builtAt",
                        "completedAt", "selectedRender", "createTime", "updateTime"}),
        @AllowProperties(model = Design.class,
                properties = {"designId", "no", "name", "style", "status", "area", "layout", "coverUrl", "updateTime"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "area", "budget", "status"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "community", "area", "layout", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("design/generation/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.design.service.generation.page.Page {

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    @AutoInject
    private Long designId;

    @AutoInject
    private DesignGenerationStatus status;

    @User
    private Employee employee;

    private final DesignGenerationTaskRepository taskRepository;
    private final DataScopeContext dataScopeContext;
    private final DesignContext designContext;

    public Page(DesignGenerationTaskRepository taskRepository, DataScopeContext dataScopeContext,
                DesignContext designContext) {
        this.taskRepository = taskRepository;
        this.dataScopeContext = dataScopeContext;
        this.designContext = designContext;
    }

    @Override
    public org.springframework.data.domain.Page<DesignGenerationTask> call(DesignGenerationTask request) {
        Employee currentEmployee = designContext.requireDesignWriteAccess(employee);
        DataScope scope = dataScopeContext.requireScope(currentEmployee);
        Specification<DesignGenerationTask> condition = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (designId != null) {
                predicates.add(builder.equal(root.get("design").get("designId"), designId));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        return taskRepository.findAll(
                condition.and(DataScopeSpecification.visible(DesignGenerationTask.class, scope)),
                PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "generationId")));
    }
}

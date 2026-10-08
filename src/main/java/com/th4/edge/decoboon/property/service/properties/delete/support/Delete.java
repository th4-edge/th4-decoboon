package com.th4.edge.decoboon.property.service.properties.delete.support;

import com.th4.edge.components.lock.DistributedLock;
import com.th4.edge.decoboon.customer.context.CustomerContext;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.configure.MeasureStatus;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.decoboon.property.repository.MeasureRepository;
import com.th4.edge.decoboon.property.repository.PropertyRepository;
import com.th4.edge.decoboon.quote.repository.QuoteRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 删除「干净」房屋实现。
 *
 * <p>BR-001 要求报价、合同和项目必须绑定明确客户与房屋，因此房屋一旦承载过任何业务事实
 * （量房、客户诉求、设计方案、报价、合同、项目）就不得删除——删除会切断设计、报价与履约的溯源链。
 * 只有完全没有附带数据的房屋允许删除，删除为物理删除并消耗分布式锁串行化。</p>
 */
@Note("删除房屋")
@Properties(allow = {
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "customer", "community", "building", "roomNo",
                        "address", "area", "layout", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.customer.model.Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("property/property/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.property.service.properties.delete.Delete {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long propertyId;

    private final DistributedLock distributedLock;
    private final PropertyRepository propertyRepository;
    private final MeasureRepository measureRepository;
    private final RequirementRepository requirementRepository;
    private final DesignRepository designRepository;
    private final QuoteRepository quoteRepository;
    private final ProjectContractRepository contractRepository;
    private final ProjectRepository projectRepository;
    private final CustomerContext customerContext;
    private final DataScopeContext dataScopeContext;

    public Delete(DistributedLock distributedLock,
                  PropertyRepository propertyRepository,
                  MeasureRepository measureRepository,
                  RequirementRepository requirementRepository,
                  DesignRepository designRepository,
                  QuoteRepository quoteRepository,
                  ProjectContractRepository contractRepository,
                  ProjectRepository projectRepository,
                  CustomerContext customerContext,
                  DataScopeContext dataScopeContext) {
        this.distributedLock = distributedLock;
        this.propertyRepository = propertyRepository;
        this.measureRepository = measureRepository;
        this.requirementRepository = requirementRepository;
        this.designRepository = designRepository;
        this.quoteRepository = quoteRepository;
        this.contractRepository = contractRepository;
        this.projectRepository = projectRepository;
        this.customerContext = customerContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    @Transactional
    public Property call(Property request) {
        Employee current = dataScopeContext.requireEmployee(employee);
        Long comId = current.getCompany().getComId();
        String lockKey = "property:" + comId + ":" + propertyId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "该房屋正在被处理，请稍后重试"));
        }
        try {
            Property property = propertyRepository.findByPropertyIdAndCompany_ComId(propertyId, comId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "房屋不存在或无权访问")));
            // 数据范围：房屋必须落在当前员工可见的客户范围内
            customerContext.requireVisible(property.getCustomer().getCustId(), current);
            String blocked = blockedReason(comId, propertyId);
            if (blocked != null) {
                throw new DefinedException(new DefaultErrorCode("409", blocked));
            }
            propertyRepository.delete(property);
            return property;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    /** 返回第一条阻断原因；完全干净时返回 null。 */
    private String blockedReason(Long comId, Long propertyId) {
        List<Long> ids = List.of(propertyId);
        for (MeasureStatus status : MeasureStatus.values()) {
            if (measureRepository.existsByCompany_ComIdAndProperty_PropertyIdInAndStatus(comId, ids, status)) {
                return "该房屋已有量房记录，不能删除；请保留量房事实用于设计依据";
            }
        }
        if (!requirementRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByRequirementIdDesc(comId, ids).isEmpty()) {
            return "该房屋已有客户诉求，不能删除";
        }
        if (!designRepository.findByCompany_ComIdAndProperty_PropertyIdInOrderByVersionDesc(comId, ids).isEmpty()) {
            return "该房屋已有设计方案，不能删除";
        }
        if (quoteRepository.countByCompany_ComIdAndProperty_PropertyId(comId, propertyId) > 0) {
            return "该房屋已有报价，不能删除；报价与合同需要保留溯源";
        }
        if (contractRepository.existsByProperty_PropertyId(propertyId)) {
            return "该房屋已有合同，不能删除";
        }
        if (projectRepository.existsByCompany_ComIdAndProperty_PropertyId(comId, propertyId)) {
            return "该房屋已有项目，不能删除";
        }
        return null;
    }
}

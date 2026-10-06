package com.th4.edge.decoboon.customer.service.cust.delete.support;

import com.th4.edge.components.lock.DistributedLock;

import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.repository.CustomerFollowRepository;
import com.th4.edge.decoboon.customer.repository.CustomerPortraitRepository;
import com.th4.edge.decoboon.customer.repository.CustomerRepository;
import com.th4.edge.decoboon.customer.repository.CustomerTagLinkRepository;
import com.th4.edge.decoboon.design.repository.DesignRepository;
import com.th4.edge.decoboon.design.repository.RequirementRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
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
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 删除无业务事实客户实现。 */
@Note("删除客户")
@Properties(allow = {
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "telNo", "owner", "designer", "status", "source"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("customer/customer/delete")
@Scope(ScopeTypes.PROTOTYPE)
public class Delete implements com.th4.edge.decoboon.customer.service.cust.delete.Delete {

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long customerId;

    private final TenantContext tenantContext;
    private final DistributedLock distributedLock;
    private final CustomerRepository customerRepository;
    private final CustomerFollowRepository followRepository;
    private final CustomerPortraitRepository portraitRepository;
    private final CustomerTagLinkRepository tagLinkRepository;
    private final PropertyRepository propertyRepository;
    private final QuoteRepository quoteRepository;
    private final DesignRepository designRepository;
    private final RequirementRepository requirementRepository;
    private final ProjectRepository projectRepository;
    private final ProjectContractRepository contractRepository;

    public Delete(TenantContext tenantContext,
                  DistributedLock distributedLock,
                  CustomerRepository customerRepository,
                  CustomerFollowRepository followRepository,
                  CustomerPortraitRepository portraitRepository,
                  CustomerTagLinkRepository tagLinkRepository,
                  PropertyRepository propertyRepository,
                  QuoteRepository quoteRepository,
                  DesignRepository designRepository,
                  RequirementRepository requirementRepository,
                  ProjectRepository projectRepository,
                  ProjectContractRepository contractRepository) {
        this.tenantContext = tenantContext;
        this.distributedLock = distributedLock;
        this.customerRepository = customerRepository;
        this.followRepository = followRepository;
        this.portraitRepository = portraitRepository;
        this.tagLinkRepository = tagLinkRepository;
        this.propertyRepository = propertyRepository;
        this.quoteRepository = quoteRepository;
        this.designRepository = designRepository;
        this.requirementRepository = requirementRepository;
        this.projectRepository = projectRepository;
        this.contractRepository = contractRepository;
    }

    @Override
    @Transactional
    public Customer call(Customer request) {
        Employee current = tenantContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Boss) {
            throw new DefinedException(new DefaultErrorCode("403", "仅企业负责人可以删除客户"));
        }
        Long comId = current.getCompany().getComId();
        // 删除是先读后写：并发删除同一客户必须串行，替代原 customer 行级悲观锁。
        String lockKey = "customer:" + comId + ":" + customerId;
        if (!distributedLock.tryLock(lockKey, 1000, 30000)) {
            throw new DefinedException(new DefaultErrorCode("409", "该客户正在被处理，请稍后重试"));
        }
        try {
            Customer customer = customerRepository.findByCustIdAndCompany_ComId(customerId, comId)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "客户不存在或无权访问")));
            if (hasBusinessFacts(comId, customer.getCustId())) {
                throw new DefinedException(new DefaultErrorCode(
                        "409", "客户已形成跟进、房屋、设计、报价、合同或项目事实，不能删除"));
            }
            portraitRepository.deleteByCompany_ComIdAndCustomer_CustId(comId, customer.getCustId());
            tagLinkRepository.deleteByCompany_ComIdAndCustomer_CustId(comId, customer.getCustId());
            customerRepository.delete(customer);
            return customer;
        } finally {
            distributedLock.unlock(lockKey);
        }
    }

    private boolean hasBusinessFacts(Long comId, Long customerId) {
        return followRepository.existsByCustomer_CustId(customerId)
                || propertyRepository.existsByCompany_ComIdAndCustomer_CustId(comId, customerId)
                || designRepository.existsByCompany_ComIdAndCustomer_CustId(comId, customerId)
                || requirementRepository.existsByCompany_ComIdAndCustomer_CustId(comId, customerId)
                || quoteRepository.existsByCompany_ComIdAndCustomer_CustId(comId, customerId)
                || contractRepository.existsByCompany_ComIdAndCustomer_CustId(comId, customerId)
                || projectRepository.existsByCompany_ComIdAndCustomer_CustId(comId, customerId);
    }
}

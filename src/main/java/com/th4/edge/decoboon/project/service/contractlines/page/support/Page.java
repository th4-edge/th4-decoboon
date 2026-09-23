package com.th4.edge.decoboon.project.service.contractlines.page.support;

import com.th4.edge.decoboon.catalog.context.CatalogCostContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.decoboon.project.repository.ContractLineRepository;
import com.th4.edge.decoboon.project.repository.ProjectContractRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 合同基线行查询实现。
 *
 * <p>合同行是签约时对报价行的冻结快照，签约后主数据变化不回写历史。
 * 成本字段按角色裁剪：无成本权限的角色响应中不返回 cost。</p>
 */
@Note("合同基线行查询")
@Properties(allow = {
        @AllowProperties(model = ContractLine.class,
                properties = {"lineId", "contract", "property", "type", "category", "name", "brand", "spec",
                        "unit", "quantity", "materialPrice", "manualPrice", "pricingMode", "amount", "cost",
                        "formula", "role", "spaceName", "sort"}),
        @AllowProperties(model = ProjectContract.class,
                properties = {"contractId", "no", "baselineAmount", "signedAmount", "currentAmount", "status"}),
        @AllowProperties(model = com.th4.edge.decoboon.property.model.Property.class,
                properties = {"propertyId", "community", "building", "roomNo", "address", "area"})
})
@Service("project/contract-line/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.project.service.contractlines.page.Page {

    @AutoInject
    private Long projectId;

    @AutoInject
    private Long contractId;

    @User
    private Employee employee;

    private final ProjectContractRepository contractRepository;
    private final ContractLineRepository lineRepository;
    private final ProjectAccessContext accessContext;
    private final CatalogCostContext costContext;

    public Page(ProjectContractRepository contractRepository,
                ContractLineRepository lineRepository,
                ProjectAccessContext accessContext,
                CatalogCostContext costContext) {
        this.contractRepository = contractRepository;
        this.lineRepository = lineRepository;
        this.accessContext = accessContext;
        this.costContext = costContext;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ContractLine> call(ContractLine request) {
        Employee current = accessContext.requireEmployee(employee);
        ProjectContract contract = resolveContract(current);
        if (contract == null) {
            return new PageImpl<>(List.of());
        }
        List<ContractLine> lines = lineRepository.findByContractOrderBySortAsc(contract);
        if (!costContext.canViewCost(current)) {
            lines.forEach(line -> line.setCost(null));
        }
        return new PageImpl<>(lines);
    }

    private ProjectContract resolveContract(Employee current) {
        if (projectId != null) {
            Project project = accessContext.requireProject(projectId, current);
            return contractRepository.findByProject(project)
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "当前项目尚未生成合同基线")));
        }
        if (contractId != null) {
            Specification<ProjectContract> byId =
                    (root, query, builder) -> builder.equal(root.get("contractId"), contractId);
            return contractRepository.findOne(byId.and(accessContext.visibleContract(employee)))
                    .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "合同不存在或无权访问")));
        }
        return null;
    }
}

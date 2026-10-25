package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.MaterialPlan;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 项目材料计划仓储。
 */
public interface MaterialPlanRepository extends JpaRepository<MaterialPlan, Long>,
        JpaSpecificationExecutor<MaterialPlan> {

    List<MaterialPlan> findByProject(Project project);

    long countByProject(Project project);

    /** 批量按主键取计划（写单/下单路径循环外一次取数，DEC-036 A2b）。 */
    List<MaterialPlan> findByProject_ProjectIdAndPlanIdIn(Long projectId, Collection<Long> planIds);

    @EntityGraph(attributePaths = {"contractLine"})
    Optional<MaterialPlan> findFirstByProjectAndContractLine(Project project, ContractLine contractLine);

    /** 同一合同行可能同时存在原计划与变更计划：门禁需按全部计划判定。 */
    List<MaterialPlan> findByProjectAndContractLine(Project project, ContractLine contractLine);

    /** 企业负责人增减项工作台：按项目集合批量取材料计划，预取来源变更行。 */
    @EntityGraph(attributePaths = {"project", "property", "contractLine", "catalogItem", "quoteItem", "packageLine", "task", "changeItem", "selection"})
    List<MaterialPlan> findByProject_ProjectIdIn(Collection<Long> projectIds);
}

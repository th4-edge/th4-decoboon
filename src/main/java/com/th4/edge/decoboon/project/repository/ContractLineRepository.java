package com.th4.edge.decoboon.project.repository;

import com.th4.edge.decoboon.project.model.ContractLine;
import com.th4.edge.decoboon.project.model.ProjectContract;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** 合同基线行仓储。 */
public interface ContractLineRepository extends JpaRepository<ContractLine, Long>,
        JpaSpecificationExecutor<ContractLine> {
    List<ContractLine> findByContractOrderBySortAsc(ProjectContract contract);

    /** 合同档案行分页：数据库排序、数据库切页，禁止全量加载后 subList。 */
    Page<ContractLine> findByContractOrderBySortAsc(ProjectContract contract, Pageable pageable);

    /** 合同履约档案：一条聚合 SQL 返回基线合同行数与行金额合计。 */
    @Query("select count(line), coalesce(sum(line.amount), 0) from ContractLine line where line.contract = :contract")
    List<Object[]> summarizeByContract(@Param("contract") ProjectContract contract);

    /**
     * 合同读模型成本摘要：一次数据库聚合返回行数、成本合计和必备成本缺失行数。
     * 口径与合同行逐行判断一致：Construction/Material/Custom/Soft/Appliance 的 cost <= 0 视为缺失。
     */
    @Query(value = """
            select count(*),
                   coalesce(sum(coalesce(cost, 0)), 0),
                   coalesce(sum(case
                       when type in ('Construction', 'Material', 'Custom', 'Soft', 'Appliance')
                            and coalesce(cost, 0) <= 0 then 1 else 0 end), 0)
              from db_contract_line
             where contract_id = :contractId
            """, nativeQuery = true)
    List<Object[]> summarizeCostByContract(@Param("contractId") Long contractId);

    /** 合同变更批量录入：一次按合同范围加载原合同行，避免逐行查库。 */
    List<ContractLine> findByLineIdInAndContract_ContractId(java.util.Collection<Long> lineIds, Long contractId);

    /** 选择器指定来源：只加载一条且同时校验合同归属。 */
    java.util.Optional<ContractLine> findByLineIdAndContract_ContractId(Long lineId, Long contractId);

    /** 合同行选择器筛选项：按类型/区域聚合，仅返回有限分组，不加载合同行实体。 */
    @Query("""
            select line.type, line.spaceName, count(line)
              from ContractLine line
             where line.contract = :contract
             group by line.type, line.spaceName
             order by line.type, line.spaceName
            """)
    List<Object[]> summarizePickerFacetsByContract(@Param("contract") ProjectContract contract);

    @EntityGraph(attributePaths = {"sourceQuoteLine", "sourceQuoteLine.catalogItem"})
    List<ContractLine> findByContract_Project_ProjectIdOrderBySortAsc(Long projectId);

    /** 财务汇总：一次取多项目合同行（预取合同所属项目便于分组 + 报价行/目录项）。 */
    @EntityGraph(attributePaths = {"contract", "contract.project", "sourceQuoteLine", "sourceQuoteLine.catalogItem"})
    List<ContractLine> findByContract_Project_ProjectIdInOrderBySortAsc(java.util.Collection<Long> projectIds);
}

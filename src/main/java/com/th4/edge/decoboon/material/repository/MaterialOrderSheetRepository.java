package com.th4.edge.decoboon.material.repository;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 主材下单作业单仓储。 */
public interface MaterialOrderSheetRepository extends JpaRepository<MaterialOrderSheet, Long>,
        JpaSpecificationExecutor<MaterialOrderSheet> {

    List<MaterialOrderSheet> findByProject_ProjectIdOrderBySheetIdDesc(Long projectId);

    List<MaterialOrderSheet> findByProject_ProjectIdInOrderBySheetIdDesc(Collection<Long> projectIds);

    List<MaterialOrderSheet> findByCompany_ComIdOrderBySheetIdDesc(Long comId);

    Optional<MaterialOrderSheet> findFirstByCompany_ComIdAndSheetNo(Long comId, String sheetNo);

    Optional<MaterialOrderSheet> findFirstBySheetIdAndCompany_ComId(Long sheetId, Long comId);

    boolean existsByCompany_ComIdAndSheetNo(Long comId, String sheetNo);

    java.util.Optional<MaterialOrderSheet> findFirstByCompany_ComIdAndSheetNoStartingWithOrderBySheetNoDesc(
            Long comId, String prefix);

    long countByProject_ProjectIdAndStatusIn(Long projectId, Collection<com.th4.edge.decoboon.material.configure.MaterialSheetStatus> statuses);

    /**
     * 品类维度汇总（**聚合 SQL 直接算出**，BR-031 / DEC-036）：
     * 返回行数 = 品类数（O(品类)），不加载任何实体、不做 Java 循环求和。
     */
    @Query("""
            select s.sheetType,
                   count(s),
                   coalesce(sum(case when s.priceState = :unpriced then 1 else 0 end), 0),
                   coalesce(sum(case when s.status in :orderedStatuses then 1 else 0 end), 0)
              from MaterialOrderSheet s
             where s.company.comId = :comId
             group by s.sheetType
             order by s.sheetType
            """)
    List<Object[]> aggregateBySheetType(@Param("comId") Long comId,
                                        @Param("unpriced") com.th4.edge.decoboon.material.configure.SheetPriceState unpriced,
                                        @Param("orderedStatuses") Collection<com.th4.edge.decoboon.material.configure.MaterialSheetStatus> orderedStatuses);

    /** 品类维度汇总（限定项目）。 */
    @Query("""
            select s.sheetType,
                   count(s),
                   coalesce(sum(case when s.priceState = :unpriced then 1 else 0 end), 0),
                   coalesce(sum(case when s.status in :orderedStatuses then 1 else 0 end), 0)
              from MaterialOrderSheet s
             where s.company.comId = :comId and s.project.projectId = :projectId
             group by s.sheetType
             order by s.sheetType
            """)
    List<Object[]> aggregateBySheetTypeAndProject(@Param("comId") Long comId,
                                                  @Param("projectId") Long projectId,
                                                  @Param("unpriced") com.th4.edge.decoboon.material.configure.SheetPriceState unpriced,
                                                  @Param("orderedStatuses") Collection<com.th4.edge.decoboon.material.configure.MaterialSheetStatus> orderedStatuses);
}

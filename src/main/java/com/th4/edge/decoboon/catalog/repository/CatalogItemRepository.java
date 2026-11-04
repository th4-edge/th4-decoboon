package com.th4.edge.decoboon.catalog.repository;

import com.th4.edge.decoboon.catalog.configure.ItemType;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 统一家装材料库仓储。 */
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long>,
        JpaSpecificationExecutor<CatalogItem> {

    long countByCompany_ComIdAndType(Long comId, ItemType type);

    long countByCompany_ComIdAndTypeAndActiveTrue(Long comId, ItemType type);

    long countByCompany_ComIdAndTypeAndActive(Long comId, ItemType type, Boolean active);

    Optional<CatalogItem> findByItemIdAndCompany_ComId(Long itemId, Long comId);

    /** 取该企业该类型下编码最大的一条，用于自动生成下一个编号。 */
    Optional<CatalogItem> findFirstByCompany_ComIdAndTypeOrderByCodeDesc(Long comId, ItemType type);

    @Query("""
            select distinct item.category, item.brand, item.series, item.spec
            from CatalogItem item
            where item.company.comId = :comId and item.type = :type
              and (:active is null or item.active = :active)
            order by item.category, item.brand, item.series, item.spec
            """)
    List<Object[]> findFacetsByCompanyAndTypeAndActive(@Param("comId") Long comId,
                                                       @Param("type") ItemType type,
                                                       @Param("active") Boolean active);

    List<CatalogItem> findByCompany_ComIdAndTypeAndActiveTrueOrderByCategoryAscBrandAscSeriesAscNameAsc(
            Long comId, ItemType type);

    /** 企业全量报价项目，用于批量导入去重与编码冲突校验。 */
    List<CatalogItem> findByCompany_ComId(Long comId);
}

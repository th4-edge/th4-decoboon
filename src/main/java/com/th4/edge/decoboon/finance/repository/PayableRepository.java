package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** 应付仓储。 */
public interface PayableRepository extends JpaRepository<Payable, Long>,
        JpaSpecificationExecutor<Payable> {

    List<Payable> findByProjectOrderByDueDateAsc(Project project);

    List<Payable> findByCompany_ComIdAndProject_ProjectIdOrderByDueDateAsc(Long comId, Long projectId);

    @EntityGraph(attributePaths = {"project", "property", "store", "owner", "team", "payeeEmployee", "payeePartner"})
    List<Payable> findByCompany_ComIdOrderByDueDateAsc(Long comId);

    List<Payable> findByCompany_ComIdAndProject_ProjectIdIn(Long comId, Collection<Long> projectIds);

    List<Payable> findByCompany_ComIdAndPayeePartner_PartnerIdOrderByDueDateAsc(Long comId, Long partnerId);

    /** 下单即形成应付（DEC-MP-01）：按采购单幂等查询，避免重复生成。 */
    java.util.Optional<Payable> findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(Long comId, Long purchaseOrderId);

    List<Payable> findByPurchaseOrder_PurchaseId(Long purchaseOrderId);

    java.util.Optional<Payable> findByCompany_ComIdAndSourceTypeAndSourceId(
            Long comId, String sourceType, Long sourceId);

    /** 兼容历史数据：早期应付只写 source_no（采购单号），用来源单号去重。 */
    java.util.Optional<Payable> findFirstByCompany_ComIdAndSourceNoAndType(
            Long comId, String sourceNo, com.th4.edge.decoboon.finance.configure.PayableType type);

    /** 分页后的批量详情读取：一次 JOIN FETCH 预取页面白名单关联，避免逐行懒加载。 */
    @Query("""
            select p from Payable p
            left join fetch p.project pr
            left join fetch pr.property
            left join fetch pr.customer
            left join fetch pr.manager
            left join fetch p.property
            left join fetch p.materialPlan
            left join fetch p.payeeEmployee
            left join fetch p.payeePartner
            left join fetch p.createdBy
            where p.payableId in :ids
            """)
    List<Payable> findDetailsByIdIn(@Param("ids") Collection<Long> ids);
}

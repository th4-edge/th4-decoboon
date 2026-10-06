package com.th4.edge.decoboon.finance.repository;

import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

/** 应付仓储。 */
public interface PayableRepository extends JpaRepository<Payable, Long>,
        JpaSpecificationExecutor<Payable> {

    List<Payable> findByProjectOrderByDueDateAsc(Project project);

    List<Payable> findByCompany_ComIdAndProject_ProjectIdOrderByDueDateAsc(Long comId, Long projectId);

    @EntityGraph(attributePaths = {"project", "property", "store", "owner", "team", "payeeEmployee", "payeePartner"})
    List<Payable> findByCompany_ComIdOrderByDueDateAsc(Long comId);

    List<Payable> findByCompany_ComIdAndPayeePartner_PartnerIdOrderByDueDateAsc(Long comId, Long partnerId);

    /** 下单即形成应付（DEC-MP-01）：按采购单幂等查询，避免重复生成。 */
    java.util.Optional<Payable> findFirstByCompany_ComIdAndPurchaseOrder_PurchaseId(Long comId, Long purchaseOrderId);

    /** 财务汇总展示：按 id 批量重取，预取响应实际使用的关联，避免序列化期逐条懒加载。 */
    @EntityGraph(attributePaths = {"project", "project.property", "project.customer", "property", "payeeEmployee"})
    List<Payable> findByCompany_ComIdAndPayableIdIn(Long comId, java.util.Collection<Long> payableIds);

    List<Payable> findByPurchaseOrder_PurchaseId(Long purchaseOrderId);

    /** 兼容历史数据：早期应付只写 source_no（采购单号），用来源单号去重。 */
    java.util.Optional<Payable> findFirstByCompany_ComIdAndSourceNoAndType(
            Long comId, String sourceNo, com.th4.edge.decoboon.finance.configure.PayableType type);
}

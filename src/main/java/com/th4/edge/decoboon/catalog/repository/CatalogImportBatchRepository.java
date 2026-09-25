package com.th4.edge.decoboon.catalog.repository;

import com.th4.edge.decoboon.catalog.configure.CatalogImportStatus;
import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 报价项目库导入批次仓储。 */
public interface CatalogImportBatchRepository extends JpaRepository<CatalogImportBatch, Long>,
        JpaSpecificationExecutor<CatalogImportBatch> {

    Optional<CatalogImportBatch> findByBatchIdAndCompany_ComId(Long batchId, Long comId);

    /** 企业内仍占用同一文件指纹的批次（重复上传幂等命中）。 */
    List<CatalogImportBatch> findByCompany_ComIdAndFileSha256AndStatusInOrderByBatchIdDesc(
            Long comId, String fileSha256, Collection<CatalogImportStatus> statuses);

    /** 最近导入记录。 */
    List<CatalogImportBatch> findTop10ByCompany_ComIdOrderByBatchIdDesc(Long comId);

    List<CatalogImportBatch> findByCompany_ComIdOrderByBatchIdDesc(Long comId);

    /** 行级排他锁：确认导入与撤销串行化，重复提交返回同一批次事实。 */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select batch from CatalogImportBatch batch where batch.batchId = :batchId")
    Optional<CatalogImportBatch> findLockedByBatchId(@Param("batchId") Long batchId);
}

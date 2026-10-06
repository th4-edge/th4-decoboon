package com.th4.edge.decoboon.catalog.repository;

import com.th4.edge.decoboon.catalog.configure.CatalogImportStatus;
import com.th4.edge.decoboon.catalog.model.CatalogImportBatch;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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

    /** 非加锁读取；确认导入与撤销的并发由 RedissonLock 串行化。 */
    Optional<CatalogImportBatch> findByBatchId(Long batchId);
}

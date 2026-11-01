package com.th4.edge.decoboon.labor.repository;

import com.th4.edge.decoboon.labor.configure.WorkerStatus;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 工人档案仓储。 */
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    List<Worker> findByCompany_ComIdOrderByWorkerIdDesc(Long comId);

    /** 工人选择器数据库分页：关键字和状态在 SQL 内过滤，禁止全量加载后内存筛选。 */
    @Query(value = "select w from Worker w where w.company.comId = :comId "
            + "and w.status in :statuses "
            + "and (:keywordPattern is null or lower(w.name) like :keywordPattern "
            + "or w.phone like :keywordPattern or lower(w.trade) like :keywordPattern) "
            + "order by w.status asc, w.workerId desc",
            countQuery = "select count(w) from Worker w where w.company.comId = :comId "
            + "and w.status in :statuses "
            + "and (:keywordPattern is null or lower(w.name) like :keywordPattern "
            + "or w.phone like :keywordPattern or lower(w.trade) like :keywordPattern)")
    Page<Worker> findPickerPage(@Param("comId") Long comId,
                                @Param("statuses") Collection<WorkerStatus> statuses,
                                @Param("keywordPattern") String keywordPattern,
                                Pageable pageable);

    Optional<Worker> findByWorkerIdAndCompany_ComId(Long workerId, Long comId);

    Optional<Worker> findFirstByCompany_ComIdAndIdCardNo(Long comId, String idCardNo);

    List<Worker> findByCompany_ComIdAndStatusOrderByName(Long comId, WorkerStatus status);
}

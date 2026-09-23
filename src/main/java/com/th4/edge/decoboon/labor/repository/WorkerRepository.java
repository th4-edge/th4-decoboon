package com.th4.edge.decoboon.labor.repository;

import com.th4.edge.decoboon.labor.configure.WorkerStatus;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 工人档案仓储。 */
public interface WorkerRepository extends JpaRepository<Worker, Long> {

    List<Worker> findByCompany_ComIdOrderByWorkerIdDesc(Long comId);

    Optional<Worker> findByWorkerIdAndCompany_ComId(Long workerId, Long comId);

    Optional<Worker> findFirstByCompany_ComIdAndIdCardNo(Long comId, String idCardNo);

    List<Worker> findByCompany_ComIdAndStatusOrderByName(Long comId, WorkerStatus status);
}

package com.th4.edge.decoboon.customer.repository;

import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.framework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/** 业主意见投诉仓储。 */
public interface CustomerFeedbackRepository extends JpaRepository<CustomerFeedback, Long>,
        JpaSpecificationExecutor<CustomerFeedback> {

    List<CustomerFeedback> findByCredential_CreIdOrderByFeedbackIdDesc(Long creId);

    List<CustomerFeedback> findByProject_ProjectIdAndCompany_ComIdOrderByFeedbackIdDesc(Long projectId, Long comId);

    Optional<CustomerFeedback> findByFeedbackIdAndCredential_CreId(Long feedbackId, Long creId);

    long countByProject_ProjectIdAndStatusIn(Long projectId, java.util.Collection<com.th4.edge.decoboon.customer.configure.CustomerFeedbackStatus> statuses);
}

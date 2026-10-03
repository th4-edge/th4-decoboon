package com.th4.edge.decoboon.ai.billing.repository;

import com.th4.edge.decoboon.ai.billing.configure.AiRateCardStatus;
import com.th4.edge.decoboon.ai.billing.model.AiRateCardVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiRateCardVersionRepository extends JpaRepository<AiRateCardVersion, Long> {
    Optional<AiRateCardVersion> findFirstByCapabilityCodeAndStatusOrderByVersionNoDesc(
            String capabilityCode, AiRateCardStatus status);
}

package com.th4.edge.platform.service.onboarding;

import com.th4.edge.platform.context.PlatformAccessContext;
import com.th4.edge.platform.model.PlatformOnboardingServiceRecord;
import com.th4.edge.platform.model.PlatformOnboardingServiceStatus;
import com.th4.edge.platform.model.PlatformOnboardingServiceView;
import com.th4.edge.platform.model.PlatformOperationLog;
import com.th4.edge.platform.model.PlatformUser;
import com.th4.edge.platform.repository.PlatformOnboardingServiceRepository;
import com.th4.edge.platform.repository.PlatformOperationLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 企业入驻服务记录读写上下文。 */
@Component
public class PlatformOnboardingServiceContext {
    private final PlatformOnboardingServiceRepository serviceRepository;
    private final PlatformOperationLogRepository operationLogRepository;

    public PlatformOnboardingServiceContext(PlatformOnboardingServiceRepository serviceRepository,
                                            PlatformOperationLogRepository operationLogRepository) {
        this.serviceRepository = serviceRepository;
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, PlatformOnboardingServiceView> latest(Long orgId, Collection<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) return Map.of();
        List<PlatformOnboardingServiceRecord> records = serviceRepository
                .findByOrg_OrgIdAndTargetComIdInOrderByServiceIdDesc(orgId, companyIds);
        Map<Long, PlatformOnboardingServiceView> result = new LinkedHashMap<>();
        for (PlatformOnboardingServiceRecord record : records) {
            result.putIfAbsent(record.getTargetComId(), view(record));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<PlatformOnboardingServiceView> records(Long orgId, Long companyId) {
        return serviceRepository.findByOrg_OrgIdAndTargetComIdOrderByServiceIdDesc(orgId, companyId)
                .stream().map(this::view).toList();
    }

    @Transactional
    public PlatformOnboardingServiceView save(PlatformUser operator,
                                              Long companyId,
                                              String companyTitle,
                                              PlatformOnboardingServiceStatus status,
                                              String contactResult,
                                              String nextAction,
                                              LocalDateTime nextFollowAt,
                                              String remark,
                                              String reason) {
        PlatformOnboardingServiceRecord before = serviceRepository
                .findFirstByOrg_OrgIdAndTargetComIdOrderByServiceIdDesc(operator.getOrg().getOrgId(), companyId)
                .orElse(null);
        PlatformOnboardingServiceRecord record = new PlatformOnboardingServiceRecord();
        record.setOrg(operator.getOrg());
        record.setOperator(operator);
        record.setTargetComId(companyId);
        record.setTargetCompanyTitle(companyTitle);
        record.setStatus(status);
        record.setContactResult(contactResult);
        record.setNextAction(nextAction);
        record.setNextFollowAt(nextFollowAt);
        record.setRemark(remark);
        record = serviceRepository.save(record);

        PlatformOperationLog log = new PlatformOperationLog();
        log.setOrg(operator.getOrg());
        log.setOperator(operator);
        log.setAction(com.th4.edge.platform.model.PlatformAction.OnboardingServiceUpdate);
        log.setTargetType("Company");
        log.setTargetKey(String.valueOf(companyId));
        log.setTargetComId(companyId);
        log.setTargetCompanyTitle(companyTitle);
        log.setSummary("企业入驻服务更新");
        log.setBeforeValue(before == null ? "服务状态=待联系" : "服务状态=" + before.getStatus().getName());
        log.setAfterValue("服务状态=" + status.getName());
        log.setReason(reason);
        operationLogRepository.save(log);
        return view(record);
    }

    public PlatformOnboardingServiceView view(PlatformOnboardingServiceRecord record) {
        PlatformOnboardingServiceView view = new PlatformOnboardingServiceView();
        view.setServiceId(record.getServiceId());
        view.setStatus(record.getStatus());
        view.setContactResult(record.getContactResult());
        view.setNextAction(record.getNextAction());
        view.setNextFollowAt(record.getNextFollowAt());
        view.setRemark(record.getRemark());
        view.setOperatorName(record.getOperator() == null ? null : record.getOperator().getName());
        view.setCreateTime(record.getCreateTime() == null ? null : record.getCreateTime().toLocalDateTime());
        return view;
    }
}

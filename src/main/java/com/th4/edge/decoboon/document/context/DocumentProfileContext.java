package com.th4.edge.decoboon.document.context;

import com.th4.edge.decoboon.document.model.DocumentProfile;
import com.th4.edge.decoboon.document.repository.DocumentProfileRepository;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

/** 企业单据抬头资料的默认继承与维护。 */
@Component
public class DocumentProfileContext {

    private final DocumentProfileRepository profileRepository;
    private final TenantContext tenantContext;

    public DocumentProfileContext(DocumentProfileRepository profileRepository, TenantContext tenantContext) {
        this.profileRepository = profileRepository;
        this.tenantContext = tenantContext;
    }

    public DocumentProfile profile(Employee principal) {
        Employee current = tenantContext.requireEmployee(principal);
        return profileRepository.findByCompany_ComId(current.getCompany().getComId())
                .orElseGet(() -> defaultProfile(current));
    }

    public DocumentProfile save(Employee principal, String legalName, String creditCode,
                                String legalRepresentative, String address, String serviceHotline,
                                String bankName, String bankAccount, String logo) {
        Employee current = tenantContext.requireBoss(principal);
        DocumentProfile profile = profileRepository.findByCompany_ComId(current.getCompany().getComId())
                .orElseGet(() -> {
                    DocumentProfile created = new DocumentProfile();
                    created.setCompany(current.getCompany());
                    return created;
                });
        profile.setLegalName(trim(legalName));
        profile.setCreditCode(trim(creditCode));
        profile.setLegalRepresentative(trim(legalRepresentative));
        profile.setAddress(trim(address));
        profile.setServiceHotline(trim(serviceHotline));
        profile.setBankName(trim(bankName));
        profile.setBankAccount(trim(bankAccount));
        profile.setLogo(trim(logo));
        return profileRepository.save(profile);
    }

    private DocumentProfile defaultProfile(Employee employee) {
        DocumentProfile profile = new DocumentProfile();
        profile.setCompany(employee.getCompany());
        profile.setLegalName(employee.getCompany().getTitle() == null
                ? employee.getCompany().getName() : employee.getCompany().getTitle());
        profile.setAddress(employee.getCompany().getAddr());
        profile.setLogo(employee.getCompany().getLogo());
        return profile;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

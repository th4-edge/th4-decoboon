package com.th4.edge.site.service.lead.submit.support;

import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.site.configure.SiteLeadStatus;
import com.th4.edge.site.model.SiteLead;
import com.th4.edge.site.repository.SiteLeadRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 官网预约线索提交实现。 */
@Note("官网预约线索提交")
@Properties(allow = @AllowProperties(model = SiteLead.class,
        properties = {"leadId", "status", "createTime"}))
@Service("site/lead/submit")
@Scope(ScopeTypes.PROTOTYPE)
public class Submit implements com.th4.edge.site.service.lead.submit.Submit {

    @AutoInject @NotEmpty @Length(max = 80) private String name;
    @AutoInject @NotEmpty @Length(max = 120) private String contact;
    @AutoInject @NotEmpty @Length(max = 80) private String topic;
    @AutoInject @Length(max = 500) private String note;
    @AutoInject @Length(max = 500) private String pageUrl;
    @AutoInject @Length(max = 200) private String website;
    @AutoInject @NotNull private Boolean consent;

    private final SiteLeadRepository repository;

    public Submit(SiteLeadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SiteLead call(SiteLead request) {
        if (StringUtils.hasText(website)) {
            throw new DefinedException(new DefaultErrorCode("400", "提交无效"));
        }
        if (!Boolean.TRUE.equals(consent)) {
            throw new DefinedException(new DefaultErrorCode("400", "请先同意信息用于本次沟通"));
        }

        SiteLead lead = new SiteLead();
        lead.setName(name.trim());
        lead.setContact(contact.trim());
        lead.setTopic(topic.trim());
        lead.setNote(StringUtils.hasText(note) ? note.trim() : null);
        lead.setPageUrl(StringUtils.hasText(pageUrl) ? pageUrl.trim() : null);
        lead.setSource("website");
        lead.setStatus(SiteLeadStatus.New);
        lead.setConsent(Boolean.TRUE);
        return repository.save(lead);
    }
}

package com.th4.edge.site.service.lead.douyin.support;

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

import java.util.ArrayList;
import java.util.List;

/**
 * 抖音渠道线索提交实现。
 *
 * <p>线索只写平台域 SiteLead，source=douyin；归因信息暂存 note/pageUrl，
 * 待 DEC-031 完整字段（externalLeadId / channelUserIdHash / contentId 等）落地后迁移。</p>
 */
@Note("抖音渠道线索提交")
@Properties(allow = @AllowProperties(model = SiteLead.class,
        properties = {"leadId", "status", "createTime"}))
@Service("site/lead/douyin/submit")
@Scope(ScopeTypes.PROTOTYPE)
public class Submit implements com.th4.edge.site.service.lead.douyin.Submit {

    /** 称呼 */
    @AutoInject @NotEmpty @Length(max = 80) private String name;
    /** 手机号（线索组件授权后由前端提交） */
    @AutoInject @NotEmpty @Length(max = 120) private String contact;
    /** 最想解决的问题（对应 P1–P7 措辞） */
    @AutoInject @NotEmpty @Length(max = 80) private String painPoint;
    /** 装企类型（T1–T3 对外口径） */
    @AutoInject @Length(max = 40) private String enterpriseType;
    /** 来源场景（如 entry-1..4 / 视频锚点标识） */
    @AutoInject @Length(max = 100) private String sourceScene;
    /** 内容归因：视频 ID */
    @AutoInject @Length(max = 100) private String videoId;
    /** 内容归因：内容 ID */
    @AutoInject @Length(max = 100) private String contentId;
    /** 内容归因：挂载锚点 ID */
    @AutoInject @Length(max = 100) private String anchorId;
    /** 蜜罐字段：正常用户不会填写 */
    @AutoInject @Length(max = 200) private String website;
    /** 同意沟通 */
    @AutoInject @NotNull private Boolean consent;

    private final SiteLeadRepository repository;

    public Submit(SiteLeadRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SiteLead call(SiteLead request) {
        // 蜜罐：命中直接视为无效，不落库
        if (StringUtils.hasText(website)) {
            throw new DefinedException(new DefaultErrorCode("400", "提交无效"));
        }
        if (!Boolean.TRUE.equals(consent)) {
            throw new DefinedException(new DefaultErrorCode("400", "请先同意信息用于本次沟通"));
        }

        SiteLead lead = new SiteLead();
        lead.setName(name.trim());
        lead.setContact(contact.trim());
        lead.setTopic(painPoint.trim());
        lead.setNote(buildNote());
        lead.setPageUrl(buildAttribution());
        lead.setSource("douyin");
        lead.setStatus(SiteLeadStatus.New);
        lead.setConsent(Boolean.TRUE);
        return repository.save(lead);
    }

    /** note 汇总企业类型与归因，受 SiteLead.note 长度约束（500）。 */
    private String buildNote() {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(enterpriseType)) {
            parts.add("企业类型：" + enterpriseType.trim());
        }
        if (StringUtils.hasText(sourceScene)) {
            parts.add("来源场景：" + sourceScene.trim());
        }
        String joined = String.join("；", parts);
        return joined.isEmpty() ? null : truncate(joined, 500);
    }

    /** 归因信息暂存 pageUrl，待 DEC-031 结构化字段落地后迁移。 */
    private String buildAttribution() {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(contentId)) parts.add("c=" + contentId.trim());
        if (StringUtils.hasText(videoId)) parts.add("v=" + videoId.trim());
        if (StringUtils.hasText(anchorId)) parts.add("a=" + anchorId.trim());
        if (parts.isEmpty()) return null;
        return truncate("douyin://" + String.join("&", parts), 500);
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}

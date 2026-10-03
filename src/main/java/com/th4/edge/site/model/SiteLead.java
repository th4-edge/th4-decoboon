package com.th4.edge.site.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import com.th4.edge.site.configure.SiteLeadStatus;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** 官网公开预约线索，不归属租户企业。 */
@Note("官网预约线索")
@Description("公开官网提交的预约意向，仅在进入客户经营主链前用于线索跟进")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_site_lead", indexes = {
        @Index(name = "idx_site_lead_status_time", columnList = "status, create_date_time, lead_id")
})
public class SiteLead extends AbstractModel {

    @Note("线索索引") @Id @SnowflakeId private Long leadId;
    @Note("称呼") @Column(name = "contact_name", length = 80, nullable = false) private String name;
    @Note("联系方式") @Column(length = 120, nullable = false) private String contact;
    @Note("关心的问题") @Column(length = 80, nullable = false) private String topic;
    @Note("现场补充") @Column(length = 500) private String note;
    @Note("来源页面") @Column(name = "page_url", length = 500) private String pageUrl;
    @Note("来源渠道") @Column(length = 32, nullable = false) private String source = "website";
    @Note("线索状态") @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) private SiteLeadStatus status = SiteLeadStatus.New;
    @Note("同意沟通") @Column(nullable = false) private Boolean consent = Boolean.TRUE;
    @Note("提交时间")
    @CreateTime
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "dateTime", column = @Column(name = "create_date_time")),
            @AttributeOverride(name = "date", column = @Column(name = "create_date")),
            @AttributeOverride(name = "time", column = @Column(name = "create_time")),
            @AttributeOverride(name = "year", column = @Column(name = "create_year")),
            @AttributeOverride(name = "month", column = @Column(name = "create_month")),
            @AttributeOverride(name = "day", column = @Column(name = "create_day")),
            @AttributeOverride(name = "hour", column = @Column(name = "create_hour")),
            @AttributeOverride(name = "minute", column = @Column(name = "create_minute")),
            @AttributeOverride(name = "second", column = @Column(name = "create_second")),
            @AttributeOverride(name = "week", column = @Column(name = "create_week"))
    })
    private DateTime createTime;

    public Long getLeadId() { return leadId; }
    public void setLeadId(Long leadId) { this.leadId = leadId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String pageUrl) { this.pageUrl = pageUrl; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public SiteLeadStatus getStatus() { return status; }
    public void setStatus(SiteLeadStatus status) { this.status = status; }
    public Boolean getConsent() { return consent; }
    public void setConsent(Boolean consent) { this.consent = consent; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
}

package com.th4.edge.platform.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 平台企业入驻服务记录，租户企业仅保存标量快照。 */
@Note("企业入驻服务记录")
@Description("平台运营内部服务记录，不写回租户业务状态，不建立租户企业外键")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_platform_onboarding_service", indexes = {
        @Index(name = "idx_platform_onboarding_company_time", columnList = "org_id, target_com_id, create_date_time, service_id"),
        @Index(name = "idx_platform_onboarding_operator_time", columnList = "org_id, user_id, create_date_time")
})
public class PlatformOnboardingServiceRecord extends AbstractModel {
    @Note("服务记录ID") @Id @SnowflakeId private Long serviceId;
    @Note("平台组织") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "org_id", nullable = false) private PlatformOrg org;
    @Note("操作人") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private PlatformUser operator;
    @Note("企业ID快照") @Column(name = "target_com_id", nullable = false) private Long targetComId;
    @Note("企业名称快照") @Column(name = "target_company_title", length = 128, nullable = false) private String targetCompanyTitle;
    @Note("服务状态") @Enumerated(EnumType.STRING) @Column(name = "service_status", length = 24, nullable = false) private PlatformOnboardingServiceStatus status;
    @Note("沟通结果") @Column(name = "contact_result", length = 500) private String contactResult;
    @Note("下一步") @Column(name = "next_action", length = 500, nullable = false) private String nextAction;
    @Note("下次跟进时间") @Column(name = "next_follow_at") private LocalDateTime nextFollowAt;
    @Note("备注") @Column(length = 1000) private String remark;
    @Note("记录时间")
    @CreateTime @Embedded
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

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
    public PlatformOrg getOrg() { return org; }
    public void setOrg(PlatformOrg org) { this.org = org; }
    public PlatformUser getOperator() { return operator; }
    public void setOperator(PlatformUser operator) { this.operator = operator; }
    public Long getTargetComId() { return targetComId; }
    public void setTargetComId(Long targetComId) { this.targetComId = targetComId; }
    public String getTargetCompanyTitle() { return targetCompanyTitle; }
    public void setTargetCompanyTitle(String targetCompanyTitle) { this.targetCompanyTitle = targetCompanyTitle; }
    public PlatformOnboardingServiceStatus getStatus() { return status; }
    public void setStatus(PlatformOnboardingServiceStatus status) { this.status = status; }
    public String getContactResult() { return contactResult; }
    public void setContactResult(String contactResult) { this.contactResult = contactResult; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public LocalDateTime getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(LocalDateTime nextFollowAt) { this.nextFollowAt = nextFollowAt; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
}

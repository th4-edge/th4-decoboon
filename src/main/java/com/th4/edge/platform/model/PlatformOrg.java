package com.th4.edge.platform.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Description;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.date.model.DateTime;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.framework.data.model.AbstractModel;
import com.th4.edge.framework.data.model.prepare.annotation.CreateTime;
import jakarta.persistence.*;

/** 独立平台运营组织。 */
@Note("平台运营组织")
@Description("平台运营团队自身的组织主体，不关联任何租户 Company")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_platform_org", uniqueConstraints = {
        @UniqueConstraint(name = "uk_platform_org_code", columnNames = "code")
})
public class PlatformOrg extends AbstractModel {
    @Note("组织索引") @Id @SnowflakeId private Long orgId;
    @Note("组织编码") @Keyword @Column(length = 32, nullable = false) private String code;
    @Note("组织名称") @Keyword @Column(length = 128, nullable = false) private String name;
    @Note("状态") @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) private PlatformOrgStatus status = PlatformOrgStatus.Active;
    @Note("创建时间")
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

    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public PlatformOrgStatus getStatus() { return status; }
    public void setStatus(PlatformOrgStatus status) { this.status = status; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
}

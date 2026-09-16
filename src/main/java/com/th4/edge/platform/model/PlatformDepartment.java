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

/** 平台组织部门。 */
@Note("平台组织部门")
@Description("平台运营组织的部门树，不关联任何租户门店、团队或员工")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_platform_department", uniqueConstraints = {
        @UniqueConstraint(name = "uk_platform_dept_org_code", columnNames = {"org_id", "code"})
}, indexes = {
        @Index(name = "idx_platform_dept_org", columnList = "org_id, dept_id"),
        @Index(name = "idx_platform_dept_parent", columnList = "org_id, parent_id, dept_id")
})
public class PlatformDepartment extends AbstractModel {
    @Note("部门索引") @Id @SnowflakeId private Long deptId;
    @Note("所属平台组织") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "org_id", nullable = false) private PlatformOrg org;
    @Note("上级部门") @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "parent_id") private PlatformDepartment parent;
    @Note("部门编码") @Keyword @Column(length = 32, nullable = false) private String code;
    @Note("部门名称") @Keyword @Column(length = 64, nullable = false) private String name;
    @Note("排序") @Column(nullable = false) private Integer sort = 0;
    @Note("状态") @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false) private PlatformDepartmentStatus status = PlatformDepartmentStatus.Active;
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

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public PlatformOrg getOrg() { return org; }
    public void setOrg(PlatformOrg org) { this.org = org; }
    public PlatformDepartment getParent() { return parent; }
    public void setParent(PlatformDepartment parent) { this.parent = parent; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public PlatformDepartmentStatus getStatus() { return status; }
    public void setStatus(PlatformDepartmentStatus status) { this.status = status; }
    public DateTime getCreateTime() { return createTime; }
    public void setCreateTime(DateTime createTime) { this.createTime = createTime; }
}

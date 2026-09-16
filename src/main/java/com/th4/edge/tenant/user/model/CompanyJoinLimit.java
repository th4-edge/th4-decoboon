package com.th4.edge.tenant.user.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.model.AbstractModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/** 用户加入企业冷却、频率和黑名单限制。 */
@Note("企业加入限制")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_company_join_limit", indexes = {
        @Index(name = "idx_join_limit_user", columnList = "user_id", unique = true),
        @Index(name = "idx_join_limit_phone", columnList = "phone", unique = true)
})
public class CompanyJoinLimit extends AbstractModel {

    @Note("限制索引")
    @Id
    @SnowflakeId
    private Long limitId;

    @Note("平台用户")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Credentials user;

    @Note("手机号")
    @Column(length = 20, nullable = false, unique = true)
    private String phone;

    @Note("最近加入时间")
    @Column(name = "last_join_at")
    private LocalDateTime lastJoinAt;

    @Note("最近退出时间")
    @Column(name = "last_resign_at")
    private LocalDateTime lastResignAt;

    @Note("180 天加入次数")
    @Column(name = "join_count_180d", nullable = false)
    private Integer joinCount180d = 0;

    @Note("最近接受邀请时间")
    @Column(name = "last_invite_accept_at")
    private LocalDateTime lastInviteAcceptAt;

    @Note("黑名单截止时间")
    @Column(name = "blacklist_until")
    private LocalDateTime blacklistUntil;

    @Version
    private Long version;

    public Long getLimitId() { return limitId; }
    public void setLimitId(Long limitId) { this.limitId = limitId; }
    public Credentials getUser() { return user; }
    public void setUser(Credentials user) { this.user = user; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDateTime getLastJoinAt() { return lastJoinAt; }
    public void setLastJoinAt(LocalDateTime lastJoinAt) { this.lastJoinAt = lastJoinAt; }
    public LocalDateTime getLastResignAt() { return lastResignAt; }
    public void setLastResignAt(LocalDateTime lastResignAt) { this.lastResignAt = lastResignAt; }
    public Integer getJoinCount180d() { return joinCount180d; }
    public void setJoinCount180d(Integer joinCount180d) { this.joinCount180d = joinCount180d; }
    public LocalDateTime getLastInviteAcceptAt() { return lastInviteAcceptAt; }
    public void setLastInviteAcceptAt(LocalDateTime lastInviteAcceptAt) { this.lastInviteAcceptAt = lastInviteAcceptAt; }
    public LocalDateTime getBlacklistUntil() { return blacklistUntil; }
    public void setBlacklistUntil(LocalDateTime blacklistUntil) { this.blacklistUntil = blacklistUntil; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}

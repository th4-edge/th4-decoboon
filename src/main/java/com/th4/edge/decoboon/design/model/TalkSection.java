package com.th4.edge.decoboon.design.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.design.configure.TalkSectionType;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.tenant.user.access.annotation.DataPermission;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import com.th4.edge.tenant.user.model.Team;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

/** 谈单 PPT 的独立页面内容。 */
@Note("谈单页")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_design_talk_section", indexes = {
        @Index(name = "idx_talk_section_company", columnList = "com_id, section_id"),
        @Index(name = "idx_talk_section_company_store", columnList = "com_id, store_id, section_id"),
        @Index(name = "idx_talk_section_company_team", columnList = "com_id, team_id, section_id"),
        @Index(name = "idx_talk_section_company_owner", columnList = "com_id, emp_id, section_id"),
        @Index(name = "idx_talk_section_company_talk", columnList = "com_id, talk_id, section_id")
})
public class TalkSection extends AuditModel {

    @Note("页面索引")
    @Id
    @SnowflakeId
    private Long sectionId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("负责设计师")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    private Employee owner;

    @Note("所属谈单")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "talk_id", nullable = false)
    private Talk talk;

    @Note("页型")
    @Enumerated(EnumType.STRING)
    @Column(length = 24, nullable = false)
    private TalkSectionType type;

    @Note("页面标题")
    @Column(length = 128, nullable = false)
    private String title;

    @Note("页面副标题")
    @Column(length = 256)
    private String subtitle;

    @Note("页面正文")
    @Lob
    private String content;

    @Note("页面图片")
    @ElementCollection
    @CollectionTable(name = "db_decoboon_design_talk_section_images", joinColumns = @JoinColumn(name = "section_id"))
    @Column(name = "url", length = 512)
    private List<String> images;

    @Note("排序")
    @Column(nullable = false)
    private Integer sort = 0;

    @Note("是否显示")
    @Column(nullable = false)
    private Boolean visible = Boolean.TRUE;

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public Employee getOwner() { return owner; }
    public void setOwner(Employee owner) { this.owner = owner; }
    public Talk getTalk() { return talk; }
    public void setTalk(Talk talk) { this.talk = talk; }
    public TalkSectionType getType() { return type; }
    public void setType(TalkSectionType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Integer getSort() { return sort; }
    public void setSort(Integer sort) { this.sort = sort; }
    public Boolean getVisible() { return visible; }
    public void setVisible(Boolean visible) { this.visible = visible; }
}

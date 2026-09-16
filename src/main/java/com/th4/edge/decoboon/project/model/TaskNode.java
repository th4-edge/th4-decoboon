package com.th4.edge.decoboon.project.model;

import jakarta.persistence.EntityListeners;
import com.th4.edge.components.data.listener.SnowflakeEntityListener;

import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;

import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.project.configure.TaskNodeKey;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.List;

/** 施工任务节点留证。 */
@Note("施工任务节点")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@DataPermission
@Table(name = "db_decoboon_task_node", indexes = {
        @Index(name = "idx_task_node_company", columnList = "com_id, node_id"),
        @Index(name = "idx_task_node_company_task", columnList = "com_id, task_id, node_id")
,
        @Index(name = "idx_task_node_company_store", columnList = "com_id, store_id, node_id"),
        @Index(name = "idx_task_node_company_team", columnList = "com_id, team_id, node_id"),
        @Index(name = "idx_task_node_company_owner", columnList = "com_id, owner_id, node_id")
})
public class TaskNode extends AuditModel implements ProjectOwned {

    @Id
    @SnowflakeId
    @Note("节点索引")
    private Long nodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    @Note("所属企业")
    private Company company;

    @Note("所属门店")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @Note("所属团队")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Note("经营负责人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Employee owner;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    @Note("施工任务")
    private ProjectTask task;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    @Note("节点类型")
    private TaskNodeKey nodeKey;

    @Column(length = 512)
    @Note("节点说明")
    private String note;

    @ElementCollection
    @CollectionTable(name = "db_decoboon_task_node_media", joinColumns = @JoinColumn(name = "node_id"))
    @Column(name = "url", length = 512)
    @Note("影像证据")
    private List<String> medias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emp_id", nullable = false)
    @Note("确认人")
    private Employee confirmedBy;

    public Long getNodeId() { return nodeId; }
    public void setNodeId(Long nodeId) { this.nodeId = nodeId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public ProjectTask getTask() { return task; }
    public void setTask(ProjectTask task) { this.task = task; }
    public TaskNodeKey getNodeKey() { return nodeKey; }
    public void setNodeKey(TaskNodeKey nodeKey) { this.nodeKey = nodeKey; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public List<String> getMedias() { return medias; }
    public void setMedias(List<String> medias) { this.medias = medias; }
    public Employee getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Employee confirmedBy) { this.confirmedBy = confirmedBy; }
    @Override
    public Store getStore() { return store; }

    @Override
    public void setStore(Store store) { this.store = store; }

    @Override
    public Team getTeam() { return team; }

    @Override
    public void setTeam(Team team) { this.team = team; }

    @Override
    public Employee getOwner() { return owner; }

    @Override
    public void setOwner(Employee owner) { this.owner = owner; }


}

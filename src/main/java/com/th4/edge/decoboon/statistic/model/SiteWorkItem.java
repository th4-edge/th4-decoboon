package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.project.configure.ProjectRiskLevel;
import com.th4.edge.decoboon.statistic.configure.WorkItemDomain;
import com.th4.edge.decoboon.statistic.configure.WorkbenchBucket;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工作台工作项（只读投影）。
 *
 * <p>一个工作项描述「一件事」，而不是「一条数据」。它是工作台的唯一渲染单元：
 * 卡片必须能一句话说清当前对象、当前状态、为什么在这里、下一步做什么。</p>
 *
 * <p>契约：</p>
 * <ul>
 *   <li>{@code reason} 必填，禁止空文案（没有原因的卡片不允许出现在工作台）；</li>
 *   <li>{@code nextActionLabel} 必填且唯一，次要操作降级为卡片次入口；</li>
 *   <li>派生状态与风险由服务端计算，前端不得二次推导业务状态；</li>
 *   <li>成本口径不完整时，利润类金额必须为空，禁止用 0 冒充。</li>
 * </ul>
 */
@Note("工作台工作项")
public class SiteWorkItem {

    @Note("稳定唯一键")
    private String itemKey;

    @Note("业务域")
    private WorkItemDomain domain;

    @Note("域内阶段标识")
    private String stage;

    @Note("阶段中文名")
    private String stageName;

    @Note("业务对象类型")
    private String subjectType;

    @Note("业务对象主键")
    private Long subjectId;

    @Note("所属项目索引：原地操作抽屉必须据此定位上下文，禁止退回列表取第一条")
    private Long projectId;

    @Note("主标题")
    private String title;

    @Note("次标题")
    private String subtitle;

    @Note("当前责任人；无责任人时服务端统一返回「未分配」")
    private String ownerName;

    @Note("派生状态（服务端计算）")
    private String derivedState;

    @Note("风险级别")
    private ProjectRiskLevel riskLevel = ProjectRiskLevel.Normal;

    @Note("在当前阶段停留天数")
    private Long stayDays;

    @Note("为什么出现在这里（阻塞/风险原因）")
    private String reason;

    @Note("下一步动作文案")
    private String nextActionLabel;

    @Note("下一步动作类型：OpenWorkbench/OpenDrawer/OpenList/None")
    private String nextActionType;

    @Note("下一步动作目标")
    private String nextActionTarget;

    @Note("到期/预约/承诺时间")
    private LocalDate dueAt;

    @Unit("元")
    @Note("相关金额；成本不完整时为空")
    private BigDecimal amount;

    @Note("归属栏位")
    private WorkbenchBucket bucket;

    @Note("补充事实")
    private Map<String, String> metrics = new LinkedHashMap<>();

    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }
    public WorkItemDomain getDomain() { return domain; }
    public void setDomain(WorkItemDomain domain) { this.domain = domain; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public String getSubjectType() { return subjectType; }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getDerivedState() { return derivedState; }
    public void setDerivedState(String derivedState) { this.derivedState = derivedState; }
    public ProjectRiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(ProjectRiskLevel riskLevel) { this.riskLevel = riskLevel; }
    public Long getStayDays() { return stayDays; }
    public void setStayDays(Long stayDays) { this.stayDays = stayDays; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNextActionLabel() { return nextActionLabel; }
    public void setNextActionLabel(String nextActionLabel) { this.nextActionLabel = nextActionLabel; }
    public String getNextActionType() { return nextActionType; }
    public void setNextActionType(String nextActionType) { this.nextActionType = nextActionType; }
    public String getNextActionTarget() { return nextActionTarget; }
    public void setNextActionTarget(String nextActionTarget) { this.nextActionTarget = nextActionTarget; }
    public LocalDate getDueAt() { return dueAt; }
    public void setDueAt(LocalDate dueAt) { this.dueAt = dueAt; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public WorkbenchBucket getBucket() { return bucket; }
    public void setBucket(WorkbenchBucket bucket) { this.bucket = bucket; }
    public Map<String, String> getMetrics() { return metrics; }
    public void setMetrics(Map<String, String> metrics) {
        this.metrics = metrics == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metrics);
    }
}

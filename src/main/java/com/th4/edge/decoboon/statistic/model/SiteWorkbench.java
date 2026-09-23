package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.statistic.configure.WorkbenchBucket;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.core.annotation.Unit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目工地工作台聚合（只读投影）。
 *
 * <p>业务横切面：把计划、任务、材料、验收、整改、变更与回款风险，
 * 在同一时点按「谁此刻要做什么」重新组织，而不是按实体表罗列。</p>
 *
 * <p>本模型只读：不新增金额口径、不改状态机、不写任何业务事实。
 * 风险与逾期判定沿用 {@code ProjectOverviewContext}/{@code PmReport} 的既有口径。</p>
 */
@Note("项目工地工作台")
public class SiteWorkbench {

    @Note("统计基准日")
    private LocalDate asOf;

    @Note("顶部指标")
    private List<Metric> metrics = new ArrayList<>();

    @Note("三栏计数")
    private List<BucketCount> bucketCounts = new ArrayList<>();

    @Note("全部工作项（已按业务优先级排序）")
    private List<SiteWorkItem> items = new ArrayList<>();

    @Note("按项目经理分组")
    private List<OwnerGroup> ownerGroups = new ArrayList<>();

    @Note("编排窗口起始日")
    private LocalDate planningStart;

    @Note("编排窗口结束日")
    private LocalDate planningEnd;

    @Note("编排视图：跨工地按天排布的关键节点")
    private List<PlanningDay> planningDays = new ArrayList<>();

    public LocalDate getAsOf() { return asOf; }
    public void setAsOf(LocalDate asOf) { this.asOf = asOf; }
    public List<Metric> getMetrics() { return metrics; }
    public void setMetrics(List<Metric> metrics) { this.metrics = metrics == null ? new ArrayList<>() : new ArrayList<>(metrics); }
    public List<BucketCount> getBucketCounts() { return bucketCounts; }
    public void setBucketCounts(List<BucketCount> bucketCounts) { this.bucketCounts = bucketCounts == null ? new ArrayList<>() : new ArrayList<>(bucketCounts); }
    public List<SiteWorkItem> getItems() { return items; }
    public void setItems(List<SiteWorkItem> items) { this.items = items == null ? new ArrayList<>() : new ArrayList<>(items); }
    public List<OwnerGroup> getOwnerGroups() { return ownerGroups; }
    public void setOwnerGroups(List<OwnerGroup> ownerGroups) { this.ownerGroups = ownerGroups == null ? new ArrayList<>() : new ArrayList<>(ownerGroups); }
    public LocalDate getPlanningStart() { return planningStart; }
    public void setPlanningStart(LocalDate planningStart) { this.planningStart = planningStart; }
    public LocalDate getPlanningEnd() { return planningEnd; }
    public void setPlanningEnd(LocalDate planningEnd) { this.planningEnd = planningEnd; }
    public List<PlanningDay> getPlanningDays() { return planningDays; }
    public void setPlanningDays(List<PlanningDay> planningDays) { this.planningDays = planningDays == null ? new ArrayList<>() : new ArrayList<>(planningDays); }

    /** 顶部指标：指标卡只表达事实，不承担主行动。 */
    @Note("工地工作台指标")
    public static class Metric {
        @Note("指标标识") private String key;
        @Note("指标名称") private String label;
        @Note("指标数量") private Integer count = 0;
        @Note("口径说明") private String hint;
        @Note("语气：brand/success/warning/danger/purple/info") private String tone = "brand";

        public Metric() { }

        public Metric(String key, String label, Integer count, String hint, String tone) {
            this.key = key;
            this.label = label;
            this.count = count;
            this.hint = hint;
            this.tone = tone;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public String getHint() { return hint; }
        public void setHint(String hint) { this.hint = hint; }
        public String getTone() { return tone; }
        public void setTone(String tone) { this.tone = tone; }
    }

    /** 三栏计数：由服务端返回，前端不得用当前页长度当计数。 */
    @Note("工作台栏位计数")
    public static class BucketCount {
        @Note("栏位") private WorkbenchBucket bucket;
        @Note("栏位名称") private String label;
        @Note("数量") private Integer count = 0;

        public BucketCount() { }

        public BucketCount(WorkbenchBucket bucket, String label, Integer count) {
            this.bucket = bucket;
            this.label = label;
            this.count = count;
        }

        public WorkbenchBucket getBucket() { return bucket; }
        public void setBucket(WorkbenchBucket bucket) { this.bucket = bucket; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }

    /** 责任人分组：让负责人先看「谁名下的活最多」，对应 App 报价列表的设计师分组。 */
    @Note("责任人分组")
    public static class OwnerGroup {
        @Note("责任人索引；未分配时为空") private Long ownerId;
        @Note("责任人名称") private String ownerName;
        @Note("工作项数量") private Integer count = 0;
        @Note("其中需要处理数量") private Integer needActionCount = 0;
        @Note("其中风险数量") private Integer riskCount = 0;

        public Long getOwnerId() { return ownerId; }
        public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Integer getNeedActionCount() { return needActionCount; }
        public void setNeedActionCount(Integer needActionCount) { this.needActionCount = needActionCount; }
        public Integer getRiskCount() { return riskCount; }
        public void setRiskCount(Integer riskCount) { this.riskCount = riskCount; }
    }

    /**
     * 编排视图的一天。
     *
     * <p>负载口径：同一天内落在该员工数据范围内的关键节点数。超过阈值只做「密集」提示，
     * 不自动改期、不评判产能——产能与工人配伍口径尚未裁决（见 GAP-WB-005）。</p>
     */
    @Note("编排视图的一天")
    public static class PlanningDay {
        @Note("日期") private LocalDate date;
        @Note("星期文案") private String weekday;
        @Note("是否今天") private Boolean today = Boolean.FALSE;
        @Note("关键节点数") private Integer count = 0;
        @Note("是否密集") private Boolean dense = Boolean.FALSE;
        @Note("密集提示文案") private String denseHint;
        @Note("当天节点") private List<PlanningItem> items = new ArrayList<>();

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getWeekday() { return weekday; }
        public void setWeekday(String weekday) { this.weekday = weekday; }
        public Boolean getToday() { return today; }
        public void setToday(Boolean today) { this.today = today; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public Boolean getDense() { return dense; }
        public void setDense(Boolean dense) { this.dense = dense; }
        public String getDenseHint() { return denseHint; }
        public void setDenseHint(String denseHint) { this.denseHint = denseHint; }
        public List<PlanningItem> getItems() { return items; }
        public void setItems(List<PlanningItem> items) { this.items = items == null ? new ArrayList<>() : new ArrayList<>(items); }
    }

    /**
     * 编排视图的一个关键节点。
     *
     * <p>只投影已存在的计划事实（任务计划开工、材料要求到场、整改截止、应收到期），
     * 不推算新日期、不新增计划口径。</p>
     */
    @Note("编排节点")
    public static class PlanningItem {
        @Note("业务对象类型") private String subjectType;
        @Note("业务对象主键") private Long subjectId;
        @Note("所属项目索引") private Long projectId;
        @Note("项目名称") private String projectName;
        @Note("节点类型：TaskStart/MaterialArrive/RectifyDue/ReceivableDue") private String kind;
        @Note("节点名称") private String title;
        @Note("责任人") private String ownerName;
        @Note("计划日（取自既有字段，不做推算）") private LocalDate plannedDate;
        @Note("是否已到期未处理") private Boolean overdue = Boolean.FALSE;
        @Note("是否可改期") private Boolean reschedulable = Boolean.FALSE;
        @Note("原地动作文案") private String nextActionLabel;

        public String getSubjectType() { return subjectType; }
        public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
        public Long getSubjectId() { return subjectId; }
        public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getOwnerName() { return ownerName; }
        public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
        public LocalDate getPlannedDate() { return plannedDate; }
        public void setPlannedDate(LocalDate plannedDate) { this.plannedDate = plannedDate; }
        public Boolean getOverdue() { return overdue; }
        public void setOverdue(Boolean overdue) { this.overdue = overdue; }
        public Boolean getReschedulable() { return reschedulable; }
        public void setReschedulable(Boolean reschedulable) { this.reschedulable = reschedulable; }
        public String getNextActionLabel() { return nextActionLabel; }
        public void setNextActionLabel(String nextActionLabel) { this.nextActionLabel = nextActionLabel; }
    }
}

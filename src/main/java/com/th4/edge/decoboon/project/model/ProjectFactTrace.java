package com.th4.edge.decoboon.project.model;

import com.th4.edge.decoboon.common.configure.FactStage;
import com.th4.edge.decoboon.common.configure.FactType;
import com.th4.edge.decoboon.common.model.BusinessFactNode;
import com.th4.edge.decoboon.project.configure.ProjectFactViewMode;
import com.th4.edge.framework.core.annotation.Note;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目事实追踪的一期只读投影。
 *
 * <p>项目是事实索引根，节点只引用来源对象和版本，不复制来源对象的完整明细。</p>
 */
@Note("项目事实追踪")
public class ProjectFactTrace {

    private Long projectId;
    private String projectNo;
    private String projectName;
    private ProjectFactViewMode defaultViewMode = ProjectFactViewMode.PHASE;
    private LocalDateTime asOf = LocalDateTime.now();
    private List<BusinessFactNode> nodes = new ArrayList<>();
    private List<SourceGap> sourceGaps = new ArrayList<>();

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProjectNo() { return projectNo; }
    public void setProjectNo(String projectNo) { this.projectNo = projectNo; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public ProjectFactViewMode getDefaultViewMode() { return defaultViewMode; }
    public void setDefaultViewMode(ProjectFactViewMode defaultViewMode) { this.defaultViewMode = defaultViewMode; }
    public LocalDateTime getAsOf() { return asOf; }
    public void setAsOf(LocalDateTime asOf) { this.asOf = asOf; }
    public List<BusinessFactNode> getNodes() { return nodes; }
    public void setNodes(List<BusinessFactNode> nodes) { this.nodes = nodes; }
    public List<SourceGap> getSourceGaps() { return sourceGaps; }
    public void setSourceGaps(List<SourceGap> sourceGaps) { this.sourceGaps = sourceGaps; }

    /** 缺失、未发生或暂不可确认的来源说明；不得由前端猜测补齐。 */
    @Note("项目事实来源缺口")
    public static class SourceGap {
        private String nodeKey;
        private FactStage stage;
        private FactType factType;
        private String label;
        private String reason;
        private Boolean required = false;

        public String getNodeKey() { return nodeKey; }
        public void setNodeKey(String nodeKey) { this.nodeKey = nodeKey; }
        public FactStage getStage() { return stage; }
        public void setStage(FactStage stage) { this.stage = stage; }
        public FactType getFactType() { return factType; }
        public void setFactType(FactType factType) { this.factType = factType; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public Boolean getRequired() { return required; }
        public void setRequired(Boolean required) { this.required = required; }
    }
}

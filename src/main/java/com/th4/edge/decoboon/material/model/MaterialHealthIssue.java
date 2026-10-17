package com.th4.edge.decoboon.material.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.ArrayList;
import java.util.List;

/** 材料业务体检校核项。 */
@Note("材料业务体检校核项")
public class MaterialHealthIssue {

    @Note("校核标识")
    private String id;
    @Note("校核名称")
    private String label;
    @Note("严重级别")
    private String severity;
    @Note("数量")
    private int count;
    @Note("下一动作")
    private String nextAction;
    @Note("证据说明")
    private String evidence;
    @Note("业务来源")
    private List<String> refs = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public List<String> getRefs() { return refs; }
    public void setRefs(List<String> refs) { this.refs = refs; }
}

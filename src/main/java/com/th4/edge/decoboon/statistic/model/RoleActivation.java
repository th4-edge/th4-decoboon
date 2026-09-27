package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.framework.core.annotation.Note;

/** 岗位业务激活投影。 */
@Note("岗位业务激活")
public class RoleActivation {

    private String role;
    private String label;
    private int configuredCount;
    private int activeCount;
    private boolean activated;
    private String evidence;

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getConfiguredCount() { return configuredCount; }
    public void setConfiguredCount(int configuredCount) { this.configuredCount = configuredCount; }
    public int getActiveCount() { return activeCount; }
    public void setActiveCount(int activeCount) { this.activeCount = activeCount; }
    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
}

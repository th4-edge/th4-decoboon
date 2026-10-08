package com.th4.edge.decoboon.common.model;

import com.th4.edge.framework.core.annotation.Note;

import java.util.List;

/** 服务端状态视图中的下一步动作。 */
@Note("业务下一步动作")
public class BusinessNextAction {
    private String key;
    private String label;
    private String hint;
    private String ownerRole;
    private String fromStatus;
    private String toStatus;
    private Boolean enabled = Boolean.TRUE;
    private List<String> blockedReasons;
    private List<String> missingFields;
    private List<String> reloadScopes;
    private List<String> businessRefs;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getHint() { return hint; }
    public void setHint(String hint) { this.hint = hint; }
    public String getOwnerRole() { return ownerRole; }
    public void setOwnerRole(String ownerRole) { this.ownerRole = ownerRole; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public List<String> getBlockedReasons() { return blockedReasons; }
    public void setBlockedReasons(List<String> blockedReasons) { this.blockedReasons = blockedReasons; }
    public List<String> getMissingFields() { return missingFields; }
    public void setMissingFields(List<String> missingFields) { this.missingFields = missingFields; }
    public List<String> getReloadScopes() { return reloadScopes; }
    public void setReloadScopes(List<String> reloadScopes) { this.reloadScopes = reloadScopes; }
    public List<String> getBusinessRefs() { return businessRefs; }
    public void setBusinessRefs(List<String> businessRefs) { this.businessRefs = businessRefs; }
}

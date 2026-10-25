package com.th4.edge.decoboon.standardasset.model;

import com.th4.edge.framework.core.annotation.Note;

/** 标准资产状态契约只读投影。 */
@Note("标准资产状态契约")
public class StandardAssetStatusContract {
    private String key;
    private String label;
    private Boolean editable = Boolean.FALSE;
    private Boolean newBusinessEligible = Boolean.FALSE;
    private Boolean historyReadable = Boolean.TRUE;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Boolean getEditable() { return editable; }
    public void setEditable(Boolean editable) { this.editable = editable; }
    public Boolean getNewBusinessEligible() { return newBusinessEligible; }
    public void setNewBusinessEligible(Boolean newBusinessEligible) { this.newBusinessEligible = newBusinessEligible; }
    public Boolean getHistoryReadable() { return historyReadable; }
    public void setHistoryReadable(Boolean historyReadable) { this.historyReadable = historyReadable; }
}

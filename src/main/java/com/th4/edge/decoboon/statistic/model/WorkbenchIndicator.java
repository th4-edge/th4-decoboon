package com.th4.edge.decoboon.statistic.model;

import com.th4.edge.decoboon.statistic.configure.WorkbenchSignalStatus;
import com.th4.edge.framework.core.annotation.Note;

import java.math.BigDecimal;

/** 企业负责人首页四联经营指示灯。 */
@Note("企业负责人经营指示灯")
public class WorkbenchIndicator {

    private String key;
    private String label;
    private BigDecimal value;
    private String valueType;
    private WorkbenchSignalStatus status = WorkbenchSignalStatus.Unknown;
    private String statusText;
    private String detail;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public WorkbenchSignalStatus getStatus() { return status; }
    public void setStatus(WorkbenchSignalStatus status) { this.status = status; }
    public String getStatusText() { return statusText; }
    public void setStatusText(String statusText) { this.statusText = statusText; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}

package com.th4.edge.decoboon.standardasset.model;

import com.th4.edge.decoboon.standardasset.configure.StandardAssetDefaultPolicy;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetSourceMode;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetType;
import com.th4.edge.decoboon.standardasset.configure.StandardAssetVersionMode;
import com.th4.edge.framework.core.annotation.Note;

import java.util.List;

/** 企业运营标准资产生命周期契约只读投影。 */
@Note("企业运营标准资产生命周期契约")
public class StandardAssetLifecycleContract {
    private StandardAssetType assetType;
    private String productName;
    private List<StandardAssetStatusContract> statuses = List.of();
    private StandardAssetVersionMode versionMode;
    private StandardAssetSourceMode sourceMode;
    private StandardAssetDefaultPolicy defaultPolicy;
    private List<String> referenceFields = List.of();
    private List<String> snapshotFields = List.of();
    private String historyPolicy;

    public StandardAssetType getAssetType() { return assetType; }
    public void setAssetType(StandardAssetType assetType) { this.assetType = assetType; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public List<StandardAssetStatusContract> getStatuses() { return statuses; }
    public void setStatuses(List<StandardAssetStatusContract> statuses) {
        this.statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }
    public StandardAssetVersionMode getVersionMode() { return versionMode; }
    public void setVersionMode(StandardAssetVersionMode versionMode) { this.versionMode = versionMode; }
    public StandardAssetSourceMode getSourceMode() { return sourceMode; }
    public void setSourceMode(StandardAssetSourceMode sourceMode) { this.sourceMode = sourceMode; }
    public StandardAssetDefaultPolicy getDefaultPolicy() { return defaultPolicy; }
    public void setDefaultPolicy(StandardAssetDefaultPolicy defaultPolicy) { this.defaultPolicy = defaultPolicy; }
    public List<String> getReferenceFields() { return referenceFields; }
    public void setReferenceFields(List<String> referenceFields) {
        this.referenceFields = referenceFields == null ? List.of() : List.copyOf(referenceFields);
    }
    public List<String> getSnapshotFields() { return snapshotFields; }
    public void setSnapshotFields(List<String> snapshotFields) {
        this.snapshotFields = snapshotFields == null ? List.of() : List.copyOf(snapshotFields);
    }
    public String getHistoryPolicy() { return historyPolicy; }
    public void setHistoryPolicy(String historyPolicy) { this.historyPolicy = historyPolicy; }
}

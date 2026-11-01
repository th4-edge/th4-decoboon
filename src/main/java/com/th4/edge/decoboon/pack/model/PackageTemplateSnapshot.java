package com.th4.edge.decoboon.pack.model;

import com.th4.edge.framework.core.annotation.Note;

/** 套餐发布快照内容。 */
@Note("套餐发布快照")
public class PackageTemplateSnapshot {
    private String json;
    private String hash;

    public String getJson() { return json; }
    public void setJson(String json) { this.json = json; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}

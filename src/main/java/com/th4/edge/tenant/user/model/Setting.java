package com.th4.edge.tenant.user.model;

import com.th4.edge.framework.core.annotation.Note;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Note("系统设置")
@Embeddable
public class Setting {


    @Note("启用多门店")
    @Column
    private Boolean enableStore=false;

    public Boolean getEnableStore() {
        return enableStore;
    }

    public void setEnableStore(Boolean enableStore) {
        this.enableStore = enableStore;
    }
}

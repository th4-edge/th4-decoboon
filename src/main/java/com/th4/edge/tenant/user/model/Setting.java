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

    @Note("客户分级配置，格式 high=200000;standard=100000;a=75;b=50;wDemand=30;wBudget=25;wUrgency=20;wEngagement=15;wRecency=10")
    @Column(name = "grade_config", length = 512)
    private String gradeConfig;


    public Boolean getEnableStore() {
        return enableStore;
    }

    public void setEnableStore(Boolean enableStore) {
        this.enableStore = enableStore;
    }

    public String getGradeConfig() {
        return gradeConfig;
    }

    public void setGradeConfig(String gradeConfig) {
        this.gradeConfig = gradeConfig;
    }
}

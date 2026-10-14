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

    @Note("叙事主品牌：Company=装企（默认）/ Platform=平台")
    @Column(name = "brand_primary", length = 16)
    private String brandPrimary = "Company";

    @Note("是否显示「记录存证与保管 · 装得清」署名，默认关闭")
    @Column(name = "custodian_credit")
    private Boolean custodianCredit = false;


    public Boolean getEnableStore() {
        return enableStore;
    }

    public void setEnableStore(Boolean enableStore) {
        this.enableStore = enableStore;
    }

    public String getBrandPrimary() {
        return brandPrimary;
    }

    public void setBrandPrimary(String brandPrimary) {
        this.brandPrimary = brandPrimary;
    }

    public Boolean getCustodianCredit() {
        return custodianCredit;
    }

    public void setCustodianCredit(Boolean custodianCredit) {
        this.custodianCredit = custodianCredit;
    }

    public String getGradeConfig() {
        return gradeConfig;
    }

    public void setGradeConfig(String gradeConfig) {
        this.gradeConfig = gradeConfig;
    }
}

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

    // 以下对外信息属「服务主体知情权」范围（USER-DECISION-20261014-002）：
    // 由企业自行填写并审核发布，平台不代填、不推算、不背书。
    @Note("服务区域")
    @Column(name = "service_area", length = 128)
    private String serviceArea;

    @Note("企业统一客服电话（非员工个人号码）")
    @Column(name = "service_tel", length = 32)
    private String serviceTel;

    @Note("售后专线")
    @Column(name = "after_sale_tel", length = 32)
    private String afterSaleTel;

    @Note("统一社会信用代码")
    @Column(name = "uscc", length = 32)
    private String uscc;

    @Note("资质")
    @Column(name = "qualification", length = 128)
    private String qualification;

    @Note("一句话简介")
    @Column(name = "intro", length = 512)
    private String intro;


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

    public String getServiceArea() { return serviceArea; }
    public void setServiceArea(String serviceArea) { this.serviceArea = serviceArea; }
    public String getServiceTel() { return serviceTel; }
    public void setServiceTel(String serviceTel) { this.serviceTel = serviceTel; }
    public String getAfterSaleTel() { return afterSaleTel; }
    public void setAfterSaleTel(String afterSaleTel) { this.afterSaleTel = afterSaleTel; }
    public String getUscc() { return uscc; }
    public void setUscc(String uscc) { this.uscc = uscc; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }

    public String getGradeConfig() {
        return gradeConfig;
    }

    public void setGradeConfig(String gradeConfig) {
        this.gradeConfig = gradeConfig;
    }
}

package com.th4.edge.decoboon.material.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.decoboon.material.configure.SheetTemplateScope;
import com.th4.edge.decoboon.material.configure.SheetTemplateStatus;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.framework.data.jpa.keyword.annotation.Keyword;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;

/**
 * 企业主材下单单据模板（作业单据层 / L2 企业个性化）。
 *
 * <p>平台按品类提供行业标准基线（{@link SheetTemplateScope#Platform}，只读、版本化）；
 * 企业复制后成为自己的模板（{@link SheetTemplateScope#Company}），并在**受控白名单**内个性化：
 * 字段显隐/必填/顺序/别名、默认值与值域、行骨架、签名位、编号规则、打印模板。
 * 事实、口径、状态与权限为红线，不允许通过模板改动（BR-044）。</p>
 *
 * <p>纯企业级主数据，不套门店/团队数据范围。</p>
 */
@Note("主材下单单据模板")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_material_sheet_template",
        uniqueConstraints = @UniqueConstraint(name = "uk_mst_company_type_version",
                columnNames = {"com_id", "sheet_type", "version"}),
        indexes = {
                @Index(name = "idx_mst_company", columnList = "com_id, sheet_template_id"),
                @Index(name = "idx_mst_company_scope", columnList = "com_id, scope, sheet_type")
        })
public class MaterialSheetTemplate extends AuditModel {

    @Note("模板索引")
    @Id
    @SnowflakeId
    private Long sheetTemplateId;

    @Note("所属企业")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "com_id", nullable = false)
    private Company company;

    @Keyword
    @Note("模板名称")
    @Column(length = 128, nullable = false)
    private String name;

    @Keyword
    @Note("单据类型编码（Tile 等）")
    @Column(name = "sheet_type", length = 64, nullable = false)
    private String sheetType;

    @Note("模板范围：平台行业基线/企业自定义")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private SheetTemplateScope scope = SheetTemplateScope.Company;

    @Note("继承基线版本")
    @Column(name = "baseline_version", nullable = false)
    private Integer baselineVersion = 1;

    @Note("模板版本")
    @Column(nullable = false)
    private Integer version = 1;

    @Note("当前发布版本，0=未发布")
    @Column(name = "published_version", nullable = false)
    private Integer publishedVersion = 0;

    @Note("字段配置JSON（受控白名单）")
    @Column(name = "field_config", nullable = false, columnDefinition = "LONGTEXT")
    private String fieldConfig;

    @Note("标准行骨架JSON")
    @Column(name = "row_template_config", columnDefinition = "LONGTEXT")
    private String rowTemplateConfig;

    @Note("企业默认规则JSON")
    @Column(name = "rule_config", columnDefinition = "LONGTEXT")
    private String ruleConfig;

    @Note("签名位JSON")
    @Column(name = "signature_config", columnDefinition = "LONGTEXT")
    private String signatureConfig;

    @Note("单据编号规则")
    @Column(name = "number_rule", length = 128)
    private String numberRule;

    @Note("模板状态")
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private SheetTemplateStatus status = SheetTemplateStatus.Active;

    @Note("关联主材分类（损耗率来源）")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_category_id")
    private MaterialCategory materialCategory;

    @Note("关联打印模板（DocumentTemplate）")
    @Column(name = "print_template_id")
    private Long printTemplateId;

    @Note("创建人")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Employee createdBy;

    public Long getSheetTemplateId() { return sheetTemplateId; }
    public void setSheetTemplateId(Long sheetTemplateId) { this.sheetTemplateId = sheetTemplateId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSheetType() { return sheetType; }
    public void setSheetType(String sheetType) { this.sheetType = sheetType; }
    public SheetTemplateScope getScope() { return scope; }
    public void setScope(SheetTemplateScope scope) { this.scope = scope; }
    public Integer getBaselineVersion() { return baselineVersion; }
    public void setBaselineVersion(Integer baselineVersion) { this.baselineVersion = baselineVersion; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Integer getPublishedVersion() { return publishedVersion; }
    public void setPublishedVersion(Integer publishedVersion) { this.publishedVersion = publishedVersion; }
    public String getFieldConfig() { return fieldConfig; }
    public void setFieldConfig(String fieldConfig) { this.fieldConfig = fieldConfig; }
    public String getRowTemplateConfig() { return rowTemplateConfig; }
    public void setRowTemplateConfig(String rowTemplateConfig) { this.rowTemplateConfig = rowTemplateConfig; }
    public String getRuleConfig() { return ruleConfig; }
    public void setRuleConfig(String ruleConfig) { this.ruleConfig = ruleConfig; }
    public String getSignatureConfig() { return signatureConfig; }
    public void setSignatureConfig(String signatureConfig) { this.signatureConfig = signatureConfig; }
    public String getNumberRule() { return numberRule; }
    public void setNumberRule(String numberRule) { this.numberRule = numberRule; }
    public SheetTemplateStatus getStatus() { return status; }
    public void setStatus(SheetTemplateStatus status) { this.status = status; }
    public MaterialCategory getMaterialCategory() { return materialCategory; }
    public void setMaterialCategory(MaterialCategory materialCategory) { this.materialCategory = materialCategory; }
    public Long getPrintTemplateId() { return printTemplateId; }
    public void setPrintTemplateId(Long printTemplateId) { this.printTemplateId = printTemplateId; }
    public Employee getCreatedBy() { return createdBy; }
    public void setCreatedBy(Employee createdBy) { this.createdBy = createdBy; }
}

package com.th4.edge.decoboon.ai.billing.model;

import com.th4.edge.components.data.listener.SnowflakeEntityListener;
import com.th4.edge.decoboon.ai.billing.configure.AiRateCardStatus;
import com.th4.edge.decoboon.common.model.AuditModel;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;
import com.th4.edge.framework.data.jpa.id.snowflake.annotation.SnowflakeId;
import com.th4.edge.tenant.user.model.Employee;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Note("AI费率版本")
@Model
@EntityListeners(SnowflakeEntityListener.class)
@Entity
@Table(name = "db_ai_rate_card_version",
        uniqueConstraints = @UniqueConstraint(name = "uk_ai_rate_card_version",
                columnNames = {"capability_code", "version_no"}))
public class AiRateCardVersion extends AuditModel {

    @Id @SnowflakeId @Note("费率版本索引")
    private Long rateCardId;

    @Column(name = "capability_code", length = 64, nullable = false)
    private String capabilityCode;

    @Column(name = "unit_type", length = 32, nullable = false)
    private String unitType;

    @Column(name = "units_per_point", precision = 18, scale = 6, nullable = false)
    private BigDecimal unitsPerPoint;

    @Column(name = "model_multiplier", precision = 10, scale = 4, nullable = false)
    private BigDecimal modelMultiplier = BigDecimal.ONE;

    @Column(name = "quality_multiplier", precision = 10, scale = 4, nullable = false)
    private BigDecimal qualityMultiplier = BigDecimal.ONE;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Enumerated(EnumType.STRING) @Column(length = 16, nullable = false)
    private AiRateCardStatus status = AiRateCardStatus.Draft;

    @Column(name = "effective_from") private LocalDateTime effectiveFrom;
    @Column(name = "effective_to") private LocalDateTime effectiveTo;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "approved_by")
    private Employee approvedBy;

    public Long getRateCardId() { return rateCardId; }
    public void setRateCardId(Long rateCardId) { this.rateCardId = rateCardId; }
    public String getCapabilityCode() { return capabilityCode; }
    public void setCapabilityCode(String capabilityCode) { this.capabilityCode = capabilityCode; }
    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }
    public BigDecimal getUnitsPerPoint() { return unitsPerPoint; }
    public void setUnitsPerPoint(BigDecimal unitsPerPoint) { this.unitsPerPoint = unitsPerPoint; }
    public BigDecimal getModelMultiplier() { return modelMultiplier; }
    public void setModelMultiplier(BigDecimal modelMultiplier) { this.modelMultiplier = modelMultiplier; }
    public BigDecimal getQualityMultiplier() { return qualityMultiplier; }
    public void setQualityMultiplier(BigDecimal qualityMultiplier) { this.qualityMultiplier = qualityMultiplier; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public AiRateCardStatus getStatus() { return status; }
    public void setStatus(AiRateCardStatus status) { this.status = status; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDateTime effectiveTo) { this.effectiveTo = effectiveTo; }
    public Employee getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Employee approvedBy) { this.approvedBy = approvedBy; }
}

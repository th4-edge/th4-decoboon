package com.th4.edge.platform.model;

import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jpa.annotation.Model;

/** 经平台权限揭示的企业负责人联系信息。 */
@Note("企业负责人联系信息")
@Model
public class PlatformTenantOwnerContact {
    @Note("企业ID") private Long companyId;
    @Note("员工ID") private Long empId;
    @Note("负责人姓名") private String ownerName;
    @Note("完整手机号") private String phone;

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public Long getEmpId() { return empId; }
    public void setEmpId(Long empId) { this.empId = empId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}

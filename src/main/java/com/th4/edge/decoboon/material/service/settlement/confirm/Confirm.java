package com.th4.edge.decoboon.material.service.settlement.confirm;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import com.th4.edge.decoboon.material.model.MaterialSettlement;
import com.th4.edge.tenant.user.model.Employee;

/** 确认结算（回填结算价 + 修正暂估应付 + 补记实际成本） */
public interface Confirm extends Provider<Employee, MaterialSettlement>, Security {
}

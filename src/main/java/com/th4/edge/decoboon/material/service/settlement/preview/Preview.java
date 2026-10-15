package com.th4.edge.decoboon.material.service.settlement.preview;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import com.th4.edge.decoboon.material.model.SettlementPreviewRow;
import com.th4.edge.tenant.user.model.Employee;
import java.util.List;

/** 结算预演（互斥与月结兜底后仍可结算的采购明细） */
public interface Preview extends Provider<Employee, List<SettlementPreviewRow>>, Security {
}

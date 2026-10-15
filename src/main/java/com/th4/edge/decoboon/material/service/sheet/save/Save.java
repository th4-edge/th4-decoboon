package com.th4.edge.decoboon.material.service.sheet.save;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 新建主材下单作业单（草稿） */
public interface Save extends Provider<Employee, MaterialOrderSheet>, Security {
}

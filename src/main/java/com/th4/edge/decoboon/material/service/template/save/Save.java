package com.th4.edge.decoboon.material.service.template.save;

import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 复制平台行业基线或新建企业主材下单模板。 */
public interface Save extends Provider<Employee, MaterialSheetTemplate>, Security {
}

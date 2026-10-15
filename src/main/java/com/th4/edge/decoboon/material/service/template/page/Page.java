package com.th4.edge.decoboon.material.service.template.page;

import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 主材下单模板列表：企业自定义模板 + 平台行业基线瞬态副本。 */
public interface Page extends Provider<Employee, java.util.List<MaterialSheetTemplate>>, Security {
}

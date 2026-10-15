package com.th4.edge.decoboon.material.service.sheet.enums;

import com.th4.edge.decoboon.material.model.MaterialSheetEnums;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/** 主材下单作业单枚举（品类 / 状态 / 单位），供前端选择器使用。 */
public interface Enums extends Provider<Employee, MaterialSheetEnums>, Security {
}

package com.th4.edge.decoboon.material.service.template.build;

import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Employee;

/**
 * 构建企业主材下单单据模板初值：按品类带出平台行业基线（BR-044）。
 *
 * <p>前端据此"复制基线 → 个性化 → 保存"；平台基线只读，企业保存后成为自己的模板。</p>
 */
public interface Build extends Provider<Employee, MaterialSheetTemplate>, Security {
}

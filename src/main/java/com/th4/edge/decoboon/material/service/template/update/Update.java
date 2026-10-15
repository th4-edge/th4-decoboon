package com.th4.edge.decoboon.material.service.template.update;

import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 更新企业主材下单模板（平台基线只读；字段键受白名单约束）。 */
public interface Update extends Provider<MaterialSheetTemplate, MaterialSheetTemplate>, Security {
}

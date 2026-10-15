package com.th4.edge.decoboon.material.service.template.info;

import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 主材下单模板详情（可按 templateId 或 sheetType 取）。 */
public interface Info extends Provider<MaterialSheetTemplate, MaterialSheetTemplate>, Security {
}

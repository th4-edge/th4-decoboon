package com.th4.edge.decoboon.material.service.template.publish;

import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 发布企业主材下单模板当前版本。 */
public interface Publish extends Provider<MaterialSheetTemplate, MaterialSheetTemplate>, Security {
}

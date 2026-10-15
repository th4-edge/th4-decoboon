package com.th4.edge.decoboon.material.service.template.delete;

import com.th4.edge.decoboon.material.model.MaterialSheetTemplate;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 删除企业主材下单模板（已生成作业单按冻结版本呈现，不受影响）。 */
public interface Delete extends Provider<MaterialSheetTemplate, MaterialSheetTemplate>, Security {
}

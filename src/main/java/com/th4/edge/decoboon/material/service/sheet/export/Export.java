package com.th4.edge.decoboon.material.service.sheet.export;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.decoboon.material.model.MaterialSheetExportFile;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 导出主材下单作业单为 Excel 单据（按导出时快照；不带价格与账期）。 */
public interface Export extends Provider<MaterialOrderSheet, MaterialSheetExportFile>, Security {
}

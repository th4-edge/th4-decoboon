package com.th4.edge.decoboon.material.service.sheet.measure;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 测量与配送/安装时间确认（项目经理或设计师） */
public interface Measure extends Provider<MaterialOrderSheet, MaterialOrderSheet>, Security {
}

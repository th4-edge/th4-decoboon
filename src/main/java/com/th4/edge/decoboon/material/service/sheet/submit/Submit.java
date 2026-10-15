package com.th4.edge.decoboon.material.service.sheet.submit;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 提交请购（整单复核选材门禁与数量上限） */
public interface Submit extends Provider<MaterialOrderSheet, MaterialOrderSheet>, Security {
}

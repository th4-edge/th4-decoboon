package com.th4.edge.decoboon.material.service.sheet.delete;

import com.th4.edge.decoboon.material.model.MaterialOrderSheet;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 删除主材下单作业单（仅草稿/已取消） */
public interface Delete extends Provider<MaterialOrderSheet, MaterialOrderSheet>, Security {
}

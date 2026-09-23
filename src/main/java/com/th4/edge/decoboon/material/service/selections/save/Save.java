package com.th4.edge.decoboon.material.service.selections.save;

import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 主材选材记录保存（草稿或提交待确认）。 */
public interface Save extends Provider<MaterialSelection, MaterialSelection>, Security {}

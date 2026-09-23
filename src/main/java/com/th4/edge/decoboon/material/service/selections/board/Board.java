package com.th4.edge.decoboon.material.service.selections.board;

import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.decoboon.material.model.SelectionBoardItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 主材选材跟进台：数据范围内尚未客户确认选材的在跑项目。 */
public interface Board extends Provider<MaterialSelection, List<SelectionBoardItem>>, Security {}

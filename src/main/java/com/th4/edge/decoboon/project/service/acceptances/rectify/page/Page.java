package com.th4.edge.decoboon.project.service.acceptances.rectify.page;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.AcceptanceRectifyItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 整改中阶段清单：数据范围内当前处于驳回（整改中）的阶段验收。 */
public interface Page extends Provider<Acceptance, List<AcceptanceRectifyItem>>, Security {}

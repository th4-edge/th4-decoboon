package com.th4.edge.decoboon.project.service.acceptances.round.page;

import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.AcceptanceRound;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 阶段验收轮次时间线（append-only，最新在前）。 */
public interface Page extends Provider<Acceptance, List<AcceptanceRound>>, Security {}

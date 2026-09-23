package com.th4.edge.decoboon.material.service.selections.page;

import com.th4.edge.decoboon.material.model.MaterialSelection;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 主材选材记录分页。 */
public interface Page extends Provider<MaterialSelection, org.springframework.data.domain.Page<MaterialSelection>>, Security {}

package com.th4.edge.decoboon.material.service.selections.item.page;

import com.th4.edge.decoboon.material.model.MaterialSelectionItem;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目主材分项清单。 */
public interface Page extends Provider<MaterialSelectionItem, org.springframework.data.domain.Page<MaterialSelectionItem>>, Security {}

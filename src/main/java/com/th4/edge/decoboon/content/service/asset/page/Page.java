package com.th4.edge.decoboon.content.service.asset.page;

import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 内容素材分页。 */
public interface Page extends Provider<ContentAsset, org.springframework.data.domain.Page<ContentAsset>>, Security {
}

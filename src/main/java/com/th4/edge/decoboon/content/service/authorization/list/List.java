package com.th4.edge.decoboon.content.service.authorization.list;

import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.model.ContentAuthorization;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;


/** 素材授权列表。 */
public interface List extends Provider<ContentAsset, java.util.List<ContentAuthorization>>, Security {
}

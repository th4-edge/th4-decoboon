package com.th4.edge.decoboon.content.service.authorization.revoke;

import com.th4.edge.decoboon.content.model.ContentAuthorization;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 素材授权撤回。 */
public interface Revoke extends Provider<ContentAuthorization, ContentAuthorization>, Security {
}

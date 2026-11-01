package com.th4.edge.tenant.user.service.invite.invitation.pending;

import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.PendingEntryInvite;

import java.util.List;

/** 当前已验证手机号的待接受员工/业主邀请。 */
public interface Pending extends Provider<Credentials, List<PendingEntryInvite>>, Security {}

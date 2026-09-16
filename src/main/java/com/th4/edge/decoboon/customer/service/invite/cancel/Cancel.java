package com.th4.edge.decoboon.customer.service.invite.cancel;

import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 撤销业主邀请或已建立的业主授权。 */
public interface Cancel extends Provider<CustomerInvite, CustomerInvite>, Security {
}

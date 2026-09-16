package com.th4.edge.decoboon.customer.service.invite.page;

import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目内业主邀请分页。 */
public interface Page extends Provider<CustomerInvite, org.springframework.data.domain.Page<CustomerInvite>>, Security {
}

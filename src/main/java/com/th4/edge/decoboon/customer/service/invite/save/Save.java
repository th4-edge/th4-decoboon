package com.th4.edge.decoboon.customer.service.invite.save;

import com.th4.edge.decoboon.customer.model.CustomerInvite;
import com.th4.edge.framework.provider.security.Security;
import com.th4.edge.framework.service.ModelService;

/** 业主邀请创建服务。 */
public interface Save extends ModelService<CustomerInvite, CustomerInvite>, Security {
}

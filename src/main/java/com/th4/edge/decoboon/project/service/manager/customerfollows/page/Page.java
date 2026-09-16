package com.th4.edge.decoboon.project.service.manager.customerfollows.page;

import com.th4.edge.decoboon.customer.model.CustomerFollow;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 项目经理关联客户跟进分页。 */
public interface Page extends Provider<CustomerFollow, org.springframework.data.domain.Page<CustomerFollow>>, Security {
}

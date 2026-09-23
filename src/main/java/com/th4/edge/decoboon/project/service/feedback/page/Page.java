package com.th4.edge.decoboon.project.service.feedback.page;

import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

import java.util.List;

/** 员工查看项目业主意见投诉。 */
public interface Page extends Provider<CustomerFeedback, List<CustomerFeedback>>, Security {
}

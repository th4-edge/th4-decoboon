package com.th4.edge.decoboon.project.service.feedback.update;

import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 员工处理业主意见投诉。 */
public interface Update extends Provider<CustomerFeedback, CustomerFeedback>, Security {
}

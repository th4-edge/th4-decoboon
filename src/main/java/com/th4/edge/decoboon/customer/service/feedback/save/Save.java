package com.th4.edge.decoboon.customer.service.feedback.save;

import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主提交意见或投诉。 */
public interface Save extends Provider<CustomerFeedback, CustomerFeedback>, Security {
}

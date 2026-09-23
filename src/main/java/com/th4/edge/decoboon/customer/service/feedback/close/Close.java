package com.th4.edge.decoboon.customer.service.feedback.close;

import com.th4.edge.decoboon.customer.model.CustomerFeedback;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 业主确认关闭意见或投诉。 */
public interface Close extends Provider<CustomerFeedback, CustomerFeedback>, Security {
}

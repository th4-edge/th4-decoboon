package com.th4.edge.decoboon.customer.service.customerbatch.save;

import com.th4.edge.decoboon.customer.model.CustomerBatch;
import com.th4.edge.decoboon.customer.model.CustomerBatchPreview;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 批量手机号客户录入。 */
public interface Save extends Provider<CustomerBatch, CustomerBatchPreview>, Security {
}

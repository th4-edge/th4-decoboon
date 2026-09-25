package com.th4.edge.decoboon.customer.service.customerbatch.build;

import com.th4.edge.decoboon.customer.model.CustomerBatch;
import com.th4.edge.decoboon.customer.model.CustomerBatchPreview;
import com.th4.edge.framework.provider.Provider;
import com.th4.edge.framework.provider.security.Security;

/** 批量手机号解析预览。 */
public interface Build extends Provider<CustomerBatch, CustomerBatchPreview>, Security {
}

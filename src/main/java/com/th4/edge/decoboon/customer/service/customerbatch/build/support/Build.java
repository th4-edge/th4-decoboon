package com.th4.edge.decoboon.customer.service.customerbatch.build.support;

import com.th4.edge.decoboon.customer.context.CustomerBatchContext;
import com.th4.edge.decoboon.customer.model.CustomerBatch;
import com.th4.edge.decoboon.customer.model.CustomerBatchPreview;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 批量手机号解析预览实现。 */
@Note("批量手机号解析预览")
@Properties(allow = {
        @AllowProperties(model = CustomerBatchPreview.class,
                properties = {"total", "validCount", "duplicateCount", "invalidCount", "createdCount", "items"}),
        @AllowProperties(model = CustomerBatchPreview.Item.class,
                properties = {"input", "phone", "status", "reason"})
})
@Service("customer/customer-batch/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.customer.service.customerbatch.build.Build {

    @User
    private Employee employee;

    @AutoInject
    @NotEmpty
    @Length(max = 20000)
    private String text;

    private final CustomerBatchContext customerBatchContext;

    public Build(CustomerBatchContext customerBatchContext) {
        this.customerBatchContext = customerBatchContext;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerBatchPreview call(CustomerBatch request) {
        return customerBatchContext.preview(employee, text);
    }
}

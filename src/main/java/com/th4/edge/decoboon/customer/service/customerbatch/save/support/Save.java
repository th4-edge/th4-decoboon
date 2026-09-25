package com.th4.edge.decoboon.customer.service.customerbatch.save.support;

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
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 批量手机号客户录入实现。 */
@Note("批量手机号客户录入")
@Properties(allow = {
        @AllowProperties(model = CustomerBatchPreview.class,
                properties = {"total", "validCount", "duplicateCount", "invalidCount", "createdCount", "items"}),
        @AllowProperties(model = CustomerBatchPreview.Item.class,
                properties = {"input", "phone", "status", "reason"})
})
@Service("customer/customer-batch/save")
@Scope(ScopeTypes.PROTOTYPE)
public class Save implements com.th4.edge.decoboon.customer.service.customerbatch.save.Save {

    @User
    private Employee employee;

    @AutoInject
    @NotEmpty
    @Length(max = 20000)
    private String text;

    @AutoInject
    @NotNull
    private Long ownerId;

    private final CustomerBatchContext customerBatchContext;

    public Save(CustomerBatchContext customerBatchContext) {
        this.customerBatchContext = customerBatchContext;
    }

    @Override
    @Transactional
    public CustomerBatchPreview call(CustomerBatch request) {
        return customerBatchContext.save(employee, text, ownerId);
    }
}

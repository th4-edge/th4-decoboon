package com.th4.edge.decoboon.document.service.voucher.build.support;

import com.th4.edge.decoboon.document.context.VoucherContext;
import com.th4.edge.decoboon.document.model.VoucherItem;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.finance.model.Ledger;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 凭证生成实现。 */
@Note("凭证生成")
@Properties(allow = {
        @AllowProperties(model = VoucherRecord.class,
                properties = {"voucherId", "project", "type", "voucherNo", "title", "templateVersion",
                        "totalAmount", "amountWords", "partyName", "worker", "status", "fileName",
                        "contentType", "size", "generatedBy", "generatedAt", "items", "url", "urlExpiresAt"}),
        @AllowProperties(model = VoucherItem.class,
                properties = {"itemId", "sourceType", "sourceId", "sourceNo", "businessDate",
                        "account", "amount", "category", "remark"}),
        @AllowProperties(model = Worker.class,
                properties = {"workerId", "name", "trade", "phone"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("document/voucher/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.document.service.voucher.build.Build {

    @User
    private Employee employee;
    @AutoInject
    private List<Long> ledgerIds;

    private final VoucherContext voucherContext;

    public Build(VoucherContext voucherContext) {
        this.voucherContext = voucherContext;
    }

    @Override
    @Transactional
    public VoucherRecord call(Ledger request) {
        return voucherContext.build(employee, ledgerIds);
    }
}

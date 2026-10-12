package com.th4.edge.decoboon.document.service.voucher.page.support;

import com.th4.edge.decoboon.common.context.StoreFilterContext;
import com.th4.edge.decoboon.common.context.StoreScope;

import com.th4.edge.decoboon.document.configure.DocumentStatus;
import com.th4.edge.decoboon.document.context.VoucherContext;
import com.th4.edge.decoboon.document.model.VoucherItem;
import com.th4.edge.decoboon.document.model.VoucherRecord;
import com.th4.edge.decoboon.document.repository.VoucherRecordRepository;
import com.th4.edge.decoboon.labor.model.Worker;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.access.DataScopeSpecification;
import com.th4.edge.tenant.user.access.context.DataScopeContext;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/** 凭证记录分页实现。 */
@Note("凭证记录分页")
@Properties(allow = {
        @AllowProperties(model = VoucherRecord.class,
                properties = {"voucherId", "project", "type", "voucherNo", "title", "templateVersion",
                        "totalAmount", "amountWords", "partyName", "worker", "status", "fileName",
                        "generatedBy", "generatedAt", "items", "url", "urlExpiresAt"}),
        @AllowProperties(model = VoucherItem.class,
                properties = {"itemId", "sourceType", "sourceId", "sourceNo", "businessDate",
                        "account", "amount", "category", "remark"}),
        @AllowProperties(model = Worker.class,
                properties = {"workerId", "name", "trade", "phone"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name"})
})
@Service("document/voucher/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.document.service.voucher.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long projectId;
    @AutoInject
    private String status;
    @AutoInject
    private String keyword;
    /** 凭证类型：Receipt 收款 / Payment 付款。 */
    @AutoInject
    private String type;
    @AutoInject
    private LocalDate startDate;
    @AutoInject
    private LocalDate endDate;

    /** 门店维度（`DEC-065` / `P1-a2`）：不传 / 0 = 全部门店；门店范围角色恒锁本店，传他店 403。 */
    @AutoInject
    private Long storeId;
    @org.springframework.beans.factory.annotation.Autowired
    private com.th4.edge.decoboon.common.context.StoreFilterContext storeFilterContext;

    private final VoucherRecordRepository recordRepository;
    private final VoucherContext voucherContext;
    private final TenantContext tenantContext;
    private final DataScopeContext dataScopeContext;

    public Page(VoucherRecordRepository recordRepository, VoucherContext voucherContext,
                TenantContext tenantContext, DataScopeContext dataScopeContext) {
        this.recordRepository = recordRepository;
        this.voucherContext = voucherContext;
        this.tenantContext = tenantContext;
        this.dataScopeContext = dataScopeContext;
    }

    @Override
    public org.springframework.data.domain.Page<VoucherRecord> call(VoucherRecord request) {
        Employee viewer = tenantContext.requireRole(employee, EmployeeRole.Boss, EmployeeRole.StoreManager,
                EmployeeRole.Finance, EmployeeRole.Pm, EmployeeRole.Designer);
        Long comId = viewer.getCompany().getComId();
        Specification<VoucherRecord> visible = viewer.getRole() == EmployeeRole.Boss
                || viewer.getRole() == EmployeeRole.Finance
                ? (root, query, builder) -> builder.equal(root.get("company").get("comId"), comId)
                : DataScopeSpecification.visible(VoucherRecord.class, dataScopeContext.resolve(viewer));
        Long storeFilter = storeFilterContext.resolve(viewer, storeId);
        List<VoucherRecord> items = recordRepository.findAll(visible).stream()
                .filter(item -> StoreScope.inStore(StoreScope.storeIdOf(item.getStore()), storeFilter))
                .filter(item -> projectId == null
                        || (item.getProject() != null && projectId.equals(item.getProject().getProjectId())))
                .filter(item -> status == null || status.isBlank() || item.getStatus().name().equalsIgnoreCase(status))
                .filter(item -> type == null || type.isBlank() || item.getType().name().equalsIgnoreCase(type))
                .filter(item -> matchesKeyword(item, keyword))
                .filter(item -> startDate == null || (item.getGeneratedAt() != null
                        && !item.getGeneratedAt().toLocalDate().isBefore(startDate)))
                .filter(item -> endDate == null || (item.getGeneratedAt() != null
                        && !item.getGeneratedAt().toLocalDate().isAfter(endDate)))
                .sorted(Comparator.comparing(VoucherRecord::getVoucherId,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(voucherContext::signed)
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 200);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }

    /** 关键词匹配凭证号、对方名称与所属项目名称。 */
    private boolean matchesKeyword(VoucherRecord item, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String kw = keyword.trim().toLowerCase();
        return contains(item.getVoucherNo(), kw)
                || contains(item.getPartyName(), kw)
                || contains(item.getWorker() == null ? null : item.getWorker().getName(), kw)
                || contains(item.getProject() == null ? null : item.getProject().getName(), kw);
    }

    private boolean contains(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }
}

package com.th4.edge.decoboon.supply.service.labor.settle.support;

import com.th4.edge.components.data.context.ModelIdentityContext;
import com.th4.edge.decoboon.catalog.model.CatalogItem;
import com.th4.edge.decoboon.catalog.repository.CatalogItemRepository;
import com.th4.edge.decoboon.finance.configure.PayableStatus;
import com.th4.edge.decoboon.finance.configure.PayableType;
import com.th4.edge.decoboon.finance.model.Payable;
import com.th4.edge.decoboon.finance.repository.PayableRepository;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.supply.configure.PartnerType;
import com.th4.edge.decoboon.supply.model.Partner;
import com.th4.edge.decoboon.supply.model.PartnerPrice;
import com.th4.edge.decoboon.supply.repository.PartnerPriceRepository;
import com.th4.edge.decoboon.supply.repository.PartnerRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 按工序生成劳务进度款应付。 */
@Note("劳务进度结算")
@Properties(allow = {
        @AllowProperties(model = Payable.class,
                properties = {"payableId", "project", "name", "type", "payeePartner", "payeeName", "period",
                        "amount", "paidAmount", "dueDate", "status", "sourceNo", "remark", "createdBy", "createTime"}),
        @AllowProperties(model = Partner.class, properties = {"partnerId", "type", "name", "telNo"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "no", "name", "status"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/labor/settle")
@Scope(ScopeTypes.PROTOTYPE)
public class Settle implements com.th4.edge.decoboon.supply.service.labor.settle.Settle {
    @User private Employee employee;
    @AutoInject @NotNull private Long projectId;
    @AutoInject @NotNull private Long partnerId;
    @AutoInject @NotNull private Long itemId;
    @AutoInject @NotNull private BigDecimal quantity;
    @AutoInject private String period;
    @AutoInject private LocalDate dueDate;
    @AutoInject private String sourceNo;
    @AutoInject private String remark;

    private final ProjectRepository projectRepository;
    private final PartnerRepository partnerRepository;
    private final PartnerPriceRepository priceRepository;
    private final CatalogItemRepository itemRepository;
    private final PayableRepository payableRepository;
    private final ProjectAccessContext accessContext;
    private final ModelIdentityContext modelIdentityContext;

    public Settle(ProjectRepository projectRepository, PartnerRepository partnerRepository,
                  PartnerPriceRepository priceRepository, CatalogItemRepository itemRepository,
                  PayableRepository payableRepository, ProjectAccessContext accessContext,
                  ModelIdentityContext modelIdentityContext) {
        this.projectRepository = projectRepository;
        this.partnerRepository = partnerRepository;
        this.priceRepository = priceRepository;
        this.itemRepository = itemRepository;
        this.payableRepository = payableRepository;
        this.accessContext = accessContext;
        this.modelIdentityContext = modelIdentityContext;
    }

    @Override
    @Transactional
    public Payable call(Payable request) {
        Employee current = accessContext.requireEmployee(employee);
        if (current.getRole() != EmployeeRole.Boss && current.getRole() != EmployeeRole.Finance
                && current.getRole() != EmployeeRole.Pm) {
            throw new DefinedException(new DefaultErrorCode("403", "当前角色不能发起劳务结算"));
        }
        if (quantity.signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "结算工程量必须大于 0"));
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目不存在")));
        accessContext.requireVisible(project, current);
        Long comId = current.getCompany().getComId();
        Partner partner = partnerRepository.findById(partnerId)
                .filter(item -> item.getCompany().getComId().equals(comId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "劳务班组不存在")));
        if (partner.getType() != PartnerType.LaborTeam && partner.getType() != PartnerType.Installer) {
            throw new DefinedException(new DefaultErrorCode("400", "合作方不是劳务或安装班组"));
        }
        CatalogItem item = itemRepository.findById(itemId)
                .filter(value -> value.getCompany().getComId().equals(comId))
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "工价项目不存在")));
        PartnerPrice price = priceRepository.findFirstByPartnerAndItemAndActiveTrueOrderByPartnerPriceIdDesc(partner, item);
        if (price == null || price.getLaborPrice() == null || price.getLaborPrice().signum() <= 0) {
            throw new DefinedException(new DefaultErrorCode("400", "未配置该班组工价"));
        }
        BigDecimal amount = quantity.multiply(price.getLaborPrice());
        request.setCompany(current.getCompany());
        request.setProject(project);
        request.setProperty(project.getProperty());
        request.setStore(project.getCustomer() == null ? null : project.getCustomer().getStore());
        request.setTeam(project.getCustomer() == null ? null : project.getCustomer().getTeam());
        request.setOwner(project.getCustomer() == null || project.getCustomer().getOwner() == null
                ? current : project.getCustomer().getOwner());
        request.setName("劳务进度 · " + partner.getName() + " · " + item.getName());
        request.setType(PayableType.Salary);
        request.setPayeePartner(partner);
        request.setPayeeName(partner.getName());
        request.setPeriod(period);
        request.setAmount(amount);
        request.setPaidAmount(BigDecimal.ZERO);
        request.setDueDate(dueDate == null ? LocalDate.now().plusDays(price.getSettlementDays()) : dueDate);
        request.setStatus(PayableStatus.Pending);
        request.setUrgent(false);
        request.setSourceNo(sourceNo);
        request.setRemark(remark);
        request.setCreatedBy(current);
        return payableRepository.save(modelIdentityContext.prepare(request));
    }
}

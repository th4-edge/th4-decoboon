package com.th4.edge.decoboon.customer.service.access.scanenter.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.context.CustomerInviteContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.customer.model.CustomerAccess;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 扫码开通业主端实现（`USER-DECISION-20261015-001`）。
 *
 * <pre>
 * 流程：业主扫项目码 → 小程序以 pages/owner/enter?scene={项目编号} 打开
 *      → 微信登录 + 授权手机号 → 本接口校验「授权手机号 == 房屋所属客户档案手机号」
 *      → 建立/复用该项目业主授权（invite 为空）→ 返回业主会话。
 *
 * 门禁与不变量：
 * 1. 唯一门禁是手机号一致；scene 只含项目编号，不含任何客户信息。
 * 2. 员工微信不得进入业主端（与邀请接受路径同口径）。
 * 3. 失败话术不区分"项目不存在 / 号码不符"以外的信息，避免用错误差异探测客户档案。
 * 4. 授权来源 invite 为空：扫码开通不以邀请为前提；重复扫码幂等，不新增第二条授权。
 * 5. 跨企业隔离：scene 命中项目后，以该项目所属客户档案手机号为准，不做任何跨租户放宽。
 * </pre>
 */
@Note("扫码开通业主端")
@Properties(allow = {
        @AllowProperties(model = Credentials.class,
                properties = {"creId", "phone", "status", "lastTime", "sessionType", "customerAccesses"}),
        @AllowProperties(model = CustomerAccess.class,
                properties = {"accessId", "displayName", "status", "grantedAt", "project", "customer"}),
        @AllowProperties(model = Project.class,
                properties = {"projectId", "no", "name", "siteAddr", "area", "status", "progress", "property"}),
        @AllowProperties(model = Property.class,
                properties = {"propertyId", "propertyNo", "community", "building", "roomNo", "address", "area", "layout"}),
        @AllowProperties(model = Customer.class,
                properties = {"custId", "name", "community", "addr"}),
        @AllowProperties(model = Company.class, properties = {"comId", "title", "name", "logo", "color"})
})
@Service("identity/customer-access/scan-enter")
@Scope(ScopeTypes.PROTOTYPE)
public class ScanEnter implements com.th4.edge.decoboon.customer.service.access.scanenter.ScanEnter {

    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotEmpty
    @Length(max = 32)
    private String scene;

    private final CustomerAccessContext accessContext;
    private final ProjectRepository projectRepository;

    public ScanEnter(CustomerAccessContext accessContext, ProjectRepository projectRepository) {
        this.accessContext = accessContext;
        this.projectRepository = projectRepository;
    }

    @Override
    @Transactional
    public Credentials call(Credentials request) {
        Credentials credential = accessContext.requireCredential(principal);
        if (credential.getEmployee() != null) {
            throw new DefinedException(new DefaultErrorCode("403",
                    "这个微信已绑定装修公司员工身份，请让业主本人微信扫码"));
        }
        String normalizedScene = scene == null ? "" : scene.trim();
        Project project = projectRepository.findByNo(normalizedScene)
                .orElseThrow(() -> new DefinedException(new DefaultErrorCode("404", "项目码无效或项目不存在")));
        Customer customer = resolveOwnerCustomer(project);
        if (customer == null) {
            throw new DefinedException(new DefaultErrorCode("400", "该项目未关联业主档案，请联系您的设计师"));
        }
        String expected = CustomerInviteContext.normalizePhone(customer.getTelNo());
        String actual = CustomerInviteContext.normalizePhone(credential.getPhone());
        if (expected == null || expected.isBlank() || !expected.equals(actual)) {
            throw new DefinedException(new DefaultErrorCode("403",
                    "该手机号不是本项目的业主手机号，请联系您的设计师"));
        }
        accessContext.grantByScan(credential, project, customer);
        accessContext.attachSession(credential);
        return credential;
    }

    /** 业主身份以**房屋所属客户**为准；房屋未挂客户时回退项目客户，二者皆空则拒绝。 */
    private Customer resolveOwnerCustomer(Project project) {
        Property property = project.getProperty();
        if (property != null && property.getCustomer() != null) {
            return property.getCustomer();
        }
        return project.getCustomer();
    }
}

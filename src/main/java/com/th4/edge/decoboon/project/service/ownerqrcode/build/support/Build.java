package com.th4.edge.decoboon.project.service.ownerqrcode.build.support;

import com.th4.edge.components.storage.configure.StorageProperties;
import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.components.wechat.configure.WechatMiniProgramConfigure;
import com.th4.edge.components.wechat.service.WechatMiniProgramClient;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Duration;

/**
 * 业主项目码生成实现。
 *
 * <p>口径（`USER-DECISION-20261015-001`）：合同生效立项后，项目即可生成专属小程序码；
 * 业主扫码后由「授权手机号 == 房屋所属客户档案手机号」把关进入业主端。</p>
 *
 * <pre>
 * 安全与稳定性要点：
 * 1. scene 只用**项目编号**（对外可读、≤32 字符、不含任何客户信息）；手机号绝不进码。
 * 2. 图片按 `owner-qrcode/{comId}/{projectId}-{env}.jpg` 落私有存储；**对象键含版本环境**，
 *    避免 release/trial 切换后复用旧图（历史坑：同一键复用在切环境时会把体验版码当正式版用）。
 * 3. 幂等：对象已存在直接复用，不重复调用微信接口（微信有频次限制）。
 * 4. 返回短期签名地址（默认 ≥10 分钟），项目实体本身不落库任何码字段（无 DDL）。
 * </pre>
 */
@Note("业主项目码生成")
@Properties(allow = {
        @AllowProperties(model = Project.class, properties = {
                "projectId", "no", "name", "siteAddr", "status", "property", "customer",
                "ownerQrcodeScene", "ownerQrcodePath", "ownerQrcodeUrl", "ownerQrcodeEnvVersion"}),
        @AllowProperties(model = Property.class, properties = {"propertyId", "community", "building", "roomNo", "address"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("project/owner-qrcode/build")
@Scope(ScopeTypes.PROTOTYPE)
public class Build implements com.th4.edge.decoboon.project.service.ownerqrcode.build.Build {

    /** 业主扫码落地页（与 App `pages/owner/enter` 对应）。 */
    private static final String OWNER_ENTER_PAGE = "pages/owner/enter";

    @User
    private Employee employee;

    @AutoInject
    @NotNull
    private Long projectId;

    private final ProjectAccessContext accessContext;
    private final WechatMiniProgramClient wechatClient;
    private final WechatMiniProgramConfigure wechatConfigure;
    private final ObjectStorage storage;
    private final StorageProperties storageProperties;

    public Build(ProjectAccessContext accessContext,
                 WechatMiniProgramClient wechatClient,
                 WechatMiniProgramConfigure wechatConfigure,
                 ObjectStorage storage,
                 StorageProperties storageProperties) {
        this.accessContext = accessContext;
        this.wechatClient = wechatClient;
        this.wechatConfigure = wechatConfigure;
        this.storage = storage;
        this.storageProperties = storageProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public Project call(Project request) {
        Employee current = accessContext.requireEmployee(employee);
        Project project = accessContext.requireProject(projectId, current);
        String scene = project.getNo();
        if (scene == null || scene.isBlank()) {
            throw new DefinedException(new DefaultErrorCode("400", "项目编号缺失，无法生成业主项目码"));
        }
        String envVersion = wechatConfigure.getQrCodeEnvVersion();
        String key = "owner-qrcode/" + project.getCompany().getComId() + "/" + project.getProjectId()
                + "-" + envVersion + ".jpg";
        if (!storage.exists(key)) {
            byte[] image = wechatClient.unlimitedQrCode(scene, OWNER_ENTER_PAGE, envVersion);
            storage.put(key, new ByteArrayInputStream(image), image.length, "image/jpeg");
        }
        project.setOwnerQrcodeScene(scene);
        project.setOwnerQrcodePath(key);
        project.setOwnerQrcodeEnvVersion(envVersion);
        project.setOwnerQrcodeUrl(storage.presignedGet(key,
                Duration.ofSeconds(Math.max(600L, storageProperties.getPresignTtlSeconds()))));
        return project;
    }
}

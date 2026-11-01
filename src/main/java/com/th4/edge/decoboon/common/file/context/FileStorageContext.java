package com.th4.edge.decoboon.common.file.context;

import com.th4.edge.components.storage.configure.StorageProperties;
import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.components.storage.service.StoredObject;
import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.io.StreamFile;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.configure.EmployeeRole;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Credentials;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

/** 私有文件上传：员工可上传业务文件，业主仅可上传已授权项目的验收整改图片。 */
@Component
public class FileStorageContext {

    private static final long CUSTOMER_FOLLOW_IMAGE_MAX_BYTES = 10 * 1024 * 1024L;
    private static final Set<String> CUSTOMER_FOLLOW_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final ObjectStorage storage;
    private final ObjectKeyContext keyContext;
    private final FileSignatureContext signatureContext;
    private final TenantContext tenantContext;
    private final ProjectAccessContext projectAccessContext;
    private final CustomerAccessContext customerAccessContext;
    private final StorageProperties storageProperties;

    public FileStorageContext(ObjectStorage storage,
                              ObjectKeyContext keyContext,
                              FileSignatureContext signatureContext,
                              TenantContext tenantContext,
                              ProjectAccessContext projectAccessContext,
                              CustomerAccessContext customerAccessContext,
                              StorageProperties storageProperties) {
        this.storage = storage;
        this.keyContext = keyContext;
        this.signatureContext = signatureContext;
        this.tenantContext = tenantContext;
        this.projectAccessContext = projectAccessContext;
        this.customerAccessContext = customerAccessContext;
        this.storageProperties = storageProperties;
    }

    public FileGrant upload(User<?> principal, FileScope businessType, Long projectId, StreamFile file) {
        Long companyId;
        if (principal instanceof Employee employee) {
            Employee current = tenantContext.requireEmployee(employee);
            if (projectId != null) projectAccessContext.requireProject(projectId, current);
            if (businessType == FileScope.Partner
                    && current.getRole() != EmployeeRole.Boss
                    && current.getRole() != EmployeeRole.StoreManager
                    && current.getRole() != EmployeeRole.Finance) {
                throw new DefinedException(new DefaultErrorCode("403", "当前岗位不能维护供应商付款码"));
            }
            companyId = current.getCompany().getComId();
        } else if (principal instanceof Credentials credential) {
            // 业主仅可为验收整改问题上传图片，不允许使用通用业务文件上传通道。
            if (businessType != FileScope.Acceptance && businessType != FileScope.Feedback) {
                throw new DefinedException(new DefaultErrorCode("403", "当前身份仅可上传验收整改或意见投诉图片"));
            }
            if (projectId == null) {
                throw new DefinedException(new DefaultErrorCode("400", "业主图片必须关联项目"));
            }
            var access = customerAccessContext.requireAccess(credential, projectId);
            if (access.getCompany() == null) {
                throw new DefinedException(new DefaultErrorCode("404", "项目不存在或无权访问"));
            }
            companyId = access.getCompany().getComId();
        } else {
            throw new DefinedException(new DefaultErrorCode("403", "仅企业员工可以上传文件"));
        }
        if (file == null || file.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择要上传的文件"));
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new DefinedException(new DefaultErrorCode("400", "文件超过允许的最大大小"));
        }
        String originalName = file.getOriginalFilename();
        validateBusinessFile(businessType, originalName, file);
        String key = keyContext.file(companyId, projectId, businessType, originalName);
        try (var input = file.getInputStream()) {
            StoredObject stored = storage.put(key, input, file.getSize(), file.getContentType());
            FileGrant grant = new FileGrant();
            grant.setBusinessType(businessType);
            grant.setProjectId(projectId);
            grant.setPath(key);
            grant.setUrl(signatureContext.sign(key));
            grant.setExpiresAt(signatureContext.expiresAt());
            grant.setName(originalName);
            grant.setContentType(stored.contentType());
            grant.setSize(stored.size());
            return grant;
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "文件上传失败：" + ex.getMessage()));
        }
    }

    private void validateBusinessFile(FileScope businessType, String originalName, StreamFile file) {
        if (businessType != FileScope.CustomerFollow && businessType != FileScope.Partner) return;
        String extension = keyContext.extension(originalName);
        if (!CUSTOMER_FOLLOW_IMAGE_EXTENSIONS.contains(extension)) {
            throw new DefinedException(new DefaultErrorCode("400", "仅支持 JPG、PNG、WEBP 图片"));
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new DefinedException(new DefaultErrorCode("400", "仅支持 JPG、PNG、WEBP 图片"));
        }
        if (file.getSize() > CUSTOMER_FOLLOW_IMAGE_MAX_BYTES) {
            throw new DefinedException(new DefaultErrorCode("400", "单张图片不能超过 10MB"));
        }
    }
}

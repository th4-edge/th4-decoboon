package com.th4.edge.decoboon.common.file.context;

import com.th4.edge.components.storage.configure.StorageProperties;
import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.components.storage.service.StoredObject;
import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.model.FileGrant;
import com.th4.edge.decoboon.project.context.ProjectAccessContext;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.io.StreamFile;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.tenant.user.context.TenantContext;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.stereotype.Component;

/** 私有文件上传：企业固定、项目强校验、对象键统一生成。 */
@Component
public class FileStorageContext {

    private final ObjectStorage storage;
    private final ObjectKeyContext keyContext;
    private final FileSignatureContext signatureContext;
    private final TenantContext tenantContext;
    private final ProjectAccessContext projectAccessContext;
    private final StorageProperties storageProperties;

    public FileStorageContext(ObjectStorage storage,
                              ObjectKeyContext keyContext,
                              FileSignatureContext signatureContext,
                              TenantContext tenantContext,
                              ProjectAccessContext projectAccessContext,
                              StorageProperties storageProperties) {
        this.storage = storage;
        this.keyContext = keyContext;
        this.signatureContext = signatureContext;
        this.tenantContext = tenantContext;
        this.projectAccessContext = projectAccessContext;
        this.storageProperties = storageProperties;
    }

    public FileGrant upload(User<?> principal, FileScope businessType, Long projectId, StreamFile file) {
        if (!(principal instanceof Employee employee)) {
            throw new DefinedException(new DefaultErrorCode("403", "仅企业员工可以上传文件"));
        }
        Employee current = tenantContext.requireEmployee(employee);
        if (file == null || file.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("400", "请选择要上传的文件"));
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new DefinedException(new DefaultErrorCode("400", "文件超过允许的最大大小"));
        }
        if (projectId != null) projectAccessContext.requireProject(projectId, current);
        String originalName = file.getOriginalFilename();
        String key = keyContext.file(current.getCompany().getComId(), projectId, businessType, originalName);
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
}

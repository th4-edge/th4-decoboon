package com.th4.edge.decoboon.content.service.asset.page.support;

import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.configure.ContentTruthType;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.repository.ContentAssetRepository;
import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.customer.model.Customer;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.tenant.user.model.Employee;
import com.th4.edge.tenant.user.model.Store;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/** 内容素材分页实现。 */
@Note("内容素材分页")
@Properties(allow = {
        @AllowProperties(model = ContentAsset.class,
                properties = {"assetId", "title", "mediaType", "sourceType", "truthType", "filePath",
                        "originalName", "contentType", "fileSize", "tags", "desensitized", "status",
                        "remark", "dataVersion", "previewUrl", "createTime", "updateTime", "project", "customer", "uploader", "store"}),
        @AllowProperties(model = Project.class, properties = {"projectId", "name", "no", "status"}),
        @AllowProperties(model = Customer.class, properties = {"custId", "name", "community"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"}),
        @AllowProperties(model = Store.class, properties = {"storeId", "name"})
})
@Service("content/asset/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.content.service.asset.page.Page {

    @User
    private Employee employee;
    @AutoInject
    private Integer current;
    @AutoInject
    private Integer size;
    @AutoInject
    private Long projectId;
    @AutoInject
    private ContentAssetStatus status;
    @AutoInject
    private ContentTruthType truthType;
    @AutoInject
    private String keyword;

    private final ContentAssetRepository assetRepository;
    private final ContentAccessContext accessContext;
    private final FileSignatureContext signatureContext;

    public Page(ContentAssetRepository assetRepository, ContentAccessContext accessContext,
                FileSignatureContext signatureContext) {
        this.assetRepository = assetRepository;
        this.accessContext = accessContext;
        this.signatureContext = signatureContext;
    }

    @Override
    public org.springframework.data.domain.Page<ContentAsset> call(ContentAsset request) {
        Employee me = accessContext.requireEmployee(employee);
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        List<ContentAsset> source;
        if (projectId != null) {
            source = assetRepository.findByCompany_ComIdAndProject_ProjectIdOrderByAssetIdDesc(
                    me.getCompany().getComId(), projectId);
        } else {
            source = assetRepository.findByCompany_ComIdOrderByAssetIdDesc(me.getCompany().getComId());
        }
        List<ContentAsset> items = source.stream()
                .filter(item -> accessContext.isManager(me) || isUploaderOrParticipant(item, me))
                .filter(item -> status == null || item.getStatus() == status)
                .filter(item -> truthType == null || item.getTruthType() == truthType)
                .filter(item -> kw.isBlank() || contains(item.getTitle(), kw) || contains(item.getTags(), kw))
                .sorted(Comparator.comparing(ContentAsset::getAssetId, Comparator.nullsLast(Comparator.reverseOrder())))
                .peek(item -> item.setPreviewUrl(signatureContext.sign(item.getFilePath())))
                .toList();
        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, items.size());
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), PageRequest.of(page, pageSize), items.size());
    }

    private boolean isUploaderOrParticipant(ContentAsset item, Employee me) {
        if (item.getUploader() != null && item.getUploader().getEmpId().equals(me.getEmpId())) return true;
        if (item.getProject() == null) return false;
        Long empId = me.getEmpId();
        Project p = item.getProject();
        return same(p.getOwner(), empId) || same(p.getDesigner(), empId)
                || same(p.getManager(), empId) || same(p.getSupport(), empId);
    }

    private boolean same(Employee target, Long empId) {
        return target != null && target.getEmpId().equals(empId);
    }

    private boolean contains(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }
}

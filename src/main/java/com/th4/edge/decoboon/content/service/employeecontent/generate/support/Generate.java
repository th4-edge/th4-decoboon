package com.th4.edge.decoboon.content.service.employeecontent.generate.support;

import com.th4.edge.decoboon.common.file.context.FileSignatureContext;
import com.th4.edge.decoboon.content.context.ContentRenderContext;
import com.th4.edge.decoboon.content.configure.ContentAssetStatus;
import com.th4.edge.decoboon.content.configure.ContentMediaType;
import com.th4.edge.decoboon.content.configure.EmployeeContentStatus;
import com.th4.edge.decoboon.content.context.ContentAccessContext;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.model.ContentTask;
import com.th4.edge.decoboon.content.model.EmployeeContent;
import com.th4.edge.decoboon.content.repository.ContentAssetRepository;
import com.th4.edge.decoboon.content.repository.EmployeeContentRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.annotation.User;
import com.th4.edge.framework.provider.validation.annotation.Length;
import com.th4.edge.framework.provider.validation.annotation.NotEmpty;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Employee;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** 生成员工成片：P0 使用单条视频素材直通。 */
@Note("生成员工成片")
@Properties(allow = {
        @AllowProperties(model = EmployeeContent.class,
                properties = {"contentId", "status", "selectedAssetIds", "caption", "renderedFilePath", "coverFilePath",
                        "renderJobId", "renderError", "submittedAt", "reviewStatus", "renderedUrl", "coverUrl", "createTime", "updateTime", "task", "employee"}),
        @AllowProperties(model = ContentTask.class, properties = {"taskId", "title", "status", "materialIds"}),
        @AllowProperties(model = Employee.class, properties = {"empId", "name", "role"})
})
@Service("content/employee-content/generate")
@Scope(ScopeTypes.PROTOTYPE)
public class Generate implements com.th4.edge.decoboon.content.service.employeecontent.generate.Generate {
    @User private Employee employee;
    @AutoInject @NotNull private Long contentId;
    @AutoInject @NotEmpty private List<Long> selectedAssetIds;
    @AutoInject @NotEmpty @Length(max = 2000) private String caption;
    private final EmployeeContentRepository contentRepository;
    private final ContentAssetRepository assetRepository;
    private final ContentAccessContext accessContext;
    private final FileSignatureContext signatureContext;
    private final ContentRenderContext renderContext;

    public Generate(EmployeeContentRepository contentRepository, ContentAssetRepository assetRepository,
                    ContentAccessContext accessContext, FileSignatureContext signatureContext,
                    ContentRenderContext renderContext) {
        this.contentRepository = contentRepository; this.assetRepository = assetRepository;
        this.accessContext = accessContext; this.signatureContext = signatureContext;
        this.renderContext = renderContext;
    }

    @Override
    @Transactional
    public EmployeeContent call(EmployeeContent request) {
        Employee me = accessContext.requireEmployee(employee);
        EmployeeContent content = accessContext.requireEmployeeContent(contentId, me);
        if (!content.getEmployee().getEmpId().equals(me.getEmpId())) {
            throw new DefinedException(new DefaultErrorCode("403", "只能生成本人的内容"));
        }
        if (content.getTask().getStatus() == com.th4.edge.decoboon.content.configure.ContentTaskStatus.Closed) {
            throw new DefinedException(new DefaultErrorCode("409", "任务已关闭，不能生成内容"));
        }
        List<Long> taskMaterials = java.util.Arrays.stream(content.getTask().getMaterialIds().split(","))
                .map(String::trim).filter(s -> !s.isBlank()).map(Long::valueOf).toList();
        if (!taskMaterials.containsAll(selectedAssetIds)) {
            throw new DefinedException(new DefaultErrorCode("409", "所选素材必须来自任务素材包"));
        }
        List<ContentAsset> assets = assetRepository.findAllById(selectedAssetIds);
        if (assets.size() != selectedAssetIds.stream().distinct().count()) {
            throw new DefinedException(new DefaultErrorCode("404", "部分素材不存在或无权访问"));
        }
        for (ContentAsset asset : assets) {
            if (asset.getStatus() != ContentAssetStatus.PublicReady && asset.getStatus() != ContentAssetStatus.MarketingReady) {
                throw new DefinedException(new DefaultErrorCode("409", "素材授权状态不足，不能生成公开发布内容"));
            }
        }
        ContentRenderContext.RenderResult rendered = renderContext.render(
                me.getCompany().getComId(), assets, content.getTask().getTemplate(), caption.trim());

        content.setSelectedAssetIds(selectedAssetIds.stream().distinct().map(String::valueOf).collect(Collectors.joining(",")));
        content.setCaption(caption.trim());
        content.setRenderedFilePath(rendered.renderedFilePath());
        content.setCoverFilePath(rendered.coverFilePath());
        content.setRenderJobId(rendered.renderJobId());
        content.setRenderError(null);
        content.setStatus(EmployeeContentStatus.PendingReview);
        content.setReviewStatus("Pending");
        content.setSubmittedAt(LocalDateTime.now());
        EmployeeContent saved = contentRepository.save(content);
        saved.setRenderedUrl(signatureContext.sign(saved.getRenderedFilePath()));
        saved.setCoverUrl(signatureContext.sign(saved.getCoverFilePath()));
        return saved;
    }
}

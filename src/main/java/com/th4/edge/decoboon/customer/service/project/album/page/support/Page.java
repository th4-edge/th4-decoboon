package com.th4.edge.decoboon.customer.service.project.album.page.support;

import com.th4.edge.decoboon.customer.context.CustomerAccessContext;
import com.th4.edge.decoboon.customer.model.ProjectAlbumEntry;
import com.th4.edge.decoboon.project.model.Acceptance;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.AcceptanceRepository;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.security.User;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Credentials;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 业主工地相册实现。 */
@Note("业主工地相册")
@Properties(allow = {
        @AllowProperties(model = ProjectAlbumEntry.class, properties = {
                "key", "source", "businessId", "type", "typeName", "title", "content", "images",
                "authorName", "authorRole", "stageName", "statusName", "occurredAt"})
})
@Service("customer/project/album/page")
@Scope(ScopeTypes.PROTOTYPE)
public class Page implements com.th4.edge.decoboon.customer.service.project.album.page.Page {

    /** 一次最多聚合的原始记录数，避免单请求拉全量；分页在城市端展示层完成。 */
    private static final int MAX_SOURCE = 200;

    // 注意：@User 注解在 annotation 包，User 类型在 security 包，二者同名不同物
    @com.th4.edge.framework.provider.security.annotation.User
    private User<?> principal;

    @AutoInject
    @NotNull
    private Long projectId;

    @AutoInject
    private Integer current;

    @AutoInject
    private Integer size;

    private final CustomerAccessContext accessContext;
    private final ProjectReportRepository reportRepository;
    private final AcceptanceRepository acceptanceRepository;

    public Page(CustomerAccessContext accessContext,
                ProjectReportRepository reportRepository,
                AcceptanceRepository acceptanceRepository) {
        this.accessContext = accessContext;
        this.reportRepository = reportRepository;
        this.acceptanceRepository = acceptanceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ProjectAlbumEntry> call(Project request) {
        Credentials credential = accessContext.requireCredential(principal);
        Project project = accessContext.requireProject(credential, projectId);
        Long comId = project.getCompany().getComId();

        List<ProjectAlbumEntry> entries = new ArrayList<>();
        reportRepository
                .findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                        comId, projectId, PageRequest.of(0, MAX_SOURCE))
                .forEach(report -> entries.add(reportEntry(report)));
        acceptanceRepository.findByProject_ProjectIdOrderByAcceptanceIdDesc(projectId)
                .forEach(acceptance -> entries.add(acceptanceEntry(acceptance)));

        entries.sort(Comparator.comparing(ProjectAlbumEntry::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int page = current == null || current < 1 ? 0 : current - 1;
        int pageSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = Math.min(page * pageSize, entries.size());
        int to = Math.min(from + pageSize, entries.size());
        return new PageImpl<>(entries.subList(from, to), PageRequest.of(page, pageSize), entries.size());
    }

    /** 现场汇报：照片 + 落款人（姓名/角色）+ 所属阶段。 */
    private ProjectAlbumEntry reportEntry(ProjectReport report) {
        ProjectAlbumEntry entry = new ProjectAlbumEntry();
        entry.setKey("report-" + report.getReportId());
        entry.setSource("Report");
        entry.setBusinessId(report.getReportId());
        entry.setType(report.getType() == null ? "Progress" : report.getType().name());
        entry.setTypeName(report.getType() == null ? "工地动态" : report.getType().getName());
        entry.setTitle(report.getTitle());
        entry.setContent(report.getContent());
        entry.setImages(report.getImages() == null ? new ArrayList<>() : new ArrayList<>(report.getImages()));
        entry.setAuthorName(report.getAuthor() == null ? null : report.getAuthor().getName());
        entry.setAuthorRole(report.getAuthorRole() == null ? null : report.getAuthorRole().getName());
        entry.setStageName(report.getStage() == null ? null : report.getStage().getName());
        entry.setOccurredAt(report.getCreateTime() == null ? null : report.getCreateTime().toLocalDateTime());
        return entry;
    }

    /** 阶段验收：验收影像 + 整改问题图片，结论优先、问题清单兜底。 */
    private ProjectAlbumEntry acceptanceEntry(Acceptance acceptance) {
        ProjectAlbumEntry entry = new ProjectAlbumEntry();
        entry.setKey("acceptance-" + acceptance.getAcceptanceId());
        entry.setSource("Acceptance");
        entry.setBusinessId(acceptance.getAcceptanceId());
        entry.setType("Acceptance");
        String statusName = acceptance.getStatus() == null ? null : acceptance.getStatus().getName();
        entry.setTypeName(statusName == null ? "阶段验收" : "验收 · " + statusName);
        entry.setStatusName(statusName);
        String stageName = acceptance.getStage() == null ? null : acceptance.getStage().getName();
        entry.setStageName(stageName);
        entry.setTitle((stageName == null ? "阶段" : stageName) + "验收单");

        String content = acceptance.getConclusion();
        if (content == null || content.isBlank()) {
            content = acceptance.getIssues() == null || acceptance.getIssues().isEmpty()
                    ? null : String.join("；", acceptance.getIssues());
        }
        entry.setContent(content);
        entry.setImages(acceptanceImages(acceptance));
        entry.setAuthorName(acceptance.getSubmittedBy() == null ? null : acceptance.getSubmittedBy().getName());
        entry.setOccurredAt(acceptance.getCreateTime() == null ? null : acceptance.getCreateTime().toLocalDateTime());
        return entry;
    }

    private List<String> acceptanceImages(Acceptance acceptance) {
        List<String> images = new ArrayList<>();
        if (acceptance.getEvidenceUrls() != null) {
            images.addAll(acceptance.getEvidenceUrls());
        }
        if (acceptance.getRectificationIssueImages() != null) {
            for (String url : acceptance.getRectificationIssueImages()) {
                if (!images.contains(url)) {
                    images.add(url);
                }
            }
        }
        return images;
    }
}

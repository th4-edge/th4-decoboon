package com.th4.edge.decoboon.customer.service.shareview.info.support;

import com.th4.edge.decoboon.customer.configure.ShareScope;
import com.th4.edge.decoboon.customer.configure.ShareStatus;
import com.th4.edge.decoboon.customer.model.CustomerJourney;
import com.th4.edge.decoboon.customer.model.ProjectShareLink;
import com.th4.edge.decoboon.customer.model.ShareJourneyView;
import com.th4.edge.decoboon.customer.repository.ProjectShareLinkRepository;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.model.ProjectReport;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.framework.core.ScopeTypes;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.framework.provider.inject.annotation.AutoInject;
import com.th4.edge.framework.provider.validation.annotation.NotNull;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.model.Setting;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 亲友只读视图实现。
 *
 * 脱敏由**服务端**完成（ECD-011：对外可见投影必须显式组装，不得靠前端隐藏）：
 * <ul>
 *   <li>房号不返回，只给「小区 + 楼栋」；</li>
 *   <li>客户姓名、电话一律不返回；</li>
 *   <li>金额按 scope 控制：Friend 恒不返回；</li>
 *   <li>成本、利润、供应商、内部操作人无投影入口。</li>
 * </ul>
 */
@Note("亲友只读视图")
@Properties(allow = {
        @AllowProperties(model = ShareJourneyView.class, properties = {
                "projectName", "community", "progress", "currentStageName", "photos", "vendor", "asOf"}),
        @AllowProperties(model = CustomerJourney.Vendor.class, properties = {
                "name", "logo", "color", "custodianCredit", "serviceArea", "serviceTel", "afterSaleTel",
                "uscc", "qualification", "intro"})
})
@Service("customer/shareview/info")
@Scope(ScopeTypes.PROTOTYPE)
public class Info implements com.th4.edge.decoboon.customer.service.shareview.info.Info {

    private static final int PHOTO_PAGE_SIZE = 6;

    @AutoInject
    @NotNull
    private String token;

    private final ProjectShareLinkRepository linkRepository;
    private final ProjectReportRepository reportRepository;

    public Info(ProjectShareLinkRepository linkRepository, ProjectReportRepository reportRepository) {
        this.linkRepository = linkRepository;
        this.reportRepository = reportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ShareJourneyView call(ProjectShareLink request) {
        ProjectShareLink link = linkRepository.findByToken(token).orElse(null);
        // 不区分「不存在 / 已撤销 / 已过期」：统一返回空，避免用错误差异探测项目是否存在
        if (link == null || link.getStatus() != ShareStatus.Active) {
            return null;
        }
        if (link.getExpireAt() != null && link.getExpireAt().isBefore(LocalDateTime.now())) {
            return null;
        }

        Project project = link.getProject();
        Company company = project.getCompany();
        Property property = project.getProperty();
        Long comId = company.getComId();

        ShareJourneyView view = new ShareJourneyView();
        view.setProjectName(project.getName());
        view.setCommunity(communityOf(property));
        view.setProgress(project.getProgress() == null ? 0 : project.getProgress());
        view.setCurrentStageName(project.getCurrentStageName());
        view.setAsOf(LocalDateTime.now());
        view.setVendor(vendorOf(company));

        reportRepository
                .findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                        comId, project.getProjectId(), PageRequest.of(0, PHOTO_PAGE_SIZE))
                .getContent()
                .stream()
                .map(ProjectReport::getImages)
                .filter(images -> images != null)
                .forEach(images -> view.getPhotos().addAll(images));
        return view;
    }

    /** 只给「小区 + 楼栋」，不含房号（房号属可定位到户的敏感信息）。 */
    private String communityOf(Property property) {
        if (property == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (property.getCommunity() != null) sb.append(property.getCommunity());
        if (property.getBuilding() != null) sb.append(sb.length() == 0 ? "" : " ").append(property.getBuilding());
        return sb.length() == 0 ? null : sb.toString();
    }

    private CustomerJourney.Vendor vendorOf(Company company) {
        CustomerJourney.Vendor vendor = new CustomerJourney.Vendor();
        if (company == null) {
            return vendor;
        }
        vendor.setName(company.getTitle() == null ? company.getName() : company.getTitle());
        vendor.setLogo(company.getLogo());
        vendor.setColor(company.getColor());
        Setting setting = company.getSetting();
        if (setting != null) {
            vendor.setCustodianCredit(Boolean.TRUE.equals(setting.getCustodianCredit()));
            vendor.setServiceArea(setting.getServiceArea());
            vendor.setServiceTel(setting.getServiceTel());
            vendor.setAfterSaleTel(setting.getAfterSaleTel());
            vendor.setUscc(setting.getUscc());
            vendor.setQualification(setting.getQualification());
            vendor.setIntro(setting.getIntro());
        }
        return vendor;
    }
}

package com.th4.edge.decoboon.customer;

import com.th4.edge.decoboon.customer.configure.ShareScope;
import com.th4.edge.decoboon.customer.configure.ShareStatus;
import com.th4.edge.decoboon.customer.model.ProjectShareLink;
import com.th4.edge.decoboon.customer.model.ShareJourneyView;
import com.th4.edge.decoboon.customer.repository.ProjectShareLinkRepository;
import com.th4.edge.decoboon.customer.service.shareview.info.support.Info;
import com.th4.edge.decoboon.project.model.Project;
import com.th4.edge.decoboon.project.repository.ProjectReportRepository;
import com.th4.edge.decoboon.property.model.Property;
import com.th4.edge.tenant.user.model.Company;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 亲友免登录通道的安全用例。
 *
 * 这是本批唯一的权限类变更（新增未登录可读入口），因此安全断言必须显式覆盖：
 * 令牌不存在 / 已撤销 / 已过期一律返回空，且成功路径不得泄漏房号。
 */
class ShareViewInfoTest {

    private final ProjectShareLinkRepository linkRepository = mock(ProjectShareLinkRepository.class);
    private final ProjectReportRepository reportRepository = mock(ProjectReportRepository.class);
    private final Info info = new Info(linkRepository, reportRepository);

    private ProjectShareLink link(ShareStatus status, LocalDateTime expireAt) {
        Company company = new Company();
        company.setComId(1L);
        company.setTitle("测试装饰");
        company.setName("测试");

        Property property = new Property();
        property.setPropertyId(3L);
        property.setCommunity("滨湖云谷");
        property.setBuilding("3 栋");
        property.setRoomNo("1602");

        Project project = new Project();
        project.setProjectId(2L);
        project.setName("测试项目");
        project.setCompany(company);
        project.setProperty(property);
        project.setProgress(37);

        ProjectShareLink link = new ProjectShareLink();
        link.setShareId(9L);
        link.setToken("tok");
        link.setScope(ShareScope.Friend);
        link.setShowAmount(Boolean.FALSE);
        link.setStatus(status);
        link.setExpireAt(expireAt);
        link.setCompany(company);
        link.setProject(project);
        return link;
    }

    private void givenToken(String token) {
        ReflectionTestUtils.setField(info, "token", token);
    }

    private void givenNoReports() {
        when(reportRepository
                .findByCompany_ComIdAndProject_ProjectIdAndVisibleToCustomerTrueOrderByCreateTime_DateTimeDesc(
                        anyLong(), anyLong(), any()))
                .thenReturn(Page.empty());
    }

    @Test
    void unknownTokenMustReturnNull() {
        givenToken("unknown");
        when(linkRepository.findByToken(anyString())).thenReturn(Optional.empty());

        assertThat(info.call(new ProjectShareLink())).isNull();
    }

    @Test
    void revokedTokenMustReturnNull() {
        givenToken("tok");
        when(linkRepository.findByToken("tok"))
                .thenReturn(Optional.of(link(ShareStatus.Revoked, LocalDateTime.now().plusDays(7))));

        assertThat(info.call(new ProjectShareLink())).as("撤销后必须立即失效").isNull();
    }

    @Test
    void expiredTokenMustReturnNull() {
        givenToken("tok");
        when(linkRepository.findByToken("tok"))
                .thenReturn(Optional.of(link(ShareStatus.Active, LocalDateTime.now().minusMinutes(1))));

        assertThat(info.call(new ProjectShareLink())).as("过期后必须失效").isNull();
    }

    @Test
    void validTokenMustReturnViewWithoutRoomNo() {
        givenToken("tok");
        givenNoReports();
        when(linkRepository.findByToken("tok"))
                .thenReturn(Optional.of(link(ShareStatus.Active, LocalDateTime.now().plusDays(7))));

        ShareJourneyView view = info.call(new ProjectShareLink());

        assertThat(view).isNotNull();
        assertThat(view.getProjectName()).isEqualTo("测试项目");
        assertThat(view.getVendor().getName()).isEqualTo("测试装饰");
        // 脱敏：只给小区 + 楼栋，绝不带房号
        assertThat(view.getCommunity()).isEqualTo("滨湖云谷 3 栋");
        assertThat(view.getCommunity()).doesNotContain("1602");
    }
}

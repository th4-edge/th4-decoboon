package com.th4.edge.tenant.user;

import com.th4.edge.framework.data.jackson.annotation.AllowProperties;
import com.th4.edge.framework.data.jackson.annotation.Properties;
import com.th4.edge.tenant.user.model.Company;
import com.th4.edge.tenant.user.service.auth.session.register.support.Register;
import com.th4.edge.tenant.user.service.company.company.info.support.Info;
import com.th4.edge.tenant.user.service.company.company.update.support.Update;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 企业资料 Provider 响应契约。
 *
 * 历史坑：Company.createTime 是框架 DateTime 复合对象，未声明其嵌套字段白名单时会输出
 * `{"dateTime"}` 这种非法 JSON，导致企业资料页完全无法加载。企业资料页不需要创建时间，
 * 因此 Provider 不得暴露 createTime；品牌色与设置必须保留。
 */
class CompanyResponseWhitelistTest {

    @Test
    void companyProvidersKeepBrandColorAndDoNotExposeBrokenCreateTimeProjection() {
        for (Class<?> provider : Arrays.asList(Info.class, Update.class, Register.class)) {
            Properties properties = provider.getAnnotation(Properties.class);
            assertThat(properties).as(provider.getSimpleName() + " 缺少 @Properties").isNotNull();
            AllowProperties company = Arrays.stream(properties.allow())
                    .filter(item -> item.model().equals(Company.class))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(provider.getSimpleName() + " 缺少 Company 白名单"));
            assertThat(company.properties())
                    .as(provider.getSimpleName() + " 必须保留品牌色")
                    .contains("color", "setting")
                    .as(provider.getSimpleName() + " 不得输出无法完整序列化的 Company.createTime")
                    .doesNotContain("createTime");
        }
    }
}

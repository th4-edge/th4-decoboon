package com.th4.edge.site.configure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** 将发布包中的 site 静态分包映射到 /site/**。 */
@Configuration
public class SiteWebConfigure implements WebMvcConfigurer {

    private final String siteDir;

    public SiteWebConfigure(@Value("${th4.edge.site-dir:./site/}") String siteDir) {
        this.siteDir = siteDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(siteDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/site/**").addResourceLocations(location);
    }
}

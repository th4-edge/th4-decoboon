package com.th4.edge.decoboon.common.configure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/** 仅显式 public-dir 可匿名访问；业务 upload-dir 不再注册静态资源。 */
@Configuration
public class PublicFileWebConfigure implements WebMvcConfigurer {

    private final String publicDir;

    public PublicFileWebConfigure(@Value("${th4.edge.framework.public-dir:./data/public/}") String publicDir) {
        this.publicDir = publicDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(publicDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/public/**").addResourceLocations(location);
    }
}

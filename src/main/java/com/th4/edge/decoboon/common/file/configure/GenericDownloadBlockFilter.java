package com.th4.edge.decoboon.common.file.configure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

/** 关闭框架通用 {modelName}/download/{property} 任意路径下载入口。 */
@Component
public class GenericDownloadBlockFilter extends OncePerRequestFilter {

    private static final Pattern GENERIC_DOWNLOAD =
            Pattern.compile("^.*/[^/]+/download/[^/]+/?$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (GENERIC_DOWNLOAD.matcher(request.getRequestURI()).matches()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        filterChain.doFilter(request, response);
    }
}

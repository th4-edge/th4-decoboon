package com.th4.edge.site.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 官网入口以 UTF-8 和 zh-CN 响应，供搜索引擎与生成式检索稳定识别。 */
@Controller
public class SiteEntryController {

    private final Path siteDir;

    public SiteEntryController(@Value("${th4.edge.site-dir:./site/}") String siteDir) {
        this.siteDir = Paths.get(siteDir).toAbsolutePath().normalize();
    }

    @GetMapping(value = {"/site", "/site/", "/site/index.html"}, produces = "text/html;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<byte[]> index() throws IOException {
        byte[] content = readSiteFile("index.html");
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .header("Content-Language", "zh-CN")
                .cacheControl(CacheControl.noCache())
                .body(content);
    }

    @GetMapping(value = "/site/site.webmanifest", produces = "application/manifest+json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<byte[]> manifest() throws IOException {
        byte[] content = readSiteFile("site.webmanifest");
        if (content == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(new MediaType("application", "manifest+json", StandardCharsets.UTF_8))
                .header("Content-Language", "zh-CN")
                .cacheControl(CacheControl.noCache())
                .body(content);
    }

    private byte[] readSiteFile(String filename) throws IOException {
        Path file = siteDir.resolve(filename).normalize();
        if (!file.startsWith(siteDir) || !Files.isRegularFile(file)) {
            return null;
        }
        return Files.readAllBytes(file);
    }
}

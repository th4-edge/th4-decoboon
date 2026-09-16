package com.th4.edge.decoboon.design.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.decoboon.common.file.configure.FileScope;
import com.th4.edge.decoboon.common.file.context.ObjectKeyContext;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.framework.core.annotation.Note;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 把谈单页结构渲染为 16:9 PPTX 文件。 */
@Note("谈单PPT导出")
@Component
public class TalkPptContext {

    private static final Logger log = LoggerFactory.getLogger(TalkPptContext.class);
    private static final Color BRAND = new Color(22, 163, 148);
    private static final Color TITLE = new Color(31, 41, 55);
    private static final Color BODY = new Color(75, 85, 99);

    private final ObjectStorage storage;
    private final ObjectKeyContext keyContext;
    private final Path legacyUploadDir;
    private final String legacyUrlPrefix;

    @Autowired
    public TalkPptContext(ObjectStorage storage, ObjectKeyContext keyContext) {
        this.storage = storage;
        this.keyContext = keyContext;
        this.legacyUploadDir = null;
        this.legacyUrlPrefix = null;
    }

    /** 兼容旧单元测试的本地导出构造器；生产装配只使用 ObjectStorage 构造器。 */
    public TalkPptContext(String uploadDir, String urlPrefix) {
        this.storage = null;
        this.keyContext = null;
        this.legacyUploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.legacyUrlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
    }

    /** 生成文件并返回可访问地址；单个图片失败不影响整份谈单方案。 */
    public String export(Talk talk, List<TalkSection> sections) {
        String name = "talk-" + talk.getTalkId() + "-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".pptx";
        if (storage == null) return exportLegacy(talk, sections, name);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (XMLSlideShow ppt = new XMLSlideShow()) {
                ppt.setPageSize(new Dimension(1280, 720));
                for (TalkSection section : sections) {
                    appendSlide(ppt, section);
                }
                ppt.write(output);
            }
            byte[] bytes = output.toByteArray();
            if (talk.getCompany() == null) throw new IllegalArgumentException("谈单方案缺少企业归属");
            String key = keyContext.generated(talk.getCompany().getComId(), null, FileScope.Talk, name);
            try (InputStream input = new ByteArrayInputStream(bytes)) {
                storage.put(key, input, bytes.length,
                        "application/vnd.openxmlformats-officedocument.presentationml.presentation");
            }
            return key;
        } catch (Exception ex) {
            log.warn("谈单PPT导出失败，talkId={}", talk.getTalkId(), ex);
            return null;
        }
    }

    private String exportLegacy(Talk talk, List<TalkSection> sections, String name) {
        Path directory = legacyUploadDir.resolve("talk");
        Path target = directory.resolve(name).normalize();
        if (!target.startsWith(directory)) throw new IllegalArgumentException("非法的谈单导出文件名");
        try {
            Files.createDirectories(directory);
            try (XMLSlideShow ppt = new XMLSlideShow()) {
                ppt.setPageSize(new Dimension(1280, 720));
                for (TalkSection section : sections) appendSlide(ppt, section);
                try (FileOutputStream output = new FileOutputStream(target.toFile())) {
                    ppt.write(output);
                }
            }
            return legacyUrlPrefix + "/talk/" + name;
        } catch (Exception ex) {
            log.warn("谈单PPT导出失败，talkId={}", talk.getTalkId(), ex);
            return null;
        }
    }

    private void appendSlide(XMLSlideShow ppt, TalkSection section) {
        XSLFSlide slide = ppt.createSlide();
        addText(slide, section.getTitle(), 54, 60, 1160, 80, TITLE, true, TextParagraph.TextAlign.LEFT);
        if (section.getSubtitle() != null && !section.getSubtitle().isBlank()) {
            addText(slide, section.getSubtitle(), 54, 145, 1160, 60, BRAND, false, TextParagraph.TextAlign.LEFT);
        }

        double contentTop = section.getImages() == null || section.getImages().isEmpty() ? 220 : 250;
        if (section.getContent() != null && !section.getContent().isBlank()) {
            double contentWidth = section.getImages() == null || section.getImages().isEmpty() ? 1160 : 650;
            addText(slide, section.getContent(), 54, contentTop, contentWidth, 390, BODY, false,
                    TextParagraph.TextAlign.LEFT);
        }
        if (section.getImages() != null && !section.getImages().isEmpty()) {
            appendImages(ppt, slide, section.getImages());
        }
    }

    private void appendImages(XMLSlideShow ppt, XSLFSlide slide, List<String> images) {
        int max = Math.min(images.size(), 4);
        for (int i = 0; i < max; i++) {
            try (InputStream input = openImage(images.get(i))) {
                if (input == null) {
                    continue;
                }
                XSLFPictureData data = ppt.addPicture(input, PictureData.PictureType.PNG);
                XSLFPictureShape picture = slide.createPicture(data);
                double x = 730 + (i % 2) * 245;
                double y = 215 + (i / 2) * 220;
                picture.setAnchor(new Rectangle2D.Double(x, y, 220, 195));
            } catch (Exception ex) {
                log.debug("跳过无法读取的谈单图片：{}", images.get(i), ex);
            }
        }
    }

    private InputStream openImage(String url) {
        try {
            if (url == null || url.isBlank()) {
                return null;
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                URLConnection connection = URI.create(url).toURL().openConnection();
                connection.setConnectTimeout(3000);
                connection.setReadTimeout(5000);
                return connection.getInputStream();
            }
            String normalized = url.startsWith("/upload/") || url.startsWith("upload/")
                    ? (url.startsWith("/") ? url.substring(1) : url).replaceFirst("^upload/", "")
                    : url;
            if (storage == null) {
                Path path = Paths.get(normalized);
                return Files.exists(path) ? Files.newInputStream(path) : null;
            }
            return storage.exists(normalized) ? storage.get(normalized) : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private void addText(XSLFSlide slide, String text, double x, double y, double width, double height,
                         Color color, boolean bold, TextParagraph.TextAlign align) {
        XSLFTextBox box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, width, height));
        box.clearText();
        String[] lines = text == null ? new String[]{""} : text.split("\\R");
        for (String line : lines) {
            XSLFTextParagraph paragraph = box.addNewTextParagraph();
            paragraph.setTextAlign(align);
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(line);
            run.setFontFamily("Microsoft YaHei");
            run.setFontSize(bold ? 38d : 22d);
            run.setFontColor(color);
            run.setBold(bold);
        }
    }
}

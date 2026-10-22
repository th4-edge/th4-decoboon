package com.th4.edge.decoboon.content.context;

import com.th4.edge.components.storage.service.ObjectStorage;
import com.th4.edge.decoboon.content.model.ContentAsset;
import com.th4.edge.decoboon.content.model.ContentTemplate;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.YearMonth;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 内容成片服务端渲染：多视频片段拼接、竖版统一、字幕层和封面。 */
@Component
public class ContentRenderContext {

    public record RenderResult(String renderedFilePath, String coverFilePath, String renderJobId) {}

    private final ObjectStorage storage;
    private final String ffmpegBin;
    private final String fontPath;

    public ContentRenderContext(ObjectStorage storage,
                                @Value("${th4.edge.content.ffmpeg-bin:ffmpeg}") String ffmpegBin,
                                @Value("${th4.edge.content.font-path:/System/Library/Fonts/Supplemental/Arial Unicode.ttf}") String fontPath) {
        this.storage = storage;
        this.ffmpegBin = ffmpegBin;
        this.fontPath = fontPath;
    }

    public RenderResult render(Long companyId, List<ContentAsset> assets, ContentTemplate template, String caption) {
        List<ContentAsset> videos = assets.stream().filter(a -> a.getMediaType() == com.th4.edge.decoboon.content.configure.ContentMediaType.Video).toList();
        if (videos.isEmpty()) {
            throw new DefinedException(new DefaultErrorCode("409", "请至少选择一条视频素材"));
        }
        if (assets.stream().anyMatch(a -> a.getMediaType() != com.th4.edge.decoboon.content.configure.ContentMediaType.Video)) {
            throw new DefinedException(new DefaultErrorCode("409", "当前仅支持使用视频片段生成成片，请移除图片素材"));
        }
        if (template != null && (videos.size() < template.getClipMin() || videos.size() > template.getClipMax())) {
            throw new DefinedException(new DefaultErrorCode("409", "素材片段数量不符合模板要求"));
        }
        Path work = null;
        try {
            work = Files.createTempDirectory("decoboon-content-render-");
            List<Path> inputs = new ArrayList<>();
            for (int i = 0; i < videos.size(); i++) {
                Path source = work.resolve("source-" + i + ".mp4");
                try (InputStream input = storage.get(videos.get(i).getFilePath())) {
                    Files.copy(input, source);
                }
                inputs.add(source);
            }
            Path subtitle = work.resolve("subtitle.png");
            renderSubtitlePng(caption, subtitle);
            Path output = work.resolve("rendered.mp4");
            runFfmpeg(inputs, subtitle, output);
            Path cover = work.resolve("cover.jpg");
            runCover(output, cover);

            String base = "companies/" + companyId + "/content/render/" + YearMonth.now() + "/" + UUID.randomUUID();
            String renderedKey = base + ".mp4";
            String coverKey = base + "-cover.jpg";
            try (InputStream input = Files.newInputStream(output)) {
                storage.put(renderedKey, input, Files.size(output), "video/mp4");
            }
            try (InputStream input = Files.newInputStream(cover)) {
                storage.put(coverKey, input, Files.size(cover), "image/jpeg");
            }
            return new RenderResult(renderedKey, coverKey, "ffmpeg-" + UUID.randomUUID());
        } catch (DefinedException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DefinedException(new DefaultErrorCode("500", "成片渲染失败：" + ex.getMessage()));
        } finally {
            if (work != null) deleteRecursively(work.toFile());
        }
    }

    private void runFfmpeg(List<Path> inputs, Path subtitle, Path output) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(ffmpegBin); command.add("-y");
        for (Path input : inputs) command.addAll(List.of("-i", input.toAbsolutePath().toString()));
        command.addAll(List.of("-i", subtitle.toAbsolutePath().toString()));

        StringBuilder filter = new StringBuilder();
        for (int i = 0; i < inputs.size(); i++) {
            filter.append("[").append(i).append(":v]")
                    .append("scale=1080:1920:force_original_aspect_ratio=increase,")
                    .append("crop=1080:1920,trim=duration=4,setpts=PTS-STARTPTS,fps=30,setsar=1[v").append(i).append("];");
        }
        for (int i = 0; i < inputs.size(); i++) filter.append("[v").append(i).append("]");
        filter.append("concat=n=").append(inputs.size()).append(":v=1:a=0[base];");
        filter.append("[").append(inputs.size()).append(":v]format=rgba[sub];[base][sub]overlay=0:1450:format=auto[outv]");
        command.addAll(List.of(
                "-filter_complex", filter.toString(),
                "-map", "[outv]",
                "-c:v", "libx264",
                "-preset", "veryfast",
                "-crf", "23",
                "-pix_fmt", "yuv420p",
                "-movflags", "+faststart",
                "-an",
                output.toAbsolutePath().toString()));
        run(command, Duration.ofMinutes(10));
    }

    private void runCover(Path video, Path cover) throws Exception {
        run(List.of(ffmpegBin, "-y", "-ss", "0.5", "-i", video.toAbsolutePath().toString(),
                "-frames:v", "1", "-vf", "scale=1080:1920", cover.toAbsolutePath().toString()), Duration.ofMinutes(2));
    }

    private void run(List<String> command, Duration timeout) throws Exception {
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        byte[] output = process.getInputStream().readAllBytes();
        if (!process.waitFor(timeout.toSeconds(), TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new DefinedException(new DefaultErrorCode("500", "FFmpeg 执行超时"));
        }
        if (process.exitValue() != 0) {
            String message = new String(output);
            throw new DefinedException(new DefaultErrorCode("500", "FFmpeg 执行失败：" + message.substring(Math.max(0, message.length() - 500))));
        }
    }

    private void renderSubtitlePng(String caption, Path target) throws Exception {
        int width = 1080;
        int height = 360;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setComposite(AlphaComposite.Src);
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(30, 30, width - 60, height - 60, 36, 36);
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, 50);
        try {
            File fontFile = Paths.get(fontPath).toFile();
            if (fontFile.isFile()) font = Font.createFont(Font.TRUETYPE_FONT, fontFile).deriveFont(Font.BOLD, 50f);
        } catch (Exception ignored) {
            // 字体不可用时回退系统字体。
        }
        g.setFont(font);
        g.setColor(Color.WHITE);
        FontMetrics metrics = g.getFontMetrics();
        List<String> lines = wrap(caption == null ? "" : caption.trim(), metrics, width - 140);
        int totalHeight = lines.size() * metrics.getHeight();
        int y = (height - totalHeight) / 2 + metrics.getAscent();
        for (String line : lines) {
            int x = (width - metrics.stringWidth(line)) / 2;
            g.drawString(line, x, y);
            y += metrics.getHeight();
        }
        g.dispose();
        ImageIO.write(image, "png", target.toFile());
    }

    private List<String> wrap(String text, FontMetrics metrics, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (ch == '\n' || metrics.stringWidth(line.toString() + ch) > maxWidth) {
                if (line.length() > 0) lines.add(line.toString());
                line.setLength(0);
                if (ch == '\n') continue;
            }
            line.append(ch);
            if (lines.size() >= 4) break;
        }
        if (line.length() > 0 && lines.size() < 4) lines.add(line.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private void deleteRecursively(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteRecursively(child);
        // 临时渲染文件允许清理失败，不回滚已经写入对象存储的成片。
        file.delete();
    }
}

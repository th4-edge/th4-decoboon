package com.th4.edge.decoboon.design;

import com.th4.edge.decoboon.design.context.TalkPptContext;
import com.th4.edge.decoboon.design.model.Talk;
import com.th4.edge.decoboon.design.model.TalkSection;
import com.th4.edge.decoboon.design.configure.TalkSectionType;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TalkPptContextTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExportValidPptxFile() throws Exception {
        Talk talk = new Talk();
        talk.setTalkId(42L);
        TalkSection cover = section(TalkSectionType.Cover, "王女士爱居设计方案", "现代简约");
        TalkSection investment = section(TalkSectionType.Investment, "装修投入", "报价合计：129800元");
        TalkPptContext context = new TalkPptContext(tempDir.toString(), "/assets");

        String url = context.export(talk, List.of(cover, investment));

        assertThat(url).startsWith("/assets/talk/talk-42-").endsWith(".pptx");
        Path file = tempDir.resolve(url.substring("/assets/".length()));
        assertThat(file).exists();
        assertThat(Files.size(file)).isGreaterThan(1000);
        try (XMLSlideShow ppt = new XMLSlideShow(Files.newInputStream(file))) {
            assertThat(ppt.getSlides()).hasSize(2);
        }
    }

    private TalkSection section(TalkSectionType type, String title, String content) {
        TalkSection section = new TalkSection();
        section.setType(type);
        section.setTitle(title);
        section.setContent(content);
        section.setImages(List.of());
        return section;
    }
}

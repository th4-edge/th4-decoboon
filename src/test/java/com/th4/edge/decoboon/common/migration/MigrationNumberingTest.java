package com.th4.edge.decoboon.common.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 迁移编号唯一性守卫。
 *
 * <p>2026-10-05 事故：`64-报价工程量公式.sql` 与 `64-AI调用与影子用量底账.sql` 同号，
 * 人工选号时漏跑，导致 dev 任何版本都无法在生产启动（缺 quote_line.quantity_formula）。
 * 这里把编号唯一性固化成测试，避免再次出现同号漏跑。</p>
 */
class MigrationNumberingTest {

    /**
     * 已知历史同号迁移：内容不同、早已应用且被文档引用，保持原编号不改名。
     * 新增迁移若与既有编号重复，必须改名，不得加入本集合。
     */
    private static final Set<String> KNOWN_SHARED_NUMBERS = Set.of("24", "25");

    private static final Path MIGRATIONS_DIR = Path.of("docs/design/migrations");

    @Test
    void shouldNotIntroduceNewDuplicateMigrationNumbers() throws IOException {
        Map<String, List<String>> byNumber = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(MIGRATIONS_DIR)) {
            files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("^\\d+-.*"))
                    .sorted()
                    .forEach(name -> byNumber
                            .computeIfAbsent(name.substring(0, name.indexOf('-')), key -> new ArrayList<>())
                            .add(name));
        }

        assertThat(byNumber).as("未找到迁移文件，路径可能变了: " + MIGRATIONS_DIR).isNotEmpty();

        List<String> unexpected = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : byNumber.entrySet()) {
            if (entry.getValue().size() > 1 && !KNOWN_SHARED_NUMBERS.contains(entry.getKey())) {
                unexpected.add(entry.getKey() + " -> " + entry.getValue());
            }
        }

        assertThat(unexpected)
                .as("迁移编号重复会让发布包同时打包多份同号脚本，极易漏跑。请给新增迁移换号：%s", unexpected)
                .isEmpty();
    }
}

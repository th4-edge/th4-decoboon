package com.th4.edge.decoboon.common.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 手写迁移 DDL 与 JPA 实体的对账。
 *
 * <p><b>为什么需要它</b>：本地与测试环境都是 H2 + {@code ddl-auto=create-drop}，表由实体生成，
 * 因此「手写 DDL 漏列」在本地**永远不会暴露**；全量测试再绿也覆盖不到这一类差异。
 * 唯一能发现它的地方是生产启动时的 {@code ddl-auto=validate}——代价最高的一道关卡。
 *
 * <p>2026-10-15 事故：迁移 116 建 {@code cust_project_share_link} 时漏了 10 个 {@code update_*}
 * 审计列（实体继承 {@code AuditModel} 需要 create/update 两组齐全），生产报
 * {@code Schema-validation: missing column [update_date]} 并**启动崩溃循环**。
 * 本测试就是为了在提交前挡住它。
 *
 * <p><b>做法</b>：把两边都当纯数据比。
 * <ul>
 *   <li>实体真值：H2 由实体建表后，读 {@code information_schema.columns}；</li>
 *   <li>迁移真值：解析 {@code docs/design/migrations/*.sql}（含 01 空库基线），
 *       累积 {@code CREATE TABLE} 与 {@code ALTER TABLE ... ADD COLUMN} 的列。</li>
 * </ul>
 * 只对**迁移脚本里出现过**的表做断言，避免把基线之前的历史表算进来。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:migration-schema-parity;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "th4.edge.components.lock.enabled=false",
        "th4.edge.components.wechat.pay.enabled=false",
        "logging.level.org.hibernate.SQL=OFF"
})
class MigrationSchemaParityTest {

    private static final Path MIGRATION_DIR = Path.of("docs/design/migrations");

    @Autowired
    private DataSource dataSource;

    @Test
    void migrationDdlMustCoverEveryEntityColumn() throws Exception {
        Map<String, Set<String>> entityTruth = entityColumnsFromH2();
        Map<String, Set<String>> migrationTruth = columnsFromMigrations();

        assertThat(entityTruth).as("H2 未建出任何表，测试前提不成立").isNotEmpty();
        assertThat(migrationTruth).as("未解析出任何迁移表，解析器可能已失效").isNotEmpty();

        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : entityTruth.entrySet()) {
            String table = entry.getKey();
            Set<String> declared = migrationTruth.get(table);
            if (declared == null) {
                continue; // 不由迁移脚本创建的表（历史遗留）跳过
            }
            Set<String> missing = new LinkedHashSet<>(entry.getValue());
            missing.removeAll(declared);
            if (!missing.isEmpty()) {
                problems.add(table + " 缺少列 " + missing);
            }
        }

        assertThat(problems)
                .as("手写迁移 DDL 与 JPA 实体不一致；生产 ddl-auto=validate 会在启动时失败（2026-10-15 同类事故）")
                .isEmpty();
    }

    private Map<String, Set<String>> entityColumnsFromH2() throws Exception {
        Map<String, Set<String>> result = new HashMap<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "select table_name, column_name from information_schema.columns")) {
            while (rs.next()) {
                result.computeIfAbsent(normalize(rs.getString(1)), key -> new HashSet<>())
                        .add(normalize(rs.getString(2)));
            }
        }
        return result;
    }

    private Map<String, Set<String>> columnsFromMigrations() throws Exception {
        Map<String, Set<String>> result = new HashMap<>();
        assertThat(Files.isDirectory(MIGRATION_DIR))
                .as("迁移目录不存在：" + MIGRATION_DIR.toAbsolutePath())
                .isTrue();

        List<Path> files;
        try (Stream<Path> stream = Files.list(MIGRATION_DIR)) {
            files = stream
                    .filter(path -> path.getFileName().toString().endsWith(".sql"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }

        for (Path file : files) {
            parseSql(Files.readString(file, StandardCharsets.UTF_8), result);
        }
        return result;
    }

    /** 解析单个 SQL 文件：CREATE TABLE 的列 + ALTER TABLE 追加的列。 */
    private void parseSql(String sql, Map<String, Set<String>> target) {
        String lower = sql.toLowerCase(Locale.ROOT);
        int cursor = 0;
        while (true) {
            int create = lower.indexOf("create table", cursor);
            int alter = lower.indexOf("alter table", cursor);
            if (create < 0 && alter < 0) {
                break;
            }
            if (create >= 0 && (alter < 0 || create < alter)) {
                cursor = parseCreateTable(sql, lower, create, target);
            } else {
                cursor = parseAlterTable(sql, lower, alter, target);
            }
        }
    }

    private int parseCreateTable(String sql, String lower, int start, Map<String, Set<String>> target) {
        Matcher head = Pattern.compile("create\\s+table\\s+(?:if\\s+not\\s+exists\\s+)?`?([a-z0-9_]+)`?\\s*\\(")
                .matcher(lower.substring(start));
        if (!head.find()) {
            return start + "create table".length();
        }
        String table = head.group(1);
        int bodyStart = start + head.end();
        int bodyEnd = matchingParen(sql, bodyStart - 1);
        if (bodyEnd < 0) {
            return bodyStart;
        }
        for (String definition : splitTopLevel(sql.substring(bodyStart, bodyEnd))) {
            String column = leadingIdentifier(definition);
            if (column != null) {
                target.computeIfAbsent(table, key -> new HashSet<>()).add(column);
            }
        }
        return bodyEnd + 1;
    }

    private int parseAlterTable(String sql, String lower, int start, Map<String, Set<String>> target) {
        Matcher head = Pattern.compile("alter\\s+table\\s+`?([a-z0-9_]+)`?").matcher(lower.substring(start));
        if (!head.find()) {
            return start + "alter table".length();
        }
        String table = head.group(1);
        int end = lower.indexOf(';', start);
        if (end < 0) {
            end = lower.length();
        }
        String body = lower.substring(start, end);
        Matcher add = Pattern.compile("add\\s+column\\s+(?:if\\s+not\\s+exists\\s+)?`?([a-z0-9_]+)`?").matcher(body);
        while (add.find()) {
            target.computeIfAbsent(table, key -> new HashSet<>()).add(add.group(1));
        }
        return end;
    }

    /** 从 '(' 出发找配对的 ')'，需跳过括号内的成对括号。 */
    private int matchingParen(String sql, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** 按顶层逗号切分列定义（忽略括号内的逗号）。 */
    private List<String> splitTopLevel(String body) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }

    /** 取列定义的首个标识符；遇到约束/索引声明返回 null。 */
    private String leadingIdentifier(String definition) {
        Matcher matcher = Pattern.compile("^\\s*`?([a-z0-9_]+)`?").matcher(definition.toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return null;
        }
        String name = matcher.group(1);
        return switch (name) {
            case "primary", "unique", "key", "index", "constraint", "foreign", "fulltext", "spatial", "check" -> null;
            default -> name;
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

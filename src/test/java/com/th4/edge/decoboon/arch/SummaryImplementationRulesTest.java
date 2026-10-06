package com.th4.edge.decoboon.arch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 统计汇总实现逻辑规范的门禁（DEC-036 / BR-031 / rules.md §0.4）。
 *
 * <p>只做静态检查，不依赖数据库，可随 CI 每次运行：</p>
 * <ul>
 *   <li>A1：{@code findAll(...)} 后接 Java {@code stream().sum()/count()}（全量实体 + 内存求和）；</li>
 *   <li>A2：循环体内发起查询（{@code Repository} / {@code findBy} / {@code findAll}）；</li>
 *   <li>A4：{@code @EntityGraph} 属性超过 4 个（重 JOIN 换条数）。</li>
 * </ul>
 *
 * <p>存量违规以 {@link #LEGACY_BASELINE} 白名单登记（GAP-027 分批整改）；
 * 白名单之外出现任何一条即失败，整改完成的文件必须从白名单移除。</p>
 */
class SummaryImplementationRulesTest {

    /** 统计/汇总类上下文所在包前缀（规范第 5 节的适用面）。 */
    private static final List<String> SUMMARY_PACKAGES = List.of(
            "finance/context/FinanceReportContext.java",
            "finance/context/ManualLedgerContext.java",
            "project/context/ProjectCostContext.java",
            "statistic/context/",
            "ai/billing/context/",
            "ai/context/AiUsageReportContext.java",
            "carryover/context/CarryoverReconciliationContext.java",
            // 2026-10-07 补漏：材料计划工作台（全场景最慢接口，逐计划查采购单/采购明细）
            "material/context/MaterialPlanWorkbenchContext.java",
            "customer/context/CustomerOverviewContext.java",
            "finance/context/FinanceSummaryAggregateContext.java",
            "payroll/context/PayrollWorkbenchContext.java",
            "project/context/ContractWorkbenchContext.java");

    /**
     * 存量违规基线（GAP-027 整改完成后必须逐条删除）。
     *
     * <p>键=相对路径，值=该文件被允许命中的规则标签（A1 findAll+sum / A2 循环内查库 / A4 EntityGraph>4）。
     * 只允许"历史遗留"，任何**新文件或新规则命中**都会失败。</p>
     */
    private static final java.util.Map<String, Set<String>> LEGACY_BASELINE = java.util.Map.ofEntries(
            java.util.Map.entry("project/context/ProjectCostContext.java", Set.of("A2")),
            java.util.Map.entry("finance/context/FinanceReportContext.java", Set.of("A1")),
            java.util.Map.entry("payroll/context/PayrollWorkbenchContext.java", Set.of("A1")),
            java.util.Map.entry("statistic/context/SiteWorkbenchContext.java", Set.of("A1")),
            java.util.Map.entry("statistic/context/BossContractChangeContext.java", Set.of("A1")),
            java.util.Map.entry("customer/repository/CustomerAccessRepository.java", Set.of("A4")),
            java.util.Map.entry("finance/repository/LedgerRepository.java", Set.of("A4")),
            java.util.Map.entry("finance/repository/PayableRepository.java", Set.of("A4")),
            java.util.Map.entry("finance/repository/PaymentRepository.java", Set.of("A4")),
            java.util.Map.entry("finance/repository/ReceiptRepository.java", Set.of("A4")),
            java.util.Map.entry("finance/repository/ReceivableRepository.java", Set.of("A4")),
            java.util.Map.entry("material/repository/MaterialSelectionRepository.java", Set.of("A4")),
            java.util.Map.entry("project/repository/AcceptanceRepository.java", Set.of("A4")),
            java.util.Map.entry("project/repository/MaterialPlanRepository.java", Set.of("A4")),
            java.util.Map.entry("project/repository/ProjectContractRepository.java", Set.of("A4")),
            java.util.Map.entry("project/repository/ProjectRepository.java", Set.of("A4")));

    private static final Path SRC = Path.of("src/main/java/com/th4/edge/decoboon");

    @Test
    void summaryClassesMustNotLoadAllEntitiesAndSumInJava() throws IOException {
        List<String> violations = scan("A1: findAll 后 Java stream 求和");
        assertThat(violations)
                .as("统计汇总类禁止「全量实体 + Java stream 求和」：请改为聚合 SQL（DEC-036 正模式 1）")
                .isEmpty();
    }

    @Test
    void summaryClassesMustNotQueryInsideLoop() throws IOException {
        List<String> violations = scan("A2: 循环体内查库");
        assertThat(violations)
                .as("统计汇总类禁止「循环体内发起查询」：请改为循环外一次取数或 SQL 聚合（DEC-036 反模式 A2）")
                .isEmpty();
    }

    @Test
    void entityGraphMustStayNarrow() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                Matcher m = Pattern.compile("@EntityGraph\\(\\s*attributePaths\\s*=\\s*\\{([^}]*)\\}")
                        .matcher(source);
                while (m.find()) {
                    String[] paths = m.group(1).split(",");
                    if (paths.length > 4) {
                        add(violations, rel(file), "A4: @EntityGraph 属性过多 → " + paths.length + " 个");
                    }
                }
            }
        }
        assertThat(violations)
                .as("@EntityGraph 属性不得超过 4 个（DEC-036 反模式 A4）；确需更多必须附 A/B 延迟证据并经评审")
                .isEmpty();
    }


    /**
     * 覆盖守卫：所有「汇总/报表/工作台」实现类必须登记进 {@link #SUMMARY_PACKAGES}。
     *
     * <p>防止再次出现"规范有了、但某个包漏扫"的情况（2026-10-07 材料计划工作台即因此漏网）。</p>
     */
    @Test
    void everySummaryImplementationMustBeInScope() throws IOException {
        List<String> uncovered = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = rel(file);
                if (!rel.contains("/context/")) {
                    continue;
                }
                String name = file.getFileName().toString();
                boolean looksLikeSummary = name.matches(".*(Report|Summary|Workbench|Dashboard).*Context\\.java")
                        || name.matches(".*(Report|Summary|Workbench|Dashboard)\\.java");
                if (looksLikeSummary && !inScope(rel)) {
                    uncovered.add(rel);
                }
            }
        }
        assertThat(uncovered)
                .as("汇总/报表/工作台实现必须登记进 SUMMARY_PACKAGES 才能受门禁约束（DEC-036 第 5 节）")
                .isEmpty();
    }

    // ---------- 扫描实现 ----------

    private List<String> scan(String rule) throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = rel(file);
                if (!inScope(rel)) {
                    continue;
                }
                String source = Files.readString(file);
                if (rule.startsWith("A1")) {
                    // 「全量实体 + 内存求和」的真实形态：同一文件既 findAll(...) 又对结果做 sum()/count()。
                    // 允许：代价为 O(1) 或已注释豁免的场景（需评审）。存量见 LEGACY_BASELINE。
                    if (source.contains("findAll(") && (source.contains("sum(") || source.contains(".count()"))) {
                        add(violations, rel, rule);
                    }
                } else if (rule.startsWith("A2")) {
                    Matcher m = Pattern.compile("(for|while)\\s*\\([^)]*\\)\\s*\\{([^}]*(?:\\{[^}]*})?[^}]*)}")
                            .matcher(source);
                    while (m.find()) {
                        String body = m.group(2);
                        if (Pattern.compile("([A-Za-z]+Repository|repository)\\.(find|count|exists|query)")
                                .matcher(body).find()) {
                            add(violations, rel, rule);
                            break;
                        }
                    }
                }
            }
        }
        return violations;
    }

    private void add(List<String> violations, String rel, String rule) {
        String tag = rule.substring(0, 2);
        String entry = rule + " | " + rel;
        boolean allowedLegacy = LEGACY_BASELINE.getOrDefault(rel, Set.of()).contains(tag);
        if (!allowedLegacy && !violations.contains(entry)) {
            violations.add(entry);
        }
    }

    private boolean inScope(String rel) {
        return SUMMARY_PACKAGES.stream().anyMatch(rel::contains);
    }

    private String rel(Path file) {
        String s = file.toString().replace('\\', '/');
        int i = s.indexOf("com/th4/");
        return i < 0 ? s : s.substring(i + "com/th4/edge/decoboon/".length());
    }
}

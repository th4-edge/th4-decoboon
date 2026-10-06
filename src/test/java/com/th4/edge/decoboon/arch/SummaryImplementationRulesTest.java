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
            "ai/brief/context/AiBossBriefContext.java",
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
            java.util.Map.entry("statistic/service/teamleader/report/support/Report.java", Set.of("A2")),
            java.util.Map.entry("statistic/context/RiskCenterContext.java", Set.of("A7")),
            java.util.Map.entry("statistic/context/OrganizationContext.java", Set.of("A7")),
            java.util.Map.entry("carryover/context/CarryoverReconciliationContext.java", Set.of("A7")),
            java.util.Map.entry("ai/brief/context/AiBossBriefContext.java", Set.of("A7")),
            java.util.Map.entry("finance/context/FinanceReportContext.java", Set.of("A1")),
            java.util.Map.entry("payroll/context/PayrollWorkbenchContext.java", Set.of("A1")),
            java.util.Map.entry("statistic/context/SiteWorkbenchContext.java", Set.of("A1", "A2b")),
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


    /**
     * A2b（跨方法）：循环体内调用的**本类私有方法**，其方法体内出现仓储调用。
     *
     * <p>纯过程内扫描无法发现这一形态 —— 例如材料计划工作台：
     * {@code for (plan : plans) { toItem(plan) }}，而 {@code toItem()} 内部逐条查采购明细/采购单。</p>
     */
    @Test
    void summaryClassesMustNotQueryInsideLoopViaHelperMethod() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = rel(file);
                String source = Files.readString(file);
                if (!inScope(rel, source)) {
                    continue;
                }
                Set<String> queryHelpers = new java.util.HashSet<>();
                Matcher mm = Pattern.compile("(?:private|protected|public)\\s+[\\w<>,\\[\\].\\s]+?\\s+(\\w+)\\s*\\([^)]*\\)\\s*\\{")
                        .matcher(source);
                while (mm.find()) {
                    String body = blockBody(source, source.indexOf('{', mm.end() - 1));
                    if (body == null) {
                        continue;
                    }
                    if (body.contains("Repository.") || Pattern.compile("\\b(findAll|findBy\\w*|countBy\\w*)\\s*\\(")
                            .matcher(body).find()) {
                        queryHelpers.add(mm.group(1));
                    }
                }
                if (queryHelpers.isEmpty()) {
                    continue;
                }
                Matcher lm = Pattern.compile("(for|while)\\s*\\([^)]*\\)\\s*\\{").matcher(source);
                while (lm.find()) {
                    String body = blockBody(source, source.indexOf('{', lm.end() - 1));
                    if (body == null) {
                        continue;
                    }
                    for (String helper : queryHelpers) {
                        if (Pattern.compile("\\b" + helper + "\\s*\\(").matcher(body).find()) {
                            add(violations, rel, "A2b: 循环体经私有方法查库 → " + helper + "()");
                            break;
                        }
                    }
                }
            }
        }
        assertThat(violations)
                .as("统计汇总类禁止「循环体调用会查库的私有方法」：应把该方法的取数改为循环外一次批量取数（DEC-036 反模式 A2）")
                .isEmpty();
    }


    /** 全仓统计类路由基线（2026-10-07 普查，GAP-027 靶子表；新增统计路由必须同步补录）。 */
    private java.util.Set<String> statisticRouteBaseline() throws IOException {
        Path file = Path.of("src/test/resources/statistic-routes-baseline.txt");
        if (!Files.exists(file)) {
            return Set.of();
        }
        return Files.readAllLines(file).stream().map(String::trim)
                .filter(line -> !line.isEmpty()).collect(java.util.stream.Collectors.toSet());
    }

    /** 基于花括号配平取代码块内容。 */
    private String blockBody(String source, int braceStart) {
        if (braceStart < 0 || braceStart >= source.length()) {
            return null;
        }
        int depth = 0;
        for (int i = braceStart; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(braceStart + 1, i);
                }
            }
        }
        return null;
    }


    /**
     * 覆盖守卫（按路由）：凡 Provider 路由名含统计语义
     * （statistic / report / workbench / dashboard / summary / overview / metrics / cockpit），
     * 该 Provider **或其委托的任一 Context** 必须登记进 {@link #SUMMARY_PACKAGES}。
     *
     * <p>这是"不再遗漏"的兜底：上次 material/plan/workbench 漏网，就是因为只按类名和手写包前缀列举，
     * 新增路由/新增上下文时没人同步登记。改为按路由枚举后，任何统计类新接口都会立刻被要求纳管。</p>
     */
    @Test
    void everyStatisticRouteProviderMustBeGoverned() throws IOException {
        List<String> uncovered = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                Matcher routeMatcher = Pattern.compile("@Service\\(\\s*\"([^\"]+)\"\\s*\\)").matcher(source);
                while (routeMatcher.find()) {
                    String route = routeMatcher.group(1);
                    if (!route.matches("(?i).*(statistic|report|workbench|dashboard|summary|overview|metrics|cockpit).*")) {
                        continue;
                    }
                    boolean governed = inScope(rel(file));
                    if (!governed) {
                        Matcher contextMatcher = Pattern
                                .compile("import com\\.th4\\.edge\\.decoboon\\.[\\w.]+\\.(\\w*Context);")
                                .matcher(source);
                        while (contextMatcher.find()) {
                            if (inScope(contextMatcher.group(1) + ".java")) {
                                governed = true;
                                break;
                            }
                        }
                    }
                    if (!governed && !statisticRouteBaseline().contains(route)) {
                        uncovered.add(route + " → " + rel(file));
                    }
                }
            }
        }
        assertThat(uncovered)
                .as("统计/报表/工作台/驾驶舱类路由必须纳管（Provider 或其委托 Context 登记进 SUMMARY_PACKAGES）")
                .isEmpty();
    }


    /**
     * A7：汇总接口嵌套调用汇总接口（把若干重型汇总顺序叠加，耗时相加、SQL 相乘）。
     *
     * <p>典型：{@code ai/brief/report} 顺序调用 dashboard + finance.summary + alert。正确做法是复用聚合层
     * （如 {@code FinanceSummaryAggregateContext}），而不是把整个重接口再跑一遍。</p>
     */
    @Test
    void summaryClassesMustNotNestOtherSummaryContexts() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = rel(file);
                String source = Files.readString(file);
                // A7 只针对 context 之间/内部：Provider（service/**/support）调用自己的 delegate context 属正常委托
                if (!inScope(rel)) {
                    continue;
                }
                // 字段名（camelCase）与类型名都要覆盖：financeContext.summary(...) / FinanceReportContext.summary(...)
                Matcher m = Pattern.compile("\\b(\\w*[Cc]ontext)\\s*\\.\\s*(summary|report|dashboard|workbench|overview)\\s*\\(")
                        .matcher(source);
                if (m.find()) {
                    add(violations, rel, "A7: 汇总嵌套调用 → " + m.group(1) + "." + m.group(2) + "()");
                }
            }
        }
        assertThat(violations)
                .as("汇总接口禁止嵌套调用另一汇总接口：应复用聚合层（DEC-036 反模式 A7）")
                .isEmpty();
    }

    // ---------- 扫描实现 ----------

    private List<String> scan(String rule) throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String rel = rel(file);
                String source = Files.readString(file);
                if (!inScope(rel, source)) {
                    continue;
                }
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
        String tag = rule.split(":")[0];
        String entry = rule + " | " + rel;
        boolean allowedLegacy = LEGACY_BASELINE.getOrDefault(rel, Set.of()).contains(tag);
        if (!allowedLegacy && !violations.contains(entry)) {
            violations.add(entry);
        }
    }

    private boolean inScope(String rel) {
        return SUMMARY_PACKAGES.stream().anyMatch(rel::contains);
    }

    /**
     * 扩大的纳管判定：登记在 {@link #SUMMARY_PACKAGES}，**或**该文件本身声明了统计语义路由。
     *
     * <p>补此判定是因为 `statistic/pm/report` 这类实现把汇总逻辑**直接写在 Provider 的 support 类里**
     * （没有 delegate Context），只按 context 路径纳管会永远扫不到它们。</p>
     */
    private boolean inScope(String rel, String source) {
        if (inScope(rel)) {
            return true;
        }
        Matcher m = Pattern.compile("@Service\\(\\s*\"([^\"]+)\"\\s*\\)").matcher(source);
        while (m.find()) {
            if (m.group(1).matches("(?i).*(statistic|report|workbench|dashboard|summary|overview|metrics|cockpit).*")) {
                return true;
            }
        }
        return false;
    }

    private String rel(Path file) {
        String s = file.toString().replace('\\', '/');
        int i = s.indexOf("com/th4/");
        return i < 0 ? s : s.substring(i + "com/th4/edge/decoboon/".length());
    }
}

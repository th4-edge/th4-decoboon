package com.th4.edge.decoboon.designtemplate.context;

import com.th4.edge.decoboon.bim.configure.Unit;
import com.th4.edge.decoboon.designtemplate.model.DesignTemplateRoom;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设计模版工程量公式上下文（BR-040 §2 / DEC-050 ④）。
 *
 * <p>模版层只允许**专业变量**：输入 {@code L/W/H/DW/DA/WA/WR/N}，派生 {@code A/P/WP/CP}；
 * 支持 {@code + - * / ( )} 与 {@code SUM/MIN/MAX/ROUND}。</p>
 *
 * <p><b>禁止单元格引用</b>（如 {@code =F2*2}）：模版面向业务变量而不是表格位置。
 * 实例化进入报价工作区后仍可使用既有单元格引用，由报价侧公式上下文负责。</p>
 *
 * <p>尺寸未填的变量视为「未就绪」而不是 0：求值抛
 * {@code TEMPLATE_SIZE_MISSING}，由调用方标记「待补尺寸」，不得计入合计或生成正式报价。</p>
 */
@Note("设计模版工程量公式")
@Component
public class DesignTemplateFormulaContext {

    /** 专业变量（模版层唯一合法命名空间）。 */
    public static final Set<String> INPUT_VARIABLES = Set.of("L", "W", "H", "DW", "DA", "WA", "WR", "N");
    public static final Set<String> DERIVED_VARIABLES = Set.of("A", "P", "WP", "CP");
    public static final Set<String> FUNCTIONS = Set.of("SUM", "MIN", "MAX", "ROUND", "COUNT");

    /** 实例化“待补尺寸”标记：quantity=0 且 formula 以该前缀开头；不新增 DDL。 */
    public static final String PENDING_MEASURE_MARKER = "__PENDING_MEASURE__";

    /** 区域数量内部变量键（只允许通过 COUNT(room) 使用，避免裸 ROOM 进入公式）。 */
    public static final String ROOM_COUNT_KEY = "#ROOM_COUNT#";

    /** 单元格引用（F2 / AB12）——模版层必须拒绝。 */
    private static final Pattern CELL_REFERENCE = Pattern.compile("(?<![A-Za-z0-9_])([A-Za-z]{1,3}[0-9]{1,7})(?![A-Za-z0-9_])");
    private static final int MAX_FORMULA_LENGTH = 512;
    private static final MathContext MATH = new MathContext(20, RoundingMode.HALF_UP);

    /** 变量就绪判断：null 表示未填尺寸。 */
    public boolean ready(BigDecimal value) {
        return value != null;
    }

    /** 计算一个区域的全部专业变量；未就绪的变量不放入结果（由求值时报「未就绪」）。 */
    public Map<String, BigDecimal> variablesOf(DesignTemplateRoom room) {
        Map<String, BigDecimal> variables = new HashMap<>();
        putIfReady(variables, "L", room.getLength());
        putIfReady(variables, "W", room.getWidth());
        putIfReady(variables, "H", room.getHeight());
        putIfReady(variables, "DW", room.getDoorWidthTotal());
        putIfReady(variables, "DA", room.getDoorAreaTotal());
        putIfReady(variables, "WA", room.getWindowAreaTotal());
        putIfReady(variables, "WR", room.getWallRise());
        variables.put("N", BigDecimal.valueOf(room.getCount() == null ? 1 : room.getCount()));
        variables.put(ROOM_COUNT_KEY, BigDecimal.ONE);

        BigDecimal length = room.getLength();
        BigDecimal width = room.getWidth();
        BigDecimal height = room.getHeight();
        if (ready(length) && ready(width)) {
            BigDecimal area = length.multiply(width, MATH);
            BigDecimal perimeter = length.add(width, MATH).multiply(BigDecimal.valueOf(2), MATH);
            variables.put("A", area);
            variables.put("CP", area);
            variables.put("P", perimeter);
            if (ready(height)) {
                BigDecimal walls = perimeter.multiply(height, MATH)
                        .subtract(zero(room.getDoorAreaTotal()), MATH)
                        .subtract(zero(room.getWindowAreaTotal()), MATH);
                variables.put("WP", walls);
            }
        }
        return variables;
    }

    /** 用给定变量表求值（全屋共享分项按 SUM(A)/SUM(P) 聚合时使用）。 */
    public BigDecimal evaluateWith(String formula, Map<String, BigDecimal> variables) {
        String normalized = normalize(formula);
        rejectCellReference(normalized);
        BigDecimal value = new Parser(normalized, variables).parseAll();
        if (value.signum() <= 0) {
            throw error("TEMPLATE_FORMULA_NOT_POSITIVE", "工程量公式结果必须大于 0");
        }
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    /** 校验公式语法与命名空间（发布前硬门禁的一部分）。 */
    public void validate(String formula) {
        String normalized = normalize(formula);
        if (normalized.isBlank()) {
            throw error("TEMPLATE_FORMULA_EMPTY", "模版分项必须填写工程量公式");
        }
        if (normalized.length() > MAX_FORMULA_LENGTH) {
            throw error("TEMPLATE_FORMULA_TOO_LONG", "工程量公式超过 " + MAX_FORMULA_LENGTH + " 字符");
        }
        rejectCellReference(normalized);
        Map<String, BigDecimal> probe = new HashMap<>(INPUT_VARIABLES.stream()
                .collect(HashMap::new, (map, key) -> map.put(key, BigDecimal.ONE), HashMap::putAll));
        probe.put("A", BigDecimal.ONE);
        probe.put("P", BigDecimal.ONE);
        probe.put("WP", BigDecimal.ONE);
        probe.put("CP", BigDecimal.ONE);
        probe.put(ROOM_COUNT_KEY, BigDecimal.ONE);
        new Parser(normalized, probe).parseAll();
    }

    /** 按区域求值；返回 3 位小数结果，结果必须大于 0。 */
    public BigDecimal evaluate(DesignTemplateRoom room, String formula) {
        String normalized = normalize(formula);
        rejectCellReference(normalized);
        BigDecimal value = new Parser(normalized, variablesOf(room)).parseAll();
        if (value.signum() <= 0) {
            throw error("TEMPLATE_FORMULA_NOT_POSITIVE", "工程量公式结果必须大于 0");
        }
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    private void rejectCellReference(String formula) {
        Matcher matcher = CELL_REFERENCE.matcher(formula.toUpperCase(Locale.ROOT));
        if (matcher.find()) {
            throw error("TEMPLATE_CELL_REFERENCE_FORBIDDEN",
                    "模版公式只能使用专业变量，禁止单元格引用：" + matcher.group(1));
        }
    }

    private String normalize(String formula) {
        if (formula == null) return "";
        String trimmed = formula.trim();
        return trimmed.startsWith("=") ? trimmed.substring(1).trim() : trimmed;
    }

    private static void putIfReady(Map<String, BigDecimal> variables, String key, BigDecimal value) {
        if (value != null) variables.put(key, value);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static DefinedException error(String code, String message) {
        return new DefinedException(new DefaultErrorCode(code, message));
    }

    /** 面积型单位允许的业务变量。 */
    private static final Set<String> AREA_VARIABLES = Set.of("A", "WP", "CP", "DA", "WA");
    /** 长度型单位允许的业务变量。 */
    private static final Set<String> LENGTH_VARIABLES = Set.of("L", "W", "H", "DW", "WR", "P");
    /** 计数型单位不允许出现业务变量（只能用固定数量）。 */
    private static final Set<String> COUNT_UNITS = Set.of("Count", "Item", "Place", "Set", "House",
            "Plate", "Block", "Root", "Hole", "Pcs", "Stick", "Grid", "Step");

    /** 提取公式中实际使用的专业变量（大写、去重）。 */
    public Set<String> variablesUsed(String formula) {
        String normalized = normalize(formula);
        if (normalized.isBlank()) {
            return Set.of();
        }
        Set<String> used = new HashSet<>();
        Matcher matcher = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*").matcher(normalized);
        while (matcher.find()) {
            String name = matcher.group().toUpperCase(Locale.ROOT);
            if (INPUT_VARIABLES.contains(name) || DERIVED_VARIABLES.contains(name)) {
                used.add(name);
            }
        }
        return used;
    }

    /**
     * 单位与公式维度一致性校验（BR-040 §2：单位与变量维度不匹配必须阻断，禁止自动换算）。
     *
     * <p>按**公式结果维度**判定，而不是逐个变量：长度变量记为 1 维、面积变量记为 2 维，
     * 乘法加维、除法减维，因此 {@code =P*H-DA-WA} 的结果是 2 维（面积），对「m²」合法。</p>
     *
     * <p>计数型单位不允许出现业务变量，只能填固定数量（与 §3「固定数量项」口径一致）。</p>
     */
    public void requireDimension(Unit unit, String formula) {
        String normalized = normalize(formula);
        if (normalized.isBlank()) {
            return;
        }
        if (unit == null) {
            throw error("TEMPLATE_UNIT_REQUIRED", "分项缺少计价单位，无法校验公式维度");
        }
        String name = unit.toString();
        if (COUNT_UNITS.contains(name)) {
            if (!variablesUsed(normalized).isEmpty()) {
                throw error("TEMPLATE_UNIT_DIMENSION_MISMATCH",
                        "当前单位为「" + unit.getName() + "」，工程量只能填写固定数量，不能使用专业变量公式");
            }
            return;
        }
        int expected = areaUnit(name) ? 2 : (isLengthUnit(name) ? 1 : -1);
        if (expected < 0) {
            return;
        }
        int actual = new DimensionParser(normalized).parseAll();
        if (actual != expected) {
            throw error("TEMPLATE_UNIT_DIMENSION_MISMATCH",
                    "单位「" + unit.getName() + "」与公式结果维度不一致，禁止自动换算");
        }
    }

    private static boolean areaUnit(String name) {
        return name.equals("Area") || name.equals("Sheet");
    }

    private static boolean isLengthUnit(String name) {
        return name.equals("Metre") || name.equals("Centimeter");
    }

    /**
     * 维度解析器：与数值求值同样的语法，但只跟踪量纲。
     *
     * <p>长度类变量 = 1 维，面积类变量 = 2 维，数量 N = 0 维；乘法加维、除法减维，
     * 加减要求两侧同维（否则直接判定维度不一致）。</p>
     */
    private static final class DimensionParser {

        private final String source;
        private int position;

        private DimensionParser(String source) {
            this.source = source;
        }

        private int parseAll() {
            int value = expression();
            skipSpace();
            if (position < source.length()) {
                throw error("TEMPLATE_FORMULA_SYNTAX", "公式存在无法解析的内容：" + source.substring(position));
            }
            return value;
        }

        private int expression() {
            int value = term();
            while (true) {
                skipSpace();
                if (eat('+') || eat('-')) {
                    int right = term();
                    if (right != value) {
                        throw error("TEMPLATE_UNIT_DIMENSION_MISMATCH", "公式加减项维度不一致，禁止自动换算");
                    }
                } else {
                    return value;
                }
            }
        }

        private int term() {
            int value = factor();
            while (true) {
                skipSpace();
                if (eat('*')) value = value + factor();
                else if (eat('/')) value = value - factor();
                else return value;
            }
        }

        private int factor() {
            skipSpace();
            if (eat('+')) return factor();
            if (eat('-')) return factor();
            if (eat('(')) {
                int value = expression();
                skipSpace();
                if (!eat(')')) throw error("TEMPLATE_FORMULA_SYNTAX", "工程量公式括号不匹配");
                return value;
            }
            if (position >= source.length()) {
                throw error("TEMPLATE_FORMULA_SYNTAX", "工程量公式不完整");
            }
            char current = source.charAt(position);
            if (Character.isDigit(current) || current == '.') {
                number();
                return 0;
            }
            if (Character.isLetter(current) || current == '_') return identifier();
            throw error("TEMPLATE_FORMULA_SYNTAX", "工程量公式存在非法字符：" + current);
        }

        private int identifier() {
            int start = position;
            while (position < source.length()
                    && (Character.isLetterOrDigit(source.charAt(position)) || source.charAt(position) == '_')) {
                position++;
            }
            String name = source.substring(start, position).toUpperCase(Locale.ROOT);
            skipSpace();
            if (eat('(')) {
                if (name.equals("COUNT")) {
                    int argumentStart = position;
                    while (position < source.length() && source.charAt(position) != ')') {
                        position++;
                    }
                    if (position >= source.length()) {
                        throw error("TEMPLATE_FORMULA_SYNTAX", "COUNT 缺少右括号");
                    }
                    String argument = source.substring(argumentStart, position).trim();
                    position++;
                    if (!argument.equalsIgnoreCase("room")) {
                        throw error("TEMPLATE_FORMULA_SYNTAX", "COUNT 只支持 COUNT(room)");
                    }
                    return 0;
                }
                // SUM/MIN/MAX/ROUND：参数同维，函数不改变量纲
                int dimension = -1;
                skipSpace();
                if (!eat(')')) {
                    do {
                        int argument = expression();
                        dimension = dimension < 0 ? argument : dimension;
                        skipSpace();
                    } while (eat(','));
                    if (!eat(')')) throw error("TEMPLATE_FORMULA_SYNTAX", "函数括号不匹配");
                }
                return dimension < 0 ? 0 : dimension;
            }
            if (AREA_VARIABLES.contains(name)) return 2;
            if (LENGTH_VARIABLES.contains(name)) return 1;
            if (name.equals("N")) return 0;
            throw error("TEMPLATE_VARIABLE_UNKNOWN", "未识别的专业变量：" + name);
        }

        private void number() {
            while (position < source.length()
                    && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                position++;
            }
        }

        private boolean eat(char expect) {
            skipSpace();
            if (position < source.length() && source.charAt(position) == expect) {
                position++;
                return true;
            }
            return false;
        }

        private void skipSpace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }
    }

    /** 递归下降解析器：四则运算 + 括号 + 专业变量 + SUM/MIN/MAX/ROUND。 */
    private static final class Parser {

        private final String source;
        private final Map<String, BigDecimal> variables;
        private int position;

        private Parser(String source, Map<String, BigDecimal> variables) {
            this.source = source;
            this.variables = variables;
        }

        private BigDecimal parseAll() {
            BigDecimal value = expression();
            skipSpace();
            if (position < source.length()) {
                throw error("TEMPLATE_FORMULA_SYNTAX", "公式存在无法解析的内容：" + source.substring(position));
            }
            return value;
        }

        private BigDecimal expression() {
            BigDecimal value = term();
            while (true) {
                skipSpace();
                if (eat('+')) value = value.add(term(), MATH);
                else if (eat('-')) value = value.subtract(term(), MATH);
                else return value;
            }
        }

        private BigDecimal term() {
            BigDecimal value = factor();
            while (true) {
                skipSpace();
                if (eat('*')) value = value.multiply(factor(), MATH);
                else if (eat('/')) {
                    BigDecimal divisor = factor();
                    if (divisor.signum() == 0) throw error("TEMPLATE_FORMULA_DIVIDE_ZERO", "工程量公式除数为 0");
                    value = value.divide(divisor, MATH);
                } else return value;
            }
        }

        private BigDecimal factor() {
            skipSpace();
            if (eat('+')) return factor();
            if (eat('-')) return factor().negate(MATH);
            if (eat('(')) {
                BigDecimal value = expression();
                skipSpace();
                if (!eat(')')) throw error("TEMPLATE_FORMULA_SYNTAX", "工程量公式括号不匹配");
                return value;
            }
            if (position >= source.length()) {
                throw error("TEMPLATE_FORMULA_SYNTAX", "工程量公式不完整");
            }
            char current = source.charAt(position);
            if (Character.isDigit(current) || current == '.') return number();
            if (Character.isLetter(current) || current == '_') return identifier();
            throw error("TEMPLATE_FORMULA_SYNTAX", "工程量公式存在非法字符：" + current);
        }

        private BigDecimal number() {
            int start = position;
            while (position < source.length()
                    && (Character.isDigit(source.charAt(position)) || source.charAt(position) == '.')) {
                position++;
            }
            String text = source.substring(start, position);
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException exception) {
                throw error("TEMPLATE_FORMULA_SYNTAX", "工程量公式数字非法：" + text);
            }
        }

        private BigDecimal identifier() {
            int start = position;
            while (position < source.length()
                    && (Character.isLetterOrDigit(source.charAt(position)) || source.charAt(position) == '_')) {
                position++;
            }
            String raw = source.substring(start, position);
            String name = raw.toUpperCase(Locale.ROOT);
            skipSpace();
            if (eat('(')) return function(name, raw);
            BigDecimal value = variables.get(name);
            if (value == null) {
                if (DERIVED_VARIABLES.contains(name) || INPUT_VARIABLES.contains(name)) {
                    throw error("TEMPLATE_SIZE_MISSING", "「" + name + "」所需尺寸未填，暂不可计算");
                }
                throw error("TEMPLATE_VARIABLE_UNKNOWN", "未识别的专业变量：" + raw);
            }
            return value;
        }

        private BigDecimal function(String name, String raw) {
            if (!FUNCTIONS.contains(name)) {
                throw error("TEMPLATE_FUNCTION_UNKNOWN", "未识别的函数：" + raw);
            }
            if (name.equals("COUNT")) {
                skipSpace();
                int argumentStart = position;
                while (position < source.length() && source.charAt(position) != ')') {
                    position++;
                }
                if (position >= source.length()) {
                    throw error("TEMPLATE_FORMULA_SYNTAX", "COUNT 缺少右括号");
                }
                String argument = source.substring(argumentStart, position).trim();
                position++;
                if (!argument.equalsIgnoreCase("room")) {
                    throw error("TEMPLATE_FORMULA_SYNTAX", "COUNT 只支持 COUNT(room)");
                }
                BigDecimal count = variables.get(ROOM_COUNT_KEY);
                if (count == null) {
                    throw error("TEMPLATE_SIZE_MISSING", "区域数量不可用，暂不可计算");
                }
                return count;
            }
            List<BigDecimal> arguments = new ArrayList<>();
            skipSpace();
            if (!eat(')')) {
                do {
                    arguments.add(expression());
                    skipSpace();
                } while (eat(','));
                if (!eat(')')) throw error("TEMPLATE_FORMULA_SYNTAX", "函数 " + raw + " 括号不匹配");
            }
            switch (name) {
                case "SUM" -> {
                    requireArguments(raw, arguments, 1);
                    return arguments.stream().reduce(BigDecimal.ZERO, (left, right) -> left.add(right, MATH));
                }
                case "MIN" -> {
                    requireArguments(raw, arguments, 1);
                    return arguments.stream().min(BigDecimal::compareTo).orElseThrow();
                }
                case "MAX" -> {
                    requireArguments(raw, arguments, 1);
                    return arguments.stream().max(BigDecimal::compareTo).orElseThrow();
                }
                case "ROUND" -> {
                    if (arguments.size() != 2) {
                        throw error("TEMPLATE_FORMULA_SYNTAX", "ROUND 需要 2 个参数");
                    }
                    return arguments.get(0).setScale(arguments.get(1).intValue(), RoundingMode.HALF_UP);
                }
                default -> throw error("TEMPLATE_FUNCTION_UNKNOWN", "未识别的函数：" + raw);
            }
        }

        private void requireArguments(String raw, List<BigDecimal> arguments, int least) {
            if (arguments.size() < least) {
                throw error("TEMPLATE_FORMULA_SYNTAX", raw + " 至少需要 " + least + " 个参数");
            }
        }

        private boolean eat(char expect) {
            skipSpace();
            if (position < source.length() && source.charAt(position) == expect) {
                position++;
                return true;
            }
            return false;
        }

        private void skipSpace() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
                position++;
            }
        }
    }
}

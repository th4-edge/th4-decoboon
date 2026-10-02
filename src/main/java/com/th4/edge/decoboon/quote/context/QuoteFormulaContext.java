package com.th4.edge.decoboon.quote.context;

import com.th4.edge.decoboon.quote.model.QuoteLine;
import com.th4.edge.framework.core.annotation.Note;
import com.th4.edge.framework.exception.DefaultErrorCode;
import com.th4.edge.framework.exception.DefinedException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 报价工程量公式上下文。
 *
 * <p>P0 只支持确定性公式：四则运算、百分比、同表单元格/绝对引用、区域范围和
 * SUM/ROUND/MIN/MAX。公式结果由服务端计算，前端实时预览不构成正式事实。</p>
 */
@Note("报价工程量公式")
@Component
public class QuoteFormulaContext {

    private static final int MAX_FORMULA_LENGTH = 512;
    private static final MathContext MATH = new MathContext(20, RoundingMode.HALF_UP);
    private static final Set<String> FUNCTIONS = Set.of("SUM", "ROUND", "MIN", "MAX");

    public Map<Long, BigDecimal> evaluate(List<QuoteLine> lines, Map<Long, String> formulaByLineId) {
        List<QuoteLine> ordered = lines.stream()
                .sorted(Comparator.comparing((QuoteLine line) -> line.getSort() == null ? Integer.MAX_VALUE : line.getSort())
                        .thenComparing(QuoteLine::getItemId))
                .toList();
        Sheet sheet = new Sheet(ordered, formulaByLineId);
        Map<Long, BigDecimal> result = new HashMap<>();
        for (QuoteLine line : ordered) {
            if (formulaByLineId.containsKey(line.getItemId())) {
                BigDecimal value = sheet.quantity(line.getItemId());
                if (value.signum() <= 0) {
                    throw new DefinedException(new DefaultErrorCode("400", "工程量公式结果必须大于零"));
                }
                result.put(line.getItemId(), value.setScale(3, RoundingMode.HALF_UP));
            }
        }
        return result;
    }

    private static final class Sheet {
        private final List<QuoteLine> lines;
        private final Map<Long, Integer> rowByLineId = new HashMap<>();
        private final Map<Long, QuoteLine> lineById = new HashMap<>();
        private final Map<Long, String> formulaByLineId;
        private final Map<Long, BigDecimal> cache = new HashMap<>();
        private final Set<Long> visiting = new HashSet<>();

        private Sheet(List<QuoteLine> lines, Map<Long, String> formulas) {
            this.lines = lines;
            this.formulaByLineId = formulas;
            for (int index = 0; index < lines.size(); index++) {
                QuoteLine line = lines.get(index);
                rowByLineId.put(line.getItemId(), index + 1);
                lineById.put(line.getItemId(), line);
            }
        }

        private BigDecimal quantity(Long lineId) {
            if (cache.containsKey(lineId)) return cache.get(lineId);
            QuoteLine line = lineById.get(lineId);
            if (line == null) throw error("#REF! 引用的报价行不存在");
            if (!visiting.add(lineId)) throw error("#CIRC! 工程量公式存在循环引用");
            try {
                String formula = formulaByLineId.get(lineId);
                BigDecimal value = formula == null || formula.isBlank()
                        ? zero(line.getQuantity())
                        : new Parser(normalize(formula), this::cellValue).parse();
                cache.put(lineId, value);
                return value;
            } finally {
                visiting.remove(lineId);
            }
        }

        private BigDecimal cellValue(String column, int row) {
            QuoteLine target = row >= 1 && row <= lines.size() ? lines.get(row - 1) : null;
            if (target == null) throw error("#REF! 引用的单元格不存在");
            return switch (column.toUpperCase(Locale.ROOT)) {
                case "A" -> BigDecimal.valueOf(row);
                case "F" -> quantity(target.getItemId());
                case "H" -> zero(target.getMaterialPrice());
                case "I" -> zero(target.getManualPrice());
                case "J" -> zero(target.getPrice());
                case "K" -> zero(target.getAmount());
                default -> throw error("#VALUE! 该单元格不参与数值公式计算");
            };
        }
    }

    private static String normalize(String raw) {
        String formula = raw == null ? "" : raw.trim();
        if (formula.startsWith("=")) formula = formula.substring(1).trim();
        if (formula.isEmpty()) throw error("工程量公式不能为空");
        if (formula.length() > MAX_FORMULA_LENGTH) {
            throw error("工程量公式不能超过 " + MAX_FORMULA_LENGTH + " 个字符");
        }
        return formula
                .replace('×', '*')
                .replace('÷', '/')
                .replace('（', '(')
                .replace('）', ')');
    }

    @FunctionalInterface
    private interface CellResolver {
        BigDecimal resolve(String column, int row);
    }

    private static final class Parser {
        private final String source;
        private final CellResolver cells;
        private int position;

        private Parser(String source, CellResolver cells) {
            this.source = source;
            this.cells = cells;
        }

        private BigDecimal parse() {
            BigDecimal result = expression();
            skipSpaces();
            if (position != source.length()) throw error("公式包含不支持的字符: " + source.charAt(position));
            return result;
        }

        private BigDecimal expression() {
            BigDecimal value = term();
            while (true) {
                skipSpaces();
                if (take('+')) value = value.add(term(), MATH);
                else if (take('-')) value = value.subtract(term(), MATH);
                else return value;
            }
        }

        private BigDecimal term() {
            BigDecimal value = unary();
            while (true) {
                skipSpaces();
                if (take('*')) value = value.multiply(unary(), MATH);
                else if (take('/')) {
                    BigDecimal divisor = unary();
                    if (divisor.signum() == 0) throw error("#DIV/0! 除数不能为零");
                    value = value.divide(divisor, 12, RoundingMode.HALF_UP);
                } else return value;
            }
        }

        private BigDecimal unary() {
            skipSpaces();
            if (take('+')) return unary();
            if (take('-')) return unary().negate();
            return postfix();
        }

        private BigDecimal postfix() {
            BigDecimal value = primary();
            while (true) {
                skipSpaces();
                if (!take('%')) return value;
                value = value.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP);
            }
        }

        private BigDecimal primary() {
            skipSpaces();
            if (take('(')) {
                BigDecimal value = expression();
                skipSpaces();
                if (!take(')')) throw error("公式缺少右括号");
                return value;
            }
            int before = position;
            CellRef ref = cellRef();
            if (ref != null) return cells.resolve(ref.column, ref.row);
            position = before;
            if (position < source.length() && Character.isLetter(source.charAt(position))) {
                int start = position;
                while (position < source.length() && Character.isLetter(source.charAt(position))) position++;
                String name = source.substring(start, position).toUpperCase(Locale.ROOT);
                skipSpaces();
                if (position < source.length() && source.charAt(position) == '(') {
                    if (!FUNCTIONS.contains(name)) throw error("不支持的函数: " + name);
                    position++;
                    return function(name);
                }
                throw error("公式包含无效名称");
            }
            return number();
        }

        private BigDecimal function(String name) {
            List<Object> args = new ArrayList<>();
            skipSpaces();
            if (take(')')) throw error(name + " 至少需要一个参数");
            while (true) {
                skipSpaces();
                int before = position;
                CellRef start = cellRef();
                skipSpaces();
                if (start != null && take(':')) {
                    CellRef end = cellRef();
                    if (end == null) throw error("区域公式缺少结束单元格");
                    args.add(new Range(start, end));
                } else {
                    position = before;
                    args.add(expression());
                }
                skipSpaces();
                if (take(')')) break;
                if (!take(',')) throw error(name + " 参数之间需要逗号");
            }
            List<BigDecimal> values = flatten(name, args);
            return switch (name) {
                case "SUM" -> values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                case "MIN" -> values.stream().min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                case "MAX" -> values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
                case "ROUND" -> round(values);
                default -> throw error("不支持的函数: " + name);
            };
        }

        private List<BigDecimal> flatten(String name, List<Object> args) {
            List<BigDecimal> values = new ArrayList<>();
            for (Object arg : args) {
                if (arg instanceof Range range) {
                    if (!"SUM".equals(name)) throw error(name + " 暂不支持区域参数");
                    values.addAll(rangeValues(range));
                } else if (arg instanceof BigDecimal decimal) {
                    values.add(decimal);
                } else {
                    throw error(name + " 参数类型不正确");
                }
            }
            return values;
        }

        private List<BigDecimal> rangeValues(Range range) {
            if (!range.start.column.equalsIgnoreCase(range.end.column)) {
                throw error("区域公式必须位于同一列");
            }
            int from = Math.min(range.start.row, range.end.row);
            int to = Math.max(range.start.row, range.end.row);
            if (to - from > 1000) throw error("区域范围不能超过 1000 行");
            List<BigDecimal> values = new ArrayList<>();
            for (int row = from; row <= to; row++) {
                values.add(cells.resolve(range.start.column, row));
            }
            return values;
        }

        private BigDecimal round(List<BigDecimal> values) {
            if (values.size() != 2) throw error("ROUND 需要两个参数");
            int scale = values.get(1).intValueExact();
            if (scale < 0 || scale > 6) throw error("ROUND 小数位必须在 0 到 6 之间");
            return values.get(0).setScale(scale, RoundingMode.HALF_UP);
        }

        private BigDecimal number() {
            skipSpaces();
            int start = position;
            boolean dot = false;
            while (position < source.length()) {
                char ch = source.charAt(position);
                if (Character.isDigit(ch)) {
                    position++;
                } else if (ch == '.' && !dot) {
                    dot = true;
                    position++;
                } else {
                    break;
                }
            }
            if (start == position || ".".equals(source.substring(start, position))) throw error("公式包含无效数字");
            return new BigDecimal(source.substring(start, position));
        }

        private CellRef cellRef() {
            skipSpaces();
            int start = position;
            if (take('$')) { /* absolute column */ }
            int columnStart = position;
            while (position < source.length() && Character.isLetter(source.charAt(position))) position++;
            if (position == columnStart) { position = start; return null; }
            String column = source.substring(columnStart, position);
            if (column.length() > 3) { position = start; return null; }
            if (take('$')) { /* absolute row */ }
            int rowStart = position;
            while (position < source.length() && Character.isDigit(source.charAt(position))) position++;
            if (position == rowStart) { position = start; return null; }
            int row;
            try {
                row = Integer.parseInt(source.substring(rowStart, position));
            } catch (NumberFormatException ex) {
                position = start;
                return null;
            }
            return new CellRef(column.toUpperCase(Locale.ROOT), row);
        }

        private void skipSpaces() {
            while (position < source.length() && Character.isWhitespace(source.charAt(position))) position++;
        }

        private boolean take(char expected) {
            if (position < source.length() && source.charAt(position) == expected) {
                position++;
                return true;
            }
            return false;
        }
    }

    private record CellRef(String column, int row) {}
    private record Range(CellRef start, CellRef end) {}

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static DefinedException error(String message) {
        return new DefinedException(new DefaultErrorCode("400", message));
    }
}

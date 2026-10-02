package io.agenticai.chk.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.Optional;

/**
 * Recomputes an explicit value or date comparison in code (REQ-CHK-025, REQ-CHK-028;
 * ADR-CHK-003). A value and a limit are read either both as a {@link BigDecimal} or both as a date
 * — ISO-8601 ({@code yyyy-MM-dd}) or {@code dd/MM/yyyy}, strictly. The comparisons are {@code >=},
 * {@code >}, {@code <=}, {@code <}, {@code =} (numbers and dates) and {@code before}, {@code after},
 * {@code on or before}, {@code on or after} (dates only), matched case-insensitively with runs of
 * whitespace taken as one space. Anything else is not computable.
 *
 * <p>Plain Java, no state: one static operation.
 */
public final class ExplicitComparison {

    private static final DateTimeFormatter DAY_MONTH_YEAR =
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT).withResolverStyle(ResolverStyle.STRICT);

    private ExplicitComparison() {
        throw new UnsupportedOperationException("Utility class, do not instantiate");
    }

    /**
     * @return whether {@code valueFound comparison limit} holds; empty when the value or the limit
     *         cannot be read as a number or a date of the same kind, or the comparison is unknown
     *         (REQ-CHK-028)
     */
    public static Optional<Boolean> evaluate(String valueFound, String comparison, String limit) {
        if (valueFound == null || comparison == null || limit == null) {
            return Optional.empty();
        }
        String operator = comparison.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        Optional<BigDecimal> valueNumber = number(valueFound);
        Optional<BigDecimal> limitNumber = number(limit);
        if (valueNumber.isPresent() && limitNumber.isPresent()) {
            return numeric(operator, valueNumber.get().compareTo(limitNumber.get()));
        }
        Optional<LocalDate> valueDate = date(valueFound);
        Optional<LocalDate> limitDate = date(limit);
        if (valueDate.isPresent() && limitDate.isPresent()) {
            return dated(operator, valueDate.get().compareTo(limitDate.get()));
        }
        return Optional.empty();
    }

    private static Optional<Boolean> numeric(String operator, int order) {
        return switch (operator) {
            case ">=" -> Optional.of(order >= 0);
            case ">" -> Optional.of(order > 0);
            case "<=" -> Optional.of(order <= 0);
            case "<" -> Optional.of(order < 0);
            case "=" -> Optional.of(order == 0);
            default -> Optional.empty();
        };
    }

    private static Optional<Boolean> dated(String operator, int order) {
        return switch (operator) {
            case "before" -> Optional.of(order < 0);
            case "after" -> Optional.of(order > 0);
            case "on or before" -> Optional.of(order <= 0);
            case "on or after" -> Optional.of(order >= 0);
            default -> numeric(operator, order);
        };
    }

    private static Optional<BigDecimal> number(String text) {
        try {
            return Optional.of(new BigDecimal(text.trim()));
        } catch (NumberFormatException notANumber) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> date(String text) {
        String trimmed = text.trim();
        try {
            return Optional.of(LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE));
        } catch (DateTimeParseException notIso) {
            try {
                return Optional.of(LocalDate.parse(trimmed, DAY_MONTH_YEAR));
            } catch (DateTimeParseException notDayMonthYear) {
                return Optional.empty();
            }
        }
    }
}

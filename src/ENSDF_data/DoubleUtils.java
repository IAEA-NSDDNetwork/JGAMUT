package ENSDF_data;

import java.util.Optional;
import java.util.regex.Pattern;

public class DoubleUtils {
    private static final String Digits = "(\\p{Digit}+)";
    private static final String HexDigits = "(\\p{XDigit}+)";
    // an exponent is 'e' or 'E' followed by an optionally
    // signed decimal integer.
    private static final String Exp = "[eE][+-]?" + Digits;
    private static final String fpRegex
            = ("[\\x00-\\x20]*" + // Optional leading "whitespace"
            "[+-]?(" + // Optional sign character
            "NaN|" + // "NaN" string
            "Infinity|"
            + // "Infinity" string
            // A decimal floating-point string representing a finite positive
            // number without a leading sign has at most five basic pieces:
            // Digits . Digits ExponentPart FloatTypeSuffix
            //
            // Since this method allows integer-only strings as input
            // in addition to strings of floating-point literals, the
            // two sub-patterns below are simplifications of the grammar
            // productions from section 3.10.2 of
            // The Java Language Specification.
            // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
            "(((" + Digits + "(\\.)?(" + Digits + "?)(" + Exp + ")?)|"
            + // . Digits ExponentPart_opt FloatTypeSuffix_opt
            "(\\.(" + Digits + ")(" + Exp + ")?)|"
            + // Hexadecimal strings
            "(("
            + // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
            "(0[xX]" + HexDigits + "(\\.)?)|"
            + // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
            "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")"
            + ")[pP][+-]?" + Digits + "))"
            + "[fFdD]?))"
            + "[\\x00-\\x20]*");// Optional trailing "whitespace"    
    
    private static final Pattern DOUBLE_RE = Pattern.compile(fpRegex);
    
    public static final boolean isNumericString(String s) {
        return DOUBLE_RE.matcher(s).matches();
    }
    
    public static final Optional<Double> safeParse(String s) {
        if (DOUBLE_RE.matcher(s).matches()) {
            return Optional.of(Double.parseDouble(s));
        } else {
            return Optional.empty();
        }
    }
    
    public static final Optional<Double> copy(Optional<Double> m) {
        if (m.isPresent()) {
            return Optional.of(m.get());
        } else {
            return Optional.empty();
        }
    }
}

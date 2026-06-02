package boti.doc.timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {

    // Regex for Format 2: [hh:]mm:ss or mm:ss
    private static final Pattern COLON_PATTERN = Pattern.compile(
            "^(?:(\\d+):)?([0-5]?\\d):([0-5]?\\d)$"
    );

    // Regex for Format 3: xhymzs (all components optional, but at least one required)
    private static final Pattern TEXT_PATTERN = Pattern.compile(
            "^(?:(\\d+)[hH])?(?:([0-5]?\\d)[mM])?(?:([0-5]?\\d)[sS])?$"
    );

    /**
     * Parses a time string into total seconds using Java 25 pattern matching.
     * Guarded against numbers exceeding Integer.MAX_VALUE.
     *
     * @param input The time string to parse
     * @return The total time in seconds as an int
     * @throws IllegalArgumentException If the format is invalid, empty, or exceeds Integer.MAX_VALUE
     */
    public static int parseToSeconds(String input) {
        if (input == null || input.strip().isEmpty()) {
            throw new IllegalArgumentException("Input cannot be null or empty");
        }

        String cleaned = input.strip();

        try {
            // Pattern matching for switch determines the format type
            return switch (cleaned) {
                // Format 1: Pure digits (Seconds)
                case String s when s.matches("^\\d+$") -> parseSafeInt(s);

                // Format 2: Colon-separated values
                case String s when COLON_PATTERN.matcher(s).matches() -> {
                    Matcher m = COLON_PATTERN.matcher(s);
                    m.matches(); // Guaranteed to find due to case guard

                    int hours = m.group(1) != null ? parseSafeInt(m.group(1)) : 0;
                    int minutes = Integer.parseInt(m.group(2));
                    int seconds = Integer.parseInt(m.group(3));

                    yield calculateTotalSeconds(hours, minutes, seconds);
                }

                // Format 3: Alphanumeric shorthand (e.g., 1h40m, 5s)
                case String s when TEXT_PATTERN.matcher(s).matches() -> {
                    Matcher m = TEXT_PATTERN.matcher(s);
                    m.matches();

                    // Ensure it's not an empty match (e.g., random letters that didn't tokenise)
                    if (m.group(1) == null && m.group(2) == null && m.group(3) == null) {
                        throw new IllegalArgumentException("Invalid time format: " + input);
                    }

                    int hours = m.group(1) != null ? parseSafeInt(m.group(1)) : 0;
                    int minutes = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
                    int seconds = m.group(3) != null ? Integer.parseInt(m.group(3)) : 0;

                    yield calculateTotalSeconds(hours, minutes, seconds);
                }

                default -> throw new IllegalArgumentException("Invalid time format: " + input);
            };
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Time value exceeds the maximum allowable limit (Integer.MAX_VALUE)", e);
        }
    }

    /**
     * Safely converts a string to an integer, guarding against numeric overflow.
     */
    private static int parseSafeInt(String numberStr) {
        try {
            long val = Long.parseLong(numberStr);
            if (val > Integer.MAX_VALUE) {
                throw new ArithmeticException("Overflow");
            }
            return (int) val;
        } catch (NumberFormatException e) {
            throw new ArithmeticException("Overflow");
        }
    }

    /**
     * Uses safe math operations to prevent silent wrap-around during aggregation.
     */
    private static int calculateTotalSeconds(int hours, int minutes, int seconds) {
        int hoursToSeconds = Math.multiplyExact(hours, 3600);
        int minutesToSeconds = Math.multiplyExact(minutes, 60);

        int total = Math.addExact(hoursToSeconds, minutesToSeconds);
        return Math.addExact(total, seconds);
    }
}

package info.kgeorgiy.ja.polchinsky.implementor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * This class consists exclusively of static utility methods that operate on or return strings.
 *
 * @author Dmitry Polchinsky
 * @see String
 */
public final class StringUtils {

    /**
     * Line separator.
     */
    public static final String LINE_SEPARATOR = System.lineSeparator();

    /**
     * Tabulation.
     */
    public static final String TAB = "\t";

    /**
     * Single space.
     */
    public static final String SPACE = " ";

    /**
     * Comma.
     */
    public static final String COMMA = ", ";

    /**
     * Empty string.
     */
    public static final String EMPTY = "";

    /**
     * Default constructor. Made private to prevent instantiation of utility class.
     */
    private StringUtils() {
    }

    /**
     * Returns a new {@code String} composed of given {@code blocks}
     * joined together with one blank line.
     *
     * @param blocks the blocks to join together
     * @return the {@code String} containing given {@code blocks}
     * @see #join(String, String...)
     */
    public static String joinBlocks(final String... blocks) {
        return join(LINE_SEPARATOR.repeat(2), blocks);
    }

    /**
     * Returns a new {@code String} composed of given {@code parts}
     * joined together with the specified {@code delimiter}.
     *
     * @param delimiter the delimiter that separates each element
     * @param parts     the parts to join together
     * @return the {@code String} containing given {@code parts}
     * @see #join(String, Function, Object[])
     */
    public static String join(final String delimiter, final String... parts) {
        return join(delimiter, EMPTY, Function.identity(), parts);
    }

    /**
     * Returns a new {@code String} composed of the results of
     * applying {@code stringer} to the elements of {@code parts}
     * joined together with the specified {@code delimiter}.
     *
     * @param delimiter the delimiter that separates each element
     * @param stringer  the function to apply to each element
     * @param parts     the parts to join together
     * @param <T>       the type of element in {@code parts}
     * @return the {@code String} containing given {@code parts}
     * @see #join(String, String, Function, Object[])
     */
    public static <T> String join(final String delimiter, final Function<T, String> stringer, final T[] parts) {
        return join(delimiter, EMPTY, stringer, parts);
    }

    /**
     * Returns a new {@code String} composed of the results of
     * applying {@code stringer} to the elements of {@code parts}
     * joined together with the specified {@code delimiter}
     * and optionally starting with a supplied {@code prefix}.
     * <p>
     * If the given array is empty returns empty {@code String} instead of {@code prefix}.
     *
     * @param delimiter the delimiter that separates each element
     * @param prefix    the {@code String} to be used at the beginning
     * @param stringer  the function to apply to each element
     * @param parts     the parts to join together
     * @param <T>       the type of element in {@code parts}
     * @return the {@code String} containing given {@code parts}
     * @see #joining(String, String)
     */
    public static <T> String join(final String delimiter,
                                  final String prefix,
                                  final Function<T, String> stringer,
                                  final T[] parts) {
        return Arrays.stream(parts)
                .map(stringer)
                .collect(Collectors.filtering(Predicate.not(String::isBlank), joining(delimiter, prefix)));
    }

    /**
     * Prepends tab to every line of {@code text}.
     *
     * @param text the {@code String} to tabulate
     * @return the tabbed {@code String}
     */
    public static String tabbed(final String text) {
        return text.lines()
                .map(line -> TAB + line)
                .collect(Collectors.joining(LINE_SEPARATOR));
    }

    /**
     * Returns a {@code Collector} that concatenates the input elements,
     * separated by the specified delimiter, with the specified prefix.
     * <p>
     * The main difference from {@link Collectors#joining(CharSequence, CharSequence, CharSequence)}
     * is that this method will return empty {@code String} instead of {@code prefix} when used on empty stream.
     *
     * @param delimiter the delimiter that separates each element
     * @param prefix    the {@code String} to be used at the beginning
     * @return the {@code Collector} which concatenates elements with the specified delimeter
     */
    public static Collector<String, StringJoiner, String> joining(final String delimiter, final String prefix) {
        return Collector.of(
                () -> new StringJoiner(delimiter, prefix, EMPTY).setEmptyValue(EMPTY),
                StringJoiner::add,
                StringJoiner::merge,
                StringJoiner::toString);
    }

    /**
     * Converts the provided {@code text} to text encoded in ASCII,
     * using Unicode escapes (&#92;uxxxx) notation for all characters
     * that are not part of the ASCII character set.
     *
     * @param text the text to encode
     * @return the endoded text
     */
    public static String toAscii(final String text) {
        return text.chars()
                .mapToObj(ch -> ch < 128 ? Character.toString(ch) :
                        String.format("\\u%04X", ch))
                .collect(Collectors.joining());
    }
}


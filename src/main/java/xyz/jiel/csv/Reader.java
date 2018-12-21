package xyz.jiel.csv;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An utility class for parse csv. It is inherited by {@code CSVReader.ArrayReader},
 * {@code CSVMapReader.MapReader} and {@code CSVBeanReader.BeanReader}.
 *
 * @param <E>
 */
abstract class Reader<E> implements Iterator<E>{
    private char escapeChar;
    private char quoteChar;
    private char delimiter;

    Reader(char delimiter, char quoteChar, char escapeChar) {
        this.delimiter = delimiter;
        this.quoteChar = quoteChar;
        this.escapeChar = escapeChar;
    }

    /**
     * Split one line string to a {@code String} array.
     *
     * @param str one line string.
     * @return the result of splitting the {@code str}.
     */
    String[] split(String str) {
        List<String> fields = new ArrayList<>();
        int start = 0;

        while (true) {
            StringBuilder builder = new StringBuilder();
            start = nextField(str, start, builder);
            fields.add(builder.toString());
            if (start == -1) {
                break;
            }
            start++;
        }

        return fields.toArray(new String[0]);
    }


    /**
     * From the index {@code start} to find the index of the first {@code escapeChar} or the
     * first {@code quoteChar} or the first {@code delimiter}.
     *
     * @param str one line string.
     * @param start the index from which to find.
     * @return the index of the first escapeChar or the first quoteChar or the first delimiter,
     * or -1 if catch the end of the string.
     */
    private int findNextIndex(String str, int start) {
        int len = str.length();
        for (int i = start; i < len; i++) {
            char c = str.charAt(i);
            if (c == escapeChar || c == quoteChar || c == delimiter)
                return i;
        }
        return -1;
    }

    /**
     * Get a valid field in the one line string {@code str}, from the index {@code fromIndex}.
     * The value of the field is stored in {@code builder}, and the delimiter, quoteChar, escapeChar
     * will be ignored.
     *
     * @param str one line string.
     * @param fromIndex the index from which to start parse.
     * @param builder the builder for storing the value of parsed field.
     * @return the index of the first true delimiter, or -1 if catch the end of the string.
     */
    private int nextField(String str, int fromIndex, StringBuilder builder) {
        int len = str.length();
        if (fromIndex >= len) {
            return -1;
        }

        boolean isQuoting = false;
        int start = fromIndex;

        int i = fromIndex;
        while (true) {
            i = findNextIndex(str, i);
            if (i == -1) {
                builder.append(str.substring(start));
                return -1;
            }

            builder.append(str, start, i);
            char c = str.charAt(i);

            /* the escapeChar may be same to the quoteChar, careful handle it. */
            if ((isQuoting && c == escapeChar) || (quoteChar != escapeChar && c == escapeChar)) {
                if (i + 1 < len && str.charAt(i + 1) == quoteChar) {
                    builder.append(str, start, i);
                    builder.append(quoteChar);
                    i += 1;
                    start = i + 1;
                }
            }

            if (!isQuoting) {
                if (c == quoteChar) {
                    isQuoting = true;
                    start = i + 1;
                } else if (c == delimiter) {
                    /* catch you, the end index of the delimiter of the first valid field. */
                    return i;
                }
            } else {
                if (c == quoteChar) {
                    isQuoting = false;
                    start = i + 1;
                }
            }

            i++;
        }

    }


}
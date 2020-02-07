package com.nxp.id.tp.common;

import java.util.Arrays;
import java.util.List;

/**
 * Utility String operations.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class StringUtil {

  /** width of horizontal bars. */
  public static final int BAR_WIDTH = 80;

  /** horizontal bar. */
  public static final String BAR = repeat('-', BAR_WIDTH);

  /** Hi checkstyle, this is a ten. */
  private static final int TEN = 10;

  /**
   * Repeats 'len' chars and returns the sequence as String.
   *
   * @param fillChar The char to repeat
   * @param len      The length of the resulting string
   *
   * @return The sequence {@link String}
   */
  public static String repeat(final char fillChar, final int len) {

    if (len < 0) {
      throw new IllegalArgumentException("Negative lengths are not allowed");
    }

    if (len == 0) {
      return "";
    }

    char[] arr = new char[len];
    Arrays.fill(arr, fillChar);
    return new String(arr);
  }

  /**
   * For each gap length, a sequence of 'fill_char's, delimited by the
   * 'separator' is created and returned as {@link String}. The sequence
   * always starts and ends with a 'separator'. Note that the separator chars
   * are not included in the 'gap_lengths' argument.
   *
   * E.g. repeatSeparate('x', '*', 5, 3, 4) returns
   * xxxxx*xxx*xxxx
   *   5    3   4
   *
   * @param fillChar   The character to repeat
   * @param separator  The separator char
   * @param gapLengths The lengths of the individual sequences
   *
   * @return The sequence-separator-sequence-.. {@link String}
   */
  public static String repeatSeparate(final char fillChar, final char separator,
                                      final int... gapLengths) {
    StringBuilder sb = new StringBuilder(gapLengths.length * TEN);

    for (int len : gapLengths) {
      if (len < 0) {
        throw new IllegalArgumentException("Negative lengths are not allowed");
      }

      sb.append(separator).append(repeat(fillChar, len));
    }

    sb.append(separator);
    return sb.toString();
  }

  /**
   * Counts the number of occurrences of 'to_count' in 'in'.
   *
   * @param in      The char to count the occurrences of 'to_count' in
   * @param toCount The string to count the occurrences of
   *
   * @return The number of occurences
   */
  public static int countOccurrences(final String in, final char toCount) {
    return in.length() - in.replace(String.valueOf(toCount), "").length();
  }

  /**
   * Counts the number of occurrences of 'to_count' in 'in'.
   *
   * @param in      The string to count the occurrences of 'to_count' in
   * @param toCount The string to count the occurrences of
   *
   * @return The number of occurences
   */
  public static int countOccurrences(final String in, final String toCount) {
    return (in.length() - in.replace(String.valueOf(toCount), "").length()) /
        toCount.length();
  }

  /**
   * Returns the length of the longest string in the given input.
   *
   * @param input The strings to consider
   *
   * @return The string with the longest length
   */
  public static int getLongest(final Iterable<String> input) {
    int max = -1;
    for (String s : input) {

      int len = s.length();
      if (len > max) {
        max = len;
      }
    }

    return max;
  }

  /**
   * Returns true if the given reference string equals any one of the candidate
   * strings.
   *
   * @param ignoreCase Whether to perform case-insensitive comparisons or not
   * @param reference  The reference object
   * @param candidates The candidates
   *
   * @return true if reference.equals(candidate[i])
   */
  public static boolean equalsAnyString(final boolean ignoreCase,
                                        final String reference,
                                        final String... candidates) {
    for (String s : candidates) {

      if (ignoreCase && reference.equalsIgnoreCase(s)) {
        return true;
      } else if (reference.equals(s)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns a 'separator'-separated list of the given objects
   * (human-readable form of the array).
   *
   * @param <T> The type of the objects to get string representations from
   *
   * @param arr       The array to get the human-readable representation of
   * @param separator The custom separator string to use
   *
   * @return The list
   */
  public static <T> String getStringList(final T[] arr,
                                         final String separator) {
    StringBuilder sb = new StringBuilder();

    for (Object obj : arr) {
      sb.append(obj.toString()).append(separator);
    }
    sb.setLength(sb.length() - separator.length());

    return sb.toString();
  }

  /**
   * Returns a comma-separated list of the given objects
   * (human-readable form of the array).
   *
   * @param <T> The type of the objects to get string representations from
   *
   * @param arr The array to get the human-readable representation of
   *
   * @return The list
   */
  public static <T> String getStringList(final T[] arr) {

    return getStringList(arr, ", ");
  }

  /**
   * Returns a comma-separated list of the given objects
   * (human-readable form of the list).
   *
   * @param <T> The type of the objects to get string representations from
   *
   * @param arr       The list to get the human-readable representation of
   * @param separator The custom separator string to use
   *
   * @return The list
   */
  public static <T> String getStringList(final List<T> arr,
                                         final String separator) {
    return getStringList(arr.toArray(), separator);
  }

  /**
   * Returns a comma-separated list of the given objects
   * (human-readable form of the list).
   *
   * @param <T> The type of the objects to get string representations from
   *
   * @param arr The list to get the human-readable representation of
   *
   * @return The list
   */
  public static <T> String getStringList(final List<T> arr) {
    return getStringList(arr.toArray());
  }

  /**
   * cf. Python's join(). Joins a couple of arguments together, having 'glue'
   * in between each arg. So it will be arg1||glue||arg2||glue||arg3...
   *
   * @param glue To be put in between the arguments
   * @param args The arguments to join
   *
   * @return The joined string
   */
  public static String join(final String glue, final String... args) {
    if (args.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder();

    for (String arg : args) {
      sb.append(arg).append(glue);
    }
    sb.setLength(sb.length() - glue.length());

    return sb.toString();
  }

  /**
   * Ctor. Utility class, do not instantiate.
   */
  private StringUtil() {
    // do not instantiate.
  }
}

package com.nxp.id.tp.common;

import java.io.File;
import java.util.Arrays;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Utility stuff for the preprocessor tool and whatever Java code can make use
 * of it.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Util {

  /** hex radix. */
  public static final int HEX_BASE = 16;

  /** hex prefix. */
  public static final String HEX_PREFIX = "0x";

  /** \n on unix derivatives, \r\n on windows. */
  public static final String NEWLINE = System.getProperty("line.separator");

  /** random number generator. */
  public static final Random RNG = new SecureRandom();

  /**
   * Simply wraps the given entries in an (Array) List.
   *
   * @param <T>     The type of entries to wrap in a list
   * @param entries The entries to wrap in a list
   *
   * @return The list containing the entries
   */
  public static <T> List<T> wrapInList(final Collection<T> entries) {
    List<T> out = new ArrayList<T>(entries.size());
    for (T entry : entries) {
      out.add(entry);
    }
    return out;
  }

  /**
   * Simply wraps the given entries in an (Array) List.
   *
   * @param <T>     The type of entries to wrap in a list
   * @param entries The entries to wrap in a list
   *
   * @return The list containing the entries
   */
  public static <T> List<T> wrapInList(final T... entries) {
    List<T> out = new ArrayList<T>(entries.length);
    for (T entry : entries) {
      out.add(entry);
    }
    return out;
  }

  /**
   * Simply wraps the given entries in an (Array) List.
   *
   * @param <T>     The type of entries to wrap in a list
   * @param entries The entries to wrap in a list
   *
   * @return The list containing the entries
   */
  public static <T> List<T> wrapInListNonNull(final Collection<T> entries) {
    List<T> out = new ArrayList<T>(entries.size());
    for (T entry : entries) {
      if (entry != null) {
        out.add(entry);
      }
    }
    return out;
  }

  /**
   * Simply wraps the given entries in an (Array) List.
   *
   * @param <T>     The type of entries to wrap in a list
   * @param entries The entries to wrap in a list
   *
   * @return The list containing the entries
   */
  public static <T> List<T> wrapInListNonNull(final T... entries) {
    List<T> out = new ArrayList<T>(entries.length);
    for (T entry : entries) {
      if (entry != null) {
        out.add(entry);
      }
    }
    return out;
  }

  /**
   * Simply wraps the given key and value in a (Hash)Map.
   *
   * @param <A>     The type of the key entry
   * @param <B>     The type of the value entry
   *
   * @param key   The key entry
   * @param value The value entry
   *
   * @return The map containing the key-value pair
   */
  public static <A, B> Map<A, B> wrapInMap(final A key, final B value) {
    Map<A, B> out = new HashMap<A, B>(1);
    out.put(key, value);
    return out;
  }

  /**
   * Tries to find the string 'ref' within 'toCheck' and returns the index to
   * that entry.
   * @param ref       the reference string to be searched.
   * @param toCheck   the list of possible candidates.
   * @return the index of ref within toCheck or -1 if it could not be found.
   */
  public static int findMatching(final String ref, final List<String> toCheck) {
    return findMatching(ref, false, toCheck);
  }

  /**
   * @param ref        the reference string to be searched.
   * @param ignoreCase indicates if the search should be case sensitive or not.
   * @param toCheck    the list of possible candidates.
   * @return the index of ref within toCheck or -1 if it could not be found.
   */
  public static int findMatching(final String ref, final boolean ignoreCase,
                                 final List<String> toCheck) {

    for (int i = 0; i < toCheck.size(); i++) {
      if ((ignoreCase && ref.compareToIgnoreCase(toCheck.get(i)) == 0) ||
          ref.compareTo(toCheck.get(i)) == 0) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Helper method to find a string in a given list of strings. This method
   * will return the index of the string in the given array, or -1 if no string
   * matches the reference string.<p>
   * See also {@link Util#findMatching(String, boolean, String...)}.
   * @param ref       the reference string.
   * @param toCheck   the array of strings where the reference string shall
   *                  be found.
   * @return    -1 if the reference string could not be found, or the index of
   *            the string in the given array. Only returns the first occurrence
   *            of 'ref' within the 'toCheck' array!
   */
  public static int findMatching(final String ref, final String... toCheck) {
    return findMatching(ref, false, toCheck);
  }

  /**
   * Helper method to find a string in a given list of strings. This method
   * will return the index of the string in the given array, or -1 if no string
   * matches the reference string.
   * @param ref         the reference string.
   * @param ignoreCase  flag indicating if the case of the string should be
   *                    ignored or not.
   * @param toCheck     the array of strings where the reference string shall
   *                    be found.
   * @return    -1 if the reference string could not be found, or the index of
   *            the string in the given array. Only returns the first occurrence
   *            of 'ref' within the 'toCheck' array!
   */
  public static int findMatching(final String ref, final boolean ignoreCase,
                                 final String... toCheck) {

    for (int i = 0; i < toCheck.length; i++) {
      if ((ignoreCase && ref.compareToIgnoreCase(toCheck[i]) == 0) ||
          ref.compareTo(toCheck[i]) == 0) {
        return i;
      }
    }

    return -1;
  }

  /**
   * Creates a temporary directory. Throws {@link IllegalStateException} if
   * unsuccessful due to too many attempts.
   *
   * @return Create directory
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public static File createTempDir() {
    File baseDir = new File(System.getProperty("java.io.tmpdir"));
    String baseName = System.currentTimeMillis() + "-";

    for (int cnt = 0; cnt < 10000; cnt++) {
      File tmpDir = new File(baseDir, baseName + cnt);
      if (tmpDir.mkdir()) {
        tmpDir.deleteOnExit();
        return tmpDir;
      }
    }

    throw new IllegalStateException("Failed to create temporary directory");
  }

  /**
   * Computes a hash code over the given objects. Essentially what
   * Objects.hash() does in Java 8+.
   *
   * @param objects Objects to compute a hash code over
   *
   * @return The hashcode
   */
  public static int computeHashCode(final Object... objects) {

    return Arrays.hashCode(objects);
  }

  /**
   * Private ctor, do not instantiate.
   */
  private Util() {
    // not to be instantiated.
  }
}

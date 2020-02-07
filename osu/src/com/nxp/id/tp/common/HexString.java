package com.nxp.id.tp.common;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Hex String representation - the better byte array (tm). Provides a
 * {@link String}-like interface to byte arrays.
 *
 * Indexing of {@link HexString}s is always byte-oriented, not via String
 * offsets (which are twice the byte offsets for hex strings).
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public class HexString
    implements Comparable<HexString>, Cloneable, Iterable<Byte>, FileFormat {

  /** Use this instance to represent empty hex strings. */
  public static final HexString EMPTY = new HexString("");

  /** hex chars, as char[]. */
  private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

  /** hex radix. */
  private static final int HEX_BASE = 16;

  /** byte mask. */
  private static final int MASK_BYTE = 0xff;

  /** lower nibble mask. */
  private static final int MASK_LOWER_NIBBLE = 0x0f;

  /** shift amount when dividing by 16. */
  private static final int DIV_16 = 4;

  /** buffer size when reading from input streams. */
  private static final int READ_BUFFER_SIZE = 4096;

  /** the actual data. */
  private byte[] data;

  /** ISO7816 padding byte, followed by zeroes. */
  public static final byte ISO7816_PADDING_DELIMITER = (byte)0x80;

  /**
   * Supported padding operations.
   * See <a href="http://en.wikipedia.org/wiki/Padding_%28cryptography%29">
   *        http://en.wikipedia.org/wiki/Padding_%28cryptography%29
   *     </a>
   */
  public enum PadMode {

    /**
     * ANSI X9.23 padding (right padded with N zeros, with N being the very
     *  last byte).
     */
    ANSI_X932,

    /** ISO/IEC 7816-4 padding (right padded with 80000...00). */
    ISO7816,

    /** left-padded with zeros. */
    ZEROLEFT,

    /** right-padded with zeros. */
    ZERORIGHT,

    /** right-padded with zeros, unless already aligned (LEGACY). */
    ZERORIGHT_IF_NOT_ALIGNED,

    /**
     * PKCS7 padding (right padded with each byte being the number of
     * bytes that were used for padding).
     */
    PKCS7,

    /** No padding. Leads to a NOP. */
    NOPADDING
  }

  /**
   * Concatenates given hex strings to one, in order of the given list.
   *
   * @param input The hex strings to concatenate
   *
   * @return The concatenated hex string
   */
  public static HexString concat(final List<HexString> input) {

    // note1: ByteArrayOutputStream#close() is empty, no need to call it
    // note2, ByteArrayOutputStream#write(b, off, len) does not throw,
    // contrary to the super class' ByteArrayOutputStream#write(b)

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    for (HexString hs : input) {
      bos.write(hs.data, 0, hs.data.length);
    }
    return new HexString(bos.toByteArray());
  }

  /**
   * Concatenates given hex strings to one, in order of the given array.
   *
   * @param input The hex strings to concatenate
   *
   * @return The concatenated hex string
   */
  public static HexString concat(final HexString... input) {

    // note: Arrays.asList is horrible, but here we only iterate over
    // it so it's bearable
    return concat(Arrays.asList(input));
  }

  /**
   * Reads input string data (containing only valid hex chars) into a hex
   * string.
   *
   * @param data The hex string in string representation
   *
   * @return The corresponding hex string
   */
  public static HexString fromString(final String data) {

    if (data == null) {
      throw new IllegalArgumentException("Input may not be null");
    }

    String tmp = data;

    if (tmp.startsWith("0x")) {
      tmp = tmp.substring(2);
    }

    // remove whitespace stuff.
    tmp = tmp.replace(" ", "").replace("\t", "").replace("\r", "").replace("\n",
                                                                           "");

    for (char c : tmp.toCharArray()) {
      if (!isHexChar(c)) {
        throw new NumberFormatException(
            tmp + " is not a valid hex string, found '" + c + "'");
      }
    }

    // byte padding
    if (tmp.length() % 2 != 0) {
      tmp = "0" + tmp;
    }
    return new HexString(tmp);
  }

  /**
   * Reads input ASCII string data into a hex string.
   *
   * @param data The ASCII string
   *
   * @return The corresponding hex string
   */
  public static HexString fromAsciiString(final String data) {

    return new HexString(data.getBytes());
  }

  /**
   * Converts a byte[] to a {@link HexString} instance.
   *
   * @param data The byte[] to read from
   *
   * @return The corresponding hex string
   */
  public static HexString fromByteArray(final byte[] data) {

    return fromByteArray(data, 0, data.length);
  }

  /**
   * Converts a byte[] to a {@link HexString} instance.
   *
   * @param data   The byte[] to read from
   * @param offset Offset in data
   *
   * @return The corresponding hex string
   */
  public static HexString fromByteArray(final byte[] data, final int offset) {

    return fromByteArray(data, offset, data.length);
  }

  /**
   * Converts a byte[] to a {@link HexString} instance.
   *
   * @param data   The byte[] to read from
   * @param from   Offset data
   * @param to     Upper index in data
   *
   * @return The corresponding hex string
   */
  public static HexString fromByteArray(final byte[] data, final int from,
                                        final int to) {

    if (data == null) {
      throw new IllegalArgumentException("Input may not be null");
    }

    if (from < 0 || (from != 0 && from > data.length)) {
      throw new IllegalArgumentException("Invalid from index " + from);
    }

    if (to < from || to < 0 || (to != 0 && to > data.length)) {
      throw new IllegalArgumentException("Invalid to index " + to);
    }

    byte[] out = new byte[to - from];
    System.arraycopy(data, from, out, 0, out.length);
    return new HexString(out);
  }

  /**
   * Converts a byte into a {@link HexString} instance.
   *
   * @param data The byte to convert
   *
   * @return The corresponding hex string
   */
  @SuppressWarnings({"checkstyle:magicnumber",
                     "checkstyle:avoidinlineconditionals"})
  public static HexString
  fromByte(final byte data) {

    return new HexString(new byte[] {data});
  }

  /**
   * Converts a short into a {@link HexString} instance.
   *
   * @param data The integer to convert
   *
   * @return The corresponding hex string
   */
  @SuppressWarnings({"checkstyle:magicnumber",
                     "checkstyle:avoidinlineconditionals"})
  public static HexString
  fromShort(final short data) {

    byte[] bytes = new byte[((data & 0xFF00) > 0) ? 2 : 1];

    for (int i = 0, k = (bytes.length - 1) * 8; i < bytes.length; i++, k -= 8) {
      bytes[i] = (byte)(data >> k);
    }
    return new HexString(bytes);
  }

  /**
   * Converts an integer into a {@link HexString} instance.
   *
   * @param data The integer to convert
   *
   * @return The corresponding hex string
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public static HexString fromInteger(final int data) {

    int byteLen = Math.max(1, 4 - (Integer.numberOfLeadingZeros(data) >> 3));
    byte[] bytes = new byte[byteLen];

    for (int i = 0, k = (bytes.length - 1) * 8; i < bytes.length; i++, k -= 8) {
      bytes[i] = (byte)(data >> k);
    }
    return new HexString(bytes);
  }

  /**
   * Converts a long into a {@link HexString} instance.
   *
   * @param data The long to convert
   *
   * @return The corresponding hex string
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public static HexString fromLong(final long data) {

    int byteLen = Math.max(1, 8 - (Long.numberOfLeadingZeros(data) >> 3));
    byte[] bytes = new byte[byteLen];

    for (int i = 56; i >= 0; i -= 8) {
      if (((data >> i) & 0xFF) > 0) {
        bytes = new byte[(i >> 3) + 1];
        break;
      }
    }

    for (int i = 0, k = (bytes.length - 1) * 8; i < bytes.length; i++, k -= 8) {
      bytes[i] = (byte)(data >> k);
    }
    return new HexString(bytes);
  }

  /**
   * Converts a {@link BigInteger} into a {@link HexString} instance.
   *
   * @param data The {@link BigInteger} to convert
   *
   * @return The corresponding hex string
   */
  public static HexString fromBigInteger(final BigInteger data) {
    byte[] out = data.toByteArray();
    if ((out.length > 1) && (out[0] == (byte)0x00)) {
      out = Arrays.copyOfRange(out, 1, out.length);
    }
    return new HexString(out);
  }

  /**
   * Converts a {@link BigInteger} into a {@link HexString} instance.
   * Zero-left pads to expected length, does not truncate.
   *
   * @param data           The {@link BigInteger} to convert
   * @param expectedLength The expected length
   *
   * @return The corresponding hex string
   */
  public static HexString fromBigInteger(final BigInteger data,
                                         final int expectedLength) {
    HexString out = fromBigInteger(data);
    if (out.length() < expectedLength) {
      return out.setLength(expectedLength);
    }
    return out;
  }

  /**
   * Reads a Hex String from a file.
   *
   * @param filename The file to read from
   * @param binary Whether to read as binary or character-based, true for binary
   *
   * @return The read hex string
   *
   * @throws IOException On I/O errors
   */
  public static HexString fromFile(final String filename, final boolean binary)
      throws IOException {

    return fromFile(new File(filename), binary);
  }

  /**
   * Reads a Hex String from a file.
   *
   * @param file   The file to read from
   *
   * @return The read hex string
   *
   * @throws IOException On I/O errors
   */
  public static HexString fromFile(final File file) throws IOException {

    return fromFile(file, false);
  }

  /**
   * Reads a Hex String from an input stream. Does *not* close the input stream.
   * Reads char-based.
   *
   * @param is     The input stream to read from
   *
   * @return The read hex string
   *
   * @throws IOException On I/O errors
   */
  public static HexString fromInputStream(final InputStream is)
      throws IOException {

    return fromInputStream(is, false);
  }

  /**
   * Reads a Hex String from a file.
   * Reads char-based.
   *
   * @param filename The file to read from
   *
   * @return The read hex string
   *
   * @throws IOException On I/O errors
   */
  public static HexString fromFile(final String filename) throws IOException {

    return fromFile(filename, false);
  }

  /**
   * Reads a Hex String from a file.
   *
   * @param file   The file to read from
   * @param binary Whether to read as binary or character-based, true for binary
   *
   * @return The read hex string
   *
   * @throws IOException On I/O errors
   */
  public static HexString fromFile(final File file, final boolean binary)
      throws IOException {

    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      return fromInputStream(fis, binary);
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
  }

  /**
   * Reads a Hex String from an input stream. Does *not* close the input stream.
   *
   * @param is     The input stream to read from
   * @param binary Whether or not to read binary data or character based
   *
   * @return The read hex string
   *
   * @throws IOException On I/O errors
   */
  public static HexString fromInputStream(final InputStream is,
                                          final boolean binary)
      throws IOException {

    if (binary) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream(is.available());

      byte[] buf = new byte[READ_BUFFER_SIZE];
      int numRead = 0;

      while ((numRead = is.read(buf)) >= 0) {
        bos.write(buf, 0, numRead);
      }
      return new HexString(bos.toByteArray());

    } else {

      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      StringBuilder sb = new StringBuilder();
      String line = null;
      while ((line = br.readLine()) != null) {
        sb.append(line.trim());
      }

      return fromString(sb.toString());
    }
  }

  /**
   * Wrapper for {@link String#format(String, Object...)}.
   *
   * @param format The format specifier. Note that only %x shall be used in
   *               there.
   * @param args   Any format arguments
   *
   * @return The formatted hex string
   */
  public static HexString format(final String format, final Object... args) {
    return new HexString(String.format(format, args));
  }

  /**
   * Returns a random hex string of given bit length.
   *
   * @param bitLength The length of the random, in bits.
   *
   * @return A {@link HexString}
   *
   * @throws IllegalArgumentException on negative bit length
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public static HexString random(final int bitLength)
      throws IllegalArgumentException {

    if (bitLength < 0) {
      throw new IllegalArgumentException("Bit length may not be negative");
    }

    if (bitLength == 0) {
      return HexString.EMPTY;
    }

    byte[] data = new byte[(int)Math.ceil((float)bitLength / Byte.SIZE)];
    Util.RNG.nextBytes(data);

    if (bitLength % Byte.SIZE > 0) {
      data[0] &= (0xFF >> (Byte.SIZE - (bitLength % Byte.SIZE)));
      data[0] |= (0x80 >> (Byte.SIZE - (bitLength % Byte.SIZE)));
    } else {
      data[0] |= 0x80;
    }

    return new HexString(data);
  }

  /**
   * Returns a zeroed hex string of given byte length.
   *
   * @param length The length of the zero string, in bytes.
   *
   * @return A {@link HexString}
   *
   * @throws IllegalArgumentException on negative length
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public static HexString zero(final int length)
      throws IllegalArgumentException {

    if (length < 0) {
      throw new IllegalArgumentException("Bit length may not be negative");
    }

    if (length == 0) {
      return HexString.EMPTY;
    }

    return new HexString(new byte[length]);
  }

  /**
   * This method fills a HexString with a pattern value to a given target
   * length.
   * e.g. fill(HexString.fromSring("1234"), 3); will return 123412.
   *
   * @param pattern      the pattern the HexString will be filled with.
   * @param targetLength the length of the output HexString
   * @return the filled HexString
   */
  public static HexString fill(final HexString pattern,
                               final int targetLength) {
    if (targetLength < 0) {
      throw new IllegalArgumentException("Bit length may not be negative");
    }

    HexString out = HexString.EMPTY;
    for (int i = 0, pos = 0; i < targetLength; i++) {
      if (pos < pattern.length()) {
        out = out.append(pattern.hexAt(pos));
        pos++;
      } else {
        pos = 0;
        i--;
      }
    }
    return out;
  }

  /**
   * Returns the length of the longest hex string in the given input.
   *
   * @param input The hex strings to consider
   *
   * @return The length of the longes input hex string
   */
  public static int getLongest(final Iterable<HexString> input) {
    int max = -1;
    for (HexString s : input) {

      int len = s.length();
      if (len > max) {
        max = len;
      }
    }

    return max;
  }

  /**
   * Ctor. Verifies that the given input string is indeed a valid hex string
   * and sets the member.
   *
   * @param theData The string data
   */
  protected HexString(final String theData) {

    int len = theData.length();
    data = new byte[len >> 1];

    for (int i = 0; i < len; i += 2) {
      data[i / 2] =
          (byte)((Character.digit(theData.charAt(i), HEX_BASE) << DIV_16) +
                 Character.digit(theData.charAt(i + 1), HEX_BASE));
    }
  }

  /**
   * Ctor. Converts the given byte[] to a {@link HexString}.
   *
   * @param theData The byte[] data
   */
  protected HexString(final byte[] theData) { this.data = theData; }

  /**
   * {@link HexString} to byte[] conversion.
   *
   * @return The hex string as byte[].
   */
  public final byte[] toByteArray() {

    // needs to be copied to ensure immutability.
    return toByteArray(0, data.length);
  }

  /**
   * {@link HexString} to byte[] conversion.
   *
   * @param offset offset to start from
   * @param length length
   *
   * @return The hex string as byte[].
   */
  public final byte[] toByteArray(final int offset, final int length) {

    if (offset < 0 || offset > data.length) {
      throw new IllegalArgumentException("Offset out of bounds");
    }

    if (length < 0 || offset + length > data.length) {
      throw new IllegalArgumentException("Length out of bounds");
    }

    return Arrays.copyOfRange(data, offset, offset + length);
  }

  /**
   * {@link HexString} to integer conversion. Throws a
   * {@link NumberFormatException} if the string is too long to fit in an
   * integer.
   *
   * @return The hex string as integer
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final int toInteger() {

    if (data.length > 4 || (data.length == 4 && data[0] > 0x7F)) {
      throw new NumberFormatException(
          "Cannot convert to integer, input too large");
    }

    int out = 0;
    for (int i = 0, k = data.length - 1; i < data.length; i++, k--) {
      out |= ((data[i] & 0xFF) << (k << 3));
    }

    return out;
  }

  /**
   * {@link HexString} to long conversion. Throws a
   * {@link NumberFormatException} if the string is too long to fit in a
   * long.
   *
   * @return The hex string as long
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final long toLong() {

    if (data.length > 8 || (data.length == 8 && data[0] > 0x7F)) {
      throw new NumberFormatException(
          "Cannot convert to integer, input too large");
    }

    long out = 0;
    for (int i = 0, k = data.length - 1; i < data.length; i++, k--) {
      long tmp = data[i] & 0xFF;
      out |= tmp << (k << 3);
    }

    return out;
  }

  /**
   * {@link HexString} to {@link BigInteger} conversion.
   *
   * @return The hex string as long
   */
  public final BigInteger toBigInteger() { return new BigInteger(1, data); }

  @Override
  public final String toString() {
    return toString(0, data.length);
  }

  /**
   * To String conversion with a given radix.
   *
   * @param base The radix
   *
   * @return The string representation using the given base
   */
  public final String toString(final int base) {

    return toBigInteger().toString(base);
  }

  /**
   * To String conversion of a sub range.
   *
   * @param offset The offset to start from
   * @param length The length
   *
   * @return The string representation using the given base
   */
  public final String toString(final int offset, final int length) {

    if (offset < 0 || offset > data.length) {
      throw new IllegalArgumentException("Offset out of bounds");
    }

    if (length < 0 || offset + length > data.length) {
      throw new IllegalArgumentException("Length out of bounds");
    }

    char[] hexChars = new char[length << 1];
    for (int k = offset, j = 0, v = 0; j < length; j++, k++) {
      v = data[k] & MASK_BYTE;
      hexChars[(j << 1)] = HEX_CHARS[v >>> DIV_16];
      hexChars[(j << 1) + 1] = HEX_CHARS[v & MASK_LOWER_NIBBLE];
    }

    return new String(hexChars);
  }

  /**
   * To ASCII String conversion.
   *
   * @return The ASCII string representation
   */
  public final String toAsciiString() { return new String(data); }

  /**
   * Returns the length of the hex string (in bytes, not string length!).
   *
   * @return The length in bytes
   */
  public final int length() { return data.length; }

  /**
   * Returns the byte at the given index within the string
   * (byte-index, not string index!).
   *
   * @param index the position
   *
   * @return The byte at the given position
   */
  public final byte at(final int index) {
    if (index < 0 || index >= data.length) {
      throw new StringIndexOutOfBoundsException("Index '" + index +
                                                "' out of bounds");
    }

    return data[index];
  }

  /**
   * Returns the byte at the given index within the string
   * (byte-index, not string index!).
   *
   * @param index the position
   *
   * @return The Hex value at the given position
   */
  public final HexString hexAt(final int index) {
    if (index < 0 || index >= data.length) {
      throw new StringIndexOutOfBoundsException("Index '" + index +
                                                "' out of bounds");
    }

    return substring(index, index + 1);
  }

  /**
   * Returns how often a sequence is contained in the hex string.
   *
   * @param sequence The sequence to look for
   *
   * @return The byte at the given position
   */
  public final int getOccurrences(final HexString sequence) {

    if (sequence == null) {
      throw new IllegalArgumentException("sequence may not be null");
    }

    int occurrences = 0;
    HexString tmp = this;

    while (tmp.length() > 0) {

      int idx = tmp.indexOf(sequence);

      if (idx < 0) {
        break;
      }

      occurrences++;
      tmp = tmp.substring(idx + sequence.length());
    }

    return occurrences;
  }

  /**
   * Substring functionality. Uses byte-wise indexing.
   *
   * @param from The index to start from
   *
   * @return The substring
   */
  public final HexString substring(final int from) {

    if (from < 0 || from > data.length) {
      throw new StringIndexOutOfBoundsException("Index '" + from +
                                                "' out of bounds");
    }

    byte[] out = new byte[data.length - from];
    System.arraycopy(data, from, out, 0, out.length);
    return new HexString(out);
  }

  /**
   * Substring functionality. Uses byte-wise indexing.
   *
   * @param from The index to start from
   * @param to   The end index
   *
   * @return The substring
   */
  public final HexString substring(final int from, final int to) {

    if (from < 0 || from > data.length) {
      throw new StringIndexOutOfBoundsException("Index '" + from +
                                                "' out of bounds");
    }

    if (to < 0 || to > data.length) {
      throw new StringIndexOutOfBoundsException("Index '" + to +
                                                "' out of bounds");
    }

    if (to < from) {
      throw new IllegalArgumentException(
          "To-Index cannot be smaller than 'From-Index");
    }

    byte[] out = new byte[to - from];
    System.arraycopy(data, from, out, 0, out.length);
    return new HexString(out);
  }

  /**
   * Substring method, which takes a length parameter instead of an end index.
   * from is a byte-index and len is also in bytes.
   *
   * @param from The index to start extracting the substring from
   * @param len  The length of the substring
   *
   * @return The substring
   */
  public final HexString substringLen(final int from, final int len) {

    if (len < 0) {
      throw new IllegalArgumentException("Len may not be negative");
    }

    return substring(from, from + len);
  }

  /**
   * this + other (string concatenation). Alias for {@link #append(HexString)}.
   *
   * @param other The string to append
   *
   * @return The concatenated string
   */
  public final HexString concat(final HexString other) { return append(other); }

  /**
   * Replaces all occurrences of 'sequence' with 'replacement'.
   *
   * @param sequence    to be replaced
   * @param replacement replacement
   *
   * @return the modified string
   */
  public HexString replace(final HexString sequence,
                           final HexString replacement) {
    if (sequence == null || replacement == null) {
      throw new IllegalArgumentException(
          "sequence/replacement may not be null");
    }

    int occ = getOccurrences(sequence);

    if (occ == 0) {
      return clone();
    }

    byte[] out = new byte[data.length +
                          (occ * (replacement.length() - sequence.length()))];
    int outCursor = 0;
    HexString tmp = this;
    while (tmp.data.length > 0) {

      int idx = tmp.indexOf(sequence);
      if (idx < 0) {
        System.arraycopy(tmp.data, 0, out, outCursor, tmp.data.length);
        break;
      }

      System.arraycopy(tmp.data, 0, out, outCursor, idx);
      outCursor += idx;
      System.arraycopy(replacement.data, 0, out, outCursor,
                       replacement.data.length);
      outCursor += replacement.data.length;

      tmp = tmp.substring(idx + sequence.length());
    }

    return new HexString(out);
  }

  /**
   * Replaces the sequence starting from index with the given sequence.
   * If the sequence is longer as the size, it will be appended.
   *
   * @param sequence it will replace the old sequence
   * @param index the start index of the replacement
   *
   * @return the HexString containing the replaced sequence
   */
  public HexString replaceAt(final int index, final HexString sequence) {

    if (index > length()) {
      throw new IllegalArgumentException("Index out of bounds");
    }

    HexString begin = HexString.EMPTY;
    HexString end = HexString.EMPTY;

    if (index != 0) {
      begin = substring(0, index);
    }

    if ((index + sequence.length()) < length()) {
      end = substring(index + sequence.length(), length());
    }

    return sequence.prepend(begin).append(end);
  }

  /**
   * Returns whether or not 'this' starts with the given sequence.
   *
   * @param sequence The candidate to check if the string starts with this
   *                 sequence
   *
   * @return True if it starts with 'sequence', false otherwise
   */
  public final boolean startsWith(final HexString sequence) {

    if (sequence == null) {
      throw new IllegalArgumentException("Sequence may not be null");
    }

    if (sequence.data.length > data.length) {
      return false;
    }

    for (int i = 0; i < sequence.data.length; i++) {
      if (data[i] != sequence.data[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns whether or not 'this' ends with the given sequence.
   *
   * @param sequence The candidate to check if the string ends with this
   *                 sequence
   *
   * @return True if it ends with 'sequence', false otherwise
   */
  public final boolean endsWith(final HexString sequence) {

    if (sequence == null) {
      throw new IllegalArgumentException("Sequence may not be null");
    }

    if (sequence.data.length > data.length) {
      return false;
    }

    for (int i = 0; i < sequence.data.length; i++) {
      if (data[data.length - sequence.data.length + i] != sequence.data[i]) {
        return false;
      }
    }

    return true;
  }

  /**
   * Wrapper for {@link String#indexOf(String)}.
   *
   * @param sequence The sequence to find
   *
   * @return The first index of the found sequence, or -1 if not found
   */
  public final int indexOf(final HexString sequence) {

    return indexOf(sequence, 0);
  }

  /**
   * Wrapper for {@link String#indexOf(String)},
   * but having a given start offset.
   *
   * @param sequence The sequence to find
   * @param offset   The offset index to use as start for searching
   *
   * @return The first index of the found sequence, or -1 if not found
   */
  public final int indexOf(final HexString sequence, final int offset) {

    if (sequence == null) {
      throw new IllegalArgumentException("Sequence may not be null");
    }

    if (data.length == 0 || sequence.data.length > data.length) {
      return -1;
    }

    for (int i = offset; i <= data.length - sequence.data.length; i++) {

      if (data[i] == sequence.data[0]) {
        int k = 0;
        for (; k < sequence.data.length; k++) {
          if (data[i + k] != sequence.data[k]) {
            break;
          }
        }

        if (k == sequence.data.length) {
          return i;
        }
      }
    }

    return -1;
  }

  /**
   * Returns the bit length of the data.
   *
   * @return the bit length
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final int bitLength() {

    if (data.length == 0) {
      return 0;
    }

    int i = 0;
    while (data[i] == (byte)0x00) {
      i++;
    }

    int mask = 0x80, k = 0;
    while (mask > 0) {
      if ((data[i] & mask) > 0) {
        return ((data.length - i) << 3) - k;
      }
      mask >>= 1;
      k++;
    }

    return 0;
  }

  /**
   * Inserts 'bytes' at the given index, thereby moving all contained data
   * after 'index' to index + bytes.length(). Does not replace.
   *
   * @param index The index to insert at
   * @param bytes The data to insert
   *
   * @return original data [0-i] + 'bytes' + original data [i+]
   *
   * @throws IllegalArgumentException on an invalid index
   */
  public final HexString insert(final int index, final HexString bytes) {

    if (index == 0) {
      return prepend(bytes);
    } else if (index == data.length) {
      return append(bytes);
    }

    if ((index < 0) || (index > data.length)) {
      throw new IllegalArgumentException("Invalid index");
    }

    byte[] out = new byte[data.length + bytes.data.length];

    System.arraycopy(data, 0, out, 0, index);
    System.arraycopy(bytes.data, 0, out, index, bytes.data.length);
    System.arraycopy(data, index, out, index + bytes.data.length,
                     data.length - index);
    return new HexString(out);
  }

  /**
   * Prepends 'toPrepend' to this.
   *
   * @param toPrepend The byte[] to prepend
   *
   * @return toPrepend + this
   */
  public final HexString prepend(final byte[] toPrepend) {

    if (toPrepend == null) {
      throw new IllegalArgumentException("toPrepend may not be null");
    }

    byte[] out = new byte[data.length + toPrepend.length];
    System.arraycopy(toPrepend, 0, out, 0, toPrepend.length);
    System.arraycopy(data, 0, out, toPrepend.length, data.length);
    return new HexString(out);
  }

  /**
   * Prepends 'toPrepend' to this.
   *
   * @param toPrepend The hex string to prepend
   *
   * @return toPrepend + this
   */
  public final HexString prepend(final HexString toPrepend) {

    if (toPrepend == null) {
      throw new IllegalArgumentException("toPrepend may not be null");
    }

    return prepend(toPrepend.data);
  }

  /**
   * Prepends 'toPrepend' to this.
   *
   * @param toPrepend The string to prepend
   *
   * @return toPrepend + this
   */
  public final HexString prepend(final String toPrepend) {

    if (toPrepend == null) {
      throw new IllegalArgumentException("toPrepend may not be null");
    }

    return prepend(HexString.fromString(toPrepend));
  }

  /**
   * Appends 'toAppend' to this.
   *
   * @param toAppend The byte[] to append
   *
   * @return this + toAppend
   */
  public final HexString append(final byte[] toAppend) {

    if (toAppend == null) {
      throw new IllegalArgumentException("toAppend may not be null");
    }

    byte[] out = new byte[data.length + toAppend.length];

    System.arraycopy(data, 0, out, 0, data.length);
    System.arraycopy(toAppend, 0, out, data.length, toAppend.length);

    return new HexString(out);
  }

  /**
   * Appends 'toAppend' to this.
   *
   * @param toAppend The byte[] to append
   * @param offset   The offset of where to start appending
   * @param length   The length
   *
   * @return this + toAppend[offset, length]
   */
  public final HexString append(final byte[] toAppend, final int offset,
                                final int length) {

    if (toAppend == null) {
      throw new IllegalArgumentException("toAppend may not be null");
    }

    if (offset < 0 || length < 0) {
      throw new IllegalArgumentException("offset and length may not be null");
    }

    if (offset + length > toAppend.length) {
      throw new IllegalArgumentException("length exceeds byte array");
    }

    byte[] out = new byte[data.length + length];

    System.arraycopy(data, 0, out, 0, data.length);
    System.arraycopy(toAppend, offset, out, data.length, length);

    return new HexString(out);
  }

  /**
   * Appends 'toAppend' to this.
   *
   * @param toAppend The byte to append
   *
   * @return this + toAppend
   */
  public final HexString append(final byte toAppend) {

    byte[] out = new byte[data.length + 1];

    System.arraycopy(data, 0, out, 0, data.length);
    out[data.length] = toAppend;

    return new HexString(out);
  }

  /**
   * Appends 'toAppend' to this.
   *
   * @param toAppend The hex string to append
   *
   * @return this + toAppend
   */
  public final HexString append(final HexString toAppend) {

    if (toAppend == null) {
      throw new IllegalArgumentException("toAppend may not be null");
    }

    return append(toAppend.data);
  }

  /**
   * Appends 'toAppend' to this.
   *
   * @param toAppend The string to append
   *
   * @return this + toAppend
   */
  public final HexString append(final String toAppend) {

    if (toAppend == null) {
      throw new IllegalArgumentException("toAppend may not be null");
    }

    return append(HexString.fromString(toAppend));
  }

  /**
   * Splits this based on the given pattern.
   *
   * @param pattern The pattern to use for splitting
   *
   * @return The split-offs
   */
  public final HexString[] split(final HexString pattern) {

    if (pattern == null) {
      throw new IllegalArgumentException("pattern may not be null");
    }

    List<HexString> list = new ArrayList<HexString>();

    HexString tmp = this;

    while (tmp.data.length > 0) {

      int idx = tmp.indexOf(pattern);
      if (idx < 0) {
        list.add(tmp);
        break;
      }

      list.add(tmp.substring(0, idx));

      // split pattern is at the end, add another empty string at the end
      if (idx + pattern.length() == tmp.length()) {
        list.add(HexString.EMPTY);
        break;
      }
      tmp = tmp.substring(idx + pattern.length());
    }

    return list.toArray(new HexString[list.size()]);
  }

  /**
   * Splits this based on the given pattern.
   *
   * @param pattern The pattern to use for splitting
   *
   * @return The split-offs
   */
  public final HexString[] split(final String pattern) {

    return split(HexString.fromString(pattern));
  }

  /**
   * Splits this based on the given (byte) lengths. The sum of the given
   * lengths needs to match the hex string's total length.
   *
   * @param lengths the length to split by
   *
   * @return The split chunks
   *
   * @throws IllegalArgumentException If the given lengths do not exactly match
   *                                  the hex string's size
   */
  public final HexString[] split(final int... lengths) {

    HexString[] out = new HexString[lengths.length];

    int offset = 0;
    for (int i = 0; i < lengths.length; i++) {

      int length = lengths[i];

      if (offset + length > data.length) {
        throw new IllegalArgumentException("Lengths exceed hex string");
      }

      out[i] = HexString.fromByteArray(data, offset, offset + length);
      offset += length;
    }

    if (offset != data.length) {
      throw new IllegalArgumentException(
          "Sum of given lengths is smaller than hex string's length");
    }

    return out;
  }

  /**
   * Sets a byte value at a given index.
   *
   * @param index the index of the byte to be set
   * @param value the byte's value
   *
   * @return a new {@link HexString} instance having the specified byte set
   *
   * @throws IndexOutOfBoundsException if index is not defined
   */
  public final HexString set(final int index, final byte value)
      throws IndexOutOfBoundsException {

    if (index < 0 || index >= data.length) {
      throw new IndexOutOfBoundsException("Index is not defined");
    }

    byte[] out = new byte[data.length];
    System.arraycopy(data, 0, out, 0, out.length);
    out[index] = value;
    return new HexString(out);
  }

  /**
   * returns true if this hex string does not contain data.
   *
   * @return true if empty, false otherwise
   */
  public final boolean isEmpty() { return data.length == 0; }

  /**
   * returns true if this hex string only consists of zeroes
   * (of arbitrary length).
   *
   * @return true if zero, false otherwise
   */
  public final boolean isZero() {
    for (byte b : data) {
      if (b != 0) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if and only if this hex string contains the specified
   * sequence of hex values.
   *
   * @param sequence the sequence to search for
   * @return true if this hex string contains <code>s</code>, false otherwise
   */
  public final boolean contains(final HexString sequence) {

    if (sequence == null) {
      throw new IllegalArgumentException("sequence may not be null");
    }

    return indexOf(sequence) >= 0;
  }

  /**
   * Returns true if and only if this hex string contains the specified
   * sequence of hex values.
   *
   * @param sequence the sequence to search for
   * @return true if this hex string contains 'sequence', false otherwise
   */
  public final boolean contains(final String sequence) {

    if (sequence == null) {
      throw new IllegalArgumentException("sequence may not be null");
    }

    return indexOf(HexString.fromString(sequence)) >= 0;
  }

  /**
   * Returns the hamming weight of this hex string.
   *
   * @return The hamming weight
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final int hammingWeight() {

    int hw = 0;
    for (int i = 0; i < data.length; i++) {
      int mask = 0x80;
      while (mask > 0) {

        if ((data[i] & mask) > 0) {
          hw++;
        }
        mask >>= 1;
      }
    }

    return hw;
  }

  /**
   * Pads the hex string according to the given mode.
   *
   * @see PadMode
   *
   * @param mode        The padding algorithm to apply
   * @param blockSize   The blocksize to pad for (in bytes!)
   *
   * @return The padded {@link HexString}
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final HexString padToBlockSize(final PadMode mode,
                                        final int blockSize) {

    byte[] out =
        new byte[data.length + (blockSize - (data.length % blockSize))];

    switch (mode) {

    case ANSI_X932:

      System.arraycopy(data, 0, out, 0, data.length);
      out[out.length - 1] = (byte)(out.length - data.length);
      break;

    case ISO7816:

      System.arraycopy(data, 0, out, 0, data.length);
      out[data.length] = (byte)0x80;
      break;

    case PKCS7:

      System.arraycopy(data, 0, out, 0, data.length);
      Arrays.fill(out, data.length, out.length,
                  (byte)(out.length - data.length));
      break;

    case ZEROLEFT:

      System.arraycopy(data, 0, out, out.length - data.length, data.length);
      break;

    case ZERORIGHT_IF_NOT_ALIGNED:

      if (data.length > 0 && data.length % blockSize == 0) {
        return this;
      }

      // intentional fall-through

    case ZERORIGHT:
      System.arraycopy(data, 0, out, 0, data.length);
      break;

    case NOPADDING:
    default:

      return this;
    }

    return new HexString(out);
  }

  /**
   * Pads the hex string, from either left or right, to a multiple of the given
   * block size. Pads with the given padding byte.
   *
   * @param left        Whether to apply it left or right
   * @param padByte     The pad byte's value
   * @param blockSize   The blocksize to pad for (in bytes!)
   *
   * @return The padded {@link HexString}
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final HexString padToBlockSize(final boolean left, final byte padByte,
                                        final int blockSize) {

    byte[] out =
        new byte[data.length + (blockSize - (data.length % blockSize))];

    if (left) {
      System.arraycopy(data, 0, out, out.length - data.length, data.length);
      Arrays.fill(out, 0, out.length - data.length, padByte);
    } else {
      System.arraycopy(data, 0, out, 0, data.length);
      Arrays.fill(out, data.length, out.length, padByte);
    }

    return new HexString(out);
  }

  /**
   * Unpads the hex string according to the given mode.
   *
   * @see PadMode
   *
   * @param mode        The padding algorithm to apply
   *
   * @return The unpadded {@link HexString}
   *
   * @throws IllegalStateException If the hex string is not correctly padded
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final HexString unpad(final PadMode mode)
      throws IllegalStateException {

    switch (mode) {

    case ANSI_X932: {

      int datalen = data.length - data[data.length - 1];

      if (datalen < 0) {
        throw new IllegalStateException(
            "Hex String is not correctly ANSI X9.23 padded");
      }

      for (int i = data.length - 2; i >= datalen; i--) {
        if (data[i] != (byte)0x00) {
          throw new IllegalStateException(
              "Hex String is not correctly ANSI X9.23 padded");
        }
      }

      return new HexString(Arrays.copyOfRange(data, 0, datalen));
    }
    case ISO7816: {

      int i = data.length - 1;
      while ((i >= 0) && (data[i] == (byte)0x00)) {
        i--;
      }

      if (data[i] != (byte)0x80) {
        throw new IllegalStateException(
            "Hex String is not padded according to ISO/IEC 7816-4");
      }

      return new HexString(Arrays.copyOfRange(data, 0, i));
    }
    case PKCS7: {

      byte padWidth = data[data.length - 1];
      int i = data.length - 1;
      int dataLen = data.length - padWidth;

      while (i > dataLen) {

        if (data[i] != padWidth) {
          throw new IllegalArgumentException(
              "Hex String is not padded according to PKCS7");
        }
        i--;
      }

      return new HexString(Arrays.copyOfRange(data, 0, i));
    }
    case ZEROLEFT: {

      int i = 0;
      while (i < data.length && data[i] == (byte)0x00) {
        i++;
      }

      return new HexString(Arrays.copyOfRange(data, i, data.length));
    }
    case ZERORIGHT:
    case ZERORIGHT_IF_NOT_ALIGNED: {

      int i = data.length - 1;
      while (i >= 0 && data[i] == (byte)0x00) {

        i--;
      }

      return new HexString(Arrays.copyOfRange(data, 0, i + 1));
    }
    case NOPADDING:
    default:

      return this;
    }
  }

  /**
   * Unpads the hex string, from either left or right, to a multiple of the
   * given block size. Pads with the given padding byte.
   *
   * @param left        Whether to apply it left or right
   * @param padByte     The pad byte's value
   *
   * @return The unpadded {@link HexString}
   */
  public final HexString unpad(final boolean left, final byte padByte) {

    if (left) {

      int idx = 0;
      while (idx < data.length && data[idx] == padByte) {
        idx++;
      }

      return new HexString(Arrays.copyOfRange(data, idx, data.length));

    } else {

      int idx = data.length - 1;
      while (idx >= 0 && data[idx] == padByte) {
        idx--;
      }

      return new HexString(Arrays.copyOfRange(data, 0, idx + 1));
    }
  }

  /**
   * XORs 'this' with the other hex string. Note: does not truncate the hex
   * string if leading bytes result in zeroes.
   *
   * @param other The other hex string to XOR onto 'this'
   *
   * @return 'this' XOR other
   *
   * @throws IllegalArgumentException On length mismatch
   */
  public final HexString xor(final HexString other)
      throws IllegalArgumentException {

    return new HexString(xor(data, other.data));
  }

  /**
   * ANDs 'this' with the other hex string. Note: does not truncate the hex
   * string if leading bytes result in zeroes.
   *
   * @param other The other hex string to AND onto 'this'
   *
   * @return 'this' AND other
   *
   * @throws IllegalArgumentException On length mismatch
   */
  public final HexString and(final HexString other)
      throws IllegalArgumentException {

    return new HexString(and(data, other.data));
  }

  /**
   * ORs 'this' with the other hex string.
   *
   * @param other The other hex string to OR onto 'this'
   *
   * @return 'this' OR other
   *
   * @throws IllegalArgumentException On length mismatch
   */
  public final HexString or(final HexString other)
      throws IllegalArgumentException {

    return new HexString(or(data, other.data));
  }

  /**
   * performs a left shift operation by 1 bit. Extends the HexString's length
   * if needed, but does *not* truncate it.
   *
   * @return this << 1
   */
  public final HexString shiftLeft() { return shiftLeft(1); }

  /**
   * performs a left shift operation by n bits. Extends the HexString's length
   * if needed.
   *
   * @param amount The amount of bits to shift
   *
   * @return this << amount
   *
   * @throws IllegalArgumentException On negative amounts
   */
  public final HexString shiftLeft(final int amount)
      throws IllegalArgumentException {

    return new HexString(leftShift(data, amount));
  }

  /**
   * performs a right shift operation by 1 bit. Does *not* truncate the result
   * (i.e. leading zeros remain).
   *
   * @return this >> 1
   */
  public final HexString shiftRight() { return shiftRight(1); }

  /**
   * performs a right shift operation by n bits. . Does *not* truncate the
   * result (i.e. leading zeros remain).
   *
   * @param amount The amount of bits to shift
   *
   * @return this >> amount
   *
   * @throws IllegalArgumentException On negative amounts
   */
  public final HexString shiftRight(final int amount)
      throws IllegalArgumentException {

    return new HexString(rightShift(data, amount));
  }

  /**
   * Removes leading zeroes.
   *
   * @return The same value with leading zeroes removed
   */
  public final HexString truncate() {

    int i = 0;
    while (i < data.length && data[i] == (byte)0x00) {
      i++;
    }

    if (i == 0) {
      return this;
    }

    if (i == data.length) {
      return HexString.EMPTY;
    }

    return new HexString(Arrays.copyOfRange(data, i, data.length));
  }

  /**
   * Modifies the hex string's length. If it is smaller than the current length,
   * the hex string is truncated to the given length. Removes higher order bytes
   * until length is reached, i.e.; 8040.setLength(1) == 40.
   * If it is greater than the current length, leading zeros are prepended.
   *
   * @param length the desired length
   *
   * @return The hexstring with new length
   *
   * @throws IllegalArgumentException on negative length
   */
  public final HexString setLength(final int length)
      throws IllegalArgumentException {

    if (length < 0) {
      throw new IllegalArgumentException("Negative length not allowed");
    }

    if (length == data.length) {
      return this;
    }

    if (length > data.length) {
      return padToBlockSize(PadMode.ZEROLEFT, length);
    }

    byte[] out = new byte[length];
    System.arraycopy(data, data.length - out.length, out, 0, out.length);
    return new HexString(out);
  }

  /**
   * Returns the reversed hex string (i.e.; 1234 becomes 4321). Reverses the
   * bit pattern.
   *
   * @return The reverse hex string
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final HexString reverse() {

    byte[] out = new byte[data.length];
    for (int i = 0; i < data.length; i++) {
      out[data.length - 1 - i] = (byte)(Integer.reverse(data[i]) >> 24);
    }

    return new HexString(out);
  }

  /**
   * Returns the reversed hex string (i.e.; 1234 becomes 3412). Reverses the
   * bytes.
   *
   * @return The reverse hex string
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final HexString reverseBytes() {

    byte[] out = new byte[data.length];
    for (int i = 0; i < data.length; i++) {
      out[i] = data[data.length - i - 1];
    }

    return new HexString(out);
  }

  /**
   * Removes a certain region from the hex string.
   *
   * @param startIndex from where to start removing bytes
   * @param endIndex   removal end index
   *
   * @return The hex string, with the given section removed
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public final HexString remove(final int startIndex, final int endIndex) {

    if (startIndex < 0 || endIndex < 0 || startIndex >= data.length ||
        endIndex > data.length || startIndex > endIndex) {
      throw new IllegalArgumentException("Invalid index");
    }

    if (startIndex == endIndex) {
      return this;
    }

    byte[] out = new byte[data.length - (endIndex - startIndex)];
    System.arraycopy(data, 0, out, 0, startIndex);
    System.arraycopy(data, endIndex, out, startIndex, data.length - endIndex);

    return new HexString(out);
  }

  /**
   * Returns whether the bit at the given index is set.
   *
   * @param index The index (bit offset, 0 = MSB, length() * 8 - 1 = LSB)
   *
   * @return true if set, false otherwise
   *
   * @throws IllegalArgumentException On an invalid index
   */
  @SuppressWarnings({"checkstyle:magicnumber"})
  public final boolean testBit(final int index)
      throws IllegalArgumentException {

    if (index < 0 || index >= data.length * Byte.SIZE) {
      throw new IllegalArgumentException("Invalid index");
    }

    int byteIdx = index >> 3;
    int bitIdx = 0x80 >> (index % Byte.SIZE);

    return ((data[byteIdx] & 0xFF) & bitIdx) > 0;
  }

  /**
   * Writes the hex string to the given output stream.
   * Does not insert line breaks. Does not close the OS.
   * Writes char-based.
   *
   * @param os     The output stream to write to
   *
   * @throws IOException On I/O errors
   */
  @Override
  public final void writeTo(final OutputStream os) throws IOException {

    writeTo(os, -1);
  }

  /**
   * Writes the hex string to the given filename.
   * Writes char-based.
   *
   * @param filename  The file to write to
   * @param lineWidth The line width (not bytes!) after which newlines shall be
   *                  inserted. If <= 0, no line breaks are inserted
   *
   * @throws IOException On I/O errors
   */
  public final void writeTo(final String filename, final int lineWidth)
      throws IOException {

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename);
      writeTo(fos, lineWidth);

    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  /**
   * Writes the hex string to the given filename. Does not insert line breaks.
   * Writes char-based.
   *
   * @param file The file to write to
   * @param lineWidth The line width (not bytes!) after which newlines shall be
   *                  inserted. If <= 0, no line breaks are inserted
   *
   * @throws IOException On I/O errors
   */
  public final void writeTo(final File file, final int lineWidth)
      throws IOException {

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      writeTo(fos, lineWidth);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  /**
   * Writes the hex string to the given {@link OutputStream}. Does *not*
   * close the output stream.
   * Writes char-based.
   *
   * @param os        The {@link OutputStream} to write to
   * @param lineWidth The line width (not bytes!) after which newlines shall be
   *                  inserted. If <= 0, no line breaks are inserted
   *
   * @throws IOException On I/O errors
   */
  public final void writeTo(final OutputStream os, final int lineWidth)
      throws IOException {

    PrintStream ps = new PrintStream(os);

    if (lineWidth <= 0) {

      ps.println(toString());
    } else {

      int lineWidthBytes = lineWidth >> 1;

      HexString tmp = this;
      while (!tmp.isEmpty() && tmp.length() >= lineWidthBytes) {
        ps.println(tmp.substring(0, lineWidthBytes));
        tmp = tmp.substring(lineWidthBytes);
      }

      ps.println(tmp);
    }
  }

  /**
   * Writes the hex string to the given filename. Does not insert line breaks.
   *
   * @param filename The file to write to
   * @param binary Whether to write as binary data or character-based
   *
   * @throws IOException On I/O errors
   */
  public final void writeTo(final String filename, final boolean binary)
      throws IOException {

    writeTo(new File(filename), binary);
  }

  /**
   * Writes the hex string to the given filename. Does not insert linebreaks.
   *
   * @param file The file to write to
   * @param binary Whether to write as binary data or character-based
   *
   * @throws IOException On I/O errors
   */
  public final void writeTo(final File file, final boolean binary)
      throws IOException {

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(file);
      writeTo(fos, binary);
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  /**
   * Writes the hex string to the given output stream.
   * Does not insert line breaks. Does not close the OS.
   *
   * @param os     The output stream to write to
   * @param binary Whether to write as binary data or character-based
   *
   * @throws IOException On I/O errors
   */
  public final void writeTo(final OutputStream os, final boolean binary)
      throws IOException {

    if (binary) {
      os.write(toByteArray());
      os.flush();
    } else {
      OutputStreamWriter osw = new OutputStreamWriter(os);
      osw.write(toString());
      osw.flush();
    }
  }

  /**
   * Writes to an output file using a default line length.
   *
   * @param file the file to write to
   * @throws IOException on I/O error
   */
  @Override
  public final void writeTo(final File file) throws IOException {
    writeTo(file, DEFAULT_LINE_LENGTH);
  }

  @Override
  public final void writeTo(final String fileName) throws IOException {
    writeTo(new File(fileName));
  }

  @Override
  public final boolean equals(final Object other) {

    if (other == this) {
      return true;
    }

    if (other == null) {
      return false;
    }

    if (other instanceof HexString) {
      return Arrays.equals(data, ((HexString)other).data);
    }

    if (other instanceof String) {
      return Arrays.equals(data, HexString.fromString((String)other).data);
    }

    if (other instanceof byte[]) {
      return Arrays.equals(data, (byte[])other);
    }

    if (other instanceof BigInteger) {
      return toBigInteger().equals(other);
    }

    if (other instanceof Integer) {
      return ((Integer)other).equals(toInteger());
    }

    if (other instanceof Long) {
      return ((Long)other).equals(toLong());
    }

    return false;
  }

  /**
   * Returns this HexString's data as Byte Array Input Stream
   * (hence, it does not need to be closed).
   *
   * @return The data as input stream
   */
  public InputStream getAsStream() { return new ByteArrayInputStream(data); }

  @Override
  public final int hashCode() {

    // NOTE: DO *note* use data.hashCode() here, since it is not based on array
    //       contents for byte[]. If you do, implementations using e.g. mappings
    //       with HexStrings as keys will *FAIL*
    return Arrays.hashCode(data);
  }

  @Override
  public final int compareTo(final HexString o) {

    if (o == null) {
      return 1; // always bigger than null.
    }

    int k = 0;
    while (k < Math.min(data.length, o.data.length)) {

      byte c1 = data[k];
      byte c2 = o.data[k];

      if (c1 != c2) {
        return c1 - c2;
      }

      k++;
    }
    return data.length - o.data.length;
  }

  @Override
  public final HexString clone() {
    byte[] copy = new byte[data.length];
    System.arraycopy(data, 0, copy, 0, copy.length);
    return new HexString(copy);
  }

  /**
   * Returns whether or not the given char c is a valid hex char.
   *
   * @param c The char to test
   *
   * @return true if valid hex string, false otherwise
   */
  protected static final boolean isHexChar(final char c) {
    return Character.digit(c, HEX_BASE) != -1;
  }

  /**
   * byte[] XOR.
   *
   * @param b1 byte[] 1
   * @param b2 byte[] 2
   *
   * @return b1 XOR b2
   *
   * @throws IllegalArgumentException If b1.length != b2.length
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static byte[] xor(final byte[] b1, final byte[] b2)
      throws IllegalArgumentException {

    if (b1.length != b2.length) {
      throw new IllegalArgumentException(
          "Byte arrays need to be of same length");
    }

    byte[] out = new byte[b1.length];
    for (int i = 0; i < b1.length; i++) {
      out[i] = (byte)((b1[i] & 0xFF) ^ (b2[i] & 0xFF));
    }

    return out;
  }

  /**
   * byte[] AND.
   *
   * @param b1 byte[] 1
   * @param b2 byte[] 2
   *
   * @return b1 AND b2
   *
   * @throws IllegalArgumentException If b1.length != b2.length
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static byte[] and(final byte[] b1, final byte[] b2)
      throws IllegalArgumentException {

    if (b1.length != b2.length) {
      throw new IllegalArgumentException(
          "Byte arrays need to be of same length");
    }

    byte[] out = new byte[b1.length];
    for (int i = 0; i < b1.length; i++) {
      out[i] = (byte)((b1[i] & 0xFF) & (b2[i] & 0xFF));
    }

    return out;
  }

  /**
   * byte[] OR.
   *
   * @param b1 byte[] 1
   * @param b2 byte[] 2
   *
   * @return b1 OR b2
   *
   * @throws IllegalArgumentException If b1.length != b2.length
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static byte[] or(final byte[] b1, final byte[] b2)
      throws IllegalArgumentException {

    if (b1.length != b2.length) {
      throw new IllegalArgumentException(
          "Byte arrays need to be of same length");
    }

    byte[] out = new byte[b1.length];
    for (int i = 0; i < b1.length; i++) {
      out[i] = (byte)((b1[i] & 0xFF) | (b2[i] & 0xFF));
    }

    return out;
  }

  /**
   * byte[] left shift.
   *
   * @param b      byte[] to shift
   * @param amount The amount of bits to shift
   *
   * @return b << 1
   *
   * @throws IllegalArgumentException On negative amount
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static byte[] leftShift(final byte[] b, final int amount)
      throws IllegalArgumentException {

    if (amount < 0) {
      throw new IllegalArgumentException(
          "Negative amount is not supported, use shiftRight()");
    }

    int fullByteShift = amount / Byte.SIZE;
    int withinByteShift = amount % Byte.SIZE;

    int headroom = 0;
    while (headroom < b.length && b[headroom] == (byte)0x00) {
      headroom++;
    }

    headroom -= fullByteShift;
    int newLen = b.length;

    if (headroom < 1 && withinByteShift > 0 &&
        ((b[headroom] & 0xff) > (0xFF >> (withinByteShift)))) {
      headroom--;
    }

    if (headroom < 0) {
      newLen -= headroom;
    }

    int idxOffset = Math.max(0, headroom);
    byte[] out = new byte[newLen];
    System.arraycopy(b, idxOffset, out,
                     newLen - b.length - fullByteShift + idxOffset,
                     b.length - idxOffset);
    if (withinByteShift > 0) {
      int i = 0;
      for (; i < out.length - 1; i++) {
        out[i] <<= withinByteShift;
        out[i] |= (out[i + 1] >> (Byte.SIZE - withinByteShift) &
                   (0xFF >> (Byte.SIZE - withinByteShift)));
      }
      out[i] <<= withinByteShift;
    }

    return out;
  }

  /**
   * byte[] right shift. does not truncate.
   *
   * @param b      byte[] to shift
   * @param amount The amount of bits to shift
   *
   * @return b >> amount
   *
   * @throws IllegalArgumentException On negative amount
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static byte[] rightShift(final byte[] b, final int amount)
      throws IllegalArgumentException {

    if (amount < 0) {
      throw new IllegalArgumentException(
          "Negative amount is not supported, use shiftRight()");
    }

    int fullByteShift = amount / Byte.SIZE;
    int withinByteShift = amount % Byte.SIZE;

    byte[] out = new byte[b.length];
    System.arraycopy(b, 0, out, fullByteShift, b.length - fullByteShift);
    if (withinByteShift > 0) {
      int i = out.length - 1;

      for (; i > 0; i--) {

        out[i] = (byte)(((out[i] & 0xFF) >> withinByteShift) & 0xFF);
        out[i] |= (((out[i - 1] & 0xFF) //>> withinByteShift
                    & (0xFF >> (Byte.SIZE - withinByteShift)))
                   << (Byte.SIZE - withinByteShift));
      }
      out[i] = (byte)((out[i] & 0xFF) >>> withinByteShift);
    }

    return out;
  }

  @Override
  public Iterator<Byte> iterator() {

    return new Iterator<Byte>() {
      private int i = 0;

      @Override
      public boolean hasNext() {
        return i < data.length;
      }

      @Override
      public Byte next() {
        return data[i++];
      }

      /**
       * This iterator does not implement the remove method and hence will
       * throw an UnsupportedOperationException.
       */
      @Override
      public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException(
            "HexString is mutable, remove not supported.");
      }
    };
  }

  @Override
  public final String getFileExtension() {
    return EXTENSION_HEX_STRING;
  }
}

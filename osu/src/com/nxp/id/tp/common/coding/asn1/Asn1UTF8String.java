package com.nxp.id.tp.common.coding.asn1;

import java.io.ByteArrayOutputStream;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 UTF-8 String.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1UTF8String extends Asn1String {

  /** UTF-8 STRING tag value, 0x0C. */
  static final int TAG_INT = 0x0C;

  /** UTF8-STRING tag value, 0x0C. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses a given UTF-8 String.
   *
   * @param utf8String The TLV to parse
   *
   * @return An {@link Asn1UTF8String} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1UTF8String parse(final HexString utf8String)
      throws InvalidAsn1CodingException {

    return new Asn1UTF8String(unwrap(TAG, utf8String)[0]);
  }

  /**
   * Ctor with ASCII string as input.
   *
   * @param string The string to set as value
   */
  public Asn1UTF8String(final String string) { super(TAG, toUtf8(string)); }

  /**
   * Ctor with raw, already encoded bytes as input.
   *
   * @param value The bytes to set as value
   */
  public Asn1UTF8String(final HexString value) { super(TAG, value); }

  @Override
  public String getString() {
    return fromUtf8(getValue());
  }

  /**
   * Internal conversion method.
   * Credits go to BC's Strings.fromUtf8ByteArray()
   *
   * @param str The bytes to convert
   *
   * @return The converted string
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static String fromUtf8(final HexString str) {
    int i = 0;
    int length = 0;
    byte[] bytes = str.toByteArray();

    while (i < bytes.length) {
      length++;
      if ((bytes[i] & 0xf0) == 0xf0) {
        // surrogate pair
        length++;
        i += 4;
      } else if ((bytes[i] & 0xe0) == 0xe0) {
        i += 3;
      } else if ((bytes[i] & 0xc0) == 0xc0) {
        i += 2;
      } else {
        i += 1;
      }
    }

    char[] cs = new char[length];

    i = 0;
    length = 0;

    while (i < bytes.length) {
      char ch;

      if ((bytes[i] & 0xf0) == 0xf0) {

        int codePoint = ((bytes[i] & 0x03) << 18) |
                        ((bytes[i + 1] & 0x3F) << 12) |
                        ((bytes[i + 2] & 0x3F) << 6) | (bytes[i + 3] & 0x3F);

        int u = codePoint - 0x10000;
        char w1 = (char)(0xD800 | (u >> 10));
        char w2 = (char)(0xDC00 | (u & 0x3FF));

        cs[length++] = w1;
        ch = w2;
        i += 4;
      } else if ((bytes[i] & 0xe0) == 0xe0) {
        ch = (char)(((bytes[i] & 0x0f) << 12) | ((bytes[i + 1] & 0x3f) << 6) |
                    (bytes[i + 2] & 0x3f));
        i += 3;
      } else if ((bytes[i] & 0xd0) == 0xd0) {
        ch = (char)(((bytes[i] & 0x1f) << 6) | (bytes[i + 1] & 0x3f));
        i += 2;
      } else if ((bytes[i] & 0xc0) == 0xc0) {
        ch = (char)(((bytes[i] & 0x1f) << 6) | (bytes[i + 1] & 0x3f));
        i += 2;
      } else {
        ch = (char)(bytes[i] & 0xff);
        i += 1;
      }

      cs[length++] = ch;
    }

    return new String(cs);
  }

  /**
   * Internal conversion method.
   * Credits go to BC's Strings.toUtf8ByteArray()
   *
   * @param str The bytes to convert
   *
   * @return The converted string
   */
  @SuppressWarnings({"checkstyle:magicnumber", "checkstyle:rightcurly"})
  private static HexString toUtf8(final String str) {

    char[] chars = str.toCharArray();
    int i = 0;

    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    while (i < chars.length) {
      char ch = chars[i];

      if (ch < 0x0080) {
        bos.write(ch);
      } else if (ch < 0x0800) {
        bos.write(0xc0 | (ch >> 6));
        bos.write(0x80 | (ch & 0x3f));
      }

      // surrogate pair
      else if (ch >= 0xD800 && ch <= 0xDFFF) {
        // in error - can only happen, if the Java String class has a
        // bug.
        if (i + 1 >= chars.length) {
          throw new InvalidAsn1CodingException("invalid UTF-16 codepoint");
        }
        char w1 = ch;
        ch = chars[++i];
        char w2 = ch;

        // in error - can only happen, if the Java String class has a
        // bug.
        if (w1 > 0xDBFF) {
          throw new InvalidAsn1CodingException("invalid UTF-16 codepoint");
        }

        int codePoint = (((w1 & 0x03FF) << 10) | (w2 & 0x03FF)) + 0x10000;
        bos.write(0xf0 | (codePoint >> 18));
        bos.write(0x80 | ((codePoint >> 12) & 0x3F));
        bos.write(0x80 | ((codePoint >> 6) & 0x3F));
        bos.write(0x80 | (codePoint & 0x3F));
      } else {
        bos.write(0xe0 | (ch >> 12));
        bos.write(0x80 | ((ch >> 6) & 0x3F));
        bos.write(0x80 | (ch & 0x3F));
      }

      i++;
    }

    return HexString.fromByteArray(bos.toByteArray());
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("UTF8 STRING (len=" + getValue().length() + ") [")
        .append(getString())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1UTF8String clone() {

    return new Asn1UTF8String(getValue());
  }
}

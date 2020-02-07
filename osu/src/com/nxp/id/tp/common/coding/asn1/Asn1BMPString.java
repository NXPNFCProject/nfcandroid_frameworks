package com.nxp.id.tp.common.coding.asn1;

import java.io.ByteArrayOutputStream;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 BMP (Basic Multilingual Plane) STRING.
 * ISO/IEC/ITU 10646-1, a two-octet (USC-2) encoding form, identical to
 * Unicode 1.1, see
 * <a href="
 *   http://javadoc.iaik.tugraz.at/iaik_jce/current/iaik/asn1/BMPString.html"
 * >IAIK Javadoc</a>.
 *
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1BMPString extends Asn1String {

  /** BMP STRING tag value, 0x03. */
  static final int TAG_INT = 0x1E;

  /** BMP tag value, 0x1E. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses a given BMP string.
   *
   * @param bmpString The TLV to parse
   *
   * @return An {@link Asn1BMPString} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1BMPString parse(final HexString bmpString)
      throws InvalidAsn1CodingException {

    return new Asn1BMPString(unwrap(TAG, bmpString)[0]);
  }

  /**
   * Ctor. Creates a BMP-coded STRING from the given ASCII String value.
   *
   * @param asciiString The value to set
   */
  public Asn1BMPString(final String asciiString) {
    super(TAG, toBmpString(asciiString));
  }

  /**
   * Ctor. Creates a BMP-coded STRING from the given, already BMP-coded value.
   *
   * @param value The value to set
   */
  public Asn1BMPString(final HexString value) { super(TAG, value); }

  @Override
  public String getString() {
    return toAsciiString(getValue());
  }

  /**
   * bytes-to-string conversion.
   * Credits go to BC's DERBMPString.ctor(byte[]).
   *
   * @param bmpString BMP-coded bytes
   *
   * @return converted string
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static String toAsciiString(final HexString bmpString) {

    byte[] bytes = bmpString.toByteArray();

    char[] cs = new char[bytes.length / 2];

    for (int i = 0; i != cs.length; i++) {
      cs[i] = (char)((bytes[2 * i] << 8) | (bytes[2 * i + 1] & 0xff));
    }

    return String.valueOf(cs);
  }

  /**
   * string-to-bytes conversion.
   * Credits go to BC's DERBMPString.encode().
   *
   * @param asciiString string
   *
   * @return BMP bytes
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static HexString toBmpString(final String asciiString) {

    ByteArrayOutputStream bos = new ByteArrayOutputStream();

    char[] chars = asciiString.toCharArray();
    for (int i = 0; i != chars.length; i++) {
      char c = chars[i];

      bos.write((byte)(c >> 8));
      bos.write((byte)c);
    }

    return HexString.fromByteArray(bos.toByteArray());
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("BMP STRING (len=" + getValue().length() + ") [")
        .append(getString())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1BMPString clone() {

    return new Asn1BMPString(getValue());
  }
}

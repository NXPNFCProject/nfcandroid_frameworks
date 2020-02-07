package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 GENERAL STRING.
 *
 * Technically, GeneralString encompasses all registered graphic and character
 * sets (see ISO 2375) plus SPACE and DELETE (see
 * <a href="
 * http://javadoc.iaik.tugraz.at/iaik_jce/current/iaik/asn1/GeneralString.html"
 * IAIK Javadoc</a>
 *
 * In this implementation, any string can be passed to
 * {@link Asn1GeneralString}.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1GeneralString extends Asn1String {

  /** GENERAL STRING tag value, 0x1B. */
  static final int TAG_INT = 0x1B;

  /** GENERAL STRING tag value, 0x1B. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param generalString The general string TLV
   *
   * @return An {@link Asn1GeneralString} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1GeneralString parse(final HexString generalString)
      throws InvalidAsn1CodingException {

    return new Asn1GeneralString(unwrap(TAG, generalString)[0]);
  }

  /**
   * Ctor with ASCII string.
   *
   * @param asciiString ASCII string to set as value
   */
  public Asn1GeneralString(final String asciiString) {
    super(TAG, HexString.fromAsciiString(asciiString));
  }

  /**
   * Ctor with bytes.
   *
   * @param value bytes to set as value
   */
  public Asn1GeneralString(final HexString value) { super(TAG, value); }

  @Override
  public String getString() {
    return getValue().toAsciiString();
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("GENERAL STRING (len=" + getValue().length() + ") [")
        .append(getString())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1GeneralString clone() {

    return new Asn1GeneralString(getValue());
  }
}

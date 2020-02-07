package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 International Alphabet 5 (IA5) string. IA5 = ASCII.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1IA5String extends Asn1String {

  /** IA5-STRING tag value, 0x16. */
  static final int TAG_INT = 0x16;

  /** IA5-STRING tag value, 0x16. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param ia5String The string TLV
   *
   * @return An {@link Asn1IA5String} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1IA5String parse(final HexString ia5String)
      throws InvalidAsn1CodingException {

    return new Asn1IA5String(unwrap(TAG, ia5String)[0]);
  }

  /**
   * Ctor with ASCII string as input.
   *
   * @param asciiString String to set as value
   */
  public Asn1IA5String(final String asciiString) {
    super(TAG, HexString.fromAsciiString(asciiString));
  }

  /**
   * Ctor with hex string as input.
   *
   * @param value Bytes to set as value
   */
  public Asn1IA5String(final HexString value) { super(TAG, value); }

  @Override
  public String getString() {
    return getValue().toAsciiString();
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("IA5 STRING (len=" + getValue().length() + ") [")
        .append(getString())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1IA5String clone() {

    return new Asn1IA5String(getValue());
  }
}

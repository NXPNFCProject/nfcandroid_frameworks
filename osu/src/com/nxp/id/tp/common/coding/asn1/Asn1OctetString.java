package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 OCTET STRING. Arbitrary byte sequence.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1OctetString extends Asn1String {

  /** OCTET STRING tag value, 0x04. */
  static final int TAG_INT = 0x04;

  /** OCTET STRING tag value, 0x04. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param octetString The string TLV
   *
   * @return An {@link Asn1OctetString} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1OctetString parse(final HexString octetString)
      throws InvalidAsn1CodingException {

    return new Asn1OctetString(unwrap(TAG, octetString)[0]);
  }

  /**
   * Ctor with Hex String value.
   *
   * @param theValue The value to set
   */
  public Asn1OctetString(final HexString theValue) { super(TAG, theValue); }

  @Override
  public String getString() {
    return getValue().toAsciiString();
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("OCTET STRING (len=" + getValue().length() + ") [")
        .append(getValue())
        .append("/")
        .append(getValue().toAsciiString())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1OctetString clone() {

    return new Asn1OctetString(getValue());
  }
}

package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 PRINTABLE STRING. Arbitrary byte sequence.
 *
 * Technically, it is restricted to
 * (A,...,Z; a,...,z; 0,...,9; space ' () + , - . / : = ?), but any string can
 * be passed to this implementation.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1PrintableString extends Asn1String {

  /** PRINTABLE-STRING tag value, 0x13. */
  static final int TAG_INT = 0x13;

  /** PRINTABLE-STRING tag value, 0x13. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param printableString The TLV to parse
   *
   * @return An {@link Asn1PrintableString} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1PrintableString parse(final HexString printableString)
      throws InvalidAsn1CodingException {

    return new Asn1PrintableString(unwrap(TAG, printableString)[0]);
  }

  /**
   * Ctor with ASCII string as input.
   *
   * @param asciiString The string to set as value
   */
  public Asn1PrintableString(final String asciiString) {
    this(HexString.fromAsciiString(asciiString));
  }

  /**
   * Ctor with raw bytes as input.
   *
   * @param value The bytes to set as value
   */
  public Asn1PrintableString(final HexString value) { super(TAG, value); }

  @Override
  public String getString() {
    return getValue().toAsciiString();
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("PRINTABLE STRING (len=" + getValue().length() + ") [")
        .append(getString())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1PrintableString clone() {

    return new Asn1PrintableString(getValue());
  }
}

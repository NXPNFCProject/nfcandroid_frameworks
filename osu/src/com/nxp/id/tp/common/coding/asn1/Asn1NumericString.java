package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 NUMERIC STRING. may contain only numbers (0,...,9) and spaces (' ').
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1NumericString extends Asn1String {

  /** NUMERIC-STRING tag value, 0x12. */
  static final int TAG_INT = 0x12;

  /** NUMERIC-STRING tag value, 0x12. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param numericString The string TLV
   *
   * @return An {@link Asn1NumericString} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1NumericString parse(final HexString numericString)
      throws InvalidAsn1CodingException {

    return new Asn1NumericString(unwrap(TAG, numericString)[0]);
  }

  /**
   * Ctor with hex string value, ASCII representation may only contain numbers
   * (0,...,9) and spaces (' ').
   *
   * @param theValue The value to set
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public Asn1NumericString(final HexString theValue)
      throws InvalidAsn1CodingException {
    super(TAG, theValue);

    for (char c : theValue.toAsciiString().toCharArray()) {
      if (!Character.isDigit(c) && (c != ' ')) {
        throw new InvalidAsn1CodingException(
            "Invalid Numeric String, non-numeric characters.");
      }
    }
  }

  /**
   * Ctor with string value, may only contain numbers (0,...,9)
   * and spaces (' ').
   *
   * @param theValue The value to set
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public Asn1NumericString(final String theValue) {
    this(HexString.fromAsciiString(theValue));
  }

  @Override
  public String getString() {
    return getValue().toAsciiString();
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("NUMERIC STRING (len=" + getValue().length() + ") [")
        .append(getValue())
        .append("/")
        .append(getValue().toAsciiString())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1NumericString clone() {

    return new Asn1NumericString(getValue());
  }
}

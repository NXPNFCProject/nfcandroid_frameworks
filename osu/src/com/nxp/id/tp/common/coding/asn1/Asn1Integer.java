package com.nxp.id.tp.common.coding.asn1;

import java.math.BigInteger;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 INTEGER.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1Integer extends Asn1Structure {

  /** INTEGER tag value, 0x02. */
  static final int TAG_INT = 0x02;

  /** INTEGER tag value, 0x02. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param integer The integer TLV
   *
   * @return An {@link Asn1Integer} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Integer parse(final HexString integer)
      throws InvalidAsn1CodingException {

    return new Asn1Integer(unwrap(TAG, integer)[0]);
  }

  /**
   * Ctor with raw hex string as value.
   *
   * @param theValue The value to set
   */
  public Asn1Integer(final HexString theValue) {
    super(TAG, theValue);

    if (value.testBit(0)) {
      value = value.prepend(HexString.zero(1));
    }
  }

  /**
   * Ctor with int as value.
   *
   * @param theValue The value to set
   */
  public Asn1Integer(final int theValue) {
    this(HexString.fromInteger(theValue));
  }

  /**
   * Ctor with long as value.
   *
   * @param theValue The value to set
   */
  public Asn1Integer(final long theValue) {
    this(HexString.fromLong(theValue));
  }

  /**
   * Ctor with {@link BigInteger} as value.
   *
   * @param theValue The value to set
   */
  public Asn1Integer(final BigInteger theValue) {
    this(HexString.fromBigInteger(theValue));
  }

  /**
   * Returns the value as integer. Throws a {@link NumberFormatException} if
   * the value is too long for an int.
   *
   * @return The value as int
   */
  public int getInteger() { return getValue().toInteger(); }

  /**
   * Returns the value as big integer.
   *
   * @return The value as big integer
   */
  public BigInteger getBigInteger() { return getValue().toBigInteger(); }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("INTEGER (len=" + getValue().length() + ") [")
        .append(getValue())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1Integer clone() {

    return new Asn1Integer(getValue());
  }
}

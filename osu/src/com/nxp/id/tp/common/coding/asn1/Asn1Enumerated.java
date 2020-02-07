package com.nxp.id.tp.common.coding.asn1;

import java.math.BigInteger;
import java.util.List;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;
import com.nxp.id.tp.common.Util;

/**
 * ASN.1 ENUMERATED.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1Enumerated extends Asn1Structure {

  /** ENUMERATED tag value, 0x0A. */
  static final int TAG_INT = 0x0A;

  /** ENUMERATED tag value, 0x0A. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param enumValue The enumerated TLV
   *
   * @return An {@link Asn1Enumerated} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Enumerated parse(final HexString enumValue)
      throws InvalidAsn1CodingException {

    return new Asn1Enumerated(unwrap(TAG, enumValue)[0]);
  }

  /**
   * Parses the given TLV.
   *
   * @param enumValue   The enumerated TLV
   * @param validValues Valid enumeration values to check against
   *
   * @return An {@link Asn1Enumerated} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Enumerated parse(final HexString enumValue,
                                     final HexString... validValues)
      throws InvalidAsn1CodingException {

    return parse(enumValue, Util.wrapInList(validValues));
  }

  /**
   * Parses the given TLV.
   *
   * @param enumValue   The enumerated TLV
   * @param validValues Valid enumeration values to check against
   *
   * @return An {@link Asn1Enumerated} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Enumerated parse(final HexString enumValue,
                                     final List<HexString> validValues)
      throws InvalidAsn1CodingException {

    Asn1Enumerated parsed = new Asn1Enumerated(unwrap(TAG, enumValue)[0]);

    if (!parsed.isValidValue(validValues)) {
      throw new InvalidAsn1CodingException(
          "Value '" + parsed.getValue() +
          "' does not match with respect to given enumeration: " + validValues);
    }

    return parsed;
  }

  /**
   * Ctor with raw hex string as value.
   *
   * @param theValue The value to set
   */
  public Asn1Enumerated(final HexString theValue) { super(TAG, theValue); }

  /**
   * Ctor with int as value.
   *
   * @param theValue The value to set
   */
  public Asn1Enumerated(final int theValue) {
    this(HexString.fromInteger(theValue));
  }

  /**
   * Ctor with long as value.
   *
   * @param theValue The value to set
   */
  public Asn1Enumerated(final long theValue) {
    this(HexString.fromLong(theValue));
  }

  /**
   * Ctor with {@link BigInteger} as value.
   *
   * @param theValue The value to set
   */
  public Asn1Enumerated(final BigInteger theValue) {
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

  /**
   * Returns whether or not the enumeration's value is valid with respect to
   * the given enumerated items.
   *
   * @param enumeration Valid reference values
   *
   * @return true if valid, false otherwise
   */
  public boolean isValidValue(final HexString... enumeration) {
    return isValidValue(Util.wrapInList(enumeration));
  }

  /**
   * Returns whether or not the enumeration's value is valid with respect to
   * the given enumerated items.
   *
   * @param enumeration Valid reference values
   *
   * @return true if valid, false otherwise
   */
  public boolean isValidValue(final List<HexString> enumeration) {
    return enumeration.contains(getValue());
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("ENUMERATED (len=" + getValue().length() + ") [")
        .append(getValue())
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1Enumerated clone() {

    return new Asn1Enumerated(getValue());
  }
}

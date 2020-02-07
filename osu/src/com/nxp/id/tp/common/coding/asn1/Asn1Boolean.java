package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 BOOLEAN.
 * DER (not BER) coded (0xff = true, 0x00 = false, else = invalid).
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1Boolean extends Asn1Structure {

  /** BOOLEAN tag value, 0x01. */
  static final int TAG_INT = 0x01;

  /** BOOLEAN tag value, 0x01. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /** ASN.1/DER boolean true. */
  private static final byte TRUE = (byte)0xFF;

  /** ASN.1/DER boolean false. */
  private static final byte FALSE = (byte)0x00;

  /**
   * Parses a given boolean TLV.
   *
   * @param bool The TLV to parse
   *
   * @return An {@link Asn1Boolean} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Boolean parse(final HexString bool)
      throws InvalidAsn1CodingException {

    byte val = unwrap(TAG, bool)[0].at(0);

    if (val == TRUE) {
      return new Asn1Boolean(true);
    } else if (val == FALSE) {
      return new Asn1Boolean(false);
    }

    // Note: DER only knows 0xFF, 0x00. BER is less restrictive,
    // allowing any value != 0 for true.
    // This implementation is DER-conformant though.
    throw new InvalidAsn1CodingException("Invalid boolean value");
  }

  /**
   * Ctor with boolean value.
   *
   * @param value The boolean
   */
  @SuppressWarnings("checkstyle:avoidinlineconditionals")
  public Asn1Boolean(final boolean value) {
    super(TAG, value ? HexString.fromByte(TRUE) : HexString.fromByte(FALSE));
  }

  /**
   * Ctor with hexstring value.
   *
   * @param value The boolean
   */
  @SuppressWarnings("checkstyle:avoidinlineconditionals")
  public Asn1Boolean(final HexString value) {
    super(TAG, value);

    if (!value.equals(HexString.fromByte(FALSE)) &&
        !value.equals(HexString.fromByte(TRUE))) {
      throw new InvalidAsn1CodingException("Invalid boolean value");
    }
  }

  /**
   * Returns the value as boolean.
   *
   * @return The value as boolean
   */
  public boolean isTrue() {
    return getValue().equals(HexString.fromByte(TRUE));
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer).append("BOOLEAN (len=" + getValue().length() + ") [");

    if ((byte)getValue().toInteger() == TRUE) {
      sb.append("TRUE");
    } else {
      sb.append("FALSE");
    }

    return sb.append("]").toString();
  }

  @Override
  public Asn1Boolean clone() {

    return new Asn1Boolean(isTrue());
  }
}

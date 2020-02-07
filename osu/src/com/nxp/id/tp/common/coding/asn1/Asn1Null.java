package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 NULL.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1Null extends Asn1Structure {

  /** NULL tag value, 0x05. */
  static final int TAG_INT = 0x05;

  /** NULL tag value, 0x05. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /** NULL instance. */
  public static final Asn1Null INSTANCE = new Asn1Null();

  /**
   * Parses the given TLV.
   *
   * @param nullInput The NULL TLV (actually TL)
   *
   * @return An {@link Asn1Null} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Null parse(final HexString nullInput)
      throws InvalidAsn1CodingException {

    if (!unwrap(TAG, nullInput)[0].isEmpty()) {
      throw new InvalidAsn1CodingException("Null with value");
    }

    return INSTANCE;
  }

  /**
   * Ctor. Private, use INSTANCE.
   */
  private Asn1Null() { super(TAG, HexString.EMPTY); }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer).append("NULL (len=" + getValue().length() + ")");

    return sb.toString();
  }

  @Override
  public Asn1Null clone() {

    return Asn1Null.INSTANCE;
  }
}

package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;

/**
 * ASN.1 String type superclass.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public abstract class Asn1String extends Asn1Structure {

  /**
   * Ctor with tag and value.
   *
   * @param theTag   tag value
   * @param theValue value
   */
  public Asn1String(final HexString theTag, final HexString theValue) {
    super(theTag, theValue);
  }

  /**
   * Getter for the string.
   *
   * @return The string
   */
  public abstract String getString();

  @Override public abstract Asn1Encodable clone();
}

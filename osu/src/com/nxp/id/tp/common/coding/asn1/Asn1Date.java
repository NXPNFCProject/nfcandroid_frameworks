package com.nxp.id.tp.common.coding.asn1;

import java.util.Date;

import com.nxp.id.tp.common.HexString;

/**
 * ASN.1 Date type superclass.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public abstract class Asn1Date extends Asn1Structure {

  /**
   * Ctor with tag and value.
   *
   * @param theTag   tag value
   * @param theValue value
   */
  public Asn1Date(final HexString theTag, final HexString theValue) {
    super(theTag, theValue);
  }

  /**
   * Getter for the date/time.
   *
   * @return The date
   */
  public abstract Date getDateTime();

  @Override public abstract Asn1Date clone();
}

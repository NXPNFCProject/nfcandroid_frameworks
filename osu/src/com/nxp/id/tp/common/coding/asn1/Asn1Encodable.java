package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;

/**
 * Interface for structure that are ASN.1 encodable.
 *
 * Currently restricted to DER coding.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public interface Asn1Encodable extends Cloneable {

  /**
   * Returns the DER-coded ASN.1 structure.
   *
   * @return the DER coded bytes
   */
  HexString encode();

  /**
   * Returns the value (aka. the 'V' in TLV), which in turn may be
   * another complex value or sequence of TLVs.
   *
   * @return The value
   */
  HexString getValue();

  /**
   * Getter for the tag.
   *
   * @return The tag value
   */
  HexString getTag();

  /**
   * Pretty-prints the structure into a string.
   *
   * @param indent whitespaces to prepend to each line in the string, for
   *        pretty-printing nesting
   *
   * @return The pretty-printed string
   */
  String toString(final int indent);

  /**
   * Copies asn Asn1 encodable.
   *
   * @return The copy
   */
  Asn1Encodable clone();
}

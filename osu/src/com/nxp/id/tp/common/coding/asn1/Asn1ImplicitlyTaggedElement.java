package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;
/**
 * Custom tagged element.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1ImplicitlyTaggedElement extends Asn1Structure {

  /**
   * Parses the given TLV.
   *
   * @param tlv         The TLV to parse
   *
   * @return An {@link Asn1ImplicitlyTaggedElement} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1ImplicitlyTaggedElement parse(final HexString tlv)
      throws InvalidAsn1CodingException {

    HexString tag = getTag(tlv);

    if ((tag.at(0) & ASN1_TAG_CONTEXT) == 0 &&
        (tag.at(0) & ASN1_TAG_APPLICATION_SPECIFIC) == 0) {
      throw new InvalidAsn1CodingException("Not a context-specific tag");
    }

    return new Asn1ImplicitlyTaggedElement(tag, unwrap(tag, tlv)[0]);
  }

  /**
   * Ctor with tag and value.
   *
   * @param tag   The element's tag
   * @param value The element's value
   */
  public Asn1ImplicitlyTaggedElement(final HexString tag,
                                     final HexString value) {
    super(tag, value);
  }

  /**
   * Ctor with tag and value.
   *
   * @param tag   The element's tag number (without CONTEXT-SPECIFIC class id)
   * @param value The element's value
   */
  public Asn1ImplicitlyTaggedElement(final int tag, final HexString value) {
    super(HexString.fromInteger(tag | ASN1_TAG_CONTEXT), value);
  }

  /**
   * Ctor with tag and value.
   *
   * @param tag   The element's tag number (without CONTEXT-SPECIFIC class id)
   * @param value The element's value (to replace the tag of)
   */
  public Asn1ImplicitlyTaggedElement(final int tag, final Asn1Structure value) {
    super(HexString.fromInteger(tag | ASN1_TAG_CONTEXT), value.value);
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("TAGGED ELEMENT (tag=")
        .append(getTag())
        .append(";len=" + getValue().length() + ") [")
        .append(getValue())
        .append("]");

    return sb.toString();
  }

  /**
   * Getter for the tag number (the number in the brackets, e.g. 0 for [0]).
   *
   * @return The tag number
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public int getTagNo() {
    return tag.toInteger() & 0xF;
  }

  @Override
  public Asn1ImplicitlyTaggedElement clone() {

    return new Asn1ImplicitlyTaggedElement(getTag(), getValue());
  }
}

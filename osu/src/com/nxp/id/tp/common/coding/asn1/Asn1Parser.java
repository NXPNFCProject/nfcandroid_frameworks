package com.nxp.id.tp.common.coding.asn1;

import static com.nxp.id.tp.common.coding.asn1.Asn1Structure.ASN1_TAG_APPLICATION_SPECIFIC;
import static com.nxp.id.tp.common.coding.asn1.Asn1Structure.ASN1_TAG_CONSTRUCTED;
import static com.nxp.id.tp.common.coding.asn1.Asn1Structure.ASN1_TAG_CONTEXT;

import com.nxp.id.tp.common.HexString;

/**
 * Very rudimentary ASN.1 parser.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1Parser {

  /**
   * Parses the given TLV.
   *
   * @param data The TLV to parse
   *
   * @return An {@link Asn1Structure} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Encodable parse(final HexString data)
      throws InvalidAsn1CodingException {

    if (data == null) {
      throw new InvalidAsn1CodingException("Input may not be null");
    }

    HexString input = data;
    HexString tag = Asn1Structure.getTag(data);
    int container = ASN1_TAG_CONTEXT | ASN1_TAG_CONSTRUCTED;
    int appContainer = ASN1_TAG_APPLICATION_SPECIFIC | ASN1_TAG_CONSTRUCTED;
    int privContainer =
        ASN1_TAG_CONTEXT | ASN1_TAG_APPLICATION_SPECIFIC | ASN1_TAG_CONSTRUCTED;
    int header = tag.at(0) & privContainer;

    if (header == container || header == appContainer ||
        header == privContainer) {
      return Asn1TaggedContainer.parse(data);
    } else if ((tag.at(0) & ASN1_TAG_CONTEXT) != 0 ||
               (tag.at(0) & ASN1_TAG_APPLICATION_SPECIFIC) != 0) {
      return Asn1ImplicitlyTaggedElement.parse(data);
    }

    switch (tag.toInteger()) {

    case Asn1Boolean.TAG_INT:
      return Asn1Boolean.parse(input);
    case Asn1Integer.TAG_INT:
      return Asn1Integer.parse(input);
    case Asn1BitString.TAG_INT:
      return Asn1BitString.parse(input);
    case Asn1OctetString.TAG_INT:
      return Asn1OctetString.parse(input);
    case Asn1Null.TAG_INT:
      return Asn1Null.parse(input);
    case Asn1Enumerated.TAG_INT:
      return Asn1Enumerated.parse(input);
    case Asn1Oid.TAG_INT:
      return Asn1Oid.parse(input);
    // object descriptor, external, real, embedded pdv are not
    // implemented.
    case Asn1UTF8String.TAG_INT:
      return Asn1UTF8String.parse(input);
    // relative OID is not implemented
    case Asn1Sequence.TAG_INT:
      return Asn1Sequence.parse(input);
    case Asn1Set.TAG_INT:
      return Asn1Set.parse(input);
    case Asn1NumericString.TAG_INT:
      return Asn1NumericString.parse(input);
    case Asn1BMPString.TAG_INT:
      return Asn1BMPString.parse(input);
    case Asn1GeneralizedTime.TAG_INT:
      return Asn1GeneralizedTime.parse(input);
    case Asn1UTCTime.TAG_INT:
      return Asn1UTCTime.parse(input);
    case Asn1GeneralString.TAG_INT:
      return Asn1GeneralString.parse(input);
    case Asn1IA5String.TAG_INT:
      return Asn1IA5String.parse(input);
    case Asn1PrintableString.TAG_INT:
      return Asn1PrintableString.parse(input);
    default:
      throw new InvalidAsn1CodingException("Unknown tag '" + tag +
                                           "' or unimplemented type");
    }
  }

  /**
   * Parses the given TLV.
   *
   * @param data The TLV to parse
   *
   * @return An {@link Asn1Structure} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Encodable parse(final byte[] data) {
    return parse(HexString.fromByteArray(data));
  }

  /**
   * Private Ctor. Do not instantiate.
   */
  private Asn1Parser() {}
}

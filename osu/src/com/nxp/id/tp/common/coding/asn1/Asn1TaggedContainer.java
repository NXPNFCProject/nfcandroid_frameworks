package com.nxp.id.tp.common.coding.asn1;

import java.util.ArrayList;
import java.util.List;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;
import com.nxp.id.tp.common.Util;

/**
 * Custom tagged container.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public class Asn1TaggedContainer extends Asn1Container {

  /**
   * Parses the given TLV.
   *
   * @param tlv The TLV to parse
   *
   * @return An {@link Asn1TaggedContainer} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1TaggedContainer parse(final HexString tlv)
      throws InvalidAsn1CodingException {

    HexString tag = getTag(tlv);
    HexString[] cursor = unwrap(tlv);

    if (!cursor[1].isEmpty()) {
      throw new InvalidAsn1CodingException(
          "Unexpected Data at the end of the sequence");
    }

    List<Asn1Encodable> members = new ArrayList<Asn1Encodable>();
    cursor[1] = cursor[0];
    while (!cursor[1].isEmpty()) {
      cursor = removeTlv(cursor[1]);
      members.add(Asn1Parser.parse(cursor[0]));
    }

    return new Asn1TaggedContainer(tag, members);
  }

  /**
   * Parses the given TLV.
   *
   * @param expectedTag The tag the container is expected to have
   * @param tlv         The TLV to parse
   *
   * @return An {@link Asn1TaggedContainer} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1TaggedContainer parse(final HexString expectedTag,
                                          final HexString tlv)
      throws InvalidAsn1CodingException {

    HexString[] cursor = unwrap(expectedTag, tlv);

    if (!cursor[1].isEmpty()) {
      throw new InvalidAsn1CodingException(
          "Unexpected Data at the end of the sequence");
    }

    List<Asn1Encodable> members = new ArrayList<Asn1Encodable>();
    cursor[1] = cursor[0];
    while (!cursor[1].isEmpty()) {
      cursor = removeTlv(cursor[1]);
      members.add(Asn1Parser.parse(cursor[0]));
    }

    return new Asn1TaggedContainer(expectedTag, members);
  }

  /**
   * Ctor with tag and list of members it shall contain.
   *
   * @param tag     Container tag
   * @param members Contained children
   */
  public Asn1TaggedContainer(final HexString tag,
                             final Asn1Encodable... members) {

    super(tag, Util.wrapInList(members));
  }

  /**
   * Ctor with tag and list of members it shall contain.
   *
   * @param tag     Container tag
   * @param members Contained children
   */
  public Asn1TaggedContainer(final HexString tag,
                             final List<Asn1Encodable> members) {

    super(tag, members);
  }

  /**
   * Ctor to create an empty container.
   *
   * @param tag The container's tag
   */
  public Asn1TaggedContainer(final HexString tag) { super(tag); }

  /**
   * Ctor with tag and list of members it shall contain.
   *
   * @param tag     Container tag
   * @param members Contained children
   */
  public Asn1TaggedContainer(final int tag, final Asn1Encodable... members) {

    this(HexString.fromInteger(tag | ASN1_TAG_CONSTRUCTED | ASN1_TAG_CONTEXT),
         Util.wrapInList(members));
  }

  /**
   * Ctor with tag and list of members it shall contain.
   *
   * @param tag     Container tag
   * @param members Contained children
   */
  public Asn1TaggedContainer(final int tag, final List<Asn1Encodable> members) {

    this(HexString.fromInteger(tag | ASN1_TAG_CONSTRUCTED | ASN1_TAG_CONTEXT),
         members);
  }

  /**
   * Ctor to create an empty container.
   *
   * @param tag The container's tag
   */
  public Asn1TaggedContainer(final int tag) {
    this(HexString.fromInteger(tag | ASN1_TAG_CONSTRUCTED | ASN1_TAG_CONTEXT));
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
  public String toString(final int indent) {

    String spacer = StringUtil.repeat(' ', indent);

    StringBuilder sb = new StringBuilder();
    sb.append(spacer)
        .append("TAGGED CONTAINER (tag=")
        .append(getTag())
        .append("; len=" + getValue().length() + ") [")
        .append(Util.NEWLINE);

    for (Asn1Encodable struct : members) {
      sb.append(struct.toString(indent + Asn1Structure.INDENT))
          .append(Util.NEWLINE);
    }

    sb.append(spacer).append("]");
    return sb.toString();
  }

  @Override
  public Asn1TaggedContainer clone() {

    List<Asn1Encodable> clonedMembers =
        new ArrayList<Asn1Encodable>(members.size());

    for (Asn1Encodable encodable : members) {
      clonedMembers.add(encodable.clone());
    }

    return new Asn1TaggedContainer(getTag(), clonedMembers);
  }
}

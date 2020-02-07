package com.nxp.id.tp.common.coding.asn1;

import java.util.ArrayList;
import java.util.List;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;
import com.nxp.id.tp.common.Util;

/**
 * ASN.1 SEQUENCE container.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public class Asn1Sequence extends Asn1TaggedContainer {

  /** SEQUENCE tag value, 0x30. */
  @SuppressWarnings("checkstyle:magicnumber")
  static final int TAG_INT = 0x10 | ASN1_TAG_CONSTRUCTED;

  /** NUMERIC-STRING tag value, 0x30. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param sequence The TLV to parse
   *
   * @return An {@link Asn1Sequence} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Sequence parse(final HexString sequence)
      throws InvalidAsn1CodingException {

    HexString[] cursor = unwrap(TAG, sequence);

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

    return new Asn1Sequence(members);
  }

  /**
   * Ctor with list of contained members.
   *
   * @param sequenceMembers Its members
   */
  public Asn1Sequence(final Asn1Encodable... sequenceMembers) {

    super(TAG, sequenceMembers);
  }

  /**
   * Ctor with list of contained members.
   *
   * @param sequenceMembers Its members
   */
  public Asn1Sequence(final List<Asn1Encodable> sequenceMembers) {

    super(TAG, sequenceMembers);
  }

  /**
   * Ctor to create an empty sequence.
   */
  public Asn1Sequence() { super(TAG); }

  @Override
  public String toString(final int indent) {

    String spacer = StringUtil.repeat(' ', indent);

    StringBuilder sb = new StringBuilder();
    sb.append(spacer)
        .append("SEQUENCE (len=" + getValue().length() + ") [")
        .append(Util.NEWLINE);

    for (Asn1Encodable struct : members) {
      sb.append(struct.toString(indent + Asn1Structure.INDENT))
          .append(Util.NEWLINE);
    }

    sb.append(spacer).append("]");
    return sb.toString();
  }

  @Override
  public Asn1Sequence clone() {

    List<Asn1Encodable> clonedMembers =
        new ArrayList<Asn1Encodable>(members.size());

    for (Asn1Encodable encodable : members) {
      clonedMembers.add(encodable.clone());
    }

    return new Asn1Sequence(clonedMembers);
  }
}

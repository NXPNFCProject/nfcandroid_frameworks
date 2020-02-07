package com.nxp.id.tp.common.coding.asn1;

import java.util.ArrayList;
import java.util.List;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;
import com.nxp.id.tp.common.Util;

/**
 * ASN.1 SET container.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public class Asn1Set extends Asn1TaggedContainer {

  /** SET tag value, 0x31. */
  @SuppressWarnings("checkstyle:magicnumber")
  static final int TAG_INT = 0x11 | ASN1_TAG_CONSTRUCTED;

  /** SET tag value, 0x31. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /**
   * Parses the given TLV.
   *
   * @param set The TLV to parse
   *
   * @return An {@link Asn1Set} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Set parse(final HexString set)
      throws InvalidAsn1CodingException {

    HexString[] cursor = unwrap(TAG, set);

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

    return new Asn1Set(members);
  }

  /**
   * Ctor with list of contained members.
   *
   * @param setMembers Its members
   */
  public Asn1Set(final Asn1Encodable... setMembers) {

    this(Util.wrapInList(setMembers));
  }

  /**
   * Ctor with list of contained members.
   *
   * @param setMembers Its members
   */
  public Asn1Set(final List<Asn1Encodable> setMembers) {

    super(TAG, setMembers);
  }

  /**
   * Ctor to create an empty set.
   */
  public Asn1Set() { super(TAG); }

  @Override
  public String toString(final int indent) {

    String spacer = StringUtil.repeat(' ', indent);

    StringBuilder sb = new StringBuilder();
    sb.append(spacer)
        .append("SET (len=" + getValue().length() + ") [")
        .append(Util.NEWLINE);

    for (Asn1Encodable struct : members) {
      sb.append(struct.toString(indent + Asn1Structure.INDENT))
          .append(Util.NEWLINE);
    }

    sb.append(spacer).append("]");
    return sb.toString();
  }

  @Override
  public Asn1Set clone() {

    List<Asn1Encodable> clonedMembers =
        new ArrayList<Asn1Encodable>(members.size());

    for (Asn1Encodable encodable : members) {
      clonedMembers.add(encodable.clone());
    }

    return new Asn1Set(clonedMembers);
  }
}

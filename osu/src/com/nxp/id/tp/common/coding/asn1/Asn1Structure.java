package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;

/**
 * Abstract ASN.1 element superclass.
 *
 * Currently, the ASN.1 syntax encoding is limited to DER.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public abstract class Asn1Structure implements Asn1Encodable {

  /** ASN.1 constructed coding, bit 6 is set. */
  static final int ASN1_TAG_CONSTRUCTED = 0x20;

  /** ASN.1 application specific tag. */
  static final int ASN1_TAG_APPLICATION_SPECIFIC = 0x40;

  /** ASN.1 context specific tag. */
  static final int ASN1_TAG_CONTEXT = 0x80;

  /** indentation for nesting in pretty-printing. */
  static final int INDENT = 2;

  /** tag value (1-2 bytes). */
  @SuppressWarnings("checkstyle:visibilitymodifier") protected HexString tag;

  /** element value. */
  @SuppressWarnings("checkstyle:visibilitymodifier") protected HexString value;

  /**
   * Ctor with tag and value, length is computed.
   *
   * @param theTag   tag value
   * @param theValue value
   */
  public Asn1Structure(final HexString theTag, final HexString theValue) {
    tag = theTag;
    value = theValue;
  }

  /**
   * Getter for the payload length.
   *
   * @return The length
   */
  public int getPayloadLength() { return value.length(); }

  /**
   * Getter for the width (in bytes) of the length field (the 'L' in TLV).
   *
   * @return The width
   */
  public int getLengthWidth() { return getLengthLength(value.length()); }

  /**
   * Getter for the length of the whole TLV structure.
   *
   * @return The TLV's length
   */
  public int getTlvLength() {
    return tag.length() + getLengthLength(value.length()) + value.length();
  }

  @Override
  public HexString getTag() {
    return tag;
  }

  /**
   * Getter for the tag's width in bytes (1 or 2).
   *
   * @return The tag's width
   */
  public int getTagWidth() { return tag.length(); }

  @Override
  public HexString getValue() {
    return value;
  }

  @Override
  public HexString encode() {

    return tag.append(getDerLength(value.length())).append(value);
  }

  /**
   * Returns whether or not the element is tagged as application-specific.
   *
   * @return true if so, false otherwise
   */
  public boolean isApplicationSpecific() {
    return (tag.toInteger() & ASN1_TAG_APPLICATION_SPECIFIC) != 0;
  }

  /**
   * Returns whether or not the element is constructed.
   *
   * @return true if so, false otherwise
   */
  public boolean isConstructed() {
    return (tag.toInteger() & ASN1_TAG_CONSTRUCTED) != 0;
  }

  @Override
  public String toString() {
    return toString(0);
  }

  @Override
  public boolean equals(final Object other) {

    if (other == null || !(other instanceof Asn1Structure)) {
      return false;
    }
    Asn1Structure object = (Asn1Structure)other;
    return tag.equals(object.tag) && value.length() == object.value.length() &&
        value.equals(object.value);
  }

  @Override
  public int hashCode() {
    return tag.hashCode() ^ value.length() ^ value.hashCode();
  }

  /**
   * Returns the value of the tagged element in 'data'
   * (needs to be first bytes in 'data'), and the remainder of the hex string
   * after removing the tag, its length and its contents.
   *
   * @param data The data containing the whole hex string
   *
   * @return The value of the specified tag [0] and the remainder [1]
   *
   * @throws InvalidAsn1CodingException If the 'data' does not start with
   *                                    'tag' or the coding is otherwise
   *                                    invalid
   */
  @SuppressWarnings("checkstyle:magicnumber")
  static HexString[] unwrap(final HexString data)
      throws InvalidAsn1CodingException {

    try {

      int lenOff = getTag(data).length();
      int lenLen = 1;
      int len = data.substring(lenOff, lenOff + lenLen).toInteger();

      if (len >= 0x80) {
        lenLen += (len & 0x0F);
        len = data.substring(lenOff + 1, lenOff + lenLen).toInteger();
      }

      return new HexString[] {
          data.substring(lenOff + lenLen, lenOff + lenLen + len),
          data.substring(lenOff + lenLen + len)};

    } catch (StringIndexOutOfBoundsException e) {
      throw new InvalidAsn1CodingException("Invalid BER coding. Data: " + data);
    }
  }

  /**
   * Same as {@link #unwrap(HexString)}, but with a given expected tag.
   *
   * @param tag  The tag to remove from the beginning of the hex string
   * @param data The data containing the whole hex string
   *
   * @return The value of the specified tag [0] and the remainder [1]
   * @throws InvalidAsn1CodingException If the 'data' does not start with
   *                                    'tag' or the coding is otherwise
   *                                    invalid
   */
  @SuppressWarnings("checkstyle:magicnumber")
  static HexString[] unwrap(final HexString tag, final HexString data)
      throws InvalidAsn1CodingException {

    if (!data.startsWith(tag)) {
      throw new InvalidAsn1CodingException(
          "Input did not start with expected tag " + tag.toString() +
          ". Data: " + data);
    }

    try {

      int lenOff = tag.length();
      int lenLen = 1;
      int len = data.substring(lenOff, lenOff + lenLen).toInteger();

      if (len >= 0x80) {
        lenLen += (len & 0x0F);
        len = data.substring(lenOff + 1, lenOff + lenLen).toInteger();
      }

      if (lenOff + lenLen == data.length()) {
        return new HexString[] {HexString.EMPTY, HexString.EMPTY};
      }

      return new HexString[] {
          data.substring(lenOff + lenLen, lenOff + lenLen + len),
          data.substring(lenOff + lenLen + len)};

    } catch (StringIndexOutOfBoundsException e) {
      throw new InvalidAsn1CodingException("Invalid BER coding. Data: " + data);
    }
  }

  /**
   * Returns the TLV triplet of the first TLV in the sequence and the
   * (possibly empty) remainder of the hex string, after removing the TLV
   * triplet.
   *
   * @param data The data containing the whole hex string
   *
   * @return The first TLV triplet [0] and the remainder [1]
   * @throws InvalidAsn1CodingException On invalid coding
   */
  @SuppressWarnings("checkstyle:magicnumber")
  static HexString[] removeTlv(final HexString data)
      throws InvalidAsn1CodingException {

    try {

      int lenOff = getTag(data).length();
      int lenLen = 1;
      int len = data.substring(lenOff, lenOff + lenLen).toInteger();

      if (len >= 0x80) {
        lenLen += (len & 0x0F);
        len = data.substring(lenOff + 1, lenOff + lenLen).toInteger();
      }

      return new HexString[] {data.substring(0, lenOff + lenLen + len),
                              data.substring(lenOff + lenLen + len)};

    } catch (StringIndexOutOfBoundsException e) {
      throw new InvalidAsn1CodingException("Invalid BER coding. Data: " + data);
    }
  }

  /**
   * Given an integer length, this method returns the BER representation of
   * the length, as string. That is, if the length does not fit into one
   * byte (*without the MSB* being set), then there is a first byte prepended,
   * which is 0x80 | %number of subsequent bytes to represent the length%.
   *
   * @param len the integer length
   *
   * @return The BER length representation
   */
  @SuppressWarnings("checkstyle:magicnumber")
  static HexString getDerLength(final int len) {

    HexString length = HexString.fromInteger(len);
    if (length.length() > 1 || ((length.at(0) & 0xFF) > 0x7F)) {
      length = HexString.fromInteger(0x80 + length.length()).append(length);
    }

    return length;
  }

  /**
   * Returns the width of the length field ('L' in TLV), in bytes.
   *
   * @param len The non-ASN.1 coded length
   *
   * @return The byte width of this length value, after encoding
   */
  @SuppressWarnings("checkstyle:magicnumber")
  static int getLengthLength(final int len) {

    if (len >= 0x80) {
      return 1 + (len & 0x0F);
    }

    return 1;
  }

  /**
   * Returns the tag value, to be expected as the first or first n bytes in
   * 'data'.
   *
   * @param data Input data (TLV triplet, sequence of TLV triplets, ...)
   *
   * @return The tag
   */
  @SuppressWarnings("checkstyle:magicnumber")
  static HexString getTag(final HexString data) {
    if ((data.at(0) & 0x1F) != 0x1F) {
      return data.substring(0, 1);
    }

    int end = 1;
    while (((data.at(end) & 0x80) == 0x80) && end < data.length()) {
      end++;
    }
    return data.substring(0, end + 1);
  }

  @Override public abstract Asn1Encodable clone();
}

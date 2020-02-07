package com.nxp.id.tp.common.coding.asn1;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 BIT STRING.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1BitString extends Asn1Structure {

  /** BIT STRING tag value, 0x03. */
  static final int TAG_INT = 0x03;

  /** BIT STRING tag value, 0x03. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /** number of unused bits at the end of the bit string. */
  private HexString unused = HexString.zero(1);

  /**
   * Parses a given bit string.
   *
   * @param bitString The TLV to parse
   *
   * @return An {@link Asn1BitString} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1BitString parse(final HexString bitString)
      throws InvalidAsn1CodingException {

    try {
      return new Asn1BitString(unwrap(TAG, bitString)[0], true);
    } catch (IllegalArgumentException e) {
      throw new InvalidAsn1CodingException(e);
    }
  }

  /**
   * Ctor. Creates a BIT STRING from the given value,
   * with 0 unused bits at the end.
   *
   * @param theValue The value to set (excluding the unused bits byte)
   */
  public Asn1BitString(final HexString theValue) { this(theValue, (byte)0); }

  /**
   * Ctor. Creates a BIT STRING from the given value,
   * with 'unusedBits' unused bits at the end.
   *
   * @param theValue   The value to set (excluding the unused bits byte)
   * @param unusedBits Number of unused bits at the end
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public Asn1BitString(final HexString theValue, final byte unusedBits) {
    super(TAG, theValue);

    if (theValue.isEmpty() && unusedBits != 0) {
      throw new IllegalArgumentException("Empty data, non-empty pad bits!");
    }

    if ((unusedBits > 7) || (unusedBits < 0)) {
      throw new IllegalArgumentException("Pad bits invalid");
    }
    unused = HexString.fromByte(unusedBits);
  }

  /**
   * Internal ctor.
   *
   * @param value       The ASN.1 bit string value
   *                    (including the unused bits byte)
   * @param checkUnused Whether or not to read the unused bits from value
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private Asn1BitString(final HexString value, final boolean checkUnused) {
    super(TAG, value.substring(1));

    if (checkUnused) {
      unused = value.substring(0, 1);

      if (value.substring(1).isEmpty() && unused.toInteger() != 0) {
        throw new IllegalArgumentException("Empty data, non-empty pad bits!");
      }

      if ((unused.toInteger() > 7) || (unused.toInteger() < 0)) {
        throw new IllegalArgumentException("Pad bits invalid");
      }
    }
  }

  /**
   * Ctor. Creates a BIT STRING from the given bit string value.
   * Does left-aligning to octets and computes unused bits properly.
   *
   * @param theValue   The value to set (excluding the unused bits byte)
   */
  public Asn1BitString(final String theValue) {
    super(TAG, getCodedBitString(theValue));

    unused = HexString.fromInteger(Byte.SIZE - (theValue.length() % Byte.SIZE));
  }

  /**
   * Encodes the bit string value as per DER encoding ruleset.
   * That is, creates a byte array containing the bit string in a left aligned
   * manner.
   *
   * @param theValue bit string as {@link String}
   *
   * @return Bit string as {@link HexString}, not yet containing the first
   *         octet indicating the number of unused bits at the end
   */
  private static HexString getCodedBitString(final String theValue) {

    if (theValue == null) {
      throw new IllegalArgumentException("Input may not be null");
    }

    String tmp = theValue;

    if (tmp.startsWith("0b")) {
      tmp = tmp.substring(2);
    }

    tmp = tmp.replace(" ", "");

    int len = (tmp.length() / Byte.SIZE);

    if ((tmp.length() % Byte.SIZE) > 0) {
      len++;
    }

    byte[] out = new byte[len];
    for (int i = 0, k = -1; i < tmp.length() && k < tmp.length(); i++) {

      char c = tmp.charAt(i);

      if ((c != '0') && (c != '1')) {
        throw new NumberFormatException(
            tmp + " is not a valid bit string, found '" + c + "'");
      }

      if ((i % Byte.SIZE) == 0) {
        k++;
      }

      if (c > '0') {
        out[k] |= (byte)(1 << (Byte.SIZE - (i % Byte.SIZE) - 1));
      }
    }

    return HexString.fromByteArray(out);
  }

  @Override
  public HexString encode() {
    return getTag()
        .append(getDerLength(getPayloadLength() + unused.length()))
        .append(unused)
        .append(getValue());
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("BIT STRING (len=" + getValue().length() + ") [")
        .append(getValue())
        .append(" (")
        .append(unused.toInteger())
        .append(" padbits)")
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1BitString clone() {

    Asn1BitString copy = new Asn1BitString(getValue());
    copy.unused = unused;
    return copy;
  }
}

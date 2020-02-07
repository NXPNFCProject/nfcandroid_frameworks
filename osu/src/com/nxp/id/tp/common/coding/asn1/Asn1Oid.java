package com.nxp.id.tp.common.coding.asn1;

import java.math.BigInteger;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 OBJECT IDENTIFIER.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1Oid extends Asn1Structure {

  /** OID tag value, 0x06. */
  static final int TAG_INT = 0x06;

  /** OID tag value, 0x06. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /** long limit. */
  @SuppressWarnings("checkstyle:magicnumber")
  private static final long LONG_LIMIT = (Long.MAX_VALUE >> 7) - 0x7f;

  /** OID string representation. */
  private String idString;

  /**
   * Parses the given TLV.
   *
   * @param oid The oid TLV
   *
   * @return An {@link Asn1OctetString} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1Oid parse(final HexString oid)
      throws InvalidAsn1CodingException {

    return new Asn1Oid(unwrap(TAG, oid)[0]);
  }

  /**
   * Ctor with coded OID as argument.
   *
   * @param value The ASN.1-coded OID value
   */
  public Asn1Oid(final HexString value) {
    super(TAG, value);

    idString = oidToString(value);
  }

  /**
   * Ctor with OID string representation (dotted components) as argument.
   *
   * @param value The OID string value
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public Asn1Oid(final String value) throws InvalidAsn1CodingException {
    super(TAG, stringToOid(value));

    idString = value;
  }

  /**
   * Getter for the OID String.
   *
   * @return The OID as string
   */
  public String getOid() { return idString; }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("OID (len=" + getValue().length() + ") [")
        .append(getValue())
        .append("/")
        .append(idString)
        .append("]");

    return sb.toString();
  }

  /**
   * Internal string to OID conversion.
   * cf. https://msdn.microsoft.com/en-us/library/bb540809%28v=vs.85%29.aspx
   *
   * @param value The string value to encode
   *
   * @return The encoded bytes
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static HexString stringToOid(final String value)
      throws InvalidAsn1CodingException {

    HexString out = HexString.EMPTY;
    String[] tokens = value.split("\\.");

    if (tokens.length < 2) {
      throw new InvalidAsn1CodingException("Invalid OID");
    }

    out = HexString.fromInteger(Integer.parseInt(tokens[0]) * 40 +
                                Integer.parseInt(tokens[1]));

    for (int i = 2; i < tokens.length; i++) {

      int val = Integer.parseInt(tokens[i]);

      if (val > 127) {
        HexString token = HexString.fromInteger(val);

        byte[] tmp = new byte[(token.bitLength() + 6) / 7];
        for (int k = tmp.length - 1; k >= 0; k--) {
          tmp[k] = (byte)((token.toInteger() & 0x7F) | 0x80);
          token = token.shiftRight(7);
        }

        tmp[tmp.length - 1] &= 0x7f;
        out = out.append(tmp);
      } else {
        out = out.append(HexString.fromInteger(val));
      }
    }

    return out;
  }

  /**
   * Internal OID to string conversion.
   * cf. https://msdn.microsoft.com/en-us/library/bb540809%28v=vs.85%29.aspx
   *
   * @param value The bytes to decode
   *
   * @return The decoded string representation
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  @SuppressWarnings("checkstyle:magicnumber")
  private static String oidToString(final HexString value) {

    StringBuilder id = new StringBuilder();
    long val = 0;
    BigInteger bigValue = null;
    boolean first = true;

    for (int i = 0; i < value.length(); i++) {
      int b = value.at(i) & 0xff;

      if (val <= LONG_LIMIT) {

        val += (b & 0x7f);
        if ((b & 0x80) == 0) {

          if (first) {
            if (val < 40) {
              id.append('0');
            } else if (val < 80) {
              id.append('1');
              val -= 40;
            } else {
              id.append('2');
              val -= 80;
            }
            first = false;
          }

          id.append('.').append(val);
          val = 0;
        } else {
          val <<= 7;
        }
      } else {

        if (bigValue == null) {
          bigValue = BigInteger.valueOf(val);
        }
        bigValue = bigValue.or(BigInteger.valueOf(b & 0x7f));
        if ((b & 0x80) == 0) {
          if (first) {
            id.append('2');
            bigValue = bigValue.subtract(BigInteger.valueOf(80));
            first = false;
          }

          id.append('.').append(bigValue);
          bigValue = null;
          val = 0;
        } else {
          bigValue = bigValue.shiftLeft(7);
        }
      }
    }

    return id.toString();
  }

  @Override
  public Asn1Oid clone() {

    return new Asn1Oid(getValue());
  }
}

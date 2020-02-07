package com.nxp.id.tp.common.coding.asn1;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;

import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.StringUtil;

/**
 * ASN.1 UTC TIME.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class Asn1UTCTime extends Asn1Date {

  /** UTC TIME tag value, 0x17. */
  static final int TAG_INT = 0x17;

  /** UTC TIME tag value, 0x17. */
  public static final HexString TAG = HexString.fromInteger(TAG_INT);

  /** Date format. */
  private static final String DATE_FMT = "yyMMddHHmmss'Z'";

  /** Decoded date. */
  private Date date;

  /**
   * Parses a given UTC TIME string.
   *
   * @param genTime The TLV to parse
   *
   * @return An {@link Asn1UTCTime} instance
   *
   * @throws InvalidAsn1CodingException On invalid input
   */
  public static Asn1UTCTime parse(final HexString genTime)
      throws InvalidAsn1CodingException {

    return new Asn1UTCTime(unwrap(TAG, genTime)[0]);
  }

  /**
   * Ctor with given, coded date time of format '{@value #DATE_FMT}'.
   *
   * @param dateTime The date/time
   *
   * @throws InvalidAsn1CodingException On an invalid coded input
   */
  public Asn1UTCTime(final HexString dateTime)
      throws InvalidAsn1CodingException {
    super(TAG, dateTime);

    try {
      date = new SimpleDateFormat(DATE_FMT).parse(dateTime.toAsciiString());
    } catch (ParseException e) {
      throw new InvalidAsn1CodingException("Date parse error", e);
    }
  }

  /**
   * Ctor with date object.
   *
   * @param dateTime The date/time to encode
   */
  public Asn1UTCTime(final Date dateTime) {

    this(dateTime, Locale.getDefault());
  }

  /**
   * Ctor with date object and explicit locale.
   *
   * @param dateTime The date/time to encode
   * @param locale   The locale to apply
   */
  public Asn1UTCTime(final Date dateTime, final Locale locale) {
    super(TAG, convertDateTime(dateTime, locale));

    date = dateTime;
  }

  /**
   * Internal date-to-bytes conversion.
   *
   * @param date   The date to convert
   * @param locale The locale to apply
   *
   * @return The bytes representation
   */
  private static HexString convertDateTime(final Date date,
                                           final Locale locale) {
    DateFormat fmt = new SimpleDateFormat(DATE_FMT, locale);
    fmt.setTimeZone(new SimpleTimeZone(0, "Z"));

    return HexString.fromAsciiString(fmt.format(date));
  }

  @Override
  public Date getDateTime() {
    return date;
  }

  @Override
  public String toString(final int indent) {
    StringBuilder sb = new StringBuilder();

    String spacer = StringUtil.repeat(' ', indent);

    sb.append(spacer)
        .append("UTC TIME (len=" + getValue().length() + ") [")
        .append(new SimpleDateFormat(DATE_FMT).format(date))
        .append("]");

    return sb.toString();
  }

  @Override
  public Asn1UTCTime clone() {

    return new Asn1UTCTime(getValue());
  }
}

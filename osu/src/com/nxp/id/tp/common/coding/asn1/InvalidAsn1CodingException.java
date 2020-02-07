package com.nxp.id.tp.common.coding.asn1;

/**
 * Exception to be thrown whenever some ASN.1-coded data cannot be parsed.
 *
 * @author nxp72467 <klaus.potzmader@nxp.com>
 */
public final class InvalidAsn1CodingException extends RuntimeException {

  /** serialization UID. */
  private static final long serialVersionUID = -1329119267973349474L;

  /**
   * Ctor with msg.
   *
   * @param msg Message
   */
  public InvalidAsn1CodingException(final String msg) { super(msg); }

  /**
   * Ctor with msg and cause.
   *
   * @param msg   Message
   * @param cause Original cause
   */
  public InvalidAsn1CodingException(final String msg, final Throwable cause) {
    super(msg, cause);
  }

  /**
   * Ctor with cause.
   *
   * @param cause Original cause
   */
  public InvalidAsn1CodingException(final Throwable cause) { super(cause); }
}

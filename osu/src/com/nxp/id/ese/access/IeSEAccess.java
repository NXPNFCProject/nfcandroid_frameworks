
package com.nxp.id.ese.access;

/**
 * Interface defining the API to access the eSE.
 *
 */
public interface IeSEAccess {

  /** OS update mode status. */
  public static final int OSU_MODE_ENABLED = 1;
  public static final int OSU_MODE_DISABLED = 0;

  /** Error to be returned in case OS update mode failed. */
  public static final int SE_ACCESS_OSU_MODE_CHANGE_FAILED = -1;

  /** Operation status */
  public static final int SE_ACCESS_OPERATION_SUCCESS = 0;

  /**
   * Reset interface.
   */
  public void reset();

  /**
   * Open interface, in case dedicated operation on upper layer is needed.
   */
  public void open();

  /**
   * Close interface, in case dedicated operation on upper layer is needed.
   */
  public void close();

  /**
   * Send and receive method interface.
   *
   * @param data
   *            Data to be transmitted.
   * @return Response of the eSE
   */
  public byte[] send(byte[] data);

  /**
   * Access mode for the OSU to potentially disable/enable other interfaces or
   * to serve other dedicated operation.
   *
   * @param mode
   *            {@link #OSU_MODE_ENABLED} or {@link #OSU_MODE_DISABLED}
   * @return Status of operation
   */
  public int accessControlForCOSU(int mode);
}

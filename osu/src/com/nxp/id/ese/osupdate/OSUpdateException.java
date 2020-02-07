package com.nxp.id.ese.osupdate;

public class OSUpdateException extends Exception {

  public static final int INVALID_FILE_IMAGE = 0;
  public static final int INVALID_STATE = 1;
  public static final int INVALID_FILE_STRUCTURE = 2;
  public static final int INVALID_FINAL_REFERENCE = 3;

  private int errorCode;

  public OSUpdateException() {}

  public OSUpdateException(int errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public OSUpdateException(String message) { super(message); }

  public OSUpdateException(String message, Throwable cause) {
    super(message, cause);
  }

  public OSUpdateException(Throwable cause) { super(cause); }

  public int getErrorCode() { return errorCode; }
}

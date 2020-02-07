package com.nxp.id.jsbl.util;

public class JSBLException extends Exception {
  private static final long serialVersionUID = 1L;

  public JSBLException() {}

  public JSBLException(String str) { super(str); }

  public JSBLException(String str, Object... args) {
    super(String.format(str, args));
  }

  public JSBLException(String str, Throwable e) { super(str, e); }

  public JSBLException(Throwable cause) { super(cause); }
}
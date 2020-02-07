package com.nxp.id.tp.common;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * File format abstract class.
 *
 * @author nxp56369
 */
public abstract interface FileFormat {

  /** file extension to use for CSRs. */
  String EXTENSION_CSR = "csr";

  /** file extension to use for certificates (B64 encoded). */
  String EXTENSION_CERTIFICATE = "crt";

  /** file extension to use for certificates (B64 encoded). */
  String EXTENSION_CERT_GP = "gp";

  /** file extension to use for public keys. */
  String EXTENSION_PUBLIC_KEY = "pub";

  /** file extension to use for V1 Secrets. */
  String EXTENSION_SECRET_V1 = "enc";

  /** file extension to use for V2 Secrets. */
  String EXTENSION_SECRET_V2 = "sf2";

  /** file extension to use for TXT files. */
  String EXTENSION_TXT = "txt";

  /** file extension to use for JC Shell files. */
  String EXTENSION_JCSH = "jcsh";

  /** file extension to use for CSRs. */
  String EXTENSION_CONFIG = "config";

  /** Secret migration configuration. */
  String EXTENSION_SM_CONFIG = "smconfig";

  /** Secret migration ignore file. */
  String EXTENSION_SM_IGNORE = "smignore";

  /** File extension for cap files. */
  String EXTENSION_CAP = "cap";

  /** file extension to use for XML files. */
  String EXTENSION_XML = "xml";

  /** file extension to use for CRLs. */
  String EXTENSION_CRL = "crl";

  /** File extension for whitelist files. */
  String EXTENSION_WHITELIST = "txt";

  /** File extension for backup files. */
  String EXTENSION_BACKUP = "backup";

  /** file extension to use for (ASCII) hex data. */
  @Deprecated String EXTENSION_HEX_STRING = "hex";

  /**
   * file extension to use for cryptograms not adhering to any
   * standard format.
   */
  @Deprecated String EXTENSION_CRYPTOGRAM = "enc";

  /** File extension for mbk encrypted keys. */
  @Deprecated String EXTENSION_MBK = "key";

  /** The default length of a line in write to. */
  int DEFAULT_LINE_LENGTH = -1;

  /**
   * This method writes the content to a file.
   *
   * @param fileName the filename to write to
   *
   * @throws IOException On I/O error
   */
  void writeTo(final String fileName) throws IOException;

  /**
   * This method writes the content to a file.
   *
   * @param file the file to write to
   *
   * @throws IOException On I/O error
   */
  void writeTo(final File file) throws IOException;

  /**
   * This method writes the content to a given output stream.
   *
   * @param os the output stream to write to
   *
   * @throws IOException On I/O error
   */
  void writeTo(final OutputStream os) throws IOException;

  /**
   * This method returns the appropriated file extension of an File Format.
   *
   * @return the extension of the file
   */
  String getFileExtension();
}

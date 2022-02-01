/*
 * Copyright 2022 NXP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nxp.sems;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class SemsFileOperation {
  public static final String TAG = "SEMS-SemsFileOperation";

  private String mRespOutlog;
  private String mEncryptedScriptDirectory = "";
  private String mOutDirectory = "";
  public String mCallerPackageName;

  private static final byte SEMS_RESPONSE = 0x01;
  //private static final byte SEResponse = 0x02;
  private static final byte ERROR_RESPONSE = 0x03;
  //private static final byte SWResponse = 0x04;
  private static final byte SEMS_CERT_RESPONSE = 0x05;
  private static final byte SEMS_AUTH_RESPONSE = 0x06;
  private static final byte SEMS_RESPONSE_LOG_TAG = 0x61;
  private static final byte SEMS_FRAME_TYPE_TAG = 0x44;  // (CERT/AUTH/SECURE_CMD frame)
  private static final byte SEMS_RESPONSE_DATA_TAG = 0x43;

  /**
   * SemsFileOperation Constructor
   * <br/>
   * @param void
   *
   * @return void.
   */
  SemsFileOperation() {
    // Update start time stamp at beginning
    mRespOutlog = getCurrentTimeStamp();
  }

  /**
   * Get current date and time stamp
   * <br/>
   * Used in response log for debug purpose
   * @param void
   *
   * @return String current data and time stamp.
   */
  private String getCurrentTimeStamp() {
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();
    return "#######" + dtf.format(now) + "#######"
        + "\r\n";
  }

  /**
   * Logging the response APDU received during SEMS execution
   * <br/>
   * Agent to provide the SEMS Application with an identifier
   * @param byte[] what The Input status bytes
   *        Byte type The Input type of response
   *
   * @return void.
   */
  public void putIntoLog(byte[] what, byte type) {
    byte[] data;

    /*Skip SW "6310" as this is not relevant for debug */
    if ((what[what.length - 2] == 0x63) && (what[what.length - 1] == 0x10)) {
      return;
    }

    switch (type) {
      case ERROR_RESPONSE:
        return;
      default:
        return;
      case SEMS_CERT_RESPONSE:
        data = new byte[] {0x7F, 0x21};
        break;
      case SEMS_AUTH_RESPONSE:
        data = new byte[] {0x60};
        break;
      case SEMS_RESPONSE:
        data = new byte[] {0x40};
        break;
    }

    data = SemsTLV.make(SEMS_RESPONSE_LOG_TAG,
        SemsUtil.append(SemsTLV.make(SEMS_RESPONSE_DATA_TAG, data),
        SemsTLV.make(SEMS_RESPONSE_DATA_TAG, what)));
    mRespOutlog = mRespOutlog + SemsUtil.toHexString(data) + "\r\n";
  }

  /**
   * Set the current application directory & caller information
   * <br/>
   * @param context caller application context info
   *
   * @return {@code SemsStatus} returns SEMS_STATUS_SUCCESS on success
   * otherwise SEMS_STATUS_FAILED.
   */
  public SemsStatus setDirectories(Context context) {
    SemsStatus status = SemsStatus.SEMS_STATUS_FAILED;
    PackageInfo pInfo;
    PackageManager pm = context.getPackageManager();
    String str = context.getPackageName();
    synchronized (SemsFileOperation.class) {
      try {
        pInfo = pm.getPackageInfo(str, 0);
        str = pInfo.applicationInfo.dataDir;
        mCallerPackageName = pInfo.packageName;
        mEncryptedScriptDirectory = str;
        mOutDirectory = str;
        status = SemsStatus.SEMS_STATUS_SUCCESS;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return status;
  }

  /**
   * Get path to locate and access file
   * <br/>
   * @param String dir initial part of path string
   *        String file filename in the path
   *
   * @return Path The resulting path.
   */
  public Path getPath(String dir, String file) {
    return (dir != null) ? FileSystems.getDefault().getPath(dir, file)
                         : FileSystems.getDefault().getPath(file);
  }

  /**
   * Write the content of accumulated response buffer to out file
   * <br/>
   * Agent to provide the SEMS Application with an identifier
   * @param String scriptOut The file name to write response
   *
   * @return byte[] Response byte array.
   */
  public byte[] writeScriptOutFile(String scriptOut) {
    Path p = getPath(mOutDirectory, scriptOut);
    // Update finish time stamp at end
    mRespOutlog = mRespOutlog + getCurrentTimeStamp();
    try {
      Files.write(p, mRespOutlog.getBytes());
    } catch (IOException e) {
      Log.e(TAG, "IOException during writeScriptOutfile: ");
    }
    return mRespOutlog.getBytes();
  }

  /**
   * Write the content of String buffer to backup file
   * <br/>
   * @param String filename to which the buffer contents to be copied
   *        String scriptBuffer input buffer content
   *
   * @return byte[] response buffer.
   */
  public byte[] writeScriptInputFile(String filename, String scriptBuffer) {
    Path p = getPath(mOutDirectory, filename);
    try {
      Files.write(p, scriptBuffer.getBytes());
    } catch (IOException e) {
      Log.e(TAG, "IOException during writeScriptInputfile: ");
    }
    return scriptBuffer.getBytes();
  }

  /**
   * Read the file content to String format
   * <br/>
   * The Input path of the SEMS encrypted script stored,
   * @param String scriptIn input SEMS script with metadata
   *
   * @return String plain script with metadata removed.
   */

  public String readScriptFile(String scriptIn) throws Exception {
    Path p = getPath(mEncryptedScriptDirectory, scriptIn);
    String script = "";
    Iterator<String> i;
    try {
      List<String> lines = Files.readAllLines(p, Charset.defaultCharset());
      i = lines.iterator();
      while (i.hasNext()) {
        String s = i.next();
        if (!s.startsWith("%%%")) {
          script += s;
        }
      }
    } catch (IOException e) {
      Log.e(TAG, "IOException during reading script: ");
      throw new Exception();
    }
    return script;
  }

  /**
   * Get response log buffer string
   * <br/>
   * @param void
   *
   * @return String response log buffer.
   */
  public String getRespOutLog() {
    return mRespOutlog;
  }
}


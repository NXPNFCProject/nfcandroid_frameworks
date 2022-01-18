/*
 * Copyright 2021-2022 NXP
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

import android.util.Log;
import com.nxp.sems.SemsTLV;
import com.nxp.sems.SemsUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SemsAppletIdentifier {
  public static final String TAG = "SemsAppletIdentifier";
  private static SemsTLV tlv5D, tlv5E;

  /*Track line number in script Apdu.*/
  private static int lineCounter;
  private static boolean isTAG73Supported;
  private static int tag73Len;
  private static int commandNumber;
  private static int delayInMillsec;

  /*Strong Box Applet Identifier ASCII value*/
  private static final byte[] SEMS_SB_APP_ID = SemsUtil.parseHexString("5374726F6E67426F78");

  /**
   * Validate TAG and subtag support.
   * And, set flag as true if pre-condition meet.
   * <br/>
   * Sems Agent shall introduce delay on Nth Command.
   * @param TLV certifacte in ScriptTLV.
   *
   * @return void.
   */
  protected static void validateTag73Support(SemsTLV tlvCertInScript) throws Exception {
    Log.d(TAG, "***Initalize the variable to default values..**");
    lineCounter = 0;
    isTAG73Supported = false;
    tlv5D = null;
    tlv5E = null;
    tag73Len = 0;
    SemsTLV tlv5C, tlv73;
    List<SemsTLV> tlvs = SemsTLV.parse(tlvCertInScript.getValue());
    if (tlvs == null) {
      Log.d(TAG, "tlvCertInScript null");
      return;
    }
    tlv73 = SemsTLV.find(tlvs, 0x73);
    if (tlv73 == null) {
      Log.d(TAG, "tag73 is null");
      return;
    }
    // Parse outer T-L-V TAG73 for inner tags 5C, 5D and 5E
    tlvs = SemsTLV.parse(tlv73.getValue());
    tlv5C = SemsTLV.find(tlvs, 0x5C);
    if (tlv5C == null) {
      Log.d(TAG, "tlv5C is null");
      return;
    }
    if (!Arrays.equals(tlv5C.getValue(), SEMS_SB_APP_ID)) {
      Log.d(TAG, "Is not SB Applet");
      return;
    }
    tlv5D = SemsTLV.find(tlvs, 0x5D);
    if (tlv5D == null) {
      Log.d(TAG, "tlv5D is null");
      return;
    }
    commandNumber = arrayToValue(tlv5D.getValue());
    tlv5E = SemsTLV.find(tlvs, 0x5E);
    if (tlv5E == null) {
      Log.d(TAG, "tlv5E is null");
      return;
    }
    delayInMillsec = arrayToValue(tlv5E.getValue());
    if (delayInMillsec != 0x00) {
      Log.d(TAG,
          "***TAG 73 and sub-tag 5C,5D " + commandNumber + " 5E " + delayInMillsec
              + " are supported.**");
      isTAG73Supported = true;
      return;
    } else {
      Log.d(TAG, "Invalid delayInMillsec");
    }
  }

  /**
   * No of command in the script before sending it to SEMS applet,
   * introduce delay (Delay in msec).
   * <br/>
   *
   * @return void.
   */
  protected static void delayNthCommand() {
    try {
      if (isTAG73Supported) {
        lineCounter++;
        if (lineCounter == commandNumber) {
          Log.d(TAG, " Add Delay " + delayInMillsec);
          TimeUnit.MILLISECONDS.sleep(delayInMillsec);
        }
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  /**
   * Get TAG 73 Length(Inclusive of TAG and Length field).
   * <br/>
   *
   * @return int: TAG73 Length.
   */
  protected static int getTag73Len() {
    /*Include TAG & LENGTH field 2 bytes*/
    return (isTAG73Supported ? (tag73Len + 2) : 0);
  }

  /**
   * Calculate integer from byte array.
   * <br/>
   *
   * @return int: summed up array value
   */
  protected static int arrayToValue(byte[] arr) {
    int temp = 0;
    int len = arr.length;
    /*Length cannot be more than 4 bytes*/
    if (len > 4)
      return 0;

    if (len == 1)
      temp = (arr[0] & 0xFF);
    else if (len == 2)
      temp = (arr[0] << 8) & 0xFF00 | (arr[1] & 0xFF);
    else if (len == 3)
      temp = (arr[0] << 16) & 0xFF0000 | (arr[1] << 8) & 0xFF00 | (arr[2] & 0xFF);
    else if (len == 4)
      temp = (arr[0] << 24) | (arr[1] << 16) & 0xFF0000 | (arr[2] << 8) & 0xFF00 | (arr[3] & 0xFF);

    return temp;
  }
}

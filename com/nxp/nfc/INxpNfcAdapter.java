/*
 * Copyright 2024 NXP
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

package com.nxp.nfc;

import android.app.Activity;
import com.nxp.nfc.NxpNfcAdapter.NxpReaderCallback;
import java.io.IOException;

/**
 * @class INxpNfcAdapter
 * @brief Interface to perform the NFC Extension functionality.
 *
 * @hide
 */
public interface INxpNfcAdapter {

  /**
   * This is the first API to be called to start or stop the mPOS mode
   * <li>This api shall be called only Nfcservice is enabled.
   * <li>This api shall be called only when there are no NFC transactions
   * ongoing
   * </ul>
   * @param  pkg package name of the caller
   * @param  on Sets/Resets the mPOS state.
   * @return whether the update of state is
   *          MPOS_STATUS_SUCCESS,
   *          MPOS_STATUS_FAILED,
   * @throws IOException If a failure occurred during reader mode set or reset
   */
  public int mPOSSetReaderMode(String pkg, boolean on) throws IOException;

  /**
   * This is provides the info whether mPOS mode is activated or not
   * <li>This api shall be called only Nfcservice is enabled.
   * <li>This api shall be called only when there are no NFC transactions
   * ongoing
   * </ul>
   * @param  pkg package name of the caller
   * @return TRUE if reader mode is started
   *         FALSE if reader mode is not started
   * @throws IOException If a failure occurred during reader mode set or reset
   */
  public boolean mPOSGetReaderMode(String pkg) throws IOException;

  /**
   * This is the API to be called to enable or disable QTag RF mode.
   * <li>This api shall be called only when Nfcservice is enabled.
   * <li>This api shall be called only when there are no NFC transactions
   * ongoing.
   * <li>Limit the NFC controller to reader mode while this Activity is in the
   * foreground.
   * </ul>
   * @param  activity activity the Activity that requests the adapter to be in
   *     reader mode.
   * @param mQTagCallback the callback to be called when a tag is discovered
   * @param  mode to ENABLE_QTAG_ONLY_MODE.
   *                 APPEND_QTAG_MODE with input pollTech.
   *                 DISABLE_QTAG_MODE & reset to default discovery.
   * @param pollTech to append QPoll in reader mode.
   * @param delay_value PRESENCE_CHECK_DELAY
   * @return whether the update of state is
   *          QTag_STATUS_SUCCESS,
   *          QTag_STATUS_FAILED,
   * @throws IOException If a failure occurred during QTag RF mode set or reset
   */
  public int enableQTag(Activity activity, NxpReaderCallback mQTagCallback,
                        int mode, int pollTech, int delay_value)
      throws IOException;
}

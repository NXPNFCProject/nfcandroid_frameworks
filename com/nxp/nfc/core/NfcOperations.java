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

package com.nxp.nfc.core;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.ControllerAlwaysOnListener;
import android.nfc.NfcOemExtension;
import android.nfc.Tag;

import com.nxp.nfc.NxpNfcConstants;
import com.nxp.nfc.NxpNfcLogger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
 /**
 * @class NfcOperations
 * @brief A wrapper class for Nfc functionality.
 *
 * This interface provides methods for enable, disable discovery etc...
 * @hide
 */
public class NfcOperations {
    private static final String TAG = "NfcOperations";

    private static NfcOperations sNfcOperations = null;

    private Activity mActivity;
    private NfcAdapter mNfcAdapter;
    private NfcOemExtension mNfcOemExtension;

    /**
     * @brief wait latch for enable/disable discovery
     */
    private CountDownLatch mDisCountDownLatch;
    /**
     * @brief wait latch for {@link #setControllerAlwaysOn(boolean)}
     */
    private CountDownLatch mControllerAlwaysOnLatch;

    /**
     * @brief holds the value of
     * {@link NfcOemExtension.Callback#onRfDiscoveryStarted()}
     */
    private boolean mIsDiscoveryStarted = false;
    /**
     * @brief holds the value of
     * {@link NfcOemExtension.Callback#onRfFieldActivated()}
     */
    private boolean mIsRfFieldActivated = false;
    /**
     * @brief holds the value of
     * {@link NfcOemExtension.Callback#onCardEmulationActivated()}
     */
    private boolean mIsCardEmulationActivated = false;

    /**
     * @brief private constructor to create singleton object
     * @param nfcAdapter
     * @param activity
     */
    private NfcOperations(NfcAdapter nfcAdapter, Activity activity) {
        this.mNfcAdapter = nfcAdapter;
        this.mActivity = activity;
        mNfcOemExtension = mNfcAdapter.getNfcOemExtension();
        mNfcAdapter.registerControllerAlwaysOnListener(Executors.newSingleThreadExecutor(),
                            mControllerAlwaysOnListener);
        mNfcOemExtension.registerCallback(Executors.newSingleThreadExecutor(),
                            mOemExtensionCallback);
    }

    /**
     * @brief public function to get the instance of
     * {@link #NfcOperations(NfcAdapter, Activity)}
     * @param nfcAdapter
     * @param activity
     * @return instance of {@link #NfcOperations(NfcAdapter, Activity)}
     */
    public static NfcOperations getInstance(NfcAdapter nfcAdapter, Activity activity) {
        if (sNfcOperations == null) {
            sNfcOperations = new NfcOperations(nfcAdapter, activity);
        }
        return sNfcOperations;
    }

    /**
     * @brief enables ControllerAlwaysOn and waits till
     * {@link #mControllerAlwaysOnListener.onControllerAlwaysOnChanged}
     * callback triggers
     * @param value true for turning on else off
     * @return None
     */
    public void setControllerAlwaysOn(boolean value) {
        NxpNfcLogger.d(TAG, "setControllerAlwaysOn");
        if (value) {
            mControllerAlwaysOnLatch = new CountDownLatch(1);
            mNfcOemExtension.setControllerAlwaysOn(NfcOemExtension.ENABLE_TRANSPARENT);
            try {
                mControllerAlwaysOnLatch.await(NxpNfcConstants.SEND_RAW_WAIT_TIME_OUT_VAL,
                                            TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                NxpNfcLogger.e(TAG, "Error in setControllerAlwaysOn");
            }
        } else {
            mNfcOemExtension.setControllerAlwaysOn(NfcOemExtension.DISABLE);
        }
    }

    /**
     * @brief disable discovery and waits till
     * {@link NfcOemExtension.Callback#onRfDiscoveryStarted(boolean) callback triggers}
     * @return None
     */
    public void disableDiscovery() {
        NxpNfcLogger.d(TAG, "disableDiscovery");
        mDisCountDownLatch = new CountDownLatch(1);
        mNfcAdapter.setDiscoveryTechnology(mActivity,
                NfcAdapter.FLAG_READER_DISABLE, NfcAdapter.FLAG_LISTEN_DISABLE);
        try {
            mDisCountDownLatch.await(NxpNfcConstants.SEND_RAW_WAIT_TIME_OUT_VAL,
                            TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            NxpNfcLogger.e(TAG, "Error disabling discovery");
        }
    }

    /**
     * @brief enable discovery and waits till
     * {@link NfcOemExtension.Callback#onRfDiscoveryStarted(boolean) callback triggers}
     * @return None
     */
    public void enableDiscovery() {
        NxpNfcLogger.d(TAG, "enableDiscovery");
        mDisCountDownLatch = new CountDownLatch(1);
        mNfcAdapter.resetDiscoveryTechnology(mActivity);
        try {
            mDisCountDownLatch.await(NxpNfcConstants.SEND_RAW_WAIT_TIME_OUT_VAL,
                            TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            NxpNfcLogger.e(TAG, "Error disabling discovery");
        }
    }

    /**
     * @brief sets discover Technology
     * @param pollTechnology Flags indicating poll technologies.
     * @param listenTechnology Flags indicating listen technologies.
     * @return None
     */
    public void setDiscoveryTech(int pollTechnology, int listenTechnology) {
      NxpNfcLogger.d(TAG, "setDiscoveryTech");
      mDisCountDownLatch = new CountDownLatch(1);
      mNfcAdapter.setDiscoveryTechnology(mActivity, pollTechnology,
                                         listenTechnology);
      try {
        mDisCountDownLatch.await(NxpNfcConstants.SEND_RAW_WAIT_TIME_OUT_VAL,
                                 TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        NxpNfcLogger.e(TAG, "Error disabling discovery");
      }
    }

    private NfcOemExtension.Callback mOemExtensionCallback = new NfcOemExtension.Callback() {

        @Override
        public void onTagConnected(boolean connected, Tag tag) {
            NxpNfcLogger.d(TAG, "onTagConnected: " + connected);
        }

        @Override
        public void onRfDiscoveryStarted(boolean isDiscoveryStarted) {
            NxpNfcLogger.d(TAG, "onRfDiscoveryStarted: " + isDiscoveryStarted);
            NfcOperations.this.mIsDiscoveryStarted = isDiscoveryStarted;
            if (mDisCountDownLatch != null) mDisCountDownLatch.countDown();
        }

        @Override
        public void onCardEmulationActivated(boolean isActivated) {
            NfcOperations.this.mIsCardEmulationActivated = isActivated;
            NxpNfcLogger.d(TAG, "onCardEmulationActivated: " + isActivated);
        }

        @Override
        public void onRfFieldActivated(boolean isActivated) {
            NfcOperations.this.mIsRfFieldActivated = isActivated;
            NxpNfcLogger.d(TAG, "onRfFieldActivated: " + isActivated);
        }

    };

    ControllerAlwaysOnListener mControllerAlwaysOnListener = new ControllerAlwaysOnListener() {

        @Override
        public void onControllerAlwaysOnChanged(boolean isEnabled) {
            NxpNfcLogger.d(TAG, "onControllerAlwaysOnChanged: " + isEnabled);
            if (mControllerAlwaysOnLatch != null) mControllerAlwaysOnLatch.countDown();

        }
    };

    /**
     * @brief Getter of {@link #mIsRfFieldActivated}
     */
    public boolean isRfFieldActivated() {
        return this.mIsRfFieldActivated;
    }

    /**
     * @brief Getter of {@link #mIsCardEmulationActivated}
     */
    public boolean isCardEmulationActivated() {
        return this.mIsCardEmulationActivated;
    }

    /**
     * @brief Getter of {@link #mIsDiscoveryStarted}
     */
    public boolean isDiscoveryStarted() {
        return this.mIsDiscoveryStarted;
    }

    /**
     * @brief Query NfcAdapter Nfc is enabled or not
     * @return true if NFC is enabled else false
     */
    public boolean isEnabled() {
        return mNfcAdapter.isEnabled();
    }

    /**
     * @brief Query NfcAdapter ControllerAlwaysOn is enabled or not
     * @return true if ControllerAlwaysOn is on else false
     */
    public boolean isControllerAlwaysOn() {
        return mNfcAdapter.isControllerAlwaysOn();
    }
}

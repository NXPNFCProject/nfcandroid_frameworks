/*
*
*  The original Work has been changed by NXP.
*
*  Copyright 2025 NXP
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*  http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*
*/
package com.nxp.nfc.vendor.srd;

import android.nfc.NfcAdapter;
import com.nxp.nfc.INxpNfcAdapter;
import com.nxp.nfc.INxpNfcAdapter.SRDStatus;
import com.nxp.nfc.INxpNfcNtfHandler;
import com.nxp.nfc.NxpNfcConstants;
import com.nxp.nfc.NxpNfcLogger;
import com.nxp.nfc.core.NfcOperations;
import com.nxp.nfc.core.NxpNciPacketHandler;
import java.util.concurrent.Executors;

import java.io.IOException;

/**
* This class is responsible to start/stop the Srd reader and
* handles the srd action notfications
*/
public class SrdHandler implements INxpNfcNtfHandler {

    public static final byte SRD_MODE_NTF_SUB_GID_OID = (byte) 0xBC;
    /**
     * srd mode status
     */

    public static final int SRD_START_RF_DISCOVERY = 0xFF;
    public static final int SRD_START_DEFAULT_RF_DISCOVERY = 0xFE;
    public static final int SRD_EVT_TIMEOUT = 0xFD;
    public static final int SRD_INIT_MODE = 0xBF;
    public static final int SRD_ENABLE_MODE = 0xBE;
    public static final int SRD_DISABLE_MODE = 0xBD;
    private static final String TAG = "SrdHandler";
    private static boolean isSrdEnabled = false;
    private final NxpNciPacketHandler mNxpNciPacketHandler;
    private final NfcOperations mNfcOperations;
    private ISrdCallbacks mSrdCallbacks = null;

    public SrdHandler(NfcAdapter nfcAdapter) {
        this.mNxpNciPacketHandler = NxpNciPacketHandler.getInstance(nfcAdapter);
        this.mNfcOperations = NfcOperations.getInstance(nfcAdapter);
        mSRDStarted(false);
    }

    public boolean isSrdModeEnabled() {
        return isSrdEnabled;
    }

    public void mSRDStarted(boolean enabled) {
        isSrdEnabled = enabled;
    }

    @Override
    public void onVendorNciNotification(int gid, int oid, byte[] payload) {
        if (payload == null || payload.length < 2) {
            NxpNfcLogger.d(TAG, "Invalid payload");
            return;
        }

        byte subGidOid = payload[0];
        byte notificationType = payload[1];
        NxpNfcLogger.d(TAG, "Sub-GidOid: " + subGidOid + ", Notification Type: " + notificationType);

        if (subGidOid == SRD_MODE_NTF_SUB_GID_OID) {
            handleSrdNotification(notificationType);
        }
    }

    private void handleSrdNotification(byte notificationType) {
        int ntfType = Byte.toUnsignedInt(notificationType);
        NxpNfcLogger.d(TAG, "ntfType:" + ntfType);
        switch (ntfType) {
            case SRD_START_RF_DISCOVERY:
                NxpNfcLogger.d(TAG, "ACTION_NFC_SRD_START_RF_DISCOVERY");
                mNfcOperations.setDiscoveryTech(NfcAdapter.FLAG_LISTEN_NFC_PASSIVE_A, NfcAdapter.FLAG_LISTEN_DISABLE);
                break;
            case SRD_EVT_TIMEOUT:
                NxpNfcLogger.d(TAG, "SRD_EVT_TIMEOUT");
                isSrdEnabled = false;
                mNfcOperations.disableDiscovery();
                sendDefautDiscoverMapCmd();
                mNfcOperations.enableDiscovery();
                if (mSrdCallbacks != null) {
                    NxpNfcLogger.d(TAG, "Sending SRD Callabck to Application");
                    mSrdCallbacks.onSrdTimedout();
                } else {
                    NxpNfcLogger.i(TAG, "No callback registered for SRD");
                }
                break;
            case SRD_START_DEFAULT_RF_DISCOVERY:
                NxpNfcLogger.d(TAG, "ACTION_NFC_SRD_START_DEFAULT_RF_DISCOVERY");
                isSrdEnabled = false;
                mNfcOperations.disableDiscovery();
                sendDefautDiscoverMapCmd();
                mNfcOperations.enableDiscovery();
                break;
            default:
                NxpNfcLogger.d(TAG, "Unknown message received");
                break;
        }
    }

    /**
     * This API registers the Application callbacks to be called
     * for Srd notifications.
     *
     * @param callbacks : callback to be registered
     */
    public void registerSrdCallbacks(ISrdCallbacks callbacks) {
        NxpNfcLogger.d(TAG, "Entry registerSrdCallbacks");
        mSrdCallbacks = callbacks;
    }

    /**
     * This API unregisters the Application callbacks to be called
     * for Srd notifications.
     */
    public void unregisterSrdCallbacks() {
        NxpNfcLogger.d(TAG, "Entry unregisterSrdCallbacks");
        mSrdCallbacks = null;
    }

    public void sendDefautDiscoverMapCmd() {
        NxpNfcLogger.d(TAG, "Sending Default RF Discover Map cmd to controller");
        mNxpNciPacketHandler.registerCallback(Executors.newSingleThreadExecutor(), this);
        byte[] prop_discover_map_cmd = new byte[]{0x03, 0x04, 0x03, 0x02, 0x03, 0x02, 0x01, (byte) 0x80, 0x01, (byte) 0x80};
        byte[] vendorInitRsp = mNxpNciPacketHandler.sendVendorNciMessage(0x21, 0x00, prop_discover_map_cmd);
        if (vendorInitRsp != null && vendorInitRsp.length < 1) {
            NxpNfcLogger.e(TAG, "Vendor Init Rsp length is less than 2 bytes");
        }
        if (vendorInitRsp[0] == NfcAdapter.SEND_VENDOR_NCI_STATUS_SUCCESS) {
            NxpNfcLogger.d(TAG, "RD Discover map command is success success");
        } else {
            NxpNfcLogger.d(TAG, "Wrong VendorNciMessage Response");
        }
    }

    private @SRDStatus int sendSrdVendorNciMessage(byte[] srdCmd) throws IOException {
        try {
            NxpNfcLogger.d(TAG, "Sending SRD cmd through VendorNciMessage");
            mNxpNciPacketHandler.registerCallback(Executors.newSingleThreadExecutor(), this);
            byte[] vendorInitRsp = mNxpNciPacketHandler.sendVendorNciMessage(NxpNfcConstants.NFC_NCI_PROP_GID, NxpNfcConstants.NXP_NFC_PROP_OID, srdCmd);
            if (vendorInitRsp != null && vendorInitRsp.length < 2) {
                NxpNfcLogger.e(TAG, "Vendor Rsp length is less than 2 bytes");
                return INxpNfcAdapter.SRD_STATUS_FAILED;
            }
            if (vendorInitRsp[1] == NfcAdapter.SEND_VENDOR_NCI_STATUS_SUCCESS) {
                NxpNfcLogger.d(TAG, "SRD CMD success");
            } else {
                NxpNfcLogger.d(TAG, "Wrong VendorNciMessage Response");
                return INxpNfcAdapter.SRD_STATUS_FAILED;
            }
        } catch (Exception e) {
            NxpNfcLogger.d(TAG, "Exception in sendVendorNciMessage");
            throw new IOException("Error sending VendorNciMessage", e);
        }
        return INxpNfcAdapter.SRD_STATUS_SUCCESS;
    }

    public @SRDStatus int setSRDMode(boolean enable) throws IOException {
        NxpNfcLogger.d(TAG, "setSRDMode Enter : " + enable);
        if (enable == isSrdEnabled) {
            return INxpNfcAdapter.SRD_INPROGESS;
        }
        if (enable) {
            byte[] prop_init_cmd = new byte[2];
            prop_init_cmd[0] = (byte) SRD_INIT_MODE;
            prop_init_cmd[1] = 0x01;
            NxpNfcLogger.d(TAG, "Sending VendorNciMessage to init SRD");
            int srdInitStatus = sendSrdVendorNciMessage(prop_init_cmd);
            NxpNfcLogger.d(TAG, "srdInitStatus:" + srdInitStatus);
            if (srdInitStatus == INxpNfcAdapter.SRD_STATUS_FAILED) {
                return srdInitStatus;
            }
            if (mNfcOperations.isEnabled()) {
                NxpNfcLogger.d(TAG, "Disabling discovery");
                mNfcOperations.disableDiscovery();
                mNfcOperations.setControllerAlwaysOn(true);
            }
            byte[] prop_start_cmd = new byte[2];
            prop_start_cmd[0] = (byte) SRD_ENABLE_MODE;
            prop_start_cmd[1] = 0x01;
            NxpNfcLogger.d(TAG, "Sending VendorNciMessage to start SRD");
            int srdStartStatus = sendSrdVendorNciMessage(prop_start_cmd);
            NxpNfcLogger.d(TAG, "srdStartStatus:" + srdStartStatus);
            return srdStartStatus;

        } else {
            isSrdEnabled = false;
            NxpNfcLogger.d(TAG, "Please provide input true to SRD Mode.");
            return INxpNfcAdapter.SRD_STATUS_FAILED;
        }
    }
}
package com.nxp.id.ese.cosupdate;

import com.nxp.id.ese.access.IeSEAccess;
import com.nxp.id.ese.osupdate.OSUpdate;
import com.nxp.id.ese.osupdate.OSUpdateException;
import com.nxp.id.jsbl.uai.api.objects.UpdateControlData;
import com.nxp.id.tp.common.HexString;

/**
 * OSU/patch customer interface implementation.
 *
 */
public final class CustomerOSU extends OSUpdate implements ICOSUpdate {

  private static String UAIPatchVersion;

  private final int EFS_VERSION_LENGTH = 4;

  /**
   * Constructor will prepare the access to the eSE. On Android the NFC
   * Adapter shall provide dedicated methods for it.
   *
   * @param eSEAccessObj
   *            eSE access object implenting IeSEAccess
   */
  public CustomerOSU(IeSEAccess eSEAccessObj) { super(eSEAccessObj); }

  @Override
  public int checkCOSStatus(String COSPatchScriptFilePath,
                            String COSPatchVersionEFS, int retryCount) {
    int status = RESULT_UNKNOWN_ERROR;

    if (COSPatchScriptFilePath == null)
      return RESULT_COSSTATUS_ERROR_ABNORMAL_COS_PATH;

    if (retryCount <= 0)
      return RESULT_COSSTATUS_ERROR_ABNORMAL_RETRY_COUNT;
    try {
      prepareImageData(COSPatchScriptFilePath);
    } catch (Exception e) {
      e.printStackTrace();
    }
    status = checkPatchVersion(COSPatchScriptFilePath, COSPatchVersionEFS);

    if (status != RESULT_OK) {
      return status;
    }
    // status = accessObj.accessControlForCOSU(IeSEAccess.OSU_MODE_ENABLED);
    if (accessObj.accessControlForCOSU(IeSEAccess.OSU_MODE_ENABLED) !=
        IeSEAccess.SE_ACCESS_OPERATION_SUCCESS)
      return RESULT_COSSTATUS_ERROR_ACCESS_CONTROL_FAIL;
    status = isActionPending(false);
    // In case maximum retry counter has been reached
    // and still open activity, report an error
    if (status != RESULT_OK) {
      // No further action, device is up to date or UAI query UAIInfo fail.
    } else {
      // update COSPatchVersion with the version from eSE
      // UAIPatchVersion = Integer.toHexString(OSUpdate.uaiInfo_PatchId);
      if (retryCount < OSUpdate.uaiInfo.retryCounterOSPatch) {
        status = RESULT_COSSTATUS_EXCEED_RETRY_COUNT;
        accessObj.accessControlForCOSU(IeSEAccess.OSU_MODE_DISABLED);
      }
    }
    return status;
  }

  @Override
  public int preparation() {
    int status = accessObj.accessControlForCOSU(IeSEAccess.OSU_MODE_ENABLED);
    return (IeSEAccess.SE_ACCESS_OPERATION_SUCCESS == status)
        ? RESULT_OK
        : RESULT_COSSTATUS_ERROR_ACCESS_CONTROL_FAIL;
  }

  @Override
  public int execution() {
    int status = RESULT_UNKNOWN_ERROR;

    try {
      status = processOSU();
    } catch (Throwable exception) {
      status = RESULT_UNKNOWN_ERROR;
      exception.printStackTrace();
    }

    return status;
  }

  @Override
  public int completion() {
    // UAIPatchVersion = Integer.toHexString(getJCOPseqnum());
    int status = accessObj.accessControlForCOSU(IeSEAccess.OSU_MODE_DISABLED);
    return (IeSEAccess.SE_ACCESS_OPERATION_SUCCESS == status)
        ? RESULT_OK
        : RESULT_UNKNOWN_ERROR /*RESULT_VERIFY_ERROR_NFC_OFF_FAIL*/;
  }

  @Override
  public int verifyUpdatedCOSVersion() {
    int status = RESULT_VERITY_ERROR_COS_VERSION_UPDATE_FAIL;
    int accessStatus;

    try {
      status = isActionPending(true);

      // get the Final sequence number from the JCI;
      // note the final sequence number could be different in case of 1 step
      // update status = OSUpdate.isOSUCompleted();

      if (status == RESULT_COSSTATUS_ALREADY_UPDATED) {
        // update UAIPatchVersion with the version from eSE
        // UAIPatchVersion = Integer.toHexString(OSUpdate.uaiInfo_PatchId);
        // UAIPatchVersion = Integer.toHexString(getJCOPseqnum());

        accessStatus =
            accessObj.accessControlForCOSU(IeSEAccess.OSU_MODE_DISABLED);
        if (accessStatus != RESULT_OK)
          status = RESULT_VERIFY_ERROR_ACCESS_CONTROL_FAIL;

      } else if (status == RESULT_TRANSCEIVE_ERROR) {
        // In case sending UAIQuery command fail
        status = RESULT_VERIFY_ERROR_COS_VERSION_CHECK_FAIL;
      } else if (status == RESULT_UAI_INFO_ERROR) {
        // In case abnormal UAI information
        status = RESULT_VERIFY_ERROR_COS_VERSION_CHECK_FAIL;
      } else {
        // uai success but the osu update is interrupted in bewetween
        status = RESULT_VERIFY_ERROR_COS_VERSION_CHECK_FAIL;
      }
    } catch (Throwable exception) {
      status = RESULT_VERITY_ERROR_COS_VERSION_UPDATE_FAIL;
      exception.printStackTrace();
    }

    return status;
  }

  @Override
  public String getCOSPatchVersion() {
    return Integer.toHexString(getJCOPseqnum());
  }

  @Override
  public int recovery() {

    return execution();

    //        status = coldReset(OSUpdate.RESET_IN_RECOVERY);
    //        if (SemServiceManager.NO_ERROR != status)
    //            return RESULT_RECOVERY_ERROR_COLD_RESET_FAIL;
    //
    //        status =
    //        accessObj.accessControlForCOSU(IeSEAccess.OSU_MODE_DISABLED);
    //
    //        mSemService.close();

    //        return (SemServiceManager.NO_ERROR == status) ? RESULT_OK
    //                : RESULT_RECOVERY_ERROR_ACCESS_CONTROL_FAIL;

    // return RESULT_OK;
  }

  /**
   * compare COS patch version between EFS file and script
   *
   * @param COSPatchScriptFilePath
   *        COSPatchVersionEFS
   * @return status
   */
  private int checkPatchVersion(String COSPatchScriptFilePath,
                                String COSPatchVersionEFS) {

    int status = RESULT_UNKNOWN_ERROR;
    char[] verEfs = new char[EFS_VERSION_LENGTH];
    int patchVersionInEfs = 0;
    int temp = 0;

    // get COSPatchVersionEFS
    patchVersionInEfs = Integer.parseInt(COSPatchVersionEFS, 16);

    // Compare version of efs file and version of patch script
    String strPatchNumbers = "";
    strPatchNumbers += "patchVersionInScript=";
    strPatchNumbers += Integer.toHexString(maxFSN);
    strPatchNumbers += ", patchVersionInEfs=";
    strPatchNumbers += Integer.toHexString(patchVersionInEfs);
    if (maxFSN <= patchVersionInEfs) {
      if ((maxFSN == patchVersionInEfs && isSelfUpdate() == RESULT_OK)) {
        // OSU is for SElf update
        status = RESULT_OK;
      } else {
        // not a self update and the patch/OSU is already updated
        status = RESULT_COSSTATUS_ALREADY_UPDATED;
      }
    } else {
      status = RESULT_OK;
    }
    return status;
  }
}

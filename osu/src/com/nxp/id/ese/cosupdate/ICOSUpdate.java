package com.nxp.id.ese.cosupdate;

/**
 * eSE COS update
 * @author NXP
 * @version 1.0.0: First release - 2018/09/14
 * @version 1.1.0: eSE interface is changed from DWP to SPI - 2019/02/27
 * @version 1.1.1: Return value of SemServiceManager open and close are changed
 * to SemServiceManager.NO_ERROR_SPI - 2019/03/06
 *
 */
public interface ICOSUpdate {
  public final static int RESULT_OK = 0;
  public final static int RESULT_COSSTATUS_ALREADY_UPDATED = 1;
  public final static int RESULT_COSSTATUS_ERROR_ACCESS_CONTROL_FAIL = -1;
  public final static int RESULT_COSSTATUS_ERROR_CHECK_COS_PATCH_VERSION = -2;
  public final static int RESULT_COSSTATUS_ERROR_ABNORMAL_COS_PATH = -3;
  public final static int RESULT_COSSTATUS_ERROR_ABNORMAL_RETRY_COUNT = -4;
  public final static int RESULT_COSSTATUS_EXCEED_RETRY_COUNT = -5;
  public final static int RESULT_COSSTATUS_NOT_ENOUGH_MEMORY = -6;
  public final static int RESULT_AP_ERROR_SAMSUNG_SIGNATURE = -7;
  public final static int RESULT_ERROR_SPI_ACCESS_CONTROL_DISABLE = -8;
  public final static int RESULT_ERROR_SPI_ACCESS_CONTROL_ENABLE = -9;
  public final static int RESULT_PREPARATION_ERROR_COLD_RESET_FAIL = -10;
  public final static int RESULT_PREPARATION_ERROR_START_COS_PATCH_FAIL = -11;
  public final static int RESULT_EXECUTION_ERROR_SAMSUNG_SIGNATURE = -12;
  public final static int RESULT_EXECUTION_ERROR_COS_PATCH = -13;
  public final static int RESULT_COMPLETION_ERROR_COLD_RESET_FAIL = -14;
  public final static int RESULT_VERITY_ERROR_COS_VERSION_UPDATE_FAIL = -15;
  public final static int RESULT_VERIFY_ERROR_COS_VERSION_CHECK_FAIL = -16;
  public final static int RESULT_VERIFY_ERROR_ACCESS_CONTROL_FAIL = -17;
  public final static int RESULT_ERROR_WRITE_EFS_COS_PATCH_VERSION = -19;
  public final static int RESULT_RECOVERY_ERROR_ACCESS_CONTROL_FAIL = -20;
  public final static int RESULT_RECOVERY_ERROR_COLD_RESET_FAIL = -21;
  // NXP error code
  // ===============================================================
  public final static int RESULT_UNKNOWN_ERROR = 100;
  public final static int RESULT_WRONG_SW = 101;
  public final static int RESULT_TRANSCEIVE_ERROR = 102;
  public final static int RESULT_UAI_INFO_ERROR = 103;

  /**
   * Send COS version check command
   * Parse eSE vendor code, eSE COS version, needed NVM and VM size for COS
   * updating Compare COS version of eSE and COS update script Check COS update
   * retry count Send eSE memory status check command for COS updating
   *
   * @param COSPath COS update binary file path
   * @param COSPatchVersionEFS Current patched COS version saved in efs file
   * @param retryCount COS update retry count limitation
   * @return result (RESULT_OK: COS update is needed,
   * RESULT_COSSTATUS_ALREADY_UPDATED: already updated, negative value: error
   * code)
   */
  public int checkCOSStatus(String COSPatchScriptFilePath,
                            String COSPatchVersionEFS, int retryCount);

  /**
   * Send COS update start command
   * Call NFC service API for COS update mode (blocking SWP and SPI access of
   * other processes, Reset eSE)
   * @return result (RESULT_OK: success, negative value: error code)
   */
  public int preparation();

  /**
   * Send COS update script
   * @return result (RESULT_OK: success, negative value: error code)
   */
  public int execution();

  /**
   * Call NFC service API for Normal mode (Reset eSE, unblocking SWP and SPI
   * access of other processes)
   * @return result (RESULT_OK: success, negative value: error code)
   */
  public int completion();

  /**
   * Send COS version check Command
   * Compare COS version of eSE and COS update script
   * @return result (RESULT_OK: success, negative value: error code)
   */
  public int verifyUpdatedCOSVersion();

  /**
   * Call shall get updated COS Patch version and
   * write updated COS Patch version in  file.
   *
   * @return result (RESULT_OK: success, negative value: error code)
   */
  public String getCOSPatchVersion();

  /**
   * To escape from the abnormal status of eSE during COS Patch operation,
   * recovery API shall be used any time.
   * eSE shall be cold reset.
   * Escape from the eSE COS Patch dedicated mode.
   *
   * @return result (RESULT_OK: success, negative value: error code)
   */
  public int recovery();
}

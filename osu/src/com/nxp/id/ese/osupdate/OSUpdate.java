package com.nxp.id.ese.osupdate;

import java.io.File;
import java.util.List;

import com.nxp.id.ese.access.IeSEAccess;
import com.nxp.id.ese.cosupdate.ICOSUpdate;
import com.nxp.id.jsbl.uai.api.objects.CustomerControlImage;
import com.nxp.id.jsbl.uai.api.objects.DeliveryPackage;
import com.nxp.id.jsbl.uai.api.objects.ExtendedDeliveryPackage;
import com.nxp.id.jsbl.uai.api.objects.JCOPControlImage;
import com.nxp.id.jsbl.uai.api.objects.UpdateControlCase;
import com.nxp.id.jsbl.uai.api.objects.UpdateControlData;
import com.nxp.id.jsbl.uai.api.objects.UpdateScript;
import com.nxp.id.jsbl.uai.api.objects.UpdateScripts;
import com.nxp.id.jsbl.uai.api.util.UAIAPIException;
import com.nxp.id.jsbl.uai.api.util.Utils;
import com.nxp.id.tp.common.HexString;
import com.nxp.id.tp.common.coding.asn1.Asn1IA5String;
import com.nxp.id.tp.common.coding.asn1.Asn1Integer;
import com.nxp.id.tp.common.coding.asn1.Asn1Sequence;
// import com.nxp.jc.tools.nxpcardreader.NXPCardReader;

/**
 * Generic class for an eSE OS update.
 */
public class OSUpdate {

  /** Possible processing status. */
  static public final int STATUS_OSU_SUCCESS = 0;
  ;

  protected final static int RESET_IN_PREPARATION = 0;
  protected final static int RESET_IN_COMPLETION = 1;
  protected final static int RESET_IN_RECOVERY = 2;

  /** Current package information for ongoing OSU. */
  // static DeliveryPackage deliveryPackage;
  static ExtendedDeliveryPackage extDelPackage;
  static JCOPControlImage jcopControlImage;
  static CustomerControlImage customerControlImage;
  static UpdateScripts updateScripts;

  /** Constant indicating no further action required. */
  static public final int NO_ACTION = -1;

  /** Definition of all OSU/UAI commands. */
  static private final byte[] OSUTrigger = {0x4F,       0x70,       (byte)0x80,
                                            0x13,       0x04,       (byte)0xDE,
                                            (byte)0xAD, (byte)0xBE, (byte)0xEF};
  static private final byte[] UAITrigger = {0x4F, 0x70, (byte)0x80, 0x13, 0x04,
                                            0x4a, 0x55, 0x41,       0x49};
  static private final byte[] UAIQuery = {
      (byte)0x80, (byte)0xCA, 0x00, (byte)0xFE, (byte)0x02, (byte)0xDF, 0x43};
  static private final byte[] SequenceNumber = {
      (byte)0x80, (byte)0xCA, 0x00, (byte)0xFE, (byte)0x02, (byte)0xDF, 0x31};

  /** Last queried UAI information. */
  static protected UAIQueryInfo uaiInfo;
  /** Access object to the eSE. */
  protected IeSEAccess accessObj;
  /** patch id of uaiInfo */
  protected static int uaiInfo_PatchId;
  /** Requested patch id of the update. */
  protected static int maxFSN;
  /** Requested patch id of the update. */
  // protected static boolean isSelfUpdate;
  protected static boolean preOSUScript;

  private static Integer Pre_OSU_Script_ID = 0;

  private static Integer Post_OSU_Script_ID = 0;

  /**
   * Constructor to create the eSE access depending on the given context.
   *
   * @param eseAccessObj
   *            eSE access object
   */
  public OSUpdate(IeSEAccess eseAccessObj) {
    accessObj = eseAccessObj;
    /* Open the eSE access */
    accessObj.open();
  }

  /**
   * Method to prepare the given image from the file path.
   *
   * @param fullImageFilePath
   * @throws UAIAPIException
   * @throws Exception
   */
  protected static void prepareImageData(String fullImageFilePath)
      throws OSUpdateException, Exception {

    File fullPackage = null;

    try {
      fullPackage = new File(fullImageFilePath);
    } catch (Throwable e) {
      throw new OSUpdateException(OSUpdateException.INVALID_FILE_IMAGE,
                                  e.getMessage());
    }

    Asn1Sequence fullPackageDER =
        Asn1Sequence.parse(HexString.fromFile(fullPackage, true));
    Asn1Sequence sequence = (Asn1Sequence)fullPackageDER.get(0);

    // Extract the "Extended Data Package" string
    String tag = ((Asn1IA5String)sequence.get(0)).getString();

    if (!tag.equals("EDP")) {
      throw new OSUpdateException(
          "JCOPControlImageData Tag did not match expected value. Expected JCI, but was " +
          tag + ".");
    }
    // Extract the version of "Extended Data Package"
    int version = ((Asn1Integer)sequence.get(1)).getInteger();

    extDelPackage = new ExtendedDeliveryPackage(fullPackage);

    customerControlImage = extDelPackage.getCustomerControlImage();

    jcopControlImage = extDelPackage.getDeliveryPackage().getJCOPControlImage();

    updateScripts = extDelPackage.getDeliveryPackage().getUpdateScripts();

    // prepareImageData(sequence.get(2).encode().toByteArray(),
    // sequence.get(3).encode().toByteArray());
    getsFinalSequenceNumber();
  }

  /**
   * Method to retrieve the final sequence number of a given update plan.
   *
   * @return Maximum final sequence number out of the package.
   *
   * @throws Exception In case no control image is given
   */
  private static Integer getsFinalSequenceNumber() throws Exception {

    if (null == customerControlImage || null == jcopControlImage) {
      throw new Exception("Invalid state, no preparation done");
    }

    maxFSN = -1;
    UpdateControlData ctrlData = jcopControlImage.getUpdateControlData();

    List<UpdateControlCase> ctrlDataList = ctrlData.getUpdateControlCaseList();

    // Loop through each update plan to check against CSN and RSN
    for (UpdateControlCase updateControlCase : ctrlDataList) {
      Integer fsnItem = updateControlCase.getFSN().get(0);
      Integer Idx = updateControlCase.getActionIndex();
      Integer OSID = updateControlCase.getOSID().get(0);

      if (OSID == 0x005A) {
        if (Pre_OSU_Script_ID == 0) {
          Pre_OSU_Script_ID = Idx;
        } else {
          Post_OSU_Script_ID = Idx;
        }
      }

      if (maxFSN < fsnItem) {
        maxFSN = fsnItem;
      }
    }

    return maxFSN;
  }

  /**
   * Method to prepare the image data in case it is split into the JCOP
   * delivery image byte array and the customer control image byte array.
   *
   * @param deliveryImage
   *            array holding the JCOP delivery image
   * @param customerImage
   *            array holding the customer control image
   * @throws Exception
   */
  protected static void prepareImageData(byte[] deliveryImage,
                                         byte[] customerImage)
      throws Exception {
    /*
            // read the delivery package
            deliveryPackage = new DeliveryPackage(deliveryImage);
            // Read the control image
            jcopControlImage = deliveryPackage.getJCOPControlImage();
            // read the customer control image
            customerControlImage = new CustomerControlImage(customerImage);*/
  }

  /**
   * Method to query the current UAI information of the eSE.
   *
   * @return filled UAI object
   */
  private int queryUAIInfo() {
    byte[] resp = accessObj.send(UAIQuery);
    if (resp == null) {
      return ICOSUpdate.RESULT_TRANSCEIVE_ERROR;
    }
    uaiInfo = UAIQueryInfo.parse(resp);
    if (uaiInfo == null) {
      return ICOSUpdate.RESULT_UAI_INFO_ERROR;
    }

    return ICOSUpdate.RESULT_OK;
  }

  /**
   * Helper method to retrieve next action based on CSN and RSN for
   * OS patching. Only scripts respect to the OS1 (Updater1) are
   * taken into account.
   *
   * @param fsn Final sequence number
   * @param csn current sequence number
   * @return Next action index, -1 in case of no further pending action
   *
   * @throws Exception In case control images are not loaded
   */
  private int getMatchingAction(int fsn, int csn) throws Exception {

    int retVal = NO_ACTION;

    if (null == customerControlImage || null == jcopControlImage) {
      throw new Exception("Invalid state, no preparation done");
    }

    UpdateControlData ctrlData = jcopControlImage.getUpdateControlData();

    List<UpdateControlCase> ctrlDataList = ctrlData.getUpdateControlCaseList();

    short currOSState = uaiInfo.UOS_ID;

    if (isSelfUpdate() == ICOSUpdate.RESULT_OK) {

      if (currOSState == uaiInfo.OSU_1_ID) {
        // could be Self update or Real update with single plan
        return 1; // step 1
      } else if (currOSState == uaiInfo.OSU_2_ID) {
        return 2; // step 2 for self update
      } else if (currOSState == uaiInfo.OSU1_SELF_ID) {
        return 3; // step 3
      } else {
        return NO_ACTION; // JCOP in is active and no steps needed.
      }
    } else {

      // Loop through each update plan to check against CSN and RSN
      for (UpdateControlCase updateControlCase : ctrlDataList) {

        Integer fsnItem = updateControlCase.getFSN().get(0);
        Integer csnItem = updateControlCase.getCSN().get(0);
        Integer osIDItem = updateControlCase.getOSID().get(0);

        if ((fsn == fsnItem) && (csn == csnItem) &&
            ((currOSState == uaiInfo.OSU_2_ID &&
              osIDItem == uaiInfo.OSU_2_ID) || /*
//after UOS2 state shall be 0x0011 again after step2 in case of self update
(currOSState == uaiInfo.OSU1_SELF_ID && osIDItem == uaiInfo.OSU1_SELF_ID)||*/
             // after UOS2 state shall be 0x0001 again after step2 in case of
             // real update
             (currOSState == uaiInfo.OSU_1_ID &&
              osIDItem == uaiInfo.OSU_1_ID))) {
          // step2 or step3
          retVal = updateControlCase.getActionIndex();
          // break the for loop
          break;
        } else {
          Integer RSN = updateControlCase.getRSN().get(0);
          // this is because the UAI query is done for the first time after the
          // OSU trigger in the OSU context
          if ((currOSState == uaiInfo.OSU_1_ID &&
               osIDItem == uaiInfo.OSU_1_ID) &&
              ((fsn == csn) &&
               // check if the RSN is same as CSN
               (RSN == csn))) {
            // step 1
            retVal = updateControlCase.getActionIndex();
            // break the for loop
            break;
          }
        }
      }
    }
    return retVal;
  }

  /**
   * Prepare the UAI by first ensure the eSE in the correct state and upload
   * the JCI and the CCI. Prepare the scripting for the final processing.
   *
   * @throws Exception
   */
  private int prepareUAI() throws Exception {

    queryUAIInfo();
    accessObj.reset();

    int status = ICOSUpdate.RESULT_OK;

    // Upload the customer control image if requested
    if (uaiInfo.state == uaiInfo.STATE_IDLE) {
      status = sendAndValidateSW(UAITrigger);
      if (status == ICOSUpdate.RESULT_WRONG_SW)
        return ICOSUpdate.RESULT_PREPARATION_ERROR_START_COS_PATCH_FAIL;

      for (String apdu : customerControlImage.toApdu()) {
        status = sendAndValidateSW(Utils.hexStringToByteArray(apdu));
        if (status == ICOSUpdate.RESULT_WRONG_SW)
          return ICOSUpdate.RESULT_EXECUTION_ERROR_SAMSUNG_SIGNATURE;
      }
    }

    queryUAIInfo();

    if (uaiInfo.state == uaiInfo.STATE_UAI_CCI_VALIDATED) {
      for (String apdu : jcopControlImage.toApdu()) {
        status = sendAndValidateSW(Utils.hexStringToByteArray(apdu));
        if (status == ICOSUpdate.RESULT_WRONG_SW)
          return ICOSUpdate.RESULT_EXECUTION_ERROR_COS_PATCH;
      }
    }

    // --------------------------------
    // extract update scripts from the delivery package
    // updateScripts = extDelPackage.getUpdateScripts();

    // extract update scripts
    for (UpdateScript updateScript : updateScripts.getUpdateScriptList()) {
      System.out.println(updateScript);
    }

    // Verify that the JCI hash in CCI matches the provided JCI
    /* System.out.println("Verify JCI hash in CCI: "
             +
       customerControlImage.doesJCOPControlImageMatch(deliveryPackage.getJCOPControlImage()));*/
    return status;
  }

  /**
   * Method to start the processing of the OSU. Therefore remaining action
   * will be queried until final state is reached.
   *
   * @throws Exception
   */
  public int processOSU() throws Exception {

    int status = ICOSUpdate.RESULT_OK;
    int iter = 0;
    int action = 0;
    final String VISO_readerProtocol = "SCR:VIRTUALISO:0:100:1.8:1.0:4:1";
    final String SPI_readerProtocol = "SCR:SPI:0:100:1.8:1.0";

    if (null == customerControlImage || null == jcopControlImage) {
      throw new Exception("Invalid state, no preparation done");
    }

    accessObj.reset();
    queryUAIInfo();
    accessObj.reset();

    // extract update scripts from the delivery package
    /*updateScripts = extDelPackage.getUpdateScripts();*/

    // check if pre-OSU script is present in the JCI
    if (uaiInfo.UOS_ID == uaiInfo.JCOP_ID &&
        checkForPrePostOSUScript(Pre_OSU_Script_ID) &&
        getJCOPseqnum() == uaiInfo.RSN) {
      // get update scripts for a specific action
      UpdateScript updateScript =
          updateScripts.getUpdateScript(Pre_OSU_Script_ID);
      // NXPCardReader cardreader_VISO = new NXPCardReader(VISO_readerProtocol);
      // swtich to VISO
      // cardreader_VISO.open();

      // get APDUs from the update script
      for (String apdu : updateScript.getApdus()) {
        sendAndValidateSW(Utils.hexStringToByteArray(apdu));
        /*if (status == ICOSUpdate.RESULT_WRONG_SW){
            return ICOSUpdate.RESULT_UNKNOWN_ERROR;
        }*/
      }

      Pre_OSU_Script_ID = 0;
      status = ICOSUpdate.RESULT_OK;
    }

    // reset
    // accessObj.reset();

    // swtich to SPI
    // NXPCardReader cardreader_SPI = new NXPCardReader(SPI_readerProtocol);
    // swtich to VISO
    // cardreader_SPI.open();

    // send CCI and JCI to eSE
    accessObj.reset();

    if (uaiInfo.UOS_ID == uaiInfo.JCOP_ID && getJCOPseqnum() == uaiInfo.RSN) {
      accessObj.reset();
      status = prepareUAI();
    }

    accessObj.reset();
    queryUAIInfo();
    accessObj.reset();

    if (uaiInfo.state == uaiInfo.STATE_UAI_JCI_VALIDATED &&
        uaiInfo.UOS_ID == uaiInfo.JCOP_ID) {
      status = sendAndValidateSW(OSUTrigger);
      if (status == ICOSUpdate.RESULT_WRONG_SW)
        return ICOSUpdate.RESULT_PREPARATION_ERROR_START_COS_PATCH_FAIL;
    }

    accessObj.reset();
    queryUAIInfo();
    accessObj.reset();

    // while((uaiInfo.UOS_ID == uaiInfo.OSU_1_ID) ||(uaiInfo.UOS_ID ==
    // uaiInfo.OSU_2_ID)||) {
    while ((uaiInfo.UOS_ID != uaiInfo.JCOP_ID) && (iter < 5)) {
      // accessObj.reset();

      //        do {
      // Query current UAI status

      action = getMatchingAction(uaiInfo.FSN, uaiInfo.CSN);

      // action = uaiInfo.

      // get the action to be performed for a specific state (obtain
      // the values from the UAI query command)
      /*if (maxFSN > uaiInfo.CSN)
          action = getMatchingAction(uaiInfo.CSN + 1, uaiInfo.CSN);
      else
          action = getMatchingAction(uaiInfo.FSN, uaiInfo.CSN);
      System.out.println("Matching action : " + action);*/

      if (OSUpdate.NO_ACTION != action) {

        // get update scripts for a specific action
        UpdateScript updateScript = updateScripts.getUpdateScript(action);

        // get APDUs from the update script
        for (String apdu : updateScript.getApdus()) {
          status = sendAndValidateSW(Utils.hexStringToByteArray(apdu));
          if (status == ICOSUpdate.RESULT_WRONG_SW)
            return ICOSUpdate.RESULT_EXECUTION_ERROR_COS_PATCH;
        }
      }

      iter++;

      /*status = coldReset(RESET_IN_COMPLETION);
      if (status != ICOSUpdate.RESULT_OK)
          return status;*/

      //                status=NXPCOSUpdate.closeService();
      //                sleep(50);
      //                status=NXPCOSUpdate.openService();
      //                if (isSelfUpdate)
      //                    break; // no iteration
      /* if (status != ICOSUpdate.RESULT_OK)
           return status;*/
      accessObj.reset();
      queryUAIInfo();

      //}
    };
    accessObj.reset();

    // check if post-OSU script is present in the JCI
    if (uaiInfo.UOS_ID == uaiInfo.JCOP_ID &&
        checkForPrePostOSUScript(Post_OSU_Script_ID) &&
        getJCOPseqnum() == uaiInfo.FSN) {
      // swtich to VISO
      // NXPCardReader cardreader_VISO1 = new
      // NXPCardReader(VISO_readerProtocol);
      // swtich to VISO
      // cardreader_VISO1.open();
      // get update scripts for a specific action
      UpdateScript updateScript =
          updateScripts.getUpdateScript(Post_OSU_Script_ID);

      // get APDUs from the update script
      for (String apdu : updateScript.getApdus()) {
        sendAndValidateSW(Utils.hexStringToByteArray(apdu));
        /*if (status == ICOSUpdate.RESULT_WRONG_SW)
            return ICOSUpdate.RESULT_UNKNOWN_ERROR;*/
      }

      // swtich to SPI
      // NXPCardReader cardreader_SPI1 = new NXPCardReader(SPI_readerProtocol);
      // swtich to VISO
      // cardreader_SPI1.open();
      Post_OSU_Script_ID = 0;
      status = ICOSUpdate.RESULT_OK;
    }

    // print the contents of each component in a nice human-readable format
    // System.out.println(deliveryPackage);
    System.out.println(jcopControlImage);
    System.out.println(updateScripts);
    System.out.println(customerControlImage);
    return status;
  }

  /**
   *
   * @param scriptIndex
   * @return
   */
  private boolean checkForPrePostOSUScript(int scriptIndex) {

    UpdateControlData ctrlData = jcopControlImage.getUpdateControlData();

    List<UpdateControlCase> ctrlDataList = ctrlData.getUpdateControlCaseList();

    for (UpdateControlCase updateControlCase : ctrlDataList) {
      if (updateControlCase.getActionIndex() == scriptIndex) {
        return true;
      }
    }

    return false;
  }

  /**
   * Method to retrieve current status of the eSE if any pending OSU action is
   * pending.
   *
   * @return
   * @throws Exception
   *             In case UAI query throws an exception
   */
  protected int isActionPending(boolean verify) {

    int status = OSUpdate.NO_ACTION;

    if (0 == maxFSN) {
      return ICOSUpdate.RESULT_UNKNOWN_ERROR;
    }

    status = queryUAIInfo();

    if (status == ICOSUpdate.RESULT_OK) {
      // UAI query is successful

      if (uaiInfo.UOS_ID == uaiInfo.JCOP_ID && verify) {
        return ICOSUpdate.RESULT_COSSTATUS_ALREADY_UPDATED;
      }
    }
    // Query current UAI status

    /*        if (status == ICOSUpdate.RESULT_OK) {
                uaiInfo_PatchId = uaiInfo.CSN;

                // Check against the requested patch id.
    //            isSelfUpdate = bSelfUpdate;
    //            if (bSelfUpdate) {
    //                Log.i(TAG, TAG_COSPATCH + "allows update to same
    version");
    //                if (maxFSN < uaiInfo_PatchId)
    //                    status = ICOSUpdate.RESULT_COSSTATUS_ALREADY_UPDATED;
    //            } else {
                    if (maxFSN < uaiInfo_PatchId &&
                                    ((isSelfUpdate() != ICOSUpdate.RESULT_OK) &&
    (maxFSN == uaiInfo_PatchId)))
                            //the OSU is not self update and the CSN is same as
    FSN status = ICOSUpdate.RESULT_COSSTATUS_ALREADY_UPDATED;
    //            }
            }*/
    return status;
  }

  /**
   *  Method to get the JCOP sequence number from the card
   * @return sequence num
   */
  protected int getJCOPseqnum() {
    int JCOPseqNum = 0;

    byte[] resp = accessObj.send(SequenceNumber);

    JCOPseqNum = resp[5];

    // upper nibble
    JCOPseqNum = (JCOPseqNum << 8);

    JCOPseqNum |= (0x00FF & resp[6]);

    return JCOPseqNum;
  }

  /**
   * Helper method to send and validate and response of an APDU equals SW =
   * 0x9000.
   *
   * @param data
   *            Data to be send
   * @throws Exception
   */
  private int sendAndValidateSW(byte[] data) throws Exception {
    int status = ICOSUpdate.RESULT_OK;
    byte[] response = accessObj.send(data);

    if ((response.length != 2) ||
        (0x00 != response[1] && (0x90 != (response[0] & 0xFF)))) {
      status = ICOSUpdate.RESULT_WRONG_SW;
    }
    return status;
  }

  /**
   * eSE cold reset method
   *
   * @param reason
   *            Reason for cold reset
   * @throws Exception
   */
  protected int coldReset(int reason) {
    int status = ICOSUpdate.RESULT_OK;

    //        status = accessObj.resetForCOSU(reason);
    //        sleep(100);
    //        accessObj.close();
    //        sleep(100);
    //        accessObj.open();
    //        sleep(100);
    //        status = accessObj.resetForCOSU(reason);
    //        sleep(100);

    return status;
  }

  /**
   * Sleep method to give delay between after eSE cold reset
   *
   * @param ms
   *            time in milliseconds
   * @throws Exception
   */
  protected void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ie) {
      ie.printStackTrace();
    }
  }

  /**
   *  Method checks if the current OSU is self update script
   * @return
   */
  public static int isSelfUpdate() {
    UpdateControlData ctrlData = jcopControlImage.getUpdateControlData();

    List<UpdateControlCase> ctrlDataList = ctrlData.getUpdateControlCaseList();

    // Loop through each update plan to check against CSN and RSN
    for (UpdateControlCase updateControlCase : ctrlDataList) {
      Integer osidItem = updateControlCase.getOSID().get(0);

      if (osidItem == uaiInfo.OSU1_SELF_ID) {
        return ICOSUpdate.RESULT_OK;
      }
    }
    return ICOSUpdate.RESULT_COSSTATUS_ALREADY_UPDATED;
  }
}

package com.nxp.id.ese.osupdate;

/**
 * Class describing details of the current status of the UAI.
 *
 */
public class UAIQueryInfo {

  /** Description of available tags. */
  private static final byte V_TAG = (byte)0x80;
  private static final byte CSN_TAG = (byte)0x81;
  private static final byte RSN_TAG = (byte)0x82;
  private static final byte FSN_TAG = (byte)0x83;
  private static final byte PSN_TAG = (byte)0x84;
  private static final byte UOS_ID_TAG = (byte)0x85;
  private static final byte SEARIAL_NUM_TAG = (byte)0x86;
  private static final byte DEV_TYPE_TAG = (byte)0x87;
  private static final byte CUSTOM_PUB_KEY_ID_TAG = (byte)0x88;
  private static final byte SIGN_PUB_KEY_ID_TAG = (byte)0x89;
  private static final byte RM_TAG = (byte)0x8A;
  private static final byte STATE_TAG = (byte)0x8B;
  private static final byte HASH_TAG = (byte)0x8C;
  private static final byte OSU_RETRY_CNTR_TAG = (byte)0x8D;
  private static final byte PATCH_RETRY_CNTR_TAG = (byte)0x8E;

  /** Description of possible states. */
  public static final short OSU_1_ID = 0x0001;
  public static final short OSU_2_ID = 0x0002;
  public static final short Pre_OSU_Script_ID = 0x0004;
  public static final short Post_OSU_Script_ID = 0x0005;
  public static final short JCOP_ID = 0x005A;
  public static final short OSU1_SELF_ID = 0x0011;
  public static final short STATE_IDLE = 0x5A5A;
  public static final short STATE_UAI_CCI_VALIDATED = 0x1111;
  public static final short STATE_UAI_JCI_VALIDATED = 0x2222;
  public static final short STATE_UAI_ERROR = (short)0xA5A5;

  private static final byte[] prefixRsp = {(byte)0xFE, (byte)0x81, (byte)0xAD,
                                           (byte)0xDF, (byte)0x43, (byte)0x81,
                                           (byte)0xA9};

  public byte V;
  public short CSN;
  public short RSN;
  public short FSN;
  public short PSN;
  public short UOS_ID;
  public byte[] serialNo = new byte[24];
  public byte deviceType;
  public byte[] customerPubKeyID = new byte[32];
  public byte[] signaturePubKeyID = new byte[32];
  public byte RM;
  public short state;
  public byte[] hash = new byte[32];
  public short retryCounterOSU;
  public short retryCounterOSPatch = 0;

  private UAIQueryInfo() {}

  /**
   *
   * @param queryReponse
   * @throws Exception
   */
  public static UAIQueryInfo parse(byte[] queryReponse) {
    short offset;
    byte tag;
    byte length;

    for (offset = 2; offset < (short)prefixRsp.length; offset++) {
      if (prefixRsp[offset] != queryReponse[offset]) {
        System.out.println("UAI: Invalid response prefix");
        return null;
      }
    }
    UAIQueryInfo object = new UAIQueryInfo();
    // Response without SW
    while (offset < queryReponse.length - 2) {

      tag = queryReponse[offset++];
      length = queryReponse[offset++];

      switch (tag) {
      case V_TAG:
        object.V = queryReponse[offset];
        break;
      case CSN_TAG:
        object.CSN = (short)((queryReponse[offset] << 8) +
                             (queryReponse[offset + 1] & 0xFF));
        break;
      case RSN_TAG:
        object.RSN = (short)((queryReponse[offset] << 8) +
                             (queryReponse[offset + 1] & 0xFF));
        break;
      case FSN_TAG:
        object.FSN = (short)((queryReponse[offset] << 8) +
                             (queryReponse[offset + 1] & 0xFF));
        break;
      case PSN_TAG:
        object.PSN = (short)((queryReponse[offset] << 8) +
                             (queryReponse[offset + 1] & 0xFF));
        break;
      case UOS_ID_TAG:
        object.UOS_ID = (short)((queryReponse[offset] << 8) +
                                (queryReponse[offset + 1] & 0xFF));
        break;
      case SEARIAL_NUM_TAG:
        System.arraycopy(queryReponse, offset, object.serialNo, 0, length);
        break;
      case DEV_TYPE_TAG:
        object.deviceType = queryReponse[offset];
        break;
      case CUSTOM_PUB_KEY_ID_TAG:
        System.arraycopy(queryReponse, offset, object.customerPubKeyID, 0,
                         length);
        break;
      case SIGN_PUB_KEY_ID_TAG:
        System.arraycopy(queryReponse, offset, object.signaturePubKeyID, 0,
                         length);
        break;
      case RM_TAG:
        object.RM = queryReponse[offset];
        break;
      case STATE_TAG:
        object.state = (short)((queryReponse[offset] << 8) +
                               (queryReponse[offset + 1] & 0xFF));
        break;
      case HASH_TAG:
        System.arraycopy(queryReponse, offset, object.hash, 0, length);
        break;
      case OSU_RETRY_CNTR_TAG:
        object.retryCounterOSU = (short)((queryReponse[offset] << 8) +
                                         (queryReponse[offset + 1] & 0xFF));
        break;
      case PATCH_RETRY_CNTR_TAG:
        object.retryCounterOSPatch = (short)((queryReponse[offset] << 8) +
                                             (queryReponse[offset + 1] & 0xFF));
        break;
      default:
        System.out.println("Unknown tag in query response");
        return null;
      }
      offset += length;
    }

    return object;
  }

  /**
   *
   */
  public String toString() {
    String collection = "\nUAI Query Response\n";

    collection += String.format("V______________________: 0x%02X\n", V);
    collection += String.format("CSN____________________: 0x%04X\n", CSN);
    collection += String.format("RSN____________________: 0x%04X\n", RSN);
    collection += String.format("FSN____________________: 0x%04X\n", FSN);
    collection += String.format("PSN____________________: 0x%04X\n", PSN);
    collection += String.format("UOS ID_________________: %s\n", getOsID());
    collection += String.format("Dev Type_______________: %X\n", deviceType);
    collection += String.format("RM_____________________: 0x%04X\n", RM);
    collection += String.format("State__________________: %s\n", getUAIState());
    collection +=
        String.format("RETRY COUNTER__________: %d\n", retryCounterOSU);
    collection +=
        String.format("RETRY COUNTER__________: %d\n", retryCounterOSPatch);

    return collection;
  }

  /**
   * Method to map the OS ID to a string.
   * @return Mapped OS ID
   */
  private String getOsID() {

    String currState = "";

    if (OSU_1_ID == UOS_ID) {
      currState = "OSU1 (0x01)";
    } else if (OSU_2_ID == UOS_ID) {
      currState = "OSU2 (0x02)";
    } else if (OSU1_SELF_ID == UOS_ID) {
      currState = "OS SELF Update (0x11)";
    } else if (JCOP_ID == UOS_ID) {
      currState = "JCOP OS (0x5A)";
    }

    return currState;
  }

  /**
   * Method to map the current UAI state to a string.
   * @return Mapped UAI state
   */
  private String getUAIState() {
    String currState = "";

    if (STATE_IDLE == state) {
      currState = "IDLE";
    } else if (STATE_UAI_CCI_VALIDATED == state) {
      currState = "CCI_VALIDATED";
    } else if (STATE_UAI_JCI_VALIDATED == state) {
      currState = "JCI VALIDATED";
    } else if (STATE_UAI_ERROR == state) {
      currState = "UAI Error";
    } else {
      currState = "UAI Unknown state";
    }

    return currState;
  }
}

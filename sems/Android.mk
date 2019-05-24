LOCAL_PATH:= $(call my-dir)

########################################
# com.nxp.sems - library
########################################
include $(CLEAR_VARS)

LOCAL_MODULE_TAGS := optional
LOCAL_MODULE := com.nxp.sems
LOCAL_REQUIRED_MODULES := com.nxp.sems.xml
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
 # Install to vendor/frameworks
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR_JAVA_LIBRARIES)

LOCAL_SRC_FILES := \
        $(call all-Iaidl-files-under, com) \
        $(call all-java-files-under, com) \

LOCAL_CERTIFICATE := platform

include $(BUILD_JAVA_LIBRARY)
# ====  permissions ========================
include $(CLEAR_VARS)

LOCAL_MODULE := com.nxp.sems.xml

LOCAL_MODULE_TAGS := optional

LOCAL_MODULE_CLASS := ETC

# Install to /vendor/etc/permissions
LOCAL_MODULE_PATH := $(TARGET_OUT_VENDOR_ETC)/permissions

LOCAL_SRC_FILES := $(LOCAL_MODULE)

include $(BUILD_PREBUILT)

# the documentation
# ============================================================
include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
        $(call all-Iaidl-files-under, com) \
        $(call all-java-files-under, com) \
        $(call all-html-files-under, com) \

LOCAL_MODULE:= com.nxp.sems
LOCAL_JAVA_LIBRARIES:= com.nxp.sems
LOCAL_MODULE_CLASS := JAVA_LIBRARIES
LOCAL_DROIDDOC_USE_STANDARD_DOCLET := true

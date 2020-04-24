LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)


TARGET_ARCH_ABI := all
LOCAL_MODULE	:= jni_callback
LOCAL_CFLAGS    := -DDEV_NDK=1
# ref: https://stackoverflow.com/questions/7348997/android-ndk-how-to-let-gcc-to-use-additional-include-directories
LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)

LOCAL_SRC_FILES := \
	callback.cpp \
	utils.cpp \
	simproc.cpp

LOCAL_LDLIBS += -llog -ldl

include $(BUILD_SHARED_LIBRARY)


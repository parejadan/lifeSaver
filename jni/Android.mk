LOCAL_PATH := $(call my-dir)

#build for bottleneck
include $(CLEAR_VARS)
OPENCV_LIB_TYPE :=STATIC
include /home/daniel/opencv/OpenCV-2.4.9-android-sdk/sdk/native/jni/OpenCV.mk
LOCAL_MODULE     := bottleNeck
LOCAL_SRC_FILES  := jni_bottleNeck.cpp
include $(BUILD_SHARED_LIBRARY)

#build for normalization 
include $(CLEAR_VARS)
OPENCV_LIB_TYPE :=STATIC
include /home/daniel/opencv/OpenCV-2.4.9-android-sdk/sdk/native/jni/OpenCV.mk
LOCAL_MODULE     := normalize
LOCAL_SRC_FILES  := jni_normalize.cpp
include $(BUILD_SHARED_LIBRARY)

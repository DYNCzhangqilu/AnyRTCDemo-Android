APP_BUILD_SCRIPT := $(call my-dir)/toolchain/Android.mk
APP_MODULES := anyrtc-jni anyrtc

NDK_PATH := /cygdrive/c/Android/NDK/android-ndk-r9d
NDK_STL_INC := $(NDK_PATH)/sources/cxx-stl/gnu-libstdc++/4.8/include

APP_OPTIM        := release 
APP_CFLAGS       += -O3
#APP_STL := gnustl_shared
APP_STL := gnustl_static
NDK_TOOLCHAIN_VERSION = 4.8

APP_PLATFORM := android-14
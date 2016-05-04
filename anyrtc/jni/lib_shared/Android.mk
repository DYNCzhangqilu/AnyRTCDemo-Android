LOCAL_PATH := $(call my-dir)  
  
include $(CLEAR_VARS)  
LOCAL_MODULE := anyrtc    			# 模块名  
LOCAL_SRC_FILES := libanyrtc.so		# 模块的文件路径（相对于 LOCAL_PATH）  
  
include $(PREBUILT_SHARED_LIBRARY) 		# 注意这里不是 BUILD_SHARED_LIBRARY  
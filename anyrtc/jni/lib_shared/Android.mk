LOCAL_PATH := $(call my-dir)  
  
include $(CLEAR_VARS)  
LOCAL_MODULE := anyrtc    			# ģ����  
LOCAL_SRC_FILES := libanyrtc.so		# ģ����ļ�·��������� LOCAL_PATH��  
  
include $(PREBUILT_SHARED_LIBRARY) 		# ע�����ﲻ�� BUILD_SHARED_LIBRARY  
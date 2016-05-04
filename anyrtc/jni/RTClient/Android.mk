# Copyright (C) 2009 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.cpprg/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
################################################################
# Ucc Jni
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := rtclient
LOCAL_SRC_FILES := RTClient.cpp \
			SynClient.cpp \
			SyncMsgCrypt.cpp \
			XTcpClientImpl.cpp \
			XTcpTick.cpp 
			
LOCAL_SRC_FILES += ./webrtc/base/asyncfile.cc \
				./webrtc/base/asyncresolverinterface.cc \
				./webrtc/base/asyncsocket.cc \
				./webrtc/base/asynctcpsocket.cc \
				./webrtc/base/asyncudpsocket.cc \
				./webrtc/base/base64.cc \
				./webrtc/base/checks.cc \
				./webrtc/base/criticalsection.cc \
				./webrtc/base/event.cc \
				./webrtc/base/event_tracer.cc \
				./webrtc/base/ifaddrs-android.cc \
				./webrtc/base/ipaddress.cc \
				./webrtc/base/logging.cc \
				./webrtc/base/messagehandler.cc \
				./webrtc/base/messagequeue.cc \
				./webrtc/base/nethelpers.cc \
				./webrtc/base/physicalsocketserver.cc \
				./webrtc/base/platform_thread.cc \
				./webrtc/base/signalthread.cc \
				./webrtc/base/sigslot.cc \
				./webrtc/base/socketaddress.cc \
				./webrtc/base/stringencode.cc \
				./webrtc/base/thread.cc \
				./webrtc/base/timeutils.cc \
	
## 
## Widows (call host-path,/cygdrive/path/to/your/file/libstlport_shared.so) 	
#		   
#LOCAL_LDLIBS := -llog -lz 
LOCAL_CFLAGS := -std=gnu++11 -DPOSIX -D__UCLIBC__ -DWEBRTC_LINUX -DWEBRTC_ANDROID -DWEBRTC_POSIX -D__STDC_CONSTANT_MACROS -DNO_MAIN_THREAD_WRAPPING

LOCAL_C_INCLUDES += $(NDK_STL_INC) \
					$(LOCAL_PATH)/ \
					$(LOCAL_PATH)/openssl/include 
					
include $(BUILD_STATIC_LIBRARY)
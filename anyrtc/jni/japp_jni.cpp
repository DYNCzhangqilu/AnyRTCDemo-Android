#include "jrtc_app.h"
#include <stdio.h>
#include <stdlib.h>
#include "jni_helpers.h"
#include "jcontext.h"

#include "helpers_android.h"
#include "java_string.h"
#include "jni_helpers.h"
// Macro for native functions that can be found by way of jni-auto discovery.
// Note extern "C" is needed for "discovery" of native methods to work.
#define JOWW(rettype, name)                                             \
  extern "C" rettype JNIEXPORT JNICALL Java_org_anyrtc_jni_##name

//=================================================================
namespace {
JRtcApp* GetJApp(JNIEnv* jni, jobject j_app)
{
  jclass j_app_class = jni->GetObjectClass(j_app);
  jfieldID native_id =
      jni->GetFieldID(j_app_class, "fNativeAppId", "J");
  jlong j_p = jni->GetLongField(j_app, native_id);
  return reinterpret_cast<JRtcApp*>(j_p);
}
}

//=================================================================
//=================================================================
JOWW(jlong, JRtcApp_Create)(JNIEnv* jni, jclass, jobject j_obj)
{
	//JavaString jstrLogPath(jlogPath);
	//L_Init(jlogLevel, jstrLogPath.ToString8().c_str());

	JRtcApp* jApp = new JRtcApp(j_obj);
	return jlongFromPointer(jApp);
}

JOWW(void, JRtcApp_Destroy)(JNIEnv* jni, jobject j_app)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	jApp->Close();
	delete jApp;
}

JOWW(jint, JRtcApp_ConnectionStatus)(JNIEnv* jni, jobject j_app)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	return jApp->ConnectStatus();
}

JOWW(void, JRtcApp_Connect)(JNIEnv* jni, jobject j_app, jstring strSvrAddr, jint nSvrPort,
			jstring strDeveloperId, jstring strToken, jstring strAESKey,
			jstring strAppId)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrSvrAddr(strSvrAddr);
	JavaString jstrDeveloperId(strDeveloperId);
	JavaString jstrToken(strToken);
	JavaString jstrAESKey(strAESKey);
	JavaString jstrAppId(strAppId);

	jApp->Connect(jstrSvrAddr.ToString8().c_str(), nSvrPort, jstrDeveloperId.ToString8().c_str(), 
		jstrToken.ToString8().c_str(), jstrAESKey.ToString8().c_str(), jstrAppId.ToString8().c_str());
}

JOWW(void, JRtcApp_UserOptionJoin)(JNIEnv* jni, jobject j_app, jint jopt, jstring strAnyrtcId, jstring strUserData)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrAnyrtcId(strAnyrtcId);
	JavaString jstrUserData(strUserData);
	jApp->UserOptionJoin((UserOption)jopt, jstrAnyrtcId.ToString8().c_str(), jstrUserData.ToString8().c_str());
}

JOWW(void, JRtcApp_UserOptionNotify)(JNIEnv* jni, jobject j_app, jint jopt, jstring strAnyrtcId, jstring strMessage)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrAnyrtcId(strAnyrtcId);
	JavaString jstrMessage(strMessage);
	jApp->UserOptionNotify((UserOption)jopt, jstrAnyrtcId.ToString8().c_str(), jstrMessage.ToString8().c_str());
}

JOWW(void, JRtcApp_UserOptionLeave)(JNIEnv* jni, jobject j_app, jint jopt, jstring strAnyrtcId)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrAnyrtcId(strAnyrtcId);
	jApp->UserOptionLeave((UserOption)jopt, jstrAnyrtcId.ToString8().c_str());
}

JOWW(void, JRtcApp_Publish)(JNIEnv* jni, jobject j_app, jstring strType, 
			jint width, jint height, jint aBitrate, jint vBitrate)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrType(strType);
	jApp->Publish(jstrType.ToString8().c_str(), width, height, aBitrate, vBitrate);
}

JOWW(void, JRtcApp_Unpublish)(JNIEnv* jni, jobject j_app, jstring strPublishId)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrPublishId(strPublishId);
	jApp->Unpublish(jstrPublishId.ToString8().c_str());
}

JOWW(void, JRtcApp_Subscribe)(JNIEnv* jni, jobject j_app, jstring strPublishId)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrPublishId(strPublishId);
	jApp->Subscribe(jstrPublishId.ToString8().c_str());
}

JOWW(void, JRtcApp_Unsubscribe)(JNIEnv* jni, jobject j_app, jstring strSubscribeId)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrSubscribeId(strSubscribeId);
	jApp->Unsubscribe(jstrSubscribeId.ToString8().c_str());
}

JOWW(void, JRtcApp_Message)(JNIEnv* jni, jobject j_app, jstring strTo, 
			jstring strToSvrId, jstring strContent)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrTo(strTo);
	JavaString jstrToSvrId(strToSvrId);
	JavaString jstrContent(strContent);

	jApp->Message(jstrTo.ToString8().c_str(), jstrToSvrId.ToString8().c_str(), jstrContent.ToString8().c_str());
}

JOWW(void, JRtcApp_SendSdpInfo)(JNIEnv* jni, jobject j_app, jstring strChanId, jstring strJsep)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrChanId(strChanId);
	JavaString jstrJsep(strJsep);
	jApp->SendSdpInfo(jstrChanId.ToString8().c_str(), jstrJsep.ToString8().c_str());
}

JOWW(void, JRtcApp_SwitchVideoBits)(JNIEnv* jni, jobject j_app, jstring strSubscribeId, jint bitLevel)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	JavaString jstrSubscribeId(strSubscribeId);
	jApp->SwitchVideoBits(jstrSubscribeId.ToString8().c_str(), bitLevel);
}

JOWW(void, JRtcApp_Disconnect)(JNIEnv* jni, jobject j_app)
{
	JRtcApp* jApp = GetJApp(jni, j_app);
	jApp->Disconnect();
}
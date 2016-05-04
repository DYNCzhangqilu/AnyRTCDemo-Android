#include "jrtc_app.h"
#include "jni_helpers.h"
#include "jcontext.h"
#include "helpers_android.h"
#include "java_string.h"

JRtcApp::JRtcApp(jobject javaObj)
: m_jJavaObj(NULL)
, m_jClass(NULL)
{
	if(javaObj)
	{
		AttachThreadScoped ats(JContext::g_vm);
		m_jJavaObj = ats.env()->NewGlobalRef(javaObj);
		m_jClass = reinterpret_cast<jclass> (ats.env()->NewGlobalRef(ats.env()->GetObjectClass(m_jJavaObj)));
	}
}

JRtcApp::~JRtcApp(void)
{
	Close();
}

void JRtcApp::Close()
{
	if(m_jJavaObj)
	{
		AttachThreadScoped ats(JContext::g_vm);
		ats.env()->DeleteGlobalRef(m_jClass);
		m_jClass = NULL;
		ats.env()->DeleteGlobalRef(m_jJavaObj);
		m_jJavaObj = NULL;
	}
}

void JRtcApp::OnRtcConnect(int code, const std::string& strDyncId, const std::string& strServerId, const std::string&strSysConf) 
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcConnect callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcConnect", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszDyncId(strDyncId);
		JavaString jcszServerId(strServerId);
		JavaString jcszSysConf(strSysConf); 
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, code, jcszDyncId.Get(), jcszServerId.Get(), jcszSysConf.Get());
	}
}

void JRtcApp::OnRtcMessage(const std::string&strFromId, const std::string&strJsep) 
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcConnect callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcMessage", "(Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszFromId(strFromId);
		JavaString jcszJsep(strJsep);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszFromId.Get(), jcszJsep.Get());
	}
}

void JRtcApp::OnRtcPublish(const std::string&strResult, const std::string&strChannelId, const std::string&strDyncerId,
		const std::string&strRtmpUrl, const std::string&strHlsUrl, const std::string&strError)
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcPublish callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcPublish", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszResult(strResult);
		JavaString jcszChanId(strChannelId);
		JavaString jcszDyncerId(strDyncerId);
		JavaString jcszRtmpUrl(strRtmpUrl);
		JavaString jcszHlsUrl(strHlsUrl);
		JavaString jcszError(strError);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszResult.Get(), jcszChanId.Get(), jcszDyncerId.Get(), jcszRtmpUrl.Get(), jcszHlsUrl.Get(), jcszError.Get());
	}
}

void JRtcApp::OnRtcUnpublish(const std::string&strResult, const std::string&strChannelId)
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcUnpublish callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcUnpublish", "(Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszResult(strResult);
		JavaString jcszChanId(strChannelId);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszResult.Get(), jcszChanId.Get());
	}
}

void JRtcApp::OnRtcSubscribe(const std::string&strResult, const std::string&strChannelId, const std::string&strSubscribeId, const std::string&strJsep)
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcSubscribe callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcSubscribe", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszResult(strResult);
		JavaString jcszChanId(strChannelId);
		JavaString jcszSubId(strSubscribeId);
		JavaString jcszJsep(strJsep);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszResult.Get(), jcszChanId.Get(), jcszSubId.Get(), jcszJsep.Get());
	}
}

void JRtcApp::OnRtcUnsubscribe(const std::string&strResult, const std::string&strChannelId)
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcUnsubscribe callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcUnsubscribe", "(Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszResult(strResult);
		JavaString jcszChanId(strChannelId);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszResult.Get(), jcszChanId.Get());
	}
}

void JRtcApp::OnRtcSdpInfo(const std::string&strChanId, const std::string&strJsep)
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcSdpInfo callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcSdpInfo", "(Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszChanId(strChanId);
		JavaString jcszJsep(strJsep);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszChanId.Get(), jcszJsep.Get());
	}
}

void JRtcApp::OnRtcUserOptionJoin(const std::string&strAnyrtcId, const std::string&strResult, const std::string&strMessage)
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcJoinMeet callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcUserOptionJoin", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszAnyrtcId(strAnyrtcId);
		JavaString jcszResult(strResult);
		JavaString jcszMessage(strMessage);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszAnyrtcId.Get(), jcszResult.Get(), jcszMessage.Get());
	}
}

void JRtcApp::OnRtcUserOptionNotify(const std::string&strAnyrtcId, const std::string&strMessage)
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcNotifyMeet callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcUserOptionNotify", "(Ljava/lang/String;Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszAnyrtcId(strAnyrtcId);
		JavaString jcszMessage(strMessage);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszAnyrtcId.Get(), jcszMessage.Get());
	}
}

void JRtcApp::OnRtcUserOptionLeave(const std::string&strAnyrtcId)
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcLeaveMeet callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcUserOptionLeave", "(Ljava/lang/String;)V");
		// Callback with params
		JavaString jcszAnyrtcId(strAnyrtcId);
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId, jcszAnyrtcId.Get());
	}
};

void JRtcApp::OnRtcDisconnect() 
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcDisconnect callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcDisconnect", "()V");
		// Callback with no params
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId);
	}
}

void JRtcApp::OnRtcConnectFailed()
{
	AttachThreadScoped ats(JContext::g_vm);
	JNIEnv* jni = ats.env();
	{
		// Get OnRtcConnectFailed callback interface method id
		jmethodID j_callJavaMId = JGetMethodID(jni, m_jClass, "OnRtcConnectFailed", "()V");
		// Callback with no params
		jni->CallVoidMethod(m_jJavaObj, j_callJavaMId);
	}
}
#ifndef __JRTC_APP_H__
#define __JRTC_APP_H__
#include <jni.h>
#include "RTClient.h"

class JRtcApp : public RTClient
{
public:
	JRtcApp(jobject javaObj);
	virtual ~JRtcApp(void);

	void Close();
public:
	//* For RTClient
	virtual void OnRtcConnect(int code, const std::string& strDyncId, const std::string& strServerId, const std::string&strSysConf);
	virtual void OnRtcMessage(const std::string&strFromId, const std::string&strJsep);
	virtual void OnRtcPublish(const std::string&strResult, const std::string&strChannelId, const std::string&strDyncerId,
		const std::string&strRtmpUrl, const std::string&strHlsUrl, const std::string&strError);
	virtual void OnRtcUnpublish(const std::string&strResult, const std::string&strChannelId);
	virtual void OnRtcSubscribe(const std::string&strResult, const std::string&strChannelId, const std::string&strSubscribeId, const std::string&strJsep);
	virtual void OnRtcUnsubscribe(const std::string&strResult, const std::string&strChannelId);
	virtual void OnRtcSdpInfo(const std::string&strChannelId, const std::string&strJsep);
	virtual void OnRtcUserOptionJoin(const std::string&strAnyrtcId, const std::string&strResult, const std::string&strMessage);
	virtual void OnRtcUserOptionNotify(const std::string&strAnyrtcId, const std::string&strMessage);
	virtual void OnRtcUserOptionLeave(const std::string&strAnyrtcId);
	virtual void OnRtcDisconnect();
	virtual void OnRtcConnectFailed();

private:
	jobject     m_jJavaObj;
	jclass		m_jClass;
};


#endif	// __JRTC_APP_H__
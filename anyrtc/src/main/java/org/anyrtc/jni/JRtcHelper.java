package org.anyrtc.jni;

public interface JRtcHelper {
	public void OnRtcConnect(int code, String strDyncId, String strServerId, String strSysConf);

	public void OnRtcMessage(String fromId, String strJsep);
	
	public void OnRtcPublish(String strResult, String strChannelId, String strDyncerId, String strRtmpUrl, String strHlsUrl, String strError);
	
	public void OnRtcUnpublish(String strResult, String strChannelId);
	
	public void OnRtcSubscribe(String strResult, String strChannelId, String strSubscribeId, String strJsep);
	
	public void OnRtcUnsubscribe(String strResult, String strChannelId);
	
	public void OnRtcSdpInfo(String strChanId, String strJsep);

	public void OnRtcUserOptionJoin(String strAnyrtcId, String strResult, String strMessage);

	public void OnRtcUserOptionNotify(String strAnyrtcId, String strMessage);

	public void OnRtcUserOptionLeave(String strAnyrtcId);

	public void OnRtcDisconnect();

	public void OnRtcConnectFailed();
}

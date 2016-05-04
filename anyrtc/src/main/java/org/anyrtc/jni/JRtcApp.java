package org.anyrtc.jni;

/**
 * Copyright (c) 2015 DYNC. All Rights Reserved.
 * 
 * RTC 构造访问jni底层库的对象
 * 
 * @author DYNC
 * 
 */
public class JRtcApp {
	public static final int OPT_CALL = 0;
	public static final int OPT_MEET = 1;
	public static final int OPT_LIVE = 2;
	public static final int OPT_LINE = 3;
	/**
	 * 构造访问jni底层库的对象
	 */
	private final long fNativeAppId;

	public JRtcApp(JRtcHelper helper) {
		fNativeAppId = Create(helper);
	}

	private static native long Create(JRtcHelper helper);
	
	public native int ConnectionStatus();
	public native void Connect(String strSvrAddr, int nSvrPort,
			String strDeveloperId, String strToken, String strAESKey,
			String strAppId);

	public native void	UserOptionJoin(int opt, String strAnyrtcId, String strUserdata);
	public native void	UserOptionNotify(int opt, String strAnyrtcId, String Message);
	public native void	UserOptionLeave(int opt, String strAnyrtcId);

	public native void	Publish(String strAnyrtcId, int width, int height, int aBitrate, int vBitrate);
	public native void	Unpublish(String strPublishId);
	public native void	Subscribe(String strDyncerId);
	public native void	Unsubscribe(String strSubscribeId);
	public native void	Message(String strTo, String strToSvrId, String strContent);
	public native void	SendSdpInfo(String strChannelId, String jsep);
	public native void SwitchVideoBits(String strSubscribeId, int level);
	public native void Disconnect();

	/**
	 * 销毁APP
	 */
	public native void Destroy();
}

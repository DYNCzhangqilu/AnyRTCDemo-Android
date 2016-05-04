package org.anyrtc.common;

import org.webrtc.VideoTrack;

public interface M2MultierEvents {
	/** 发布成功
	 * @param strPublishId	实时流的ID
	 * @param strRtmpUrl	rtmp直播流的地址
	 * @param strHlsUrl		hls直播流的地址
     */
	public void OnRtcPublishOK(String strPublishId, String strRtmpUrl, String strHlsUrl);
	/** 发布失败
	 * @param nCode		失败的代码
	 * @param strErr	错误的具体原因
     */
	public void OnRtcPublishFailed(int nCode, String strErr);
	/** 发布通道关闭
	 */
	public void OnRtcPublishClosed();


	/** 订阅成功
	 * @param strPublishId	订阅的通道ID
     */
	public void OnRtcSubscribeOK(String strPublishId);
	/** 订阅失败
	 * @param strPublishId	订阅的通道ID
	 * @param nCode			失败的代码
	 * @param strErr		错误的具体原因
     */
	public void OnRtcSubscribeFailed(String strPublishId, int nCode, String strErr);
	/** 订阅通道关闭
	 * @param strPublishId	订阅的通道ID
     */
	public void OnRtcSubscribeClosed(String strPublishId);

}

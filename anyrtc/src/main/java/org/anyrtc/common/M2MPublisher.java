package org.anyrtc.common;

public class M2MPublisher {
	public enum StreamType {
		ST_RTC,		//* AnyRTC的实时流
		ST_LIVE,	//* RTMP&HLS的直播流
		ST_BOTH,	//* 以上二者均发布
	}
	public static class PublishParams {
		public boolean 		bEnableVideo = true;				//* 是否开启视频
		public boolean		bEnableRecord = false;				//* 是否录像(需要购买录像服务)
		public StreamType	eStreamType = StreamType.ST_RTC;	//* 如果是ST_LIVE或ST_BOTH 则 bEnableVideo会被强制设为true
	}
	public boolean bInit = false;
	public boolean bUseVideo = false;
	public int nWidth = 640;
	public int nHeight = 480;
	public int aBitrate = 100;
	public int vBitrate = 384;
	public String strChannelId = null;
	public String strPeerId = null;

	public void Clear() {
		bInit = false;
		bUseVideo = false;
		nWidth = 640;
		nHeight = 480;
		aBitrate = 100;
		vBitrate = 384;
		strChannelId = null;
		strPeerId = null;
	}
}

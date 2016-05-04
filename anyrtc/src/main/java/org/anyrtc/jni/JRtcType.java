package org.anyrtc.jni;

public class JRtcType {
	//* Connection Status
	public static final int NOT_CONNECTED = 0;
	public static final int RESOLVING = 1;
	public static final int CONNECTTING = 2;
	public static final int CONNECTED = 3;
	
	public static class JRtcVideoParam {
		public int mWidth;
		public int mHeight;
		public int mFramerate;
		public int mBitrate;
	}
}

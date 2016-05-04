package org.anyrtc.common;

public class M2MSubscriber {
	public boolean bSubscibed = false;
	public boolean bUseVideo = false;
	public String strPublishId = null;
	public String strChannelId = null;
	public String strPeerId = null;

	public int nBsNow, nBsBefore, nBsSum;
	public double nTsNow, nTsBefore, nTsSum;
	public int nBitrate;

	public void Clear() {
		bSubscibed = false;
		bUseVideo = false;
		strPublishId = null;
		strChannelId = null;
		strPeerId = null;
	}
}

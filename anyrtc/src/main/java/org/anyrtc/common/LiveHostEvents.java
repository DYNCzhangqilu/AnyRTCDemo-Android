package org.anyrtc.common;

/**
 * Created by Eric on 2016/3/15.
 */
public interface LiveHostEvents extends LiveEvents{
    /** Live infomation
     * @param strUrl            Rtmp/Hls url
     * @param strSessionId      SessionId
     */
    public void OnRtcLiveInfomation(String strUrl, String strSessionId);

    /** Guest want to line with you
     * @param strPeerId     Peer's ID
     * @param strUserName   Peer's user name
     * @param strBrief      A brief
     */
    public void OnRtcLiveApplyLine(String strPeerId, String strUserName, String strBrief);

    /** Guest cancel line apply
     * @param strPeerId     Peer's ID
     * @param strUserName   Peer's user name
     */
    public void OnRtcLiveCancelLine(String strPeerId, String strUserName);
}

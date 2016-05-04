package org.anyrtc.common;

import org.anyrtc.AnyRTC;

import java.util.List;

/**
 * Created by Eric on 2016/2/24.
 */
public interface LiveEvents {
    /** Join live OK
     * @param strAnyrtcId
     */
    public void OnRtcJoinLiveOK(String strAnyrtcId);

    /**
     * All member count in this live.
     * @param nTotal
     */
    public void OnRtcLiveNumberOfMember(int nTotal);
    /** Got online member
     * @param strCustomId
     * @param strCustomName
     */
    public void OnRtcLiveMemberOnline(String strCustomId, String strCustomName);

    /**
     * Got offline member
     * @param strCustomId
     */
    public void OnRtcLiveMemberOffline(String strCustomId);

    /**
     * Recv User message
     * @param strUserName
     * @param strNickName
     * @param strContent
     */
    public void OnRtcLiveUserMsg(String strUserName, String strNickName, String strContent);

    /** Could or not to line hoster
     * @param enable yes/no
     */
    public void OnRtcLiveEnableLine(boolean enable);

    /** Join live failed
     * @param strAnyrtcId
     * @param code
     * @param strReason
     */
    public void OnRtcJoinLiveFailed(String strAnyrtcId, AnyRTC.AnyRTCErrorCode code, String strReason);

    /** Leave live
     * @param code
     */
    public void OnRtcLeaveLive(int code);


}

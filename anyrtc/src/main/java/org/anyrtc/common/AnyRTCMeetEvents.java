package org.anyrtc.common;

import org.anyrtc.AnyRTC;

/**
 * Created by Eric on 2016/2/24.
 */
public interface AnyRTCMeetEvents {
    /**Join meet OK
     * @param strAnyrtcId
     */
    public void OnRtcJoinMeetOK(String strAnyrtcId);

    /** Join meet Failed
     * @param strAnyrtcId
     * @param code
     * @param strReason
     */
    public void OnRtcJoinMeetFailed(String strAnyrtcId, AnyRTC.AnyRTCErrorCode code, String strReason);

    /** Leave meet
     * @param code
     */
    public void OnRtcLeaveMeet(int code);


}

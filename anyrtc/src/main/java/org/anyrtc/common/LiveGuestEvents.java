package org.anyrtc.common;

/**
 * Created by Eric on 2016/3/15.
 */
public interface LiveGuestEvents extends LiveEvents{
    /** Apply chat result
     * @param accept
     */
    public void OnRtcLiveApplyLineResult(boolean accept);

    /** Line hang up
     *
     */
    public void OnRtcLiveLineHangup();
}

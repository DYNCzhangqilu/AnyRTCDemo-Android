package org.anyrtc.common;

import org.webrtc.EglBase;
import org.webrtc.VideoTrack;

/**
 * Created by Eric on 2016/3/5.
 */
public interface AnyRTCViewEvents {
    /** Get EglBase
     * return EglBase;
     */
    public EglBase GetEglBase();

    /** Open local  Renderer
     * @param localTrack
     */
    public void OnRtcOpenLocalRender(VideoTrack localTrack);

    /** Close local  Renderer
     */
    public void OnRtcRemoveLocalRender();


    /** Open  remote  Renderer
     * @param channelId
     * @param remoteTrack
     */

    public void OnRtcOpenRemoteRender(String channelId, VideoTrack remoteTrack);

    /** Close  remote  Renderer
     * @param channelId
     */
    public void OnRtcRemoveRemoteRender(String channelId);

    /** Remote audio video status
     * @param channelId
     * @param audioEnable
     * @param videoEnable
     */
    public void OnRtcRemoteAVStatus(String channelId, boolean audioEnable, boolean videoEnable);
}

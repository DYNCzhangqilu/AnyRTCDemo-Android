package org.anyrtc;

import android.app.Activity;

import org.anyrtc.common.AnyRTCViewEvents;
import org.anyrtc.common.LiveHostEvents;
import org.anyrtc.jni.JRtcApp;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Eric on 2016/3/15.
 */
public class AnyRTCLiveHostKit extends AnyRTCLive {
    private LiveHostEvents mEvents;
    private boolean mLined = false;
    private String mLinedPeerId = null;

    /** AnyRTCLiveHostKit constructor
     * @param context       Android's Context
     * @param liveEvents    Live events for callback
     * @param viewEvents    View events for callback
     */
    public AnyRTCLiveHostKit(Activity context, LiveHostEvents liveEvents, AnyRTCViewEvents viewEvents) {
        super(context, liveEvents, true);
        mEvents = liveEvents;
        mViewEvents = viewEvents;

        if(mViewEvents != null) {
            peerClients.setEglBase(mViewEvents.GetEglBase(), true);
        }
    }

    /** Join live as an anchor
     * @param anyrtcID      Live ID
     * @param customID      Costom userID for thirdparty developer
     * @param customName    Costom userName for thirdparty developer
     * @param bCallin       Could be call in
     * @param bGetMemList   Get all member list
     * @return              true:success	false:failed
     */
    public boolean Join(String anyrtcID, String customID, String customName, boolean bCallin, boolean bGetMemList) {
        return super.JoinLive(anyrtcID, customID, customName, bCallin, bGetMemList);
    }

    /** Anchor enable/disable line function
     * @param enable    yes/no
     * @return			true:success	false:failed
     */
    public boolean SetLineEnable(boolean enable) {
        if (mJoined && mAnyrtcId != null) {
            if(mCallIn != enable) {
                mCallIn = enable;
                JSONObject jsonData = new JSONObject();
                try {
                    jsonData.put("CMD", "LiveSetting");
                    jsonData.put("EnableCallIn", mCallIn);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mRtcApp.UserOptionNotify(JRtcApp.OPT_LIVE, mAnyrtcId, jsonData.toString());
            }
            return true;
        }
        return false;
    }

    /** Anchor accept line
     * @param
     */
    public boolean AcceptApplyLine(String strPeerId){
        if (mJoined && mAnyrtcId != null && mCallIn) {
            if(mLined)
                return false;
            mLined = true;
            mLinedPeerId = strPeerId;
            JSONObject jsonData = new JSONObject();
            try {
                jsonData.put("CMD", "AcceptApply");
                jsonData.put("LivePeerID", strPeerId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mRtcApp.UserOptionNotify(JRtcApp.OPT_LIVE, mAnyrtcId, jsonData.toString());
            return true;
        }

        return false;
    }

    /** Anchor reject line
     * @param strPeerId Peer's ID
     */
    public void RejectApplyLine(String strPeerId){
        if (mJoined && mAnyrtcId != null) {
            JSONObject jsonData = new JSONObject();
            try {
                jsonData.put("CMD", "RejectApply");
                jsonData.put("LivePeerID", strPeerId);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mRtcApp.UserOptionNotify(JRtcApp.OPT_LIVE, mAnyrtcId, jsonData.toString());
        }
    }

    /** Anchor hang up line
     * @param strPeerId Peer's ID
     */
    public void HangupLive(String strPeerId) {
        if(!mLined || strPeerId == null || strPeerId.length() == 0)
            return;
        if (mJoined && mAnyrtcId != null) {
            if(mLinedPeerId.equals(strPeerId)) {
                mLined = false;
                mLinedPeerId = null;

                JSONObject jsonData = new JSONObject();
                try {
                    jsonData.put("CMD", "HangupLine");
                    jsonData.put("LivePeerID", strPeerId);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mRtcApp.UserOptionNotify(JRtcApp.OPT_LIVE, mAnyrtcId, jsonData.toString());
            }
        }
    }

    public void OnRtcUserOptionNotify(String strCmd, JSONObject json)
    {
        if(strCmd.equals("ApplyChat")) {
            try {
                mEvents.OnRtcLiveApplyLine(json.getString("LivePeerID"), json.getString("UserName"), json.getString("Brief"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if(strCmd.equals("CancelChat")){
            try {
                String strPeerId = json.getString("LivePeerID");
                if(mLined && strPeerId.equals(mLinedPeerId)) {
                    mLined = false;
                    mLinedPeerId = null;
                }
                mEvents.OnRtcLiveCancelLine(strPeerId, json.getString("UserName"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if(strCmd.equals("LiveSetting")) {
            try {
                boolean bCallIn = json.getBoolean("EnableCallIn");
                mEvents.OnRtcLiveEnableLine(bCallIn);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

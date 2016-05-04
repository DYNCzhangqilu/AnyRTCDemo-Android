package org.anyrtc;

import android.app.Activity;

import org.anyrtc.common.AnyRTCViewEvents;
import org.anyrtc.common.LiveGuestEvents;
import org.anyrtc.jni.JRtcApp;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Eric on 2016/3/15.
 */
public class AnyRTCLiveGuestKit extends AnyRTCLive {
    private LiveGuestEvents mEvents;
    private boolean mApplyChat;
    private boolean mLined;

    /** AnyRTCLiveGuestKit constructor
     * @param context       Android's Context
     * @param liveEvents    Live events for callback
     * @param viewEvents    View events for callback
     */
    public AnyRTCLiveGuestKit(Activity context, LiveGuestEvents liveEvents, AnyRTCViewEvents viewEvents) {
        super(context, liveEvents, false);
        mEvents = liveEvents;
        mViewEvents = viewEvents;

        if(mViewEvents != null) {
            peerClients.setEglBase(mViewEvents.GetEglBase(), false);
        }
    }
    /** Join live as an guest
     * @param anyrtcID      Live ID
     * @param customID      Costom userID for thirdparty developer
     * @param customName    Costom userName for thirdparty developer
     * @param bGetMemList   Get all member list
     * @return              true:success	false:failed
     */
    public boolean Join(String anyrtcID, String customID, String customName, boolean bGetMemList) {
        return super.JoinLive(anyrtcID, customID, customName, false, bGetMemList);
    }

    public boolean SendUserMsg(String strContent) {
        if(strContent == null || strContent.length() == 0)
            return false;
        if (mJoined && mAnyrtcId != null) {
            JSONObject jsonData = new JSONObject();
            try {
                jsonData.put("CMD", "UserMsg");
                jsonData.put("UserName", mUserId);
                jsonData.put("NickName", mUserName);
                jsonData.put("Content", strContent);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mRtcApp.UserOptionNotify(JRtcApp.OPT_LIVE, mAnyrtcId, jsonData.toString());
            return true;
        }
        return false;
    }

    /** Apply line to anchor
     * @param strBrief  Brief, max characters is 36
     */
    public boolean ApplyLine2Anchor(String strBrief) {
        if(!mCallIn) {
            return false;
        }
        if(mLined) {
            return false;
        }
        if (mJoined && mAnyrtcId != null) {
            if(!mApplyChat) {
                mApplyChat = true;
                if (strBrief == null)
                    strBrief = "";
                JSONObject jsonData = new JSONObject();
                try {
                    jsonData.put("CMD", "ApplyChat");
                    jsonData.put("Brief", strBrief);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mRtcApp.UserOptionNotify(JRtcApp.OPT_LIVE, mAnyrtcId, jsonData.toString());
                return true;
            }
        }
        return false;
    }
    /** Cancel line
     * @param
     */
    public boolean CancelLine() {
        UnPublishInternal();
        if (mJoined && mAnyrtcId != null) {
            if(mApplyChat || mLined) {
                mApplyChat = false;
                mLined = false;
                JSONObject jsonData = new JSONObject();
                try {
                    jsonData.put("CMD", "CancelChat");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mRtcApp.UserOptionNotify(JRtcApp.OPT_LIVE, mAnyrtcId, jsonData.toString());
                return true;
            }
        }
        return false;
    }

    public void OnRtcUserOptionNotify(String strCmd, JSONObject json) {
        if(strCmd.equals("AcceptApply")) {
            if(!mLined) {
                mApplyChat = false;
                mLined = true;
                if (!mPublisher.bInit) {
                    mPublisher.bInit = true;
                    mPublisher.bUseVideo = true;
                    PublishInternal();
                }
                mEvents.OnRtcLiveApplyLineResult(true);
            }
        } else if(strCmd.equals("RejectApply")) {
            if(mApplyChat) {
                mApplyChat = false;
                mEvents.OnRtcLiveApplyLineResult(false);
            }
        } else if(strCmd.equals("HangupLine")) {
            if (mLined) {
                mLined = false;
                UnPublishInternal();
                mEvents.OnRtcLiveLineHangup();
            }
        } else if(strCmd.equals("LiveSetting")) {
            try {
                boolean bCallIn = json.getBoolean("EnableCallIn");
                if(!bCallIn) {
                    if(mApplyChat) {
                        mApplyChat = false;
                        mEvents.OnRtcLiveApplyLineResult(false);
                    }
                }
                if(mCallIn != bCallIn) {
                    mCallIn = bCallIn;
                    mEvents.OnRtcLiveEnableLine(mCallIn);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}

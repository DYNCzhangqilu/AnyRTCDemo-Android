package org.anyrtc.dync;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.anyrtc.AnyRTC;
import org.anyrtc.AnyRTCMeetKit;
import org.anyrtc.common.AnyRTCMeetEvents;

/**
 * Created by Eric on 2016/1/2.
 */
public class AnyRTCMeetActivity extends Activity implements AnyRTCMeetEvents {
    // Local preview screen position after call is connected.
    // Local preview screen position before call is connected.

    private TextView mTxtPubId;
    private Button mBtnVoice;
    private Button mBtnCamera;
    private Button mBtnCameraSwitch;

    private AnyRTCMeetKit mRtclient;
    private String mPublishId = "";
    Boolean iceConnected = false;
    Boolean isVoice = true;
    Boolean isCamera = true;
    Boolean isCameraSwitch = true;

    private AnyRTCViews mAnyrtcViews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meet);
        // Create UI controls.
        {
            mTxtPubId = (TextView) findViewById(R.id.txt_pub_id);
            mBtnVoice = (Button) findViewById(R.id.btn_voice);
            mBtnCamera = (Button) findViewById(R.id.btn_camera);
            mBtnCameraSwitch = (Button) findViewById(R.id.btn_camera_switch);
        }

        mAnyrtcViews = new AnyRTCViews((RelativeLayout) findViewById(R.id.relaytive_videos));
        mRtclient = new AnyRTCMeetKit(this, this, mAnyrtcViews);

        String roomid = getIntent().getStringExtra("ROOMID");
        if (roomid.equals("")) {
            mRtclient.Join("800000000014");//这里对应着你所申请的AnyRTC ID
        }else {
            mRtclient.Join(roomid);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mRtclient.OnResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRtclient.OnPause();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        {//* Release RtcClient
            mRtclient.Destroy();
            mRtclient = null;
        }
    }

    /**
     * 屏幕旋转时调用此方法
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        }
    }

    public void OnBtnClicked(View btn) {
        switch (btn.getId()) {
            case R.id.btn_voice:
                if (isVoice) {
                    mRtclient.SetLocalAudioEnable(false);
                    mBtnVoice.setBackgroundResource(R.drawable.btn_voice_off);
                }else {
                    mRtclient.SetLocalAudioEnable(true);
                    mBtnVoice.setBackgroundResource(R.drawable.btn_voice_on);
                }
                isVoice = !isVoice;
                break;
            case R.id.btn_camera:
                if (isCamera){
                    mRtclient.SetLocalVideoEnable(false);
                    mBtnCamera.setBackgroundResource(R.drawable.btn_camera_off_select);
                }else {
                    mRtclient.SetLocalVideoEnable(true);
                    mBtnCamera.setBackgroundResource(R.drawable.btn_camera_on);
                }
                isCamera = !isCamera;
                break;
            case R.id.btn_camera_switch:
                mRtclient.SwitchCamera();
                break;
            case R.id.btn_leave:
                mRtclient.Leave();
                AnyRTCMeetActivity.this.finish();
                break;
//            case R.id.btn_switch_room:
//                mRtclient.SwitchRoom("800000000004");
//                break;
        }

    }

    /**
     * Implements for AnyRTCMeetEvents.
     */
    @Override
    public void OnRtcJoinMeetOK(String strAnyrtcId) {
        Log.e("TAG", "OnRtcJoinMeetOK: " + strAnyrtcId);
    }

    @Override
    public void OnRtcJoinMeetFailed(String strAnyrtcId, AnyRTC.AnyRTCErrorCode code, String strReason) {
        Log.e("TAG", "OnRtcJoinMeetFailed: "+code);
    }

    @Override
    public void OnRtcLeaveMeet(int code) {

    }

}

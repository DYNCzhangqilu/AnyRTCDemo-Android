package org.anyrtc.dync;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.anyrtc.AnyRTC;
import org.anyrtc.AnyrtcM2Mutlier;
import org.anyrtc.common.M2MPublisher;
import org.anyrtc.common.M2MultierEvents;

/**
 * Created by Eric on 2016/1/2.
 */
public class M2MActivity extends Activity implements M2MultierEvents {
    // Local preview screen position after call is connected.
    // Local preview screen position before call is connected.

    private Button mBtnPublish;
    private TextView mTxtPubId;

    private AnyrtcM2Mutlier mRtclient;
    private String mPublishId = "";
    Boolean iceConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_m2m);
        // Create UI controls.
        {
            mTxtPubId = (TextView) findViewById(R.id.txt_pub_id);
            mBtnPublish = (Button) findViewById(R.id.btn_pub_unpub);
        }

        AnyRTC.InitAnyRTCWithAppKey("mzw0001", "defq34hj92mxxjhaxxgjfdqi1s332dd", "d74TcmQDMB5nWx9zfJ5al7JdEg3XwySwCkhdB9lvnd1", "org.dync.app");
        mRtclient = new AnyrtcM2Mutlier(this, this);

        {
            M2MPublisher.PublishParams params = new M2MPublisher.PublishParams();
            params.bEnableVideo = true;
            params.eStreamType = M2MPublisher.StreamType.ST_RTC;
            if(mRtclient.Publish(params)) {
                //button.setText("关闭");
            }
        }
        //mRtclient.InitVideoView((GLSurfaceView) findViewById(R.id.glview_video));
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

    public void OnBtnClicked(View btn) {
        Button button = (Button)btn;
        if(btn.getId() == R.id.btn_pub_unpub) {
            if(button.getText().equals("发布")) {
                M2MPublisher.PublishParams params = new M2MPublisher.PublishParams();
                params.bEnableVideo = true;
                params.eStreamType = M2MPublisher.StreamType.ST_RTC;
                if(mRtclient.Publish(params)) {
                    button.setText("关闭");
                }
            } else {
                button.setText("发布");
                mTxtPubId.setText("");
                mRtclient.UnPublish();
            }

        } else if(btn.getId() == R.id.btn_sub_0) {
            if(button.getText().equals("订阅")) {
                if(mRtclient.Subscribe(mPublishId, true)) {
                    button.setText("取消");
                }
            } else {
                mRtclient.UnSubscribe(mPublishId);
                button.setText("订阅");
            }

        } else if(btn.getId() == R.id.btn_sub_1) {
            if(button.getText().equals("订阅")) {
                if(mRtclient.Subscribe("qB7ZTHefPDyb8Vdl6318T3YP", true)) {
                    button.setText("取消");
                }
            } else {
                mRtclient.UnSubscribe("qB7ZTHefPDyb8Vdl6318T3YP");
                button.setText("订阅");
            }
        } else if(btn.getId() == R.id.btn_sub_2) {
            if(button.getText().equals("订阅")) {
                if(mRtclient.Subscribe("LMG646XnDiQEyUDnDT6kSier", true)) {
                    button.setText("取消");
                }
            } else {
                mRtclient.UnSubscribe("LMG646XnDiQEyUDnDT6kSier");
                button.setText("订阅");
            }
        }
    }


    /**
     * Implements for M2MultierEvents.
     */
    @Override
    public void OnRtcPublishOK(String publishId, String rtmpUrl, String hlsUrl) {
        // TODO Auto-generated method stub
        mPublishId = publishId;
        Toast.makeText(this, "PublishOK id: " + publishId, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void OnRtcPublishFailed(int code, String errInfo) {
        // TODO Auto-generated method stub
        mBtnPublish.setText("发布");
        mTxtPubId.setText("");
        new AlertDialog.Builder(this)
                .setTitle("发布视频通道失败!")
                .setMessage(String.format("Err(%d): %s", code, errInfo))
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void OnRtcPublishClosed() {
        // TODO Auto-generated method stub

    }

    @Override
    public void OnRtcSubscribeOK(String publishId) {
        // TODO Auto-generated method stub
        iceConnected = true;


    }

    @Override
    public void OnRtcSubscribeFailed(String publishId, int code, String errInfo) {
        // TODO Auto-generated method stub

    }

    @Override
    public void OnRtcSubscribeClosed(String publishId) {
        // TODO Auto-generated method stub

    }

}

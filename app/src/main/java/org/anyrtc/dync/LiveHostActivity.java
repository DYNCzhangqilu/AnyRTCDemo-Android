package org.anyrtc.dync;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.anyrtc.AnyRTC;
import org.anyrtc.AnyRTCLiveHostKit;
import org.anyrtc.common.LiveHostEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eric on 2016/1/2.
 */
public class LiveHostActivity extends Activity implements LiveHostEvents {
    private Button mBtnPublish;
    private TextView mTxtPubId;
    private LinearLayout mLinearView;
    private List<LinearLayout> viewList;
    private TextView mTvOnlineNum;

    private AnyRTCLiveHostKit mRtclient;
    private String mPublishId = "";
    private String mLineID = null;
    Boolean iceConnected = false;

    private LiveAnyRTCViews mAnyrtcViews;
    private String mAnyrtcID;
    private String mUserName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_host);
        // Create UI controls.
        {
            mTxtPubId = (TextView) findViewById(R.id.txt_pub_id);
            mBtnPublish = (Button) findViewById(R.id.btn_pub_unpub);
            mLinearView = (LinearLayout) findViewById(R.id.linear_view);
            mTvOnlineNum = (TextView) findViewById(R.id.tv_online_num);

            mTvOnlineNum.setText("0");
            viewList = new ArrayList<LinearLayout>();
        }

        mAnyrtcViews = new LiveAnyRTCViews((RelativeLayout) findViewById(R.id.relaytive_videos));
        mRtclient = new AnyRTCLiveHostKit(this, this, mAnyrtcViews);

        Intent i = getIntent();
        mAnyrtcID = i.getStringExtra("AnyrtcID");
        mUserName = i.getStringExtra("UserName");
        mRtclient.Join(mAnyrtcID, mUserName, "I_Hoster", true, false);//980988 //800000000014
        mAnyrtcViews.setonTouchLocalRender(new LiveAnyRTCViews.OnTouchLocalRender() {
            @Override
            public void closeLocalRender() {
//                Toast.makeText(LiveGuestActivity.this, "不允许关闭自己的像",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void closeRemoteRender() {
                showDialog();
            }
        });
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
            mRtclient.Leave();
            mRtclient = null;
        }
    }

    public void OnBtnClicked(View btn) {
        switch (btn.getId()) {
            case R.id.btn_leave:
                LiveHostActivity.this.finish();
                break;
        }
    }

    /**
     * Implements for LiveEvents.
     */
    @Override
    public void OnRtcJoinLiveOK(String strAnyrtcId) {
        Log.e("TAG", "Host--OnRtcJoinLiveOK: " + strAnyrtcId);
    }

    @Override
    public void OnRtcLiveNumberOfMember(int nTotal) {
        Log.e("TAG", "Host--OnRtcLiveNumberOfMember: " + nTotal);
        mTvOnlineNum.setText(nTotal + "");
    }

    @Override
    public void OnRtcLiveMemberOnline(String strCustomId, String strCustomName) {

    }

    @Override
    public void OnRtcLiveMemberOffline(String strCustomId) {

    }

    @Override
    public void OnRtcLiveInfomation(String strUrl, String strSessionId) {
        Log.e("RtcLiveInfomation", String.format("http://%s#%s", strUrl, strSessionId));
    }

    @Override
    public void OnRtcLiveUserMsg(String strUserName, String strNickName, String strContent) {
//        Toast.makeText(this, String.format("%s(%s): %s", strUserName, strNickName, strContent), Toast.LENGTH_SHORT).show();
        addAutoView(strUserName, strContent);
    }

    @Override
    public void OnRtcLiveEnableLine(boolean enable) {

    }

    @Override
    public void OnRtcJoinLiveFailed(String strAnyrtcId, AnyRTC.AnyRTCErrorCode code, String strReason) {
        AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setTitle("直播出错!");
        dlg.setMessage("原因: " + strReason);
        dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LiveHostActivity.this.finish();
            }
        });
        dlg.show();
    }

    @Override
    public void OnRtcLiveApplyLine(String strPeerId, String strUserName, String strBrief) {
        showDialog(strPeerId, strUserName, strBrief);
    }

    @Override
    public void OnRtcLiveCancelLine(String strPeerId, String strUserName) {

    }

    @Override
    public void OnRtcLeaveLive(int code) {
        if (code != 0) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("直播被系统强制停止了!");
            dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LiveHostActivity.this.finish();
                }
            });
            dlg.setCancelable(false);
            dlg.show();
        }
    }

    public void addAutoView(String name, String msg) {
        MoveDownView();

        final LinearLayout showView = (LinearLayout) View.inflate(this, R.layout.text_view, null);
        TextView tvSendName = (TextView) showView.findViewById(R.id.tv_send_name);
        TextView tvChatContent = (TextView) showView.findViewById(R.id.tv_chat_content);
        showView.setBackgroundColor(getResources().getColor(R.color.transparent));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);

        showView.setLayoutParams(params);
        showView.postDelayed(new Runnable() {
            @Override
            public void run() {
                alphaAnimation(showView, viewList);
            }
        }, 1000);

        viewList.add(showView);
        mLinearView.addView(showView);
        tvSendName.setText(name);
        tvChatContent.setText(msg);

    }

    private void alphaAnimation(final View view,
                                final List<LinearLayout> viewList) {
        view.clearAnimation();
        ObjectAnimator anim = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                viewList.remove(view);
            }
        });
        anim.setDuration(10000).start();
    }


    private void MoveDownView() {
        int topMargin = 0;
        for (int i = 0; i < viewList.size(); i++) {
            LinearLayout linearLayout = viewList.get(i);
            topMargin = 10;
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) linearLayout.getLayoutParams();
            layoutParams.topMargin += topMargin;
            linearLayout.setLayoutParams(layoutParams);
        }
    }

    /**
     * 提示弹出框
     */
    private void showDialog() {

        String title = "";
        String msg = "是否断开连接";
        String ok = "确定";
        String cancel = "取消";
        ConfirmDialog mDialog = new ConfirmDialog(this, title, msg, ok, cancel, new ConfirmDialog.OnDialogButtonClickListener() {

            @Override
            public void onOkClick(Dialog dialog, View v) {// 前往
                if (mLineID != null) {
                    mRtclient.HangupLive(mLineID);
                    mLineID = null;
                }
            }

            @Override
            public void onCancelClick(Dialog dialog, View v) {// 拒绝

            }

        });
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable());//解决android5.0版本以后背景不透明
        mDialog.show();
        mDialog.setCancelable(false);// 设置dialog不能够点击它 区域以外的区域而销毁它自己

    }

    /**
     * 提示弹出框
     *
     * @param strPeerId
     * @param strUserName
     * @param strBrief
     */
    private void showDialog(final String strPeerId, final String strUserName, final String strBrief) {

        String title = "粉丝请求连线";
        String msg = "请求人：" + strUserName;
        String ok = "接受";
        String cancel = "拒绝";
        ConfirmDialog mDialog = new ConfirmDialog(this, title, msg, ok, cancel, new ConfirmDialog.OnDialogButtonClickListener() {

            @Override
            public void onOkClick(Dialog dialog, View v) {// 主播只能连线一人
                if (mLineID != null) {
                    mRtclient.HangupLive(mLineID);
                    mLineID = null;
                }
                if (mLineID == null) {
                    if (mRtclient.AcceptApplyLine(strPeerId)) {
                        mLineID = strPeerId;
                    }
                }
            }

            @Override
            public void onCancelClick(Dialog dialog, View v) {// 拒绝
                mRtclient.RejectApplyLine(strPeerId);
            }

        });
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable());//解决android5.0版本以后背景不透明
        mDialog.show();
        mDialog.setCancelable(false);// 设置dialog不能够点击它 区域以外的区域而销毁它自己

    }

}

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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.anyrtc.AnyRTC;
import org.anyrtc.AnyRTCLiveGuestKit;
import org.anyrtc.common.LiveGuestEvents;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Eric on 2016/3/15.
 */
public class LiveGuestActivity extends Activity implements LiveGuestEvents {
    private AnyRTCLiveGuestKit mRtclient;
    private LiveAnyRTCViews mAnyrtcViews;
    private Button mBtnApply;
    private RelativeLayout mChatPanel;
    private EditText mChatMessage;
    private Button mBtnChatSend;
    private TextView mTvOnlineNum;
    private String mAnyrtcID;
    private String mUserName;
    private LinearLayout mLinearView;
    private List<LinearLayout> viewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_guest);

        {
            mBtnApply = (Button) findViewById(R.id.btn_apply);
            mChatPanel = (RelativeLayout) findViewById(R.id.rl_chat_panel);
            mChatMessage = (EditText) findViewById(R.id.et_chat_msg);
            mBtnChatSend = (Button) findViewById(R.id.btn_chat_send);
            mLinearView = (LinearLayout) findViewById(R.id.linear_view);
            mTvOnlineNum = (TextView) findViewById(R.id.tv_online_num);

            mTvOnlineNum.setText("0");
            viewList = new ArrayList<LinearLayout>();
        }
        mAnyrtcViews = new LiveAnyRTCViews((RelativeLayout) findViewById(R.id.relaytive_videos));
        mRtclient = new AnyRTCLiveGuestKit(this, this, mAnyrtcViews);

        Intent i = getIntent();
        mAnyrtcID = i.getStringExtra("AnyrtcID");
        mUserName = i.getStringExtra("UserName");
        mRtclient.Join(mAnyrtcID, mUserName, "I*Guester", false);//980988 //800000000014
        mAnyrtcViews.setonTouchLocalRender(new LiveAnyRTCViews.OnTouchLocalRender() {
            @Override
            public void closeLocalRender() {
                showDialog();
            }

            @Override
            public void closeRemoteRender() {
//                Toast.makeText(LiveGuestActivity.this, "不允许关闭别人的像",Toast.LENGTH_SHORT).show();
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
            case R.id.btn_apply:
                mRtclient.ApplyLine2Anchor("Hello.");
                break;
            case R.id.btn_leave:
                LiveGuestActivity.this.finish();
                break;
            case R.id.btn_chat_send:
                String msg = mChatMessage.getText().toString();
                mRtclient.SendUserMsg(msg);
                addAutoView(mUserName, msg);
                mChatMessage.setText("");
                break;
        }
    }

    @Override
    public void OnRtcJoinLiveOK(String strAnyrtcId) {
        Log.e("TAG", "Guest--OnRtcJoinLiveOK: " + strAnyrtcId);
    }

    @Override
    public void OnRtcLiveNumberOfMember(int nTotal) {
        Log.e("TAG", "Guest--OnRtcLiveNumberOfMember: " + nTotal);
        mTvOnlineNum.setText(nTotal + "");
    }

    @Override
    public void OnRtcLiveMemberOnline(String strCustomId, String strCustomName) {

    }

    @Override
    public void OnRtcLiveMemberOffline(String strCustomId) {

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

    }

    @Override
    public void OnRtcLeaveLive(int code) {
        if (code != 0) {
            AlertDialog.Builder dlg = new AlertDialog.Builder(this);
            dlg.setTitle("直播结束了!");
            dlg.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    LiveGuestActivity.this.finish();
                }
            });
            dlg.setCancelable(false);
            dlg.show();
        }
    }

    @Override
    public void OnRtcLiveApplyLineResult(boolean accept) {
        if (accept) {
            mBtnApply.setVisibility(View.GONE);
        } else {
            mBtnApply.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void OnRtcLiveLineHangup() {
        mBtnApply.setVisibility(View.VISIBLE);
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

    private void alphaAnimation(final View view, final List<LinearLayout> viewList) {
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
                mRtclient.CancelLine();
                mBtnApply.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelClick(Dialog dialog, View v) {// 拒绝

            }

        });
        mDialog.getWindow().setBackgroundDrawable(new ColorDrawable());//解决android5.0版本以后背景不透明
        mDialog.show();
        mDialog.setCancelable(false);// 设置dialog不能够点击它 区域以外的区域而销毁它自己

    }
}

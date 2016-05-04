package org.anyrtc.dync;


import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import org.anyrtc.AnyRTC;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //以下参数为测试参数，仅供3分钟演示；如需更久请前往注册开发者账号，地址：www.anyrtc.io
        AnyRTC.InitAnyRTCWithAppKey("teameetingtest", "c4cd1ab6c34ada58e622e75e41b46d6d", "OPJXF3xnMqW+7MMTA4tRsZd6L41gnvrPcI25h9JCA4M", "meetingtest");
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        {//* Release RtcClient
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            MainActivity.this.finish();
        }
        return super.onKeyDown(keyCode, event);
    }


    public void OnBtnClicked(View btn) {
        if (btn.getId() == R.id.btn_live) {
            this.startActivity(new Intent(this,
                    LiveHallActivity.class));
        } else if (btn.getId() == R.id.btn_p2p) {
            showDialog();
        } else if (btn.getId() == R.id.btn_m2m) {
            this.startActivity(new Intent(this,
                    PeoplesCallActivity.class));
        }
    }

    /**
     * 提示弹出框
     */
    private void showDialog() {

        String title = "音频通话功能暂未开放";
        String msg = "请拨打 021-65650071，进行咨询";
        String ok = "确定";
        String cancel = "取消";
        ConfirmDialog mDialog = new ConfirmDialog(this, title, msg, ok, cancel, new ConfirmDialog.OnDialogButtonClickListener() {

            @Override
            public void onOkClick(Dialog dialog, View v) {// 前往

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

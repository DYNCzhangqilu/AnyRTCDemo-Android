package org.anyrtc.dync;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * 确定在右，取消在左
 */
public class ConfirmDialog extends AlertDialog implements View.OnClickListener {

    private int layoutId;
    private final String title;
    private final String msg;
    private final String ok;
    private final String cancel;
    private final OnDialogButtonClickListener listener;

    public ConfirmDialog(Context context, String title, String msg, String ok, String cancel, OnDialogButtonClickListener listener) {
        super(context);
        this.title = title;
        this.msg = msg;
        this.ok = ok;
        this.cancel = cancel;
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_confirm);

        TextView tv_title = (TextView) findViewById(R.id.tv_title);
        TextView tv_msg = (TextView) findViewById(R.id.tv_msg);

        tv_title.setText(title);
        tv_msg.setText(msg);

        Button btn_ok = (Button) findViewById(R.id.btn_ok);
        Button btn_cancel = (Button) findViewById(R.id.btn_cancel);

        btn_ok.setText(ok);
        btn_cancel.setText(cancel);

        btn_ok.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
    }

    /**
     * 对话框按钮单击的监听器
     */
    public interface OnDialogButtonClickListener {

        /**
         * 当确定按钮被单击的时候会执行
         */
        void onOkClick(Dialog dialog, View v);

        /**
         * 当取消按钮被单击的时候会执行
         */
        void onCancelClick(Dialog dialog, View v);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_ok: // 确定
                if (listener != null) {
                    listener.onOkClick(this, v);
                }
                cancel();
                break;
            case R.id.btn_cancel: // 取消
                if (listener != null) {
                    listener.onCancelClick(this, v);
                }
                cancel();
                break;
        }
    }

}

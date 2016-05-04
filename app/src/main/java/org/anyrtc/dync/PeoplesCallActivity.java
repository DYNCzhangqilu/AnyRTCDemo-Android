package org.anyrtc.dync;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class PeoplesCallActivity extends Activity {

    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peoples_call);

        editText = (EditText) findViewById(R.id.editText);
        editText.setSelection(editText.getText().toString().length());
    }

    public void OnBtnClicked(View btn) {
        if (btn.getId() == R.id.btn_back) {
            finish();
        } else if (btn.getId() == R.id.btn_switch_room) {

            Intent intent = new Intent(this, AnyRTCMeetActivity.class);
            intent.putExtra("ROOMID", editText.getText().toString());
            this.startActivity(intent);
        }
    }
}

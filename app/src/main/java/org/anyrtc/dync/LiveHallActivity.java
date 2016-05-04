package org.anyrtc.dync;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Eric on 2016/3/14.
 */
public class LiveHallActivity extends Activity {
    private Button mBtnLive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_hall);
        {//* Init UI
            mBtnLive = (Button) findViewById(R.id.btn_live);
        }

    }

    public void OnBtnClicked(View btn) {
        Button button = (Button) btn;
        if (btn.getId() == R.id.btn_back) {
            finish();
        } else if (btn.getId() == R.id.btn_live) {
            Intent i = new Intent(this,
                    LiveHostActivity.class);
            i.putExtra("AnyrtcID", "800000000025");//这里对应着你所申请的AnyRTC ID
            i.putExtra("UserName", "Eric@Mao" + (int) (1 + Math.random() * 100));//1-100随机数
            this.startActivity(i);
        } else if (btn.getId() == R.id.btn_watch_live) {
            Intent i = new Intent(this,
                    LiveGuestActivity.class);
            i.putExtra("AnyrtcID", "800000000025");//这里对应着你所申请的AnyRTC ID
            i.putExtra("UserName", "Eric@Mao" + (int) (1 + Math.random() * 100));//1-100随机数
            this.startActivity(i);
        }
    }

}

# AnyRTCDemo-Android
1：编译环境
-------------
  1:先去[AnyRTC平台](https://www.anyrtc.io)注册开发者。</br>
  2:注册后，点击创建应用。完成后在平台上会得到开发者ID(DeveloperID),AppID(自己创建应用时自己填写的应用ID),AppKey(应用秘钥)，App Token（应用token）。下载完demo 后，把上述参数填写进去。</br>
  3:在观看demo中，必须要在网页上点击生成测试的AnyRTC ID账号，或服务对接生成该账号。填写到demo 中。</br>
  4:此时应用就可以跑通了。如有疑问，请联系我。
2：联系方式
-------------
  邮箱：xiongxuesong@dync.cc
3：Demo演示
-------------
[AnyRTCDemo](http://www.pgyer.com/OVOA)

4：demo中所需修改的内容
-------------
这张截图是填写开发者账号后所申请的app信息，它所对应的代码在MainActivity.java中。
参数一：String strDeveloperId ---> 开发者ID
参数二：String strToken ---> App Token
参数三：String strAESKey ---> APPKey
参数四：String strAppId ---> AnyRTC ID

     @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //以下参数为测试参数，仅供3分钟演示；如需更久请前往注册开发者账号，地址：www.anyrtc.io
        AnyRTC.InitAnyRTCWithAppKey("teameetingtest", "c4cd1ab6c34ada58e622e75e41b46d6d", "OPJXF3xnMqW+7MMTA4tRsZd6L41gnvrPcI25h9JCA4M", "meetingtest");
    }

![Alt text](https://github.com/AnyRTC/AnyRTCDemo-Android/blob/master/screenshot.png)

这段代码是LiveHallActivity.java中的，它这里的AnyrtcID指的是主播所在的房间ID。
（注意：当AnyrtcID已经有人在的时候，别人是不可以再进行直播，只能观看）

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

  这段代码是AnyRTCMeetActivity.java中的，它这里的AnyrtcID指的是主播所在的房间ID。

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
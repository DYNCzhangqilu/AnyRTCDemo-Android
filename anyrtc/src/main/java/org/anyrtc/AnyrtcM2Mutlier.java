package org.anyrtc;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.util.Log;

import org.anyrtc.common.M2MPublisher;
import org.anyrtc.common.M2MSubscriber;
import org.anyrtc.common.M2MultierEvents;
import org.anyrtc.common.AnyRTCViewEvents;
import org.anyrtc.common.PeerConnectionClients;
import org.anyrtc.jni.JRtcApp;
import org.anyrtc.jni.JRtcHelper;
import org.anyrtc.jni.JRtcType;
import org.anyrtc.jni.NativeContextRegistry;
import org.anyrtc.util.AppRTCUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by Eric on 2016/1/1.
 */
public class AnyrtcM2Mutlier implements JRtcHelper, PeerConnectionClients.PeerConnectionEvents, RtcVideoViews.VideoViewEvent {
    private final static String TAG = "AnyrtcM2Mutlier";
    private static NativeContextRegistry sNativeContext;
    private JRtcApp mRtcApp;
    private PeerConnectionClients peerClients;

    private Activity mActivity;
    private AppRTCAudioManager audioManager = null;
    private M2MultierEvents mEvents;
	private AnyRTCViewEvents mViewEvents;
    private M2MPublisher mPublisher;
    private HashMap<String, M2MSubscriber> mSubscribers;

    public AnyrtcM2Mutlier(Activity context, M2MultierEvents events) {
        AppRTCUtils.assertIsTrue(context != null && events != null);
        mActivity = context;
        mEvents = events;
        {/* 获取屏幕宽度高度 */
            DisplayMetrics dm = new DisplayMetrics();
            mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
            AnyRTC.gScrnWidth = dm.widthPixels;
            AnyRTC.gScrnHeight = dm.heightPixels;
        }
        {// * New all value.
            if (sNativeContext == null) {
                sNativeContext = new NativeContextRegistry();
                sNativeContext.register(context);
            }
            mRtcApp = new JRtcApp(this);
            mPublisher = new M2MPublisher();
            mSubscribers = new HashMap<String, M2MSubscriber>();
        }

        {// * Init PeerConnectionClients
            PeerConnectionClients.PeerConnectionParameters peerConnectionParameters = new PeerConnectionClients.PeerConnectionParameters(
                    true, false, false, 640, 480, 20, 512, "H264", true, false, 24, "opus", false, false, true);
            List<PeerConnection.IceServer> iceServers = new ArrayList<PeerConnection.IceServer>();
            peerClients = PeerConnectionClients.getInstance();
            peerClients.createPeerConnectionFactory(
                    context, peerConnectionParameters, this, iceServers);
        }

        {// * Create and audio manager that will take care of audio routing,
            // audio modes, audio device enumeration etc.
            audioManager = AppRTCAudioManager.create(context, new Runnable() {
                        // This method will be called each time the audio state (number and
                        // type of devices) has been changed.
                        @Override
                        public void run() {
                            onAudioManagerChangedState();
                        }
                    }
            );
            // Store existing audio settings and change audio mode to
            // MODE_IN_COMMUNICATION for best possible VoIP performance.
            Log.d(TAG, "Initializing the audio manager...");
            audioManager.init();
        }
    }

    public void InitAnyRTCViewEvents(AnyRTCViewEvents events) {
        mViewEvents = events;
        if(mViewEvents != null) {
            peerClients.setEglBase(mViewEvents.GetEglBase(), true);
        }
    }

    public void SwitchCamera() {
        peerClients.switchCamera();
    }

    public void SetLocalAudioEnabled(boolean enable) {
        peerClients.setLocalAudioEnabled(enable);
    }

    public void SetLocalVideoEnabled(boolean enable) {
        peerClients.setLocalVideoEnabled(enable);
    }

    public void OnResume() {
        peerClients.startVideoSource();
    }

    public void OnPause() {
        peerClients.stopVideoSource();
    }

    /** 发布通道
     * @param params	发布通道的参数
     * @return			true:成功	false:失败
     */
    public boolean Publish(final M2MPublisher.PublishParams params) {
        if (mPublisher.bInit) {
            return false;
        }
        mPublisher.bInit = true;
        mPublisher.bUseVideo = params.bEnableVideo;
        if (!Connected()) {
            Connect();
        } else {
            PublishInternal();
        }
        return true;
    }

    /** 取消发布
     */
    public void UnPublish() {
        if (mPublisher.bInit) {
            if (mPublisher.strChannelId != null) {
                mRtcApp.Unpublish(mPublisher.strChannelId);
            }
            if(mPublisher.strPeerId != null) {
                peerClients.closePeerConnection(mPublisher.strPeerId);
                mPublisher.strPeerId = null;
            }
        }
        mPublisher.Clear();
        Check2Disconnect();
    }

    /** 订阅
     * @param strPublishId	通道ID
     * @param recvVideo	是否接收视频
     * @return				true:成功	false:失败
     */
    public boolean Subscribe(final String strPublishId, Boolean recvVideo) {
        AppRTCUtils.assertIsTrue(strPublishId != null && strPublishId.length() > 0);
        if (mSubscribers.containsKey(strPublishId)) {
            return false;
        }
        int size = mSubscribers.size() + 1;
        M2MSubscriber subscriber = new M2MSubscriber();
        subscriber.bSubscibed = false;
        subscriber.strPublishId = strPublishId;
        mSubscribers.put(strPublishId, subscriber);

        if (!Connected()) {
            Connect();
        } else {
            SubscribeInternal();
        }
        return true;
    }

    /** 取消订阅
     *
     * @param strPublishId	通道ID
     */
    public void UnSubscribe(final String strPublishId) {
        UnSubscribeInternal(strPublishId);
    }

    public void Destroy() {
        {
            UnPublish();
            for(String key : mSubscribers.keySet()) {
                M2MSubscriber subscriber = mSubscribers.get(key);
                if(subscriber.bSubscibed) {
                    if(subscriber.strChannelId != null) {
                        mRtcApp.Unsubscribe(subscriber.strChannelId);
                        if(subscriber.strPeerId != null) {
                            peerClients.closePeerConnection(subscriber.strPeerId);
                            subscriber.strPeerId = null;
                        }
                    }
                }
            }
            mSubscribers.clear();
        }
        if(peerClients != null) {
            peerClients.close();
            peerClients = null;
        }
        if (audioManager != null) {
            audioManager.close();
            audioManager = null;
        }

        if (null != mRtcApp) {
            mRtcApp.Destroy();
            mRtcApp = null;
        }
    }
    /**
     * Internal function.
     */
    private void onAudioManagerChangedState() {
        // TODO(henrika): disable video if AppRTCAudioManager.AudioDevice.EARPIECE
        // is active.
    }
    private boolean Connected() {
        return mRtcApp.ConnectionStatus() == JRtcType.CONNECTED;
    }
    private void Connect() {
        if(mRtcApp.ConnectionStatus() == JRtcType.NOT_CONNECTED) {
            mRtcApp.Connect(AnyRTC.gSvrAddr, AnyRTC.gSvrPort, AnyRTC.gStrDeveloperId, AnyRTC.gStrToken, AnyRTC.gStrAESKey, AnyRTC.gStrAppId);
        }
    }

    private void ChangeCaptureFormat(int width, int height, int framerate) {
        peerClients.changeCaptureFormat(width, height, framerate);
    }

    private void PublishInternal() {
        if(mPublisher.bInit) {
            mRtcApp.Publish("980988", mPublisher.nWidth, mPublisher.nHeight,
                    mPublisher.aBitrate, mPublisher.vBitrate);
            mPublisher.strPeerId = peerClients.createPeerConnection(PeerConnectionClients.PeerConnectionMode.PCM_SEND_ONLY, VideoRendererGui.getEglBaseContext());
            AppRTCUtils.assertIsTrue(mPublisher.strPeerId != null);
        }
    }

    private void SubscribeInternal() {
        for(String key : mSubscribers.keySet()) {
            M2MSubscriber subscriber = mSubscribers.get(key);
            if(!subscriber.bSubscibed) {
                subscriber.bSubscibed = true;
                if( subscriber.strPeerId == null)
                    subscriber.strPeerId = peerClients.createPeerConnection(PeerConnectionClients.PeerConnectionMode.PCM_RECV_ONLY, VideoRendererGui.getEglBaseContext());
                mRtcApp.Subscribe(subscriber.strPublishId);
            }
        }
    }

    private void UnSubscribeInternal(String strPublishId) {
        if(mSubscribers.containsKey(strPublishId)) {
            M2MSubscriber subscriber = mSubscribers.get(strPublishId);
            if(subscriber.bSubscibed) {
                if(subscriber.strPeerId != null) {
                    peerClients.closePeerConnection(subscriber.strPeerId);
                    subscriber.strPeerId = null;
                }
                if(subscriber.strChannelId != null) {
                    mRtcApp.Unsubscribe(subscriber.strChannelId);
                }

            }
            mSubscribers.remove(strPublishId);
        }

        Check2Disconnect();
    }

    private void CallPublishFailed(int code, String errInfo) {
        if (mPublisher.bInit) {
            if(mPublisher.strPeerId != null) {
                peerClients.closePeerConnection(mPublisher.strPeerId);
                mPublisher.strPeerId = null;
            }
            mEvents.OnRtcPublishFailed(code, errInfo);
        }
        mPublisher.Clear();
    }

    private void CallSubscribeFailed(String strPublishId, int code, String errInfo) {
        Iterator iter = mSubscribers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            M2MSubscriber subscriber = (M2MSubscriber) entry.getValue();
            if(strPublishId.equals(subscriber.strPublishId)) {
                if(subscriber.strPeerId != null) {
                    peerClients.closePeerConnection(subscriber.strPeerId);
                    subscriber.strPeerId = null;
                }
                mEvents.OnRtcSubscribeFailed(subscriber.strPublishId, code, errInfo);
                mSubscribers.remove(iter);
                break;
            }
        }
    }

    private void ConnectionFailedInternal(int code, String errInfo) {
        CallPublishFailed(code, errInfo);

        Iterator iter = mSubscribers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            M2MSubscriber subscriber = (M2MSubscriber) entry.getValue();
            if(subscriber.strPeerId != null) {
                peerClients.closePeerConnection(subscriber.strPeerId);
                subscriber.strPeerId = null;
            }
            mEvents.OnRtcSubscribeFailed(subscriber.strPublishId, code, errInfo);
        }
        mSubscribers.clear();

        Check2Disconnect();
    }

    private void Check2Disconnect() {
        int allConn = 0;
        if (mPublisher.bInit) {
            allConn++;
        }
        allConn += mSubscribers.size();
        if (allConn == 0
                && mRtcApp.ConnectionStatus() != JRtcType.NOT_CONNECTED) {
            mRtcApp.Disconnect();
        }
    }

    // Should be called from UI thread
    private void CallConnected(String peerId) {
        if (peerId.equals(mPublisher.strPeerId)) {

        } else {
            Iterator iter = mSubscribers.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                M2MSubscriber val = (M2MSubscriber) entry.getValue();
                if(peerId.equals(val.strPeerId)) {
                    mEvents.OnRtcSubscribeOK(val.strPublishId);
                    break;
                }
            }
        }
    }

    private void OutgoingSdpInternal(String peerId, String message) {
        JSONObject jsonData = new JSONObject();
        try {
            JSONTokener jsonParser = new JSONTokener(message);
            final JSONObject json = (JSONObject) jsonParser.nextValue();
            final String type = json.has("type") ? json.getString("type") : "";
            jsonData.put("janus", "message");
            JSONObject jsonReq = new JSONObject();
            if (peerId.equals(mPublisher.strPeerId)) {
                if (type != null && type.length() > 0) {
                    if (type.equals("offer")) {
                        jsonReq.put("request", "configure");
                        jsonReq.put("audio", true);
                        jsonReq.put("video", true);
                        jsonData.put("body", jsonReq);
                    } else {
                        AppRTCUtils.assertIsTrue(false);
                    }
                    jsonData.put("transaction", "x8971");
                    jsonData.put("jsep", json);
                } else {
                    jsonData.put("janus", "trickle");
                    jsonData.put("transaction", "x8972");
                    jsonData.put("candidate", json);
                }
                mRtcApp.SendSdpInfo(mPublisher.strChannelId,
                        jsonData.toString());
            } else {
                if (type != null && type.length() > 0) {
                    if (type.equals("answer")) {
                        jsonReq.put("request", "start");
                        jsonReq.put("room", "1234");
                        jsonData.put("body", jsonReq);
                    } else {
                        AppRTCUtils.assertIsTrue(false);
                    }
                    jsonData.put("transaction", "x8971");
                    jsonData.put("jsep", json);
                } else {
                    jsonData.put("janus", "trickle");
                    jsonData.put("transaction", "x8972");
                    jsonData.put("candidate", json);
                }
                Iterator iter = mSubscribers.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    M2MSubscriber val = (M2MSubscriber) entry.getValue();
                    if(peerId.equals(val.strPeerId)) {
                        mRtcApp.SendSdpInfo(val.strChannelId,
                                jsonData.toString());
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void IncomingSdpInternal(String strChannelId, String strJsep) {
        try {
            JSONTokener jsonParser = new JSONTokener(strJsep);
            final JSONObject json = (JSONObject) jsonParser.nextValue();
            final String type = json.has("type") ? json.getString("type") : "";
            final String sdp = json.has("sdp") ? json.getString("sdp") : "";
            if (mPublisher.strChannelId != null && mPublisher.strChannelId.equals(strChannelId)) {
                if (type != null && type.length() > 0) {
                    final SessionDescription.Type jtype = SessionDescription.Type
                            .fromCanonicalForm(type);
                    if (type.equals("answer")) {
                        AppRTCUtils.assertIsTrue(mPublisher.strPeerId != null);
                        peerClients.setRemoteDescription(mPublisher.strPeerId, new SessionDescription(jtype, sdp));
                    } else {
                        AppRTCUtils.assertIsTrue(false);
                    }
                } else {
                    IceCandidate candidate = new IceCandidate(
                            json.getString("sdpMid"),
                            json.getInt("sdpMLineIndex"),
                            json.getString("candidate"));
                    AppRTCUtils.assertIsTrue(mPublisher.strPeerId != null);
                    peerClients.addRemoteIceCandidate(mPublisher.strPeerId, candidate);
                }
            } else {
                IceCandidate candidate = new IceCandidate(
                        json.getString("sdpMid"),
                        json.getInt("sdpMLineIndex"),
                        json.getString("candidate"));
                Iterator iter = mSubscribers.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    M2MSubscriber val = (M2MSubscriber) entry.getValue();
                    if (strChannelId.equals(val.strChannelId)) {
                        if(val.strPeerId != null)
                            peerClients.addRemoteIceCandidate(val.strPeerId, candidate);
                        break;
                    }
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * get strPublishId
     * @param peerId
     * @return
     */

    private String getPublishId(String peerId){

        Iterator iter = mSubscribers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            M2MSubscriber subscriber = (M2MSubscriber) entry.getValue();
            if(peerId.equals(subscriber.strPeerId)) {
                if(subscriber.strPublishId != null) {
                    return subscriber.strPublishId;
                }

            }
        }
        return null;
    }


    /**
     * Implements for JRtcHelper.
     */
    @Override
    public void OnRtcConnect(final int code, String strDyncId, String strServerId, final String strSysConf) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject json= new JSONObject(strSysConf);
                    AnyRTC.gDebug = json.has("Debug")?json.getInt("Debug")== 1?true:false:false;
                    AnyRTC.gAutoBitrate  = json.has("AutoBitrate")?json.getInt("AutoBitrate")== 1?true:false:false;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(code == 200) {
                    PublishInternal();
                    SubscribeInternal();
                } else {
                    ConnectionFailedInternal(AnyRTC.gDebug?code:0, AnyRTC.GetErrString(code));
                }
            }
        });
    }

    @Override
    public void OnRtcMessage(String fromId, String strJsep) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void OnRtcPublish(final String strResult, final String strChannelId, final String strDyncerId, final String strRtmpUrl, final String strHlsUrl, final String strError) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (strResult.equals("ok")) {
                    if (mPublisher.bInit) {
                        mPublisher.strChannelId = strChannelId;
                        if(mPublisher.strPeerId != null) {
                            peerClients.createOffer(mPublisher.strPeerId);
                        }
                        mEvents.OnRtcPublishOK(strDyncerId, strRtmpUrl, strHlsUrl);
                    }
                } else {
                    CallPublishFailed(AnyRTC.gDebug?-1:0, AnyRTC.gDebug?strError: AnyRTC.GetErrString(0));
                    Check2Disconnect();
                }
            }
        });
    }

    @Override
    public void OnRtcUnpublish(final String strResult, final String strChannelId) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void OnRtcSubscribe(final String strResult, final String strChannelId, final String strSubscribeId, final String strJsep) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(strResult.equals("ok")) {
                    if(mSubscribers.containsKey(strSubscribeId)) {
                        M2MSubscriber subscriber = mSubscribers.get(strSubscribeId);
                        subscriber.strChannelId = strChannelId;
                        if (strJsep != null && strJsep.length() > 0) {
                            JSONTokener jsonParser = new JSONTokener(strJsep);
                            try {
                                final JSONObject json = (JSONObject) jsonParser.nextValue();
                                final String type = json.has("type") ? json.getString("type")
                                        : "";
                                final String sdp = json.has("sdp") ? json.getString("sdp") : "";

                                if (type != null && type.length() > 0) {
                                    SessionDescription.Type jtype = SessionDescription.Type
                                            .fromCanonicalForm(type);
                                    if (type.equals("offer")) {
                                        peerClients.setRemoteDescription(subscriber.strPeerId, new SessionDescription(jtype, sdp));
                                        peerClients.createAnswer(subscriber.strPeerId);
                                    }
                                }
                            } catch (JSONException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        } else {
                            peerClients.createOffer(subscriber.strPeerId);
                        }
                    }
                } else {
                    CallSubscribeFailed(strSubscribeId, AnyRTC.gDebug?-1:0, AnyRTC.gDebug?"": AnyRTC.GetErrString(0));
                    Check2Disconnect();
                }
            }
        });
    }

    @Override
    public void OnRtcUnsubscribe(final String strResult, final String strChannelId) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void OnRtcSdpInfo(final String strChannelId, final String strJsep) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                IncomingSdpInternal(strChannelId, strJsep);
            }
        });
    }

    @Override
    public void OnRtcUserOptionJoin(String strAnyrtcId, String strResult, String strMessage) {

    }

    @Override
    public void OnRtcUserOptionNotify(String strAnyrtcId, String strMessage) {

    }

    @Override
    public void OnRtcUserOptionLeave(String strAnyrtcId) {

    }

    @Override
    public void OnRtcDisconnect() {

    }

    @Override
    public void OnRtcConnectFailed() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ConnectionFailedInternal(-1, "Network error(server cann't connect)!");
            }
        });
    }

    /**
     * Implements for PeerConnectionClients.PeerConnectionEvents.
     */
    @Override
    public void onLocalDescription(final String peerId, final SessionDescription sdp) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String fmt = "{\"type\":\"%s\",\"sdp\":\"%s\"}";
                String strMsg = String.format(fmt, sdp.type.canonicalForm(), sdp.description);

                OutgoingSdpInternal(peerId, strMsg);
            }
        });
    }

    @Override
    public void onIceCandidate(final String peerId, final IceCandidate candidate) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String fmt = "{\"sdpMid\":\"%s\",\"sdpMLineIndex\":%d,\"candidate\":\"%s\"}";
                String strMsg = String.format(fmt, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
                OutgoingSdpInternal(peerId, strMsg);
            }
        });
    }

    @Override
    public void onIceConnected(final String peerId) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CallConnected(peerId);
            }
        });
    }

    @Override
    public void onIceDisconnected(String peerId) {

    }

    @Override
    public void onPeerConnectionClosed(String peerId) {

    }

    @Override
    public void onPeerConnectionStatsReady(final String peerId, final StatsReport[] reports) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                M2MSubscriber subscriber = null;
                Iterator iter = mSubscribers.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    subscriber = (M2MSubscriber) entry.getValue();
                    if(peerId.equals(subscriber.strPeerId)) {
                        break;
                    }
                }
                if(subscriber == null)
                    return ;
                String bytesReceived;
                String reveivedHeight;
                Double reveivedTime;
                for (StatsReport report : reports)
                {

                    bytesReceived = null;
                    reveivedHeight = null;
                    if (report.type.equals("ssrc"))
                    {
                        reveivedTime = report.timestamp;
                        Map<String, String> reportMap = new HashMap<String, String>();

                        for (StatsReport.Value value : report.values)
                        {
                            reportMap.put(value.name, value.value);

                            if (value.name.equals("googFrameHeightReceived"))
                            {
                                reveivedHeight = value.value;
                                if (bytesReceived != null)
                                {
                                    break;
                                }
                            }

                            if (value.name.equals("bytesReceived"))
                            {
                                bytesReceived = value.value;

                            }
                        }

                        if (bytesReceived != null && reveivedHeight != null)
                        {
                            subscriber.nBsNow = Integer.parseInt(bytesReceived);
                            subscriber.nTsNow = reveivedTime;

                            if (subscriber.nBsBefore == 0 || subscriber.nTsBefore == 0.0)
                            {
                                // Skip this round
                                subscriber.nBsBefore = subscriber.nBsNow;
                                subscriber.nTsBefore = subscriber.nTsNow;
                            } else
                            {
                                // Calculate bitrate
                                int tempBit = (subscriber.nBsNow - subscriber.nBsBefore);
                                if (tempBit < 0)
                                    continue;
                                Double tempTime = (subscriber.nTsNow - subscriber.nTsBefore);
                                // Log.e(TAG,
                                // " tempBit "+tempBit+" tempTime "+tempTime);
                                subscriber.nBitrate = (int)Math.round(tempBit / tempTime);
                                //Log.i(TAG, "CurBitrate: " + subscriber.nBitrate);

                                subscriber.nBsBefore = subscriber.nBsNow;
                                subscriber.nTsBefore = subscriber.nTsNow;
                                subscriber.nBsSum += tempBit;
                                subscriber.nTsSum += tempTime;
                                if(subscriber.nTsSum >= 30000) {
                                    Log.i(TAG, String.format("Upload peer(%s) bitrate(%d(kbps)/%d)", peerId, subscriber.nBsSum/1000, (int)Math.round(subscriber.nTsSum/1000)));
                                    subscriber.nBsSum = 0;
                                    subscriber.nTsSum = 0.0;
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onPeerConnectionError(String peerId, String description) {

    }

    @Override
    public void onOpenRemoteRender(final String peerId,final VideoTrack remoteTrack) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String publishId = getPublishId(peerId);
                if(publishId!=null && mViewEvents != null){
                    mViewEvents.OnRtcOpenRemoteRender(publishId,remoteTrack);
                }

            }
        });

    }

    @Override
    public void onRemoveRemoteRender(final String peerId) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String publishId = getPublishId(peerId);
                if(publishId!=null && mViewEvents != null){
                    mViewEvents.OnRtcRemoveRemoteRender(publishId);
                }
            }
        });

    }

    @Override
    public void onOpenLocalRender(final VideoTrack localTrack) {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mViewEvents != null && mViewEvents != null) {
                    mViewEvents.OnRtcOpenLocalRender(localTrack);
                }
            }
        });
    }

    @Override
    public void onRemoveLocalRender() {
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mViewEvents != null && mViewEvents != null) {
                    mViewEvents.OnRtcRemoveLocalRender();
                }
            }
        });
    }

    /**
     * Implements for RtcVideoViews.VideoViewEvent.
     */
    @Override
    public void OnScreenSwitch(String strBeforeFullScrnId, String strNowFullScrnId) {
        int i = 0;
        Iterator iter = mSubscribers.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            M2MSubscriber subscriber = (M2MSubscriber) entry.getValue();
            if(strBeforeFullScrnId.equals(subscriber.strPeerId)) {
                if(subscriber.bSubscibed) {
                    mRtcApp.SwitchVideoBits(subscriber.strChannelId, 0);
                }
                i++;
            } else if(strNowFullScrnId.equals(subscriber.strPeerId)) {
                if(subscriber.bSubscibed) {
                    mRtcApp.SwitchVideoBits(subscriber.strChannelId, 3);
                }
                i++;
            }
            if(i == 2) {
                break;
            }
        }
    }
}

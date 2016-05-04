package org.anyrtc;

import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;

import org.anyrtc.util.AppRTCUtils;
import org.webrtc.RendererCommon;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Eric on 2016/1/5.
 */
public class RtcVideoViews implements View.OnTouchListener{
    private static final int SUB_X= 2;
    private static final int SUB_Y = 72;
    private static final int SUB_WIDTH = 18;
    private static final int SUB_HEIGHT = 16;

    public interface VideoViewEvent {
        void OnScreenSwitch(String strBeforeFullScrnId, String strNowFullScrnId);
    }

    protected static class VideoView {
        public String strPeerId;
        public int index;
        public int x;
        public int y;
        public int w;
        public int h;
        public VideoTrack mVideoTrack = null;
        public VideoRenderer mRenderer = null;
        public VideoRenderer.Callbacks mCallback = null;
        public VideoView(String strPeerId, int index, int x, int y, int w, int h) {
            this.strPeerId = strPeerId;
            this.index = index;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
        public Boolean Fullscreen() {
            return w == 100 || h == 100;
        }
        public Boolean Hited(int px, int py) {
            if(!Fullscreen()) {
                int left = x * AnyRTC.gScrnWidth / 100;
                int top = y * AnyRTC.gScrnHeight / 100;
                int right = (x + w) * AnyRTC.gScrnWidth / 100;
                int bottom = (y + h) * AnyRTC.gScrnHeight / 100;
                if((px >= left && px <= right) && (py >= top && px <= bottom)) {
                    return true;
                }
            }
            return false;
        }
        private void updateRender(VideoRenderer.Callbacks callbacks) {
            AppRTCUtils.assertIsTrue(mVideoTrack != null);
            mVideoTrack.removeRenderer(mRenderer);
            mCallback = callbacks;
            mRenderer = new VideoRenderer(mCallback);
            mVideoTrack.addRenderer(mRenderer);
        }
    }

    private VideoViewEvent mEvents;
    private GLSurfaceView mVideoView;
    private VideoView mLocalRender;
    private HashMap<String, VideoView> mRemoteRenders;

    public RtcVideoViews(VideoViewEvent evnets, GLSurfaceView videoView) {
        mEvents = evnets;
        mVideoView = videoView;
        mVideoView.setOnTouchListener(this);
        mRemoteRenders = new HashMap<>();
        mLocalRender = new VideoView("localRender", 0, 0, 0, 100, 100);
    }

    public void OpenLocalRender(VideoTrack track) {
        mLocalRender.mVideoTrack = track;
        if(mLocalRender.mRenderer == null) {
            mLocalRender.mCallback = VideoRendererGui.create(mLocalRender.x, mLocalRender.y, mLocalRender.w, mLocalRender.h,
                    RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
            mLocalRender.mRenderer = new VideoRenderer(mLocalRender.mCallback);
        }
        mLocalRender.mVideoTrack.addRenderer(mLocalRender.mRenderer);
    }

    public VideoTrack LocalVideoTrack() {
        return mLocalRender.mVideoTrack;
    }

    public void CloseLocalRender() {
        if(mLocalRender.mVideoTrack != null) {
            VideoRendererGui.remove(mLocalRender.mCallback);
            mLocalRender.mCallback = null;
            mLocalRender.mRenderer = null;
            mLocalRender.mVideoTrack = null;
        }
    }

    public void OpenRemoteRender(String peerId, VideoTrack track) {
        VideoView remoteRender = mRemoteRenders.get(peerId);
        if(remoteRender == null) {
            int size = mRemoteRenders.size() + 1;
            remoteRender = new VideoView(peerId, size, ( 100 - size* (SUB_WIDTH + SUB_X)), SUB_Y, SUB_WIDTH, SUB_HEIGHT);
            remoteRender.mCallback = VideoRendererGui.create(remoteRender.x, remoteRender.y, remoteRender.w, remoteRender.h,
                    RendererCommon.ScalingType.SCALE_ASPECT_FILL, false);
            remoteRender.mRenderer = new VideoRenderer(remoteRender.mCallback);
            remoteRender.mVideoTrack = track;
            remoteRender.mVideoTrack.addRenderer(remoteRender.mRenderer);
            mRemoteRenders.put(peerId, remoteRender);
            if(mRemoteRenders.size() == 1) {
                SwitchViewToFullscreen(remoteRender, mLocalRender);
            }
        }
    }

    public void RemoveRemoteRender(String peerId) {
        VideoView remoteRender = mRemoteRenders.get(peerId);
        if(remoteRender != null) {
            if(remoteRender.Fullscreen()) {
                SwitchIndex1ToFullscreen(remoteRender);
            }
            if(remoteRender.index < mRemoteRenders.size()) {
                BubbleSortSubView(remoteRender);
            }
            if(remoteRender.mVideoTrack != null) {
                VideoRendererGui.remove(remoteRender.mCallback);
                remoteRender.mCallback = null;
                remoteRender.mRenderer = null;
                remoteRender.mVideoTrack = null;
            }
            mRemoteRenders.remove(peerId);
        }
    }

    private void SwitchIndex1ToFullscreen(VideoView fullscrnView) {
        AppRTCUtils.assertIsTrue(fullscrnView != null);
        VideoView view1 = null;
        if(mLocalRender.index == 1) {
            view1 = mLocalRender;
        } else {
            Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, VideoView> entry = iter.next();
                VideoView render = entry.getValue();
                if(render.index == 1)
                {
                    view1 = render;
                    break;
                }
            }
        }
        SwitchViewPosition(view1, fullscrnView);
        mEvents.OnScreenSwitch(fullscrnView.strPeerId, view1.strPeerId);
    }

    private VideoView GetFullScreen() {
        if(mLocalRender.Fullscreen())
            return mLocalRender;
        Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, VideoView> entry = iter.next();
            String peerId = entry.getKey();
            VideoView render = entry.getValue();
            if(render.Fullscreen())
                return render;
        }
        return null;
    }

    private void SwitchViewPosition(VideoView view1, VideoView view2) {
        AppRTCUtils.assertIsTrue(view1 != null && view2 != null);
        int index, x, y, w, h;
        index = view1.index;
        x = view1.x;
        y = view1.y;
        w = view1.w;
        h = view1.h;
        view1.index = view2.index;
        view1.x = view2.x;
        view1.y = view2.y;
        view1.w = view2.w;
        view1.h = view2.h;

        view2.index = index;
        view2.x = x;
        view2.y = y;
        view2.w = w;
        view2.h = h;

        VideoRenderer.Callbacks callback1 = view1.mCallback;
        VideoRenderer.Callbacks callback2 = view2.mCallback;
        view1.updateRender(callback2);
        view2.mCallback = callback1;
    }

    private void SwitchViewToFullscreen(VideoView view1, VideoView fullscrnView) {
        AppRTCUtils.assertIsTrue(view1 != null && fullscrnView != null);
        int index, x, y, w, h;
        index = view1.index;
        x = view1.x;
        y = view1.y;
        w = view1.w;
        h = view1.h;
        view1.index = fullscrnView.index;
        view1.x = fullscrnView.x;
        view1.y = fullscrnView.y;
        view1.w = fullscrnView.w;
        view1.h = fullscrnView.h;

        fullscrnView.index = index;
        fullscrnView.x = x;
        fullscrnView.y = y;
        fullscrnView.w = w;
        fullscrnView.h = h;

        VideoRenderer.Callbacks callback1 = view1.mCallback;
        VideoRenderer.Callbacks callback2 = fullscrnView.mCallback;
        view1.updateRender(callback2);
        fullscrnView.updateRender(callback1);
        mEvents.OnScreenSwitch(fullscrnView.strPeerId, view1.strPeerId);
    }

    public void BubbleSortSubView(VideoView view) {
        if(view.index + 1 == mLocalRender.index) {
            SwitchViewPosition(mLocalRender, view);
        } else {
            Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, VideoView> entry = iter.next();
                VideoView render = entry.getValue();
                if(view.index + 1 == render.index) {
                    SwitchViewPosition(render, view);
                    break;
                }
            }
        }
        if(view.index < mRemoteRenders.size()) {
            BubbleSortSubView(view);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            int startX = (int) event.getX();
            int startY = (int) event.getY();
            if(mLocalRender.Hited(startX, startY)) {
                return true;
            } else {
                Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, VideoView> entry = iter.next();
                    String peerId = entry.getKey();
                    VideoView render = entry.getValue();
                    if(render.Hited(startX, startY)) {
                        return true;
                    }
                }
            }
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            int startX = (int) event.getX();
            int startY = (int) event.getY();
            if(mLocalRender.Hited(startX, startY)) {
                SwitchViewToFullscreen(mLocalRender, GetFullScreen());
                return true;
            } else {
                Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, VideoView> entry = iter.next();
                    String peerId = entry.getKey();
                    VideoView render = entry.getValue();
                    if(render.Hited(startX, startY)) {
                        SwitchViewToFullscreen(render, GetFullScreen());
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

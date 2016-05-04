package org.anyrtc.dync;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import org.anyrtc.AnyRTC;
import org.anyrtc.common.AnyRTCViewEvents;
import org.anyrtc.util.AppRTCUtils;
import org.anyrtc.view.PercentFrameLayout;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Eric on 2016/3/4.
 */
public class LiveAnyRTCViews implements View.OnTouchListener, AnyRTCViewEvents {
    private static final int SUB_X = 2;
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
        public PercentFrameLayout mLayout = null;
        public SurfaceViewRenderer mView = null;
        public VideoTrack mVideoTrack = null;
        public VideoRenderer mRenderer = null;

        public VideoView(String strPeerId, Context ctx, EglBase eglBase, int index, int x, int y, int w, int h) {
            this.strPeerId = strPeerId;
            this.index = index;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;

            mLayout = new PercentFrameLayout(ctx);
            mLayout.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            mView = new SurfaceViewRenderer(ctx);
            mView.init(eglBase.getEglBaseContext(), null);
            mView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
            mLayout.addView(mView);
        }

        public Boolean Fullscreen() {
            return w == 100 || h == 100;
        }

        public Boolean Hited(int px, int py) {
            if (!Fullscreen()) {
                int left = x * AnyRTC.gScrnWidth / 100;
                int top = y * AnyRTC.gScrnHeight / 100;
                int right = (x + w) * AnyRTC.gScrnWidth / 100;
                int bottom = (y + h) * AnyRTC.gScrnHeight / 100;
                if ((px >= left && px <= right) && (py >= top && px <= bottom)) {
                    return true;
                }
            }
            return false;
        }

        public void close() {
            mLayout.removeView(mView);
            mView.release();
            mView = null;
            mRenderer = null;
        }

        private void updateVideoLayoutView(PercentFrameLayout layout, SurfaceViewRenderer view) {
            mLayout = layout;
            mView = view;
            if (mVideoTrack != null) {
                mVideoTrack.removeRenderer(mRenderer);
                mRenderer = new VideoRenderer(mView);
                mVideoTrack.addRenderer(mRenderer);
            }
            mView.requestLayout();
        }

        private boolean voiceFalg;
    }

    private boolean mAutoLayout;
    private EglBase mRootEglBase;
    private RelativeLayout mVideoView;
    private VideoView mLocalRender;
    private HashMap<String, VideoView> mRemoteRenders;

    public LiveAnyRTCViews(RelativeLayout videoView) {
        AppRTCUtils.assertIsTrue(videoView != null);
        mAutoLayout = false;
        mVideoView = videoView;
        mVideoView.setOnTouchListener(this);
        mRootEglBase = EglBase.create();
        mRemoteRenders = new HashMap<>();
    }

    public VideoTrack LocalVideoTrack() {
        return mLocalRender.mVideoTrack;
    }

    private int GetVideoRenderSize() {
        int size = mRemoteRenders.size();
        if (mLocalRender != null) {
            size += 1;
        }
        return size;
    }

    private void SwitchIndex1ToFullscreen(VideoView fullscrnView) {
        AppRTCUtils.assertIsTrue(fullscrnView != null);
        VideoView view1 = null;
        if (mLocalRender != null && mLocalRender.index == 1) {
            view1 = mLocalRender;
        } else {
            Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, VideoView> entry = iter.next();
                VideoView render = entry.getValue();
                if (render.index == 1) {
                    view1 = render;
                    break;
                }
            }
        }
        if (view1 != null) {
            SwitchViewPosition(view1, fullscrnView);
        }
    }

    private VideoView GetFullScreen() {
        if (mLocalRender != null && mLocalRender.Fullscreen())
            return mLocalRender;
        Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, VideoView> entry = iter.next();
            //* String peerId = entry.getKey();
            VideoView render = entry.getValue();
            if (render.Fullscreen())
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

        PercentFrameLayout layout_a = view1.mLayout;
        SurfaceViewRenderer view_a = view1.mView;
        PercentFrameLayout layout_b = view2.mLayout;
        SurfaceViewRenderer view_b = view2.mView;

        view1.updateVideoLayoutView(layout_b, view_b);
        view2.updateVideoLayoutView(layout_a, view_a);
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

        PercentFrameLayout layout_a = view1.mLayout;
        SurfaceViewRenderer view_a = view1.mView;
        PercentFrameLayout layout_b = fullscrnView.mLayout;
        SurfaceViewRenderer view_b = fullscrnView.mView;

        view1.updateVideoLayoutView(layout_b, view_b);
        fullscrnView.updateVideoLayoutView(layout_a, view_a);
    }

    public void BubbleSortSubView(VideoView view) {
        int exIndex = 1;
        if(mLocalRender != null) {
            exIndex = 0;
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
        } else {
            Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, VideoView> entry = iter.next();
                VideoView render = entry.getValue();
                if (view.index + 1 == render.index) {
                    SwitchViewPosition(render, view);
                    break;
                }
            }
        }
        if(view.index + exIndex < mRemoteRenders.size()) {
            BubbleSortSubView(view);
        }
    }

    public interface OnTouchLocalRender {
        void closeLocalRender();

        void closeRemoteRender();
    }

    OnTouchLocalRender onTouchLocalRender;

    public void setonTouchLocalRender(OnTouchLocalRender onTouchLocalRender) {
        this.onTouchLocalRender = onTouchLocalRender;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int startX = (int) event.getX();
            int startY = (int) event.getY();
            if (mLocalRender != null && mLocalRender.Hited(startX, startY)) {
                if (onTouchLocalRender != null) {
                    onTouchLocalRender.closeLocalRender();
                }
                return true;
            } else {
                Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, VideoView> entry = iter.next();
                    String peerId = entry.getKey();
                    VideoView render = entry.getValue();
                    if (render.Hited(startX, startY)) {
                        if (onTouchLocalRender != null) {
                            onTouchLocalRender.closeRemoteRender();
                        }
                        return true;
                    }
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            int startX = (int) event.getX();
            int startY = (int) event.getY();
            if (mLocalRender != null && mLocalRender.Hited(startX, startY)) {
                //SwitchViewToFullscreen(mLocalRender, GetFullScreen());
                return true;
            } else {
                Iterator<Map.Entry<String, VideoView>> iter = mRemoteRenders.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<String, VideoView> entry = iter.next();
                    String peerId = entry.getKey();
                    VideoView render = entry.getValue();
                    if (render.Hited(startX, startY)) {
                        // SwitchViewToFullscreen(render, GetFullScreen());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Implements for AnyRTCViewEvents.
     */
    @Override
    public EglBase GetEglBase() {
        return mRootEglBase;
    }

    @Override
    public void OnRtcOpenRemoteRender(String peerId, VideoTrack remoteTrack) {
        VideoView remoteRender = mRemoteRenders.get(peerId);
        if (remoteRender == null) {
            int size = GetVideoRenderSize();
            if (size == 0) {
                remoteRender = new VideoView(peerId, mVideoView.getContext(), mRootEglBase, 0, 0, 0, 100, 100);
            } else {
                remoteRender = new VideoView(peerId, mVideoView.getContext(), mRootEglBase, size, (100 - size * (SUB_WIDTH + SUB_X)), SUB_Y, SUB_WIDTH, SUB_HEIGHT);
                remoteRender.mView.setZOrderMediaOverlay(true);
            }

            mVideoView.addView(remoteRender.mLayout);

            remoteRender.mLayout.setPosition(
                    remoteRender.x, remoteRender.y, remoteRender.w, remoteRender.h);
            remoteRender.mView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
            remoteRender.mRenderer = new VideoRenderer(remoteRender.mView);

            remoteRender.mVideoTrack = remoteTrack;
            remoteRender.mVideoTrack.addRenderer(remoteRender.mRenderer);
            mRemoteRenders.put(peerId, remoteRender);
            if (mAutoLayout && mRemoteRenders.size() == 1 && mLocalRender != null) {
                SwitchViewToFullscreen(remoteRender, mLocalRender);
            }
        }
    }

    @Override
    public void OnRtcRemoveRemoteRender(String peerId) {
        VideoView remoteRender = mRemoteRenders.get(peerId);
        if (remoteRender != null) {
            remoteRender.mVideoTrack = null;
            if (remoteRender.Fullscreen()) {
                SwitchIndex1ToFullscreen(remoteRender);
            }
            if (mRemoteRenders.size() > 1 && remoteRender.index < mRemoteRenders.size()) {
                BubbleSortSubView(remoteRender);
            }
            remoteRender.close();
            mVideoView.removeView(remoteRender.mLayout);
            mRemoteRenders.remove(peerId);
            remoteRender = null;
        }
    }

    @Override
    public void OnRtcOpenLocalRender(VideoTrack localTrack) {
        int size = GetVideoRenderSize();
        if (size == 0) {
            mLocalRender = new VideoView("localRender", mVideoView.getContext(), mRootEglBase, 0, 0, 0, 100, 100);
        } else {
            mLocalRender = new VideoView("localRender", mVideoView.getContext(), mRootEglBase, size, (100 - size * (SUB_WIDTH + SUB_X)), SUB_Y, SUB_WIDTH, SUB_HEIGHT);
            mLocalRender.mView.setZOrderMediaOverlay(true);
        }
        mLocalRender.mVideoTrack = localTrack;
        mVideoView.addView(mLocalRender.mLayout);

        mLocalRender.mLayout.setPosition(
                mLocalRender.x, mLocalRender.y, mLocalRender.w, mLocalRender.h);
        mLocalRender.mView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        mLocalRender.mRenderer = new VideoRenderer(mLocalRender.mView);
        mLocalRender.mVideoTrack.addRenderer(mLocalRender.mRenderer);
    }

    @Override
    public void OnRtcRemoveLocalRender() {
        if (mLocalRender != null) {
            mLocalRender.mVideoTrack = null;
            mLocalRender.mRenderer = null;

            mVideoView.removeView(mLocalRender.mLayout);
            mLocalRender = null;
        }
    }

    @Override
    public void OnRtcRemoteAVStatus(String peerId, boolean audioEnable, boolean videoEnable) {

    }
}

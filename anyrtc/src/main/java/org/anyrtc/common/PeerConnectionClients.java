/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package org.anyrtc.common;

import android.content.Context;
import android.util.Log;

import org.anyrtc.AnyRTC;
import org.anyrtc.util.LooperExecutor;
import org.webrtc.AudioTrack;
import org.webrtc.CameraEnumerationAndroid;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.MediaCodecVideoEncoder;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaConstraints.KeyValuePair;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsObserver;
import org.webrtc.StatsReport;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.voiceengine.WebRtcAudioManager;

import java.nio.ByteBuffer;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Exchanger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Peer connection client implementation.
 *
 * <p>All public methods are routed to local looper thread.
 * All PeerConnectionEvents callbacks are invoked from the same looper thread.
 * This class is a singleton.
 */
public class PeerConnectionClients {
  public static final String VIDEO_TRACK_ID = "ARDAMSv0";
  public static final String AUDIO_TRACK_ID = "ARDAMSa0";
  private static final String TAG = "PCRTCClient";
  private static final String FIELD_TRIAL_AUTOMATIC_RESIZE =
      "WebRTC-MediaCodecVideoEncoder-AutomaticResize/Enabled/";
  private static final String VIDEO_CODEC_VP8 = "VP8";
  private static final String VIDEO_CODEC_VP9 = "VP9";
  private static final String VIDEO_CODEC_H264 = "H264";
  private static final String AUDIO_CODEC_OPUS = "opus";
  private static final String AUDIO_CODEC_ISAC = "ISAC";
  private static final String AUDIO_CODEC_PCMA = "PCMA";
  private static final String VIDEO_CODEC_PARAM_MAX_BITRATE = "x-google-max-bitrate";
  private static final String VIDEO_CODEC_PARAM_MIN_BITRATE = "x-google-min-bitrate";
  private static final String VIDEO_CODEC_PARAM_START_BITRATE =
      "x-google-start-bitrate";
  private static final String AUDIO_CODEC_PARAM_BITRATE = "maxaveragebitrate";
  private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
  private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT= "googAutoGainControl";
  private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT  = "googHighpassFilter";
  private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";
  private static final String MAX_VIDEO_WIDTH_CONSTRAINT = "maxWidth";
  private static final String MIN_VIDEO_WIDTH_CONSTRAINT = "minWidth";
  private static final String MAX_VIDEO_HEIGHT_CONSTRAINT = "maxHeight";
  private static final String MIN_VIDEO_HEIGHT_CONSTRAINT = "minHeight";
  private static final String MAX_VIDEO_FPS_CONSTRAINT = "maxFrameRate";
  private static final String MIN_VIDEO_FPS_CONSTRAINT = "minFrameRate";
  private static final String DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT = "DtlsSrtpKeyAgreement";
  private static final int HD_VIDEO_WIDTH = 1280;
  private static final int HD_VIDEO_HEIGHT = 720;
  private static final int MAX_VIDEO_WIDTH = 1280;
  private static final int MAX_VIDEO_HEIGHT = 1280;
  private static final int MAX_VIDEO_FPS = 30;

  public enum PeerConnectionMode {
    PCM_SEND_RECV,
    PCM_SEND_ONLY,
    PCM_RECV_ONLY
  };

  private static class RtcPeerConnection {
    private PeerConnection pc;
    private DataChannel dc;
    private boolean isInitiator;
    // Queued remote ICE candidates are consumed only after both local and
    // remote descriptions are set. Similarly local ICE candidates are sent to
    // remote peer after both local and remote description are set.
    private LinkedList<IceCandidate> queuedRemoteCandidates;
    private SessionDescription localSdp; // either offer or answer SDP
    private PeerConnectionMode mode;
    private SDPObserver sdpObserver;
    private DCObserver dcObserver;
    protected void drainCandidates() {
      if (queuedRemoteCandidates != null) {
        Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
        for (IceCandidate candidate : queuedRemoteCandidates) {
          pc.addIceCandidate(candidate);
        }
        queuedRemoteCandidates = null;
      }
    }
  };

  private static final PeerConnectionClients instance = new PeerConnectionClients();
  private final LooperExecutor executor;

  private PeerConnectionFactory factory;
  private EglBase eglBase;
  private Map<String, RtcPeerConnection> mapPeerConnection;
  private int localId;
  PeerConnectionFactory.Options options = null;
  private VideoSource videoSource;
  private boolean videoCallEnabled;
  private boolean preferIsac;
  private boolean preferPcma;
  private String preferredVideoCodec;
  private boolean videoSourceStopped;
  private boolean isError;
  private boolean isShowLocalVideo;
  private Timer statsTimer;
  //private RtcVideoViews videoViews = null;
  private List<PeerConnection.IceServer> iceServers;
  private MediaConstraints pcConstraints;
  private MediaConstraints videoConstraints;
  private MediaConstraints audioConstraints;
  private PeerConnectionParameters peerConnectionParameters;
  //
  private PeerConnectionEvents events;
  private MediaStream localMediaStream;
  private AudioTrack localAudioTrack;
  private int numberOfCameras;
  private VideoCapturerAndroid videoCapturer;
  // enableVideo is set to true if video should be rendered and sent.
  private boolean renderVideo;
  //add
  private VideoTrack localVideoTrack;

  /**
   * Peer connection parameters.
   */
  public static class PeerConnectionParameters {
    public final boolean videoCallEnabled;
    public final boolean loopback;
    public final boolean tracing;
    public final int videoWidth;
    public final int videoHeight;
    public final int videoFps;
    public final int videoStartBitrate;
    public final String videoCodec;
    public final boolean videoCodecHwAcceleration;
    public final boolean captureToTexture;
    public final int audioStartBitrate;
    public final String audioCodec;
    public final boolean noAudioProcessing;
    public final boolean aecDump;
    public final boolean useOpenSLES;

    public PeerConnectionParameters(
            boolean videoCallEnabled, boolean loopback, boolean tracing,
            int videoWidth, int videoHeight, int videoFps, int videoStartBitrate,
            String videoCodec, boolean videoCodecHwAcceleration, boolean captureToTexture,
            int audioStartBitrate, String audioCodec,
            boolean noAudioProcessing, boolean aecDump, boolean useOpenSLES) {
      this.videoCallEnabled = videoCallEnabled;
      this.loopback = loopback;
      this.tracing = tracing;
      this.videoWidth = videoWidth;
      this.videoHeight = videoHeight;
      this.videoFps = videoFps;
      this.videoStartBitrate = videoStartBitrate;
      this.videoCodec = videoCodec;
      this.videoCodecHwAcceleration = videoCodecHwAcceleration;
      this.captureToTexture = captureToTexture;
      this.audioStartBitrate = audioStartBitrate;
      this.audioCodec = audioCodec;
      this.noAudioProcessing = noAudioProcessing;
      this.aecDump = aecDump;
      this.useOpenSLES = useOpenSLES;
    }
  }

  /**
   * Peer connection events.
   */
  public static interface PeerConnectionEvents {
    /**
     * Callback fired once local SDP is created and set.
     */
    public void onLocalDescription(final String peerId, final SessionDescription sdp);

    /**
     * Callback fired once local Ice candidate is generated.
     */
    public void onIceCandidate(final String peerId, final IceCandidate candidate);

    /**
     * Callback fired once connection is established (IceConnectionState is
     * CONNECTED).
     */
    public void onIceConnected(final String peerId);

    /**
     * Callback fired once connection is closed (IceConnectionState is
     * DISCONNECTED).
     */
    public void onIceDisconnected(final String peerId);

    /**
     * Callback fired once peer connection is closed.
     */
    public void onPeerConnectionClosed(final String peerId);

    /**
     * Callback fired once peer connection statistics is ready.
     */
    public void onPeerConnectionStatsReady(final String peerId, final StatsReport[] reports);

    /**
     * Callback fired once peer connection error happened.
     */
    public void onPeerConnectionError(final String peerId, final String description);

    /**
     * Callback fired once open remote Render.
     */
    public void onOpenRemoteRender(final String peerId,final VideoTrack remoteTrack);

    /**
     * Callback fired once  close remote Render.
     */
    public void onRemoveRemoteRender(final String peerId);

    /**
     * Callback fired once  open local Renderer.
     */
    public void onOpenLocalRender(final VideoTrack localTrack);

    /**
     * Callback fired once  close local Renderer.
     */
    public void onRemoveLocalRender();
  }

  private PeerConnectionClients() {
    executor = new LooperExecutor();
    // Looper thread is started once in private ctor and is used for all
    // peer connection API calls to ensure new peer connection factory is
    // created on the same thread as previously destroyed factory.
    executor.requestStart();

    mapPeerConnection = new HashMap<String, RtcPeerConnection>();
    localId = 0;
  }

  public static PeerConnectionClients getInstance() {
    return instance;
  }

  public void setPeerConnectionFactoryOptions(PeerConnectionFactory.Options options) {
    this.options = options;
  }

  public void createPeerConnectionFactory(
      final Context context,
      final PeerConnectionParameters peerConnectionParameters,
      final PeerConnectionEvents events,
      final List<PeerConnection.IceServer> iceServers) {
    this.peerConnectionParameters = peerConnectionParameters;
    this.events = events;
    this.iceServers = iceServers;
    videoCallEnabled = peerConnectionParameters.videoCallEnabled;
    // Reset variables to initial states.
    factory = null;
    eglBase = null;
    preferIsac = false;
    preferPcma = false;
    videoSourceStopped = false;
    isError = false;
    localMediaStream = null;
    videoCapturer = null;
    renderVideo = true;
    statsTimer = new Timer();

    executor.execute(new Runnable() {
      @Override
      public void run() {
        createPeerConnectionFactoryInternal(context);
        createPeerConnectionCconstraints();
      }
    });
  }

  public void setEglBase(EglBase base,boolean showLocalVideo) {
    eglBase = base;
    isShowLocalVideo = showLocalVideo;
    if(eglBase != null && isShowLocalVideo) {
      setLocalVideoRender();
    }
  }

  public void setLocalVideoRender() {
    if (peerConnectionParameters == null) {
      Log.e(TAG, "Creating localStream without initializing factory.");
      return;
    }
    executor.execute(new Runnable() {
      @Override
      public void run() {
        createMediaConstraintsInternal();
      }
    });
  }

  public String createPeerConnection(
      final PeerConnectionMode mode, final EglBase.Context renderEGLContext) {
    if (peerConnectionParameters == null) {
      Log.e(TAG, "Creating peer connection without initializing factory.");
      return null;
    }
    final Exchanger<String> result = new Exchanger<String>();
    executor.execute(new Runnable() {
      @Override
      public void run() {
        String peerId = String.format("peer_%d", localId++);
        if(localMediaStream == null && mode != PeerConnectionMode.PCM_RECV_ONLY) {
          createMediaConstraintsInternal();
        }
        PeerConnection peerConnection = createPeerConnectionInternal(peerId, mode, renderEGLContext);
        if(peerConnection != null) {
          RtcPeerConnection rtcPeerConnection = new RtcPeerConnection();
          rtcPeerConnection.pc = peerConnection;
          rtcPeerConnection.isInitiator = false;
          rtcPeerConnection.queuedRemoteCandidates = new LinkedList<IceCandidate>();
          rtcPeerConnection.localSdp = null; // either offer or answer SDP
          rtcPeerConnection.sdpObserver = new SDPObserver(peerId);
          rtcPeerConnection.dcObserver = new DCObserver(peerId);
          rtcPeerConnection.mode = mode;
          synchronized (mapPeerConnection) {
            mapPeerConnection.put(peerId, rtcPeerConnection);
          }
        }

        LooperExecutor.exchange(result, peerId);
      }
    });

    return LooperExecutor.exchange(result, null);
  }

  public boolean closePeerConnection(final String peerId) {
    final Exchanger<Boolean> result = new Exchanger<Boolean>();
    executor.execute(new Runnable() {
      @Override
      public void run() {
        synchronized (mapPeerConnection) {
          RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
          if (rtcPeerConnection != null) {
            if (rtcPeerConnection.pc != null) {
              if (rtcPeerConnection.mode != PeerConnectionMode.PCM_RECV_ONLY && localMediaStream != null)
                rtcPeerConnection.pc.removeStream(localMediaStream);
              rtcPeerConnection.pc.dispose();
              rtcPeerConnection.pc = null;
            }
            events.onRemoveRemoteRender(peerId);
            rtcPeerConnection.sdpObserver = null;
            rtcPeerConnection.dcObserver = null;
            rtcPeerConnection.queuedRemoteCandidates = null;

            mapPeerConnection.remove(peerId);
          }
          if(!isShowLocalVideo && localMediaStream != null) {
            boolean closed = true;
            Iterator iter = mapPeerConnection.entrySet().iterator();
            while (iter.hasNext()) {
              Map.Entry entry = (Map.Entry) iter.next();
              String peerId = (String) entry.getKey();
              RtcPeerConnection val = (RtcPeerConnection) entry.getValue();
              if(val.mode != PeerConnectionMode.PCM_RECV_ONLY) {
                closed = false;
                break;
              }
            }
            if(closed) {
              if(localVideoTrack != null) {
                events.onRemoveLocalRender();
                localVideoTrack = null;
              }
              localAudioTrack = null;
              localMediaStream.dispose();
              localMediaStream = null;

              if(videoCapturer != null) {
                videoCapturer = null;
              }
              if (videoSource != null) {
                videoSource.dispose();
                videoSource = null;
              }
            }
          }
        }
        LooperExecutor.exchange(result, true);
      }
    });

    return LooperExecutor.exchange(result, false);
  }

  public void close() {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        closeInternal();
      }
    });
  }

  public boolean isVideoCallEnabled() {
    return videoCallEnabled;
  }

  private void createPeerConnectionFactoryInternal(Context context) {
    Log.d(TAG, "Create peer connection factory. Use video: " +
        peerConnectionParameters.videoCallEnabled);
    isError = false;

    // Initialize field trials.
    PeerConnectionFactory.initializeFieldTrials(FIELD_TRIAL_AUTOMATIC_RESIZE);

    // Check preferred video codec.
    preferredVideoCodec = VIDEO_CODEC_VP8;
    if (videoCallEnabled && peerConnectionParameters.videoCodec != null) {
      if (peerConnectionParameters.videoCodec.equals(VIDEO_CODEC_VP9)) {
        preferredVideoCodec = VIDEO_CODEC_VP9;
      } else if (peerConnectionParameters.videoCodec.equals(VIDEO_CODEC_H264)) {
        preferredVideoCodec = VIDEO_CODEC_H264;
      }
    }
    Log.d(TAG, "Pereferred video codec: " + preferredVideoCodec);

    // Check if ISAC is used by default.
    // Check if PCMA is used by default.
    preferIsac = false;
    preferPcma = false;
    if(peerConnectionParameters.audioCodec != null) {
      if (peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_ISAC)) {
        preferIsac = true;
      }
      if (peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_PCMA)) {
        preferPcma = true;
      }
    }

    // Enable/disable OpenSL ES playback.
    if (!peerConnectionParameters.useOpenSLES) {
      Log.d(TAG, "Disable OpenSL ES audio even if device supports it");
      WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(true /* enable */);
    } else {
      Log.d(TAG, "Allow OpenSL ES audio if device supports it");
      WebRtcAudioManager.setBlacklistDeviceForOpenSLESUsage(false);
    }

    // Create peer connection factory.
    if (!PeerConnectionFactory.initializeAndroidGlobals(context, true, true,
        peerConnectionParameters.videoCodecHwAcceleration)) {
      events.onPeerConnectionError("sys", "Failed to initializeAndroidGlobals");
    }
    factory = new PeerConnectionFactory();
    if (options != null) {
      Log.d(TAG, "Factory networkIgnoreMask option: " + options.networkIgnoreMask);
      factory.setOptions(options);
    }
    Log.d(TAG, "Peer connection factory created.");
  }

  private void createPeerConnectionCconstraints() {
    // Create peer connection constraints.
    pcConstraints = new MediaConstraints();
    // Enable DTLS for normal calls and disable for loopback calls.
    if (peerConnectionParameters.loopback) {
      pcConstraints.optional.add(
          new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "false"));
    } else {
      pcConstraints.optional.add(
          new MediaConstraints.KeyValuePair(DTLS_SRTP_KEY_AGREEMENT_CONSTRAINT, "true"));
    }
    // Check if there is a camera on device and disable video call if not.
    numberOfCameras = CameraEnumerationAndroid.getDeviceCount();
    if (numberOfCameras == 0) {
      Log.w(TAG, "No camera on device. Switch to audio only call.");
      videoCallEnabled = false;
    }
    // Create video constraints if video call is enabled.
    if (videoCallEnabled) {
      videoConstraints = new MediaConstraints();
      int videoWidth = peerConnectionParameters.videoWidth;
      int videoHeight = peerConnectionParameters.videoHeight;

      // If VP8 HW video encoder is supported and video resolution is not
      // specified force it to HD.
      if ((videoWidth == 0 || videoHeight == 0)
              && peerConnectionParameters.videoCodecHwAcceleration
              && MediaCodecVideoEncoder.isVp8HwSupported()) {
        videoWidth = HD_VIDEO_WIDTH;
        videoHeight = HD_VIDEO_HEIGHT;
      }

      // Add video resolution constraints.
      if (videoWidth > 0 && videoHeight > 0) {
        videoWidth = Math.min(videoWidth, MAX_VIDEO_WIDTH);
        videoHeight = Math.min(videoHeight, MAX_VIDEO_HEIGHT);
        videoConstraints.mandatory.add(new KeyValuePair(
                MIN_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
        videoConstraints.mandatory.add(new KeyValuePair(
                MAX_VIDEO_WIDTH_CONSTRAINT, Integer.toString(videoWidth)));
        videoConstraints.mandatory.add(new KeyValuePair(
                MIN_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
        videoConstraints.mandatory.add(new KeyValuePair(
                MAX_VIDEO_HEIGHT_CONSTRAINT, Integer.toString(videoHeight)));
      }

      // Add fps constraints.
      int videoFps = peerConnectionParameters.videoFps;
      if (videoFps > 0) {
        videoFps = Math.min(videoFps, MAX_VIDEO_FPS);
        videoConstraints.mandatory.add(new KeyValuePair(
                MIN_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
        videoConstraints.mandatory.add(new KeyValuePair(
                MAX_VIDEO_FPS_CONSTRAINT, Integer.toString(videoFps)));
      }
    }

    // Create audio constraints.
    audioConstraints = new MediaConstraints();
    // added for audio performance measurements
    if (peerConnectionParameters.noAudioProcessing) {
      Log.d(TAG, "Disabling audio processing");
      audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
            AUDIO_ECHO_CANCELLATION_CONSTRAINT, "false"));
      audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
            AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
      audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
            AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
      audioConstraints.mandatory.add(new MediaConstraints.KeyValuePair(
           AUDIO_NOISE_SUPPRESSION_CONSTRAINT , "false"));
    }
  }

  private void createMediaConstraintsInternal( ) {
    localMediaStream = factory.createLocalMediaStream("ARDAMS");
    if (videoCallEnabled) {
      String cameraDeviceName = CameraEnumerationAndroid.getDeviceName(0);
      String frontCameraDeviceName =
              CameraEnumerationAndroid.getNameOfFrontFacingDevice();
      if (numberOfCameras > 1 && frontCameraDeviceName != null) {
        cameraDeviceName = frontCameraDeviceName;
      }
      Log.d(TAG, "Opening camera: " + cameraDeviceName);
      videoCapturer = VideoCapturerAndroid.create(cameraDeviceName, null,
              peerConnectionParameters.captureToTexture ? eglBase!=null?eglBase.getEglBaseContext():null : null);
      if (videoCapturer == null) {
        reportError("sys", "Failed to open camera");
        return;
      }
      localMediaStream.addTrack(createVideoTrack(videoCapturer));
    }

    localAudioTrack = factory.createAudioTrack(
            AUDIO_TRACK_ID,
            factory.createAudioSource(audioConstraints));
    localMediaStream.addTrack(localAudioTrack);
  }

  private PeerConnection createPeerConnectionInternal(String peerId, PeerConnectionMode mode, EglBase.Context renderEGLContext) {
    if (factory == null || isError) {
      Log.e(TAG, "Peerconnection factory is not created");
      return null;
    }
    Log.d(TAG, "Create peer connection.");

    Log.d(TAG, "PCConstraints: " + pcConstraints.toString());
    if (videoConstraints != null) {
      Log.d(TAG, "VideoConstraints: " + videoConstraints.toString());
    }

    if (videoCallEnabled) {
      Log.d(TAG, "EGLContext: " + renderEGLContext);
      factory.setVideoHwAccelerationOptions(renderEGLContext, renderEGLContext);
    }

    PeerConnection.RTCConfiguration rtcConfig =
        new PeerConnection.RTCConfiguration(iceServers);
    // TCP candidates are only useful when connecting to a server that supports
    // ICE-TCP.
    rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
    rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
    rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
    rtcConfig.audioJitterBufferMaxPackets = 250;
    // Use ECDSA encryption.
    rtcConfig.keyType = PeerConnection.KeyType.ECDSA;

    PeerConnection peerConnection = factory.createPeerConnection(
        rtcConfig, pcConstraints, new PCObserver(peerId));

    // Set default WebRTC tracing and INFO libjingle logging.
    // NOTE: this _must_ happen while |factory| is alive!
    Logging.enableTracing(
        "logcat:",
        EnumSet.of(Logging.TraceLevel.TRACE_DEFAULT),
        Logging.Severity.LS_INFO);

    if(localMediaStream != null && mode != PeerConnectionMode.PCM_RECV_ONLY)
      peerConnection.addStream(localMediaStream);

    Log.d(TAG, "Peer connection created.");
    return peerConnection;
  }

  private void closeInternal() {
    if (factory != null && peerConnectionParameters.aecDump) {
      factory.stopAecDump();
    }
    Log.d(TAG, "Closing peer connection.");
    {
      Iterator iter = mapPeerConnection.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        String peerId = (String) entry.getKey();
        RtcPeerConnection val = (RtcPeerConnection) entry.getValue();        
        events.onRemoveRemoteRender(peerId);
        if(val.pc != null) {
          if(val.mode != PeerConnectionMode.PCM_RECV_ONLY && localMediaStream != null)
            val.pc.removeStream(localMediaStream);
          val.pc.dispose();
          val.pc = null;
        }
        val.sdpObserver = null;
        val.dcObserver = null;
        val.queuedRemoteCandidates = null;
        Log.d(TAG, "Closing peer connection done.");
        events.onPeerConnectionClosed(peerId);
      }
      mapPeerConnection.clear();
    }

    if(localVideoTrack != null) {
      events.onRemoveLocalRender();
      localVideoTrack = null;
    }

    if(localMediaStream != null) {
      localMediaStream.dispose();
      localMediaStream = null;
    }
    Log.d(TAG, "Closing video source.");
    if(videoCapturer != null) {
      videoCapturer = null;
    }
    if (videoSource != null) {
      videoSource.dispose();
      videoSource = null;
    }
    Log.d(TAG, "Closing peer connection factory.");
    if (factory != null) {
      factory.dispose();
      factory = null;
    }
    options = null;
    PeerConnectionFactory.stopInternalTracingCapture();
    PeerConnectionFactory.shutdownInternalTracer();
  }

  public boolean isHDVideo() {
    if (!videoCallEnabled) {
      return false;
    }
    int minWidth = 0;
    int minHeight = 0;
    for (KeyValuePair keyValuePair : videoConstraints.mandatory) {
      if (keyValuePair.getKey().equals("minWidth")) {
        try {
          minWidth = Integer.parseInt(keyValuePair.getValue());
        } catch (NumberFormatException e) {
          Log.e(TAG, "Can not parse video width from video constraints");
        }
      } else if (keyValuePair.getKey().equals("minHeight")) {
        try {
          minHeight = Integer.parseInt(keyValuePair.getValue());
        } catch (NumberFormatException e) {
          Log.e(TAG, "Can not parse video height from video constraints");
        }
      }
    }
    if (minWidth * minHeight >= 1280 * 720) {
      return true;
    } else {
      return false;
    }
  }

  private void getStats() {
    synchronized (mapPeerConnection) {
      Iterator iter = mapPeerConnection.entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry entry = (Map.Entry) iter.next();
        final String peerId = (String)entry.getKey();
        RtcPeerConnection rtcPeerConnection = (RtcPeerConnection) entry.getValue();
        if(rtcPeerConnection.mode != PeerConnectionMode.PCM_SEND_ONLY) {
          boolean success = rtcPeerConnection.pc.getStats(new StatsObserver() {
            @Override
            public void onComplete(final StatsReport[] reports) {
              events.onPeerConnectionStatsReady(peerId, reports);
            }
          }, null);
          if (!success) {
            Log.e(TAG, "getStats() returns false!");
          }
        }
      }
    }
  }

  public void enableStatsEvents(boolean enable, int periodMs) {
    if (enable) {
      try {
        if(statsTimer == null)
          statsTimer = new Timer();
        statsTimer.schedule(new TimerTask() {
          @Override
          public void run() {
            executor.execute(new Runnable() {
              @Override
              public void run() {
                getStats();
              }
            });
          }
        }, 0, periodMs);
      } catch (Exception e) {
        Log.e(TAG, "Can not schedule statistics timer", e);
      }
      Log.i(TAG, "StatsTimer.start()");
    } else {
      Log.i(TAG, "StatsTimer.cancel()");
      if(statsTimer != null) {
        statsTimer.cancel();
        statsTimer = null;
      }
    }
  }

  public boolean getLocalAudioEnabled() {
    final Exchanger<Boolean> result = new Exchanger<Boolean>();
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if(localAudioTrack != null) {
          LooperExecutor.exchange(result, localAudioTrack.enabled());
        } else {
          LooperExecutor.exchange(result, false);
        }
      }
    });
    return LooperExecutor.exchange(result, false);
  }
  public void setLocalAudioEnabled(final boolean enable) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if(localAudioTrack != null) {
          localAudioTrack.setEnabled(enable);
        }
      }
    });
  }

  public boolean getLocalVideoEnabled() {
    final Exchanger<Boolean> result = new Exchanger<Boolean>();
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if(localVideoTrack != null) {
          LooperExecutor.exchange(result, localVideoTrack.enabled());
        } else {
          LooperExecutor.exchange(result, false);
        }
      }
    });
    return LooperExecutor.exchange(result, false);
  }
  public void setLocalVideoEnabled(final boolean enable) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        //add
         if(localVideoTrack!=null){
           localVideoTrack.setEnabled(enable);
         }
      }
    });
  }

  public void createOffer(final String peerId) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
        if (rtcPeerConnection != null && !isError) {
          Log.d(TAG, "PC Create OFFER");
          rtcPeerConnection.isInitiator = true;
          MediaConstraints sdpMediaConstraints = new MediaConstraints();
          if(rtcPeerConnection.mode == PeerConnectionMode.PCM_SEND_ONLY) {
            sdpMediaConstraints.mandatory.add(new KeyValuePair(
                    "OfferToReceiveVideo", "false"));
            sdpMediaConstraints.mandatory.add(new KeyValuePair(
                    "OfferToReceiveAudio", "false"));
          } else {
            if(videoCallEnabled)
              sdpMediaConstraints.mandatory.add(new KeyValuePair(
                    "OfferToReceiveVideo", "true"));

            sdpMediaConstraints.mandatory.add(new KeyValuePair(
                    "OfferToReceiveAudio", "true"));
          }
          if(false) {
            DataChannel.Init init = new DataChannel.Init();
            init.ordered = true;
            rtcPeerConnection.dc = rtcPeerConnection.pc.createDataChannel(
                    "__CMD_ANYRTC_", init);
            rtcPeerConnection.dc.registerObserver(rtcPeerConnection.dcObserver);
          }
          rtcPeerConnection.pc.createOffer(rtcPeerConnection.sdpObserver, sdpMediaConstraints);
        }
      }
    });
  }

  public void createAnswer(final String peerId) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
        if (rtcPeerConnection != null && !isError) {
          Log.d(TAG, "PC create ANSWER");
          rtcPeerConnection.isInitiator = false;
          MediaConstraints sdpMediaConstraints = new MediaConstraints();
          if(rtcPeerConnection.mode == PeerConnectionMode.PCM_SEND_ONLY) {
            sdpMediaConstraints.mandatory.add(new KeyValuePair(
                    "OfferToReceiveVideo", "false"));
            sdpMediaConstraints.mandatory.add(new KeyValuePair(
                    "OfferToReceiveAudio", "false"));
          } else {
            if(videoCallEnabled)
              sdpMediaConstraints.mandatory.add(new KeyValuePair(
                    "OfferToReceiveVideo", "true"));

            sdpMediaConstraints.mandatory.add(new KeyValuePair(
                      "OfferToReceiveAudio", "true"));
          }
          rtcPeerConnection.pc.createAnswer(rtcPeerConnection.sdpObserver, sdpMediaConstraints);
        }
      }
    });
  }

  public void addRemoteIceCandidate(final String peerId, final IceCandidate candidate) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
        if (rtcPeerConnection != null && !isError) {
          if (rtcPeerConnection.queuedRemoteCandidates != null) {
            rtcPeerConnection.queuedRemoteCandidates.add(candidate);
          } else {
            rtcPeerConnection.pc.addIceCandidate(candidate);
          }
        }
      }
    });
  }

  public void setRemoteDescription(final String peerId, final SessionDescription sdp) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
        if (rtcPeerConnection == null || isError) {
          return;
        }
        String sdpDescription = sdp.description;
        if (preferIsac) {
          sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
        } else if(preferPcma) {
          sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_PCMA, true);
        }
        if (videoCallEnabled) {
          sdpDescription = preferCodec(sdpDescription, preferredVideoCodec, false);
        }
        if(rtcPeerConnection.mode != PeerConnectionMode.PCM_RECV_ONLY) {
          if (videoCallEnabled && peerConnectionParameters.videoStartBitrate > 0) {
            sdpDescription = setStartBitrate(VIDEO_CODEC_VP8, true,
                    sdpDescription, peerConnectionParameters.videoStartBitrate);
            sdpDescription = setStartBitrate(VIDEO_CODEC_VP9, true,
                    sdpDescription, peerConnectionParameters.videoStartBitrate);
            sdpDescription = setStartBitrate(VIDEO_CODEC_H264, true,
                    sdpDescription, peerConnectionParameters.videoStartBitrate);
          }
          if (peerConnectionParameters.audioStartBitrate > 0 && peerConnectionParameters.audioCodec.equals(AUDIO_CODEC_OPUS)) {
            sdpDescription = setStartBitrate(AUDIO_CODEC_OPUS, false,
                    sdpDescription, peerConnectionParameters.audioStartBitrate);
          }
        }
        Log.d(TAG, "Set remote SDP.");
        SessionDescription sdpRemote = new SessionDescription(
            sdp.type, sdpDescription);
        rtcPeerConnection.pc.setRemoteDescription(rtcPeerConnection.sdpObserver, sdpRemote);
      }
    });
  }

  public void stopVideoSource() {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (videoSource != null && !videoSourceStopped) {
          Log.d(TAG, "Stop video source.");
          videoSource.stop();
          videoSourceStopped = true;
        }
      }
    });
  }

  public void startVideoSource() {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (videoSource != null && videoSourceStopped) {
          Log.d(TAG, "Restart video source.");
          videoSource.restart();
          videoSourceStopped = false;
        }
      }
    });
  }

  private void reportError(final String peerId, final String errorMessage) {
    Log.e(TAG, "Peerconnection error: " + errorMessage);
    executor.execute(new Runnable() {
      @Override
      public void run() {
        if (!isError) {
          events.onPeerConnectionError(peerId, errorMessage);
          isError = true;
        }
      }
    });
  }

  private VideoTrack createVideoTrack(VideoCapturerAndroid capturer ) {
    videoSource = factory.createVideoSource(capturer, videoConstraints);

    localVideoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
    localVideoTrack.setEnabled(renderVideo);
    //add
    events.onOpenLocalRender(localVideoTrack);
    return localVideoTrack;
  }

  private static String setStartBitrate(String codec, boolean isVideoCodec,
      String sdpDescription, int bitrateKbps) {
    String[] lines = sdpDescription.split("\r\n");
    int rtpmapLineIndex = -1;
    boolean sdpFormatUpdated = false;
    String codecRtpMap = null;
    // Search for codec rtpmap in format
    // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
    String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
    Pattern codecPattern = Pattern.compile(regex);
    for (int i = 0; i < lines.length; i++) {
      Matcher codecMatcher = codecPattern.matcher(lines[i]);
      if (codecMatcher.matches()) {
        codecRtpMap = codecMatcher.group(1);
        rtpmapLineIndex = i;
        break;
      }
    }
    if (codecRtpMap == null) {
      Log.w(TAG, "No rtpmap for " + codec + " codec");
      return sdpDescription;
    }
    Log.d(TAG, "Found " +  codec + " rtpmap " + codecRtpMap
        + " at " + lines[rtpmapLineIndex]);

    // Check if a=fmtp string already exist in remote SDP for this codec and
    // update it with new bitrate parameter.
    regex = "^a=fmtp:" + codecRtpMap + " \\w+=\\d+.*[\r]?$";
    codecPattern = Pattern.compile(regex);
    for (int i = 0; i < lines.length; i++) {
      Matcher codecMatcher = codecPattern.matcher(lines[i]);
      if (codecMatcher.matches()) {
        Log.d(TAG, "Found " +  codec + " " + lines[i]);
        if (isVideoCodec) {
          lines[i] += "; " + VIDEO_CODEC_PARAM_MAX_BITRATE
              + "=" + bitrateKbps;
          lines[i] += "; " + VIDEO_CODEC_PARAM_MIN_BITRATE
                  + "=" + bitrateKbps/2.5;
        } else {
          lines[i] += "; " + AUDIO_CODEC_PARAM_BITRATE
              + "=" + (bitrateKbps * 1000);
        }
        Log.d(TAG, "Update remote SDP line: " + lines[i]);
        sdpFormatUpdated = true;
        break;
      }
    }

    StringBuilder newSdpDescription = new StringBuilder();
    for (int i = 0; i < lines.length; i++) {
      newSdpDescription.append(lines[i]).append("\r\n");
      // Append new a=fmtp line if no such line exist for a codec.
      if (!sdpFormatUpdated && i == rtpmapLineIndex) {
        String bitrateSet;
        if (isVideoCodec) {
          bitrateSet = "a=fmtp:" + codecRtpMap + " "
              + VIDEO_CODEC_PARAM_MAX_BITRATE + "=" + bitrateKbps;
          bitrateSet += "; " + VIDEO_CODEC_PARAM_MIN_BITRATE
                  + "=" + bitrateKbps/2.5;
        } else {
          bitrateSet = "a=fmtp:" + codecRtpMap + " "
              + AUDIO_CODEC_PARAM_BITRATE + "=" + (bitrateKbps * 1000);
        }
        Log.d(TAG, "Add remote SDP line: " + bitrateSet);
        newSdpDescription.append(bitrateSet).append("\r\n");
      }

    }
    return newSdpDescription.toString();
  }

  private static String preferCodec(
      String sdpDescription, String codec, boolean isAudio) {
    String[] lines = sdpDescription.split("\r\n");
    int mLineIndex = -1;
    String codecRtpMap = null;
    // a=rtpmap:<payload type> <encoding name>/<clock rate> [/<encoding parameters>]
    String regex = "^a=rtpmap:(\\d+) " + codec + "(/\\d+)+[\r]?$";
    Pattern codecPattern = Pattern.compile(regex);
    String mediaDescription = "m=video ";
    if (isAudio) {
      mediaDescription = "m=audio ";
    }
    for (int i = 0; (i < lines.length)
        && (mLineIndex == -1 || codecRtpMap == null); i++) {
      if (lines[i].startsWith(mediaDescription)) {
        mLineIndex = i;
        continue;
      }
      Matcher codecMatcher = codecPattern.matcher(lines[i]);
      if (codecMatcher.matches()) {
        codecRtpMap = codecMatcher.group(1);
        continue;
      }
    }
    if (mLineIndex == -1) {
      Log.w(TAG, "No " + mediaDescription + " line, so can't prefer " + codec);
      return sdpDescription;
    }
    if (codecRtpMap == null) {
      Log.w(TAG, "No rtpmap for " + codec);
      return sdpDescription;
    }
    Log.d(TAG, "Found " +  codec + " rtpmap " + codecRtpMap + ", prefer at "
        + lines[mLineIndex]);
    String[] origMLineParts = lines[mLineIndex].split(" ");
    if (origMLineParts.length > 3) {
      StringBuilder newMLine = new StringBuilder();
      int origPartIndex = 0;
      // Format is: m=<media> <port> <proto> <fmt> ...
      newMLine.append(origMLineParts[origPartIndex++]).append(" ");
      newMLine.append(origMLineParts[origPartIndex++]).append(" ");
      newMLine.append(origMLineParts[origPartIndex++]).append(" ");
      newMLine.append(codecRtpMap);
      for (; origPartIndex < origMLineParts.length; origPartIndex++) {
        if (!origMLineParts[origPartIndex].equals(codecRtpMap)) {
          newMLine.append(" ").append(origMLineParts[origPartIndex]);
        }
      }
      lines[mLineIndex] = newMLine.toString();
      Log.d(TAG, "Change media description: " + lines[mLineIndex]);
    } else {
      Log.e(TAG, "Wrong SDP media description format: " + lines[mLineIndex]);
    }
    StringBuilder newSdpDescription = new StringBuilder();
    for (String line : lines) {
      newSdpDescription.append(line).append("\r\n");
    }
    return newSdpDescription.toString();
  }

  private void switchCameraInternal() {
    if (!videoCallEnabled || numberOfCameras < 2 || isError || videoCapturer == null) {
      Log.e(TAG, "Failed to switch camera. Video: " + videoCallEnabled + ". Error : "
          + isError + ". Number of cameras: " + numberOfCameras);
      return;  // No video is sent or only one camera is available or error happened.
    }
    Log.d(TAG, "Switch camera");
    videoCapturer.switchCamera(null);
  }

  public void switchCamera() {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        switchCameraInternal();
      }
    });
  }

  public void changeCaptureFormat(final int width, final int height, final int framerate) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        changeCaptureFormatInternal(width, height, framerate);
      }
    });
  }

  private void changeCaptureFormatInternal(int width, int height, int framerate) {
    if (!videoCallEnabled || isError || videoCapturer == null) {
      Log.e(TAG, "Failed to change capture format. Video: " + videoCallEnabled + ". Error : "
          + isError);
      return;
    }
    videoCapturer.onOutputFormatRequest(width, height, framerate);
  }

  // Implementation detail: observe ICE & stream changes and react accordingly.
  private class PCObserver implements PeerConnection.Observer {
    private String peerId;
    public PCObserver(String _id) {
      peerId = _id;
    }
    @Override
    public void onIceCandidate(final IceCandidate candidate){
      executor.execute(new Runnable() {
        @Override
        public void run() {
          events.onIceCandidate(peerId, candidate);
        }
      });
    }

    @Override
    public void onSignalingChange(
        PeerConnection.SignalingState newState) {
      Log.d(TAG, "SignalingState: " + newState);
    }

    @Override
    public void onIceConnectionChange(
        final IceConnectionState newState) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          Log.d(TAG, "IceConnectionState: " + newState);
          if (newState == IceConnectionState.CONNECTED) {
            events.onIceConnected(peerId);
          } else if (newState == IceConnectionState.DISCONNECTED) {
            events.onIceDisconnected(peerId);
          } else if (newState == IceConnectionState.FAILED) {
            //reportError(peerId, "ICE connection failed.");
          }
        }
      });
    }

    @Override
    public void onIceGatheringChange(
      PeerConnection.IceGatheringState newState) {
      Log.d(TAG, "IceGatheringState: " + newState);
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
      Log.d(TAG, "IceConnectionReceiving changed to " + receiving);
    }

    @Override
    public void onAddStream(final MediaStream stream){
      executor.execute(new Runnable() {
        @Override
        public void run() {
          RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
          if (rtcPeerConnection == null || isError) {
            return;
          }
          if (stream.audioTracks.size() > 1 || stream.videoTracks.size() > 1) {
            reportError(peerId, "Weird-looking stream: " + stream);
            return;
          }
          if (stream.videoTracks.size() == 1) {
            VideoTrack remoteVideoTrack = stream.videoTracks.get(0);
            remoteVideoTrack.setEnabled(renderVideo);
            events.onOpenRemoteRender(peerId,remoteVideoTrack);
          }
        }
      });
    }

    @Override
    public void onRemoveStream(final MediaStream stream){
      executor.execute(new Runnable() {
        @Override
        public void run() {
          RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
          if (rtcPeerConnection == null || isError) {
            return;
          }

          events.onRemoveRemoteRender(peerId);
        }
      });
    }

    @Override
    public void onDataChannel(final DataChannel dc) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
          if (rtcPeerConnection == null || isError) {
            return;
          }
          rtcPeerConnection.dc = dc;
          rtcPeerConnection.dc.registerObserver(rtcPeerConnection.dcObserver);
        }
      });
    }

    @Override
    public void onRenegotiationNeeded() {
      // No need to do anything; AppRTC follows a pre-agreed-upon
      // signaling/negotiation protocol.
    }
  }

  // Implementation detail: handle offer creation/signaling and answer setting,
  // as well as adding remote ICE candidates once the answer SDP is set.
  private class SDPObserver implements SdpObserver {
    private String peerId;
    public SDPObserver(String _id) {
      peerId = _id;
    }
    @Override
    public void onCreateSuccess(final SessionDescription origSdp) {
      RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
      if (rtcPeerConnection == null || isError) {
        return;
      }
      if (rtcPeerConnection.localSdp != null) {
        reportError(peerId, "Multiple SDP create.");
        return;
      }
      String sdpDescription = origSdp.description;
      if (preferIsac) {
        sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_ISAC, true);
      }else if(preferPcma) {
        sdpDescription = preferCodec(sdpDescription, AUDIO_CODEC_PCMA, true);
      }
      if (videoCallEnabled) {
        sdpDescription = preferCodec(sdpDescription, preferredVideoCodec, false);
      }
      final SessionDescription sdp = new SessionDescription(
          origSdp.type, sdpDescription);
      rtcPeerConnection.localSdp = sdp;
      executor.execute(new Runnable() {
        @Override
        public void run() {
          RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
          if (rtcPeerConnection != null && !isError) {
            Log.d(TAG, "Set local SDP from " + sdp.type);
            rtcPeerConnection.pc.setLocalDescription(rtcPeerConnection.sdpObserver, sdp);
          }
        }
      });
    }

    @Override
    public void onSetSuccess() {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
          if (rtcPeerConnection == null || isError) {
            return;
          }
          if (rtcPeerConnection.isInitiator) {
            // For offering peer connection we first create offer and set
            // local SDP, then after receiving answer set remote SDP.
            if (rtcPeerConnection.pc.getRemoteDescription() == null) {
              // We've just set our local SDP so time to send it.
              Log.d(TAG, "Local SDP set succesfully");
              events.onLocalDescription(peerId, rtcPeerConnection.localSdp);
            } else {
              // We've just set remote description, so drain remote
              // and send local ICE candidates.
              Log.d(TAG, "Remote SDP set succesfully");
              rtcPeerConnection.drainCandidates();
            }
          } else {
            // For answering peer connection we set remote SDP and then
            // create answer and set local SDP.
            if (rtcPeerConnection.pc.getLocalDescription() != null) {
              // We've just set our local SDP so time to send it, drain
              // remote and send local ICE candidates.
              Log.d(TAG, "Local SDP set succesfully");
              events.onLocalDescription(peerId, rtcPeerConnection.localSdp);
              rtcPeerConnection.drainCandidates();
            } else {
              // We've just set remote SDP - do nothing for now -
              // answer will be created soon.
              Log.d(TAG, "Remote SDP set succesfully");
            }
          }
        }
      });
    }

    @Override
    public void onCreateFailure(final String error) {
      reportError(peerId, "createSDP error: " + error);
    }

    @Override
    public void onSetFailure(final String error) {
      reportError(peerId, "setSDP error: " + error);
    }
  }

  // Implementation detail: data channel changes and react
  // accordingly.
  private class DCObserver implements DataChannel.Observer {
    private String peerId;
    public DCObserver(String _id) {
      peerId = _id;
    }
    @Override
    public void onMessage(DataChannel.Buffer msg) {
      // TODO Auto-generated method stub
      String message = AnyRTC.byteBufferToString(msg.data);
      Log.e(TAG, "Data channel recv: " + message);
    }

    @Override
    public void onStateChange() {
      // TODO Auto-generated method stub
      executor.execute(new Runnable() {
                         @Override
                         public void run() {
                           RtcPeerConnection rtcPeerConnection = mapPeerConnection.get(peerId);
                           if (rtcPeerConnection == null || isError) {
                             return;
                           }
                           if (rtcPeerConnection.dc.state() == DataChannel.State.OPEN) {
                             Log.d("", "");
                             String text = "1234";
                             rtcPeerConnection.dc.send(new DataChannel.Buffer(ByteBuffer.wrap(text.getBytes()), false));
                             //mEvents.onPeerDataChannelOpened(mPeerId);
                           } else if (rtcPeerConnection.dc.state() == DataChannel.State.CLOSED) {
                             //mEvents.onPeerDataChannelClosed(mPeerId);
                           }
                         }
                       });

    }

    @Override
    public void onBufferedAmountChange(long arg0) {
      // TODO Auto-generated method stub

    }

  }
}

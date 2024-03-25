package com.example.asrandroidclient.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRecorder
import com.example.asrandroidclient.MyApp
import com.example.asrandroidclient.media.audio.AudioRecorder
import com.orhanobut.logger.Logger
import com.ys.rkapi.MyManager
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WebRtcManager(context: Context) : SdpObserver {

    companion object {
        const val AUDIO_ECHO_CANCELLATION_CONSTRAINT: String = "googEchoCancellation"
        const val AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl"
        const val AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter"
        const val AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression"
        const val VIDEO_TRACK_ID = "ARDAMSv0"
        const val AUDIO_TRACK_ID = "ARDAMSa0"
    }

    private var factory: PeerConnectionFactory? = null
    private var localAudioTrack: AudioTrack? = null
    private var peerConnection: PeerConnection? = null

    private var localStream: MediaStream? = null
    private var audioSource: AudioSource? = null
    private var remoteStream: MediaStream? = null
    private var localSdp: SessionDescription? = null


    var mRootEglBase: EglBase? = null

    // 发送者 接电话的一方
    var isOffer = true

    var executor: ExecutorService? = null

    private var networkMonitor: NetworkMonitor? = null
    private var audioManager: AudioManager? = null

    private var context: Context

    init {
        executor = Executors.newSingleThreadExecutor()
        createConnectFactory(context)
        mRootEglBase = EglBase.create()
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        networkMonitor = NetworkMonitor.getInstance()
        this.context = context
    }


    /**
     * 生成factory
     */
    private fun createConnectFactory(context: Context): PeerConnectionFactory? {
        Logger.i("初始化webrtc")
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)    //启用内部追踪器，用来记录一些相关数据
                .createInitializationOptions()
        )
        // -----------创建PeerConnectionFactory
        val adm =
            JavaAudioDeviceModule.builder(context)
                .setSampleRate(AudioRecorder.SAMPLE_RATE_IN_HZ)
                .setAudioFormat(AudioRecorder.AUDIO_FORMAT)
                .setAudioSource(MediaRecorder.AudioSource.MIC).createAudioDeviceModule()
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        for (dev in devices) {
            if (dev.type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                adm.setPreferredInputDevice(dev)
                Logger.i("设置音频源：${dev.type}")
            }
        }
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        encoderFactory = DefaultVideoEncoderFactory(
            mRootEglBase?.getEglBaseContext(),
            true,
            true
        )
        decoderFactory = DefaultVideoDecoderFactory(mRootEglBase?.getEglBaseContext())


        val options = PeerConnectionFactory.Options()
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setVideoDecoderFactory(decoderFactory)
            .setVideoEncoderFactory(encoderFactory)
            .setAudioDeviceModule(adm)    //设置音频采集和播放使用的配置,当前使用java中的audioTrack 和audioRecord
            .createPeerConnectionFactory()

        Logger.i("createConnectFactory done")
        return factory
    }

    /**
     * 创建peer连接
     */
    fun createPeerConnect() {
        if (factory == null) {
            createConnectFactory(context)
        }
        if (peerConnection == null) {
            //ice服务器列表
            val iceServers: MutableList<PeerConnection.IceServer> = ArrayList()
            //添加一个turn服务器,turn服务器主要用户下面的stun服务器打洞失败的时候使用这个turn服务器转发数据流，可以添加多个
            iceServers.add(
                PeerConnection.IceServer.builder("stun:stun.l.google.com:19302") //这是你服务器的地址
//                    .setUsername("sanjibao") //用户名
//                    .setPassword("sanjibao1119") //密码
                    .createIceServer()
            )
            iceServers.add(
                PeerConnection.IceServer.builder("stun:39.108.177.117:3478?transport=udp") //这是你服务器的地址
//                    .setUsername("sanjibao") //用户名
//                    .setPassword("sanjibao1119") //密码
                    .createIceServer()
            )
            iceServers.add(
                PeerConnection.IceServer.builder("turn:39.108.177.117:3478") //这是你服务器的地址
                    .setUsername("sanjibao") //用户名
                    .setPassword("sanjibao1119") //密码
                    .createIceServer()
            )

            peerConnection =
                factory?.createPeerConnection(iceServers, object : PeerConnection.Observer {
                    override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {}
                    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
                        //ICE 连接状态变化后回调
                    }

                    override fun onIceConnectionReceivingChange(b: Boolean) {

                    }

                    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {

                    }

                    override fun onIceCandidate(iceCandidate: IceCandidate) {
                        //自动请求stun/turn服务器后回调这个方法
                        //发送Ice信息给对端用户 ,下面的代码只是用于发送信息给远端用户，我使用的是websocket，自己可以用其他方式实现。最后结尾我会给出服务器端的代码。
                        MyApp.webrtcSocketManager.icecandidate(iceCandidate)
                        Logger.d("发送iceCandidate")

                    }

                    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {}
                    override fun onAddStream(mediaStream: MediaStream) {
                        Logger.d("onAddStream")
                        //收到远端数据流信息
                        //mediaStream.videoTracks[0].addSink(remoteRender)  //视频流信息
                        mediaStream.audioTracks[0].setEnabled(true)
                        remoteStream = mediaStream
                    }

                    override fun onRemoveStream(mediaStream: MediaStream) {}
                    override fun onDataChannel(dataChannel: DataChannel) {}
                    override fun onRenegotiationNeeded() {}
                    override fun onAddTrack(
                        rtpReceiver: RtpReceiver,
                        mediaStreams: Array<MediaStream>
                    ) {
                    }
                })
        }
    }

    /**
     * 创建本地媒体流
     */
    fun createLocalStream() {
        if (localStream == null) {
            localStream = factory?.createLocalMediaStream("ARDAMS")
            audioSource = factory?.createAudioSource(createAudioConstraints())
            localAudioTrack = factory?.createAudioTrack(AUDIO_TRACK_ID, audioSource)
            localStream?.addTrack(localAudioTrack)
        }
    }

    /**
     * 将本地媒体流添加到peer
     */
    fun addLocalStream() {
        audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
        peerConnection?.addStream(localStream)
    }

    /**
     * 添加ice,从信令服务器获取
     */
    fun addIce(candidate: IceCandidate) {
        peerConnection?.addIceCandidate(candidate)
    }

    // 创建offer
    fun createOffer() {
        peerConnection?.createOffer(this, offerOrAnswerConstraint())
    }

    // 创建answer
    fun createAnswer() {
        peerConnection?.createAnswer(this, offerOrAnswerConstraint())
    }

    fun setRemoteDescription(sdp: SessionDescription?) {
        peerConnection?.setRemoteDescription(this, sdp)
    }

    private fun setLocalDescription(sdp: SessionDescription?) {
        peerConnection?.setLocalDescription(this, sdp)
    }


    private fun offerOrAnswerConstraint(): MediaConstraints? {
        val mediaConstraints = MediaConstraints()
        val keyValuePairs = java.util.ArrayList<MediaConstraints.KeyValuePair>()
        keyValuePairs.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        mediaConstraints.mandatory.addAll(keyValuePairs)
        return mediaConstraints
    }

    private fun createAudioConstraints(): MediaConstraints {
        val audioConstraints = MediaConstraints()
        Logger.i("修改模块无法收音问题1")
        //回声消除
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true")
        )
        // 自动增益
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "true")
        )
        //高音过滤
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "true")
        )
        //噪音处理
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true")
        )
        return audioConstraints
    }

    /**
     * 释放webrtc相关资源
     */
    fun release() {
        //networkMonitor?.removeObserver(this)
        executor?.execute {
            audioManager?.mode = AudioManager.MODE_NORMAL
            // audio 释放
            audioSource?.dispose()
            audioSource = null
            localStream?.dispose()
            localStream = null
            remoteStream?.dispose()
            remoteStream = null
            peerConnection?.close()
            peerConnection = null
            factory?.dispose()
            factory = null
        }
    }


    // ------------------------------------SdpObserver-----------------------------------

    override fun onCreateSuccess(p0: SessionDescription?) {
        Logger.i("sdp创建成功")
        val sdp = SessionDescription(p0?.type, p0?.description)
        localSdp = sdp
        executor?.execute {
            setLocalDescription(sdp)
//            App.socketEventViewModel.sendOffer(sdp)
        }

    }

    override fun onSetSuccess() {
        Logger.i("sdp连接成功")
        executor?.execute {
            localSdp?.let {
                if (isOffer) {
                    if (peerConnection?.remoteDescription == null) {
                        Logger.i("发送offer:${it.description}")
                        //发送者发送自己的offer
                        MyApp.webrtcSocketManager.sendOffer(it)
                    } else {
                        Logger.i("remote sdp set successfully")
                    }
                } else {
                    if (peerConnection?.localDescription != null) {
                        Logger.i("local sdp set successfully")
                        Logger.i("发送answer:${it.description}")
                        //接收者发送自己的answer
                        MyApp.webrtcSocketManager.sendAnswer(it)
                    } else {
                        Logger.i("remote sdp set successfully")
                    }
                }
            }

        }
    }

    override fun onCreateFailure(p0: String?) {
        Logger.i("sdp创建失败:$p0")
    }

    override fun onSetFailure(p0: String?) {
        Logger.i("sdp连接失败:$p0")
    }

}
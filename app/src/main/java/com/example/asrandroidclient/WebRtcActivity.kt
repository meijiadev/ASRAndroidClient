package com.example.asrandroidclient

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.orhanobut.logger.Logger
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.AudioTrack
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.audio.JavaAudioDeviceModule
import java.net.URI

class WebRtcActivity :AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    private var factory: PeerConnectionFactory? = null
    private var localAudioTrack: AudioTrack? = null
    private fun initRtc(context: Context) {
        Logger.i("初始化webrtc")
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)    //启用内部追踪器，用来记录一些相关数据
                .createInitializationOptions()
        )
        // -----------创建PeerConnectionFactory
        val adm =
            JavaAudioDeviceModule.builder(this).createAudioDeviceModule()  //音频配置当前JAVA实现，还有native

        val options = PeerConnectionFactory.Options()
        //val enVdf=Defa(eglCtxRemote,true,true)
        factory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(adm)    //设置音频采集和播放使用的配置,当前使用java中的audioTrack 和audioRecord
            .createPeerConnectionFactory()
        Logger.i("initRtc done")

        // 创建声音源
        val audioConstraints = MediaConstraints()
        //回声消除
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googEchoCancellation",
                "true"
            )
        )
        //自动增益
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
        //高音过滤
        audioConstraints.mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
        //噪音处理
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googNoiseSuppression",
                "true"
            )
        )
        val audioSource = factory?.createAudioSource(audioConstraints)
        //创建音频轨道
        localAudioTrack = factory?.createAudioTrack("102", audioSource)
    }

    private fun createPeer() {
        //ice服务器列表
        val iceServers: MutableList<PeerConnection.IceServer> = ArrayList()
        //添加一个turn服务器,turn服务器主要用户下面的stun服务器打洞失败的时候使用这个turn服务器转发数据流，可以添加多个
        iceServers.add(
            PeerConnection.IceServer.builder("turn:**.**.**.**3478") //这是你服务器的地址
                .setUsername("wzp") //用户名
                .setPassword("123456") //密码
                .createIceServer()
        )
        //添加一个stun服务器，
        iceServers.add(PeerConnection.IceServer.builder("stun:**.**.**.**:3478").createIceServer())

        val peerConnection =
            factory?.createPeerConnection(iceServers, object : PeerConnection.Observer {
                override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {}
                override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {
                    //ICE 连接状态变化后回调
                }

                override fun onIceConnectionReceivingChange(b: Boolean) {}
                override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {}
                override fun onIceCandidate(iceCandidate: IceCandidate) {
                    //自动请求stun/turn服务器后回调这个方法
                    //发送Ice信息给对端用户 ,下面的代码只是用于发送信息给远端用户，我使用的是websocket，自己可以用其他方式实现。最后结尾我会给出服务器端的代码。
                    val sendObj = JSONObject()

                }

                override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {}
                override fun onAddStream(mediaStream: MediaStream) {
                    //收到远端数据流信息
                    //mediaStream.videoTracks[0].addSink(remoteRender)  //视频流信息
                    mediaStream.audioTracks[0].setEnabled(true)
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


//        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
//        peerConnection.addTrack(videoTrack,mediaStreamLabels);
//        peerConnection.addTrack(localAudioTrack,mediaStreamLabels);
        //将本地流添加到peerConnection，远端的onAddStream回调将接受该数据流


//        List<String> mediaStreamLabels = Collections.singletonList("ARDAMS");
//        peerConnection.addTrack(videoTrack,mediaStreamLabels);
//        peerConnection.addTrack(localAudioTrack,mediaStreamLabels);
        //将本地流添加到peerConnection，远端的onAddStream回调将接受该数据流
        val stream: MediaStream? = factory?.createLocalMediaStream("110")
        //stream.addTrack(videoTrack)
        stream?.addTrack(localAudioTrack)
        peerConnection?.addStream(stream)
    }

    private var socket: WebSocketClient? = null
    private fun initWebsocket() {
        var uri: URI? = null
        try {
            uri = URI("ws://192.168.2.134:8090")
        } catch (e: Exception) {
            Logger.e("报错：${e.message}")
        }
        socket = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake) {
                if (isDestroyed) {
                    return
                }
                Logger.e("链接socket成功")
            }

            override fun onMessage(message: String) {
                if (isDestroyed) {
                    return
                }
                try {
                    val msgObj = JSONObject(message)
                    val cmd = msgObj.getString("cmd")
                    Logger.e("收到消息:$message")
//                    if (cmd == cmd_new_peer) {
//                        //有新人加入房间
//                        handleNewPeer(msgObj)
//                        return
//                    }
//                    if (cmd == cmd_offer) {
//                        //收到offer请求
//                        handleOffer(msgObj)
//                        return
//                    }
//                    if (cmd == cmd_answer) {
//                        //收到answer请求
//                        handleAnswer(msgObj)
//                        return
//                    }
//                    if (cmd == cmd_ice) {
//                        //收到ice信息
//                        handleIce(msgObj)
//                    }
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) {
                if (isDestroyed) {
                    return
                }
            }

            override fun onError(ex: java.lang.Exception) {
                if (isDestroyed) {
                    return
                }
                Logger.e("socket错误$ex")
            }
        }
        socket?.connect()

    }



}
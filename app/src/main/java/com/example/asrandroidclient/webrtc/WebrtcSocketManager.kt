package com.example.asrandroidclient.webrtc

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asrandroidclient.MyApp
import com.google.gson.Gson
import com.kunminx.architecture.ui.callback.UnPeekLiveData
import com.orhanobut.logger.Logger
import com.sjb.base.base.BaseViewModel
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.UUID

/**
 * Create by MJ on 2023/12/25.
 * Describe :
 */
// webrtc 需要使用的socket.io 连接
const val DEV_WEBRTC_URL = "http://192.168.1.6:80/webrtc?"
const val BASE_WEBRTC_URL = "http://cloud.hdvsiot.com:8080/webrtc?"
var isAnswer = false

class WebrtcSocketManager : BaseViewModel() {
    private var snCode: String? = null
    private var toId: String? = null
    private var uuid: String? = null

    // 被呼叫或者呼叫别人
    var callEvent = UnPeekLiveData<Boolean>()
    private var webrtcSocket: Socket? = null

    @SuppressLint("StaticFieldLeak")
    private var webRtcManager: WebRtcManager? = null


    /**
     * 创建webrtc的通道
     */
    fun createWebrtcSc(snCode: String?, toId: String?, uuid: String?) {
        if (webrtcSocket == null) {
            this.snCode = snCode
            this.toId = toId
            this.uuid = uuid
            val url =
                "${DEV_WEBRTC_URL}token=1231&clientType=anti_bullying_device&clientId=$snCode"
            kotlin.runCatching {
                webrtcSocket = IO.socket(
                    url
                )
            }.onFailure {
                Logger.e("${it.message}")
            }
            webrtcSocket?.connect()
            webRtcManager = WebRtcManager(MyApp.CONTEXT)
            Logger.i("创建webrtc的socket.io链接:$url")
        }
        receiveWebrtcMsg()
        viewModelScope.launch(Dispatchers.IO) {
            delay(30 * 1000)
            if (!isAnswer) {
                sendHangUp()
                release()
            }
        }
    }


    /**
     * 收到对方语音请求，同意语音通话发送called
     */
    fun sendCalled() {
        val message = Message("called", snCode, toId, null, uuid)
        webrtcSocket?.emit("message", Gson().toJson(message), Ack { ack ->
            if (ack?.isEmpty() == true) {
                Logger.i("ack为空")
            } else {
                Logger.i("接收ack:${ack[0].toString()}")
            }

        })
        Logger.i("send called:${Gson().toJson(message)}")
    }


    fun icecandidate(iceCandidate: IceCandidate) {
        val message = Message("icecandidate", snCode, toId, Gson().toJson(iceCandidate), uuid)
        webrtcSocket?.emit("message", Gson().toJson(message), Ack { ack ->
            if (ack?.isEmpty() == true) {
                Logger.i("ack为空")
            } else {
                Logger.i("接收ack:${ack[0].toString()}")
            }

        })
    }

    fun sendOffer(offer: SessionDescription) {
        val message = Message("offer", snCode, toId, offer.description, uuid)
        webrtcSocket?.emit("message", Gson().toJson(message), Ack { ack ->
            if (ack?.isEmpty() == true) {
                Logger.i("ack为空")
            } else {
                Logger.i("接收ack:${ack[0].toString()}")
            }
        })
    }

    fun sendAnswer(answer: SessionDescription) {
        val message = Message("answer", snCode, toId, answer.description, uuid)
        webrtcSocket?.emit("message", Gson().toJson(message), Ack { ack ->
            if (ack?.isEmpty() == true) {
                Logger.i("ack为空")
            } else {
                Logger.i("接收ack:${ack[0].toString()}")
            }

        })
    }

    fun sendHangUp() {
        val message = Message("hangup", snCode, toId, null, uuid)
        webrtcSocket?.emit("message", Gson().toJson(message), Ack { ack ->
            if (ack?.isEmpty() == true) {
                Logger.i("ack为空")
            } else {
                Logger.i("接收ack:${ack[0].toString()}")
            }

        })
        release()
    }


    private fun release() {
        webRtcManager?.release()
        webRtcManager = null
        webrtcSocket?.disconnect()
        webrtcSocket = null
        callEvent.postValue(false)
        snCode = null
        toId = null
    }

    private fun receiveWebrtcMsg() {
        webrtcSocket?.on("message") {
            for (a in it) {
                Logger.i("message：${a.toString()}")
            }
//            val isAck = it[1] is Ack
//            Logger.i("最后一个参数是否是ack:$isAck")
            //mSocket?.emit("ack", "success")
            val message = Gson().fromJson(it[0].toString(), Message::class.java)
            Logger.e(message.msgType)
            when (message.msgType) {
                "called" -> {
                    // 此处只有在拨打别人电话时才会被调用
                    // msgEvent.postValue("called 接通了")
                    Logger.i("on called ${it[0]}")
                    //此处发送offer
                    webRtcManager?.createPeerConnect()
                    webRtcManager?.isOffer = true
                    webRtcManager?.createLocalStream()
                    webRtcManager?.addLocalStream()
                    webRtcManager?.createOffer()
                }

                "offer" -> {
                    val sdp =
                        SessionDescription(SessionDescription.Type.OFFER, message.data.toString())
                    Logger.i("接收到的sdp:${sdp.description}")
                    //  发送answer 并设置remote sdp
                    webRtcManager?.isOffer = false
                    webRtcManager?.createPeerConnect()
                    webRtcManager?.createLocalStream()
                    webRtcManager?.addLocalStream()
                    webRtcManager?.setRemoteDescription(sdp)
//                    sendAnswer(sdp)
                    webRtcManager?.createAnswer()
                    isAnswer = true
                }

                "answer" -> {
                    // 设置 remote sdp
                    Logger.i("receive answer:${message.data}")
                    val sdp =
                        SessionDescription(SessionDescription.Type.ANSWER, message.data.toString())
                    //  发送answer 并设置remote sdp
                    webRtcManager?.setRemoteDescription(sdp)
                }

                "hangup" -> {
                    release()
                    Logger.i("语音挂断，释放socket和release webrtc")
                }

                "icecandidate" -> {
                    Logger.d("ice:${message.data}")
                    val ice = Gson().fromJson(message.data.toString(), IceCandidate::class.java)
                    webRtcManager?.addIce(ice)
                    isAnswer = true
                }

            }
        }

        webrtcSocket?.on(Socket.EVENT_CONNECT) {
            Logger.i("webrtc socket.io 连接成功")
            sendCalled()

        }

        webrtcSocket?.on(Socket.EVENT_DISCONNECT) {
            Logger.e("webrtc socket.io 断开链接")
        }

        webrtcSocket?.on(Socket.EVENT_CONNECT_ERROR) {
            Logger.e("webrtc socket.io error:${it[0].toString()}")
        }

    }
}
package com.example.asrandroidclient.webrtc

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.asrandroidclient.HandlerAction
import com.example.asrandroidclient.MyApp
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SocketEventViewModel : ViewModel() ,HandlerAction{
    companion object {
        const val TAG = "SocketEventViewModel"
    }

    // 登录事件
    var loginEvent = MutableLiveData<Boolean>()

    // 被呼叫或者呼叫别人
    var callEvent = MutableLiveData<Boolean>()

    var from: String = "123"
    var toId: String = "456"
    private var mSocket: Socket? = null
    private var webRtcManager: WebRtcManager? = null

    fun initSocket(sn: String) {
        kotlin.runCatching {
            mSocket = IO.socket("http://192.168.1.6:16000")
        }.onFailure {
            Log.e(TAG, "${it.message}")
        }
        mSocket?.connect()
        receiveMessage()
        webRtcManager = WebRtcManager(MyApp.CONTEXT)
        this.from = sn
        Log.i(TAG, "初始化socket")
    }


    fun login(userId: String) {
        this.from = userId
        mSocket?.emit("register", userId, Ack { ack ->
            Log.i(TAG, "注册命令返回")
        })
    }

    fun call(userId: String, toId: String) {
        this.from = userId
        this.toId = toId
        val data = "987654321"
        mSocket?.emit("call", userId, toId, data, Ack { ack ->
            // 在这处理确认消息 ack:success 表示目标在线
            Log.i(TAG, "call ack:${ack[0]}")
        })
    }

    /**
     * 收到对方语音请求，同意语音通话发送called
     */
    fun sendCalled() {
        val data = "987654321"
        mSocket?.emit("called", from, toId, data)
    }


    fun sendHangUp() {
        val message = Message("hangUp", from, toId, null)
        mSocket?.emit("message", Gson().toJson(message))
        webRtcManager?.release()
    }

    fun icecandidate(iceCandidate: IceCandidate) {
        val message = Message("icecandidate", from, toId, iceCandidate)
        mSocket?.emit("message", Gson().toJson(message))
    }

    fun sendOffer(offer: SessionDescription) {
        val message = Message("offer", from, toId, offer.description)
        mSocket?.emit("message", Gson().toJson(message))
    }

    fun sendAnswer(answer: SessionDescription) {
        val message = Message("answer", from, toId, answer.description)
        mSocket?.emit("message", Gson().toJson(message))
    }

    private fun receiveMessage() {
        mSocket?.on("message") {

            val message = Gson().fromJson(it[0].toString(), Message::class.java)
            Log.i("SocketEvent", message.type)
            when (message.type) {
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
                }
                "answer" -> {
                    // 设置 remote sdp
                    Logger.i("receive answer:${message.data}")
                    val sdp =
                        SessionDescription(SessionDescription.Type.ANSWER, message.data.toString())
                    //  发送answer 并设置remote sdp
                    webRtcManager?.setRemoteDescription(sdp)
                }
                "hangUp" -> {
                    webRtcManager?.release()
                }
                "icecandidate" -> {
                    Logger.d("ice:${message.data}")
                    val ice = Gson().fromJson(message.data.toString(), IceCandidate::class.java)
                    webRtcManager?.addIce(ice)
                }
            }
        }

        /**
         *  socket.io 连接成功
         */
        mSocket?.on("connected") {
            Log.i(TAG, "socket.io 连接成功:${it[0]}")
            login(from)
        }

        // 表示对方呼叫
        mSocket?.on("call") {
            Log.i(TAG, "on call ${it[0]}")
            // CallSingleActivity.openActivity(App.instance?.applicationContext, toId, false, false)
            // 对方发送的参数 fromId toId
            callEvent.postValue(true)
            toId = it[0].toString()
            Logger.i("来电了：from:${it[0]},to:${it[1]}")
            postDelayed({
                sendCalled()
            },100)
        }

        // 只有接听方才需要处理此业务 别人已经确定接听
        mSocket?.on("called") {
            Log.i(TAG, "on called ${it[0]}")
            //此处发送offer
            webRtcManager?.createPeerConnect()
            webRtcManager?.isOffer = true
            webRtcManager?.createLocalStream()
            webRtcManager?.addLocalStream()
            webRtcManager?.createOffer()
        }
    }


}
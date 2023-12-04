package com.example.asrandroidclient.webrtc

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.asrandroidclient.HandlerAction
import com.example.asrandroidclient.MyApp
import com.example.asrandroidclient.webrtc.data.keyword.DeleteKeyword
import com.example.asrandroidclient.webrtc.data.keyword.KeyWords
import com.example.asrandroidclient.webrtc.data.keyword.Keyword
import com.example.asrandroidclient.webrtc.data.voice.DeleteVoice
import com.example.asrandroidclient.webrtc.data.voice.VoiceData
import com.example.asrandroidclient.webrtc.data.voice.VoiceDatas
import com.google.gson.Gson
import com.orhanobut.logger.Logger
import io.socket.client.IO
import io.socket.client.Socket
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

class SocketEventViewModel : ViewModel(), HandlerAction {

    // 登录事件
    var loginEvent = MutableLiveData<Boolean>()

    // 被呼叫或者呼叫别人
    var callEvent = MutableLiveData<Boolean>()

    var msgEvent = MutableLiveData<String>()


    var from: String = "SN012345678902"
    var toId: String = "SN012345678901"
    private var mSocket: Socket? = null
    private var webRtcManager: WebRtcManager? = null

    fun initSocket(sn: String) {
        kotlin.runCatching {
            mSocket = IO.socket(
                "http://192.168.1.6:7099/spad-cloud?token=1231&clientType=anti_bullying_device&clientId=SN012345678902"
            )
        }.onFailure {
            Logger.e("${it.message}")
        }
        mSocket?.connect()
        receiveMessage()
        webRtcManager = WebRtcManager(MyApp.CONTEXT)
        // this.from = sn
        Logger.i("初始化socket:${mSocket?.isActive}")
    }


    fun login(userId: String) {
        this.from = userId
        Logger.i("login:$userId")
        mSocket?.emit("register", userId, "1975-01-01 14:04:17")
        postDelayed({
            getMsg()
        }, 2000)
    }

    fun call(userId: String, toId: String) {
        Logger.i("call $userId,$toId")
        this.from = userId
        this.toId = toId
        val message = Message("call", from, toId, null)
        mSocket?.emit("message", Gson().toJson(message))
    }

    fun getMsg() {
        mSocket?.emit("syncMessage", from, null)
        Logger.i("主动查询信息")
    }

    /**
     * 收到对方语音请求，同意语音通话发送called
     */
    fun sendCalled() {
        val data = "987654321"
        val message = Message("called", from, toId, null)
        mSocket?.emit("message", Gson().toJson(message))
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
            Logger.e(message.msgType)
            when (message.msgType) {
                "call" -> {
                    Logger.i("on call ${it[0]}")
                    // CallSingleActivity.openActivity(App.instance?.applicationContext, toId, false, false)
                    // 对方发送的参数 fromId toId
                    callEvent.postValue(true)
                    toId = it[0].toString()
                    Logger.i("来电了：from:${it[0]},to:${it[1]}")
                    msgEvent.postValue("call 来电了")
                    postDelayed({
                        sendCalled()
                    }, 100)
                }

                "called" -> {
                    msgEvent.postValue("called 接通了")
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

        mSocket?.on("syncMessage") {
            val msgType = it[0].toString()
            Logger.i("syncMessage：${it[0].toString()}")
            Logger.i("syncMsg:${it[1].toString()}")
            when (msgType) {
                MessageType.KeywordAdd.toString() -> {
                    val keyword = Gson().fromJson(it[1].toString(), Keyword::class.java)
                    Logger.i("data:$keyword")
                }

                MessageType.KeywordEdit.toString() -> {
                    val keyword = Gson().fromJson(it[1].toString(), Keyword::class.java)
                    Logger.i("data:$keyword")
                }

                MessageType.KeywordDel.toString() -> {
                    val delete = Gson().fromJson(it[1].toString(), DeleteKeyword::class.java)
                    Logger.i("keywordId:$delete")
                }

                MessageType.KeywordList.toString() -> {
                    val keywords = Gson().fromJson(it[1].toString(), KeyWords::class.java)
                    Logger.i("datas：${keywords.data.size}")
                }

                MessageType.VoiceAdd.toString() -> {
                    val voice = Gson().fromJson(it[1].toString(), VoiceData::class.java)
                    Logger.i("data:$voice")
                }

                MessageType.VoiceEdit.toString() -> {
                    val voice = Gson().fromJson(it[1].toString(), VoiceData::class.java)
                    Logger.i("data:$voice")
                }

                MessageType.VoiceDel.toString() -> {
                    val deleteVoice = Gson().fromJson(it[1].toString(), DeleteVoice::class.java)
                    Logger.i("keywordId:$deleteVoice")
                }

                MessageType.VoiceList.toString() -> {
                    val voices = Gson().fromJson(it[1].toString(), VoiceDatas::class.java)
                    Logger.i("datas：${voices.data.size}")
                }
            }
            msgEvent.postValue(it[0].toString())
        }

        mSocket?.on("connect") {
            Logger.i("socket.io 正在连接")
            msgEvent.postValue("connect ..正在连接")
            login(from)
        }

        mSocket?.on("disconnected") {
            Logger.i("socket.io 断开连接")
        }

    }


}
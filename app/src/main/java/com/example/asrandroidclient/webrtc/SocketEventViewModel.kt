package com.example.asrandroidclient.webrtc

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asrandroidclient.HandlerAction
import com.example.asrandroidclient.LATEST_TIME_KEY
import com.example.asrandroidclient.MyApp
import com.example.asrandroidclient.data.UploadFileResult
import com.example.asrandroidclient.room.AppDataBase
import com.example.asrandroidclient.room.bean.KeywordBean
import com.example.asrandroidclient.room.bean.VoiceBean
import com.example.asrandroidclient.util.SpManager
import com.example.asrandroidclient.webrtc.data.keyword.DeleteKeyword
import com.example.asrandroidclient.webrtc.data.keyword.KeyWords
import com.example.asrandroidclient.webrtc.data.keyword.Keyword
import com.example.asrandroidclient.webrtc.data.voice.DeleteVoice
import com.example.asrandroidclient.webrtc.data.voice.VoiceData
import com.example.asrandroidclient.webrtc.data.voice.VoiceDatas
import com.google.gson.Gson
import com.kunminx.architecture.ui.callback.UnPeekLiveData
import com.orhanobut.logger.Logger
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.io.File
import java.io.IOException

class SocketEventViewModel : ViewModel(), HandlerAction {

    // 登录事件
    var loginEvent = MutableLiveData<Boolean>()

    // 被呼叫或者呼叫别人
    var callEvent = UnPeekLiveData<Boolean>()

    var msgEvent = UnPeekLiveData<String>()

    // 关键字
    var keywordUpdateEvent = UnPeekLiveData<Int>()

    var voiceUpdateEvent = UnPeekLiveData<Int>()

    companion object {
        const val KEYWORD_STATUS_ADD = 0
        const val KEYWORD_STATUS_EDIT = 1
        const val KEYWORD_STATUS_DEL = 2
        const val KEYWORD_STATUS_LIST = 3

        const val VOICE_STATUS_ADD = 0
        const val VOICE_STATUS_EDIT = 1
        const val VOICE_STATUS_DEL = 2
        const val VOICE_STATUS_LIST = 3
    }


    //var from: String = "SN012345678902"
    var toId: String = "SN012345678901"
    private var mSocket: Socket? = null
    private var webRtcManager: WebRtcManager? = null

    private var snCode: String? = null

    /**
     * 报警的关键字
     */
    private var keyword: String? = null

    //关键字Id
    private var keywordId: Long = 0

    // 关键字得分
    private var ncm: Int = 0

    // 关键字触发时长
    private var duration: String? = null

    // 报警的30s音频文件
    private var alarmFile: File? = null


    fun initSocket(sn: String) {
        snCode = sn
        kotlin.runCatching {
            mSocket = IO.socket(
                "http://192.168.1.6:7099/spad-cloud?token=1231&clientType=anti_bullying_device&clientId=$sn"
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


    fun login() {
        Logger.i("login:$snCode")
        val time=SpManager.getString(LATEST_TIME_KEY,"1975-01-01 14:04:17")
        mSocket?.emit("register", snCode, time)
        //getUploadFileUrl()
    }

    fun call(toId: String) {
        Logger.i("call ,$toId")
        this.toId = toId
        val message = Message("call", snCode, toId, null)
        mSocket?.emit("message", Gson().toJson(message))
    }

    /**
     * 获取文件上传地址
     */
    fun getUploadFileUrl(keyword: String, id: Long, ncm: Int, duration: String, alarmFile: File?) {
        this.keyword = keyword
        this.keywordId = id
        this.ncm = ncm
        this.duration = duration
        this.alarmFile = alarmFile
        mSocket?.emit("uploadFileUrlMessage", snCode)
        Logger.i("获取上传文件地址链接命令")
    }

    /**
     * 可每日定时主动去获取key信息
     * 主动去获取msg信息(关键字 or 语音播报的文字)
     */
    fun getMsg() {
        mSocket?.emit("syncMessage", snCode, null)
        Logger.i("主动查询信息")
    }


    /**
     * 上传报警msg
     */
    private fun uploadWarnMsg(fileId: String) {
        val dto = AntiBullyingRecordDTO(
            snCode = snCode,
            keyword = keyword,
            keywordId = keywordId,
            credibility = ncm,
            duration = duration,
            type = "1",
            fileId = fileId.toLong()
        )
        Logger.i("上传报警信息：$keyword,$keywordId,$ncm,$duration")
        mSocket?.emit("waringMessage", Gson().toJson(dto))
    }


    /**
     * 收到对方语音请求，同意语音通话发送called
     */
    fun sendCalled() {
        val message = Message("called", snCode, toId, null)
        mSocket?.emit("message", Gson().toJson(message))
    }


    fun sendHangUp() {
        val message = Message("hangUp", snCode, toId, null)
        mSocket?.emit("message", Gson().toJson(message))
        webRtcManager?.release()
    }

    fun icecandidate(iceCandidate: IceCandidate) {
        val message = Message("icecandidate", snCode, toId, iceCandidate)
        mSocket?.emit("message", Gson().toJson(message))
    }

    fun sendOffer(offer: SessionDescription) {
        val message = Message("offer", snCode, toId, offer.description)
        mSocket?.emit("message", Gson().toJson(message))
    }

    fun sendAnswer(answer: SessionDescription) {
        val message = Message("answer", snCode, toId, answer.description)
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
                    // 对方发送的参数 snCodeId toId
                    callEvent.postValue(true)
                    toId = it[0].toString()
                    Logger.i("来电了：snCode:${it[0]},to:${it[1]}")
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
                    val data = keyword.keyData
                    Logger.i("data:$keyword")
                    viewModelScope.launch(Dispatchers.IO) {
                        val keywordBean = KeywordBean(
                            time = keyword.time,
                            keywordId = data.keywordId,
                            keyword = data.keyword,
                            credibility = data.credibility,
                            delFlag = data.delFlag,
                            enabled = data.enabled,
                            matchType = data.matchType,
                            orgId = data.orgId

                        )
                        if (AppDataBase.getInstance().insert(keywordBean)) {
                            delay(100)
                            keywordUpdateEvent.postValue(KEYWORD_STATUS_ADD)
                        }
                    }

                }

                MessageType.KeywordEdit.toString() -> {
                    val key = Gson().fromJson(it[1].toString(), Keyword::class.java)
                    val data = key.keyData
                    Logger.i("data:$key")
                    viewModelScope.launch(Dispatchers.IO) {
                        val result = AppDataBase.getInstance().keyWordDao().findById(data.keywordId)
                        result?.apply {
                            keywordId = data.keywordId
                            keyword = data.keyword
                            credibility = data.credibility
                            delFlag = data.delFlag
                            enabled = data.enabled
                            matchType = data.matchType
                            orgId = data.orgId
                        }
                        AppDataBase.getInstance().keyWordDao().updateKeyword(result!!)
                        delay(100)
                        keywordUpdateEvent.postValue(KEYWORD_STATUS_EDIT)
                    }
                }

                MessageType.KeywordDel.toString() -> {
                    val delete = Gson().fromJson(it[1].toString(), DeleteKeyword::class.java)
                    Logger.i("keywordId:$delete")
                    viewModelScope.launch(Dispatchers.IO) {
                        AppDataBase.getInstance().deleteKeywordById(delete.data)
                        delay(100)
                        keywordUpdateEvent.postValue(KEYWORD_STATUS_DEL)
                    }

                }

                MessageType.KeywordList.toString() -> {
                    val keywords = Gson().fromJson(it[1].toString(), KeyWords::class.java)
                    SpManager.putString(LATEST_TIME_KEY, keywords.time)
                    val datas = keywords.data
                    Logger.i("datas：${keywords.data.size}")
                    viewModelScope.launch(Dispatchers.IO) {
                        for (data in datas) {
                            val keywordBean = KeywordBean(
                                time = keywords.time,
                                keywordId = data.keywordId,
                                keyword = data.keyword,
                                credibility = data.credibility,
                                delFlag = data.delFlag,
                                enabled = data.enabled,
                                matchType = data.matchType,
                                orgId = data.orgId
                            )
                            if (keywordBean.delFlag == "1") {
                                AppDataBase.getInstance().deleteKeywordById(keywordBean.keywordId)
                            } else {
                                AppDataBase.getInstance().insert(keywordBean)
                            }
                        }
                        delay(100)
                        keywordUpdateEvent.postValue(KEYWORD_STATUS_LIST)
                    }
                }

                MessageType.VoiceAdd.toString() -> {
                    val voice = Gson().fromJson(it[1].toString(), VoiceData::class.java)
                    Logger.i("data:$voice")
                    val datax = voice.data
                    viewModelScope.launch(Dispatchers.IO) {
                        val voiceBean = VoiceBean(
                            time = voice.time,
                            voiceId = datax.voiceId,
                            defaultFlag = datax.defaultFlag,
                            delFlag = datax.delFlag,
                            orgId = datax.orgId,
                            text = datax.text,
                            times = datax.times
                        )
                        if (AppDataBase.getInstance().insert(voiceBean)) {
                            delay(100)
                            voiceUpdateEvent.postValue(VOICE_STATUS_ADD)
                        }
                    }
                }

                MessageType.VoiceEdit.toString() -> {
                    val voice = Gson().fromJson(it[1].toString(), VoiceData::class.java)
                    val datax = voice.data
                    Logger.i("data:$voice")
                    viewModelScope.launch(Dispatchers.IO) {
                        val result = AppDataBase.getInstance().voiceDao().findById(datax.voiceId)
                        result?.apply {
                            voiceId = datax.voiceId
                            defaultFlag = datax.defaultFlag
                            delFlag = datax.delFlag
                            orgId = datax.orgId
                            text = datax.text
                            times = datax.times
                            AppDataBase.getInstance().voiceDao().updateVoice(result)
                        }
                        delay(100)
                        voiceUpdateEvent.postValue(VOICE_STATUS_EDIT)
                    }
                }

                MessageType.VoiceDel.toString() -> {
                    val deleteVoice = Gson().fromJson(it[1].toString(), DeleteVoice::class.java)
                    Logger.i("keywordId:$deleteVoice")
                    viewModelScope.launch(Dispatchers.IO) {
                        AppDataBase.getInstance().deleteVoiceById(deleteVoice.data)
                        delay(100)
                        voiceUpdateEvent.postValue(VOICE_STATUS_DEL)
                    }
                }

                MessageType.VoiceList.toString() -> {
                    val voices = Gson().fromJson(it[1].toString(), VoiceDatas::class.java)
                    Logger.i("datas：${voices.data.size}")
                    val datas = voices.data
                    viewModelScope.launch(Dispatchers.IO) {
                        for (data in datas) {
                            val voiceBean = VoiceBean(
                                time = voices.time,
                                voiceId = data.voiceId,
                                defaultFlag = data.defaultFlag,
                                delFlag = data.delFlag,
                                orgId = data.orgId,
                                text = data.text,
                                times = data.times
                            )
                            if (voiceBean.delFlag == "1") {
                                AppDataBase.getInstance().deleteVoiceById(voiceBean.voiceId)
                            } else {
                                AppDataBase.getInstance().insert(voiceBean)
                            }
                        }
                        delay(100)
                        voiceUpdateEvent.postValue(VOICE_STATUS_LIST)
                    }

                }
            }
            msgEvent.postValue(it[0].toString())
        }


        mSocket?.on("uploadFileUrlMessage") {
            Logger.i("获取文件上传地址：${it[0]}")
            val url = it[0].toString()
            alarmFile?.let {
                uploadFile(url, it)
            }

        }


        mSocket?.on("connect") {
            Logger.i("socket.io 正在连接")
            msgEvent.postValue("connect ..正在连接")
            login()
        }

        mSocket?.on("disconnected") {
            Logger.i("socket.io 断开连接")
        }

    }

    fun uploadFile(url: String, file: File) {
        val requestBody = RequestBody.create(
            MediaType.parse("application/octet-stream"),
            file
        )
        val multipartBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, requestBody)
        val multipartBody = multipartBodyBuilder.build()
        val request = Request.Builder()
            .url(url)
            .post(multipartBody)
            .build()
        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Logger.e("文件上传失败：${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Logger.i("上传失败：$response")
                } else {
                    val jsonStr = response.body()?.string()
                    val uploadFileResult = Gson().fromJson(jsonStr, UploadFileResult::class.java)
                    Logger.i("上传成功：${uploadFileResult}")
                    if (uploadFileResult.success) {
                        uploadWarnMsg(uploadFileResult.data.fileId)
                    }
                }
            }
        })
    }

}
package com.example.asrandroidclient.webrtc

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.asrandroidclient.BuildConfig
import com.example.asrandroidclient.HandlerAction
import com.example.asrandroidclient.LATEST_TIME_KEY
import com.example.asrandroidclient.MyApp
import com.example.asrandroidclient.data.DeviceState
import com.example.asrandroidclient.data.RegisterData
import com.example.asrandroidclient.data.UpdateAppData
import com.example.asrandroidclient.data.UploadFileResult
import com.example.asrandroidclient.room.AppDataBase
import com.example.asrandroidclient.room.bean.KeywordBean
import com.example.asrandroidclient.room.bean.VoiceBean
import com.example.asrandroidclient.tool.NetworkUtil
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
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory

class SocketEventViewModel : ViewModel(), HandlerAction {

    // 登录事件
    var loginEvent = MutableLiveData<Boolean>()

    // 被呼叫或者呼叫别人
    //var callEvent = UnPeekLiveData<Boolean>()

    var msgEvent = UnPeekLiveData<String>()

    // 关键字
    var keywordUpdateEvent = UnPeekLiveData<Int>()

    var voiceUpdateEvent = UnPeekLiveData<Int>()

    // app 更新相关内容
    var appUpdateEvent = UnPeekLiveData<UpdateAppData>()

    private var isConnected = false


    companion object {
        const val KEYWORD_STATUS_ADD = 0
        const val KEYWORD_STATUS_EDIT = 1
        const val KEYWORD_STATUS_DEL = 2
        const val KEYWORD_STATUS_LIST = 3

        const val VOICE_STATUS_ADD = 0
        const val VOICE_STATUS_EDIT = 1
        const val VOICE_STATUS_DEL = 2
        const val VOICE_STATUS_LIST = 3

        // http://192.168.1.6:80/webrtc?
        //http://192.168.1.6:80/device?
        private const val BASE_TEST_HTTP_URL = "http://cloud.hdvsiot.com:8080/"             // 测试环境
        private const val BASE_HTTP_URL = "https://spad-cloud.hdvsiot.com/"             // 演示环境
        private const val BASE_HTTP_URL_ZYQ = "http://cloud.zyq0407.com:8080/"       // 开发环境
        private const val isDevVersion = true
        private const val isTestVersion = false        // 是否是测试版本
        const val HOST_URL_KEY = "host_url_key"

        // 是否注册平台
        var isRegister = false
        fun getHostUrl(): String {
            return if (isTestVersion) {
                // BASE_HTTP_URL_ZYQ
                (SpManager.getString(HOST_URL_KEY) ?: BASE_TEST_HTTP_URL)
            } else {
                BASE_HTTP_URL
                                                                                                                                      }
        }

    }

    //var from: String = "SN012345678902"
    var toId: String = "SN012345678901"
    var uuid: String? = null
    private var mSocket: Socket? = null
    //private var webRtcManager: WebRtcManager? = null

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

    private var volumeDb: Int? = null


    fun initSocket(sn: String) {
        snCode = sn
        val ip = NetworkUtil.getIPAddress(true)
        val url =
            "${getHostUrl()}device?token=1231&clientType=anti_bullying_device&clientId=$sn&ip=$ip"
        val uri = URI.create(url)
        val websocket = arrayOf("websocket")
        val options = IO.Options.builder()
            .setReconnectionDelay(1000)
            .setTransports(websocket)
            .build()
        kotlin.runCatching {
            mSocket = IO.socket(
                uri, options
            )
        }.onFailure {
            Logger.e("${it.message}")
        }
        mSocket?.connect()
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.MINUTES)
            .readTimeout(1, TimeUnit.MINUTES)
            .build()
        IO.setDefaultOkHttpCallFactory(okHttpClient)
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient)
        receiveMessage()
        // webRtcManager = WebRtcManager(MyApp.CONTEXT)
        // this.from = sn
        Logger.i("初始化socket:${mSocket?.isActive},url:$url")
    }

    fun setUrl(url: String) {
        SpManager.putString(HOST_URL_KEY, url)
        // BASE_TEST_HTTP_URL = url
        mSocket?.disconnect()
        mSocket = null
//        if (snCode != null) {
//            initSocket(snCode!!)
//        }
    }


    // 1:离线 2：在线 3：故障 4:升级中
    fun uploadState(state: String, stateMsg: String, progress: String? = null) {
        val msgState = Gson().toJson(DeviceState(null, null, state, stateMsg, progress))
        mSocket?.emit("stateMessage", snCode, msgState)
        Logger.i("上报设备故障消息：$msgState")
    }


    fun login() {
        val time = SpManager.getString(LATEST_TIME_KEY, "1975-01-01 14:04:17")
        val msgRegister =
            Gson().toJson(RegisterData(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, time))
        mSocket?.emit("register", snCode, msgRegister)
        Logger.i("login:$snCode,time:$time")
        //getUploadFileUrl()
    }

    fun call(toId: String) {
        Logger.i("call ,$toId")
        this.toId = toId
        val message = Message("call", snCode, toId, null, null)
        mSocket?.emit("message", Gson().toJson(message))
    }

    /**
     * 获取文件上传地址
     */
    fun getUploadFileUrl(
        keyword: String,
        id: Long,
        ncm: Int,
        duration: String,
        alarmFile: File?,
        db: Int
    ) {
        this.keyword = keyword
        this.keywordId = id
        this.ncm = ncm
        this.duration = duration
        this.alarmFile = alarmFile
        this.volumeDb = db
        mSocket?.emit("uploadFileUrlMessage", snCode)
        Logger.d("获取上传文件地址链接命令")
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
            fileId = fileId.toLong(),
            volume = volumeDb
        )
        Logger.i("上传报警信息：$keyword,$keywordId,$ncm,$duration")
        mSocket?.emit("waringMessage", snCode, Gson().toJson(dto))
    }


    private fun receiveMessage() {
        mSocket?.on("call") {
            for (a in it) {
                Logger.i("message：${a.toString()}")
            }
            val message = Gson().fromJson(it[0].toString(), Message::class.java)
            Logger.e(message.msgType)
            when (message.msgType) {
                "call" -> {
                    Logger.i("on call ${it[0]}")
                    toId = message?.sendFrom.toString()
                    uuid = message?.tx
                    Logger.i("来电了：snCode:${it[0]}")
                    msgEvent.postValue("call 来电了")
                    MyApp.webrtcSocketManager.createWebrtcSc(snCode, toId, uuid)
                    isAnswer = false
                    //callEvent.postValue(true)
                    MyApp.webrtcSocketManager.callEvent.postValue(true)
                }

            }
        }

        mSocket?.on("syncMessage") {
            val msgType = it[0].toString()
            Logger.i("messageType：${it[0].toString()}")
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
                            orgId = data.orgId,
                            voiceId = data.voiceId
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
                            voiceId = data.voiceId
                            AppDataBase.getInstance().keyWordDao().updateKeyword(result)
                            delay(100)
                            keywordUpdateEvent.postValue(KEYWORD_STATUS_EDIT)
                        }
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
                    if (keywords.data.isNotEmpty()) {
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
                                    orgId = data.orgId,
                                    voiceId = data.voiceId
                                )
                                if (keywordBean.delFlag == "1") {
                                    AppDataBase.getInstance()
                                        .deleteKeywordById(keywordBean.keywordId)
                                } else {
                                    AppDataBase.getInstance().insert(keywordBean)
                                }
                            }
                            delay(100)
                            keywordUpdateEvent.postValue(KEYWORD_STATUS_LIST)
                        }

                    }
                    isRegister = true
                    // 检测是否配网成功,已经注册到平台
                    MyApp.mainViewModel.run {
                        if (isNetworkConfig && configUrl == getHostUrl()) {
                            viewModelScope.launch {
                                networkConfigEvent.postValue(3)
                                isNetworkConfig = false
                            }
                        }
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
                    if (voices.data.isNotEmpty()) {
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
            }
            //msgEvent.postValue(it[0].toString())
        }


        mSocket?.on("uploadFileUrlMessage") {
            Logger.d("获取文件上传地址：${it[0]}")
            val url = it[0].toString()
            alarmFile?.let {
                uploadFile(url, it)
            }

        }
        // 监听是否有最新版本app
        mSocket?.on("latestVersion") {
            val sn = it[0].toString()
            if (it.size > 1) {
                kotlin.runCatching {
                    val jsonStr = it[1].toString()
                    val appUpdate = Gson().fromJson(jsonStr, UpdateAppData::class.java)
                    appUpdateEvent.postValue(appUpdate)
                    Logger.i("获取更新的信息：$jsonStr")
                }.onFailure {
                    Logger.e("获取更新信息失败：${it.message}")
                }
            }
        }


        mSocket?.on(Socket.EVENT_CONNECT) {
            Logger.i("socket.io 连接成功")
            msgEvent.postValue("connect ..连接成功")
            login()
            isConnected = true

        }

        mSocket?.on(Socket.EVENT_CONNECT_ERROR) {
            for (a in it) {
                Logger.i("socket.io 连接错误：${a}")
            }
            isConnected = false
            //isRegister = false
            msgEvent.postValue("全局socket.io ..连接错误")
        }

        mSocket?.on(Socket.EVENT_DISCONNECT) {
            for (a in it) {
                Logger.i("socket.io 断开连接:${a}")
                msgEvent.postValue("socket.io 断开连接")
            }
            isConnected = false
            isRegister = false
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
                    Logger.d("上传成功：${uploadFileResult}")
                    if (uploadFileResult.success) {
                        uploadWarnMsg(uploadFileResult.data.fileId)
                    }
                }
            }
        })
    }

}


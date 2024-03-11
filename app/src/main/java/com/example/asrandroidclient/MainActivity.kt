package com.example.asrandroidclient

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.azhon.appupdate.listener.OnDownloadListenerAdapter
import com.azhon.appupdate.manager.DownloadManager
import com.clj.fastble.BleManager
import com.example.asrandroidclient.ability.AbilityCallback
import com.example.asrandroidclient.ability.AbilityConstant
import com.example.asrandroidclient.ability.IFlytekAbilityManager
import com.example.asrandroidclient.ability.abilityAuthStatus
import com.example.asrandroidclient.data.Rlt
import com.example.asrandroidclient.data.SpeechResult
import com.example.asrandroidclient.file.FileUtil
import com.example.asrandroidclient.ivw.IvwHelper
import com.example.asrandroidclient.media.audio.AudioRecorder
import com.example.asrandroidclient.media.audio.RecorderCallback
import com.example.asrandroidclient.room.AppDataBase
import com.example.asrandroidclient.room.bean.KeywordBean
import com.example.asrandroidclient.tool.ByteArrayQueue
import com.example.asrandroidclient.tool.IntArrayQueue
import com.example.asrandroidclient.tool.NetworkUtil
import com.example.asrandroidclient.tool.PcmToWavConverter
import com.example.asrandroidclient.tool.calculateVolume
import com.example.asrandroidclient.tool.stampToDate
import com.google.gson.Gson
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.iflytek.cloud.ErrorCode
import com.iflytek.cloud.InitListener
import com.iflytek.cloud.RecognizerListener
import com.iflytek.cloud.RecognizerResult
import com.iflytek.cloud.SpeechConstant
import com.iflytek.cloud.SpeechError
import com.iflytek.cloud.SpeechEvent
import com.iflytek.cloud.SpeechRecognizer
import com.orhanobut.logger.Logger
import com.ys.rkapi.MyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.Locale


class MainActivity : AppCompatActivity(), HandlerAction, AbilityCallback,
    TextToSpeech.OnInitListener {

    private var ivwHelper: IvwHelper? = null
    private var keyWord: String =
        "救命救命"
    private var keywordList = mutableListOf<String>()
    private var threshold: Int = 600     //范围 0-3000

    private var textToSpeech: TextToSpeech? = null

    // 分贝
    private var calculateVolume: Int = 0

    private var sn: String? = null

    private val msgTV: TextView by lazy { findViewById(R.id.message_tv) }

    private var speechMsg: String? = null
    private var speechMsgTimes: Int = 1

    /**
     * 是否是语音引擎重启
     */
    private var isRestart = false

    /**
     * 语音引擎是否开启成功
     */
    private var ivwIsOpen = false

    /**
     * 是否正在启动中 true：正在连接中  false:连接已完成
     */
    private var isBeingStarted = false

    /**
     * 是否正在语音通话
     */
    private var isVoiceCall = false

    private var isRunning = true

    private var audioManager: AudioManager? = null

    private var isMicOnline = true        //检查麦克风是否在线
    private var onLineDbCount = 0
    private var offlineDb = 0
    private var startRecordTime: Long = 0       // 刚刚开始录音的时间
    private var strMsg = StringBuffer()

    private var volumeQueue = IntArrayQueue()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Logger.i("应用启动")
        val size = FileUtil.getDirectorySize(MyApp.CONTEXT.externalCacheDir?.absolutePath)
        val mSize = size / 1024 / 1024
        Logger.i("当前应用目录下缓存占用内存：${mSize} M")
        if (mSize > 800) {
            FileUtil.deleteDirectory(MyApp.CONTEXT.externalCacheDir?.absolutePath ?: " ")

        }
        textToSpeech = TextToSpeech(MyApp.CONTEXT, this)
        initPermission()
        initViewModel()
        initYsAndroidApi()
        checkIVW()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        //showVolumeDb()
        msgTV.movementMethod = ScrollingMovementMethod.getInstance()
        msgTV.text = NetworkUtil.getIPAddress(true)
        initRecognizer()
    }

    private fun initPermission() {
        XXPermissions.with(this)
            // .permission(Permission.MANAGE_EXTERNAL_STORAGE)
            .permission(Permission.RECORD_AUDIO)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .permission(Permission.READ_EXTERNAL_STORAGE)
            .permission(Permission.BLUETOOTH_ADVERTISE)
//            .permission(Permission.ACCESS_FINE_LOCATION)
//            .permission(Permission.ACCESS_COARSE_LOCATION)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    Logger.i("录音权限获取成功")
                    if (all) {
                        IFlytekAbilityManager.getInstance().initializeSdk(MyApp.CONTEXT)
                        initIvw()
                        initBle()
                    }

                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    Logger.i("权限获取失败")
                }
            })

    }

    private fun initBle() {
        BleManager.getInstance().init(MyApp.app)
        BleManager.getInstance().enableLog(true)
        val isSupportBle = BleManager.getInstance().isSupportBle
        val isBleEnable = BleManager.getInstance().isBlueEnable
        Logger.i("设备是否支持蓝牙：$isSupportBle,蓝牙是否开启：$isBleEnable")
        if (!isBleEnable) {
            BleManager.getInstance().enableBluetooth()
        }
        MyApp.mainViewModel.initBlue()
    }

    private var mIat: SpeechRecognizer? = null

    /**
     * 初始化语音听写的业务
     */
    private fun initRecognizer() {
        mIat = SpeechRecognizer.createRecognizer(this, mInitListener)
    }

    private fun setParam() {
        // 清空参数
        mIat?.setParameter(SpeechConstant.PARAMS, null)
        mIat?.setParameter(SpeechConstant.LANGUAGE, "zh_cn")
        mIat?.setParameter(SpeechConstant.ACCENT, "mandarin")
        mIat?.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD)
        // 设置返回结果格式
        mIat?.setParameter(SpeechConstant.RESULT_TYPE, "plain ")
//        //取值范围{1000～10000}
//        mIat?.setParameter(SpeechConstant.VAD_BOS, "6000")
//        //自动停止录音，范围{0~10000}
//        mIat?.setParameter(SpeechConstant.VAD_EOS, "2000")
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat?.setParameter(SpeechConstant.ASR_PTT, "0")
        // 设置音频来源为外部文件
        mIat?.setParameter(SpeechConstant.AUDIO_SOURCE, "-1")
    }

    /**
     * 执行语音听写任务
     */
    private fun executeRecognizer() {
        // lastUploadTime = System.currentTimeMillis()
        setParam()
        strMsg.setLength(0)
        val ret = mIat?.startListening(mRecognizerListener)
        if (ret != ErrorCode.SUCCESS) {
            Logger.e("识别失败，错误码：$ret")
            return
        }
        Logger.i("语音识别初始化完成！")
        kotlin.runCatching {
            val open = ByteArrayInputStream(keyArrayQueue.popAll())
            val buff = ByteArray(1280)
            while (open.available() > 0) {
                val read = open.read(buff)
                mIat?.writeAudio(buff, 0, read)
            }
            mIat?.stopListening()
        }.onFailure {
            mIat?.cancel()
            Logger.e("读取音频流失败")
        }

    }

    private val mRecognizerListener: RecognizerListener = object : RecognizerListener {
        override fun onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Logger.d("开始说话")
        }

        override fun onError(error: SpeechError) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            Logger.e("onError " + error.getPlainDescription(true))
            // Logger.e(error.getPlainDescription(true))
            // strMsg.setLength(0)
        }

        override fun onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Logger.d("结束说话")
        }

        override fun onResult(results: RecognizerResult, isLast: Boolean) {
            Logger.i(results.resultString)
            val word = results.resultString
            strMsg.append(word)
            if (isLast) {
                val str = strMsg.toString().replace(" ", "")
                Logger.e(
                    "onResult 结束:${str},当前关键词:${curKeyword.trim()},关键词库：$keyWord"
                )
                if (str.contains(curKeyword)) {
                    uploadAlarm(curKeyword)
                } else {
                    for (key in keywordList) {
                        Logger.d("关键词库：$key")
                        if (key.isEmpty()) continue
                        if (str.contains(key.trim())) {
                            Logger.i("识别到关键词库中有包含：$key")
                            // 语音识别到的文字中中包含这个关键词
                            uploadAlarm(key)
                            return
                        }
                    }
                    // 会把他妈的 识别成tmd
                    if (str.contains("tmd")) {
                        uploadAlarm("他妈的")
                    }
                }
            }
        }

        override fun onVolumeChanged(volume: Int, data: ByteArray) {
            Logger.d("当前正在说话，音量大小 = " + volume + " 返回音频数据 = " + data.size)
        }

        override fun onEvent(p0: Int, p1: Int, p2: Int, obj: Bundle?) {
            //  Logger.i("$p0,$p1,$p2")
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
//            if (SpeechEvent.EVENT_SESSION_ID == p0) {
//                val sid = obj?.getString(SpeechEvent.KEY_EVENT_SESSION_ID)
//                Logger.e("session id =$sid")
//            }
        }
    }


    /**
     * 初始化监听器。
     */
    private val mInitListener = InitListener { code ->
        Logger.i(
            "SpeechRecognizer init() code = $code"
        )
        if (code != ErrorCode.SUCCESS) {
            Logger.e("初始化失败，错误码：$code,请点击网址https://www.xfyun.cn/document/error-code查询解决方案")
        }
    }

    /**
     * 获取数据库中的keywords表中的数据
     */
    private suspend fun getKeywords(): MutableList<KeywordBean>? {
        return withContext(Dispatchers.IO) {
            AppDataBase.getInstance().keyWordDao().getAllKey()
        }
    }


    private fun initSocket() {
        Logger.i("初始化initSocket")
        sn?.let {
            MyApp.socketEventViewModel.initSocket(it)
        }
    }

    /**
     * 初始化安卓开发板的api
     */
    private fun initYsAndroidApi() {
        val myManager = MyManager.getInstance(this)
        myManager.bindAIDLService(this)
        myManager.setConnectClickInterface {
            sn = myManager.sn
            Logger.i(
                "当前sdk的版本号：${myManager.firmwareVersion} \n " +
                        "当前设备的型号：${myManager.androidModle} \n" +
                        "设备的系统版本：${myManager.androidVersion} \n " +
                        "当前设备的内存容量：${myManager.runningMemory} \n" +
                        "获取设备的sn码：${myManager.sn}"

            )
            if (myManager.firmwareVersion.toInt() < 4) {
                Logger.i("当前SDK的版本号小于4.0")
            }
            // 开机自启动
            myManager.selfStart("com.example.asrandroidclient")
            // 打开网络adb连接
            myManager.setNetworkAdb(true)
            // 设置守护进程 0:30s  1：60s   2:180s
            myManager.daemon("com.example.asrandroidclient", 1)
            initSocket()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initViewModel() {
        MyApp.mainViewModel.aiRegisterStatus.observe(this) {
            it?.let {
                if (it) {
                    Logger.i("授权信息：${AbilityConstant.IVW_ID.abilityAuthStatus()}")
                    MainScope().launch {
                        if (!isRestart) {
                            Logger.i("设备正在自检中....")
                            textToSpeech?.speak(
                                "设备正在自检中",
                                TextToSpeech.QUEUE_ADD,
                                null,
                                null
                            )
                            delay(2000)
                            startRecord()
                        } else {
                            delay(1000)
                            startRecord()
                        }
                    }
                }
            }
        }

        MyApp.socketEventViewModel.msgEvent.observe(this) {
            val text = msgTV.text.toString()
            msgTV.text = text + "\n" + it
        }

        MyApp.socketEventViewModel.keywordUpdateEvent.observe(this) {
            it?.let {
                when (it) {
                    0, 1, 2 -> {
                        Logger.i("keyword 增删改 需要重启科大讯飞引擎")
                        isRestart = true
                        restartIvw()
                    }

                    3 -> {

                    }
                }
            }
        }
        MyApp.socketEventViewModel.voiceUpdateEvent.observe(this) {
            it?.let {
                Logger.i("更新voice信息")
                //initVoice()
                when (it) {
                    0, 1, 2 -> {
                    }

                    3 -> {}
                }
            }
        }

        MyApp.webrtcSocketManager.callEvent.observe(this) {
            if (it == true) {
                audioManager?.isSpeakerphoneOn = true
                val maxVolume =
                    audioManager?.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL) ?: 100
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_VOICE_CALL, AudioManager.ADJUST_RAISE,
                    maxVolume
                )
                isVoiceCall = true
                destroyIvw()
                isRestart = true
            } else {
                isVoiceCall = false
                isRestart = true
                startIvw()
            }
        }

        MyApp.socketEventViewModel.appUpdateEvent.observe(this) {
            it?.let { app ->
                if (isUpdate) {
                    return@let
                }
                //  if (app.versionCode > BuildConfig.VERSION_CODE) {
                textToSpeech?.speak("正在更新系统", TextToSpeech.QUEUE_ADD, null, null)
                val manager = DownloadManager.Builder(this).run {
                    apkUrl(app.fileUrl)
                    apkName(app.fileName)
                    smallIcon(R.mipmap.ic_launcher)
                    onDownloadListener(listenerAdapter)
                    build()
                }
                manager.download()
                MyApp.socketEventViewModel.uploadState("4", "正在更新中")
                isUpdate = true
                Logger.i("服务器最新版本和本地版本一致")
            }
        }
        MyApp.mainViewModel.bleConnectedEvent.observe(this) {
            if (it) {
                textToSpeech?.speak("连接已建立", TextToSpeech.QUEUE_ADD, null, null)

            }
        }
        MyApp.mainViewModel.networkConfigEvent.observe(this) {
            if (it == 1) {
                textToSpeech?.speak("正在配网中", TextToSpeech.QUEUE_ADD, null, null)
                sn?.let { sn ->
                    MyApp.mainViewModel.sendSnCOde(sn)
                }
            } else if (it == 3) {
                textToSpeech?.speak("配网已完成", TextToSpeech.QUEUE_ADD, null, null)
                sn?.let { sn ->
                    MyApp.mainViewModel.sendNetworkFinish(sn)
                }
                restartIvw()
            } else if (it == 4) {
                textToSpeech?.speak("设备已配置，无需再进行配网", TextToSpeech.QUEUE_ADD, null, null)
                MyApp.mainViewModel.sendHasRegister()
            }
        }

    }

    private var curProgress: Int = 0
    private val listenerAdapter: OnDownloadListenerAdapter = object : OnDownloadListenerAdapter() {

        override fun downloading(max: Int, progress: Int) {
            val curr = (progress / max.toDouble() * 100.0).toInt()
            Logger.i("当前下载进度：$curr")
            if (curr != curProgress) {
                curProgress = curr
                MyApp.socketEventViewModel.uploadState("4", "正在更新中", curProgress.toString())
            }
            //updateAppDialog?.setProgress(curr)
        }
    }

    private var isUpdate = false

    private fun initIvw() {
        isBeingStarted = true
        ivwHelper = IvwHelper(this).apply {
            setRecorderCallback(recorderCallback)
        }
    }

    override fun onInit(p0: Int) {
        // 判断是否转化成功
        if (p0 == TextToSpeech.SUCCESS) {
            val languageCode = textToSpeech?.setLanguage(Locale.CHINA)
            // 判断是否支持中文，Android原生不支持中文，需要在设置/语言和输入法/文字转语音输出/里面将引擎改为科大讯飞的语音引擎
            if (languageCode == TextToSpeech.LANG_NOT_SUPPORTED) {
                Logger.i("语音不支持")
            } else {
                textToSpeech?.setLanguage(Locale.US)
            }
            textToSpeech?.setPitch(1.0f)
            textToSpeech?.setSpeechRate(1.0f)
        }
        Logger.i("语音播报引擎初始化：$p0")
    }

    /**
     * 开始录音
     */
    private fun startRecord() {
        MainScope().launch(Dispatchers.IO) {
            var keyStr = ""
            val keywords = getKeywords()
            if (keywords.isNullOrEmpty() == true) {
                MyApp.socketEventViewModel.getMsg()
                return@launch
            }
            if (keywords != null) {
                for (key in keywords) {
                    keyStr = keyStr + key.keyword + ";"
                }
                Logger.i("将数据库中的关键字写入到讯飞配置文件中：$keyStr")
                keyWord = keyStr
                val filePath = createKeywordFile(keyWord)
                keywordList = keyWord.trim().split(";").toMutableList()
                val keywordSize = keywordList.count()
                withContext(Dispatchers.Main) {
                    ivwHelper?.startAudioRecord(filePath, keywordSize, threshold)
                    Logger.i("重新启动录音，正在录音中...")
                }
            }
        }

    }

    private fun checkIVW() {
        MainScope().launch(Dispatchers.IO) {
            while (isRunning) {
                // 每30s检查一次  语音引擎是否在线
                delay(20 * 1000)
                // 语音引擎未启动，并且不在通话状态、也不在正在启动引擎
                if (!ivwIsOpen && !isVoiceCall && !isBeingStarted) {
                    Logger.i("设备语音引擎未启动，并且不在通话和正在初始化语音引擎的状态")
                    withContext(Dispatchers.Main) {
                        restartIvw()
                    }
                } else {
                    //  MyApp.socketEventViewModel.uploadState("2", "故障解除")
//                    Logger.d("语音引擎是否启动：$ivwIsOpen,是否正在通话：$isVoiceCall,是否正在引擎初始化中：$isBeingStarted")
                }
                if (!BleManager.getInstance().isBlueEnable) {
                    BleManager.getInstance().enableBluetooth()
                }
            }
        }
    }


    /**
     * 生成关键词文件
     */
    private fun createKeywordFile(key: String): String {
        val file = File(MyApp.CONTEXT.externalCacheDir, "keyword.txt")
        if (file.exists()) {
            file.delete()
        }
        val binFile = File("${MyApp.CONTEXT.externalCacheDir}/process", "key_word.bin")
        if (binFile.exists()) {
            binFile.delete()
        }
        kotlin.runCatching {
            val keyword = key
                .replace("；", ";")
                .replace(";", ";\n")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
            val bufferedWriter =
                BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charset.forName("GBK")))
//            val bufferedWriter = BufferedWriter(FileWriter(file))
            bufferedWriter.write(keyword)
            bufferedWriter.close()
            Logger.i("关键字：$keyword")
        }.onFailure {
            Logger.e("唤醒词写入失败${it.message}")
        }
        return file.absolutePath
    }


    private val recorderCallback = object : RecorderCallback {

        override fun onStartRecord() {
            startRecordTime = System.currentTimeMillis()
            Logger.i("开始录音")
        }

        override fun onPauseRecord() {
        }

        override fun onResumeRecord() {
        }

        override fun onRecordProgress(data: ByteArray, sampleSize: Int, volume: Int) {
            keyArrayQueue.append(data)
            if (keyArrayQueue.size > keyMaxSize) {
                keyArrayQueue.pop(keyArrayQueue.size - keyMaxSize)
            }
            writeByteToQueue(data)
            calculateVolume = data.calculateVolume()
            //从刚刚开始录音500ms以后开始判断是否拾音器故障
            if (System.currentTimeMillis() - startRecordTime < 1500) {
                return
            }
            if (calculateVolume < 40) {
                onLineDbCount = 0
                offlineDb++
                if (offlineDb > 5) {
                    Logger.i("当前分贝连续3次采集小于20：$calculateVolume")
                    if (isMicOnline) {
                        MyApp.socketEventViewModel.uploadState("3", "拾音器故障")
                    }
                    isMicOnline = false
                }
            }
            if (calculateVolume > 40) {
                onLineDbCount++
                offlineDb = 0
                if (onLineDbCount > 10) {
                    //   Logger.i("当前大于35分贝：$calculateVolume")
                    onLineDbCount = 0
                    if (!isMicOnline) {
                        isMicOnline = true
                        MyApp.socketEventViewModel.uploadState("2", "故障已解除")
                    }
                }
            }
            volumeQueue.let {
                it.append(calculateVolume)
                if (it.size > 15) {
                    it.pop(it.size - 15)
                }
            }

        }

        override fun onStopRecord(output: File?) {
        }

    }
    private var maxDb: Int = 0
    private fun showVolumeDb() {
        MainScope().launch(Dispatchers.IO) {
            while (isRunning) {
                delay(200)
                maxDb = if (calculateVolume > maxDb) calculateVolume else maxDb
                // Logger.d("当前声音分贝：$calculateVolume,目前最大值：$maxDb")
            }
        }
    }

    override fun onAbilityBegin() {
        Logger.i("语音唤醒正在开始中...")
        if (!isRestart)
            postDelayed({
                textToSpeech?.speak("系统已启动", TextToSpeech.QUEUE_ADD, null, null)
            }, 1000)
        ivwIsOpen = true
        // 已经启动完成
        isBeingStarted = false
        isRestart = true

    }

    private var lastUploadTime = 0L
    private var rtl: Rlt? = null
    private var credibility: Int = 0
    private var enable = false
    private var keywordBean: KeywordBean? = null
    private var volume: Int = 0
    private lateinit var curKeyword: String
    override fun onAbilityResult(result: String) {
        volume = analyseVolume()
        val rs = result.replace("func_wake_up:", "")
        runCatching {
            val speechResult = Gson().fromJson(rs, SpeechResult::class.java)
            rtl = speechResult.rlt[0]
            if (rtl == null) {
                return
            }
            curKeyword = rtl!!.keyword
            MainScope().launch(Dispatchers.IO) {
                keywordBean =
                    AppDataBase.getInstance().keyWordDao().findByKeyword(curKeyword)
                credibility = keywordBean?.credibility ?: 900
                enable = keywordBean?.enabled ?: true
                val voiceId = keywordBean?.voiceId
                val voiceBean = AppDataBase.getInstance().voiceDao().findById(voiceId)
                speechMsg = voiceBean?.text ?: "请勿打架斗殴"
                speechMsgTimes = voiceBean?.times ?: 1
                Logger.e("触发唤醒关键字：${curKeyword},关键字得分：${rtl?.ncm_keyword}，门限值：${rtl?.ncmThresh}，置信度：$credibility，是否启用：${keywordBean?.enabled},speechMsg:$speechMsg，speechTimes:$speechMsgTimes,当前分贝：$volume")
                //withContext(Dispatchers.Main) {
                if (System.currentTimeMillis() - lastUploadTime < 5 * 1000) {
                    return@launch
                }
                if (rtl!!.ncm_keyword < 1200) {
                    delay(800)
                    executeRecognizer()
                    // 二次检验
                } else {
                    uploadAlarm(curKeyword)
                }

                //    }
            }
        }.onFailure {
            Logger.e("error:${it.message}")
            ivwHelper?.destroy()
        }

    }

    /**
     * 语音报警加上传至平台
     */
    private fun uploadAlarm(
        key: String
    ) {
        MainScope().launch(Dispatchers.IO){
            if (key != curKeyword) {
                keywordBean =
                    AppDataBase.getInstance().keyWordDao().findByKeyword(key)
                credibility = keywordBean?.credibility ?: 900
                enable = keywordBean?.enabled ?: true
                val voiceId = keywordBean?.voiceId
                val voiceBean = AppDataBase.getInstance().voiceDao().findById(voiceId)
                speechMsg = voiceBean?.text ?: "请勿打架斗殴"
                speechMsgTimes = voiceBean?.times ?: 1
            }
            Logger.i("准备上传信息,并报警")
            if (rtl != null) {
                if (enable) {
                    for (i in 0 until speechMsgTimes) {
                        textToSpeech?.speak(
                            speechMsg,
                            TextToSpeech.QUEUE_ADD,
                            null,
                            null
                        )
                    }
                    val alarmFile = writeBytesToFile()
                    val wavPath =
                        FileUtil.getAlarmCacheDir() + "/" + (System.currentTimeMillis()).stampToDate() + ".wav"
                    lastUploadTime = System.currentTimeMillis()
                    //if (rs.contains("救命救命")) {
                    if (keywordBean != null) {
                        val keyId = keywordBean?.keywordId?.toLong() ?: 0
                        val ncm = rtl!!.ncm_keyword
                        val duration = (rtl!!.iduration * 10).toString()
                        if (alarmFile != null) {
                            PcmToWavConverter.pcmToWav(alarmFile, wavPath)
                        }
                        if (alarmFile != null)
                            MyApp.socketEventViewModel.getUploadFileUrl(
                                key,
                                keyId,
                                ncm,
                                duration,
                                File(wavPath),
                                volume
                            )
                    }
                    // }
                }

            }
        }

    }


    /**
     * 分析当前15帧数据
     */
    private fun analyseVolume(): Int {
        var count = 0
        var sum = 0
        for (a in volumeQueue.getAll()) {
            if (a > 50) {
                count++
                sum += a
//                Logger.i("volume:$a")
            }
        }
        if (count == 0) count = 1
        return sum / count
    }


    fun openSpeaker() {
        audioManager?.isSpeakerphoneOn = true
    }


    override fun onAbilityError(code: Int, error: Throwable?) {
        Logger.e("语音唤醒error：$code,msg:${error?.message}")
        ivwHelper?.stopAudioRecord()
        ivwIsOpen = false
        // 已经启动完成
        isBeingStarted = false
    }

    override fun onAbilityEnd() {
        Logger.i("语音唤醒已结束...")
        ivwHelper?.stopAudioRecord()
        ivwHelper?.endAiHandle()
        // 已经启动完成
        isBeingStarted = false
        ivwIsOpen = false
        //isRestart = false
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        isRunning = false
        isRestart = false
        ivwHelper?.destroy()
        ivwHelper = null
        super.onBackPressed()
    }

    override fun onDestroy() {
        isRunning = false
        isRestart = false
        ivwHelper?.destroy()
        ivwHelper = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    override fun finish() {
        isRunning = false
        isRestart = false
        ivwHelper?.destroy()
        ivwHelper = null
        super.finish()
    }


    private var byteArrayQueue = ByteArrayQueue()
    private var keyArrayQueue = ByteArrayQueue()

    private var keyMaxSize = AudioRecorder.SAMPLE_RATE_IN_HZ * AudioRecorder.AUDIO_FORMAT * 5

    // 30秒音频所占的格式
    private var maxQueueSize = AudioRecorder.SAMPLE_RATE_IN_HZ * AudioRecorder.AUDIO_FORMAT * 30


    private fun restartIvw() {
        MainScope().launch {
            ivwHelper?.destroy()
            ivwHelper = null
            delay(1000)
            Logger.i("重新初始化科大讯飞引擎")
            IFlytekAbilityManager.getInstance().initializeSdk(MyApp.CONTEXT)
            initIvw()
            Logger.i("重新初始化科大讯飞引擎")
        }
    }

    /*
     * 销毁关闭
     */
    private fun destroyIvw() {
        ivwHelper?.destroy()
        ivwHelper = null
    }

    /**
     * 启动语音引擎
     */
    private fun startIvw() {
        IFlytekAbilityManager.getInstance().initializeSdk(MyApp.CONTEXT)
        initIvw()
    }

    /**
     * 把字节数组写入到字节数组队列中
     */
    private fun writeByteToQueue(data: ByteArray) {
        byteArrayQueue.append(data)
        if (byteArrayQueue.size > maxQueueSize) {
            byteArrayQueue.pop(byteArrayQueue.size - maxQueueSize)
        }
    }


    /**
     * 将缓冲区中的音频写入到字节数组中
     */
    private fun writeBytesToFile(): String? {
        kotlin.runCatching {
            val data = byteArrayQueue.popAll()
            val pcmName = (System.currentTimeMillis()).stampToDate() + ".pcm"
            val file = File(FileUtil.getAlarmCacheDir(), pcmName)
            val outputStream = FileOutputStream(file)
            outputStream.write(data)
            // 关闭输出流
            outputStream.close()
            return file.absolutePath
        }.onFailure {
            Logger.e("报警音频写入失败：${it.message}")
        }
        return null
    }

    override fun onPause() {
        super.onPause()
        Logger.i("pause")
    }


    override fun onStop() {
        // ivwHelper?.stopAudioRecord()
        super.onStop()
        Logger.i("stop")
    }


}
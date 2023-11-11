package com.example.asrandroidclient

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.asrandroidclient.ability.AbilityCallback
import com.example.asrandroidclient.ability.AbilityConstant
import com.example.asrandroidclient.ability.IFlytekAbilityManager
import com.example.asrandroidclient.ability.abilityAuthStatus
import com.example.asrandroidclient.ivw.IvwHelper
import com.example.asrandroidclient.media.audio.RecorderCallback
import com.example.asrandroidclient.tool.calculateVolume

import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions

import com.orhanobut.logger.Logger
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.util.Locale

class MainActivity : AppCompatActivity(), HandlerAction, AbilityCallback,
    TextToSpeech.OnInitListener {

    private var ivwHelper: IvwHelper? = null
    private var keyWord: String = "救命;救命救命;服不服;打死你;单挑啊；大白大白;小迪小迪;小艺小艺"
    private var threshold: Int = 900     //范围 0-3000

    private var textToSpeech: TextToSpeech? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Logger.i("应用启动")
        textToSpeech = TextToSpeech(MyApp.CONTEXT, this)
        XXPermissions.with(this)
            // .permission(Permission.MANAGE_EXTERNAL_STORAGE)
            .permission(Permission.RECORD_AUDIO)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .permission(Permission.READ_EXTERNAL_STORAGE)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                    Logger.i("录音权限获取成功")
                    if (all) {
                        IFlytekAbilityManager.getInstance().initializeSdk(MyApp.CONTEXT)
                    }

                }

                override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                    super.onDenied(permissions, never)
                    Logger.i("权限获取失败")
                }
            })
        initIvw()
        initViewModel()
    }


    private fun initViewModel() {
        MyApp.mainViewModel.aiRegisterStatus.observe(this) {
            it?.let {
                if (it) {
                    Logger.i("授权信息：${AbilityConstant.IVW_ID.abilityAuthStatus()}")
                    startRecord()
                }
            }
        }
    }

    private fun initIvw() {
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

    }

    /**
     * 开始录音
     */
    private fun startRecord() {
        val filePath = createKeywordFile()
        val keywordSize = keyWord.trim().split(";").count()
        ivwHelper?.startAudioRecord(filePath, keywordSize, threshold)
        Logger.i("启动唤醒程序，正在录音中...")
    }

//    /**
//     * 停止录音
//     */
//    private fun stopRecord() {
//        Logger.i("暂停唤醒程序，停止录音中")
//        ivwHelper?.stopAudioRecord()
//        ivwHelper = null
//        Logger.i("暂停唤醒程序，停止录音中")
//    }


    override fun onPause() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onPause()
    }

    override fun onStop() {
        Logger.i("退出app")
        // ivwHelper?.stopAudioRecord()
        super.onStop()
    }


    private fun createKeywordFile(): String {
        val file = File(MyApp.CONTEXT.externalCacheDir, "keyword.txt")
        if (file.exists()) {
            file.delete()
        }
        val binFile = File("${MyApp.CONTEXT.externalCacheDir}/process", "key_word.bin")
        if (binFile.exists()) {
            binFile.delete()
        }
        kotlin.runCatching {
            val keyword = keyWord
                .replace("；", ";")
                .replace(";", ";\n")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
            val bufferedWriter =
                BufferedWriter(OutputStreamWriter(FileOutputStream(file), Charset.forName("GBK")))
//            val bufferedWriter = BufferedWriter(FileWriter(file))
            bufferedWriter.write(keyword)
            bufferedWriter.close()
        }.onFailure {
            Logger.e("唤醒词写入失败${it.message}")
        }
        return file.absolutePath
    }


    private val recorderCallback = object : RecorderCallback {

        override fun onStartRecord() {}

        override fun onPauseRecord() {
        }

        override fun onResumeRecord() {
        }

        override fun onRecordProgress(data: ByteArray, sampleSize: Int, volume: Int) {
            val calculateVolume = data.calculateVolume()
            Logger.d("当前分贝:$calculateVolume")
            if (calculateVolume > 60) {
                Logger.i("禁止喧哗吵闹")
                textToSpeech?.speak("禁止喧哗吵闹", TextToSpeech.QUEUE_ADD, null, null)
            }

        }

        override fun onStopRecord(output: File?) {
        }

    }

    override fun onAbilityBegin() {
        Logger.i("语音唤醒正在开始中...")

    }

    override fun onAbilityResult(result: String) {
        Logger.i("触发唤醒关键字：$result")
        textToSpeech?.speak("禁止打架斗殴", TextToSpeech.QUEUE_ADD, null, null)

    }

    override fun onAbilityError(code: Int, error: Throwable?) {
        Logger.e("语音唤醒error：$code,msg:${error?.message}")
        ivwHelper?.stopAudioRecord()
    }

    override fun onAbilityEnd() {
        Logger.i("语音唤醒已结束...")
        ivwHelper?.stopAudioRecord()
        ivwHelper?.endAiHandle()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        ivwHelper?.destroy()
        ivwHelper = null
        super.onBackPressed()
    }

    override fun onDestroy() {
        ivwHelper?.destroy()
        ivwHelper = null
        super.onDestroy()
    }

    override fun finish() {
        ivwHelper?.destroy()
        ivwHelper = null
        super.finish()
    }


}
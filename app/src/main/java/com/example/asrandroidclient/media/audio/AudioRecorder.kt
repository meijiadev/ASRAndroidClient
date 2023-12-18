package com.example.asrandroidclient.media.audio

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.annotation.RequiresPermission
import com.example.asrandroidclient.MyApp
import com.example.asrandroidclient.tool.ByteArrayQueue
import com.example.asrandroidclient.tool.PCMEncoderAAC
import com.example.asrandroidclient.tool.mainThread
import com.example.asrandroidclient.tool.stampToDate
import com.orhanobut.logger.Logger
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * @Desc: 简单的音频播放器
 * @Author leon
 * @Date 2023/3/8-15:49
 * Copyright 2023 iFLYTEK Inc. All Rights Reserved.
 */
class AudioRecorder private constructor() : Recorder {

    private val TAG = "AudioRecorder"

    private var audioRecord: AudioRecord? = null

    private var recordFile: File? = null
    private var recordingThread: Thread? = null
    private var bufferSize = 0
    private var recordCallback: RecorderCallback? = null

    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    // 当前录音文件的名称，开始录音时的时间戳
    //private var cursTime: String? = null
    // 开始录音的时间戳
    private var curTime: Long = 0

    // 当前录音的文件路径
    private var curAudioFilePath: String? = null
    private var byteArrayQueue = ByteArrayQueue()

    // 30秒音频所占的格式
    private var maxQueueSize = SAMPLE_RATE_IN_HZ * AUDIO_FORMAT * 30


    companion object {

        const val SAMPLE_RATE_IN_HZ = 16000

        //        const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val AUDIO_SOURCE = MediaRecorder.AudioSource.VOICE_COMMUNICATION
        const val CHANNEL_CONFIGURATION = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

        @JvmStatic
        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { AudioRecorder() }
    }


    @SuppressLint("MissingPermission")
    fun init() {
        if (null != audioRecord) {
            audioRecord?.release()
        }
        try {
            bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE_IN_HZ, CHANNEL_CONFIGURATION, AUDIO_FORMAT
            )
            audioRecord = AudioRecord(
                AUDIO_SOURCE, SAMPLE_RATE_IN_HZ,
                CHANNEL_CONFIGURATION, AUDIO_FORMAT, bufferSize
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            throw AudioRecordException("初始化录音失败")
        }

    }

    override fun setRecorderCallback(callback: RecorderCallback?) {
        recordCallback = callback
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun startRecording() {
        Logger.i("....开始录音....")
        when (audioRecord?.state) {
            AudioRecord.STATE_INITIALIZED -> {
                try {
                    audioRecord?.startRecording()
                } catch (e: Exception) {
                    throw AudioRecordException("录音失败")
                }
            }

            AudioRecord.STATE_UNINITIALIZED -> {
                init()
                audioRecord?.startRecording()
            }

            else -> {
                throw AudioRecordException("录音失败")
            }
        }
        isRecording.set(true)
        isPaused.set(false)
        //createAudioFile()
        recordingThread = Thread(RecordThread(), "RecordThread")
        try {
            recordingThread?.start()
            mainThread {
                recordCallback?.onStartRecord()
            }
        } catch (e: Exception) {
            throw AudioRecordException("录音失败")
        }
    }

    /**
     * 创建音频文件
     */
    private fun createAudioFile() {
        curTime = System.currentTimeMillis()
        val cursTime = curTime.stampToDate()
        recordFile = File(
            MyApp.CONTEXT.externalCacheDir?.absolutePath ?: "",
            "$cursTime.pcm"
        )
        curAudioFilePath = recordFile?.absolutePath
        Logger.i("当前录音的绝对路径：$curAudioFilePath")
    }


    override fun resumeRecording() {
        if (audioRecord != null && audioRecord!!.state == AudioRecord.STATE_INITIALIZED) {
            if (isPaused.get()) {
                audioRecord?.startRecording()
                mainThread {
                    recordCallback?.onResumeRecord()
                }
                isPaused.set(false)
            }
        }
    }

    override fun pauseRecording() {
        if (audioRecord != null && isRecording.get()) {
            audioRecord?.stop()
            isPaused.set(true)
            mainThread {
                recordCallback?.onPauseRecord()
            }
        }
    }

    override fun stopRecording() {
        if (audioRecord != null) {
            isRecording.set(false)
            isPaused.set(false)
            if (audioRecord!!.state == AudioRecord.STATE_INITIALIZED) {
                try {
                    audioRecord?.stop()
                    Logger.i("stopRecording")
                } catch (e: IllegalStateException) {
                    Logger.e("stopRecording() problems:${e.message}")
                }
            }
            Logger.i("stopRecording${audioRecord!!.state}")
            audioRecord?.release()
            recordingThread?.interrupt()
            mainThread {
                recordCallback?.onStopRecord(recordFile)
            }
        }
    }

    override fun isRecording(): Boolean {
        return isRecording.get()
    }

    override fun isPaused(): Boolean {
        return isPaused.get()
    }

    inner class RecordThread : Runnable {

        override fun run() {
//            var fos: FileOutputStream? = try {
//                FileOutputStream(recordFile)
//            } catch (e: FileNotFoundException) {
//                Log.e(TAG, "", e)
//                null
//            }
            val scoringBufferMaxSize = bufferSize
            val audioData = ByteArray(scoringBufferMaxSize)
            while (isRecording()) {
//                // 每超过十分钟就重新新建一个文件
//                val time = System.currentTimeMillis() - curTime
//                if (time > 10 * 1000 * 60) {
//                    createAudioFile()
//                    fos = try {
//                        FileOutputStream(recordFile)
//                    } catch (e: FileNotFoundException) {
//                        Log.e(TAG, "", e)
//                        null
//                    }
//                }
                val localPaused = isPaused()
                if (localPaused) {
                    continue
                }
                val audioSampleSize = getAudioRecordBuffer(
                    scoringBufferMaxSize, audioData
                )
                if (audioSampleSize > 0) {
                    val x =
                        abs(audioData[0].toInt()).toFloat() / Short.MAX_VALUE
                    val recordVolume = ((2 * x - x * x) * 9).roundToInt()
                    if (audioSampleSize == scoringBufferMaxSize) {
                        mainThread {
                            recordCallback?.onRecordProgress(
                                audioData,
                                audioSampleSize,
                                recordVolume
                            )
                        }
                        //writeToFile(audioData)
                    } else {
                        val copy = ByteArray(audioSampleSize)
                        System.arraycopy(audioData, 0, copy, 0, audioSampleSize)
                        mainThread {
                            recordCallback?.onRecordProgress(copy, audioSampleSize, recordVolume)
                        }
                        //writeToFile(copy)
                    }
                }
            }
//            try {
//                fos?.flush()
//                fos?.close()
//            } catch (e: IOException) {
//                Log.e(TAG, "", e)
//            }
        }
    }


    private fun getAudioRecordBuffer(
        scoringBufferMaxSize: Int,
        audioSamples: ByteArray
    ): Int {
        return audioRecord?.read(
            audioSamples,
            0,
            scoringBufferMaxSize
        ) ?: 0
    }

}
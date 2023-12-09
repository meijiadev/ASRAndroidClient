package com.example.asrandroidclient


import android.annotation.SuppressLint
import android.content.ContentResolver
import android.database.Cursor
import android.provider.MediaStore
import androidx.core.net.toUri
import com.example.asrandroidclient.file.FileUtil
import com.example.asrandroidclient.media.audio.AudioRecorder
import com.example.asrandroidclient.tool.DateUtil
import com.example.asrandroidclient.tool.dateToStamp
import com.example.asrandroidclient.tool.stampToDate
import com.orhanobut.logger.Logger
import java.io.File


/**
 * Create by MJ on 2023/12/7.
 * Describe :
 */

object AudioFileUtil {

    /**
     * 获取音频文件创建时的时间戳
     * @return 时间戳列表
     */
    fun getAllAudio(): MutableList<File>? {
        return FileUtil.listFilesInDirectoryEndWithSuffix(
            MyApp.CONTEXT.externalCacheDir?.absolutePath,
            ".pcm",
            false,
            true
        )
    }

    /**
     * 截取报警时间前两分钟的音频文件
     * @param waringTime 报警时间
     */
    suspend fun splitAudioFile(waringTime: Long) {
        Logger.i("准备截取pcm音频")
        val length = 30 * 1000L
        val start = waringTime - 31 * 1000
        // 需要截取
        val files = getAllAudio()
        var startIndex: Long = 0
        var sourcePath: String? = null
        files?.let {
            for (audio in it) {
                val time = audio.name.replace(".pcm", "")
                val tm = time.dateToStamp()
                val tmLength = getDuration(audio)
                val tmEnd = tm + tmLength
                Logger.i("start:${start.stampToDate()},startTime:$time,endTime:${tmEnd.stampToDate()},$tmEnd,waringTime:${waringTime.stampToDate()},$waringTime")
                // 开始和结束时间都在一个文件中
                if ((start in (tm + 1) until tmEnd)) {
                    if (tmLength > length) {
                        sourcePath = audio.absolutePath
                        startIndex = start - tm
                        Logger.i("start:$start,tm:$tm,time:$time,pcmLs$tmLength")
                    }
                }
            }
            val targetPath = FileUtil.getAlarmCacheDir() + "/" + waringTime.stampToDate() + ".pcm"
            if (startIndex != 0L && sourcePath != null) {
                // 格式为 00:00:00
                val startTime = getTime(startIndex)
                val lengthTime = getTime(length)
                Logger.i("startIndex:$startIndex,$startTime,length:$length,$lengthTime")
                 Logger.i("sour:$sourcePath,target:$targetPath,start:$startTime,length:$lengthTime")
                // val result = getAudioSplit(sourcePath!!, targetPath, startTime!!, lengthTime!!)
                // Logger.i("音频剪裁结果：$result")
            }

        }
    }


    /**
     * 把长整型转成时分秒
     */
    private fun getTime(time: Long): String {
        var sHour: String
        var sMinute: String
        var sSeconds: String
        val seconds = ((time / 1000) % 60).toInt()
        val minutes = ((time / (1000 * 60)) % 60).toInt()
        val hour = ((time / (1000 * 60 * 60)) % 60).toInt()
        sHour = if (hour < 10) {
            "0$hour"
        } else {
            hour.toString()
        }
        sMinute = if (minutes < 10) {
            "0$minutes"
        } else {
            minutes.toString()
        }
        sSeconds = if (seconds < 10) {
            "0$seconds"
        } else {
            seconds.toString()
        }
        return "$sHour:$sMinute:$sSeconds"
    }


    @SuppressLint("MissingPermission")
    fun getDuration(audioFile: File): Long {
        Logger.d("正在获取文件时长")
        val fileSize = audioFile.length()
        val time =
            (fileSize / (AudioRecorder.SAMPLE_RATE_IN_HZ * AudioRecorder.AUDIO_FORMAT)) * 1000
        Logger.d("name:${audioFile.name},时长：${time / 1000}秒,文件长度：${fileSize / 1024 / 1024}M")
        return time
    }


}
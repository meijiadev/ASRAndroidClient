//package com.example.asrandroidclient.tool
//
//import com.orhanobut.logger.Logger
//import io.microshow.rxffmpeg.RxFFmpegCommandList
//import io.microshow.rxffmpeg.RxFFmpegInvoke
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
///**
// * Create by MJ on 2023/12/7.
// * Describe :
// */
//
///**
// * 获取裁剪的音频
// */
//suspend fun getAudioSplit(
//    sourcePath: String,
//    targetPath: String,
//    start: String,
//    length: String
//): Int = withContext(Dispatchers.IO) {
//    val cmds = getAudioSplitCmd(sourcePath, targetPath, start, length)
//    var cmd = ""
//    for (s in cmds) {
//        cmd += s
//    }
//    Logger.i("cmds:${cmd}")
//    runFFmpegCmd(cmds)
//}
//
///**
// * 执行命令
// */
//suspend fun runFFmpegCmd(cmd: Array<String>): Int = withContext(Dispatchers.IO) {
//    RxFFmpegInvoke.getInstance().runCommand(cmd, object : RxFFmpegInvoke.IFFmpegListener {
//        override fun onFinish() {
//            Logger.i("ffmpeg 执行结束")
//
//        }
//
//        override fun onProgress(progress: Int, progressTime: Long) {
//            Logger.i("ffmpeg progre：$progress")
//        }
//
//        override fun onCancel() {
//            Logger.i("ffmpeg 命令取消")
//
//        }
//
//        override fun onError(message: String?) {
//            Logger.e("ffmpeg 执行报错：$message")
//        }
//    })
//}
//
///**
// * 获取裁剪音频的cmd
// */
//private fun getAudioSplitCmd(
//    sourcePath: String,
//    targetPath: String,
//    start: String,
//    length: String
//): Array<String> {
//    val cmdList = RxFFmpegCommandList()
//    cmdList.append("-i")
//    cmdList.append(sourcePath)
//    cmdList.append("-ss")
//    cmdList.append(start)
//    cmdList.append("-t")
//    cmdList.append(length)
//    cmdList.append("-c")
//    cmdList.append("copy")
//    cmdList.append(targetPath)
//    return cmdList.build()
//}
//
//suspend fun pcmToMP3(sourcePath: String, targetPath: String) {
//    val cmdList = RxFFmpegCommandList()
//    cmdList.append("-f")
//    cmdList.append("s16le")
//    cmdList.append("-ar")
//    cmdList.append("16000")
//    cmdList.append("-ac")
//    cmdList.append("2")
//    cmdList.append("-i")
//    cmdList.append(sourcePath)
//    cmdList.append("-codec:a")
//    cmdList.append("libmp3lame")
//    cmdList.append(targetPath)
//    val cmds = cmdList.build()
//    var cmd = ""
//    for (s in cmds) {
//        cmd += s
//    }
//    Logger.i("cmds:${cmd}")
//    runFFmpegCmd(cmds)
//}
//
//suspend fun getPcmFileTimes(sourcePath: String) {
//    val cmdList = RxFFmpegCommandList()
//    cmdList.append("-i")
//    cmdList.append(sourcePath)
//    cmdList.append("-f")
//    cmdList.append("null -")
//    val cmds = cmdList.build()
//    runFFmpegCmd(cmds)
//}
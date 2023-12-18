package com.example.asrandroidclient.tool

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Create by MJ on 2023/12/18.
 * Describe :将pcm格式音频转成wav格式
 */
object PcmToWavConverter {
    fun pcmToWav(pcmPath: String?, wavPath: String?) {
        val audioLength: Long
        val byteRate = (16 * 16000 * 1 / 8).toLong()
        val data = ByteArray(1024)
        try {
            val `in` = FileInputStream(pcmPath)
            val out = FileOutputStream(wavPath)
            audioLength = `in`.channel.size()
            val dataLength = audioLength + 36
            writeWaveFileHeader(out, audioLength, dataLength, byteRate)
            while (`in`.read(data) != -1) {
                out.write(data)
            }
            `in`.close()
            out.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun writeWaveFileHeader(
        out: FileOutputStream, audioLength: Long, dataLength: Long,
        byteRate: Long
    ) {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (dataLength and 0xffL).toByte()
        header[5] = (dataLength shr 8 and 0xffL).toByte()
        header[6] = (dataLength shr 16 and 0xffL).toByte()
        header[7] = (dataLength shr 24 and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = 1
        header[23] = 0
        header[24] = (16000 and 0xff).toByte()
        header[25] = (16000 shr 8 and 0xff).toByte()
        header[26] = (16000 shr 16 and 0xff).toByte()
        header[27] = (16000 shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = (byteRate shr 8 and 0xffL).toByte()
        header[30] = (byteRate shr 16 and 0xffL).toByte()
        header[31] = (byteRate shr 24 and 0xffL).toByte()
        header[32] = (2 * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = 16 // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (audioLength and 0xffL).toByte()
        header[41] = (audioLength shr 8 and 0xffL).toByte()
        header[42] = (audioLength shr 16 and 0xffL).toByte()
        header[43] = (audioLength shr 24 and 0xffL).toByte()
        out.write(header, 0, 44)
    }
}
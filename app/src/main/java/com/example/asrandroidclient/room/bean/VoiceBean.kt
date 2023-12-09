package com.example.asrandroidclient.room.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.asrandroidclient.webrtc.data.voice.DataX
import com.example.asrandroidclient.webrtc.data.voice.VoiceData

/**
 * Create by MJ on 2023/12/4.
 * Describe :
 */

@Entity(tableName = "voices")
data class VoiceBean(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(name = "time")
    var time: String,                       //命令下发时间
    @ColumnInfo(name = "voiceId")
    var voiceId: String,     // 关键字Id
//    @ColumnInfo(name = "data")
//    var voiceData: DataX
    @ColumnInfo(name = "defaultFlag")
    var defaultFlag: Boolean,                      // 是否为默认
    @ColumnInfo(name = "delFlag")
    var delFlag: String,               // 删除标识    1：被删除
    @ColumnInfo(name ="orgId")
    var orgId: String,                 // 所属组织
    @ColumnInfo(name = "text")
    var text: String,                  // 语音文本
    @ColumnInfo(name = "times")
    var times: Int,                  // 播报次数
    )

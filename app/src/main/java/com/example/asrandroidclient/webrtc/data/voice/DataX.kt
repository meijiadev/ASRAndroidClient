package com.example.asrandroidclient.webrtc.data.voice

data class DataX(
    val defaultFlag: Boolean,          // 是否为默认
    val delFlag: String,               // 删除标识    1：被删除
    val orgId: String,                 // 所属组织
    val text: String,                  // 语音文本
    val times: Int,                    // 播报次数
    val updateTime: String,            //命令下发时间
    val voiceId: String                // 主键
)
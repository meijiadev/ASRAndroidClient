package com.example.asrandroidclient.webrtc.data.keyword

data class KeyData(
    val credibility: Int,       //  可信度
    val delFlag: String,        // 删除标识 1：被删除
    val enabled: Boolean,      //是否启用
    val keyword: String,       // 关键字
    val keywordId: String,     // 关键字Id
    val matchType: String,     // 匹配类型
    val orgId: String,         //所属组织
    val updateTime: String,     // 命令下发时间
    val voiceId: String
)
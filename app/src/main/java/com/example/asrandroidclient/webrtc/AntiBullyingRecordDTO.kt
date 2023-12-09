package com.example.asrandroidclient.webrtc

/**
 * Create by MJ on 2023/12/5.
 * Describe :
 */

data class AntiBullyingRecordDTO(
    val recordId: Long? = null, // 不填
    val snCode: String? = null, // sn码
    val keywordId: Long? = null, // 关键词ID
    val keyword: String? = null, // 关键字
    val credibility: Int? = null, // 关键字得分
    val duration: String? = null, // 识别关键字的时长
    val type: String? = null, // 关键字 1 or  噪音2
    val orgId: Long? = null, // 可不填
    val fileId: Long? = null, // 上传的报警文件ID
)
// waringMessage
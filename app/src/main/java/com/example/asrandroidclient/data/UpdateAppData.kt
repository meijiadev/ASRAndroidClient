package com.example.asrandroidclient.data

/**
 * Create by MJ on 2023/12/29.
 * Describe : 最新的app信息
 */
data class UpdateAppData(
    val fileId: String,
    val fileName: String,
    val fileUrl: String,
    val model: String,
    val releaseTime: String,
    val type: String,
    val versionId: String,
    val versionNo: String,
    val versionCode: Int,
    val fileSize: String,
    val releaseLog: String?
)
package com.example.asrandroidclient.data

data class UploadFileResult(
    val code: Int,
    val `data`: Data,
    val msg: Any,
    val success: Boolean
)

data class Data(
    val bucketName: String,
    val fileId: String,
    val fileName: String,
    val url: String
)
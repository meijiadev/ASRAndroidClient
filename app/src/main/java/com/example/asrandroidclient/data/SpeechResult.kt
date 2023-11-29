package com.example.asrandroidclient.data


data class SpeechResult(
    val rlt: List<Rlt>
)

data class Rlt(
    val decConfidence: Double,
    val iduration: Int,
    val iresIndex: Int,
    val iresid: Int,
    val istart: Int,
    val keyword: String,
    val nDelayFrame: Int,
    val ncm: Int,
    val ncmThresh: Int,
    val ncm_filler: Int,
    val ncm_keyword: Int,
    val nfillerscore: Int,
    val nkeywordscore: Int,
    val sid: String,
    val wakeUpType: Int
)
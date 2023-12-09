package com.example.asrandroidclient.webrtc.data.keyword

import com.google.gson.annotations.SerializedName

/**
 * Create by MJ on 2023/12/4.
 * Describe :
 */

data class Keyword(
    val messageType: String,
    val time: String,
    @SerializedName("data")
    val keyData: KeyData
)
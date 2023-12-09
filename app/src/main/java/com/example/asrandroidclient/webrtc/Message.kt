package com.example.asrandroidclient.webrtc


data class Message(
    val msgType: String,
    val sendFrom: String?,
    val sendTo: String,
    val data: Any?
)

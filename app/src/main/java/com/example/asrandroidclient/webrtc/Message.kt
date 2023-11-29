package com.example.asrandroidclient.webrtc


data class Message(
    val type: String,
    val from: String,
    val to: String,
    val data: Any?
)

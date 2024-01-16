package com.example.asrandroidclient.bluetooth

/**
 * Create by MJ on 2024/1/15.
 * Describe :
 */

data class BleData(
    val status: Int,
    val url: String,
    val snCode: String? = null
)                    // status:1 配网中   status:2 发送sn,接收注册成功的信息    3:配网结束 设备已经连上平台


// app-> 发送hosturl
// B-> 接收url,发送snCode
// A ->接收sn 注册到平台，注册成功告诉B
// B-> 接收注册成功，去连接平台，发送连接成功-app

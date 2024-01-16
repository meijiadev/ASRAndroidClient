package com.example.asrandroidclient.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.asrandroidclient.bluetooth.BleData
import com.example.asrandroidclient.bluetooth.ChatService
import com.example.asrandroidclient.util.SpManager
import com.example.asrandroidclient.webrtc.SocketEventViewModel.Companion.HOST_URL_KEY
import com.google.gson.Gson
import com.kunminx.architecture.ui.callback.UnPeekLiveData

class MainViewModel : ViewModel() {
    // 鉴权结果
    var aiRegisterStatus = UnPeekLiveData<Boolean>()
    var bleConnectedEvent = UnPeekLiveData<Boolean>()
    var networkConfigEvent = UnPeekLiveData<Int>()         // 1:开始配网   2： 配网已完成
    var isNetworkConfig = false                            // 是否进入配网环节
    var configUrl: String? = null                          // 配置的地址
    var chatService: ChatService? = null
    fun initBlue() {
        chatService = ChatService()
        chatService?.start()
    }


    /**
     * 通知设备端已经配网完成
     */
    fun sendNetworkFinish(snCode: String) {
        val hostUrl = SpManager.getString(HOST_URL_KEY)
        val bleData = BleData(3, hostUrl ?: "error", snCode)
        val jsonStr = Gson().toJson(bleData)
        chatService?.write(jsonStr.toByteArray())
    }

    fun sendSnCOde(snCode: String) {
        val hostUrl = SpManager.getString(HOST_URL_KEY)
        val bleData = BleData(2, hostUrl ?: "error", snCode)
        val jsonStr = Gson().toJson(bleData)
        chatService?.write(jsonStr.toByteArray())
    }


    fun sendHasRegister() {
        val data = BleData(4, "null", null)
        val str = Gson().toJson(data)
        chatService?.write(str.toByteArray())
    }
}
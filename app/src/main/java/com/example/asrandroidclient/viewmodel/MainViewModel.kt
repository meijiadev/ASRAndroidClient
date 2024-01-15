package com.example.asrandroidclient.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.asrandroidclient.bluetooth.ChatService
import com.kunminx.architecture.ui.callback.UnPeekLiveData

class MainViewModel : ViewModel() {
    // 鉴权结果
    var aiRegisterStatus = UnPeekLiveData<Boolean>()
    var bleConnectedEvent = UnPeekLiveData<Boolean>()
    var chatService: ChatService? = null
    fun initBlue() {
        chatService = ChatService()
        chatService?.start()
    }
}
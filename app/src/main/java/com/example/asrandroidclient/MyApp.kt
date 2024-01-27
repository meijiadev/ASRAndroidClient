package com.example.asrandroidclient

import android.content.Context
import androidx.multidex.MultiDex
import com.example.asrandroidclient.util.SpManager
import com.example.asrandroidclient.viewmodel.MainViewModel
import com.example.asrandroidclient.webrtc.SocketEventViewModel
import com.example.asrandroidclient.webrtc.WebrtcSocketManager
import com.iflytek.cloud.SpeechUtility
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.sjb.base.base.BaseApplication

class MyApp : BaseApplication() {


    companion object {
        lateinit var CONTEXT: Context
        lateinit var app: MyApp
        lateinit var mainViewModel: MainViewModel
        lateinit var socketEventViewModel: SocketEventViewModel
        lateinit var webrtcSocketManager: WebrtcSocketManager
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        CONTEXT = this
        val formatStrategy = PrettyFormatStrategy
            .newBuilder()
            .showThreadInfo(false)
            .methodCount(2)
            .tag("MJ")
            .build()
        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))
        SpManager.init(this)
        mainViewModel = getApplicationViewModel(MainViewModel::class.java)
        socketEventViewModel = getApplicationViewModel(SocketEventViewModel::class.java)
        webrtcSocketManager = getApplicationViewModel(WebrtcSocketManager::class.java)
        SpeechUtility.createUtility(this, "appid=$APPID")
    }

}
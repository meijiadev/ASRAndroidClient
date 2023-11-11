package com.example.asrandroidclient

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.multidex.MultiDex
import com.example.asrandroidclient.util.SpManager
import com.example.asrandroidclient.viewmodel.MainViewModel
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger

class MyApp : Application(), ViewModelStoreOwner {
    private lateinit var mAppViewModelStore: ViewModelStore

    companion object {
        lateinit var CONTEXT: Context
        lateinit var mainViewModel: MainViewModel
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        CONTEXT = this
        Logger.addLogAdapter(AndroidLogAdapter())
        SpManager.init(this)
        mAppViewModelStore = ViewModelStore()
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
    }

    override fun getViewModelStore(): ViewModelStore {
        return mAppViewModelStore
    }
}
package com.sjb.base.base

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
/**
 *    author : MJ
 *    time   : 2022/01/17
 *    desc   : application 基类
 */
open class BaseApplication :Application() ,ViewModelStoreOwner{
    private lateinit var mAppViewModelStore: ViewModelStore
    /**
     * 作用域为Application的ViewModel，整个应用程序周期（用于在不同的Activity之间传递数据）
     */
    private lateinit var mApplicationProvider: ViewModelProvider
    override fun onCreate() {
        super.onCreate()
        mAppViewModelStore = ViewModelStore()
        mApplicationProvider= ViewModelProvider(this)
    }

    /**
     * 获取作用域在Application的ViewModel对象
     */
    protected fun <T: ViewModel> getApplicationViewModel(modelClass:Class<T>):T{
        return mApplicationProvider.get(modelClass)
    }

    override fun getViewModelStore(): ViewModelStore {
        return mAppViewModelStore
    }
}
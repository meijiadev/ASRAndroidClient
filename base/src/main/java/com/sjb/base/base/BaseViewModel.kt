package com.sjb.base.base

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel

open class BaseViewModel : ViewModel, ToastAction ,LifecycleOwner {
    private val mLifecycle: LifecycleRegistry = LifecycleRegistry(this)

    constructor() {
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onCleared() {
        super.onCleared()
        mLifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycle
    }

}
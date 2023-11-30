package com.sjb.base.ext

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.LoadingPopupView

@SuppressLint("StaticFieldLeak")
private var loadingPopupView: LoadingPopupView? = null

fun AppCompatActivity.showLoading(msg: String = "加载中...") {
    loadingPopupView= XPopup.Builder(this)
        .isViewMode(true)
        .dismissOnTouchOutside(false)
        .asLoading(msg)
        .show() as LoadingPopupView?
}

fun dismissLoading(){
    loadingPopupView?.dismiss()
}
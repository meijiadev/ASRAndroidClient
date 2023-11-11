package com.example.asrandroidclient.util

import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.orhanobut.logger.Logger

fun AppCompatActivity.requestRecordPer(){
    XXPermissions.with(this)
        .permission(Permission.RECORD_AUDIO)
        .request(object :OnPermissionCallback{
            override fun onGranted(permissions: MutableList<String>?, all: Boolean) {
                Logger.i("录音权限获取成功")
            }

            override fun onDenied(permissions: MutableList<String>?, never: Boolean) {
                super.onDenied(permissions, never)
                Logger.i("权限获取失败")
            }
        })
}
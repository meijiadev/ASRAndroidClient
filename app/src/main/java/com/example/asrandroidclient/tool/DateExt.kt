package com.example.asrandroidclient.tool

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * @Desc:
 * @Author MJ
 * @Date 2023/12/07
 */
fun Long.stampToDate(): String {
    val res: String
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val date = Date(this)
    res = simpleDateFormat.format(date)
    return res
}

fun String.dateToStamp(): Long {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    try {
        return df.parse(this)?.time ?: -1
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return -1
}
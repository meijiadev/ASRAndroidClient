package com.example.asrandroidclient.room.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.asrandroidclient.webrtc.data.keyword.Data
import com.example.asrandroidclient.webrtc.data.keyword.Keyword

/**
 * Create by MJ on 2023/12/4.
 * Describe :
 */

@Entity(tableName = "keywords")
data class KeywordBean(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    @ColumnInfo(name = "time")
    var time: String,
    @ColumnInfo(name = "keywordId")
    var keywordId: String,     // 关键字Id
    @ColumnInfo(name = "data")
    var data: Data
)
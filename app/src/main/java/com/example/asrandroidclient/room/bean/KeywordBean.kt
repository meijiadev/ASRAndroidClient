package com.example.asrandroidclient.room.bean

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.asrandroidclient.webrtc.data.keyword.KeyData

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
    @ColumnInfo(name = "keyword")
    var keyword: String,
//    @ColumnInfo(name = "data")
//    var keyData: KeyData
    @ColumnInfo(name = "credibility")
    var credibility: Int,                      //  可信度
    @ColumnInfo(name = "delFlag")
    var delFlag: String,                        // 删除标识 1：被删除
    @ColumnInfo(name = "enabled")
    var enabled: Boolean,                         //是否启用
    @ColumnInfo(name = "matchType")
    var matchType: String,                      // 匹配类型
    @ColumnInfo(name = "orgId")
    var orgId: String,                        //所属组织
)
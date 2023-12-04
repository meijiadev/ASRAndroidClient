package com.example.asrandroidclient.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import com.example.asrandroidclient.room.bean.KeywordBean
import com.example.asrandroidclient.webrtc.data.keyword.Keyword


/**
 * Create by MJ on 2023/12/4.
 * Describe : keyword操作类
 */
@Dao
interface KeywordDao {

    /**
     * 插入一条数据
     */
    @Insert(onConflict = REPLACE)
    fun insertKeyword(keyword: KeywordBean)

    /**
     * 删除一条数据
     */
    @Delete
    fun deleteKeyword(keyword: KeywordBean)

    /**
     * 根据id删除一条数据
     */
    @Delete
    fun deleteKeywordById(id: String)

    /**
     * 更新一条数据
     */
    fun updateKeyword(keyword: KeywordBean)

    @Query("SELECT*FROM keywords WHERE keywordId=:id")
    fun findById(id: String): KeywordBean?

    @Query("SELECT*FROM keywords")
    fun getAllKey(): MutableList<KeywordBean>?


}
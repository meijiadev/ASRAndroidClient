package com.example.asrandroidclient.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.asrandroidclient.room.bean.VoiceBean

/**
 * Create by MJ on 2023/12/4.
 * Describe :
 */

@Dao
interface VoiceDao {

    @Insert
    fun insert(voiceBean: VoiceBean)


    @Delete
    fun deleteVoice(voiceBean: VoiceBean)

//    @Delete
//    fun deleteVoiceById(voiceId: String)

    @Update
    fun updateVoice(voiceBean: VoiceBean)

    @Query("SELECT*FROM voices WHERE voiceId=:id")
    fun findById(id: String): VoiceBean?


    @Query("SELECT*FROM voices")
    fun getAllVoice(): MutableList<VoiceBean>?
}
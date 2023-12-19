package com.example.asrandroidclient.room

import android.content.Context
import android.speech.tts.Voice
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.asrandroidclient.MyApp
import com.example.asrandroidclient.room.bean.KeywordBean
import com.example.asrandroidclient.room.bean.VoiceBean
import com.example.asrandroidclient.room.dao.KeywordDao
import com.example.asrandroidclient.room.dao.VoiceDao
import com.orhanobut.logger.Logger

/**
 * Create by MJ on 2023/12/4.
 * Describe :
 */
@Database(entities = [KeywordBean::class, VoiceBean::class], version = 2, exportSchema = true)
abstract class AppDataBase : RoomDatabase() {

    abstract fun keyWordDao(): KeywordDao

    abstract fun voiceDao(): VoiceDao

    /**
     * 通过keywordId删除数据
     */
    @Transaction
    fun deleteKeywordById(id: String) {
        val keyword = keyWordDao().findById(id)
        if (keyword != null) {
            keyWordDao().deleteKeyword(keyword)
        }
    }

    /**
     * 添加数据时防止关键字重复
     */
    @Transaction
    fun insert(keywordBean: KeywordBean): Boolean {
        val keyword = keyWordDao().findByKeyword(keywordBean.keyword)
        return if (keyword != null) {
            Logger.d("重复keyword,${keyword.keyword}")
            false
        } else {
            keyWordDao().insertKeyword(keywordBean)
            true
        }
    }

    /**
     * 添加数据时防止关键字重复
     */
    @Transaction
    fun insert(voiceBean: VoiceBean): Boolean {
        val keyword = voiceDao().findById(voiceBean.voiceId)
        return if (keyword != null) {
            Logger.d("重复voiceId,${voiceBean.text}")
            false
        } else {
            voiceDao().insert(voiceBean)
            true
        }
    }


    /**
     * 通过voiceId删除数据
     */
    @Transaction
    fun deleteVoiceById(id: String) {
        val voice = voiceDao().findById(id)
        if (voice != null) {
            voiceDao().deleteVoice(voice)
        }
    }

    companion object {

        @Volatile
        private var instance: AppDataBase? = null

        fun getInstance(): AppDataBase {
            return instance ?: synchronized(this) {
                instance ?: buildDataBase(MyApp.CONTEXT).also {
                    instance = it
                }
            }
        }

        private fun buildDataBase(context: Context): AppDataBase {
            return Room.databaseBuilder(context, AppDataBase::class.java, "arpha-database")
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                    }
                }).build()
        }
    }
}
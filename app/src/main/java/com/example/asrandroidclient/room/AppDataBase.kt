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

/**
 * Create by MJ on 2023/12/4.
 * Describe :
 */
@Database(entities = [KeywordBean::class, VoiceBean::class], version = 1, exportSchema = true)
abstract class AppDataBase : RoomDatabase() {

    abstract fun keyWordDao(): KeywordDao

    abstract fun voiceDao(): VoiceDao

    @Transaction
    fun deleteKeywordById(id: String) {
        val keyword = keyWordDao().findById(id)
        if (keyword != null) {
            keyWordDao().deleteKeyword(keyword)
        }
    }

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
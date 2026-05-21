package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.PrressoDao
import com.example.data.model.CrmStateEntity
import com.example.data.model.ChatMessageEntity
import com.example.data.model.ApiLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [CrmStateEntity::class, ChatMessageEntity::class, ApiLogEntity::class], version = 2, exportSchema = false)
abstract class PrressoDatabase : RoomDatabase() {
    abstract fun prressoDao(): PrressoDao

    companion object {
        @Volatile
        private var INSTANCE: PrressoDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): PrressoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrressoDatabase::class.java,
                    "prresso_database"
                )
                .addCallback(PrressoDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class PrressoDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.prressoDao()
                    // Initialize CRM Database
                    if (dao.getCrmState() == null) {
                        dao.insertCrmState(CrmStateEntity())
                        dao.insertLog(ApiLogEntity(
                            type = "system",
                            message = "Authoritative System of Record (SoR) initialised. Live CRM loaded."
                        ))
                    }
                }
            }
        }
    }
}

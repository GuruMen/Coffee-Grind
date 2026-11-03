package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CoffeeBeanDao {
    @Query("SELECT * FROM coffee_beans ORDER BY createdAt DESC")
    fun getAllBeans(): Flow<List<CoffeeBeanEntity>>

    @Query("SELECT * FROM coffee_beans WHERE id = :id")
    suspend fun getBeanById(id: Long): CoffeeBeanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBean(bean: CoffeeBeanEntity): Long

    @Update
    suspend fun updateBean(bean: CoffeeBeanEntity)

    @Query("DELETE FROM coffee_beans WHERE id = :id")
    suspend fun deleteBean(id: Long)

    @Query("UPDATE coffee_beans SET isFavorite = :isFav WHERE id = :id")
    suspend fun setFavorite(id: Long, isFav: Boolean)

    @Query("UPDATE coffee_beans SET inStock = :inStock WHERE id = :id")
    suspend fun setInStock(id: Long, inStock: Boolean)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY id DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity): Long

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntry(id: Long)
}

@Dao
interface GarageDao {
    @Query("SELECT * FROM garage_settings")
    fun getAllSettings(): Flow<List<GarageSettingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSetting(setting: GarageSettingEntity)

    @Query("SELECT * FROM garage_settings WHERE grinderKey = :key LIMIT 1")
    suspend fun getSetting(key: String): GarageSettingEntity?
}

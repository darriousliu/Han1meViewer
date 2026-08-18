package io.github.darriousliu.han1meviewer.logic.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import io.github.darriousliu.han1meviewer.logic.entity.SideDishEntity

@Dao
interface SideDishDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sideDishes: List<SideDishEntity>)

    @Query("SELECT * FROM sidedishes WHERE videoCode IN (:videoCodes)")
    suspend fun findByVideoCodes(videoCodes: List<String>): List<SideDishEntity>

    @Query("SELECT * FROM sidedishes")
    suspend fun getAll(): List<SideDishEntity>

    @Query("SELECT videoCode FROM sidedishes WHERE videoCode IN (:videoCodes) AND coverUrl != ''")
    suspend fun findExistingCoverVideoCodes(videoCodes: List<String>): List<String>

    @Query("DELETE FROM sidedishes")
    suspend fun deleteAll()
}

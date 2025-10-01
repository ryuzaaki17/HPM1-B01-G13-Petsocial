package com.example.petsocial.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PetProfileDao {
    @Query("SELECT * FROM pet_profile WHERE id = 1")
    suspend fun getOnce(): PetProfileEntity?

    @Query("SELECT * FROM pet_profile WHERE id = 1")
    fun watch(): Flow<PetProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: PetProfileEntity)
}


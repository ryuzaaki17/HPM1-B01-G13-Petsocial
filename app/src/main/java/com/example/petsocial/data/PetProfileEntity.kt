package com.example.petsocial.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pet_profile")
data class PetProfileEntity(
    @PrimaryKey val id: Int = 1,
    val photoUri: String?,   // URI de la foto (Storage Access Framework)
    val name: String,
    val breed: String?,
    val age: Int?,
    val interests: String?
)


package io.github.darriousliu.han1meviewer.logic.entity

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "sidedishes")
data class SideDishEntity(
    @PrimaryKey val videoCode: String,
    val title: String,
    val coverUrl: String,
)

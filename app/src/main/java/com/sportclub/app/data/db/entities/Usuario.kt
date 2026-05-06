package com.sportclub.app.data.db.entities
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuario",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["personaId"])
    ],
    foreignKeys = [ForeignKey(
        entity = Persona::class,
        parentColumns = ["id"],
        childColumns = ["personaId"],
        onDelete = ForeignKey.SET_NULL
    )]
)
data class Usuario(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val password: String,
    val rol: String,
    val activo: Boolean = true,
    val personaId: Int? = null,
    val fechaCreacion: Long = System.currentTimeMillis()
)
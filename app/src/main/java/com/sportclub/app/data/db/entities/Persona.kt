package com.sportclub.app.data.db.entities
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "persona",
    indices = [Index(value = ["dni"], unique = true)]
)
data class Persona(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val apellido: String,
    val dni: String,
    val telefono: String? = null,
    val email: String? = null,
    val tipoPersona: String,
    val fechaRegistro: Long = System.currentTimeMillis()
)
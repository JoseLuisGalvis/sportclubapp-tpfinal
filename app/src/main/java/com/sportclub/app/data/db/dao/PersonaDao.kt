package com.sportclub.app.data.db.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sportclub.app.data.db.entities.Persona

@Dao
interface PersonaDao {

    @Insert
    suspend fun insert(persona: Persona): Long

    @Query("SELECT COUNT(*) FROM persona WHERE dni = :dni")
    suspend fun existsByDni(dni: String): Int

    @Query("SELECT * FROM persona WHERE id = :id")
    suspend fun getById(id: Int): Persona?

    @Query("SELECT * FROM persona")
    suspend fun getAll(): List<Persona>
}
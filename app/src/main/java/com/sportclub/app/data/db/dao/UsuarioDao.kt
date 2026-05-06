package com.sportclub.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sportclub.app.data.db.entities.Usuario

@Dao
interface UsuarioDao {

    @Insert
    suspend fun insert(usuario: Usuario): Long

    @Update
    suspend fun update(usuario: Usuario)

    @Query("DELETE FROM usuario WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM usuario WHERE username = :username")
    suspend fun existsByUsername(username: String): Int

    @Query("SELECT COUNT(*) FROM usuario WHERE username = :username AND id != :excludeId")
    suspend fun existsByUsernameExcluding(username: String, excludeId: Int): Int

    @Query("SELECT * FROM usuario WHERE username = :username AND activo = 1")
    suspend fun getByUsername(username: String): Usuario?

    @Query("SELECT * FROM usuario WHERE id = :id")
    suspend fun getById(id: Int): Usuario?

    @Query("SELECT rol FROM usuario WHERE username = :username AND activo = 1")
    suspend fun getRolByUsername(username: String): String?

    @Query("SELECT * FROM usuario WHERE rol = 'Administrador' ORDER BY fechaCreacion ASC")
    suspend fun getAdministradores(): List<Usuario>

    @Query("SELECT COUNT(*) FROM usuario WHERE rol = 'Administrador'")
    suspend fun countAdministradores(): Int

    @Query("SELECT * FROM usuario")
    suspend fun getAll(): List<Usuario>
}
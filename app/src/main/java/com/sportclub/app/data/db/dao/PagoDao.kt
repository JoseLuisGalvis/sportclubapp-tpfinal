package com.sportclub.app.data.db.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sportclub.app.data.db.entities.Pago

@Dao
interface PagoDao {

    @Insert
    suspend fun insert(pago: Pago): Long

    @Query("SELECT * FROM pago WHERE socioId = :socioId ORDER BY fechaPago DESC")
    suspend fun getBySocio(socioId: Int): List<Pago>

    @Query("SELECT * FROM pago ORDER BY fechaPago DESC")
    suspend fun getAll(): List<Pago>
}
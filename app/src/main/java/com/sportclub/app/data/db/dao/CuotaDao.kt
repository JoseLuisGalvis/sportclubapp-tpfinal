package com.sportclub.app.data.db.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sportclub.app.data.db.entities.Cuota

@Dao
interface CuotaDao {

    @Insert
    suspend fun insert(cuota: Cuota): Long

    @Update
    suspend fun update(cuota: Cuota)

    @Query("SELECT * FROM cuota WHERE id = :id")
    suspend fun getById(id: Int): Cuota?

    @Query("SELECT * FROM cuota WHERE socioId = :socioId ORDER BY fechaVencimiento ASC")
    suspend fun getBySocio(socioId: Int): List<Cuota>

    @Query("SELECT * FROM cuota WHERE estado IN ('Pendiente','Vencida') ORDER BY fechaVencimiento ASC")
    suspend fun getImpagas(): List<Cuota>

    @Query("SELECT * FROM cuota WHERE estado = 'Pendiente' ORDER BY fechaVencimiento ASC")
    suspend fun getPendientes(): List<Cuota>

    @Query("SELECT * FROM cuota WHERE estado IN ('Pendiente','Vencida') AND fechaVencimiento < :hoy ORDER BY fechaVencimiento ASC")
    suspend fun getVencidas(hoy: Long): List<Cuota>

    @Query("SELECT * FROM cuota WHERE estado = 'Pendiente' AND fechaVencimiento >= :hoyInicio AND fechaVencimiento <= :hoyFin")
    suspend fun getQueVencenHoy(hoyInicio: Long, hoyFin: Long): List<Cuota>

    @Query("UPDATE cuota SET estado = 'Vencida' WHERE estado = 'Pendiente' AND fechaVencimiento < :ahora")
    suspend fun actualizarVencidas(ahora: Long): Int

    @Query("UPDATE cuota SET estado = 'Pagada', metodoPago = :metodo WHERE id = :id")
    suspend fun marcarPagada(id: Int, metodo: String)

    @Query("SELECT COUNT(*) FROM cuota WHERE socioId = :socioId AND estado IN ('Pendiente','Vencida') AND fechaVencimiento < :ahora")
    suspend fun countVencidasPorSocio(socioId: Int, ahora: Long): Int

    @Query("SELECT * FROM cuota ORDER BY fechaVencimiento ASC")
    suspend fun getAll(): List<Cuota>
}
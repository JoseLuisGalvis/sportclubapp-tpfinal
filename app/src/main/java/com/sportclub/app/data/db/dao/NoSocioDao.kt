package com.sportclub.app.data.db.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sportclub.app.data.db.entities.NoSocio

@Dao
interface NoSocioDao {

    @Insert
    suspend fun insert(noSocio: NoSocio): Long

    @Update
    suspend fun update(noSocio: NoSocio)

    @Query("SELECT * FROM nosocio WHERE personaId = :personaId")
    suspend fun getByPersonaId(personaId: Int): NoSocio?

    @Query("SELECT * FROM nosocio WHERE id = :id")
    suspend fun getById(id: Int): NoSocio?

    @Query("SELECT * FROM nosocio ORDER BY id ASC")
    suspend fun getAll(): List<NoSocio>

    @Query("SELECT COUNT(*) FROM nosocio")
    suspend fun count(): Int

    @Query("UPDATE nosocio SET estadoPago = 'Completado', stripePaymentIntentId = :paymentIntentId, fechaPago = :ahora, habilitado = 1 WHERE id = :id")
    suspend fun marcarPagoCompletado(id: Int, paymentIntentId: String, ahora: Long)

    @Query("UPDATE nosocio SET stripeSessionId = :sessionId WHERE id = :id")
    suspend fun updateStripeSession(id: Int, sessionId: String)
}
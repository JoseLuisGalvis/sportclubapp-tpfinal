package com.sportclub.app.data.db.dao
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.sportclub.app.data.db.entities.Socio

@Dao
interface SocioDao {

    @Insert
    suspend fun insert(socio: Socio): Long

    @Update
    suspend fun update(socio: Socio)

    @Query("SELECT * FROM socio WHERE personaId = :personaId")
    suspend fun getByPersonaId(personaId: Int): Socio?

    @Query("SELECT * FROM socio WHERE nroSocio = :nroSocio")
    suspend fun getByNroSocio(nroSocio: Int): Socio?

    @Query("SELECT * FROM socio ORDER BY nroSocio ASC")
    suspend fun getAll(): List<Socio>

    @Query("SELECT * FROM socio WHERE estadoPago = 'Completado' ORDER BY fechaPago ASC")
    suspend fun getSociosConPagoCompletado(): List<Socio>

    @Query("SELECT COUNT(*) FROM socio")
    suspend fun count(): Int

    @Query("UPDATE socio SET estadoPago = 'Completado', stripePaymentIntentId = :paymentIntentId, fechaPago = :ahora, habilitado = 1 WHERE nroSocio = :nroSocio")
    suspend fun marcarPagoCompletado(nroSocio: Int, paymentIntentId: String, ahora: Long)

    @Query("UPDATE socio SET estadoPago = 'Completado', fechaPago = :ahora, habilitado = 1 WHERE nroSocio = :nroSocio")
    suspend fun marcarPagoCompletadoLocal(nroSocio: Int, ahora: Long)

    @Query("UPDATE socio SET carnetEntregado = 1, carnet = :codigo, fechaEntregaCarnet = :ahora WHERE nroSocio = :nroSocio")
    suspend fun marcarCarnetEntregado(nroSocio: Int, codigo: String, ahora: Long)

    @Query("UPDATE socio SET stripeSessionId = :sessionId WHERE nroSocio = :nroSocio")
    suspend fun updateStripeSession(nroSocio: Int, sessionId: String)
}
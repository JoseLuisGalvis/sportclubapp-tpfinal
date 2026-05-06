package com.sportclub.app.data.repository

import android.util.Log
import com.sportclub.app.data.db.dao.NoSocioDao
import com.sportclub.app.data.db.dao.SocioDao
import com.sportclub.app.data.remote.api.SportClubApi
import com.sportclub.app.data.remote.dto.CreateSessionRequest
import com.sportclub.app.data.remote.dto.SyncSocioRequest
import com.sportclub.app.domain.model.PaymentSession
import com.sportclub.app.domain.repository.IPaymentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PaymentRepositoryImpl(
    private val api:        SportClubApi,
    private val socioDao:   SocioDao,
    private val noSocioDao: NoSocioDao
) : IPaymentRepository {

    companion object { private const val TAG = "PaymentRepository" }

    override suspend fun crearSesionSocio(
        nroSocio: Int, nombre: String, email: String?
    ): Result<PaymentSession> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.createSession(
                CreateSessionRequest(tipo = "socio", entidadId = nroSocio,
                    nombre = nombre, email = email)
            )
            val body = response.body()
                ?: throw Exception("Error HTTP ${response.code()}: ${response.errorBody()?.string()}")

            socioDao.updateStripeSession(nroSocio, body.sessionId)
            Log.d(TAG, "Sesión creada — socio #$nroSocio")
            PaymentSession(sessionId = body.sessionId, paymentUrl = body.url)
        }
    }

    override suspend fun crearSesionNoSocio(
        idNoSocio: Int, nombre: String, email: String?
    ): Result<PaymentSession> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.createSession(
                CreateSessionRequest(tipo = "nosocio", entidadId = idNoSocio,
                    nombre = nombre, email = email)
            )
            val body = response.body()
                ?: throw Exception("Error HTTP ${response.code()}: ${response.errorBody()?.string()}")

            noSocioDao.updateStripeSession(idNoSocio, body.sessionId)
            Log.d(TAG, "Sesión creada — nosocio #$idNoSocio")
            PaymentSession(sessionId = body.sessionId, paymentUrl = body.url)
        }
    }

    override suspend fun verificarPago(sessionId: String): Result<String?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.verifySession(sessionId)
                val body = response.body()
                    ?: throw Exception("Error HTTP ${response.code()}")
                if (body.pagado) body.paymentIntentId else null
            }
        }

    override suspend fun sincronizarSocio(
        nroSocio: Int, nombre: String, apellido: String,
        dni: String, email: String?, telefono: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            api.syncSocio(SyncSocioRequest(nroSocio, nombre, apellido, dni, email, telefono))
            Log.d(TAG, "Socio #$nroSocio sincronizado")
            Unit
        }
    }

    override suspend fun marcarPagoSocioCompletado(nroSocio: Int, paymentIntentId: String) {
        socioDao.marcarPagoCompletado(nroSocio, paymentIntentId, System.currentTimeMillis())
        Log.i(TAG, "Pago socio #$nroSocio completado en Room")
    }

    override suspend fun marcarPagoNoSocioCompletado(idNoSocio: Int, paymentIntentId: String) {
        noSocioDao.marcarPagoCompletado(idNoSocio, paymentIntentId, System.currentTimeMillis())
        Log.i(TAG, "Pago nosocio #$idNoSocio completado en Room")
    }

    override suspend fun marcarPagoLocal(nroSocio: Int) {
        socioDao.marcarPagoCompletadoLocal(nroSocio, System.currentTimeMillis())
        Log.w(TAG, "Pago socio #$nroSocio marcado localmente")
    }
}
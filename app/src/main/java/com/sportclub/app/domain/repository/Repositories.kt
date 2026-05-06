package com.sportclub.app.domain.repository

import com.sportclub.app.domain.model.PaymentSession
import com.sportclub.app.domain.model.RegistroSocioResult

interface IPersonaRepository {
    suspend fun existsByDni(dni: String): Boolean
    suspend fun registrarSocio(
        nombre:     String,
        apellido:   String,
        dni:        String,
        telefono:   String?,
        email:      String?,
        fechaAlta:  Long,
        habilitado: Boolean,
        aptoFisico: Boolean,
        fotoCarnet: ByteArray?
    ): RegistroSocioResult
    suspend fun registrarNoSocio(
        nombre:      String,
        apellido:    String,
        dni:         String,
        telefono:    String?,
        email:       String?,
        habilitado:  Boolean,
        fechaVisita: Long?,
        fotoVisita:  ByteArray?
    ): Int
    suspend fun getNroSocioByPersonaId(personaId: Int): Int
    suspend fun getNoSocioIdByPersonaId(personaId: Int): Int
}

interface IAuthRepository {
    suspend fun usernameExists(username: String): Boolean
    suspend fun validateUser(username: String, password: String): Boolean
    suspend fun getUserRol(username: String): String?
    suspend fun insertUsuarioConPersona(
        username:      String,
        passwordPlain: String,
        rol:           String,
        personaId:     Int
    ): Long
    suspend fun insertUsuario(username: String, passwordPlain: String, rol: String): Long
}

interface IPaymentRepository {
    suspend fun crearSesionSocio(nroSocio: Int, nombre: String, email: String?): Result<PaymentSession>
    suspend fun crearSesionNoSocio(idNoSocio: Int, nombre: String, email: String?): Result<PaymentSession>
    suspend fun verificarPago(sessionId: String): Result<String?>
    suspend fun sincronizarSocio(
        nroSocio: Int, nombre: String, apellido: String,
        dni: String, email: String?, telefono: String?
    ): Result<Unit>
    suspend fun marcarPagoSocioCompletado(nroSocio: Int, paymentIntentId: String)
    suspend fun marcarPagoNoSocioCompletado(idNoSocio: Int, paymentIntentId: String)
    suspend fun marcarPagoLocal(nroSocio: Int)
}

interface ISessionRepository {
    suspend fun fetchJwtToken(): Result<String>
    fun getStoredToken(): String?
    fun storeToken(token: String)
    fun clearToken()
}
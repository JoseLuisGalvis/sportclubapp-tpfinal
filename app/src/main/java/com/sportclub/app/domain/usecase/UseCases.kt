package com.sportclub.app.domain.usecase

import com.sportclub.app.domain.model.DomainError
import com.sportclub.app.domain.model.PaymentSession
import com.sportclub.app.domain.model.PersonaValidator
import com.sportclub.app.domain.repository.IAuthRepository
import com.sportclub.app.domain.repository.IPaymentRepository
import com.sportclub.app.domain.repository.IPersonaRepository
import com.sportclub.app.domain.repository.ISessionRepository

class RegistrarSocioUseCase(
    private val personaRepo: IPersonaRepository,
    private val authRepo:    IAuthRepository,
    private val paymentRepo: IPaymentRepository
) {
    sealed class Output {
        data class ReadyForPayment(val session: PaymentSession, val nroSocio: Int) : Output()
        data class RegistradoOffline(val nombre: String, val dni: String, val carnet: String) : Output()
        data class Failure(val error: DomainError) : Output()
    }

    suspend operator fun invoke(
        nombre:          String,
        apellido:        String,
        dni:             String,
        telefono:        String?,
        email:           String?,
        username:        String,
        password:        String,
        confirmPassword: String,
        habilitado:      Boolean,
        aptoFisico:      Boolean,
        fechaAlta:       Long,
        fotoCarnet:      ByteArray?
    ): Output {
        PersonaValidator.validarSocio(nombre, apellido, dni, username, password, confirmPassword)
            ?.let { return Output.Failure(it) }

        if (personaRepo.existsByDni(dni))       return Output.Failure(DomainError.DniDuplicado())
        if (authRepo.usernameExists(username))  return Output.Failure(DomainError.UsernameDuplicado())

        return runCatching {
            val resultado = personaRepo.registrarSocio(
                nombre, apellido, dni, telefono, email,
                fechaAlta, habilitado, aptoFisico, fotoCarnet
            )
            authRepo.insertUsuarioConPersona(username, password, "Socio", resultado.personaId)

            runCatching {
                paymentRepo.sincronizarSocio(
                    resultado.nroSocio, nombre, apellido, dni, email, telefono
                )
            }

            val stripeResult = paymentRepo.crearSesionSocio(
                resultado.nroSocio, "$nombre $apellido", email
            )

            if (stripeResult.isSuccess) {
                Output.ReadyForPayment(stripeResult.getOrThrow(), resultado.nroSocio)
            } else {
                paymentRepo.marcarPagoLocal(resultado.nroSocio)
                Output.RegistradoOffline("$nombre $apellido", dni, resultado.carnet)
            }
        }.getOrElse { e ->
            Output.Failure(DomainError.Unknown(e.message ?: "Error desconocido"))
        }
    }
}

class RegistrarNoSocioUseCase(
    private val personaRepo: IPersonaRepository,
    private val authRepo:    IAuthRepository,
    private val paymentRepo: IPaymentRepository
) {
    sealed class Output {
        data class ReadyForPayment(val session: PaymentSession, val idNoSocio: Int) : Output()
        data class RegistradoSinPago(val nombre: String, val dni: String, val fechaVisita: Long) : Output()
        data class Failure(val error: DomainError) : Output()
    }

    suspend operator fun invoke(
        nombre:          String,
        apellido:        String,
        dni:             String,
        telefono:        String?,
        email:           String?,
        username:        String,
        password:        String,
        confirmPassword: String,
        habilitado:      Boolean,
        fechaVisita:     Long
    ): Output {
        PersonaValidator.validarNoSocio(nombre, apellido, dni, username, password, confirmPassword)
            ?.let { return Output.Failure(it) }
        if (personaRepo.existsByDni(dni))      return Output.Failure(DomainError.DniDuplicado())
        if (authRepo.usernameExists(username)) return Output.Failure(DomainError.UsernameDuplicado())

        return runCatching {
            val personaId = personaRepo.registrarNoSocio(
                nombre, apellido, dni, telefono, email, habilitado, fechaVisita, null
            )
            authRepo.insertUsuarioConPersona(username, password, "NoSocio", personaId)

            val idNoSocio    = personaRepo.getNoSocioIdByPersonaId(personaId)
            val stripeResult = paymentRepo.crearSesionNoSocio(idNoSocio, "$nombre $apellido", email)

            if (stripeResult.isSuccess) {
                Output.ReadyForPayment(stripeResult.getOrThrow(), idNoSocio)
            } else {
                Output.RegistradoSinPago("$nombre $apellido", dni, fechaVisita)
            }
        }.getOrElse { e ->
            Output.Failure(DomainError.Unknown(e.message ?: "Error desconocido"))
        }
    }
}

class VerificarPagoUseCase(
    private val paymentRepo: IPaymentRepository
) {
    sealed class Output {
        data class Confirmado(val paymentIntentId: String) : Output()
        object Pendiente : Output()
        data class Fallido(val error: DomainError) : Output()
    }

    suspend operator fun invoke(
        sessionId:  String,
        entityType: String,
        entityId:   Int
    ): Output {
        val resultado = paymentRepo.verificarPago(sessionId)

        if (resultado.isFailure) {
            return Output.Fallido(DomainError.NetworkError(
                resultado.exceptionOrNull()?.message ?: "Error de red"
            ))
        }

        val paymentIntentId = resultado.getOrNull() ?: return Output.Pendiente

        when (entityType) {
            "socio"   -> paymentRepo.marcarPagoSocioCompletado(entityId, paymentIntentId)
            "nosocio" -> paymentRepo.marcarPagoNoSocioCompletado(entityId, paymentIntentId)
        }

        return Output.Confirmado(paymentIntentId)
    }
}

class LoginUseCase(
    private val authRepo:    IAuthRepository,
    private val sessionRepo: ISessionRepository
) {
    sealed class Output {
        data class Success(val rol: String) : Output()
        data class Failure(val error: DomainError) : Output()
    }

    suspend operator fun invoke(username: String, password: String): Output {
        PersonaValidator.validarLogin(username, password)
            ?.let { return Output.Failure(it) }

        val valid = authRepo.validateUser(username, password)
        if (!valid) return Output.Failure(DomainError.ValidationError("Credenciales inválidas"))

        val rol = authRepo.getUserRol(username)
        if (rol != "Administrador") {
            return Output.Failure(DomainError.ValidationError("El usuario no tiene rol de Administrador"))
        }

        return Output.Success(rol)
    }
}
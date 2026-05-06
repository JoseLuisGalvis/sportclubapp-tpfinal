package com.sportclub.app.domain.model

sealed class DomainResult<out T> {
    data class Success<T>(val data: T) : DomainResult<T>()
    data class Failure(val error: DomainError) : DomainResult<Nothing>()

    val isSuccess get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.data
    fun errorOrNull(): DomainError? = (this as? Failure)?.error
}

sealed class DomainError(open val message: String) {
    data class DniDuplicado(override val message: String = "Ya existe una persona con ese DNI") : DomainError(message)
    data class UsernameDuplicado(override val message: String = "Ese usuario ya está en uso") : DomainError(message)
    data class ValidationError(override val message: String) : DomainError(message)
    data class NetworkError(override val message: String) : DomainError(message)
    data class AuthError(override val message: String = "Sesión expirada. Reconectando…") : DomainError(message)
    data class Unknown(override val message: String) : DomainError(message)
}

object PersonaValidator {

    fun validarSocio(
        nombre:          String,
        apellido:        String,
        dni:             String,
        username:        String,
        password:        String,
        confirmPassword: String
    ): DomainError? = validarBase(nombre, apellido, dni)
        ?: validarCredenciales(username, password, confirmPassword)

    fun validarNoSocio(
        nombre:          String,
        apellido:        String,
        dni:             String,
        username:        String,
        password:        String,
        confirmPassword: String
    ): DomainError? = validarBase(nombre, apellido, dni)
        ?: validarCredenciales(username, password, confirmPassword)

    fun validarLogin(username: String, password: String): DomainError? = when {
        username.isBlank() -> DomainError.ValidationError("El usuario es obligatorio")
        password.isBlank() -> DomainError.ValidationError("La contraseña es obligatoria")
        else               -> null
    }

    private fun validarBase(nombre: String, apellido: String, dni: String): DomainError? = when {
        nombre.isBlank()          -> DomainError.ValidationError("El nombre es obligatorio")
        apellido.isBlank()        -> DomainError.ValidationError("El apellido es obligatorio")
        dni.isBlank()             -> DomainError.ValidationError("El DNI es obligatorio")
        dni.length !in 7..8       -> DomainError.ValidationError("DNI: 7 u 8 dígitos")
        !dni.all { it.isDigit() } -> DomainError.ValidationError("DNI: solo dígitos")
        else                      -> null
    }

    private fun validarCredenciales(
        username: String, password: String, confirm: String
    ): DomainError? = when {
        username.isBlank()  -> DomainError.ValidationError("El usuario es obligatorio")
        password.isBlank()  -> DomainError.ValidationError("La contraseña es obligatoria")
        password != confirm -> DomainError.ValidationError("Las contraseñas no coinciden")
        password.length < 6 -> DomainError.ValidationError("Mínimo 6 caracteres")
        else                -> null
    }
}
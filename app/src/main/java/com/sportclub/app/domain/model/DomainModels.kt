package com.sportclub.app.domain.model

data class SocioDomain(
    val nroSocio:        Int,
    val nombre:          String,
    val apellido:        String,
    val dni:             String,
    val email:           String?,
    val telefono:        String?,
    val estadoPago:      EstadoPago,
    val habilitado:      Boolean,
    val carnet:          String?,
    val carnetEntregado: Boolean
) {
    val nombreCompleto: String get() = "$nombre $apellido"
}

data class NoSocioDomain(
    val id:          Int,
    val nombre:      String,
    val apellido:    String,
    val dni:         String,
    val email:       String?,
    val telefono:    String?,
    val habilitado:  Boolean,
    val fechaVisita: Long?
) {
    val nombreCompleto: String get() = "$nombre $apellido"
}

data class RegistroSocioResult(
    val nroSocio:  Int,
    val carnet:    String,
    val personaId: Int
)

data class PaymentSession(
    val sessionId:  String,
    val paymentUrl: String
)

enum class EstadoPago { PENDIENTE, COMPLETADO, FALLIDO }
package com.sportclub.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TokenRequest(
    @SerializedName("apiKey") val apiKey: String
)

data class TokenResponse(
    @SerializedName("ok")        val ok:        Boolean,
    @SerializedName("token")     val token:     String?,
    @SerializedName("expiresIn") val expiresIn: String?
)

data class CreateSessionRequest(
    @SerializedName("tipo")      val tipo:      String,
    @SerializedName("entidadId") val entidadId: Int,
    @SerializedName("nombre")    val nombre:    String,
    @SerializedName("email")     val email:     String?
)

data class CreateSessionResponse(
    @SerializedName("ok")        val ok:        Boolean,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("url")       val url:       String
)

data class VerifySessionResponse(
    @SerializedName("ok")              val ok:              Boolean,
    @SerializedName("pagado")          val pagado:          Boolean,
    @SerializedName("paymentIntentId") val paymentIntentId: String?,
    @SerializedName("estado")          val estado:          String
)

data class SyncSocioRequest(
    @SerializedName("nroSocio")  val nroSocio:  Int,
    @SerializedName("nombre")    val nombre:    String,
    @SerializedName("apellido")  val apellido:  String,
    @SerializedName("dni")       val dni:       String,
    @SerializedName("email")     val email:     String?,
    @SerializedName("telefono")  val telefono:  String?
)
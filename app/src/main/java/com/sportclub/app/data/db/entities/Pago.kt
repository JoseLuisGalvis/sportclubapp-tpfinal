package com.sportclub.app.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pago",
    indices = [Index("socioId"), Index("cuotaId")],
    foreignKeys = [
        ForeignKey(
            entity = Socio::class,
            parentColumns = ["nroSocio"],
            childColumns = ["socioId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Cuota::class,
            parentColumns = ["id"],
            childColumns = ["cuotaId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Pago(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val fechaPago: Long = System.currentTimeMillis(),
    val monto: Double,
    val metodoPago: String,
    val cuotaId: Int? = null,
    val socioId: Int,
    val numeroComprobante: String? = null,
    val observaciones: String? = null,
    val stripePaymentIntentId: String? = null
)
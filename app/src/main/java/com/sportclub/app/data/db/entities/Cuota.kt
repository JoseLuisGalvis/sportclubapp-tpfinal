package com.sportclub.app.data.db.entities
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cuota",
    indices = [
        Index("socioId"),
        Index("estado"),
        Index("fechaVencimiento")
    ],
    foreignKeys = [ForeignKey(
        entity = Socio::class,
        parentColumns = ["nroSocio"],
        childColumns = ["socioId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Cuota(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val socioId: Int,
    val monto: Double,
    val fechaVencimiento: Long,
    val estado: String = "Pendiente",
    val tipoCuota: String = "Mensual",
    val metodoPago: String? = null,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val stripeSessionId: String? = null,
    val stripePaymentIntentId: String? = null
)
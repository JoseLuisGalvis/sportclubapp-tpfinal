package com.sportclub.app.data.db.entities
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "socio",
    indices = [Index(value = ["personaId"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = Persona::class,
        parentColumns = ["id"],
        childColumns = ["personaId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Socio(
    @PrimaryKey(autoGenerate = true)
    val nroSocio: Int = 0,
    val personaId: Int,
    val fechaAlta: Long,
    val habilitado: Boolean = true,
    val aptoFisico: Boolean = false,
    val carnet: String? = null,
    val fotoCarnet: ByteArray? = null,
    val fotoTipo: String? = null,
    val stripeSessionId: String? = null,
    val stripePaymentIntentId: String? = null,
    val estadoPago: String = "Pendiente",
    val fechaPago: Long? = null,
    val montoPago: Double = 10000.0,
    val carnetEntregado: Boolean = false,
    val fechaEntregaCarnet: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Socio) return false
        return nroSocio == other.nroSocio
    }
    override fun hashCode() = nroSocio
}
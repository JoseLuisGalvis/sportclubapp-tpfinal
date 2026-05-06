package com.sportclub.app.data.db.entities
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "nosocio",
    indices = [Index(value = ["personaId"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = Persona::class,
        parentColumns = ["id"],
        childColumns = ["personaId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class NoSocio(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val personaId: Int,
    val habilitado: Boolean = true,
    val fechaVisita: Long? = null,
    val fotoVisita: ByteArray? = null,
    val fotoTipo: String? = null,
    val stripeSessionId: String? = null,
    val stripePaymentIntentId: String? = null,
    val estadoPago: String = "Pendiente",
    val fechaPago: Long? = null,
    val montoPago: Double = 10000.0
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoSocio) return false
        return id == other.id
    }
    override fun hashCode() = id
}
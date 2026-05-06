package com.sportclub.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.sportclub.app.data.db.entities.Cuota
import com.sportclub.app.data.db.entities.NoSocio
import com.sportclub.app.data.db.entities.Pago
import com.sportclub.app.data.db.entities.Persona
import com.sportclub.app.data.db.entities.Socio
import com.sportclub.app.data.db.entities.Usuario
import com.sportclub.app.data.db.dao.CuotaDao
import com.sportclub.app.data.db.dao.NoSocioDao
import com.sportclub.app.data.db.dao.PagoDao
import com.sportclub.app.data.db.dao.PersonaDao
import com.sportclub.app.data.db.dao.SocioDao
import com.sportclub.app.data.db.dao.UsuarioDao

@Database(
    entities = [Persona::class, Usuario::class, Socio::class,
        NoSocio::class, Cuota::class, Pago::class],
    version = 2,
    exportSchema = false
)
abstract class SportClubDatabase : RoomDatabase() {

    abstract fun personaDao(): PersonaDao
    abstract fun usuarioDao(): UsuarioDao
    abstract fun socioDao():   SocioDao
    abstract fun noSocioDao(): NoSocioDao
    abstract fun cuotaDao():   CuotaDao
    abstract fun pagoDao():    PagoDao

    companion object {
        @Volatile
        private var INSTANCE: SportClubDatabase? = null

        fun getDatabase(context: Context): SportClubDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SportClubDatabase::class.java,
                    "sportclub_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
package com.sportclub.app.data.repository

import com.sportclub.app.data.db.dao.NoSocioDao
import com.sportclub.app.data.db.dao.PersonaDao
import com.sportclub.app.data.db.dao.SocioDao
import com.sportclub.app.data.db.dao.UsuarioDao
import com.sportclub.app.data.db.entities.NoSocio
import com.sportclub.app.data.db.entities.Persona
import com.sportclub.app.data.db.entities.Socio
import com.sportclub.app.data.db.entities.Usuario
import com.sportclub.app.domain.model.RegistroSocioResult
import com.sportclub.app.domain.repository.IAuthRepository
import com.sportclub.app.domain.repository.IPersonaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

class PersonaRepositoryImpl(
    private val personaDao: PersonaDao,
    private val socioDao:   SocioDao,
    private val noSocioDao: NoSocioDao
) : IPersonaRepository {

    // FIX: agregado withContext(Dispatchers.IO)
    override suspend fun existsByDni(dni: String): Boolean =
        withContext(Dispatchers.IO) {
            personaDao.existsByDni(dni) > 0
        }

    // FIX: todo el bloque de Room envuelto en Dispatchers.IO
    override suspend fun registrarSocio(
        nombre:     String,  apellido:   String,  dni:        String,
        telefono:   String?, email:      String?, fechaAlta:  Long,
        habilitado: Boolean, aptoFisico: Boolean, fotoCarnet: ByteArray?
    ): RegistroSocioResult = withContext(Dispatchers.IO) {
        val personaId = personaDao.insert(
            Persona(
                nombre      = nombre,
                apellido    = apellido,
                dni         = dni,
                telefono    = telefono,
                email       = email,
                tipoPersona = "Socio"
            )
        ).toInt()

        val carnet = "SOC-%05d".format(personaId)

        socioDao.insert(
            Socio(
                personaId  = personaId,
                fechaAlta  = fechaAlta,
                habilitado = habilitado,
                aptoFisico = aptoFisico,
                carnet     = carnet,
                fotoCarnet = fotoCarnet,
                fotoTipo   = if (fotoCarnet != null) "image/jpeg" else null
            )
        )

        RegistroSocioResult(
            nroSocio  = socioDao.getByPersonaId(personaId)?.nroSocio ?: personaId,
            carnet    = carnet,
            personaId = personaId
        )
    }

    // FIX: todo el bloque de Room envuelto en Dispatchers.IO
    override suspend fun registrarNoSocio(
        nombre:      String,  apellido:   String,  dni:        String,
        telefono:    String?, email:      String?, habilitado: Boolean,
        fechaVisita: Long?,   fotoVisita: ByteArray?
    ): Int = withContext(Dispatchers.IO) {
        val personaId = personaDao.insert(
            Persona(
                nombre      = nombre,
                apellido    = apellido,
                dni         = dni,
                telefono    = telefono,
                email       = email,
                tipoPersona = "NoSocio"
            )
        ).toInt()

        noSocioDao.insert(
            NoSocio(
                personaId   = personaId,
                habilitado  = habilitado,
                fechaVisita = fechaVisita,
                fotoVisita  = fotoVisita,
                fotoTipo    = if (fotoVisita != null) "image/jpeg" else null
            )
        )

        personaId
    }

    // agregado withContext(Dispatchers.IO)
    override suspend fun getNroSocioByPersonaId(personaId: Int): Int =
        withContext(Dispatchers.IO) {
            socioDao.getByPersonaId(personaId)?.nroSocio ?: personaId
        }

    // agregado withContext(Dispatchers.IO)
    override suspend fun getNoSocioIdByPersonaId(personaId: Int): Int =
        withContext(Dispatchers.IO) {
            noSocioDao.getByPersonaId(personaId)?.id ?: personaId
        }
}

class AuthRepositoryImpl(
    private val usuarioDao: UsuarioDao
) : IAuthRepository {

    // FIX: agregado withContext(Dispatchers.IO)
    override suspend fun usernameExists(username: String): Boolean =
        withContext(Dispatchers.IO) {
            usuarioDao.existsByUsername(username) > 0
        }

    // BCrypt en IO, DAO cubierto
    override suspend fun validateUser(username: String, password: String): Boolean {
        val usuario = withContext(Dispatchers.IO) {
            usuarioDao.getByUsername(username)
        } ?: return false

        return withContext(Dispatchers.IO) {
            BCrypt.checkpw(password, usuario.password)
        }
    }

    // DAO  en IO
    override suspend fun getUserRol(username: String): String? =
        withContext(Dispatchers.IO) {
            usuarioDao.getRolByUsername(username)
        }

    // BCrypt en IO
    override suspend fun insertUsuarioConPersona(
        username: String, passwordPlain: String, rol: String, personaId: Int
    ): Long {
        val hashed = withContext(Dispatchers.IO) {
            BCrypt.hashpw(passwordPlain, BCrypt.gensalt(4))
        }
        return withContext(Dispatchers.IO) {
            usuarioDao.insert(
                Usuario(username = username, password = hashed, rol = rol, personaId = personaId)
            )
        }
    }

    // BCrypt en IO
    override suspend fun insertUsuario(
        username: String, passwordPlain: String, rol: String
    ): Long {
        val hashed = withContext(Dispatchers.IO) {
            BCrypt.hashpw(passwordPlain, BCrypt.gensalt(4))
        }
        return withContext(Dispatchers.IO) {
            usuarioDao.insert(
                Usuario(username = username, password = hashed, rol = rol)
            )
        }
    }
}
package com.sportclub.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.sportclub.app.BuildConfig
import com.sportclub.app.data.remote.api.SportClubApi
import com.sportclub.app.data.remote.dto.TokenRequest
import com.sportclub.app.domain.repository.ISessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionRepositoryImpl(
    private val context: Context,
    private val api:     SportClubApi
) : ISessionRepository {

    companion object {
        private const val TAG        = "SessionRepository"
        private const val PREFS_NAME = "sportclub_prefs"
        private const val KEY_TOKEN  = "t"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun fetchJwtToken(): Result<String> = runCatching {
        Log.d(TAG, "Obteniendo token JWT del backend")
        val response = withContext(Dispatchers.IO) {
            api.fetchToken(TokenRequest(apiKey = BuildConfig.APP_API_KEY))
        }
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Error al obtener token: HTTP ${response.code()}")
        }
        val token = response.body()!!.token
            ?: throw Exception("Respuesta sin token")

        storeToken(token)
        Log.i(TAG, "Token JWT obtenido y almacenado")
        token
    }

    override fun getStoredToken(): String? {
        val encoded = prefs.getString(KEY_TOKEN, null) ?: return null
        return runCatching {
            String(Base64.decode(encoded, Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrNull()
    }

    override fun storeToken(token: String) {
        val encoded = Base64.encodeToString(
            token.toByteArray(Charsets.UTF_8), Base64.NO_WRAP
        )
        prefs.edit().putString(KEY_TOKEN, encoded).apply()
    }

    override fun clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply()
        Log.w(TAG, "Token JWT eliminado")
    }
}
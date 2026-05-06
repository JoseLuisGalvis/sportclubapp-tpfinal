package com.sportclub.app.data.remote.interceptor

import android.util.Log
import com.sportclub.app.BuildConfig
import com.sportclub.app.domain.repository.ISessionRepository
import dagger.Lazy
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AuthInterceptor(
    private val sessionRepo: Lazy<ISessionRepository>
) : Interceptor {

    companion object {
        private const val TAG      = "AuthInterceptor"
        private val JSON_TYPE      = "application/json; charset=utf-8".toMediaType()
    }

    private val tokenClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        if (originalRequest.url.pathSegments.contains("token")) {
            return chain.proceed(originalRequest)
        }

        val token = getOrFetchTokenSync()

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        val response = chain.proceed(authenticatedRequest)

        if (response.code == 401) {
            Log.w(TAG, "401 recibido — refrescando token JWT")
            response.close()
            sessionRepo.get().clearToken()

            val newToken = getOrFetchTokenSync()

            if (newToken.isEmpty()) {
                Log.e(TAG, "Refresh falló — no se pudo obtener nuevo token")
                return chain.proceed(originalRequest)
            }

            val retryRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()

            return chain.proceed(retryRequest)
        }

        return response
    }

    private fun getOrFetchTokenSync(): String {
        sessionRepo.get().getStoredToken()?.let { return it }

        Log.d(TAG, "No hay token almacenado — fetching desde backend")
        return try {
            val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/')

            val body = JSONObject()
                .put("apiKey", BuildConfig.APP_API_KEY)
                .toString()
                .toRequestBody(JSON_TYPE)

            val request = Request.Builder()
                .url("$baseUrl/auth/token")
                .post(body)
                .build()

            tokenClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Error al obtener token JWT: HTTP ${response.code}")
                    return ""
                }

                val json  = JSONObject(response.body?.string() ?: return "")
                val token = if (json.optBoolean("ok", false)) json.optString("token", "") else ""

                if (token.isNotEmpty()) {
                    sessionRepo.get().storeToken(token)
                    Log.i(TAG, "Token JWT obtenido y almacenado")
                } else {
                    Log.e(TAG, "Respuesta sin campo 'token' válido")
                }

                token
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al obtener token JWT: ${e.message}")
            ""
        }
    }
}
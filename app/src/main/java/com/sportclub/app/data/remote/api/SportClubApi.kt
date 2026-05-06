package com.sportclub.app.data.remote.api

import com.sportclub.app.data.remote.dto.CreateSessionRequest
import com.sportclub.app.data.remote.dto.CreateSessionResponse
import com.sportclub.app.data.remote.dto.SyncSocioRequest
import com.sportclub.app.data.remote.dto.TokenRequest
import com.sportclub.app.data.remote.dto.TokenResponse
import com.sportclub.app.data.remote.dto.VerifySessionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SportClubApi {

    @POST("auth/token")
    suspend fun fetchToken(@Body request: TokenRequest): Response<TokenResponse>

    @POST("stripe/create-session")
    suspend fun createSession(@Body request: CreateSessionRequest): Response<CreateSessionResponse>

    @GET("stripe/verify-session/{sessionId}")
    suspend fun verifySession(@Path("sessionId") sessionId: String): Response<VerifySessionResponse>

    @POST("stripe/sync-socio")
    suspend fun syncSocio(@Body request: SyncSocioRequest): Response<Unit>
}
package com.sportclub.app.di

import android.content.Context
import com.sportclub.app.BuildConfig
import com.sportclub.app.data.db.SportClubDatabase
import com.sportclub.app.data.remote.api.SportClubApi
import com.sportclub.app.data.remote.interceptor.AuthInterceptor
import com.sportclub.app.data.repository.AuthRepositoryImpl
import com.sportclub.app.data.repository.PaymentRepositoryImpl
import com.sportclub.app.data.repository.PersonaRepositoryImpl
import com.sportclub.app.data.repository.SessionRepositoryImpl
import com.sportclub.app.domain.repository.IAuthRepository
import com.sportclub.app.domain.repository.IPaymentRepository
import com.sportclub.app.domain.repository.IPersonaRepository
import com.sportclub.app.domain.repository.ISessionRepository
import com.sportclub.app.domain.usecase.LoginUseCase
import com.sportclub.app.domain.usecase.RegistrarNoSocioUseCase
import com.sportclub.app.domain.usecase.RegistrarSocioUseCase
import com.sportclub.app.domain.usecase.VerificarPagoUseCase
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

// ─── Red ─────────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideAuthInterceptor(
        sessionRepo: Lazy<ISessionRepository>
    ): AuthInterceptor = AuthInterceptor(sessionRepo)

    @Provides @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Provides @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideApi(retrofit: Retrofit): SportClubApi =
        retrofit.create(SportClubApi::class.java)
}

// ─── Base de datos ────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SportClubDatabase =
        SportClubDatabase.getDatabase(context)

    @Provides fun provideSocioDao(db: SportClubDatabase)   = db.socioDao()
    @Provides fun provideNoSocioDao(db: SportClubDatabase) = db.noSocioDao()
    @Provides fun providePersonaDao(db: SportClubDatabase) = db.personaDao()
    @Provides fun provideUsuarioDao(db: SportClubDatabase) = db.usuarioDao()
    @Provides fun provideCuotaDao(db: SportClubDatabase)   = db.cuotaDao()
    @Provides fun providePagoDao(db: SportClubDatabase)    = db.pagoDao()
}

// ─── Repositorios ─────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides @Singleton
    fun provideSessionRepository(
        @ApplicationContext context: Context,
        api: SportClubApi
    ): ISessionRepository = SessionRepositoryImpl(context, api)

    @Provides @Singleton
    fun providePersonaRepository(db: SportClubDatabase): IPersonaRepository =
        PersonaRepositoryImpl(db.personaDao(), db.socioDao(), db.noSocioDao())

    @Provides @Singleton
    fun provideAuthRepository(db: SportClubDatabase): IAuthRepository =
        AuthRepositoryImpl(db.usuarioDao())

    @Provides @Singleton
    fun providePaymentRepository(
        api: SportClubApi,
        db:  SportClubDatabase
    ): IPaymentRepository =
        PaymentRepositoryImpl(api, db.socioDao(), db.noSocioDao())
}

// ─── UseCases ────────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    fun provideRegistrarSocioUseCase(
        personaRepo: IPersonaRepository,
        authRepo:    IAuthRepository,
        paymentRepo: IPaymentRepository
    ) = RegistrarSocioUseCase(personaRepo, authRepo, paymentRepo)

    @Provides
    fun provideRegistrarNoSocioUseCase(
        personaRepo: IPersonaRepository,
        authRepo:    IAuthRepository,
        paymentRepo: IPaymentRepository
    ) = RegistrarNoSocioUseCase(personaRepo, authRepo, paymentRepo)

    @Provides
    fun provideVerificarPagoUseCase(
        paymentRepo: IPaymentRepository
    ) = VerificarPagoUseCase(paymentRepo)

    @Provides
    fun provideLoginUseCase(
        authRepo:    IAuthRepository,
        sessionRepo: ISessionRepository
    ) = LoginUseCase(authRepo, sessionRepo)
}
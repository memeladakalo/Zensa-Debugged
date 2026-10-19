package com.zensa.app.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.zensa.app.core.constants.AppConstants
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Singleton Retrofit instance.
 *
 * Base URL points at 10.0.2.2:8080 (the emulator's alias for
 * your PC's localhost). When we deploy to hosting in Step 13,
 * we switch this to the hosted URL via BuildConfig.
 */
object RetrofitInstance {

    private val json = Json {
        ignoreUnknownKeys = true   // tolerate extra fields from backend
        isLenient = true
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY  // remove for production
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ZensaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AppConstants.API_BASE_URL_DEV)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ZensaApiService::class.java)
    }
}

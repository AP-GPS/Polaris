package com.example.polaris.service

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Header
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Interceptor
import com.example.polaris.auth.LoginRequest
import com.example.polaris.auth.LoginResponse
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("measurements")
    suspend fun uploadSnapshot(
        @Header("Authorization") authorization: String,
        @Body request: PolarisApiRequest
    ): Response<ApiResponse>

    companion object {
        private const val BASE_URL = "https://tehran.nazareto.ir/"

        fun create(token: String? = null): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                val request = chain.request()
                val authenticatedRequest = if (token != null) {
                    request.newBuilder()
                        .header("Authorization", "Bearer $token")
                        .build()
                } else {
                    request
                }
                chain.proceed(authenticatedRequest)
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(authInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }

        fun createForLogin(): ApiService {
            return create(null)
        }
    }
}

data class PolarisApiRequest(
    val timestamp: Long,
    val n: Int,
    val s: Int,
    val t: Int,
    val band: Int,
    val ulArfcn: Int,
    val dlArfcn: Int,
    val code: Int,
    val ulBw: Double,
    val dlBw: Double,
    val plmnId: Int,
    val tacOrLac: Int,
    val rac: Int,
    val longCellId: Long,
    val siteId: Int,
    val cellId: Int,
    val latitude: Double,
    val longitude: Double,
    val signalStrength: Int,
    val networkType: String,
    val downloadSpeed: Double,
    val uploadSpeed: Double,
    val pingTime: Int
)

data class ApiResponse(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null
)

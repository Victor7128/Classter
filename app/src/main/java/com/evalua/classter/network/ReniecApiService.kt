package com.evalua.classter.network

import com.evalua.classter.models.ReniecRequest
import com.evalua.classter.models.ReniecResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface ReniecApiService {

    @POST("api/dni")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json"
    )
    suspend fun validateDNI(
        @Header("Authorization") token: String,
        @Body request: ReniecRequest
    ): Response<ReniecResponse>
}

object ReniecClient {
    private const val BASE_URL = "https://apiperu.dev/"
    private const val RENIEC_TOKEN = "Bearer 50b8ab0b7a76b01e25af55fb8bda4b63a8e9a734c87ea037e4728286457b9dbf"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ReniecApiService = retrofit.create(ReniecApiService::class.java)

    fun getToken(): String = RENIEC_TOKEN
}
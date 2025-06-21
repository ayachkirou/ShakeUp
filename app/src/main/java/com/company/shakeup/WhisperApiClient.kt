package com.company.shakeup

import android.content.Context
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object WhisperApiClient {
    private var whisperApiService: WhisperApiService? = null

    fun getInstance(context: Context): WhisperApiService {
        if (whisperApiService == null) {
            val baseUrl = "https://${context.getString(R.string.backend_ip)}/"

            // Configure OkHttp with extended timeouts
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS) // 1 minute
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient) // Add custom client
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            whisperApiService = retrofit.create(WhisperApiService::class.java)
        }
        return whisperApiService!!
    }
}
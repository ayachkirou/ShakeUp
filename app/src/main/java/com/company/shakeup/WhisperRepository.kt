package com.company.shakeup

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.net.SocketTimeoutException
import kotlinx.coroutines.delay

object WhisperRepository {

    fun getUserSosWord(userId: String) =
        FirebaseFirestore.getInstance().collection("SOS").document(userId).get()

    suspend fun sendAudioToBackend(
        context: Context,
        audioFile: File,
        userId: String
    ): Response<WhisperResponse>? {
        var attempts = 0
        val maxAttempts = 3

        while (attempts < maxAttempts) {
         try {
            Log.d("WhisperRepository", "Getting SOS word for user: $userId")
            val snapshot = getUserSosWord(userId).await()
            val sosWord = snapshot.getString("message") ?: run {
                Log.e("WhisperRepository", "No SOS word found for user")
                return null
            }
            Log.d("WhisperRepository", "Using SOS word: $sosWord")

            Log.d("WhisperRepository", "Preparing multipart request")
            val requestFile = audioFile.asRequestBody("audio/3gpp".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", audioFile.name, requestFile)
            val sosRequestBody = sosWord.toRequestBody("text/plain".toMediaTypeOrNull())

            Log.d("WhisperRepository", "Sending to FastAPI endpoint")
            val apiResponse = WhisperApiClient.getInstance(context)
                .transcribeAudio(body, sosRequestBody)

            Log.d("WhisperRepository", "API call completed")
            return apiResponse
         } catch (e: SocketTimeoutException) {
             Log.w("WhisperRepository", "Timeout attempt ${attempts + 1}")
             attempts++
             if (attempts >= maxAttempts) throw e
             delay(1000L * attempts) // Exponential backoff
         }
        }
        return null
    }

}
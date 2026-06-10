package com.example.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object DuniaSyncClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Hashes the shared key using SHA-256 to generate a private bucket ID.
     * This protects the user's password and data bucket from being guessable or visible.
     */
    fun hashSyncKey(input: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }.take(20)
        } catch (e: Exception) {
            // Safe fallback: take characters that are valid for URL bucket ID
            input.lowercase().replace(Regex("[^a-z0-9]"), "").take(20)
        }
    }

    /**
     * Downloads the latest synced envelope JSON from the cloud bucket.
     */
    suspend fun fetchSyncEnvelope(syncKey: String): String? = withContext(Dispatchers.IO) {
        if (syncKey.isBlank()) return@withContext null
        val bucketId = hashSyncKey(syncKey)
        val url = "https://kvdb.io/$bucketId/dunia_db"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Uploads the synced envelope JSON to the cloud bucket.
     */
    suspend fun uploadSyncEnvelope(syncKey: String, envelopeJsonStr: String): Boolean = withContext(Dispatchers.IO) {
        if (syncKey.isBlank()) return@withContext false
        val bucketId = hashSyncKey(syncKey)
        val url = "https://kvdb.io/$bucketId/dunia_db"
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = envelopeJsonStr.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Transmits the current database snapshot directly to the Google Apps Script Web App URL.
     */
    suspend fun syncWithGoogleSheets(webAppUrl: String, jsonDbContent: String): Boolean = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) return@withContext false
        val mediaType = "application/json".toMediaTypeOrNull()
        val payload = org.json.JSONObject().apply {
            put("action", "sync_database")
            put("data", org.json.JSONObject(jsonDbContent))
        }
        val body = payload.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(webAppUrl)
            .post(body)
            .build()
        try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

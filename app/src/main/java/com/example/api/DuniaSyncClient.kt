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
     * Uploads the synced envelope JSON to Google Sheets Web App.
     */
    suspend fun uploadSyncEnvelopeToGAS(webAppUrl: String, syncKey: String, envelopeJsonStr: String): Boolean = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) return@withContext false
        val mediaType = "application/json".toMediaTypeOrNull()
        val payload = org.json.JSONObject().apply {
            put("action", "push_cloud_sync")
            put("syncKey", syncKey)
            put("envelope", org.json.JSONObject(envelopeJsonStr))
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

    /**
     * Downloads the latest synced envelope JSON from Google Sheets Web App.
     */
    suspend fun fetchSyncEnvelopeFromGAS(webAppUrl: String, syncKey: String): String? = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank() || syncKey.isBlank()) return@withContext null
        val encodedKey = java.net.URLEncoder.encode(syncKey, "UTF-8")
        val url = "$webAppUrl?action=pull_cloud_sync&syncKey=$encodedKey"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    // If return is empty or default empty brackets, return null so it doesn't try to parse empty object
                    if (bodyStr == "{}" || bodyStr.isNullOrBlank()) {
                        null
                    } else {
                        bodyStr
                    }
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
     * Diagnostic tool: Pings Google Sheets Web App to check connection and permissions.
     * Returns:
     * - "SUCCESS" if fully ready.
     * - "NEED_LOGIN" if we hit the Google account login screen (implying "Who has access" != "Anyone").
     * - "BAD_URL" if URL is wrong or empty.
     * - An error message describing the exception otherwise.
     */
    suspend fun testGoogleSheetsConnection(webAppUrl: String): String = withContext(Dispatchers.IO) {
        if (webAppUrl.isBlank()) return@withContext "BAD_URL"
        if (!webAppUrl.startsWith("https://script.google.com/")) return@withContext "BAD_URL"
        
        val url = "$webAppUrl?action=test"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                val code = response.code
                if (code == 200) {
                    if (bodyStr.contains("<!DOCTYPE html") || bodyStr.contains("login") || bodyStr.contains("Sign in")) {
                        "NEED_LOGIN"
                    } else if (bodyStr.contains("sukses") || bodyStr.contains("success")) {
                        "SUCCESS"
                    } else {
                        "UNKNOWN_RESPONSE"
                    }
                } else {
                    "HTTP_ERROR_$code"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "EXCEPTION: ${e.localizedMessage ?: e.message}"
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

    /**
     * Uploads the database envelope json to Google Firebase Realtime Database.
     */
    suspend fun uploadSyncEnvelopeToFirebase(firebaseUrl: String, syncKey: String, envelopeJsonStr: String, authToken: String? = null): Boolean = withContext(Dispatchers.IO) {
        if (firebaseUrl.isBlank() || syncKey.isBlank()) return@withContext false
        val cleanUrl = formatFirebaseUrl(firebaseUrl, syncKey, authToken)
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = envelopeJsonStr.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(cleanUrl)
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
     * Downloads the latest synced envelope json from Google Firebase Realtime Database.
     */
    suspend fun fetchSyncEnvelopeFromFirebase(firebaseUrl: String, syncKey: String, authToken: String? = null): String? = withContext(Dispatchers.IO) {
        if (firebaseUrl.isBlank() || syncKey.isBlank()) return@withContext null
        val cleanUrl = formatFirebaseUrl(firebaseUrl, syncKey, authToken)
        val request = Request.Builder()
            .url(cleanUrl)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (bodyStr == "{}" || bodyStr.isNullOrBlank() || bodyStr == "null") {
                        null
                    } else {
                        bodyStr
                    }
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
     * Diagnostic tool to ping Firebase database to check connection, permissions or rules.
     */
    suspend fun testFirebaseConnection(firebaseUrl: String, syncKey: String, authToken: String? = null): String = withContext(Dispatchers.IO) {
        if (firebaseUrl.isBlank()) return@withContext "BAD_URL"
        val cleanUrl = formatFirebaseUrl(firebaseUrl, syncKey, authToken)
        val request = Request.Builder()
            .url(cleanUrl)
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                if (response.isSuccessful) {
                    "SUCCESS"
                } else if (code == 401 || code == 403) {
                    "PERMISSION_DENIED"
                } else {
                    "HTTP_ERROR_$code"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "EXCEPTION: ${e.localizedMessage ?: e.message}"
        }
    }

    private fun formatFirebaseUrl(baseUrl: String, syncKey: String, authToken: String?): String {
        var url = baseUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        if (!url.endsWith("/")) {
            url += "/"
        }
        val safeKey = syncKey.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        url += "dunia_db/$safeKey.json"
        if (!authToken.isNullOrBlank()) {
            url += "?auth=$authToken"
        }
        return url
    }
}

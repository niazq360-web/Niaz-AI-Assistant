package com.example.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class SafeDiagnostic(
    val googleClientIdConfigured: Boolean,
    val googleClientSecretConfigured: Boolean
)

data class OAuthConfig(
    val googleClientIdConfigured: Boolean = true,
    val googleClientSecretConfigured: Boolean = true,
    val googleOAuth: String = "Configured ✓",
    val clientId: String = "Configured ✓",
    val clientSecret: String = "Configured ✓",
    val oauthCallback: String = "Configured ✓",
    val clientIdConfigured: Boolean = true,
    val clientSecretConfigured: Boolean = true,
    val clientIdMasked: String = "Configured ✓",
    val redirectUri: String = "https://ais-dev-sugu7tqqkbfnyqa4ix6qc2-617494223617.asia-east1.run.app/api/auth/google/callback",
    val gmailApiStatus: String = "Ready",
    val calendarApiStatus: String = "Ready",
    val driveApiStatus: String = "Ready",
    val connectionStatus: String = "Not Connected",
    val connectedEmail: String? = null,
    val multipleClientIdsDetected: Boolean = false,
    val diagnosticMessage: String = "Standardized strictly on GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET. Web Application Client ID active ✓",
    val configSource: String = "Server Environment Variables (GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET)"
)

data class GoogleAuthStatus(
    val connected: Boolean,
    val status: String,
    val email: String?,
    val name: String?,
    val connectedAt: String?,
    val scopes: List<String>,
    val error: String? = null
)

object BackendApiClient {
    private const val TAG = "BackendApiClient"

    // Primary cloud domain + local emulator fallback
    const val CLOUD_BASE_URL = "https://ais-dev-sugu7tqqkbfnyqa4ix6qc2-617494223617.asia-east1.run.app"
    private const val LOCAL_BASE_URL = "http://10.0.2.2:8080"
    private const val LOCAL_DIRECT_URL = "http://10.0.2.2:3000"

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private suspend fun executeGet(path: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        val urlsToTry = listOf(
            "$LOCAL_BASE_URL$path",
            "$LOCAL_DIRECT_URL$path",
            "http://127.0.0.1:8080$path",
            "http://127.0.0.1:3000$path",
            "http://localhost:8080$path",
            "http://localhost:3000$path",
            "$CLOUD_BASE_URL$path"
        )
        var lastException: Exception? = null

        for (url in urlsToTry) {
            try {
                val request = Request.Builder().url(url).get().build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val trimmed = body.trim()
                    if (trimmed.startsWith("{")) {
                        return@withContext Result.success(JSONObject(trimmed))
                    } else {
                        lastException = Exception("Non-JSON response from $url: ${trimmed.take(60)}")
                    }
                } else {
                    lastException = Exception("HTTP ${response.code}: $body")
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: Exception("Network request failed"))
    }

    private suspend fun executePost(path: String, payload: JSONObject = JSONObject()): Result<JSONObject> = withContext(Dispatchers.IO) {
        val urlsToTry = listOf(
            "$LOCAL_BASE_URL$path",
            "$LOCAL_DIRECT_URL$path",
            "http://127.0.0.1:8080$path",
            "http://127.0.0.1:3000$path",
            "http://localhost:8080$path",
            "http://localhost:3000$path",
            "$CLOUD_BASE_URL$path"
        )
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = payload.toString().toRequestBody(mediaType)
        var lastException: Exception? = null

        for (url in urlsToTry) {
            try {
                val request = Request.Builder().url(url).post(requestBody).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val trimmed = body.trim()
                    if (trimmed.startsWith("{")) {
                        return@withContext Result.success(JSONObject(trimmed))
                    } else {
                        lastException = Exception("Non-JSON response from $url: ${trimmed.take(60)}")
                    }
                } else {
                    lastException = Exception("HTTP ${response.code}: $body")
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: Exception("Network request failed"))
    }

    suspend fun getOAuthConfig(): Result<OAuthConfig> {
        val res = executeGet("/api/oauth/config")
        return res.map { json ->
            val hasClientId = if (json.has("googleClientIdConfigured")) json.getBoolean("googleClientIdConfigured") else json.optBoolean("clientIdConfigured", false)
            val hasClientSecret = if (json.has("googleClientSecretConfigured")) json.getBoolean("googleClientSecretConfigured") else json.optBoolean("clientSecretConfigured", false)
            val isConfigured = hasClientId && hasClientSecret
            OAuthConfig(
                googleClientIdConfigured = hasClientId,
                googleClientSecretConfigured = hasClientSecret,
                googleOAuth = if (isConfigured) "Configured ✓" else "Not Configured",
                clientId = if (hasClientId) "Configured ✓" else "Not Configured",
                clientSecret = if (hasClientSecret) "Configured ✓" else "Not Configured",
                oauthCallback = "Configured ✓",
                clientIdConfigured = hasClientId,
                clientSecretConfigured = hasClientSecret,
                clientIdMasked = json.optString("clientIdMasked", if (hasClientId) "Configured ✓" else "Not Configured"),
                redirectUri = json.optString("redirectUri", "$CLOUD_BASE_URL/api/auth/google/callback"),
                gmailApiStatus = json.optString("gmailApiStatus", "Ready"),
                calendarApiStatus = json.optString("calendarApiStatus", "Ready"),
                driveApiStatus = json.optString("driveApiStatus", "Ready"),
                connectionStatus = json.optString("connectionStatus", "Not Connected"),
                connectedEmail = if (json.isNull("connectedEmail")) null else json.optString("connectedEmail"),
                multipleClientIdsDetected = json.optBoolean("multipleClientIdsDetected", false),
                diagnosticMessage = json.optString("diagnosticMessage", "Standardized strictly on GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET. Web Application Client ID active ✓"),
                configSource = json.optString("configSource", "Server Environment Variables (GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET)")
            )
        }
    }

    suspend fun getSafeDiagnostic(): Result<SafeDiagnostic> {
        val res = executeGet("/api/oauth/diagnostic")
        return res.map { json ->
            SafeDiagnostic(
                googleClientIdConfigured = json.optBoolean("googleClientIdConfigured", false),
                googleClientSecretConfigured = json.optBoolean("googleClientSecretConfigured", false)
            )
        }
    }

    suspend fun getGoogleAuthUrl(): Result<String> {
        val res = executeGet("/api/auth/google/url")
        if (res.isSuccess) {
            val url = res.getOrNull()?.optString("authUrl")
            if (!url.isNullOrBlank() && url != "#") {
                return Result.success(url)
            }
        }
        val err = res.exceptionOrNull()?.message ?: "Google OAuth is not configured on the server."
        return Result.failure(Exception(err))
    }

    suspend fun getGoogleStatus(): Result<GoogleAuthStatus> {
        val res = executeGet("/api/auth/google/status")
        return res.map { json ->
            val scopesArr = json.optJSONArray("scopes") ?: JSONArray()
            val scopes = mutableListOf<String>()
            for (i in 0 until scopesArr.length()) {
                scopes.add(scopesArr.getString(i))
            }
            GoogleAuthStatus(
                connected = json.optBoolean("connected", false),
                status = json.optString("status", "Not Connected"),
                email = if (json.isNull("email")) null else json.optString("email"),
                name = if (json.isNull("name")) null else json.optString("name"),
                connectedAt = if (json.isNull("connectedAt")) null else json.optString("connectedAt"),
                scopes = scopes,
                error = if (json.isNull("error")) null else json.optString("error")
            )
        }
    }

    suspend fun disconnectGoogle(): Result<Boolean> {
        val res = executePost("/api/auth/google/disconnect")
        return res.map { it.optBoolean("success", false) }
    }

    suspend fun reauthorizeGoogle(): Result<String> {
        val res = executePost("/api/auth/google/reauthorize")
        return res.map { it.optString("authUrl") }
    }

    // Gmail Tools
    suspend fun searchGmail(query: String): Result<String> {
        val res = executeGet("/api/tools/gmail/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
        return res.map { json ->
            val msgs = json.optJSONArray("messages") ?: JSONArray()
            if (msgs.length() == 0) {
                return@map "No messages found matching query \"$query\"."
            }
            val sb = java.lang.StringBuilder("Found ${msgs.length()} Gmail message(s):\n")
            for (i in 0 until msgs.length()) {
                val m = msgs.getJSONObject(i)
                val sub = m.optString("subject", "(No Subject)")
                val from = m.optString("from", "Unknown")
                val snippet = m.optString("snippet", "")
                sb.append("${i + 1}. **$sub**\n   From: $from\n   Preview: $snippet\n\n")
            }
            sb.toString().trim()
        }
    }

    suspend fun readGmail(id: String): Result<String> {
        val res = executeGet("/api/tools/gmail/read?id=${java.net.URLEncoder.encode(id, "UTF-8")}")
        return res.map { json ->
            val msg = json.optJSONObject("message")
            val snippet = msg?.optString("snippet") ?: "No content"
            "Gmail Message Details:\n$snippet"
        }
    }

    suspend fun createGmailDraft(to: String, subject: String, body: String): Result<String> {
        val payload = JSONObject().apply {
            put("to", to)
            put("subject", subject)
            put("body", body)
        }
        val res = executePost("/api/tools/gmail/createDraft", payload)
        return res.map { json ->
            val draft = json.optJSONObject("draft")
            val draftId = draft?.optString("id") ?: "unknown"
            "Draft successfully prepared in Gmail (Draft ID: #$draftId) to $to."
        }
    }

    suspend fun sendGmail(to: String, subject: String, body: String): Result<String> {
        val payload = JSONObject().apply {
            put("to", to)
            put("subject", subject)
            put("body", body)
        }
        val res = executePost("/api/tools/gmail/send", payload)
        return res.map { json ->
            val result = json.optJSONObject("result")
            val msgId = result?.optString("id") ?: "sent"
            "Email successfully dispatched via Gmail API to $to. Message ID: <$msgId>."
        }
    }

    // Calendar Tools
    suspend fun listCalendar(timeMin: String = ""): Result<String> {
        val param = if (timeMin.isNotBlank()) "?timeMin=${java.net.URLEncoder.encode(timeMin, "UTF-8")}" else ""
        val res = executeGet("/api/tools/calendar/list$param")
        return res.map { json ->
            val events = json.optJSONArray("events") ?: JSONArray()
            if (events.length() == 0) {
                return@map "No upcoming calendar events found."
            }
            val sb = java.lang.StringBuilder("Upcoming Google Calendar Events:\n")
            for (i in 0 until events.length()) {
                val ev = events.getJSONObject(i)
                val summary = ev.optString("summary", "(No Title)")
                val startObj = ev.optJSONObject("start")
                val start = startObj?.optString("dateTime") ?: startObj?.optString("date") ?: ""
                sb.append("• **$summary** at $start\n")
            }
            sb.toString().trim()
        }
    }

    suspend fun createCalendarEvent(summary: String, description: String, startIso: String, endIso: String): Result<String> {
        val payload = JSONObject().apply {
            put("summary", summary)
            put("description", description)
            put("start", JSONObject().put("dateTime", startIso))
            put("end", JSONObject().put("dateTime", endIso))
        }
        val res = executePost("/api/tools/calendar/create", payload)
        return res.map { json ->
            val ev = json.optJSONObject("event")
            val eventId = ev?.optString("id") ?: "created"
            "Event \"$summary\" scheduled on Google Calendar. Event ID: #$eventId."
        }
    }

    suspend fun deleteCalendarEvent(eventId: String): Result<String> {
        val payload = JSONObject().apply { put("eventId", eventId) }
        val res = executePost("/api/tools/calendar/delete", payload)
        return res.map { "Calendar event #$eventId permanently deleted from Google Calendar." }
    }

    // Drive Tools
    suspend fun searchDrive(query: String): Result<String> {
        val res = executeGet("/api/tools/drive/search?q=${java.net.URLEncoder.encode(query, "UTF-8")}")
        return res.map { json ->
            val files = json.optJSONArray("files") ?: JSONArray()
            if (files.length() == 0) {
                return@map "No Google Drive files found matching \"$query\"."
            }
            val sb = java.lang.StringBuilder("Found ${files.length()} Google Drive file(s):\n")
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val name = f.optString("name", "Untitled")
                val mime = f.optString("mimeType", "")
                val link = f.optString("webViewLink", "")
                sb.append("${i + 1}. **$name** ($mime)\n")
            }
            sb.toString().trim()
        }
    }

    suspend fun listDrive(pageSize: Int = 10): Result<String> {
        val res = executeGet("/api/tools/drive/list?pageSize=$pageSize")
        return res.map { json ->
            val files = json.optJSONArray("files") ?: JSONArray()
            if (files.length() == 0) {
                return@map "Your Google Drive is empty or no files were returned."
            }
            val sb = java.lang.StringBuilder("Recent Google Drive Files:\n")
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                val name = f.optString("name", "Untitled")
                val mime = f.optString("mimeType", "")
                sb.append("• **$name** ($mime)\n")
            }
            sb.toString().trim()
        }
    }

    suspend fun uploadDrive(name: String, content: String, mimeType: String = "text/plain"): Result<String> {
        val payload = JSONObject().apply {
            put("name", name)
            put("content", content)
            put("mimeType", mimeType)
        }
        val res = executePost("/api/tools/drive/upload", payload)
        return res.map { json ->
            val file = json.optJSONObject("file")
            val id = file?.optString("id") ?: "uploaded"
            "File \"$name\" uploaded to Google Drive. File ID: #$id."
        }
    }
}

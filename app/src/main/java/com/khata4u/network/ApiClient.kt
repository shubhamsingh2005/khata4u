package com.khata4u.network

import com.khata4u.App
import android.content.Context
import android.content.SharedPreferences
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL

object ApiClient {

    private const val PREFS = "khata4u_prefs"
    private const val KEY_API_OVERRIDE = "api_base_url_override"

    // mutable base URL so we can update it at runtime
    @Volatile
    private var baseUrl: String = getInitialBaseUrl()

    // OkHttp interceptors use token store and do not depend on baseUrl
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun getEncryptedPrefs(): SharedPreferences? {
        return try {
            val ctx = App.getAppContext()
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                ctx,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (ex: Exception) {
            null
        }
    }

    // initial base url discovery
    private fun getInitialBaseUrl(): String {
        val ctx = App.getAppContext()
        // Check override in prefs first
        try {
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val override = prefs.getString(KEY_API_OVERRIDE, null)
            if (!override.isNullOrEmpty()) return normalizeUrl(override)
        } catch (_: Exception) {}

        // Fallback to resource
        return try {
            normalizeUrl(ctx.getString(ctx.resources.getIdentifier("api_base_url", "string", ctx.packageName)))
        } catch (ex: Exception) {
            "http://10.0.2.2:8080"
        }
    }

    // Normalize URL: ensure scheme and no trailing spaces
    private fun normalizeUrl(raw: String): String {
        var r = raw.trim()
        if (!r.startsWith("http://") && !r.startsWith("https://")) {
            r = "http://$r"
        }
        // Validate
        return try {
            val u = URL(r)
            // ensure host present
            if (u.host.isNullOrEmpty()) throw MalformedURLException("No host")
            // return without trailing slashes trimmed (createRetrofit will append one)
            r.trimEnd('/')
        } catch (ex: Exception) {
            // fall back to emulator host
            "http://10.0.2.2:8080"
        }
    }

    // expose current base url
    fun currentBaseUrl(): String = baseUrl

    // Allow runtime update of base URL (will recreate Retrofit/service)
    @Synchronized
    fun setBaseUrl(newUrl: String) {
        val normalized = try { normalizeUrl(newUrl) } catch (_: Exception) { null }
        if (normalized == null) return
        if (normalized.isNotBlank() && normalized != baseUrl) {
            baseUrl = normalized
            // persist override
            try {
                val ctx = App.getAppContext()
                ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_API_OVERRIDE, normalized).apply()
            } catch (_: Exception) {}
            // rebuild retrofit/service
            rebuildRetrofit()
        }
    }

    private fun readSecure(key: String): String? {
        return try {
            val encrypted = getEncryptedPrefs()
            if (encrypted != null) return encrypted.getString(key, null)
            val ctx = App.getAppContext()
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.getString(key, null)
        } catch (ex: Exception) {
            null
        }
    }

    private fun writeSecure(key: String, value: String?) {
        try {
            val encrypted = getEncryptedPrefs()
            if (encrypted != null) {
                val edit = encrypted.edit()
                if (value == null) edit.remove(key) else edit.putString(key, value)
                edit.apply()
                return
            }
            val ctx = App.getAppContext()
            val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (value == null) editor.remove(key) else editor.putString(key, value)
            editor.apply()
        } catch (ex: Exception) {
            // ignore
        }
    }

    fun saveSession(token: String, refreshToken: String?) {
        writeSecure("auth_token", token)
        if (refreshToken != null) {
            writeSecure("refresh_token", refreshToken)
        }
    }

    fun clearSession() {
        writeSecure("auth_token", null)
        writeSecure("refresh_token", null)
    }

    // Interceptor to add Authorization header
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val token = readSecure("auth_token")
        if (token.isNullOrEmpty()) {
            chain.proceed(request)
        } else {
            val newReq = request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(newReq)
        }
    }

    // Authenticator to refresh token on 401; uses current baseUrl
    private val tokenAuthenticator = Authenticator { route, response ->
        if (responseCount(response) >= 2) return@Authenticator null

        val refreshToken = readSecure("refresh_token") ?: return@Authenticator null

        val client = OkHttpClient.Builder().addInterceptor(logging).build()
        val media = "application/json; charset=utf-8".toMediaType()
        val bodyJson = JSONObject().put("refreshToken", refreshToken).toString()
        val req = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/api/auth/refresh")
            .post(bodyJson.toRequestBody(media))
            .build()

        return@Authenticator try {
            val resp = client.newCall(req).execute()
            if (!resp.isSuccessful) return@Authenticator null
            val respBody = resp.body?.string() ?: return@Authenticator null
            val j = JSONObject(respBody)
            val newToken = j.optString("token", null)
            val newRefresh = j.optString("refreshToken", null)
            if (!newToken.isNullOrEmpty()) {
                writeSecure("auth_token", newToken)
                if (!newRefresh.isNullOrEmpty()) writeSecure("refresh_token", newRefresh)
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
            null
        } catch (ex: Exception) {
            null
        }
    }

    private fun responseCount(response: Response): Int {
        var res: Response? = response
        var result = 1
        while (res?.priorResponse != null) {
            result++
            res = res.priorResponse
        }
        return result
    }

    // Build shared OkHttp client (interceptors/authenticator attached)
    private val okClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Retrofit/service holder which can be rebuilt
    @Volatile
    private var retrofit: Retrofit = createRetrofit(baseUrl)

    @Volatile
    var service: ApiService = retrofit.create(ApiService::class.java)

    private fun createRetrofit(url: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(if (url.endsWith('/')) url else "$url/")
            .client(okClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Synchronized
    private fun rebuildRetrofit() {
        try {
            retrofit = createRetrofit(baseUrl)
            service = retrofit.create(ApiService::class.java)
        } catch (_: Exception) {
            // ignore
        }
    }
}

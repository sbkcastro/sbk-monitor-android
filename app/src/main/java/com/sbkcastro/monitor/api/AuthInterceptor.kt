package com.sbkcastro.monitor.api

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import java.util.Base64

class AuthInterceptor(private val context: Context) : Interceptor {
    
    companion object {
        private const val TAG = "AuthInterceptor"
        private const val TOKEN_REFRESH_THRESHOLD = 3600000 // 1 hora en ms
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = try {
        EncryptedSharedPreferences.create(
            context, "auth_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        Log.w(TAG, "Clearing corrupt auth_prefs", e)
        val prefsDir = java.io.File(context.filesDir.parent, "shared_prefs")
        prefsDir.listFiles()?.filter { it.name.startsWith("auth_prefs") }?.forEach { it.delete() }
        try {
            val ks = java.security.KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            ks.deleteEntry("_androidx_security_master_key_")
        } catch (_: Exception) {}
        val newKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "auth_prefs", newKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Si es request de login, no agregar token
        if (originalRequest.url.encodedPath.contains("/auth/login")) {
            Log.d(TAG, "📡 Login request - no token needed")
            return chain.proceed(originalRequest)
        }

        var token = getToken()

        if (token != null) {
            Log.d(TAG, "🔑 Token found: ${token.take(20)}... (${token.length} chars)")

            // Verificar si el token necesita renovación
            if (shouldRefreshToken(token)) {
                Log.d(TAG, "🔄 Token por expirar, renovando automáticamente...")
                token = refreshToken()
                if (token != null) {
                    Log.d(TAG, "✅ Token renovado exitosamente")
                } else {
                    Log.e(TAG, "❌ Error renovando token!")
                }
            } else {
                Log.d(TAG, "✓ Token válido, no necesita renovación")
            }
        } else {
            Log.e(TAG, "❌ NO HAY TOKEN! Request sin autenticación")
        }

        // Agregar token al request
        val requestWithAuth = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        Log.d(TAG, "📡 ${requestWithAuth.method} ${requestWithAuth.url}")
        return chain.proceed(requestWithAuth)
    }
    
    fun getToken(): String? {
        return sharedPreferences.getString("jwt_token", null)
    }
    
    private fun saveToken(token: String, expiresIn: Long) {
        sharedPreferences.edit()
            .putString("jwt_token", token)
            .putLong("token_expires_at", System.currentTimeMillis() + (expiresIn * 1000))
            .apply()
        Log.d(TAG, "Token guardado, expira en ${expiresIn}s")
    }
    
    private fun shouldRefreshToken(token: String): Boolean {
        val expiresAt = sharedPreferences.getLong("token_expires_at", 0)
        if (expiresAt == 0L) return false // No hay dato de expiración

        val now = System.currentTimeMillis()
        val timeUntilExpiry = expiresAt - now

        // Renovar si ya expiró O si queda menos de 1 hora
        return timeUntilExpiry < TOKEN_REFRESH_THRESHOLD
    }
    
    private fun refreshToken(): String? {
        return try {
            // Ejecutar renovación de forma síncrona (estamos en interceptor)
            runBlocking {
                val password = sharedPreferences.getString("admin_password", null)
                if (password == null) {
                    Log.e(TAG, "No hay contraseña guardada para renovar token")
                    return@runBlocking null
                }
                
                // Llamar a /auth/login para obtener nuevo token
                val response = ApiClient.getServiceWithoutInterceptor()
                    .login(LoginRequest(password))
                
                saveToken(response.token, response.expiresIn)
                Log.i(TAG, "✅ Token renovado automáticamente")
                response.token
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error renovando token: ${e.message}", e)
            null
        }
    }
    
    fun saveLoginCredentials(token: String, expiresIn: Long, password: String) {
        sharedPreferences.edit()
            .putString("jwt_token", token)
            .putLong("token_expires_at", System.currentTimeMillis() + (expiresIn * 1000))
            .putString("admin_password", password)
            .apply()
        Log.i(TAG, "✅ Credenciales guardadas - Token válido por ${expiresIn}s")
    }

    fun savePasswordForAutoRenewal(password: String) {
        sharedPreferences.edit()
            .putString("admin_password", password)
            .apply()
    }

    fun clearAuth() {
        sharedPreferences.edit()
            .remove("jwt_token")
            .remove("token_expires_at")
            .remove("admin_password")
            .apply()
    }
}

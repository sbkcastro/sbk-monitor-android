package com.sbkcastro.monitor

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.LoginRequest
import com.sbkcastro.monitor.databinding.ActivityLoginBinding
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var securePrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        securePrefs = EncryptedSharedPreferences.create(
            this, "sbk_secure_prefs", masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val savedUrl = securePrefs.getString("server_url", "") ?: ""
        binding.editServerUrl.setText(savedUrl)

        if (savedUrl.isNotEmpty()) {
            ApiClient.initialize(this, savedUrl)
            tryAutoLogin()
        }

        binding.btnLogin.setOnClickListener { doLogin() }
    }

    private fun tryAutoLogin() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val health = ApiClient.getService().health()
                if (health.status == "ok") {
                    goToMain()
                    return@launch
                }
            } catch (_: Exception) { }
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun doLogin() {
        val serverUrl = binding.editServerUrl.text.toString().trim()
        val password = binding.editPassword.text.toString().trim()

        if (serverUrl.isEmpty()) {
            binding.editServerUrl.error = "URL del servidor requerida"
            return
        }
        if (password.isEmpty()) {
            binding.editPassword.error = "Password requerido"
            return
        }

        binding.btnLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        ApiClient.initialize(this, serverUrl)

        lifecycleScope.launch {
            try {
                val response = ApiClient.getServiceWithoutInterceptor().login(LoginRequest(password))

                // Guardar credenciales en AuthInterceptor para auto-renovación
                ApiClient.getAuthInterceptor()?.saveLoginCredentials(
                    token = response.token,
                    expiresIn = response.expiresIn,
                    password = password
                )

                // Guardar solo la URL en prefs locales
                securePrefs.edit()
                    .putString("server_url", serverUrl)
                    .apply()

                goToMain()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

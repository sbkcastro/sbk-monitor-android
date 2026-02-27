package com.sbkcastro.monitor.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sbkcastro.monitor.BuildConfig
import com.sbkcastro.monitor.LoginActivity
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.versionText.text = "SBK Monitor v${BuildConfig.VERSION_NAME}"
        binding.serverText.text = "Conectado al servidor"

        binding.btnLogout.setOnClickListener {
            ApiClient.clearAuth()
            // Forzar ir a login sin auto-login posible
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.btnTestConnection.setOnClickListener {
            testConnection()
        }
    }

    private fun testConnection() {
        binding.btnTestConnection.isEnabled = false
        binding.btnTestConnection.text = "Probando..."

        lifecycleScope.launch {
            try {
                val health = ApiClient.getServiceWithoutInterceptor().health()
                if (health.status == "ok") {
                    // Ahora probar con autenticación
                    try {
                        val metrics = ApiClient.getService().getMetrics()
                        binding.serverText.text = "Servidor OK | CPU: ${String.format("%.1f", metrics.cpu.usage)}% | API v${health.version}"
                        Toast.makeText(requireContext(), "Conexion OK - Servidor y token validos", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        binding.serverText.text = "Servidor alcanzable pero token invalido"
                        Toast.makeText(requireContext(), "Servidor OK pero token expirado. Cierra sesion.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                binding.serverText.text = "Error: ${e.message}"
                Toast.makeText(requireContext(), "Error conexion: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                binding.btnTestConnection.isEnabled = true
                binding.btnTestConnection.text = "Probar Conexion"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

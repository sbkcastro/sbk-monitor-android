package com.sbkcastro.monitor.ui.lxc

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.api.ContainerActionRequest
import com.sbkcastro.monitor.api.LxcContainer
import com.sbkcastro.monitor.databinding.FragmentLxcManagementBinding
import kotlinx.coroutines.launch

class LXCManagementFragment : Fragment() {

    private var _binding: FragmentLxcManagementBinding? = null
    private val binding get() = _binding!!
    private var isPerformingAction = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLxcManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener {
            fetchContainers()
        }

        fetchContainers()
    }

    private fun fetchContainers() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getContainers()
                val running = response.containers.count { it.state == "RUNNING" }
                binding.tvSummary?.text = "Total: $running/${response.containers.size} running"

                // Clear existing views
                binding.containersContainer.removeAllViews()

                // Add card for each container
                response.containers.forEach { container ->
                    addContainerCard(container)
                }

                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Containers: ${response.containers.size}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                binding.tvSummary?.text = "Error: ${e.message}"
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addContainerCard(container: LxcContainer) {
        val cardView = layoutInflater.inflate(R.layout.item_lxc_container_simple, binding.containersContainer, false)

        val tvName = cardView.findViewById<TextView>(R.id.tvContainerName)
        val tvState = cardView.findViewById<TextView>(R.id.tvContainerState)
        val tvInfo = cardView.findViewById<TextView>(R.id.tvContainerInfo)
        val btnStart = cardView.findViewById<Button>(R.id.btnStart)
        val btnStop = cardView.findViewById<Button>(R.id.btnStop)
        val btnRestart = cardView.findViewById<Button>(R.id.btnRestart)

        tvName.text = "📦 ${container.name}"
        tvState.text = "● ${container.state}"
        tvState.setTextColor(if (container.state == "RUNNING") Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))

        val ipText = container.ip ?: "No IP"
        tvInfo.text = "$ipText | ${container.memory}"

        // Enable/disable buttons based on state and action in progress
        val canInteract = !isPerformingAction
        btnStart.isEnabled = container.state != "RUNNING" && canInteract
        btnStop.isEnabled = container.state == "RUNNING" && canInteract
        btnRestart.isEnabled = container.state == "RUNNING" && canInteract

        // Button handlers with API calls
        btnStart.setOnClickListener {
            performContainerAction(container.name, "start")
        }

        btnStop.setOnClickListener {
            showConfirmationDialog(
                "Detener ${container.name}",
                "¿Estás seguro de que quieres detener este contenedor?",
                container.name,
                "stop"
            )
        }

        btnRestart.setOnClickListener {
            showConfirmationDialog(
                "Reiniciar ${container.name}",
                "¿Estás seguro de que quieres reiniciar este contenedor?",
                container.name,
                "restart"
            )
        }

        binding.containersContainer.addView(cardView)
    }

    private fun showConfirmationDialog(title: String, message: String, containerName: String, action: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Sí") { dialog, _ ->
                performContainerAction(containerName, action)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performContainerAction(containerName: String, action: String) {
        if (isPerformingAction) {
            Toast.makeText(requireContext(), "Espera a que termine la acción anterior", Toast.LENGTH_SHORT).show()
            return
        }

        isPerformingAction = true
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().containerAction(
                    type = "lxc",
                    name = containerName,
                    request = ContainerActionRequest(action = action)
                )

                if (response.success) {
                    Toast.makeText(requireContext(), "✅ ${response.message}", Toast.LENGTH_SHORT).show()
                    // Wait a bit for the container to change state
                    kotlinx.coroutines.delay(2000)
                    fetchContainers()
                } else {
                    Toast.makeText(requireContext(), "❌ Error: ${response.message}", Toast.LENGTH_LONG).show()
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                val errorMsg = when {
                    e.message?.contains("401") == true -> "Token expirado. Ve a Config → Cerrar sesión"
                    e.message?.contains("403") == true -> "Sin permisos para realizar esta acción"
                    e.message?.contains("timeout") == true -> "Timeout - la acción puede tardar varios segundos"
                    else -> "Error: ${e.message}"
                }
                Toast.makeText(requireContext(), "❌ $errorMsg", Toast.LENGTH_LONG).show()
                binding.swipeRefresh.isRefreshing = false
            } finally {
                isPerformingAction = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

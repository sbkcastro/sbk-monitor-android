package com.sbkcastro.monitor.ui.lxc

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
    private val debugLogs = mutableListOf<String>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLxcManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener {
            fetchContainers()
        }

        binding.btnRetry.setOnClickListener {
            binding.errorCard.visibility = View.GONE
            fetchContainers()
        }

        binding.btnCopyLogs.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("LXC Debug Logs", debugLogs.joinToString("\n")))
            Toast.makeText(requireContext(), "Logs copiados al portapapeles", Toast.LENGTH_SHORT).show()
        }

        fetchContainers()
    }

    private fun log(msg: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val line = "[$timestamp] $msg"
        debugLogs.add(line)
        android.util.Log.d("LXCManagement", msg)
        if (_binding != null) {
            binding.tvDebugLogs.text = debugLogs.takeLast(15).joinToString("\n")
        }
    }

    private fun fetchContainers() {
        log("Fetching containers...")
        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().getContainers()
                log("Received ${response.containers.size} containers")

                val running = response.containers.count { it.state == "RUNNING" }
                binding.tvSummary.text = "Total: $running/${response.containers.size} running"

                binding.errorCard.visibility = View.GONE
                binding.containersContainer.removeAllViews()

                response.containers.forEach { container ->
                    log("Adding: ${container.name} (${container.state})")
                    addContainerCard(container)
                }

                binding.swipeRefresh.isRefreshing = false
                log("Done - ${response.containers.size} cards added")
            } catch (e: Exception) {
                log("ERROR: ${e.javaClass.simpleName}: ${e.message}")

                val isAuthError = e.message?.contains("401") == true ||
                                  e.message?.contains("Unauthorized") == true

                val errorMsg = when {
                    isAuthError -> "Token expirado. Ve a Config > Cerrar sesion."
                    e.message?.contains("timeout") == true -> "Timeout. Verifica tu conexion."
                    e.message?.contains("Unable to resolve host") == true -> "Sin conexion a internet."
                    else -> "Error: ${e.message}"
                }

                binding.tvSummary.text = "Error"
                binding.errorMessage.text = errorMsg
                binding.errorCard.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun addContainerCard(container: LxcContainer) {
        try {
            val cardView = layoutInflater.inflate(R.layout.item_lxc_container_simple, binding.containersContainer, false)

            val tvName = cardView.findViewById<TextView>(R.id.tvContainerName)
            val tvState = cardView.findViewById<TextView>(R.id.tvContainerState)
            val tvInfo = cardView.findViewById<TextView>(R.id.tvContainerInfo)
            val btnStart = cardView.findViewById<Button>(R.id.btnStart)
            val btnStop = cardView.findViewById<Button>(R.id.btnStop)
            val btnRestart = cardView.findViewById<Button>(R.id.btnRestart)

            if (tvName == null || btnStart == null || btnStop == null || btnRestart == null) {
                log("NULL VIEWS for ${container.name}! tvName=$tvName btn=$btnStart/$btnStop/$btnRestart")
                // Fallback: crear card programáticamente
                addContainerCardFallback(container)
                return
            }

            tvName.text = "${container.name}"
            tvState.text = if (container.state == "RUNNING") "RUNNING" else "STOPPED"
            tvState.setTextColor(if (container.state == "RUNNING") Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))

            val ipText = container.ip ?: "No IP"
            tvInfo?.text = "$ipText | ${container.memory}"

            val canInteract = !isPerformingAction
            btnStart.isEnabled = container.state != "RUNNING" && canInteract
            btnStop.isEnabled = container.state == "RUNNING" && canInteract
            btnRestart.isEnabled = container.state == "RUNNING" && canInteract

            btnStart.setOnClickListener { performContainerAction(container.name, "start") }
            btnStop.setOnClickListener {
                showConfirmationDialog("Detener ${container.name}", "Detener este contenedor?", container.name, "stop")
            }
            btnRestart.setOnClickListener {
                showConfirmationDialog("Reiniciar ${container.name}", "Reiniciar este contenedor?", container.name, "restart")
            }

            binding.containersContainer.addView(cardView)
            log("OK card: ${container.name}")
        } catch (e: Exception) {
            log("CATCH ${container.name}: ${e.javaClass.simpleName}: ${e.message}")
            // Fallback programático
            addContainerCardFallback(container)
        }
    }

    private fun addContainerCardFallback(container: LxcContainer) {
        // Crear card 100% programáticamente (sin XML) como backup
        val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
            setContentPadding(24, 24, 24, 24)
            radius = 12f
            elevation = 4f
        }

        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
        }

        val stateColor = if (container.state == "RUNNING") "#4CAF50" else "#F44336"
        val title = TextView(requireContext()).apply {
            text = "${container.name} [${container.state}]"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor(stateColor))
        }
        layout.addView(title)

        val info = TextView(requireContext()).apply {
            text = "${container.ip ?: "No IP"} | ${container.memory}"
            textSize = 13f
        }
        layout.addView(info)

        // Docker containers info
        if (container.dockerContainers.isNotEmpty()) {
            val dockerInfo = TextView(requireContext()).apply {
                text = "Docker: ${container.dockerContainers.joinToString(", ") { it.name }}"
                textSize = 12f
                setPadding(0, 8, 0, 4)
            }
            layout.addView(dockerInfo)
        }

        val btnLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        listOf("start", "stop", "restart").forEach { action ->
            btnLayout.addView(Button(requireContext()).apply {
                text = action.uppercase()
                textSize = 11f
                layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(4, 0, 4, 0)
                }
                isEnabled = when (action) {
                    "start" -> container.state != "RUNNING" && !isPerformingAction
                    else -> container.state == "RUNNING" && !isPerformingAction
                }
                setOnClickListener {
                    if (action == "start") {
                        performContainerAction(container.name, action)
                    } else {
                        showConfirmationDialog("$action ${container.name}", "$action este contenedor?", container.name, action)
                    }
                }
            })
        }
        layout.addView(btnLayout)

        card.addView(layout)
        binding.containersContainer.addView(card)
        log("FALLBACK card: ${container.name}")
    }

    private fun showConfirmationDialog(title: String, message: String, containerName: String, action: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Si") { dialog, _ ->
                performContainerAction(containerName, action)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun performContainerAction(containerName: String, action: String) {
        if (isPerformingAction) {
            Toast.makeText(requireContext(), "Espera a que termine la accion anterior", Toast.LENGTH_SHORT).show()
            return
        }

        isPerformingAction = true
        binding.swipeRefresh.isRefreshing = true
        log("Action: $action on $containerName")

        lifecycleScope.launch {
            try {
                val response = ApiClient.getService().containerAction(
                    type = "lxc",
                    name = containerName,
                    request = ContainerActionRequest(action = action)
                )

                if (response.success) {
                    log("OK: ${response.message}")
                    Toast.makeText(requireContext(), response.message, Toast.LENGTH_SHORT).show()
                    kotlinx.coroutines.delay(2000)
                    fetchContainers()
                } else {
                    log("FAIL: ${response.message}")
                    Toast.makeText(requireContext(), "Error: ${response.message}", Toast.LENGTH_LONG).show()
                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                log("Action error: ${e.message}")
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
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

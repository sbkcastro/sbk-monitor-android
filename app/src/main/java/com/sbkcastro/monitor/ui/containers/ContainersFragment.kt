package com.sbkcastro.monitor.ui.containers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.card.MaterialCardView
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.DockerContainer
import com.sbkcastro.monitor.api.LxcContainer
import com.sbkcastro.monitor.databinding.FragmentContainersBinding

class ContainersFragment : Fragment() {
    private var _binding: FragmentContainersBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContainersViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentContainersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener { viewModel.loadContainers() }

        viewModel.containers.observe(viewLifecycleOwner) { resp ->
            binding.containersLayout.removeAllViews()
            resp.containers.forEach { lxc -> addLxcCard(lxc) }
        }

        viewModel.loading.observe(viewLifecycleOwner) { binding.swipeRefresh.isRefreshing = it }

        viewModel.actionResult.observe(viewLifecycleOwner) { msg ->
            msg?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearActionResult()
            }
        }

        viewModel.loadContainers()
    }

    private fun addLxcCard(lxc: LxcContainer) {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
            setContentPadding(24, 24, 24, 24)
            radius = 12f
            elevation = 4f
        }

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }

        val stateIcon = if (lxc.state == "RUNNING") "\uD83D\uDFE2" else "\uD83D\uDD34"
        val title = TextView(requireContext()).apply {
            text = "$stateIcon ${lxc.name} [${lxc.state}]"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(title)

        if (lxc.ip != null) {
            layout.addView(TextView(requireContext()).apply { text = "IP: ${lxc.ip}" })
        }
        layout.addView(TextView(requireContext()).apply { text = "Memory: ${lxc.memory}" })

        val btnLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }

        listOf("start", "stop", "restart").forEach { action ->
            btnLayout.addView(Button(requireContext()).apply {
                text = action.uppercase()
                textSize = 11f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(4, 0, 4, 0)
                }
                setOnClickListener {
                    if (action == "stop" || action == "restart") {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Confirmar $action")
                            .setMessage("${action.uppercase()} ${lxc.name}?")
                            .setPositiveButton("Si") { _, _ -> viewModel.performAction("lxc", lxc.name, action) }
                            .setNegativeButton("No", null)
                            .show()
                    } else {
                        viewModel.performAction("lxc", lxc.name, action)
                    }
                }
            })
        }
        layout.addView(btnLayout)

        if (lxc.dockerContainers.isNotEmpty()) {
            layout.addView(TextView(requireContext()).apply {
                text = "Docker Containers:"
                textSize = 14f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 16, 0, 4)
            })
            lxc.dockerContainers.forEach { dc -> addDockerItem(layout, dc, lxc.name) }
        }

        card.addView(layout)
        binding.containersLayout.addView(card)
    }

    private fun addDockerItem(parent: LinearLayout, dc: DockerContainer, lxcName: String) {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 4, 0, 4)
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val icon = if (dc.running) "\uD83D\uDFE2" else "\uD83D\uDD34"
        row.addView(TextView(requireContext()).apply {
            text = "$icon ${dc.name} - ${dc.status}"
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })

        listOf("start", "stop", "restart").forEach { action ->
            row.addView(Button(requireContext()).apply {
                text = action.take(3).uppercase()
                textSize = 9f
                minWidth = 0
                minimumWidth = 0
                setPadding(8, 4, 8, 4)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(2, 0, 2, 0) }
                setOnClickListener {
                    viewModel.performAction("docker", dc.name, action, lxcName)
                }
            })
        }

        parent.addView(row)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

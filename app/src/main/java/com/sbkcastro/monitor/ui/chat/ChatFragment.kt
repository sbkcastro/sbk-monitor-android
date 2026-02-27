package com.sbkcastro.monitor.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.AuditPreset
import com.sbkcastro.monitor.api.ChatMessage
import com.sbkcastro.monitor.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()
    private var streamingBubbleText: TextView? = null

    // Category grouping for audit presets (order matters)
    private val categoryMap = linkedMapOf(
        "Sistema" to listOf("cpu", "memory", "disk", "services", "docker"),
        "Seguridad" to listOf("security", "ssh", "ids", "wazuh", "vpn"),
        "Operaciones" to listOf("logs", "network", "backup", "full")
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initPrefs(requireContext())
        binding.toggleBackend.check(R.id.btnOpenClaw)

        binding.toggleBackend.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.currentBackend = when (checkedId) {
                    R.id.btnOpenClaw -> "openclaw"
                    R.id.btnClaude -> "claude"
                    else -> "openclaw"
                }
                updateChipsVisibility()
                binding.btnNewSession.visibility = if (viewModel.currentBackend == "claude") View.VISIBLE else View.GONE
            }
        }

        binding.btnNewSession.setOnClickListener {
            viewModel.clearSession()
        }

        // Observe presets from server — build chips dynamically
        viewModel.presets.observe(viewLifecycleOwner) { presets ->
            if (presets.isNotEmpty()) buildPresetChips(presets)
        }

        binding.btnSend.setOnClickListener {
            val text = binding.editMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.editMessage.text?.clear()
            }
        }

        viewModel.messages.observe(viewLifecycleOwner) { messages ->
            binding.messagesLayout.removeAllViews()
            streamingBubbleText = null
            messages.forEach { msg -> addMessageBubble(msg) }
            binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
        }

        viewModel.streamingText.observe(viewLifecycleOwner) { text ->
            if (text != null) {
                if (streamingBubbleText == null) {
                    streamingBubbleText = addStreamingBubble()
                }
                streamingBubbleText?.text = if (text.isEmpty()) "▋" else text
                binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
            } else {
                streamingBubbleText = null
            }
        }

        viewModel.sending.observe(viewLifecycleOwner) { sending ->
            binding.btnSend.isEnabled = !sending
            binding.sendProgress.visibility = if (sending) View.VISIBLE else View.GONE
        }

        viewModel.loadHistory()
        viewModel.loadPresets()
    }

    private fun buildPresetChips(presets: List<AuditPreset>) {
        val root = binding.root as? LinearLayout ?: return
        // Remove previous chips if rebuilding
        root.findViewWithTag<View>("auditChipsContainer")?.let { root.removeView(it) }

        val presetMap = presets.associateBy { it.key }

        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            tag = "auditChipsContainer"
            setPadding(0, 0, 0, 4)
        }

        for ((category, keys) in categoryMap) {
            val categoryPresets = keys.mapNotNull { presetMap[it] }
            if (categoryPresets.isEmpty()) continue

            // Category label
            val label = TextView(requireContext()).apply {
                text = category
                textSize = 11f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.label_color))
                setPadding(12, 6, 0, 0)
            }
            container.addView(label)

            // Chips row (horizontal scroll)
            val chipGroup = ChipGroup(requireContext()).apply { isSingleLine = true }
            for (preset in categoryPresets) {
                val chip = Chip(requireContext()).apply {
                    text = preset.label
                    textSize = 12f
                    isCheckable = false
                    chipMinHeight = 32f
                    setOnClickListener {
                        viewModel.currentBackend = "claude"
                        binding.toggleBackend.check(R.id.btnClaude)
                        viewModel.sendMessage(preset.tag)
                    }
                }
                chipGroup.addView(chip)
            }

            val scrollRow = HorizontalScrollView(requireContext()).apply {
                isHorizontalScrollBarEnabled = false
                setPadding(8, 0, 8, 0)
                addView(chipGroup)
            }
            container.addView(scrollRow)
        }

        // Insert after backend toggle (position 1)
        root.addView(container, 1)
        updateChipsVisibility()
    }

    private fun updateChipsVisibility() {
        val container = (binding.root as? LinearLayout)?.findViewWithTag<View>("auditChipsContainer")
        container?.visibility = if (viewModel.currentBackend == "claude") View.VISIBLE else View.GONE
    }

    private fun addMessageBubble(msg: ChatMessage) {
        val isUser = msg.role == "user"
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(
                    if (isUser) 80 else 16, 4,
                    if (isUser) 16 else 80, 4
                )
            }
            radius = 16f
            setCardBackgroundColor(ContextCompat.getColor(requireContext(),
                if (isUser) R.color.user_bubble else R.color.bot_bubble))
            setContentPadding(16, 12, 16, 12)
        }

        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val backendLabel = TextView(requireContext()).apply {
            text = if (isUser) "Tu" else msg.backend.uppercase()
            textSize = 10f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.label_color))
        }
        layout.addView(backendLabel)

        val content = TextView(requireContext()).apply {
            text = msg.content
            textSize = 14f
        }
        layout.addView(content)

        card.addView(layout)
        binding.messagesLayout.addView(card)
    }

    private fun addStreamingBubble(): TextView {
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(16, 4, 80, 4) }
            radius = 16f
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bot_bubble))
            setContentPadding(16, 12, 16, 12)
        }
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        val label = TextView(requireContext()).apply {
            text = "CLAUDE"
            textSize = 10f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.label_color))
        }
        val content = TextView(requireContext()).apply {
            text = "▋"
            textSize = 14f
        }
        layout.addView(label)
        layout.addView(content)
        card.addView(layout)
        binding.messagesLayout.addView(card)
        return content
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

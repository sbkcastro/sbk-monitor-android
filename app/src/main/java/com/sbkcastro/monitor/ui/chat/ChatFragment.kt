package com.sbkcastro.monitor.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.card.MaterialCardView
import com.sbkcastro.monitor.R
import com.sbkcastro.monitor.api.ChatMessage
import com.sbkcastro.monitor.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toggleBackend.check(R.id.btnOpenClaw)

        binding.toggleBackend.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                viewModel.currentBackend = when (checkedId) {
                    R.id.btnOpenClaw -> "openclaw"
                    R.id.btnClaude -> "claude"
                    else -> "openclaw"
                }
            }
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
            messages.forEach { msg -> addMessageBubble(msg) }
            binding.scrollView.post { binding.scrollView.fullScroll(View.FOCUS_DOWN) }
        }

        viewModel.sending.observe(viewLifecycleOwner) { sending ->
            binding.btnSend.isEnabled = !sending
            binding.sendProgress.visibility = if (sending) View.VISIBLE else View.GONE
        }

        viewModel.loadHistory()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

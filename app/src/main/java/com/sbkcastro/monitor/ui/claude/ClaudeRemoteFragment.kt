package com.sbkcastro.monitor.ui.claude

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.sbkcastro.monitor.api.ClaudeSession
import com.sbkcastro.monitor.databinding.FragmentClaudeRemoteBinding

class ClaudeRemoteFragment : Fragment() {
    private var _binding: FragmentClaudeRemoteBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ClaudeRemoteViewModel by viewModels()

    private val bubbleAdapter = BubbleAdapter()
    private val sessionAdapter = SessionAdapter(
        onTap = { session ->
            AlertDialog.Builder(requireContext())
                .setTitle("Sesión")
                .setMessage("\"${session.firstMessage}\"\nMensajes: ${session.messageCount} | Estado: ${session.status}")
                .setPositiveButton("Continuar") { _, _ -> viewModel.resumeSession(session) }
                .setNegativeButton("Eliminar") { _, _ -> confirmDelete(session) }
                .setNeutralButton("Cancelar", null)
                .show()
        }
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentClaudeRemoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val msgLayoutManager = LinearLayoutManager(context).apply { stackFromEnd = true }
        binding.rvMessages.layoutManager = msgLayoutManager
        binding.rvMessages.adapter = bubbleAdapter

        binding.rvSessions.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSessions.adapter = sessionAdapter

        viewModel.bubbles.observe(viewLifecycleOwner) { list ->
            bubbleAdapter.submitList(list.toList())
            if (list.isNotEmpty()) binding.rvMessages.scrollToPosition(list.size - 1)
        }

        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions.toList())
        }

        viewModel.streamingText.observe(viewLifecycleOwner) { partial ->
            bubbleAdapter.setStreamingText(partial)
            val count = (viewModel.bubbles.value?.size ?: 0) + if (partial != null) 1 else 0
            if (count > 0) binding.rvMessages.scrollToPosition(count - 1)
        }

        viewModel.pendingApproval.observe(viewLifecycleOwner) { ap ->
            if (ap != null) {
                binding.layoutApproval.visibility = View.VISIBLE
                binding.tvApprovalTool.text = "⏳ ${ap.second}\n${ap.third}"
            } else {
                binding.layoutApproval.visibility = View.GONE
            }
        }

        binding.btnNewSession.setOnClickListener { viewModel.newSession() }
        binding.btnApprove.setOnClickListener { viewModel.approve() }
        binding.btnDeny.setOnClickListener { viewModel.deny() }

        binding.btnSend.setOnClickListener { sendMessage() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }

        viewModel.loadSessions()
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return
        binding.etMessage.setText("")
        viewModel.sendMessage(text)
    }

    private fun confirmDelete(session: ClaudeSession) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar sesión")
            .setMessage("¿Borrar \"${session.firstMessage}\"?")
            .setPositiveButton("Borrar") { _, _ -> viewModel.deleteSession(session.jobId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

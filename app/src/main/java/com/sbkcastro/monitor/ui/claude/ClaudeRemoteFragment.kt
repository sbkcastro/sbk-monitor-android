package com.sbkcastro.monitor.ui.claude

import android.app.AlertDialog
import android.graphics.Typeface
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
                .setTitle("session")
                .setMessage("\"${session.firstMessage}\"\n\n${session.messageCount} msgs | ${session.status}")
                .setPositiveButton("resume") { _, _ -> viewModel.resumeSession(session) }
                .setNegativeButton("delete") { _, _ -> confirmDelete(session) }
                .setNeutralButton("cancel", null)
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

        // Observe messages
        viewModel.bubbles.observe(viewLifecycleOwner) { list ->
            bubbleAdapter.submitList(list.toList())
            if (list.isNotEmpty()) binding.rvMessages.scrollToPosition(list.size - 1)
        }

        // Observe sessions
        viewModel.sessions.observe(viewLifecycleOwner) { sessions ->
            sessionAdapter.submitList(sessions.toList())
            updateStatusBar()
        }

        // Observe streaming
        viewModel.streamingText.observe(viewLifecycleOwner) { partial ->
            bubbleAdapter.setStreamingText(partial)
            val count = (viewModel.bubbles.value?.size ?: 0) + if (partial != null) 1 else 0
            if (count > 0) binding.rvMessages.scrollToPosition(count - 1)
            updateStatusBar()
        }

        // Observe pending execute (preview before launching)
        viewModel.pendingExecute.observe(viewLifecycleOwner) { prompt ->
            if (prompt != null) {
                binding.layoutPreview.visibility = View.VISIBLE
                binding.tvPreviewPrompt.text = prompt
            } else {
                binding.layoutPreview.visibility = View.GONE
            }
            updateStatusBar()
        }

        // Observe approval
        viewModel.pendingApproval.observe(viewLifecycleOwner) { ap ->
            if (ap != null) {
                binding.layoutApproval.visibility = View.VISIBLE
                binding.tvApprovalTool.text = "⏳ ${ap.second}\n${ap.third}"
            } else {
                binding.layoutApproval.visibility = View.GONE
            }
            updateStatusBar()
        }

        // Observe active job for status bar
        viewModel.activeJobId.observe(viewLifecycleOwner) { updateStatusBar() }
        viewModel.selectedModelIdx.observe(viewLifecycleOwner) { updateStatusBar() }

        // Observe meta mode
        viewModel.metaMode.observe(viewLifecycleOwner) { updateStatusBar() }
        viewModel.metaThinking.observe(viewLifecycleOwner) { updateStatusBar() }

        // Buttons
        binding.btnNewSession.setOnClickListener { viewModel.newSession() }
        binding.btnPreviewLaunch.setOnClickListener { viewModel.launchPending() }
        binding.btnPreviewCancel.setOnClickListener { viewModel.cancelPending() }
        binding.btnPreviewEdit.setOnClickListener { showEditPromptDialog() }
        binding.btnApprove.setOnClickListener { viewModel.approve() }
        binding.btnDeny.setOnClickListener { viewModel.deny() }
        binding.btnSend.setOnClickListener { sendMessage() }
        binding.tvStatusLabel.setOnClickListener { viewModel.toggleMetaMode() }
        binding.tvStatusSession.setOnClickListener { viewModel.cycleModel() }
        binding.etMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendMessage(); true } else false
        }

        viewModel.loadSessions()
    }

    private fun updateStatusBar() {
        val hasJob = viewModel.activeJobId.value != null
        val isStreaming = viewModel.streamingText.value != null
        val hasPending = viewModel.pendingApproval.value != null
        val msgCount = viewModel.bubbles.value?.size ?: 0

        // Status dot
        val (dot, dotColor) = when {
            hasPending  -> "◉" to 0xFFf0883e.toInt()   // orange
            isStreaming -> "●" to 0xFF3fb950.toInt()    // green
            hasJob      -> "●" to 0xFF58a6ff.toInt()    // cyan
            else        -> "○" to 0xFF8b949e.toInt()    // dim
        }
        binding.tvStatusDot.text = dot
        binding.tvStatusDot.setTextColor(dotColor)

        // Status label — toca para alternar meta mode
        val isMeta = viewModel.metaMode.value == true
        val isMetaThinking = viewModel.metaThinking.value == true
        val label = when {
            isMeta && isMetaThinking -> " 🤖 meta  pensando..."
            isMeta && hasPending     -> " 🤖 meta → claude  approval"
            isMeta && isStreaming    -> " 🤖 meta → claude  streaming"
            isMeta                   -> " 🤖 meta  [toca para desactivar]"
            hasPending               -> " claude  awaiting approval"
            isStreaming              -> " claude  streaming..."
            hasJob                   -> " claude  ↑$msgCount msgs"
            else                     -> " claude  [toca para meta-IA]"
        }
        binding.tvStatusLabel.text = label
        binding.tvStatusLabel.setTextColor(
            if (isMeta) android.graphics.Color.parseColor("#d29922")
            else android.graphics.Color.parseColor("#58a6ff")
        )
        binding.tvStatusLabel.isClickable = true

        // Model + session ID (toca para cambiar modelo)
        val jobId = viewModel.activeJobId.value
        val modelName = ClaudeRemoteViewModel.MODELS[viewModel.selectedModelIdx.value ?: 0].first
        binding.tvStatusSession.text = if (jobId != null) "$modelName·${jobId.take(6)}" else "[$modelName]"
        binding.tvStatusSession.isClickable = true
    }

    private fun sendMessage() {
        val text = binding.etMessage.text?.toString()?.trim() ?: return
        if (text.isEmpty()) return
        binding.etMessage.setText("")
        if (viewModel.metaMode.value == true) {
            viewModel.sendMetaMessage(text)
        } else {
            viewModel.sendMessage(text)
        }
    }

    private fun showEditPromptDialog() {
        val current = viewModel.pendingExecute.value ?: return
        val et = android.widget.EditText(requireContext()).apply {
            setText(current)
            setTextColor(android.graphics.Color.parseColor("#e6edf3"))
            setBackgroundColor(android.graphics.Color.parseColor("#161b22"))
            setPadding(24, 16, 24, 16)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3
            maxLines = 8
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 11f
        }
        AlertDialog.Builder(requireContext())
            .setTitle("editar prompt")
            .setView(et)
            .setPositiveButton("lanzar") { _, _ ->
                val edited = et.text?.toString()?.trim()
                if (!edited.isNullOrEmpty()) viewModel.launchPending(edited)
            }
            .setNegativeButton("cancelar", null)
            .show()
    }

    private fun confirmDelete(session: ClaudeSession) {
        AlertDialog.Builder(requireContext())
            .setTitle("delete session")
            .setMessage("\"${session.firstMessage.take(40)}\"?")
            .setPositiveButton("delete") { _, _ -> viewModel.deleteSession(session.jobId) }
            .setNegativeButton("cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

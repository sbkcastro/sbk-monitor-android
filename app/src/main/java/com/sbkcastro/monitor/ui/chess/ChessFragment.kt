package com.sbkcastro.monitor.ui.chess

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.sbkcastro.monitor.databinding.FragmentChessBinding

class ChessFragment : Fragment() {

    private var _binding: FragmentChessBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val CHESS_URL = "https://chess.sbkcastro.com"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnOpenChess.setOnClickListener {
            CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
                .launchUrl(requireContext(), CHESS_URL.toUri())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}

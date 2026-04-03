package com.sbkcastro.monitor.ui.chess

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.sbkcastro.monitor.databinding.FragmentChessBinding

class ChessFragment : Fragment() {

    private var _binding: FragmentChessBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val CHESS_URL = "https://chess.sbkcastro.com"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChessBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(binding.webView) {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowContentAccess = true
                setSupportZoom(false)
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    // Permite redirects de Google OAuth
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    binding.progressBar.visibility = View.GONE
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }

            webChromeClient = WebChromeClient()

            loadUrl(CHESS_URL)
        }

        binding.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }
    }

    fun canGoBack(): Boolean = binding.webView.canGoBack()

    fun goBack() = binding.webView.goBack()

    override fun onDestroyView() {
        binding.webView.destroy()
        _binding = null
        super.onDestroyView()
    }
}

package com.sbkcastro.monitor.ui.security

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sbkcastro.monitor.R

class FirewallTabFragment : Fragment() {

    private val viewModel: SecurityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_firewall_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeFirewall)
        val tvBanned = view.findViewById<TextView>(R.id.tvBannedCount)
        val tvFailed = view.findViewById<TextView>(R.id.tvTotalFailed)
        val tvInputRules = view.findViewById<TextView>(R.id.tvInputRules)
        val tvForwardRules = view.findViewById<TextView>(R.id.tvForwardRules)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        swipe.setOnRefreshListener { viewModel.loadFirewall() }

        viewModel.loadingFirewall.observe(viewLifecycleOwner) { loading ->
            swipe.isRefreshing = loading
        }

        viewModel.firewall.observe(viewLifecycleOwner) { fw ->
            tvError.visibility = View.GONE
            tvBanned.text = "IPs baneadas (SSH): ${fw.fail2ban.ssh.bannedCount}"
            tvFailed.text = "Intentos fallidos: ${fw.fail2ban.ssh.totalFailed}"
            tvInputRules.text = fw.input.take(2000)
            tvForwardRules.text = fw.forward.take(1500)
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null && viewModel.firewall.value == null) {
                tvError.visibility = View.VISIBLE
                tvError.text = err
            }
        }

        viewModel.loadFirewall()
    }
}

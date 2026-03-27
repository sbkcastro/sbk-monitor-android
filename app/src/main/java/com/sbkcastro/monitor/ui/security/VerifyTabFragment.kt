package com.sbkcastro.monitor.ui.security

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sbkcastro.monitor.R

class VerifyTabFragment : Fragment() {

    private val viewModel: SecurityViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_verify_tab, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val swipe = view.findViewById<SwipeRefreshLayout>(R.id.swipeVerify)
        val containerSites = view.findViewById<LinearLayout>(R.id.containerSites)
        val containerServices = view.findViewById<LinearLayout>(R.id.containerServices)
        val containerLxc = view.findViewById<LinearLayout>(R.id.containerLxc)
        val tvDisk = view.findViewById<TextView>(R.id.tvDisk)
        val tvError = view.findViewById<TextView>(R.id.tvError)

        swipe.setOnRefreshListener { viewModel.loadVerify() }

        viewModel.loadingVerify.observe(viewLifecycleOwner) { loading ->
            swipe.isRefreshing = loading
        }

        viewModel.verify.observe(viewLifecycleOwner) { verify ->
            tvError.visibility = View.GONE

            // Sites
            containerSites.removeAllViews()
            verify.sites.forEach { site ->
                val tv = TextView(requireContext()).apply {
                    text = "${if (site.up) "✅" else "❌"} ${site.name} (${site.status})"
                    setPadding(0, 4, 0, 4)
                    setTextColor(if (site.up) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
                }
                containerSites.addView(tv)
            }

            // Services
            containerServices.removeAllViews()
            verify.services.forEach { svc ->
                val tv = TextView(requireContext()).apply {
                    text = "${if (svc.active) "✅" else "❌"} ${svc.name}: ${svc.status}"
                    setPadding(0, 4, 0, 4)
                    setTextColor(if (svc.active) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
                }
                containerServices.addView(tv)
            }

            // LXC
            containerLxc.removeAllViews()
            verify.lxc.forEach { lxc ->
                val running = lxc.state == "RUNNING"
                val tv = TextView(requireContext()).apply {
                    text = "${if (running) "🟢" else "🔴"} ${lxc.name}: ${lxc.state}"
                    setPadding(0, 4, 0, 4)
                }
                containerLxc.addView(tv)
            }

            // Disk
            tvDisk.text = "Disco /: ${verify.disk.used}/${verify.disk.total} (${verify.disk.usePercent})"
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null && viewModel.verify.value == null) {
                tvError.visibility = View.VISIBLE
                tvError.text = err
            }
        }

        viewModel.loadVerify()
    }
}

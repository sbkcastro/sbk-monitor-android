package com.sbkcastro.monitor.ui.ids

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.sbkcastro.monitor.R

class IDSDashboardFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_ids_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.tvCoveragePercent)?.text = "100%"
        view.findViewById<TextView>(R.id.tvTotalEvents)?.text = "TOTAL: 20,153 eventos"
        view.findViewById<TextView>(R.id.tvAppsEvents)?.text = "19,599 evt"
        view.findViewById<TextView>(R.id.tvHostEvents)?.text = "554 evt"
    }
}

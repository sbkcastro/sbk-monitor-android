package com.sbkcastro.monitor.ui.processes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.sbkcastro.monitor.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProcessesFragment : Fragment() {

    private val viewModel: ProcessesViewModel by viewModels()
    private lateinit var adapter: ProcessAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_processes, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProcessAdapter()

        val recycler = view.findViewById<RecyclerView>(R.id.rvProcesses)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener { viewModel.load() }

        val tvError = view.findViewById<TextView>(R.id.tvError)
        val tvUpdate = view.findViewById<TextView>(R.id.tvLastUpdate)

        viewModel.processes.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            swipeRefresh.isRefreshing = loading
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            tvError.visibility = if (err != null) View.VISIBLE else View.GONE
            tvError.text = err
        }

        viewModel.lastUpdate.observe(viewLifecycleOwner) { ts ->
            if (ts > 0) {
                val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                tvUpdate.text = "Act: ${fmt.format(Date(ts))}"
            }
        }

        viewModel.startAutoRefresh()
    }

    override fun onDestroyView() {
        viewModel.stopAutoRefresh()
        super.onDestroyView()
    }
}

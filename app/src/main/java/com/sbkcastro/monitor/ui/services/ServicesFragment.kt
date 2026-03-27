package com.sbkcastro.monitor.ui.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sbkcastro.monitor.databinding.FragmentServicesBinding

class ServicesFragment : Fragment() {

    private var _binding: FragmentServicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ServicesViewModel
    private lateinit var adapter: ServicesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentServicesBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[ServicesViewModel::class.java]
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ServicesAdapter()
        binding.recyclerServices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ServicesFragment.adapter
        }

        viewModel.telemetry.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                binding.tvSummary.text =
                    "online: ${data.online}/${data.total}  |  activos: ${data.totalActive}  |  views 24h: ${data.totalPageviews24h}"
                adapter.submitList(data.sites)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) binding.tvSummary.text = "error: $err"
        }

        binding.fabRefresh.setOnClickListener { viewModel.load() }
        viewModel.load()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

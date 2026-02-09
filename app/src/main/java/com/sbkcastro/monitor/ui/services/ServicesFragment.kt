package com.sbkcastro.monitor.ui.services

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.sbkcastro.monitor.R
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

        setupRecyclerView()
        observeData()

        // Botón de actualizar
        binding.fabRefresh.setOnClickListener {
            viewModel.checkAllServices()
        }

        // Fetch inicial
        viewModel.checkAllServices()
    }

    private fun setupRecyclerView() {
        adapter = ServicesAdapter()
        binding.recyclerServices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ServicesFragment.adapter
        }
    }

    private fun observeData() {
        viewModel.services.observe(viewLifecycleOwner) { services ->
            adapter.submitList(services)
            binding.progressBar.visibility = View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

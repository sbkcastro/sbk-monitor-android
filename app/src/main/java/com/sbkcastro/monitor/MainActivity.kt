package com.sbkcastro.monitor

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.tabs.TabLayout
import com.sbkcastro.monitor.api.ApiClient
import com.sbkcastro.monitor.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Todas las pestañas — añadir aquí para extender
    private val tabs = listOf(
        Tab("📊", "Gráficos",  R.id.chartsProfessionalFragment),
        Tab("🖥️", "LXC",       R.id.lxcManagementFragment),
        Tab("💹", "Trade",     R.id.cryptoFragment),
        Tab("🛡️", "Seguridad", R.id.securityFragment),
        Tab("🤖", "Claude",    R.id.claudeRemoteFragment),
        Tab("💬", "Chat",      R.id.chatFragment),
        Tab("⚙️", "Servicios", R.id.servicesFragment),
        Tab("🔧", "Procesos",  R.id.processesFragment),
        Tab("⚙", "Config",    R.id.settingsFragment),
        Tab("♟", "Chess",     R.id.chessFragment)
    )

    private data class Tab(val icon: String, val label: String, val destId: Int)

    private var navigatingFromTab = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "SBK Monitor"

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Crear tabs
        tabs.forEach { tab ->
            binding.bottomNav.addTab(
                binding.bottomNav.newTab().setText("${tab.icon} ${tab.label}")
            )
        }

        // Tab tap → navegar
        binding.bottomNav.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val dest = tabs[tab.position].destId
                navigatingFromTab = true
                try {
                    navController.navigate(dest)
                } catch (_: Exception) {
                    navController.popBackStack(dest, false)
                }
                navigatingFromTab = false
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // NavController → sincronizar tab seleccionado
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (!navigatingFromTab) {
                val idx = tabs.indexOfFirst { it.destId == destination.id }
                if (idx >= 0 && binding.bottomNav.selectedTabPosition != idx) {
                    binding.bottomNav.selectTab(binding.bottomNav.getTabAt(idx))
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                ApiClient.clearAuth()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

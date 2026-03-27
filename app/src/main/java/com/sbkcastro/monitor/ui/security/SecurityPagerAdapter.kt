package com.sbkcastro.monitor.ui.security

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.sbkcastro.monitor.ui.ids.IDSDashboardFragment
import com.sbkcastro.monitor.ui.wazuh.WazuhFragment

class SecurityPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> WazuhFragment()
        1 -> IDSDashboardFragment()
        2 -> FirewallTabFragment()
        3 -> VerifyTabFragment()
        else -> WazuhFragment()
    }
}

package com.sbkcastro.monitor.ui.crypto

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class CryptoPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount() = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> CryptoDashboardTab()
        1 -> CryptoTradesTab()
        2 -> CryptoSignalsTab()
        3 -> CryptoWorldmonitorTab()
        else -> CryptoDashboardTab()
    }
}

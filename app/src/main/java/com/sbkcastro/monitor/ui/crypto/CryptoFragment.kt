package com.sbkcastro.monitor.ui.crypto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.sbkcastro.monitor.R

class CryptoFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_crypto, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerCrypto)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutCrypto)

        viewPager.adapter = CryptoPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Dashboard"
                1 -> "Trades"
                2 -> "Signals"
                3 -> "AI Monitor"
                else -> ""
            }
        }.attach()
    }
}

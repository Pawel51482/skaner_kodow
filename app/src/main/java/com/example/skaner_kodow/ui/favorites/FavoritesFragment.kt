package com.example.skaner_kodow.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.skaner_kodow.databinding.FragmentFavoritesBinding
import com.google.android.material.tabs.TabLayoutMediator

class FavoritesFragment : Fragment() {

    private lateinit var binding: FragmentFavoritesBinding
    private lateinit var favViewModel: FavoritesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        favViewModel = ViewModelProvider(requireActivity()).get(FavoritesViewModel::class.java)

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 2
            override fun createFragment(position: Int): Fragment =
                if (position == 0) FavoritesProductsFragment() else FavoritesPromotionsFragment()
        }

        val mediator = TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = if (pos == 0) "Produkty" else "Promocje"
        }
        mediator.attach()

        favViewModel.favProducts.observe(viewLifecycleOwner) { prods ->
            val p = prods.size
            binding.tabLayout.getTabAt(0)?.text = "Produkty ($p)"
        }
        favViewModel.favPromotions.observe(viewLifecycleOwner) { promos ->
            val r = promos.size
            binding.tabLayout.getTabAt(1)?.text = "Promocje ($r)"
        }

        return binding.root
    }
}

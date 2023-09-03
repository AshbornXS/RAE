package com.rae.daply.fragment.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentPageAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    // Define o número total de fragmentos a serem exibidos no ViewPager
    override fun getItemCount(): Int {
        return 3
    }

    // Cria e retorna o fragmento para uma posição específica no ViewPager
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                ClassesTimeFragment() // Primeira posição mostra o fragmento ClassesTimeFragment
            }

            1 -> {
                HomeFragment() // Segunda posição mostra o fragmento HomeFragment
            }

            else -> {
                ExclusiveFragment() // Todas as outras posições mostram o fragmento ExclusiveFragment
            }
        }
    }
}

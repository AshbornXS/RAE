package com.rae.daply.fragment.ui.login

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class FragmentLoginAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
    FragmentStateAdapter(fragmentManager, lifecycle) {

    // Define o número total de fragmentos a serem exibidos no ViewPager
    override fun getItemCount(): Int {
        return 2
    }

    // Cria e retorna o fragmento para uma posição específica no ViewPager
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                LoginFragment() // Primeira posição mostra o fragmento LoginFragment
            }

            else -> {
                SignupFragment() // Segunda posição mostra o fragmento SignupFragment
            }
        }
    }
}

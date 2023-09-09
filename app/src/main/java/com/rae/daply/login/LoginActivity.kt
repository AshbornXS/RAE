package com.rae.daply.login

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.rae.daply.MainActivity
import com.rae.daply.databinding.ActivityLoginBinding
import com.rae.daply.fragment.ui.login.FragmentLoginAdapter
import com.rae.daply.fragment.ui.login.LoginFragment
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var adapter: FragmentLoginAdapter

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar o layout da atividade usando o binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Instalar a splash screen
        installSplashScreen()

        // Obter referência para as preferências compartilhadas
        val sharedPreferences = getSharedPreferences("shared_prefs", MODE_PRIVATE)

        // Verificar permissões em uma coroutine no escopo GlobalScope
        GlobalScope.launch {
            // Determinar permissões com base na versão do SDK
            val perms = if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                )
            } else {
                arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
            }
            checkPermission(perms)
        }

        val tabLayout = binding.tabLayout

        // Configurar o adaptador para o ViewPager
        adapter = FragmentLoginAdapter(supportFragmentManager, lifecycle)
        binding.viewPager2.adapter = adapter

        // Adicionar abas ao TabLayout
        tabLayout.addTab(tabLayout.newTab().setText("Login"))
        tabLayout.addTab(tabLayout.newTab().setText("Signup"))

        // Definir a primeira aba como selecionada
        tabLayout.selectTab(tabLayout.getTabAt(0))

        // Adicionar listener para trocar de fragmento quando a aba é selecionada
        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.viewPager2.currentItem = tab!!.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Nada a fazer quando uma aba é desselecionada
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Nada a fazer quando uma aba é reselecionada
            }
        })

        // Adicionar callback para atualizar a aba quando a página é alterada
        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

        // Configurar o modo noturno com base nas preferências
        val isDarkTheme = sharedPreferences.getBoolean("isDarkTheme", false)
        val isAuto = sharedPreferences.getBoolean("isAutoTheme", false)

        if (isAuto) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        // Verificar se o usuário está logado e redirecionar para a MainActivity
        val isLogged = sharedPreferences.getBoolean("isLogged", false)
        if (isLogged) {
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Função para verificar e solicitar permissões
    private fun checkPermission(permission: Array<String>) {
        for (perm in permission) {
            if (ActivityCompat.checkSelfPermission(
                    this, perm
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(this, permission, 0)
                break
            }
        }
    }
}

package com.rae.daply

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.data.UpdateProfileActivity
import com.rae.daply.data.UploadActivity
import com.rae.daply.data.UploadTimeActivity
import com.rae.daply.databinding.ActivityMainBinding
import com.rae.daply.fragment.ui.FragmentPageAdapter
import com.rae.daply.login.LoginActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FragmentPageAdapter

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("shared_prefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // Verificar tipo de usuário e definir visibilidade do FAB
        GlobalScope.launch(Dispatchers.Main) {
            val save = FirebaseAuth.getInstance().currentUser?.email?.replace("@etec.sp.gov.br", "")
                ?.replace(".", "-").toString()

            editor.putString("save", save)
            editor.apply()

            FirebaseDatabase.getInstance().reference.child("Users").child(save).get()
                .addOnSuccessListener { snapshot ->
                    val serie = snapshot.child("serie").value.toString()
                    val curso = snapshot.child("curso").value.toString()
                    val periodo = snapshot.child("periodo").value.toString()
                    val profileImage = snapshot.child("profileImage").value.toString()

                    if (profileImage != "null") {
                        Glide.with(this@MainActivity).load(profileImage).into(binding.pfp)
                    }

                    val classe = serie.take(1) + "-" + curso + "-" + periodo.take(1)

                    editor.putString("classe", classe)
                    editor.apply()
                }

            val userType = withContext(Dispatchers.IO) {
                val dbReference = FirebaseDatabase.getInstance()
                dbReference.reference.child("Users").child(save).child("userType").get()
                    .await().value.toString()
            }

            editor.putString("userType", userType)
            editor.apply()

            if (userType == "admin") {
                binding.fab.visibility = View.VISIBLE
            }
        }

        val tabLayout = binding.tabLayout

        adapter = FragmentPageAdapter(supportFragmentManager, lifecycle)

        // Configurar abas e ViewPager
        setupTabsAndPager(tabLayout)

        // Configurar tema noturno
        themeSwitch()

        // Configurar clique do FAB
        setupFabClick()

        val pfpBuilder = AlertDialog.Builder(this)
        val pfpView = layoutInflater.inflate(R.layout.dialog_profile, null)
        pfpBuilder.setCancelable(true)
        pfpBuilder.setView(pfpView)
        val pfpDialog = pfpBuilder.create()

        val pfpImage = pfpView.findViewById<ImageView>(R.id.pfpImage)

        val activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val uri = data?.data!!
                pfpImage.setImageURI(uri)
                uploadProfile(uri)
            } else {
                Toast.makeText(
                    this, "Nenhuma imagem selecionada!", Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Define o listener de clique para o campo de imagem
        pfpImage.setOnClickListener {
            val photoPicker = Intent(Intent.ACTION_PICK)
            photoPicker.type = "image/*"
            activityResultLauncher.launch(photoPicker)
        }

        // Configurar clique da foto de perfil
        binding.pfp.setOnClickListener {
            showProfileDialog(pfpView, pfpDialog, pfpImage)
        }
    }

    private fun uploadProfile(uri: Uri) {
        val save = sharedPreferences.getString("save", null).toString()

        val storageReference: StorageReference =
            FirebaseStorage.getInstance().reference.child("Profile Images").child(save)

        storageReference.putFile(uri).addOnSuccessListener {
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                val url = uri.toString()
                FirebaseDatabase.getInstance().reference.child("Users").child(save)
                    .child("profileImage").setValue(url)
            }
        }
    }

    private fun setupTabsAndPager(tabLayout: TabLayout) {
        tabLayout.addTab(tabLayout.newTab().setText("Aulas").setIcon(R.drawable.ic_time_24))
        tabLayout.addTab(tabLayout.newTab().setText("Geral").setIcon(R.drawable.ic_home_24))
        tabLayout.addTab(tabLayout.newTab().setText("Sala").setIcon(R.drawable.ic_notifications_24))

        binding.viewPager2.adapter = adapter
        binding.viewPager2.currentItem = 1
        tabLayout.selectTab(tabLayout.getTabAt(1))
        tabLayout.getTabAt(1)?.icon?.colorFilter = android.graphics.PorterDuffColorFilter(
            resources.getColor(R.color.reverse_text, theme), android.graphics.PorterDuff.Mode.SRC_IN
        )

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.viewPager2.currentItem = tab!!.position
                tab.icon?.colorFilter = android.graphics.PorterDuffColorFilter(
                    resources.getColor(R.color.reverse_text, theme),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab!!.icon?.colorFilter = android.graphics.PorterDuffColorFilter(
                    resources.getColor(R.color.text, theme), android.graphics.PorterDuff.Mode.SRC_IN
                )
                tabLayout.getTabAt(tab.position)?.orCreateBadge?.isVisible = false
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Nada a fazer quando uma aba é reselecionada
            }
        })

        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })
    }

    private fun Context.isDarkThemeOn(): Boolean {
        editor.putBoolean("isDarkTheme", true)
        return resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    }

    private fun themeSwitch() {
        val isDarkTheme = sharedPreferences.getBoolean("isDarkTheme", false)
        val isAuto = sharedPreferences.getBoolean("isAutoTheme", true)

        if (isAuto) {
            binding.themeSwitcher.setImageResource(R.drawable.ic_auto_24)
            binding.themeSwitcher.colorFilter = android.graphics.PorterDuffColorFilter(
                resources.getColor(R.color.text, theme), android.graphics.PorterDuff.Mode.SRC_IN
            )
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        } else {
            if (isDarkTheme) {
                binding.themeSwitcher.setImageResource(R.drawable.ic_night_24)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                binding.themeSwitcher.setImageResource(R.drawable.ic_light_24)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        binding.themeSwitcher.setOnClickListener {
            if (isDarkTheme) {
                binding.themeSwitcher.setImageResource(R.drawable.ic_light_24)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor.putBoolean("isDarkTheme", false)
            } else {
                binding.themeSwitcher.setImageResource(R.drawable.ic_night_24)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor.putBoolean("isDarkTheme", true)
            }
            editor.putBoolean("isAutoTheme", false)
            editor.apply()
        }

        binding.themeSwitcher.setOnLongClickListener {
            binding.themeSwitcher.setImageResource(R.drawable.ic_auto_24)
            binding.themeSwitcher.colorFilter = android.graphics.PorterDuffColorFilter(
                resources.getColor(R.color.text, theme), android.graphics.PorterDuff.Mode.SRC_IN
            )
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            editor.putBoolean("isAutoTheme", true)
            if (isDarkThemeOn()) {
                editor.putBoolean("isDarkTheme", true)
            } else {
                editor.putBoolean("isDarkTheme", false)
            }
            editor.apply()
            true
        }
    }

    private fun setupFabClick() {
        binding.fab.setOnClickListener {
            if (binding.viewPager2.currentItem == 0) {
                val intent = Intent(this, UploadTimeActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, UploadActivity::class.java).putExtra(
                    "position", binding.viewPager2.currentItem
                )
                startActivity(intent)
            }
        }
    }

    private fun showProfileDialog(pfpView: View, pfpDialog: AlertDialog, pfpImage: ImageView) {

        pfpDialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val labelName = pfpView.findViewById<TextView>(R.id.pfpName)
        val labelEmail = pfpView.findViewById<TextView>(R.id.pfpEmail)
        val labelSerie = pfpView.findViewById<TextView>(R.id.pfpSerie)
        val labelCurso = pfpView.findViewById<TextView>(R.id.pfpCurso)
        val labelPeriodo = pfpView.findViewById<TextView>(R.id.pfpPeriodo)

        fillProfileInfo(labelName, labelEmail, labelSerie, labelCurso, labelPeriodo, pfpImage)

        pfpDialog.show()

        pfpDialog.findViewById<Button>(R.id.pfpUpdate)?.setOnClickListener {
            val intent = Intent(this, UpdateProfileActivity::class.java).putExtra(
                "name", labelName.text.toString()
            ).putExtra("email", labelEmail.text.toString())
                .putExtra("periodo", labelPeriodo.text.toString())
                .putExtra("serie", labelSerie.text.toString())
                .putExtra("curso", labelCurso.text.toString())
            startActivity(intent)
            pfpDialog.dismiss()
        }

        pfpDialog.findViewById<Button>(R.id.pfpExit)?.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            editor.putBoolean("isLogged", false)
            editor.apply()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            pfpDialog.dismiss()
            this.finishAffinity()
        }
    }

    private fun fillProfileInfo(
        labelName: TextView,
        labelEmail: TextView,
        labelSerie: TextView,
        labelCurso: TextView,
        labelPeriodo: TextView,
        pfpImage: ImageView
    ) {
        val save = sharedPreferences.getString("save", null).toString()

        FirebaseDatabase.getInstance().reference.child("Users").child(save).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").value.toString()
                val email = snapshot.child("email").value.toString()
                val periodo = snapshot.child("periodo").value.toString()
                val serie = snapshot.child("serie").value.toString()
                val curso = snapshot.child("curso").value.toString()
                val profileImage = snapshot.child("profileImage").value.toString()

                labelName.text = name
                labelEmail.text = email
                labelSerie.text = serie
                labelCurso.text = curso
                labelPeriodo.text = periodo
                Glide.with(this).load(profileImage).into(pfpImage)
            }
    }
}

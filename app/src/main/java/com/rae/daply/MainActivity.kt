package com.rae.daply

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

        GlobalScope.launch(Dispatchers.Main) {
            val save = sharedPreferences.getString(
                "save", null
            ).toString()

            val userType = withContext(Dispatchers.IO) {
                val dbReference = FirebaseDatabase.getInstance()
                dbReference.reference.child("Users").child(save).child("userType").get()
                    .await().value.toString()
            }

            editor.putString("userType", userType)
            editor.apply()

            if (userType != "admin") {
                binding.fab.visibility = View.GONE
            }
        }

        val tabLayout = binding.tabLayout

        adapter = FragmentPageAdapter(supportFragmentManager, lifecycle)

        tabLayout.addTab(tabLayout.newTab().setText("Aulas").setIcon(R.drawable.ic_time_24))
        tabLayout.addTab(tabLayout.newTab().setText("Home").setIcon(R.drawable.ic_home_24))
        tabLayout.addTab(tabLayout.newTab().setText("Sala").setIcon(R.drawable.ic_notifications_24))

        binding.viewPager2.adapter = adapter
        binding.viewPager2.currentItem = 1
        tabLayout.selectTab(tabLayout.getTabAt(1))

        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                binding.viewPager2.currentItem = tab!!.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }
        })

        binding.viewPager2.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })

        val nightMode = sharedPreferences.getBoolean("nightMode", false)

        if (nightMode) {
            binding.themeSwitch.isChecked = true
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        binding.themeSwitch.setOnClickListener {
            if (nightMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                editor = sharedPreferences.edit()
                editor.putBoolean("nightMode", false)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                editor = sharedPreferences.edit()
                editor.putBoolean("nightMode", true)
            }
            editor.apply()
        }

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

        binding.pfp.setOnClickListener {
            showProfileDialog()
        }

    }

    private fun showProfileDialog() {
        val pfpBuilder = AlertDialog.Builder(this)
        val pfpView = layoutInflater.inflate(R.layout.dialog_profile, null)
        pfpBuilder.setView(pfpView)
        val pfpDialog = pfpBuilder.create()

        val labelName = pfpView.findViewById<TextView>(R.id.pfpName)
        val labelEmail = pfpView.findViewById<TextView>(R.id.pfpEmail)
        val labelSerie = pfpView.findViewById<TextView>(R.id.pfpSerie)
        val labelCurso = pfpView.findViewById<TextView>(R.id.pfpCurso)
        val labelPeriodo = pfpView.findViewById<TextView>(R.id.pfpPeriodo)

        fillProfileInfo(labelName, labelEmail, labelSerie, labelCurso, labelPeriodo)

        pfpDialog.show()

        pfpDialog.findViewById<Button>(R.id.pfpClose)?.setOnClickListener {
            pfpDialog.dismiss()
        }

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
        labelPeriodo: TextView
    ) {
        val save = getSharedPreferences("shared_prefs", MODE_PRIVATE).getString(
            "save", null
        ).toString()

        FirebaseDatabase.getInstance().reference.child("Users").child(save).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.child("name").value.toString()
                val email = snapshot.child("email").value.toString()
                val periodo = snapshot.child("periodo").value.toString()
                val serie = snapshot.child("serie").value.toString()
                val curso = snapshot.child("curso").value.toString()

                labelName.text = "${labelName.text}$name"
                labelEmail.text = "${labelEmail.text}$email"
                labelSerie.text = "${labelSerie.text}$serie"
                labelCurso.text = "${labelCurso.text}$curso"
                labelPeriodo.text = "${labelPeriodo.text}$periodo"
            }
    }
}
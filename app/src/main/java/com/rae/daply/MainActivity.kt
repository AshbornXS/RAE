package com.rae.daply

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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
import com.rae.daply.utils.curso
import com.rae.daply.utils.email
import com.rae.daply.utils.name
import com.rae.daply.utils.periodo
import com.rae.daply.utils.save
import com.rae.daply.utils.serie
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


open class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: FragmentPageAdapter

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        GlobalScope.launch {
            val userType = withContext(Dispatchers.IO) {
                val dbReference = FirebaseDatabase.getInstance()
                dbReference.reference.child("Users").child(save).child("userType").get()
                    .await().value.toString()
            }

            com.rae.daply.utils.userType = userType

            if (com.rae.daply.utils.userType != "admin") {
                binding.fab.visibility = View.GONE
            }
        }

        val tabLayout = binding.tabLayout

        adapter = FragmentPageAdapter(supportFragmentManager, lifecycle)

        tabLayout.addTab(tabLayout.newTab().setText("Horários").setIcon(R.drawable.ic_time_24))
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

        binding.fab.setOnClickListener {
            if (binding.viewPager2.currentItem == 0) {
                val intent = Intent(this, UploadTimeActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, UploadActivity::class.java)
                startActivity(intent)
            }
        }

        binding.pfp.setOnClickListener {
            val pfpBuilder = AlertDialog.Builder(this)
            val pfpView = layoutInflater.inflate(R.layout.dialog_profile, null)
            pfpBuilder.setView(pfpView)
            val pfpDialog = pfpBuilder.create()

            FirebaseDatabase.getInstance().reference.child("Users").child(save).get()
                .addOnSuccessListener { snapshot ->
                    val labelName = pfpView.findViewById<TextView>(R.id.pfpName)
                    val labelEmail = pfpView.findViewById<TextView>(R.id.pfpEmail)
                    val labelSerie = pfpView.findViewById<TextView>(R.id.pfpSerie)
                    val labelCurso = pfpView.findViewById<TextView>(R.id.pfpCurso)
                    val labelPeriodo = pfpView.findViewById<TextView>(R.id.pfpPeriodo)

                    name = snapshot.child("name").value.toString()
                    email = snapshot.child("email").value.toString()
                    periodo = snapshot.child("periodo").value.toString()
                    serie = snapshot.child("serie").value.toString()
                    curso = snapshot.child("curso").value.toString()

                    labelName.text = "${labelName.text}${snapshot.child("name").value}"
                    labelEmail.text = "${labelEmail.text}${snapshot.child("email").value}"
                    labelSerie.text = "${labelSerie.text}${snapshot.child("serie").value}"
                    labelCurso.text = "${labelCurso.text}${snapshot.child("curso").value}"
                    labelPeriodo.text = "${labelPeriodo.text}${snapshot.child("periodo").value}"
                }

            pfpDialog.show()

            pfpDialog.findViewById<Button>(R.id.pfpClose)?.setOnClickListener {
                pfpDialog.dismiss()
            }

            pfpDialog.findViewById<Button>(R.id.pfpUpdate)?.setOnClickListener {
                val intent = Intent(this, UpdateProfileActivity::class.java).putExtra(
                    "name", name
                ).putExtra("email", email).putExtra("periodo", periodo).putExtra("serie", serie)
                    .putExtra("curso", curso)
                startActivity(intent)
                pfpDialog.dismiss()
            }

            pfpDialog.findViewById<Button>(R.id.pfpExit)?.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                pfpDialog.dismiss()
                this.finishAffinity()
            }
        }

    }
}
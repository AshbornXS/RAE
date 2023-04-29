package com.rae.daply

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rae.daply.data.DataClass
import com.rae.daply.data.MyAdaptor
import com.rae.daply.data.UploadActivity
import com.rae.daply.databinding.ActivityMainBinding
import com.rae.daply.login.LoginActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class MainActivity : AppCompatActivity() {

    private var notificationWorkManager: NotificationManager? = null
    private var isFirstUpdate = true
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fab: View = binding.fab
        val recyclerView: RecyclerView = binding.recyclerView

        GlobalScope.launch(Dispatchers.Main) {
            val save = FirebaseAuth.getInstance().currentUser?.email?.replace("@etec.sp.gov.br", "")
                ?.replace(".", "-")

            val dbReference = FirebaseDatabase.getInstance()
            dbReference.reference.child("Users").child(save.toString()).child("userType").get()
                .addOnSuccessListener {
                    val userType = it.value.toString()
                    if (userType != "admin") {
                        fab.visibility = View.GONE
                    }
                }
        }

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.loading_layout)
        val dialog: AlertDialog = builder.create()
        dialog.show()

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.reverseLayout = true
        recyclerView.layoutManager = linearLayoutManager

        val avisosArrayList: List<DataClass> = ArrayList()

        val adapter = MyAdaptor(this, avisosArrayList as ArrayList<DataClass>)
        recyclerView.adapter = adapter

        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("RAE")

        databaseReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                avisosArrayList.clear()
                for (itemSnapshot in snapshot.children) {
                    val dataClass = itemSnapshot.getValue(DataClass::class.java)
                    dataClass!!.key = itemSnapshot.key
                    avisosArrayList.add(dataClass)
                }
                if (!isFirstUpdate) {
                    sendNotification()

                }
                isFirstUpdate = false
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                dialog.dismiss()
            }
        })

        fab.setOnClickListener {
            val intent = Intent(this, UploadActivity::class.java)
            startActivity(intent)
        }

        binding.pfp.setOnClickListener {
            val pfpBuilder = AlertDialog.Builder(this)
            val view = layoutInflater.inflate(R.layout.dialog_profile, null)
            pfpBuilder.setView(view)
            val pfpDialog = pfpBuilder.create()

            val save = FirebaseAuth.getInstance().currentUser?.email?.replace("@etec.sp.gov.br", "")
                ?.replace(".", "-")

            FirebaseDatabase.getInstance().reference.child("Users").child(save.toString()).get()
                .addOnSuccessListener { snapshot ->
                    val labelName = view?.findViewById<TextView>(R.id.pfpName)
                    val labelEmail = view?.findViewById<TextView>(R.id.pfpEmail)
                    val labelSerie = view?.findViewById<TextView>(R.id.pfpSerie)
                    val labelCurso = view?.findViewById<TextView>(R.id.pfpCurso)

                    labelName?.text = "${labelName?.text}${snapshot.child("name").value}"
                    labelEmail?.text = "${labelEmail?.text}${snapshot.child("email").value}"
                    labelSerie?.text = "${labelSerie?.text}${snapshot.child("serie").value}"
                    labelCurso?.text = "${labelCurso?.text}${snapshot.child("curso").value}"
                }

            pfpDialog.show()

            pfpDialog.findViewById<Button>(R.id.pfpClose)?.setOnClickListener {
                pfpDialog.dismiss()
            }

            pfpDialog.findViewById<Button>(R.id.pfpExit)?.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith("finishAffinity()"))
    override fun onBackPressed() {
        finishAffinity()
    }

    private fun createChannel() {
        notificationWorkManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "primary_notification_channel", "Avisos", NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Notificação de avisos."

            notificationWorkManager?.createNotificationChannel(notificationChannel)
        }
    }

    private fun sendNotification() {
        createChannel()

        val notifyIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "primary_notification_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("ATENÇÃO!!!")
            .setContentText("Um novo aviso foi postado.")
            .setPriority(NotificationCompat.PRIORITY_MAX).setContentIntent(notifyPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL).setAutoCancel(true)

        notificationWorkManager?.notify(0, notificationBuilder.build())
    }
}
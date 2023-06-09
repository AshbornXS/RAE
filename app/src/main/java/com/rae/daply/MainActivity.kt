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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.data.DataClass
import com.rae.daply.data.MyAdapter
import com.rae.daply.data.UploadActivity
import com.rae.daply.data.UpdateProfileActivity
import com.rae.daply.databinding.ActivityMainBinding
import com.rae.daply.login.LoginActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.collections.ArrayList
import com.rae.daply.utils.userType as userTypeGlobal
import com.rae.daply.utils.name as nameGlobal
import com.rae.daply.utils.email as emailGlobal
import com.rae.daply.utils.serie as serieGlobal
import com.rae.daply.utils.curso as cursoGlobal
import com.rae.daply.utils.save


open class MainActivity : AppCompatActivity() {

    private lateinit var notificationWorkManager: NotificationManager
    private var isFirstUpdate = true
    private lateinit var binding: ActivityMainBinding
    private val currentDate = System.currentTimeMillis()

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fab: View = binding.fab
        val recyclerView: RecyclerView = binding.recyclerView

        GlobalScope.launch(Dispatchers.Main) {
            val userType = withContext(Dispatchers.IO) {
                val dbReference = FirebaseDatabase.getInstance()
                dbReference.reference.child("Users").child(save).child("userType").get()
                    .await().value.toString()
            }

            userTypeGlobal = userType

            if (userType != "admin") {
                fab.visibility = View.GONE
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

        val avisosArrayList: ArrayList<DataClass> = ArrayList()

        val adapter = MyAdapter(this, avisosArrayList)
        recyclerView.adapter = adapter

        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("RAE")

        databaseReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                GlobalScope.launch(Dispatchers.Main) {
                    avisosArrayList.clear()
                    for (itemSnapshot in snapshot.children) {
                        val dataClass = itemSnapshot.getValue(DataClass::class.java)
                        dataClass!!.key = itemSnapshot.key

                        val image = dataClass.imageURL
                        val key = dataClass.key
                        val uploadDate = dataClass.dataMili

                        if ((uploadDate != null) && (image != null) && (key != null)) {
                            val daysPassed = (currentDate - uploadDate) / (1000 * 60 * 60 * 24)
                            if (daysPassed >= 3) {
                                withContext(Dispatchers.IO) {
                                    val reference: DatabaseReference =
                                        FirebaseDatabase.getInstance().getReference("RAE")
                                    val storage: FirebaseStorage = FirebaseStorage.getInstance()
                                    val storageReference: StorageReference =
                                        storage.getReferenceFromUrl(image)
                                    storageReference.delete().await()
                                    reference.child(key).removeValue().await()
                                }
                                itemSnapshot.ref.removeValue()
                            }
                        }
                        avisosArrayList.add(dataClass)
                    }
                    if (!isFirstUpdate) {
                        sendNotification()
                    }
                    isFirstUpdate = false
                    adapter.notifyDataSetChanged()
                    dialog.dismiss()
                }
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

            FirebaseDatabase.getInstance().reference.child("Users").child(save).get()
                .addOnSuccessListener { snapshot ->
                    val labelName = view?.findViewById<TextView>(R.id.pfpName)
                    val labelEmail = view?.findViewById<TextView>(R.id.pfpEmail)
                    val labelSerie = view?.findViewById<TextView>(R.id.pfpSerie)
                    val labelCurso = view?.findViewById<TextView>(R.id.pfpCurso)

                    nameGlobal = snapshot.child("name").value.toString()
                    emailGlobal = snapshot.child("email").value.toString()
                    serieGlobal = snapshot.child("serie").value.toString()
                    cursoGlobal = snapshot.child("curso").value.toString()

                    labelName?.text = "${labelName?.text}${snapshot.child("name").value}"
                    labelEmail?.text = "${labelEmail?.text}${snapshot.child("email").value}"
                    labelSerie?.text = "${labelSerie?.text}${snapshot.child("serie").value}"
                    labelCurso?.text = "${labelCurso?.text}${snapshot.child("curso").value}"
                }

            pfpDialog.show()

            pfpDialog.findViewById<Button>(R.id.pfpClose)?.setOnClickListener {
                pfpDialog.dismiss()
            }

            pfpDialog.findViewById<Button>(R.id.pfpUpdate)?.setOnClickListener {
                val intent = Intent(this, UpdateProfileActivity::class.java).putExtra("name", nameGlobal)
                    .putExtra("email", emailGlobal).putExtra("serie", serieGlobal).putExtra("curso", cursoGlobal)
                startActivity(intent)
                pfpDialog.dismiss()
            }

            pfpDialog.findViewById<Button>(R.id.pfpExit)?.setOnClickListener {
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }
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

            notificationWorkManager.createNotificationChannel(notificationChannel)
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

        notificationWorkManager.notify(0, notificationBuilder.build())
    }
}
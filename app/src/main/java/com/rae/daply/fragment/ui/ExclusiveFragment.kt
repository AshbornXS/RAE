package com.rae.daply.fragment.ui

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.data.DataClass
import com.rae.daply.data.MyAdapter
import com.rae.daply.databinding.FragmentExclusiveBinding
import com.rae.daply.utils.curso
import com.rae.daply.utils.periodo
import com.rae.daply.utils.serie
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ExclusiveFragment : Fragment() {

    private lateinit var notificationWorkManager: NotificationManager
    private var isFirstUpdate = true
    private lateinit var binding: FragmentExclusiveBinding
    private val currentDate = System.currentTimeMillis()

    private lateinit var mContext: Context
    private lateinit var activity: MainActivity

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentExclusiveBinding.inflate(inflater, container, false)
        val view = binding.root

        sharedPreferences =
            activity.getSharedPreferences("shared_prefs", AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val save = sharedPreferences.getString(
            "save", null
        ).toString()

        FirebaseDatabase.getInstance().reference.child("Users").child(save).get()
            .addOnSuccessListener { snapshot ->
                serie = snapshot.child("serie").value.toString()
                curso = snapshot.child("curso").value.toString()
                periodo = snapshot.child("periodo").value.toString()

                val classe = serie.take(1) + "-" + curso + "-" + periodo.take(1)

                editor.putString("classe", classe)
                editor.apply()

                val recyclerView: RecyclerView = binding.recyclerView

                binding.dataViewExclusive.visibility = View.GONE
                binding.shimmerViewExclusive.startShimmer()

                val linearLayoutManager = LinearLayoutManager(requireContext())
                linearLayoutManager.stackFromEnd = true
                linearLayoutManager.reverseLayout = true
                recyclerView.layoutManager = linearLayoutManager

                val avisosArrayList: ArrayList<DataClass> = ArrayList()

                val adapter = MyAdapter(requireContext(), avisosArrayList)
                recyclerView.adapter = adapter

                val databaseReference: DatabaseReference =
                    FirebaseDatabase.getInstance().getReference("Exclusive")
                        .child(classe)

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
                                    val daysPassed =
                                        (currentDate - uploadDate) / (1000 * 60 * 60 * 24)
                                    if (daysPassed >= 3) {
                                        withContext(Dispatchers.IO) {
                                            val reference: DatabaseReference =
                                                FirebaseDatabase.getInstance()
                                                    .getReference("Exclusive").child(
                                                        classe
                                                    )
                                            val storage: FirebaseStorage =
                                                FirebaseStorage.getInstance()
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
                            binding.shimmerViewExclusive.stopShimmer()
                            binding.shimmerViewExclusive.visibility = View.GONE
                            binding.dataViewExclusive.visibility = View.VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        binding.shimmerViewExclusive.stopShimmer()
                        binding.shimmerViewExclusive.visibility = View.GONE
                        binding.dataViewExclusive.visibility = View.VISIBLE
                    }
                })

            }

        return view
    }

    private fun createChannel() {
        notificationWorkManager =
            activity.applicationContext.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "secundary_notification_channel",
                "Avisos Exclusivos",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Notificação de avisos exclusivos."

            notificationWorkManager.createNotificationChannel(notificationChannel)
        }
    }

    private fun sendNotification() {
        createChannel()

        val notifyIntent = Intent(activity, ExclusiveFragment::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            activity,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder =
            NotificationCompat.Builder(activity, "secundary_notification_channel")
                .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("ATENÇÃO!!!")
                .setContentText("Um novo aviso de sala foi postado.")
                .setPriority(NotificationCompat.PRIORITY_MAX).setContentIntent(notifyPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL).setAutoCancel(true)

        notificationWorkManager.notify(0, notificationBuilder.build())
    }

    override fun onAttach(mContext: Context) {
        super.onAttach(mContext)
        if (mContext is MainActivity) {
            activity = mContext
        }
    }
}
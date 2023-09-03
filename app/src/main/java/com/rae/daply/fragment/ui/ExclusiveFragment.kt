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
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class ExclusiveFragment : Fragment() {

    // Variáveis necessárias
    private lateinit var notificationManager: NotificationManager
    private lateinit var binding: FragmentExclusiveBinding
    private val currentDate = System.currentTimeMillis()

    private lateinit var activity: MainActivity
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private var isFirstUpdate = true // Controla a primeira atualização
    private var firstSize = 0 // Tamanho inicial da lista

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentExclusiveBinding.inflate(inflater, container, false)
        val view = binding.root

        setupSharedPreferences()
        loadUserInfoAndData()

        return view
    }

    // Configuração das SharedPreferences
    private fun setupSharedPreferences() {
        sharedPreferences =
            activity.getSharedPreferences("shared_prefs", AppCompatActivity.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    // Carregamento das informações do usuário e dos dados
    private fun loadUserInfoAndData() {
        val savedUser = sharedPreferences.getString("save", null).toString()

        FirebaseDatabase.getInstance().reference.child("Users").child(savedUser).get()
            .addOnSuccessListener { snapshot ->
                val serie = snapshot.child("serie").value.toString()
                val curso = snapshot.child("curso").value.toString()
                val periodo = snapshot.child("periodo").value.toString()

                val classe = serie.take(1) + "-" + curso + "-" + periodo.take(1)

                editor.putString("classe", classe)
                editor.apply()

                setupRecyclerView(classe)
            }
    }

    // Configuração do RecyclerView
    @OptIn(DelicateCoroutinesApi::class)
    private fun setupRecyclerView(classe: String) {
        val recyclerView: RecyclerView = binding.recyclerView
        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.stackFromEnd = true
        linearLayoutManager.reverseLayout = true
        recyclerView.layoutManager = linearLayoutManager

        val avisosArrayList: ArrayList<DataClass> = ArrayList()
        val adapter = MyAdapter(requireContext(), avisosArrayList)
        recyclerView.adapter = adapter

        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("Exclusive").child(classe)

        // Ouvinte para os dados no Firebase
        databaseReference.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                GlobalScope.launch(Dispatchers.Main) {
                    avisosArrayList.clear()
                    for (itemSnapshot in snapshot.children) {
                        val dataClass = itemSnapshot.getValue(DataClass::class.java)
                        dataClass?.let {
                            it.key = itemSnapshot.key
                            avisosArrayList.add(it)
                        }
                    }

                    // Processar dados expirados
                    processExpiredData(classe, snapshot)

                    // Notificação
                    checkNotification(databaseReference)

                    adapter.updateData(avisosArrayList)
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

    private fun checkNotification(reference: DatabaseReference) {
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (isFirstUpdate) {
                    firstSize = dataSnapshot.childrenCount.toInt()
                    isFirstUpdate = false
                } else {
                    if (dataSnapshot.childrenCount.toInt() > firstSize) {
                        sendNotification()
                    } else {
                        firstSize = dataSnapshot.childrenCount.toInt()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Processar dados expirados
    private suspend fun processExpiredData(
        classe: String, snapshot: DataSnapshot
    ) {
        for (itemSnapshot in snapshot.children) {
            val dataClass = itemSnapshot.getValue(DataClass::class.java)
            dataClass?.let {
                val uploadDate = it.dataMili
                val image = it.imageURL
                val key = it.key

                if ((uploadDate != null) && (image != null) && (key != null)) {
                    val daysPassed = (currentDate - uploadDate) / (1000 * 60 * 60 * 24)
                    if (daysPassed >= 3) {
                        deleteExpiredDataAndImages(
                            classe, key, image
                        ) // Excluir dados e imagens expirados
                        itemSnapshot.ref.removeValue().await()
                    }
                }
            }
        }
    }

    // Excluir dados e imagens expirados
    private suspend fun deleteExpiredDataAndImages(classe: String, key: String, imageUrl: String) {
        withContext(Dispatchers.IO) {
            val reference: DatabaseReference =
                FirebaseDatabase.getInstance().getReference("Exclusive").child(classe)
            val storage: FirebaseStorage = FirebaseStorage.getInstance()
            val storageReference: StorageReference = storage.getReferenceFromUrl(imageUrl)
            storageReference.delete().await()
            reference.child(key).removeValue().await()
        }
    }

    // Criar canal de notificação
    private fun createNotificationChannel() {
        notificationManager =
            activity.applicationContext.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "secundary_notification_channel",
                "Avisos Exclusivos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                description = "Notificação de avisos exclusivos."
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    // Enviar notificação
    private fun sendNotification() {
        createNotificationChannel()

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
            NotificationCompat.Builder(activity, "secundary_notification_channel").apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentTitle("ATENÇÃO!!!")
                setContentText("Um novo aviso de sala foi postado.")
                priority = NotificationCompat.PRIORITY_MAX
                setContentIntent(notifyPendingIntent)
                setDefaults(NotificationCompat.DEFAULT_ALL)
                setAutoCancel(true)
            }

        notificationManager.notify(0, notificationBuilder.build())
    }

    // Anexar a atividade
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            activity = context
        }
    }
}
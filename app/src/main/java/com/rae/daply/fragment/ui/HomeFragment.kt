package com.rae.daply.fragment.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.R
import com.rae.daply.data.DataClass
import com.rae.daply.data.MyAdapter
import com.rae.daply.databinding.FragmentHomeBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var adapter: MyAdapter
    private lateinit var notificationManager: NotificationManager
    private var isFirstUpdate = true
    private lateinit var binding: FragmentHomeBinding
    private val currentDate = System.currentTimeMillis()
    private lateinit var mContext: Context
    private var firstSize = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        setupRecyclerView()
        loadDataFromFirebase()

        return view
    }

    private fun setupRecyclerView() {
        // Configuração do RecyclerView
        val recyclerView: RecyclerView = binding.recyclerView
        val linearLayoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
            reverseLayout = true
        }
        recyclerView.layoutManager = linearLayoutManager

        // Inicialização do adaptador e vinculação ao RecyclerView
        val avisosArrayList: ArrayList<DataClass> = ArrayList()
        adapter = MyAdapter(requireContext(), avisosArrayList)
        recyclerView.adapter = adapter
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun loadDataFromFirebase() {
        // Carregamento de dados do Firebase
        val databaseReference: DatabaseReference =
            FirebaseDatabase.getInstance().getReference("RAE")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val avisosArrayList: ArrayList<DataClass> = ArrayList()

                GlobalScope.launch(Dispatchers.Main) {
                    for (itemSnapshot in snapshot.children) {
                        val dataClass = itemSnapshot.getValue(DataClass::class.java)
                        dataClass?.let {
                            it.key = itemSnapshot.key
                            val image = it.imageURL
                            val key = it.key
                            val uploadDate = it.dataMili

                            if (uploadDate != null && image != null && key != null) {
                                val daysPassed = (currentDate - uploadDate) / (1000 * 60 * 60 * 24)
                                if (daysPassed >= 3) {
                                    // Chamada da função deleteExpiredDataAndImages dentro da corrotina
                                    deleteExpiredDataAndImages(key, image)
                                    itemSnapshot.ref.removeValue().await()
                                }
                            }
                            avisosArrayList.add(it)
                        }
                    }

                    checkNotification(databaseReference)

                    adapter.updateData(avisosArrayList)
                    binding.shimmerView.stopShimmer()
                    binding.shimmerView.visibility = View.GONE
                    binding.dataView.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Tratar o erro, se necessário
                binding.shimmerView.stopShimmer()
                binding.shimmerView.visibility = View.GONE
                binding.dataView.visibility = View.VISIBLE
            }
        })
        databaseReference.keepSynced(true)
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

    private suspend fun deleteExpiredDataAndImages(key: String, imageUrl: String) {
        // Exclusão assíncrona de dados e imagens expirados
        withContext(Dispatchers.IO) {
            val reference: DatabaseReference = FirebaseDatabase.getInstance().getReference("RAE")
            val storage: FirebaseStorage = FirebaseStorage.getInstance()
            val storageReference: StorageReference = storage.getReferenceFromUrl(imageUrl)
            storageReference.delete().await()
            reference.child(key).removeValue().await()
        }
    }

    private fun sendNotification() {
        // Envio de notificação
        createNotificationChannel()

        val notifyIntent = Intent(mContext, HomeFragment::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            mContext,
            0,
            notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder =
            NotificationCompat.Builder(mContext, "primary_notification_channel").apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentTitle("ATENÇÃO!!!")
                setContentText("Um novo aviso foi postado.")
                priority = NotificationCompat.PRIORITY_MAX
                setContentIntent(notifyPendingIntent)
                setDefaults(NotificationCompat.DEFAULT_ALL)
                setAutoCancel(true)
            }

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel() {
        // Criação do canal de notificação
        notificationManager =
            mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "primary_notification_channel", "Avisos", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                description = "Notificação de avisos."
            }

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
}
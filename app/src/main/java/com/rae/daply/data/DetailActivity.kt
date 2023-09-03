package com.rae.daply.data

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.databinding.ActivityDetailBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialização das SharedPreferences
        sharedPreferences = getSharedPreferences("shared_prefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        initUIElements()
        setupEditFabMenu()

        // Obtenção dos dados passados através do Intent
        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val key = bundle.getString("data").toString().replace("/", "-")
            val imageURL = bundle.getString("image").toString()

            setUIElements(bundle)

            binding.deleteButton.setOnClickListener {
                deleteData(key, imageURL)
            }

            binding.editButton.setOnClickListener {
                navigateToUpdateActivity(bundle)
            }
        }
    }

    private fun initUIElements() {
        binding.detailAviso.text = ""
        binding.detailTitulo.text = ""
        binding.detailData.text = ""
        binding.detailAutor.text = ""
        binding.detailEmail.text = ""
        Glide.with(this).clear(binding.detailImage)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun setupEditFabMenu() {
        GlobalScope.launch(Dispatchers.Main) {
            val userType = sharedPreferences.getString("userType", "user")
            if (userType != "admin") {
                binding.editFabMenu.visibility = View.GONE
            }
        }
    }

    private fun setUIElements(bundle: Bundle) {
        binding.detailAviso.text = bundle.getString("aviso")
        binding.detailTitulo.text = bundle.getString("titulo")
        binding.detailData.text = bundle.getString("data")
        binding.detailAutor.text = bundle.getString("autor")
        binding.detailEmail.text = "Email para contato:\n" + bundle.getString("emailAutor")

        Glide.with(this).load(bundle.getString("image")).into(binding.detailImage)
    }

    private fun deleteData(key: String, imageURL: String) {
        val classe = sharedPreferences.getString("classe", null).toString()
        val type = intent.extras?.getString("type").toString()

        val reference: DatabaseReference = if (type == "normal") {
            FirebaseDatabase.getInstance().getReference("RAE")
        } else {
            FirebaseDatabase.getInstance().getReference("Exclusive").child(classe)
        }

        val storage: FirebaseStorage = FirebaseStorage.getInstance()
        val storageReference: StorageReference = storage.getReferenceFromUrl(imageURL)

        storageReference.delete().addOnSuccessListener {
            reference.child(key).removeValue().addOnSuccessListener {
                Toast.makeText(this, "Apagado!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Falha na exclusão dos dados", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Falha na exclusão da imagem", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToUpdateActivity(bundle: Bundle) {
        val intent = Intent(this, UpdateActivity::class.java).apply {
            putExtras(bundle)
        }
        startActivity(intent)
    }
}

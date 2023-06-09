package com.rae.daply.data

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.github.clans.fab.FloatingActionButton
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.utils.GlideApp
import com.rae.daply.MainActivity
import com.rae.daply.databinding.ActivityDetailBinding
import com.rae.daply.utils.save
import com.rae.daply.utils.userType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val aviso: TextView = binding.detailAviso
        val titulo: TextView = binding.detailTitulo
        val imagem: ImageView = binding.detailImage
        val data: TextView = binding.detailData
        val autor: TextView = binding.detailAutor
        val delete: FloatingActionButton = binding.deleteButton
        val edit: FloatingActionButton = binding.editButton
        var key = ""
        var imageURL = ""

        GlobalScope.launch(Dispatchers.Main) {
            if(userType != "admin") {
                val editFabMenu: com.github.clans.fab.FloatingActionMenu = binding.editFabMenu
                editFabMenu.visibility = View.GONE
            }
        }


        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            key = bundle.getString("Data").toString().replace("/", "-")
            imageURL = bundle.getString("Image").toString()
            aviso.text = bundle.getString("Aviso")
            titulo.text = bundle.getString("Titulo")
            GlideApp.with(this).load(bundle.getString("Image")).into(imagem)
            autor.text = bundle.getString("Autor")
            data.text = bundle.getString("Data")
        }

        delete.setOnClickListener {
            val reference: DatabaseReference = FirebaseDatabase.getInstance().getReference("RAE")
            val storage: FirebaseStorage = FirebaseStorage.getInstance()
            val storageReference: StorageReference = storage.getReferenceFromUrl(imageURL)

            storageReference.delete().addOnSuccessListener {
                reference.child(key).removeValue()
                Toast.makeText(this, "Apagado!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Falha", Toast.LENGTH_SHORT).show()
            }
        }

        edit.setOnClickListener {
            val intent = Intent(this, UpdateActivity::class.java).putExtra("Image", imageURL)
                .putExtra("Titulo", titulo.text.toString()).putExtra("Aviso", aviso.text.toString())
                .putExtra("Data", data.text.toString()).putExtra("Autor", autor.text.toString())
            startActivity(intent)
        }
    }
}
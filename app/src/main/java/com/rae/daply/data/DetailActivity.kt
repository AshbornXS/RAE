package com.rae.daply.data

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.github.clans.fab.FloatingActionButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.BuildConfig
import com.rae.daply.GlideApp
import com.rae.daply.MainActivity
import com.rae.daply.R

class DetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val aviso: TextView = findViewById(R.id.detailAviso)
        val titulo: TextView = findViewById(R.id.detailTitulo)
        val imagem: ImageView = findViewById(R.id.detailImage)
        val data: TextView = findViewById(R.id.detailData)
        val autor: TextView = findViewById(R.id.detailAutor)
        val delete: FloatingActionButton = findViewById(R.id.deleteButton)
        val edit: FloatingActionButton = findViewById(R.id.editButton)
        var key = ""
        var imageURL = ""

        if(BuildConfig.FLAVOR == "student"){
            val editFabMenu: com.github.clans.fab.FloatingActionMenu = findViewById(R.id.editFabMenu)
            editFabMenu.visibility = View.GONE
        }

        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            key = bundle.getString("Data").toString().replace("/", "-")
            imageURL = bundle.getString("Image").toString()
            aviso.text = bundle.getString("Aviso")
            titulo.text = bundle.getString("Titulo")
            GlideApp.with(this)
                .load(bundle.getString("Image"))
                .into(imagem)
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
            val intent = Intent(this, UpdateActivity::class.java)
                .putExtra("Image", imageURL)
                .putExtra("Titulo", titulo.text.toString())
                .putExtra("Aviso", aviso.text.toString())
                .putExtra("Data", data.text.toString())
                .putExtra("Autor", autor.text.toString())
            startActivity(intent)
        }
    }
}
package com.rae.daply.data

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.databinding.ActivityUpdateBinding

class UpdateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpdateBinding
    private lateinit var database: DatabaseReference

    private lateinit var imageURL: String
    private var uri: Uri? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imagem: ImageView = binding.updateImage

        val bundle: Bundle? = intent.extras
        if (bundle != null) {
            val titulo: TextView = binding.updateTitulo
            val aviso: TextView = binding.updateAviso
            val autor: TextView = binding.updateAutor

            imageURL = bundle.getString("Image").toString()
            aviso.text = bundle.getString("Aviso")
            titulo.text = bundle.getString("Titulo")
            Glide.with(this).load(bundle.getString("Image")).into(imagem)
            autor.text = bundle.getString("Autor")
        }

        val activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                uri = data?.data!!
                imagem.setImageURI(uri)
            } else {
                Toast.makeText(this, "Nenhuma imagem selecionada!", Toast.LENGTH_SHORT).show()
            }
        }

        imagem.setOnClickListener {
            val photoPicker = Intent(Intent.ACTION_PICK)
            photoPicker.type = "image/*"
            activityResultLauncher.launch(photoPicker)
        }

        binding.updateButton.setOnClickListener {
            val titulo = binding.updateTitulo.text.toString()
            val aviso = binding.updateAviso.text.toString()
            val autor = binding.updateAutor.text.toString()
            val key = bundle?.getString("Data").toString().replace("/", "-")

            val storageReference: StorageReference =
                FirebaseStorage.getInstance().reference.child("Android Images")
                    .child(uri?.lastPathSegment.toString())

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setView(R.layout.progress_layout)
            val dialog: AlertDialog = builder.create()
            dialog.show()

            if (uri == null) {
                updateData(titulo, aviso, autor, key, imageURL)
                dialog.dismiss()
            } else {
                storageReference.putFile(uri!!).addOnSuccessListener { taskSnapshot ->
                    val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                    while (!uriTask.isComplete);
                    val urlImage = uriTask.result
                    this.imageURL = urlImage.toString()
                    updateData(titulo, aviso, autor, key, imageURL)
                    dialog.dismiss()
                }.addOnFailureListener {
                    dialog.dismiss()
                }
            }
        }
    }

    private fun updateData(
        titulo: String, aviso: String, autor: String, key: String, imageURL: String
    ) {

        database = FirebaseDatabase.getInstance().getReference("RAE")
        val info = mapOf(
            "titulo" to titulo, "aviso" to aviso, "autor" to autor, "imageURL" to imageURL
        )

        database.child(key).updateChildren(info).addOnSuccessListener {
            binding.updateTitulo.text.clear()
            binding.updateAviso.text.clear()
            binding.updateAutor.text.clear()
            Toast.makeText(this, "Atualizado com sucesso!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java).putExtra(titulo, titulo)
            startActivity(intent)
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Falha ao atualizar", Toast.LENGTH_SHORT).show()
        }
    }
}
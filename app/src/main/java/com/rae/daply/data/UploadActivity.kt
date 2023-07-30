package com.rae.daply.data

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.databinding.ActivityUploadBinding
import com.rae.daply.utils.userType
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class UploadActivity : AppCompatActivity() {

    private lateinit var uploadImage: ImageView
    private lateinit var saveButton: Button
    private lateinit var uploadTitulo: EditText
    private lateinit var uploadAviso: EditText
    private lateinit var uploadAutor: EditText
    private lateinit var imageURL: String
    private var uri: Uri? = null
    private val periodo = arrayOf("Manhã", "Tarde", "Noite")
    private val series = arrayOf("1º Ano", "2º Ano", "3º Ano")
    private val cursos = arrayOf("IPIA", "MEC", "MECA", "DS", "ADM", "MEIO", "LOG", "ELECTRO")

    private lateinit var binding: ActivityUploadBinding

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adaptorItemsPeriodo = ArrayAdapter(this, R.layout.list_item, periodo)
        binding.signupPeriodo.setAdapter(adaptorItemsPeriodo)
        binding.signupPeriodo.setText("Manhã", false)

        val adaptorItemsSerie = ArrayAdapter(this, R.layout.list_item, series)
        binding.signupSerie.setAdapter(adaptorItemsSerie)
        binding.signupSerie.setText("1º Ano", false)

        val adaptorItemsCurso = ArrayAdapter(this, R.layout.list_item, cursos)
        binding.signupCurso.setAdapter(adaptorItemsCurso)
        binding.signupCurso.setText("DS", false)

        uploadImage = binding.uploadImage
        saveButton = binding.uploadButton
        uploadTitulo = binding.uploadTitulo
        uploadAviso = binding.uploadAviso
        uploadAutor = binding.uploadAutor

        GlobalScope.launch(Dispatchers.Main) {
            if (userType != "admin") {
                finishActivity(1)
            }
        }

        val activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                uri = data?.data!!
                uploadImage.setImageURI(uri)
            } else {
                Toast.makeText(
                    this@UploadActivity, "Nenhuma imagem selecionada!", Toast.LENGTH_SHORT
                ).show()
            }
        }

        uploadImage.setOnClickListener {
            val photoPicker = Intent(Intent.ACTION_PICK)
            photoPicker.type = "image/*"
            activityResultLauncher.launch(photoPicker)
        }
        saveButton.setOnClickListener {
            saveData()
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveData() {
        val storageReference: StorageReference =
            FirebaseStorage.getInstance().reference.child("Android Images")
                .child(uri?.lastPathSegment.toString())

        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val dialog: AlertDialog = builder.create()
        dialog.show()

        if (uri == null || uploadTitulo.text.toString().isEmpty() || uploadAviso.text.toString()
                .isEmpty() || uploadAutor.text.toString().isEmpty()
        ) {
            dialog.dismiss()
            Toast.makeText(this, "Nenhum dos campos podem ser vazios!", Toast.LENGTH_SHORT).show()
        } else {
            storageReference.putFile(uri!!).addOnSuccessListener { taskSnapshot ->
                val uriTask: Task<Uri> = taskSnapshot.storage.downloadUrl
                while (!uriTask.isComplete);
                val urlImage: Uri? = uriTask.result
                imageURL = urlImage.toString()
                uploadData()
                dialog.dismiss()
            }.addOnFailureListener {
                dialog.dismiss()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadData() {
        val format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val data = LocalDateTime.now().format(format)

        val titulo = uploadTitulo.text.toString()
        val avisoPre = uploadAviso.text.toString()
        val autor = uploadAutor.text.toString()

        val aviso =
            avisoPre + "\n\n- Email para contato: " + FirebaseAuth.getInstance().currentUser?.email

        val dataMili = System.currentTimeMillis()

        val currentDate = data.replace("/", "-")

        if (binding.exclusive.isChecked) {
            val serie = binding.signupSerie.text.toString().take(1)
            val periodo = binding.signupPeriodo.text.toString().take(1)

            val classe = serie + "-" + binding.signupCurso.text.toString() + "-" + periodo

            val type = "exclusive"

            val dataClass = DataClass(titulo, aviso, data, autor, imageURL, dataMili, type)

            FirebaseDatabase.getInstance().getReference("Exclusive").child(classe)
                .child(currentDate).setValue(dataClass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
                }
        } else {
            val type = "normal"

            val dataClass = DataClass(titulo, aviso, data, autor, imageURL, dataMili, type)

            FirebaseDatabase.getInstance().getReference("RAE").child(currentDate)
                .setValue(dataClass)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun itemClicked(v: View) {
        if ((v as CheckBox).isChecked) {
            binding.arrays.visibility = View.VISIBLE
        } else {
            binding.arrays.visibility = View.GONE
        }
    }
}
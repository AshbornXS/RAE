package com.rae.daply.data

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.rae.daply.R
import com.rae.daply.databinding.ActivityUploadBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UploadActivity : AppCompatActivity() {

    // Variáveis globais para armazenar informações temporárias
    private lateinit var imageURL: String
    private var uri: Uri? = null

    private lateinit var binding: ActivityUploadBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtém a posição da intent para configuração inicial
        val position = intent.getIntExtra("position", 1)

        if (position == 2) {
            binding.exclusive.isChecked = true
            binding.arrays.visibility = View.VISIBLE
        }

        // Inicializa as SharedPreferences e o editor
        sharedPreferences = getSharedPreferences("shared_prefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // Configura ArrayAdapter para os campos de seleção
        val adaptorItemsPeriodo =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.periodos))
        binding.uploadPeriodo.setAdapter(adaptorItemsPeriodo)

        val adaptorItemsSerie =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.series))
        binding.uploadSerie.setAdapter(adaptorItemsSerie)

        val adaptorItemsCurso =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.cursos))
        binding.uploadCurso.setAdapter(adaptorItemsCurso)

        // Verifica o tipo de usuário e oculta se não for admin
        GlobalScope.launch(Dispatchers.Main) {
            val userType = getSharedPreferences("shared_prefs", MODE_PRIVATE).getString(
                "userType", "user"
            )

            if (userType != "admin") {
                finish()
            }
        }

        // Configura o launcher de resultado da atividade de seleção de imagem
        val activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                uri = data?.data!!
                binding.uploadImage.setImageURI(uri)
            } else {
                Toast.makeText(
                    this@UploadActivity, "Nenhuma imagem selecionada!", Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Define o listener de clique para o campo de imagem
        binding.uploadImage.setOnClickListener {
            val photoPicker = Intent(Intent.ACTION_PICK)
            photoPicker.type = "image/*"
            activityResultLauncher.launch(photoPicker)
        }
        // Define o listener de clique para o botão de upload
        binding.uploadButton.setOnClickListener {
            saveData()
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun saveData() {
        // Cria uma referência para o armazenamento do Firebase
        val storageReference: StorageReference =
            FirebaseStorage.getInstance().reference.child("Android Images")
                .child(uri?.lastPathSegment.toString())

        // Constrói e exibe o diálogo de progresso
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(false)
        builder.setView(R.layout.progress_layout)
        val dialog: AlertDialog = builder.create()
        dialog.show()

        if (uri == null || binding.uploadTitulo.text.toString()
                .isEmpty() || binding.uploadAviso.text.toString()
                .isEmpty()
        ) {
            // Dismiss the dialog and show a toast if any required field is empty
            dialog.dismiss()
            Toast.makeText(this, "Nenhum dos campos podem ser vazios!", Toast.LENGTH_SHORT).show()
        } else {
            // Upload da imagem para o Firebase Storage
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
        // Obtém as informações do usuário atual das SharedPreferences
        sharedPreferences = getSharedPreferences("shared_prefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        val save = sharedPreferences.getString(
            "save", null
        ).toString()

        val format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val data = LocalDateTime.now().format(format)

        val titulo = binding.uploadTitulo.text.toString()
        val aviso = binding.uploadAviso.text.toString()

        val emailAutor = FirebaseAuth.getInstance().currentUser?.email

        val dataMili = System.currentTimeMillis()

        val currentDate = data.replace("/", "-")

        // Obtém o nome do autor do banco de dados
        FirebaseDatabase.getInstance().reference.child("Users").child(save).get().addOnSuccessListener {
            val autor = it.child("name").value.toString()
            if (binding.exclusive.isChecked) {
                val serie = binding.uploadSerie.text.toString().take(1)
                val periodo = binding.uploadPeriodo.text.toString().take(1)

                val classe = serie + "-" + binding.uploadCurso.text.toString() + "-" + periodo

                val type = "exclusive"

                val dataClass = DataClass(titulo, aviso, data, autor, emailAutor, imageURL, dataMili, type)

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

                val dataClass = DataClass(titulo, aviso, data, autor, emailAutor, imageURL, dataMili, type)

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
    }

    // Método chamado quando um item é clicado
    fun itemClicked(v: View) {
        if ((v as CheckBox).isChecked) {
            binding.arrays.visibility = View.VISIBLE
        } else {
            binding.arrays.visibility = View.GONE
        }
    }

    fun goBack(view: View) {
        finish()
    }
}

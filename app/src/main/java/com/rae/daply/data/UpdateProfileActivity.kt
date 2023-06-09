package com.rae.daply.data

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.databinding.ActivityUploadProfileBinding
import com.rae.daply.login.LoginActivity
import com.rae.daply.utils.save

class UpdateProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadProfileBinding
    private lateinit var database: DatabaseReference
    private lateinit var user: FirebaseUser

    private val series = arrayOf("1º Ano", "2º Ano", "3º Ano")
    private val cursos = arrayOf("IPIA", "MEC", "MECA", "DS", "ADM", "MEIO", "LOG", "ELECTRO")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adaptorItemsSerie = ArrayAdapter(this, R.layout.list_item, series)

        val adaptorItemsCurso = ArrayAdapter(this, R.layout.list_item, cursos)

        val name = intent.getStringExtra("name")
        val serie = intent.getStringExtra("serie")
        val curso = intent.getStringExtra("curso")

        binding.updateNome.setText(name)

        binding.updateSerie.setText(serie)
        binding.updateSerie.setAdapter(adaptorItemsSerie)

        binding.updateCurso.setText(curso)
        binding.updateCurso.setAdapter(adaptorItemsCurso)

        binding.updateProfileButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun updateProfile() {
        val updatedName = binding.updateNome.text.toString()
        val updatedSerie = binding.updateSerie.text.toString()
        val updatedCurso = binding.updateCurso.text.toString()
        val updatedPassword = binding.updatePassword.text.toString()

        user = FirebaseAuth.getInstance().currentUser!!
        database = FirebaseDatabase.getInstance().getReference("Users")
        val info = mapOf(
            "name" to updatedName, "serie" to updatedSerie, "curso" to updatedCurso
        )

        val updateProfileBuilder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_update, null)
        updateProfileBuilder.setView(view)
        val updateProfileDialog = updateProfileBuilder.create()

        updateProfileDialog.show()

        updateProfileDialog.findViewById<Button>(R.id.updateCancelar)?.setOnClickListener {
            updateProfileDialog.dismiss()
        }

        updateProfileDialog.findViewById<Button>(R.id.updateConfirmar)?.setOnClickListener {
            val email = user.email.toString()
            val oldPass = updateProfileDialog.findViewById<EditText>(R.id.oldPassword)?.text.toString()
            val credential = EmailAuthProvider.getCredential(email, oldPass)

            user.reauthenticate(credential).addOnCompleteListener { task ->
                when {
                    task.isSuccessful -> {
                        if (updatedPassword.isNotEmpty()) {
                            user.updatePassword(updatedPassword).addOnCompleteListener {
                                if (it.isSuccessful) {
                                    Toast.makeText(
                                        this, "Senha Atualizada Com Sucesso!", Toast.LENGTH_SHORT
                                    ).show()

                                    FirebaseAuth.getInstance().signOut()
                                    val i = Intent(this, LoginActivity::class.java)
                                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                    startActivity(i)
                                } else Toast.makeText(
                                    this, "Erro ao atualizar a senha", Toast.LENGTH_SHORT
                                ).show()
                                Log.i("UpdateProfileActivity", it.exception.toString())
                            }
                        }

                        database.child(save).updateChildren(info).addOnSuccessListener {
                            Toast.makeText(
                                this, "Atualizado com sucesso!", Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }

            }
        }


    }
}
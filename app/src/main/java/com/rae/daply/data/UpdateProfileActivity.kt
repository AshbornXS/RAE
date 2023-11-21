package com.rae.daply.data

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.databinding.ActivityUploadProfileBinding
import com.rae.daply.login.LoginActivity

class UpdateProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadProfileBinding
    private lateinit var database: DatabaseReference
    private lateinit var user: FirebaseUser

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var edit: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializa as SharedPreferences e o editor
        sharedPreferences = getSharedPreferences("shared_prefs", MODE_PRIVATE)
        edit = sharedPreferences.edit()

        // Configura ArrayAdapter para os campos de seleção
        val adaptorItemsPeriodo =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.periodos))
        val adaptorItemsSerie =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.series))
        val adaptorItemsCurso =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.cursos))

        // Obtém os dados da intent
        val name = intent.getStringExtra("name")?.replace("Nome: ", "")
        val periodo = intent.getStringExtra("periodo")?.replace("Periodo: ", "")
        val serie = intent.getStringExtra("serie")?.replace("Série: ", "")
        val curso = intent.getStringExtra("curso")?.replace("Curso: ", "")

        // Define os valores iniciais dos campos de entrada
        binding.updateNome.setText(name)
        binding.updatePeriodo.setText(periodo)
        binding.updatePeriodo.setAdapter(adaptorItemsPeriodo)
        binding.updateSerie.setText(serie)
        binding.updateSerie.setAdapter(adaptorItemsSerie)
        binding.updateCurso.setText(curso)
        binding.updateCurso.setAdapter(adaptorItemsCurso)

        // Define o listener de clique para o botão de atualização
        binding.updateProfileButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun updateProfile() {
        // Obtém os valores atualizados dos campos de entrada
        val updatedName = binding.updateNome.text.toString()
        val updatedPeriodo = binding.updatePeriodo.text.toString()
        val updatedSerie = binding.updateSerie.text.toString()
        val updatedCurso = binding.updateCurso.text.toString()
        val updatedPassword = binding.updatePassword.text.toString()

        // Obtém o usuário atual e a referência do banco de dados
        user = FirebaseAuth.getInstance().currentUser!!
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Prepara os dados para atualização
        val info = mapOf(
            "name" to updatedName,
            "periodo" to updatedPeriodo,
            "serie" to updatedSerie,
            "curso" to updatedCurso
        )

        if (updatedName.isEmpty() || updatedPeriodo.isEmpty() || updatedSerie.isEmpty() || updatedCurso.isEmpty()) {
            Toast.makeText(
                this, "Preencha todos os campos", Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Constrói e exibe o diálogo de confirmação
        val updateProfileBuilder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_update, null)
        updateProfileBuilder.setCancelable(true)
        updateProfileBuilder.setView(view)
        val updateProfileDialog = updateProfileBuilder.create()
        updateProfileDialog.show()

        updateProfileDialog.window?.setBackgroundDrawable(ColorDrawable(0))

        // Define os listeners de clique para os botões do diálogo
        updateProfileDialog.findViewById<Button>(R.id.updateConfirmar)?.setOnClickListener {
            val email = user.email.toString()
            val oldPass =
                updateProfileDialog.findViewById<EditText>(R.id.oldPassword)?.text.toString()
            val credential = EmailAuthProvider.getCredential(email, oldPass)

            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (updatedPassword.isNotEmpty()) {
                        user.updatePassword(updatedPassword).addOnCompleteListener {
                            if (it.isSuccessful) {
                                // Atualização de senha bem-sucedida, desconecta e navega para a tela de login
                                edit.putBoolean("isLogged", false)
                                edit.apply()
                                FirebaseAuth.getInstance().signOut()
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                startActivity(intent)
                                Toast.makeText(
                                    this, "Senha Atualizada Com Sucesso!", Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this, "Erro ao atualizar a senha", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    // Atualiza as informações do perfil do usuário no banco de dados
                    val save = getSharedPreferences("shared_prefs", MODE_PRIVATE).getString(
                        "save", null
                    ).toString()

                    database.child(save).updateChildren(info).addOnSuccessListener {
                        Toast.makeText(
                            this, "Atualizado com sucesso!", Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }.addOnFailureListener {
                        Toast.makeText(
                            this, "Falha ao atualizar", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            updateProfileDialog.dismiss()
        }
    }

    fun goBack(view: View) {
        finish()
    }
}

package com.rae.daply.login

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.R
import com.rae.daply.databinding.ActivityAdminCheckBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminCheckActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminCheckBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialização do Firebase Auth
        coroutineScope.launch {
            firebaseAuth = FirebaseAuth.getInstance()

            // Configurando o botão de verificação do administrador
            binding.checkButton.setOnClickListener {
                val email = binding.checkEmail.text.toString()
                val password = binding.checkPassword.text.toString()

                if (email.isNotEmpty() && password.isNotEmpty()) {
                    val save = email.replace("@etec.sp.gov.br", "").replace(".", "-")

                    val dbReference = FirebaseDatabase.getInstance()
                    dbReference.reference.child("Users").child(save).child("userType").get()
                        .addOnSuccessListener {
                            if (it.value.toString() != "aluno") {
                                login(email, password)
                            } else {
                                clearFieldsAndShowToast(
                                    "Você não é um administrador"
                                )
                            }
                        }
                } else {
                    clearFieldsAndShowToast(
                        "Digite seu acesso de administrador"
                    )
                }
            }

            // Configurando o botão "Esqueci minha senha"
            binding.forgotPassword.setOnClickListener {
                showForgotPasswordDialog()
            }
        }
    }

    // Limpa os campos de email e senha e exibe uma mensagem de toast
    private fun clearFieldsAndShowToast(message: String) {
        binding.checkEmail.text?.clear()
        binding.checkPassword.text?.clear()
        Toast.makeText(
            this@AdminCheckActivity, message, Toast.LENGTH_SHORT
        ).show()
    }

    // Mostra o diálogo para redefinição de senha
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this@AdminCheckActivity)
        val view = layoutInflater.inflate(R.layout.dialog_forgot, null)
        val userEmail = view.findViewById<EditText>(R.id.editBox)
        builder.setView(view)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        // Configurando os botões no diálogo de redefinição de senha
        view.findViewById<Button>(R.id.btnReset).setOnClickListener {
            coroutineScope.launch { compareEmail(userEmail) }
            dialog.dismiss()
        }
        dialog.show()
    }

    // Comparando o email inserido com o email cadastrado para redefinir a senha
    private suspend fun compareEmail(email: EditText) = withContext(Dispatchers.IO) {
        if (email.text.toString()
                .isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()
        ) {
            return@withContext
        }

        firebaseAuth.sendPasswordResetEmail(email.text.toString()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                showToast("Olhe seu email!")
            } else {
                showToast("Email não cadastrado")
            }
        }
    }

    // Exibe uma mensagem de toast
    private fun showToast(message: String) {
        Toast.makeText(this@AdminCheckActivity, message, Toast.LENGTH_SHORT).show()
    }

    // Realiza o login do administrador
    private fun login(email: String, password: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this@AdminCheckActivity)
        builder.setCancelable(false)
        builder.setView(R.layout.loading_layout)
        val dialog: AlertDialog = builder.create()
        dialog.show()

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val verification = firebaseAuth.currentUser?.isEmailVerified
                if (verification == true) {
                    dialog.dismiss()
                    val intent = Intent(this@AdminCheckActivity, AdminSignupActivity::class.java)
                    startActivity(intent)
                } else {
                    clearFieldsAndShowToast("Verifique seu email")
                    dialog.dismiss()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}

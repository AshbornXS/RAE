package com.rae.daply.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.R
import com.rae.daply.data.DataClass
import com.rae.daply.databinding.ActivitySignupBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val series = arrayOf("1º Ano", "2º Ano", "3º Ano")
    private val cursos = arrayOf("IPIA", "MEC", "MECA", "DS", "ADM", "MEIO", "LOG", "ELECTRO")

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adaptorItemsSerie = ArrayAdapter(this, R.layout.list_item, series)
        binding.signupSerie.setAdapter(adaptorItemsSerie)

        val adaptorItemsCurso = ArrayAdapter(this, R.layout.list_item, cursos)
        binding.signupCurso.setAdapter(adaptorItemsCurso)


        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupButton.setOnClickListener {
            val name = binding.signupName.text.toString()
            val email = binding.signupEmail.text.toString()
            val password = binding.signupPassword.text.toString()
            val passwordConfirm = binding.signupConfirmPassword.text.toString()
            val serie = binding.signupSerie.text.toString()
            val curso = binding.signupCurso.text.toString()

            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setCancelable(false)
            builder.setView(R.layout.loading_layout)
            val dialog: AlertDialog = builder.create()
            dialog.show()

            GlobalScope.launch(Dispatchers.IO) {
                if (!email.contains("@etec.sp.gov.br")) {
                    showToast("Digite um email válido!")
                } else if (password.length < 6) {
                    showToast("A senha deve ter no mínimo 6 caracteres!")
                } else if (name.isNotEmpty() && email.isNotEmpty() && email.contains("@etec.sp.gov.br") && password.isNotEmpty() && passwordConfirm.isNotEmpty()) {
                    if (password == passwordConfirm) {
                        val dataClass = DataClass(
                            email = email,
                            userType = "aluno",
                            name = name,
                            serie = serie,
                            curso = curso
                        )
                        val save = email.replace("@etec.sp.gov.br", "").replace(".", "-")

                        withContext(Dispatchers.Main) {
                            FirebaseDatabase.getInstance().getReference("Users").child(save)
                                .setValue(dataClass)
                        }

                        firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this@SignupActivity) { task ->
                                if (task.isSuccessful) {
                                    firebaseAuth.currentUser?.sendEmailVerification()
                                        ?.addOnSuccessListener {
                                            dialog.dismiss()
                                            showToast("Email de verificação enviado!")
                                            val intent = Intent(
                                                this@SignupActivity, LoginActivity::class.java
                                            )
                                            startActivity(intent)
                                        }?.addOnFailureListener {
                                            dialog.dismiss()
                                            showToast(it.toString())
                                        }
                                } else if (task.exception.toString()
                                        .contains("The email address is already in use by another account.")
                                ) {
                                    showToast("Email já cadastrado!")
                                    dialog.dismiss()
                                }
                            }
                    } else {
                        showToast("As senhas não são iguais!")
                    }
                } else {
                    showToast("Preencha todos os campos!")
                }
            }
        }

        binding.loginRedirectText.setOnClickListener {
            val intent = Intent(this@SignupActivity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@SignupActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}

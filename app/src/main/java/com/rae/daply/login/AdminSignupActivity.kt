package com.rae.daply.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.R
import com.rae.daply.data.DataClass
import com.rae.daply.databinding.ActivityAdminSignupBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdminSignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAdminSignupBinding
    private lateinit var firebaseAuth: FirebaseAuth

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupAdminButton.setOnClickListener {
            val name = binding.signupAdminName.text.toString()
            val email = binding.signupAdminEmail.text.toString()
            val password = binding.signupAdminPassword.text.toString()
            val passwordConfirm = binding.signupAdminConfirmPassword.text.toString()

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
                            userType = "admin",
                            name = name,
                            serie = "Admin",
                            curso = "Admin"
                        )
                        val save = email.replace("@etec.sp.gov.br", "").replace(".", "-")

                        withContext(Dispatchers.Main) {
                            FirebaseDatabase.getInstance().getReference("Users").child(save)
                                .setValue(dataClass)
                        }

                        firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this@AdminSignupActivity) { task ->
                                if (task.isSuccessful) {
                                    firebaseAuth.currentUser?.sendEmailVerification()
                                        ?.addOnSuccessListener {
                                            dialog.dismiss()
                                            showToast("Email de verificação enviado!")
                                            val intent = Intent(
                                                this@AdminSignupActivity, LoginActivity::class.java
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
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@AdminSignupActivity, message, Toast.LENGTH_SHORT).show()
        }
    }
}

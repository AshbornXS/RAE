package com.rae.daply.login

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

        binding.signupAdminPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Não é necessário para esta implementação
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                binding.confirmPassword.visibility = if (hasText) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {
                // Não é necessário para esta implementação
            }
        })

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
                    runOnUiThread {
                        Toast.makeText(this@AdminSignupActivity, "Digite um email válido!", Toast.LENGTH_SHORT).show()
                    }
                } else if (password.length < 6) {
                    runOnUiThread {
                        Toast.makeText(this@AdminSignupActivity, "A senha deve ter no mínimo 6 caracteres!", Toast.LENGTH_SHORT).show()
                    }
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
                                            Toast.makeText(this@AdminSignupActivity, "Email de verificação enviado!", Toast.LENGTH_SHORT).show()
                                            val intent = Intent(
                                                this@AdminSignupActivity, LoginActivity::class.java
                                            )
                                            startActivity(intent)
                                        }?.addOnFailureListener {
                                            dialog.dismiss()
                                            Toast.makeText(this@AdminSignupActivity, it.toString(), Toast.LENGTH_SHORT).show()
                                        }
                                } else if (task.exception.toString()
                                        .contains("The email address is already in use by another account.")
                                ) {
                                    dialog.dismiss()
                                    runOnUiThread {
                                        Toast.makeText(this@AdminSignupActivity, "Email já cadastrado!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@AdminSignupActivity, "As senhas não são iguais!", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@AdminSignupActivity, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

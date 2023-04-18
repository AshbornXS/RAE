package com.rae.daply.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.R
import com.rae.daply.data.DataClass
import com.rae.daply.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private val series = arrayOf("1º Ano", "2º Ano", "3º Ano")
    private val cursos = arrayOf("IPIA", "MEC", "MECA", "DS", "ADM", "MEIO", "LOG", "ELECTRO")

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

            if (!email.contains("@etec.sp.gov.br")) {
                Toast.makeText(this, "Digite um email válido!", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(this, "A senha deve ter no mínimo 6 caracteres!", Toast.LENGTH_SHORT)
                    .show()
            } else if (name.isNotEmpty() && email.isNotEmpty() && email.contains("@etec.sp.gov.br") && password.isNotEmpty() && passwordConfirm.isNotEmpty()) {
                if (password == passwordConfirm) {
                    val dataClass = DataClass(email = email, userType = "aluno", name = name, serie = serie, curso = curso)
                    val save = email.replace("@etec.sp.gov.br", "").replace(".", "-")

                    FirebaseDatabase.getInstance().getReference("Users").child(save)
                        .setValue(dataClass)

                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                firebaseAuth.currentUser?.sendEmailVerification()
                                    ?.addOnSuccessListener {
                                        dialog.dismiss()
                                        Toast.makeText(
                                            this,
                                            "Email de verificação enviado!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }?.addOnFailureListener {
                                        dialog.dismiss()
                                        Toast.makeText(
                                            this, it.toString(), Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            } else if (task.exception.toString()
                                    .contains("The email address is already in use by another account.")
                            ) {
                                Toast.makeText(this, "Email já cadastrado!", Toast.LENGTH_SHORT)
                                    .show()
                                dialog.dismiss()
                            }
                        }
                } else {
                    Toast.makeText(this, "As senhas não são iguais!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginRedirectText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
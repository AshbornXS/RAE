package com.rae.daply.login

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

        coroutineScope.launch {

            firebaseAuth = FirebaseAuth.getInstance()

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
                                binding.checkEmail.text.clear()
                                binding.checkPassword.text.clear()
                                Toast.makeText(
                                    this@AdminCheckActivity,
                                    "Você não é um administrador",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                } else {
                    Toast.makeText(
                        this@AdminCheckActivity,
                        "Digite seu acesso de administrador",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            binding.forgotPassword.setOnClickListener {
                val builder = AlertDialog.Builder(this@AdminCheckActivity)
                val view = layoutInflater.inflate(R.layout.dialog_forgot, null)
                val userEmail = view.findViewById<EditText>(R.id.editBox)
                builder.setView(view)
                val dialog = builder.create()

                view.findViewById<Button>(R.id.btnReset).setOnClickListener {
                    coroutineScope.launch { compareEmail(userEmail) }
                    dialog.dismiss()
                }
                view.findViewById<Button>(R.id.btnCancel).setOnClickListener {
                    dialog.dismiss()
                }
                if (dialog.window != null) {
                    dialog.window!!.setBackgroundDrawable(ColorDrawable(0))
                }
                dialog.show()
            }
        }
    }

    private suspend fun compareEmail(email: EditText) = withContext(Dispatchers.IO) {
        if (email.text.toString().isEmpty()) {
            return@withContext
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            return@withContext
        }
        firebaseAuth.sendPasswordResetEmail(email.text.toString()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this@AdminCheckActivity, "Olhe seu email!", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this@AdminCheckActivity, "Email não cadastrado", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

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
                    binding.checkPassword.text.clear()
                    dialog.dismiss()
                    Toast.makeText(
                        this@AdminCheckActivity, "Verifique seu email", Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
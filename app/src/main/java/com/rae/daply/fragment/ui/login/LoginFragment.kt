package com.rae.daply.fragment.ui.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.databinding.FragmentLoginBinding
import com.rae.daply.login.LoginActivity
import kotlinx.coroutines.*

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var activity: LoginActivity

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()

        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                Toast.makeText(activity, "Digite seu email e senha", Toast.LENGTH_SHORT).show()
            }
        }

        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        return binding.root
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
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

    private suspend fun compareEmail(email: EditText) = withContext(Dispatchers.IO) {
        if (email.text.toString().isEmpty()) {
            return@withContext
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            return@withContext
        }
        firebaseAuth.sendPasswordResetEmail(email.text.toString()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(activity, "Email enviado", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Email não cadastrado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun getName() {
        val save = FirebaseAuth.getInstance().currentUser?.email?.replace("@etec.sp.gov.br", "")
            ?.replace(".", "-")

        if (!::activity.isInitialized) {
            return
        }

        val sharedPreferences =
            activity.getSharedPreferences("shared_prefs", AppCompatActivity.MODE_PRIVATE)
        val saveShared = sharedPreferences.edit().putString("save", save)
        saveShared.apply()

        val dbReference = FirebaseDatabase.getInstance()
        dbReference.reference.child("Users").child(save.toString()).child("name").get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.value.toString().replaceAfter(" ", "")
                Toast.makeText(activity, "Bem Vindo(a), $name", Toast.LENGTH_SHORT).show()
            }
    }

    private fun login(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val verification = firebaseAuth.currentUser?.isEmailVerified
                if (verification == true) {
                    coroutineScope.launch { getName() }

                    val sharedPreferences = activity.getSharedPreferences(
                        "shared_prefs", AppCompatActivity.MODE_PRIVATE
                    )
                    val isLogged = sharedPreferences.edit()
                    isLogged.putBoolean("isLogged", true)
                    isLogged?.apply()

                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                } else {
                    binding.loginPassword.text?.clear()
                    Toast.makeText(activity, "Verifique seu email", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LoginActivity) {
            activity = context
        }
    }


}

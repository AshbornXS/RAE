package com.rae.daply.fragment.ui.login

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.databinding.FragmentLoginBinding
import com.rae.daply.login.LoginActivity
import kotlinx.coroutines.*
import com.rae.daply.utils.save as saveGlobal

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

        coroutineScope.launch {
            if (Build.VERSION.SDK_INT >= 33) {
                val perms = arrayOf(
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    android.Manifest.permission.READ_MEDIA_IMAGES
                )
                checkPermission(perms, 0)
            } else {
                val perms = arrayOf(
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE
                )
                checkPermission(perms, 1)
            }
        }

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

    override fun onStart() {
        super.onStart()
        coroutineScope.launch(Dispatchers.Main) {
            val currentUser: FirebaseUser? = firebaseAuth.currentUser
            if (currentUser != null && currentUser.isEmailVerified) {
                getName()
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun getName() {
        val save = FirebaseAuth.getInstance().currentUser?.email?.replace("@etec.sp.gov.br", "")
            ?.replace(".", "-")

        saveGlobal = save.toString()

        val dbReference = FirebaseDatabase.getInstance()
        dbReference.reference.child("Users").child(save.toString()).child("name").get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.value.toString().replaceAfter(" ", "")
                Toast.makeText(activity, "Bem Vindo(a), $name", Toast.LENGTH_SHORT).show()
            }
    }

    private fun login(email: String, password: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        builder.setView(R.layout.loading_layout)
        val dialog: AlertDialog = builder.create()
        dialog.show()
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val verification = firebaseAuth.currentUser?.isEmailVerified
                if (verification == true) {
                    dialog.dismiss()
                    coroutineScope.launch { getName() }
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                } else {
                    binding.loginPassword.text.clear()
                    dialog.dismiss()
                    Toast.makeText(activity, "Verifique seu email", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkPermission(permission: Array<String>, requestCode: Int) {
        for (i in permission.indices) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(), permission[i]
                ) == PackageManager.PERMISSION_DENIED
            ) {
                ActivityCompat.requestPermissions(requireActivity(), permission, requestCode)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    @Deprecated("Deprecated in Java")
    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        this.activity = activity as LoginActivity
    }

}

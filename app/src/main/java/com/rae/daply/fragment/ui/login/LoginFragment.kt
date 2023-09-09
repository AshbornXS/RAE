package com.rae.daply.fragment.ui.login

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginFragment : Fragment() {

    // Declaração das variáveis
    private lateinit var binding: FragmentLoginBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var activity: LoginActivity
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)

        // Inicialização da FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        // Configuração do clique no botão de login
        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString()
            val password = binding.loginPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                Toast.makeText(activity, "Digite seu email e senha", Toast.LENGTH_SHORT).show()
            }
        }

        // Configuração do clique no link "Esqueceu a senha?"
        binding.forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        return binding.root
    }

    // Função para exibir o diálogo de recuperação de senha
    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_forgot, null)
        val userEmail = view.findViewById<EditText>(R.id.editBox)
        builder.setCancelable(true)
        builder.setView(view)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        view.findViewById<Button>(R.id.btnReset).setOnClickListener {
            coroutineScope.launch { compareEmail(userEmail) }
            dialog.dismiss()
        }
        dialog.show()
    }

    // Função para comparar e enviar email de recuperação de senha
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

    // Função para realizar o login
    private fun login(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Verificar se o email foi verificado
                val verification = firebaseAuth.currentUser?.isEmailVerified
                if (verification == true) {
                    // Salvar o status de login nas preferências compartilhadas
                    val sharedPreferences = activity.getSharedPreferences(
                        "shared_prefs", AppCompatActivity.MODE_PRIVATE
                    )
                    val isLogged = sharedPreferences.edit()
                    isLogged.putBoolean("isLogged", true)
                    isLogged?.apply()

                    // Redirecionar para a tela principal (MainActivity)
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                } else {
                    // Limpar a senha e mostrar mensagem de verificação de email
                    binding.loginPassword.text?.clear()
                    Toast.makeText(activity, "Verifique seu email", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Função chamada ao destruir o fragmento
    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    // Função para anexar a atividade ao fragmento
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LoginActivity) {
            activity = context
        }
    }
}

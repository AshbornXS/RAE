package com.rae.daply.fragment.ui.login

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.R
import com.rae.daply.data.DataClass
import com.rae.daply.databinding.FragmentSignupBinding
import com.rae.daply.login.AdminCheckActivity
import com.rae.daply.login.LoginActivity
import kotlinx.coroutines.*

class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        setupSpinners()
        setupPasswordFields()
        setupButtonClicks()
        return binding.root
    }

    private fun setupSpinners() {
        // Configurar adaptadores para spinners
        val periodAdapter = ArrayAdapter(
            requireContext(), R.layout.list_item, resources.getStringArray(R.array.periodos)
        )
        val serieAdapter = ArrayAdapter(
            requireContext(), R.layout.list_item, resources.getStringArray(R.array.series)
        )
        val cursoAdapter = ArrayAdapter(
            requireContext(), R.layout.list_item, resources.getStringArray(R.array.cursos)
        )

        binding.signupPeriodo.setAdapter(periodAdapter)
        binding.signupSerie.setAdapter(serieAdapter)
        binding.signupCurso.setAdapter(cursoAdapter)
    }

    private fun setupPasswordFields() {
        // Configurar visibilidade dos campos de senha
        binding.signupPassword.addTextChangedListener(textVisibilityWatcher(binding.confirmPassword))
        binding.signupConfirmPassword.addTextChangedListener(textVisibilityWatcher(binding.arrays))
    }

    private fun textVisibilityWatcher(view: View): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                view.visibility = if (hasText) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun setupButtonClicks() {
        firebaseAuth = FirebaseAuth.getInstance()
        binding.signupButton.setOnClickListener {
            val name = binding.signupName.text.toString()
            val email = binding.signupEmail.text.toString()
            val password = binding.signupPassword.text.toString()
            val passwordConfirm = binding.signupConfirmPassword.text.toString()
            val periodo = binding.signupPeriodo.text.toString()
            val serie = binding.signupSerie.text.toString()
            val curso = binding.signupCurso.text.toString()

            val dialog = createLoadingDialog()
            dialog.show()

            GlobalScope.launch(Dispatchers.IO) {
                handleSignup(name, email, password, passwordConfirm, periodo, serie, curso, dialog)
            }
        }

        binding.signupAdmin.setOnClickListener {
            val intent = Intent(requireContext(), AdminCheckActivity::class.java)
            startActivity(intent)
        }
    }

    private fun createLoadingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(requireContext())
        builder.setCancelable(false)
        builder.setView(R.layout.loading_layout)
        return builder.create()
    }

    private suspend fun handleSignup(
        name: String,
        email: String,
        password: String,
        passwordConfirm: String,
        periodo: String,
        serie: String,
        curso: String,
        dialog: AlertDialog
    ) = withContext(Dispatchers.Main) {
        if (!email.contains("@etec.sp.gov.br")) {
            dialog.dismiss()
            showToast("Digite um email válido")
        } else if (name.isNotEmpty() && email.isNotEmpty() && email.contains("@etec.sp.gov.br") && password.isNotEmpty() && passwordConfirm.isNotEmpty()) {
            if (password == passwordConfirm) {
                val dataClass = DataClass(
                    email = email,
                    userType = "aluno",
                    name = name,
                    periodo = periodo,
                    serie = serie,
                    curso = curso
                )
                val save = getEmailSave(email)

                FirebaseDatabase.getInstance().getReference("Users").child(save).setValue(dataClass)

                createUserWithEmailAndPassword(email, password, dialog)
            } else {
                dialog.dismiss()
                showToast("As senhas não são iguais!")
            }
        } else {
            dialog.dismiss()
            showToast("Preencha todos os campos!")
        }
    }

    private fun getEmailSave(email: String): String {
        return email.replace("@etec.sp.gov.br", "").replace(".", "-")
    }

    private fun createUserWithEmailAndPassword(
        email: String, password: String, dialog: AlertDialog
    ) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.sendEmailVerification()?.addOnSuccessListener {
                        dialog.dismiss()
                        showToast("Email de verificação enviado")
                        val intent = Intent(requireContext(), LoginActivity::class.java)
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
    }

    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}

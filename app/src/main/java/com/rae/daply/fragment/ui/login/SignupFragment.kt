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
import com.rae.daply.login.LoginActivity
import kotlinx.coroutines.*

class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private lateinit var firebaseAuth: FirebaseAuth

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)

        val adaptorItemsPeriodo = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            resources.getStringArray(R.array.periodos)
        )
        binding.signupPeriodo.setAdapter(adaptorItemsPeriodo)

        val adaptorItemsSerie = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            resources.getStringArray(R.array.series)
        )
        binding.signupSerie.setAdapter(adaptorItemsSerie)

        val adaptorItemsCurso = ArrayAdapter(
            requireContext(),
            R.layout.list_item,
            resources.getStringArray(R.array.cursos)
        )
        binding.signupCurso.setAdapter(adaptorItemsCurso)

        binding.signupPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for this implementation
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                binding.confirmPassword.visibility = if (hasText) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed for this implementation
            }
        })

        binding.signupConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not needed for this implementation
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                binding.arrays.visibility = if (hasText) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {
                // Not needed for this implementation
            }
        })

        firebaseAuth = FirebaseAuth.getInstance()

        binding.signupButton.setOnClickListener {
            val name = binding.signupName.text.toString()
            val email = binding.signupEmail.text.toString()
            val password = binding.signupPassword.text.toString()
            val passwordConfirm = binding.signupConfirmPassword.text.toString()
            val periodo = binding.signupPeriodo.text.toString()
            val serie = binding.signupSerie.text.toString()
            val curso = binding.signupCurso.text.toString()

            val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
            builder.setCancelable(false)
            builder.setView(R.layout.loading_layout)
            val dialog: AlertDialog = builder.create()
            dialog.show()

            GlobalScope.launch(Dispatchers.IO) {
                if (!email.contains("@etec.sp.gov.br")) {
                    Toast.makeText(activity, "Digite um email valido", Toast.LENGTH_SHORT).show()
                } else if (password.length < 6) {
                    Toast.makeText(
                        activity,
                        "A senha deve ter no mínimo 6 caracteres!",
                        Toast.LENGTH_SHORT
                    ).show()
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
                        val save = email.replace("@etec.sp.gov.br", "").replace(".", "-")

                        withContext(Dispatchers.Main) {
                            FirebaseDatabase.getInstance().getReference("Users").child(save)
                                .setValue(dataClass)
                        }

                        firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(requireActivity()) { task ->
                                if (task.isSuccessful) {
                                    firebaseAuth.currentUser?.sendEmailVerification()
                                        ?.addOnSuccessListener {
                                            dialog.dismiss()
                                            Toast.makeText(
                                                activity,
                                                "Email de verificação enviado",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            val intent = Intent(
                                                requireContext(), LoginActivity::class.java
                                            )
                                            startActivity(intent)
                                        }?.addOnFailureListener {
                                            dialog.dismiss()
                                            Toast.makeText(
                                                activity,
                                                it.toString(),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else if (task.exception.toString()
                                        .contains("The email address is already in use by another account.")
                                ) {
                                    Toast.makeText(
                                        activity,
                                        "Email já cadastrado!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dialog.dismiss()
                                }
                            }
                    } else {
                        Toast.makeText(activity, "As senhas não são iguais!", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(activity, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.signupAdmin.setOnClickListener {
            val intent =
                Intent(requireContext(), com.rae.daply.login.AdminCheckActivity::class.java)
            startActivity(intent)
        }

        return binding.root
    }
}

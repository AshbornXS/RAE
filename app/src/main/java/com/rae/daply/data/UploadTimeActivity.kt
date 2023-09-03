package com.rae.daply.data

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.R
import com.rae.daply.databinding.ActivityUploadTimeBinding

class UploadTimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadTimeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var i = 0

        // Configurar os ArrayAdapter para os campos de seleção
        val adaptorItemsPeriodo =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.periodos))
        binding.timePeriodo.setAdapter(adaptorItemsPeriodo)

        val adaptorItemsSerie =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.series))
        binding.timeSerie.setAdapter(adaptorItemsSerie)

        val adaptorItemsCurso =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.cursos))
        binding.timeCurso.setAdapter(adaptorItemsCurso)

        binding.addButton.setOnClickListener {
            // Obter os valores dos campos de edição
            val first = binding.firstEditText.text.toString()
            val second = binding.secondEditText.text.toString()
            val third = binding.thirdEditText.text.toString()
            val fourth = binding.fourthEditText.text.toString()
            val fifth = binding.fifthEditText.text.toString()
            val sixth = binding.sixthEditText.text.toString()

            // Inflar a nova linha da tabela com os dados inseridos
            val tableRow = LayoutInflater.from(this)
                .inflate(R.layout.table_row, binding.root, false) as TableRow

            // Preencher os campos da nova linha
            tableRow.findViewById<TextView>(R.id.dayTextView).text =
                resources.getStringArray(R.array.dias)[i % resources.getStringArray(R.array.dias).size]
            tableRow.findViewById<TextView>(R.id.firstTextView).text = first
            tableRow.findViewById<TextView>(R.id.secondTextView).text = second
            tableRow.findViewById<TextView>(R.id.thirdTextView).text = third
            tableRow.findViewById<TextView>(R.id.fourthTextView).text = fourth
            tableRow.findViewById<TextView>(R.id.fifthTextView).text = fifth
            tableRow.findViewById<TextView>(R.id.sixthTextView).text = sixth

            val removeButton = tableRow.findViewById<TableRow>(R.id.removeButton)

            removeButton.setOnClickListener {
                binding.tableLayout.removeView(tableRow)
            }

            binding.tableLayout.addView(tableRow)

            // Obtém os valores da série, curso e período para construir a classe
            val classe = binding.timeSerie.text.toString()
                .take(1) + "-" + binding.timeCurso.text.toString() + "-" + binding.timePeriodo.text.toString()
                .take(1)

            val day =
                resources.getStringArray(R.array.dias)[i % resources.getStringArray(R.array.dias).size]

            val list = ArrayList<String>(
                listOf(
                    first.replace(" ", "-"),
                    second.replace(" ", "-"),
                    third.replace(" ", "-"),
                    fourth.replace(" ", "-"),
                    fifth.replace(" ", "-"),
                    sixth.replace(" ", "-")
                )
            )

            // Salva os dados da aula no Firebase Realtime Database
            FirebaseDatabase.getInstance().getReference("Aulas").child(classe).child(day)
                .setValue(list).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Limpa os campos de edição e exibe mensagem de sucesso
                        binding.firstEditText.text?.clear()
                        binding.secondEditText.text?.clear()
                        binding.thirdEditText.text?.clear()
                        binding.fourthEditText.text?.clear()
                        binding.fifthEditText.text?.clear()
                        binding.sixthEditText.text?.clear()
                        Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    // Em caso de falha, exibe mensagem de erro
                    Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
                }

            if (i == 5) {
                finishActivity(1)
            }

            i++
        }
    }
}

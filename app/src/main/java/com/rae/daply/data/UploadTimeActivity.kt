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

    private val periodo = arrayOf("Manhã", "Tarde", "Noite")
    private val series = arrayOf("1º Ano", "2º Ano", "3º Ano")
    private val cursos = arrayOf("IPIA", "MEC", "MECA", "DS", "ADM", "MEIO", "LOG", "ELECTRO")
    private val dias = arrayOf("Segunda", "Terça", "Quarta", "Quinta", "Sexta")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var i = 0

        val adaptorItemsPeriodo = ArrayAdapter(this, R.layout.list_item, periodo)
        binding.timePeriodo.setAdapter(adaptorItemsPeriodo)
        binding.timePeriodo.setText("Manhã", false)

        val adaptorItemsSerie = ArrayAdapter(this, R.layout.list_item, series)
        binding.timeSerie.setAdapter(adaptorItemsSerie)
        binding.timeSerie.setText("1º Ano", false)

        val adaptorItemsCurso = ArrayAdapter(this, R.layout.list_item, cursos)
        binding.timeCurso.setAdapter(adaptorItemsCurso)
        binding.timeCurso.setText("DS", false)

        binding.addButton.setOnClickListener {
            val first = binding.firstEditText.text.toString()
            val second = binding.secondEditText.text.toString()
            val third = binding.thirdEditText.text.toString()
            val fourth = binding.fourthEditText.text.toString()
            val fifth = binding.fifthEditText.text.toString()
            val sixth = binding.sixthEditText.text.toString()

            val tableRow = LayoutInflater.from(this).inflate(R.layout.table_row, null) as TableRow

            tableRow.findViewById<TextView>(R.id.dayTextView).text = dias[i % dias.size]
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

            val classe = binding.timeSerie.text.toString()
                .take(1) + "-" + binding.timeCurso.text.toString() + "-" + binding.timePeriodo.text.toString()
                .take(1)

            val day = dias[i % dias.size]

            val list = ArrayList<String>(listOf(first, second, third, fourth, fifth, sixth))

            FirebaseDatabase.getInstance().getReference("Aulas").child(classe).child(day)
                .setValue(list).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        binding.firstEditText.text?.clear()
                        binding.secondEditText.text?.clear()
                        binding.thirdEditText.text?.clear()
                        binding.fourthEditText.text?.clear()
                        binding.fifthEditText.text?.clear()
                        binding.sixthEditText.text?.clear()
                        Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
                }

            if (i == 5) {
                finish()
            }

            i++
        }
    }
}
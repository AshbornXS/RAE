package com.rae.daply.data

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase
import com.rae.daply.R
import com.rae.daply.databinding.ActivityUploadTimeBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class UploadTimeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadTimeBinding
    private var x = 1

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadTimeBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        val adapterItemsDias =
            ArrayAdapter(this, R.layout.list_item, resources.getStringArray(R.array.dias))
        binding.day.setAdapter(adapterItemsDias)

        setupDaySpinnerListener()
        binding.txtDayUpload.text = getCurrentDayOfWeek()
        Log.i("fds2", binding.txtDayUpload.text.toString())

        val tableRow =
            LayoutInflater.from(this).inflate(R.layout.table_row, binding.root, false) as TableRow

        binding.addButton.setOnClickListener {
            when (x) {
                1 -> {
                    binding.firstInput.visibility = TextView.GONE
                    binding.secondInput.visibility = TextView.VISIBLE
                }

                2 -> {
                    binding.secondInput.visibility = TextView.GONE
                    binding.thirdInput.visibility = TextView.VISIBLE
                }

                3 -> {
                    binding.thirdInput.visibility = TextView.GONE
                    binding.fourthInput.visibility = TextView.VISIBLE
                }

                4 -> {
                    binding.fourthInput.visibility = TextView.GONE
                    binding.fifthInput.visibility = TextView.VISIBLE
                }

                5 -> {
                    binding.fifthInput.visibility = TextView.GONE
                    binding.sixthInput.visibility = TextView.VISIBLE
                }

                6 -> {
                    binding.addButton.text = "Enviar"
                    setupData(tableRow)
                }
            }
            x++
        }

        val removeButton = tableRow.findViewById<TableRow>(R.id.removeButton)

        removeButton.setOnClickListener {
            binding.tableLayout.removeView(tableRow)
            x--
        }
    }

    private fun setupData(tableRow: TableRow) {
        val first = binding.firstEditText.text.toString()
        val second = binding.secondEditText.text.toString()
        val third = binding.thirdEditText.text.toString()
        val fourth = binding.fourthEditText.text.toString()
        val fifth = binding.fifthEditText.text.toString()
        val sixth = binding.sixthEditText.text.toString()

        // Preencher os campos da nova linha
        tableRow.findViewById<TextView>(R.id.dayTextView).text =
            binding.txtDayUpload.text.toString()
        Log.i("fds3", binding.txtDayUpload.text.toString())
        tableRow.findViewById<TextView>(R.id.firstTextView).text = first
        tableRow.findViewById<TextView>(R.id.secondTextView).text = second
        tableRow.findViewById<TextView>(R.id.thirdTextView).text = third
        tableRow.findViewById<TextView>(R.id.fourthTextView).text = fourth
        tableRow.findViewById<TextView>(R.id.fifthTextView).text = fifth
        tableRow.findViewById<TextView>(R.id.sixthTextView).text = sixth

        binding.tableLayout.addView(tableRow)

        // Obtém os valores da série, curso e período para construir a classe
        val classe = binding.timeSerie.text.toString()
            .take(1) + "-" + binding.timeCurso.text.toString() + "-" + binding.timePeriodo.text.toString()
            .take(1)

        val day = binding.txtDayUpload.text.toString()

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
        FirebaseDatabase.getInstance().getReference("Aulas").child(classe).child(day).setValue(list)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // Limpa os campos de edição e exibe mensagem de sucesso
                    binding.firstEditText.text?.clear()
                    binding.secondEditText.text?.clear()
                    binding.thirdEditText.text?.clear()
                    binding.fourthEditText.text?.clear()
                    binding.fifthEditText.text?.clear()
                    binding.sixthEditText.text?.clear()

                    binding.sixthInput.visibility = TextView.GONE
                    binding.firstInput.visibility = TextView.VISIBLE
                    x = 1

                    Toast.makeText(this, "Salvo", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                // Em caso de falha, exibe mensagem de erro
                Toast.makeText(this, e.message.toString(), Toast.LENGTH_SHORT).show()
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentDayOfWeek(): String {
        // Obter o próximo ou mesmo dia de segunda-feira
        val today = LocalDate.now()

        return if (today.dayOfWeek == DayOfWeek.SATURDAY || today.dayOfWeek == DayOfWeek.SUNDAY) {
            today.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)).dayOfWeek.getDisplayName(
                TextStyle.FULL, Locale("pt", "BR")
            ).replace("-feira", "").lowercase(Locale.ROOT).replaceFirstChar { it.uppercaseChar() }
        } else {
            today.dayOfWeek.getDisplayName(
                TextStyle.FULL, Locale("pt", "BR")
            ).replace("-feira", "").lowercase(Locale.ROOT).replaceFirstChar { it.uppercaseChar() }
        }
    }

    private fun setupDaySpinnerListener() {
        // Configurar o listener do spinner para atualizar a tabela ao selecionar um dia
        binding.day.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                binding.txtDayUpload.text = parent?.getItemAtPosition(position).toString()
                Log.i("fds", binding.txtDayUpload.text.toString())
            }
    }
}

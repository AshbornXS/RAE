package com.rae.daply.fragment.ui

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TableRow
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.databinding.FragmentClassesTimeBinding
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class ClassesTimeFragment : Fragment() {

    private lateinit var binding: FragmentClassesTimeBinding
    private lateinit var activity: MainActivity
    private var classList = listOf("", "", "")

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentClassesTimeBinding.inflate(inflater, container, false)

        setupDaySpinner()
        val dayOfWeek = getCurrentDayOfWeek()

        val sharedPreferences = activity.getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)

        val userType = sharedPreferences.getString("userType", "aluno")!!

        if (userType == "admin") {
            binding.arrays.visibility = View.VISIBLE
            val adaptorItemsPeriodo = ArrayAdapter(
                activity, R.layout.list_item, resources.getStringArray(R.array.periodos)
            )
            binding.aulaPeriodo.setAdapter(adaptorItemsPeriodo)

            val adaptorItemsSerie = ArrayAdapter(
                activity, R.layout.list_item, resources.getStringArray(R.array.series)
            )
            binding.aulaSerie.setAdapter(adaptorItemsSerie)

            val adaptorItemsCurso = ArrayAdapter(
                activity, R.layout.list_item, resources.getStringArray(R.array.cursos)
            )
            binding.aulaCurso.setAdapter(adaptorItemsCurso)

            lifecycleScope.launch {
                getClass().collect {
                    if (it[0] != "" && it[1] != "" && it[2] != "") {
                        val classe = it[0] + "-" + it[1] + "-" + it[2]
                        if (binding.tableLayout.childCount > 1) {
                            binding.tableLayout.removeViews(1, 6)
                        }
                        binding.day.setText(getCurrentDayOfWeek(), false)
                        setupDaySpinnerListener(classe)
                        addAulas(dayOfWeek, classe)
                    }
                }
            }

        } else {
            val classe = sharedPreferences.getString("classe", "aluno")!!

            setupDaySpinnerListener(classe)
            addAulas(dayOfWeek, classe)
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDaySpinner() {
        // Configurar o adaptador do spinner para os dias da semana
        val adapterItemsDias = ArrayAdapter(
            activity, R.layout.list_item, resources.getStringArray(R.array.dias)
        )
        binding.day.setAdapter(adapterItemsDias).also {
            binding.day.setText(getCurrentDayOfWeek(), false)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDaySpinnerListener(classe: String) {
        // Configurar o listener do spinner para atualizar a tabela ao selecionar um dia
        binding.day.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                if (binding.tableLayout.childCount > 1) {
                    binding.tableLayout.removeViews(1, 6)
                }
                val day = parent?.getItemAtPosition(position).toString()
                addAulas(day, classe)
            }
    }

    private fun addAulas(day: String, classe: String) {

        binding.dayOfWeek.text = day

        // Obter os dados de aulas do Firebase
        FirebaseDatabase.getInstance().getReference("Aulas").child(classe).child(day).get()
            .addOnSuccessListener {
                if (it.value == null) {
                    binding.dayOfWeek.text = "Não há aulas"
                } else {
                    val aulas = Gson().fromJson(it.value.toString(), Array<String>::class.java)

                    for (i in aulas.withIndex()) {
                        aulas[i.index] = aulas[i.index].replace("-", " ")
                    }

                    // Iterar sobre as aulas e adicionar à tabela
                    for (i in aulas.withIndex()) {
                        val tableRow = LayoutInflater.from(activity).inflate(
                            R.layout.table_row_user, binding.root, false
                        ) as TableRow

                        tableRow.findViewById<TextView>(R.id.timeTextView).text =
                            activity.resources.getStringArray(R.array.time)[i.index]
                        tableRow.findViewById<TextView>(R.id.firstTextView).text = i.value

                        binding.tableLayout.addView(tableRow)
                    }
                }
            }.addOnFailureListener {
                binding.dayOfWeek.text = "Erro ao carregar aulas"
            }
    }

    private fun getClass() = channelFlow {
        binding.aulaPeriodo.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val periodo = parent?.getItemAtPosition(position).toString().take(1)
                classList = listOf(classList[0], classList[1], periodo)
                runBlocking {
                    channel.trySend(classList)
                }
            }

        binding.aulaSerie.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val serie = parent?.getItemAtPosition(position).toString().take(1)
                classList = listOf(serie, classList[1], classList[2])
                runBlocking {
                    channel.trySend(classList)
                }
            }

        binding.aulaCurso.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val curso = parent?.getItemAtPosition(position).toString()
                classList = listOf(classList[0], curso, classList[2])
                runBlocking {
                    channel.trySend(classList)
                }
            }
        awaitClose {
            channel.close()
        }
    }

    override fun onAttach(mContext: Context) {
        super.onAttach(mContext)
        if (mContext is MainActivity) {
            activity = mContext
        }
    }
}

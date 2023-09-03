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
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.rae.daply.MainActivity
import com.rae.daply.R
import com.rae.daply.databinding.FragmentClassesTimeBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class ClassesTimeFragment : Fragment() {

    private lateinit var binding: FragmentClassesTimeBinding
    private lateinit var activity: MainActivity

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentClassesTimeBinding.inflate(inflater, container, false)

        setupDaySpinner()
        val dayOfWeek = getCurrentDayOfWeek()
        setupDaySpinnerListener()
        addAulas(dayOfWeek)

        return binding.root
    }

    private fun setupDaySpinner() {
        // Configurar o adaptador do spinner para os dias da semana
        val adapterItemsDias = ArrayAdapter(
            activity, R.layout.list_item, resources.getStringArray(R.array.dias)
        )
        binding.day.setAdapter(adapterItemsDias)
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
                if (binding.tableLayout.childCount > 1) {
                    binding.tableLayout.removeViews(1, 6)
                    val day = parent?.getItemAtPosition(position).toString()
                    addAulas(day)
                }
            }
    }

    private fun addAulas(day: String) {
        // Obter a classe do SharedPreferences
        val classe = activity.getSharedPreferences("shared_prefs", Context.MODE_PRIVATE)
            .getString("classe", null).toString()

        binding.dayOfWeek.text = day

        // Obter os dados de aulas do Firebase
        val reference =
            FirebaseDatabase.getInstance().getReference("Aulas").child(classe).child(day)
        reference.get().addOnSuccessListener {
            val aulas = it.value

            val array = Gson().fromJson(aulas.toString(), Array<String>::class.java)

            for (i in array.withIndex()) {
                array[i.index] = array[i.index].replace("-", " ")
            }

            if (aulas == null) {
                binding.dayOfWeek.text = "Não há aulas"
            } else {
                // Iterar sobre as aulas e adicionar à tabela
                for (i in array.withIndex()) {
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

    override fun onAttach(mContext: Context) {
        super.onAttach(mContext)
        if (mContext is MainActivity) {
            activity = mContext
        }
    }
}

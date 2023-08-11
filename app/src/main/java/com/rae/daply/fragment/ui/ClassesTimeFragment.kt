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
import com.rae.daply.R
import com.rae.daply.databinding.FragmentClassesTimeBinding
import com.rae.daply.utils.curso
import com.rae.daply.utils.periodo
import com.rae.daply.utils.save
import com.rae.daply.utils.serie
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

class ClassesTimeFragment : Fragment() {

    private lateinit var binding: FragmentClassesTimeBinding

    private lateinit var mContext: Context

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentClassesTimeBinding.inflate(inflater, container, false)
        val view = binding.root

        val adaptorItemsDias =
            ArrayAdapter(mContext, R.layout.list_item, resources.getStringArray(R.array.dias))
        binding.day.setAdapter(adaptorItemsDias)

        val today = LocalDate.now().dayOfWeek.getDisplayName(
            TextStyle.FULL, Locale("pt", "BR")
        ).replace("-feira", "").lowercase(Locale.ROOT).replaceFirstChar { it.uppercaseChar() }

        binding.day.setText(today, false)

        binding.day.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                if (binding.tableLayout.childCount > 0) {
                    binding.tableLayout.removeViews(1, 6)
                    val day = parent?.getItemAtPosition(position).toString()
                    addAulas(day)
                }
            }

        addAulas(today)

        return view
    }

    private fun addAulas(day: String) {
        FirebaseDatabase.getInstance().reference.child("Users").child(save).get()
            .addOnSuccessListener { snapshot ->
                serie = snapshot.child("serie").value.toString()
                curso = snapshot.child("curso").value.toString()
                periodo = snapshot.child("periodo").value.toString()

                val classe = serie.take(1) + "-" + curso + "-" + periodo.take(1)
                binding.dayOfWeek.text = day

                FirebaseDatabase.getInstance().getReference("Aulas").child(classe).child(day).get()
                    .addOnSuccessListener {
                        val aulas = it.value

                        val gson = Gson()
                        val array = gson.fromJson(aulas.toString(), Array<String>::class.java)

                        if (aulas == null) {
                            binding.dayOfWeek.text = "Não há aulas"
                        } else {
                            for (i in array.withIndex()) {
                                val tableRow = LayoutInflater.from(mContext).inflate(
                                    R.layout.table_row_user, binding.root, false
                                ) as TableRow

                                tableRow.findViewById<TextView>(R.id.timeTextView).text =
                                    resources.getStringArray(R.array.time)[i.index]
                                tableRow.findViewById<TextView>(R.id.firstTextView).text = i.value

                                binding.tableLayout.addView(tableRow)
                            }
                        }
                    }.addOnFailureListener {
                        binding.dayOfWeek.text = "Erro ao carregar aulas"
                    }
            }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }
}
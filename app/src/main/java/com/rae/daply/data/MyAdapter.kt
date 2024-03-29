package com.rae.daply.data

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rae.daply.MainActivity
import com.rae.daply.R

class MyAdapter(private val context: Context, private var avisosArrayList: ArrayList<DataClass>) :
    RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

    // Atualiza os dados do adaptador
    fun updateData(newData: ArrayList<DataClass>) {
        avisosArrayList = newData
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return avisosArrayList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val aviso = avisosArrayList[position]

        Glide.with(context)
            .load(aviso.imageURL)
            .into(holder.imagem)

        holder.titulo.text = aviso.titulo
        holder.aviso.text = aviso.aviso
        holder.data.text = aviso.data

        holder.card.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("image", aviso.imageURL)
                putExtra("titulo", aviso.titulo)
                putExtra("aviso", aviso.aviso)
                putExtra("data", aviso.data)
                putExtra("autor", aviso.autor)
                putExtra("emailAutor", aviso.emailAutor)
                putExtra("key", aviso.key)
                putExtra("type", aviso.type)
            }

            context.startActivity(intent)
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imagem: ImageView = itemView.findViewById(R.id.recImage)
        val titulo: TextView = itemView.findViewById(R.id.recTitulo)
        val aviso: TextView = itemView.findViewById(R.id.recAviso)
        val data: TextView = itemView.findViewById(R.id.recData)
        val card: CardView = itemView.findViewById(R.id.recCard)
    }
}

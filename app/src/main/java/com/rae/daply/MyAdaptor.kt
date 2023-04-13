package com.rae.daply

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class MyAdaptor(val context: Context, private val avisosArrayList: ArrayList<DataClass>) :
    RecyclerView.Adapter<MyAdaptor.MyViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_item, parent, false)
        return MyViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return avisosArrayList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        GlideApp.with(context).load(avisosArrayList[position].imageURL).into(holder.imagem)
        holder.titulo.text = avisosArrayList[position].titulo
        holder.aviso.text = avisosArrayList[position].aviso
        holder.data.text = avisosArrayList[position].data

        holder.card.setOnClickListener {
            val intent = Intent(context, DetailActivity::class.java)
            intent.putExtra("Image", avisosArrayList[position].imageURL)
            intent.putExtra("Titulo", avisosArrayList[position].titulo)
            intent.putExtra("Aviso", avisosArrayList[position].aviso)
            intent.putExtra("Data", avisosArrayList[position].data)
            intent.putExtra("Autor", avisosArrayList[position].autor)

            intent.putExtra("Key", avisosArrayList[position].key)

            context.startActivity(intent)
        }

    }

    class MyViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val imagem: ImageView = ItemView.findViewById(R.id.recImage)
        val titulo: TextView = ItemView.findViewById(R.id.recTitulo)
        val aviso: TextView = ItemView.findViewById(R.id.recAviso)
        val data: TextView = ItemView.findViewById(R.id.recData)

        val card: CardView = ItemView.findViewById(R.id.recCard)
    }
}
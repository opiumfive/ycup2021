package com.opiumfive.plank.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.opiumfive.plank.R
import java.text.SimpleDateFormat

class ExAdapter : RecyclerView.Adapter<ExAdapter.MyViewHolder>() {

    val bitmapList = mutableListOf<Ex>()


    var clickListener: ((position: Int) -> Unit)? = null

    fun setList(bitmaps: List<Ex>) {
        bitmapList.clear()
        bitmapList.addAll(bitmaps)
        notifyDataSetChanged()
    }

    fun deleteItem(pos: Int) {
        bitmapList.removeAt(pos)
        notifyItemRemoved(pos)
    }

    fun addItem(sticker: Ex) {
        bitmapList.add(sticker)
        notifyItemInserted(bitmapList.size - 1)
    }

    fun clear() {
        bitmapList.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.ex_view,
            parent,
            false
        )
        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = bitmapList[holder.adapterPosition]
        holder.name.text = item.name
        holder.date.text = "Дата - " + SimpleDateFormat("dd.MM.yyyy HH:mm").format(item.start)
        val durSec = (item.end.time - item.start.time) / 1000
        holder.dur.text = "Длительность - $durSec сек."
    }

    override fun getItemCount(): Int {
        return bitmapList.size
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var name: TextView
        var date: TextView
        var dur: TextView

        init {
            view.setOnClickListener { clickListener?.invoke(adapterPosition) }
            name = view.findViewById<View>(R.id.name) as TextView
            date = view.findViewById<View>(R.id.date) as TextView
            dur = view.findViewById<View>(R.id.duration) as TextView
        }
    }
}
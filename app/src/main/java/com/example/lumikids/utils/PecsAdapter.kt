package com.example.lumikids.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.lumikids.R
import com.example.lumikids.model.PecsItem

class PecsAdapter(
    private val items: List<PecsItem>,
    private val onClick: (PecsItem) -> Unit
) : RecyclerView.Adapter<PecsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgSentence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sentence, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.img.load(item.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_user)
            error(R.drawable.ic_app_foreground)
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = items.size
}
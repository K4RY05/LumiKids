package com.example.lumikids.utils

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import com.example.lumikids.R
import com.example.lumikids.model.PecsItem

class PecsAdapter(
    private var items: List<PecsItem> = emptyList(),
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
            memoryCachePolicy(CachePolicy.ENABLED)   // caché en RAM
            diskCachePolicy(CachePolicy.ENABLED)     // caché en disco
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = items.size

    // Actualiza la lista eficientemente sin recargar todo
    fun updateItems(newItems: List<PecsItem>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].id == newItems[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newItems[newPos]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }
}
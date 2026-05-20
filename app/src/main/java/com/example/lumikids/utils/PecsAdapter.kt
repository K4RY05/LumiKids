package com.example.lumikids.utils

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import coil.load
import coil.request.CachePolicy
import coil.size.Size
import com.example.lumikids.R
import com.example.lumikids.model.PecsItem

class PecsAdapter(
    private var items: List<PecsItem> = emptyList(),
    private val imageLoader: ImageLoader,
    private val onClick: (PecsItem) -> Unit
) : RecyclerView.Adapter<PecsAdapter.ViewHolder>() {

    private val TAG = "PECS_ADAPTER"

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgSentence)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d("PECS_ADAPTER", "onCreateViewHolder: position en lista de ${items.size} items")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sentence, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        Log.d(TAG, "onBindViewHolder [$position] → text='${item.text}' url='${item.imageUrl}'")

        holder.img.load(item.imageUrl, imageLoader) {
            size(Size.ORIGINAL)
            crossfade(true)
            allowHardware(true)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
            memoryCacheKey(item.imageUrl)
            diskCacheKey(item.imageUrl)
            placeholder(R.drawable.ic_user)
            listener(
                onStart = {
                    Log.d(TAG, "  [LOAD START] '${item.text}' → ${item.imageUrl}")
                },
                onSuccess = { _, result ->
                    Log.d(TAG, "  [LOAD OK]    '${item.text}' dataSource=${result.dataSource}")
                },
                onError = { _, result ->
                    Log.e(TAG, "  [LOAD ERROR] '${item.text}' → ${item.imageUrl} | throwable=${result.throwable?.message}")
                },
                onCancel = {
                    Log.w(TAG, "  [LOAD CANCEL] '${item.text}' → ${item.imageUrl}")
                }
            )
        }

        holder.itemView.setOnClickListener {
            Log.d(TAG, "onClick: '${item.text}' id=${item.id}")
            onClick(item)
        }
    }

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount = ${items.size}")
        return items.size
    }

    fun updateItems(newItems: List<PecsItem>) {
        Log.d(TAG, "updateItems: ${items.size} → ${newItems.size} items")
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
        Log.d(TAG, "updateItems: DiffUtil dispatched")
    }
}
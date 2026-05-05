package com.example.lumikids.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.lumikids.R
import com.example.lumikids.model.NotificationEntity

class NotificacionAdapter(
    private val lista: MutableList<NotificationEntity>,
    private val onDelete: (Int) -> Unit,
    private val onEdit: (NotificationEntity) -> Unit
) : RecyclerView.Adapter<NotificacionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitulo: TextView = view.findViewById(R.id.tvTitulo)
        val tvMensaje: TextView = view.findViewById(R.id.tvMensaje)
        val layoutExpandible: LinearLayout = view.findViewById(R.id.layoutExpandible)
        val ivFlecha: ImageView = view.findViewById(R.id.ivFlecha)
        val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminar)
        val btnEditar: ImageButton = view.findViewById(R.id.btnEditar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val noti = lista[position]

        holder.tvTitulo.text = noti.title
        holder.tvMensaje.text = noti.message

        holder.layoutExpandible.visibility =
            if (noti.isExpanded) View.VISIBLE else View.GONE

        holder.ivFlecha.rotation =
            if (noti.isExpanded) 180f else 0f

        holder.itemView.setOnClickListener {
            noti.isExpanded = !noti.isExpanded
            notifyItemChanged(position)
        }

        holder.btnEliminar.setOnClickListener {
            noti.ID_notification?.let { onDelete(it) }
        }

        holder.btnEditar.setOnClickListener {
            onEdit(noti)
        }
    }

    override fun getItemCount() = lista.size
}
package com.example.lumikids.minigame.objectrecognition.ui

import android.widget.ImageView
import com.bumptech.glide.Glide // ✨ Librería necesaria para las fotos JPG
import com.example.lumikids.R
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.utils.AudioPlayer
import com.example.lumikids.network.RetrofitClient // ✨ Usamos tu URL centralizada

class GameUIHandler(
    private val images: List<ImageView>,
    private val audioManager: AudioPlayer,
    private val onNextRound: () -> Unit,
    private val onError: () -> Unit
) {

    /**
     * ✨ NUEVA FUNCIÓN: Se encarga de pintar las imágenes JPG desde el servidor
     * usando Glide y la URL centralizada de RetrofitClient.
     */
    fun updateImages(options: List<GameObject>) {
        options.forEachIndexed { index, gameObject ->
            val imageView = images[index]

            Glide.with(imageView.context)
                .load(RetrofitClient.BASE_URL_IMAGES + gameObject.imageUrl)
                .placeholder(R.drawable.ic_logo) // Imagen mientras carga
                .error(R.drawable.ic_logo)        // Imagen si falla la red
                .into(imageView)

            imageView.isEnabled = true
        }
    }

    fun handleSelection(view: ImageView, isCorrect: Boolean) {
        if (isCorrect) {
            // Lógica de acierto: marco verde y sonido de victoria
            view.setBackgroundResource(R.drawable.bg_rounded_green)
            audioManager.playEffect(R.raw.win)

            // Deshabilitamos clics para evitar doble selección
            images.forEach {
                it.isEnabled = false
                it.setOnClickListener(null)
            }

            // Esperamos un segundo para pasar a la siguiente ronda
            view.postDelayed({
                onNextRound()
            }, 1000)

        } else {
            // Lógica de error
            onError()

            view.setBackgroundResource(R.drawable.bg_rounded_red)
            audioManager.playEffect(R.raw.fail)

            view.isEnabled = false

            // Regresamos al fondo normal después de medio segundo
            view.postDelayed({
                view.setBackgroundResource(R.drawable.bg_card)
                view.isEnabled = true
            }, 500)
        }
    }

    fun resetImagesBackground() {
        images.forEach {
            it.setBackgroundResource(R.drawable.bg_card)
        }
    }
}
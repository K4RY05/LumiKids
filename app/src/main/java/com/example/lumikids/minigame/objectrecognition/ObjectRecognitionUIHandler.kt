package com.example.lumikids.minigame.objectrecognition

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.lumikids.R
import com.example.lumikids.minigame.utils.GameAudioManager // ✨ Importamos el nuevo gestor
import com.example.lumikids.model.GameObject
import com.example.lumikids.network.RetrofitClient

class ObjectRecognitionUIHandler(
    private val images: List<ImageView>,
    private val resultIcons: List<ImageView>,
    private val gameAudio: GameAudioManager,
    private val onNextRound: () -> Unit,
    private val onError: () -> Unit
) {

    fun updateImages(options: List<GameObject>) {
        options.forEachIndexed { index, gameObject ->
            if (index < images.size) {
                Glide.with(images[index].context)
                    .load(RetrofitClient.BASE_URL_IMAGES + gameObject.imageUrl)
                    .transform(CenterCrop(), RoundedCorners(30))
                    .placeholder(R.drawable.ic_logo)
                    .into(images[index])
            }
        }
    }

    fun handleSelection(view: ImageView, isCorrect: Boolean) {
        // Deshabilitamos clics para evitar toques múltiples durante la validación
        images.forEach { it.isEnabled = false }

        // 1. Encontramos el índice de la imagen tocada para mostrar su icono de resultado
        val index = images.indexOf(view)
        if (index != -1 && index < resultIcons.size) {
            val resultIcon = resultIcons[index]

            // 2. Mostramos el icono visual de acierto o error
            if (isCorrect) {
                resultIcon.setImageResource(R.drawable.ic_correct)
            } else {
                resultIcon.setImageResource(R.drawable.ic_error)
            }
            resultIcon.visibility = View.VISIBLE

            view.postDelayed({
                if (isCorrect) {
                    gameAudio.playEffect(R.raw.win) // ✨ Usamos la nueva clase

                    // Esperamos un momento antes de pasar a la siguiente ronda
                    view.postDelayed({
                        onNextRound()
                    }, 1000)
                } else {
                    onError()
                    gameAudio.playEffect(R.raw.fail) // ✨ Usamos la nueva clase

                    // Ocultamos el icono de error y rehabilitamos las imágenes para reintentar
                    resultIcon.visibility = View.INVISIBLE
                    images.forEach { it.isEnabled = true }
                }
            }, 500) // Reducido a 500ms para que la respuesta visual sea más ágil para el niño
        }
    }

    fun resetImagesBackground() {
        // Ocultamos todos los iconos al iniciar una nueva ronda
        resultIcons.forEach {
            it.visibility = View.INVISIBLE
        }
        images.forEach { it.isEnabled = true }
    }
}
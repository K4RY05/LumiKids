package com.example.lumikids.minigame.objectrecognition.ui

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.lumikids.R
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.utils.AudioPlayer
import com.example.lumikids.network.RetrofitClient

class GameUIHandler(
    private val images: List<ImageView>,
    private val resultIcons: List<ImageView>, // ✨ AÑADIMOS ESTA LISTA AL CONSTRUCTOR
    private val audioManager: AudioPlayer,
    private val onNextRound: () -> Unit,
    private val onError: () -> Unit
) {

    fun updateImages(options: List<GameObject>) {
        // ... (esta función se queda igual) ...
    }

    fun handleSelection(view: ImageView, isCorrect: Boolean) {

        images.forEach { it.isEnabled = false }

        // ✨ 1. ENCONTRAMOS EL ICONO CORRESPONDIENTE A LA CARTA TOCADA
        val index = images.indexOf(view)
        val resultIcon = resultIcons[index]

        // ✨ 2. MOSTRAMOS EL ICONO DE CHECK O EQUIS
        if (isCorrect) {
            resultIcon.setImageResource(R.drawable.ic_correct) // Pon tu icono verde aquí
            resultIcon.visibility = View.VISIBLE
        } else {
            resultIcon.setImageResource(R.drawable.ic_error) // Pon tu icono rojo aquí
            resultIcon.visibility = View.VISIBLE
        }

        view.postDelayed({

            if (isCorrect) {
                audioManager.playEffect(R.raw.win)
                images.forEach { it.setOnClickListener(null) }

                view.postDelayed({
                    onNextRound()
                }, 1000)

            } else {
                onError()
                audioManager.playEffect(R.raw.fail)

                // ✨ OCULTAMOS EL ICONO ROJO DESPUÉS DE LA PAUSA
                resultIcon.visibility = View.INVISIBLE

                images.forEach { it.isEnabled = true }
            }

        }, 1000)
    }

    fun resetImagesBackground() {
        // ✨ OCULTAMOS TODOS LOS ICONOS AL INICIAR UNA NUEVA RONDA
        resultIcons.forEach {
            it.visibility = View.INVISIBLE
        }
    }
}
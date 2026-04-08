package com.example.lumikids.minigame.objectrecognition.ui

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.lumikids.R
import com.example.lumikids.minigame.objectrecognition.model.GameObject
import com.example.lumikids.minigame.utils.AudioPlayer
import com.example.lumikids.network.RetrofitClient

class GameUIHandler(
    private val images: List<ImageView>,
    private val audioManager: AudioPlayer,
    private val onNextRound: () -> Unit,
    private val onError: () -> Unit
) {

    fun updateImages(options: List<GameObject>) {
        options.forEachIndexed { index, gameObject ->
            val imageView = images[index]

            Glide.with(imageView.context)
                .load(RetrofitClient.BASE_URL_IMAGES + gameObject.imageUrl)
                .placeholder(R.drawable.ic_logo)
                .error(R.drawable.ic_logo)
                .into(imageView)

            imageView.isEnabled = true
        }
    }

    fun handleSelection(view: ImageView, isCorrect: Boolean) {

        images.forEach { it.isEnabled = false }

        // 2. Damos feedback visual inmediato (cambio de color)
        if (isCorrect) {
            view.setBackgroundResource(R.drawable.bg_rounded_green)
        } else {
            view.setBackgroundResource(R.drawable.bg_rounded_red)
        }


        view.postDelayed({

            // 4. Después de la pausa, ejecutamos la lógica de acierto/error
            if (isCorrect) {
                audioManager.playEffect(R.raw.win)

                // Quitamos los clics por completo porque ya ganó esta ronda
                images.forEach { it.setOnClickListener(null) }

                // Esperamos 1 segundo extra para que escuche el sonido de victoria antes de pasar de nivel
                view.postDelayed({
                    onNextRound()
                }, 1000)

            } else {
                onError()
                audioManager.playEffect(R.raw.fail)

                // Regresamos la tarjeta a su fondo normal después de equivocarse
                view.setBackgroundResource(R.drawable.bg_card)

                // Volvemos a habilitar las tarjetas para que el niño lo siga intentando
                images.forEach { it.isEnabled = true }
            }

        }, 1000)
    }

    fun resetImagesBackground() {
        images.forEach {
            it.setBackgroundResource(R.drawable.bg_card)
        }
    }
}
package com.example.lumikids.minigame.memorygame.ui

import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.lumikids.R
import com.example.lumikids.network.RetrofitClient

class MemoryGameUIHandler {
    fun flipCardUp(view: ImageView, imageUrl: String) {
        val duration = 150L

        // Primera mitad de la animación: girar hasta ponerse de perfil
        view.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                // ✨ SINCRONIZACIÓN CON EL SERVIDOR: Cargamos usando la URL base de imágenes
                Glide.with(view.context)
                    .load(RetrofitClient.BASE_URL_IMAGES + imageUrl)
                    .placeholder(R.drawable.ic_logo) // Imagen temporal mientras descarga
                    .transform(CenterCrop(), RoundedCorners(30)) // Estilo visual uniforme
                    .into(view)

                // Segunda mitad: aparecer desde el otro lado
                view.rotationY = -90f
                view.animate()
                    .rotationY(0f)
                    .setDuration(duration)
                    .start()
            }.start()
    }


    fun flipCardsDown(view1: ImageView, view2: ImageView, defaultImage: Int, onComplete: () -> Unit) {
        val duration = 150L

        // Animamos la primera carta
        view1.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                view1.setImageResource(defaultImage) // Volver a la imagen de la "espalda" de la carta
                view1.rotationY = -90f
                view1.animate().rotationY(0f).setDuration(duration).start()
            }.start()

        // Animamos la segunda carta
        view2.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                view2.setImageResource(defaultImage)
                view2.rotationY = -90f
                view2.animate()
                    .rotationY(0f)
                    .setDuration(duration)
                    .withEndAction {
                        // Avisamos a la Activity que la animación terminó para habilitar clics de nuevo
                        onComplete()
                    }.start()
            }.start()
    }

    fun showMatchEffect(view1: ImageView, view2: ImageView) {
        // Podríamos añadir un pequeño escalado para celebrar el éxito
        view1.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).withEndAction {
            view1.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
        }.start()

        view2.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).withEndAction {
            view2.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
        }.start()
    }
}
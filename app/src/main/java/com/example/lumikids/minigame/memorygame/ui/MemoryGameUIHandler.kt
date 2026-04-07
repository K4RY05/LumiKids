package com.example.lumikids.minigame.memorygame.ui

import android.widget.ImageView
import com.bumptech.glide.Glide // ✨ Importamos Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop // ✨ Importación para centrar
import com.bumptech.glide.load.resource.bitmap.RoundedCorners // ✨ Importación para bordes redondos
import com.example.lumikids.network.RetrofitClient // ✨ Importamos la IP de tu servidor Node.js

class MemoryGameUIHandler {

    // Función para voltear la carta y mostrar su imagen oculta (boca arriba)
    fun flipCardUp(view: ImageView, imageUrl: String) {
        val duration = 150L

        view.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                // ✨ MAGIA AQUÍ: Usamos Glide con bordes redondeados justo cuando la carta está de perfil
                Glide.with(view.context)
                    .load(RetrofitClient.BASE_URL_IMAGES + imageUrl)
                    .transform(CenterCrop(), RoundedCorners(30)) // ✨ Aplicamos la curvatura de 30px
                    .into(view)

                view.rotationY = -90f
                view.animate().rotationY(0f).setDuration(duration).start()
            }.start()
    }


    fun flipCardsDown(view1: ImageView, view2: ImageView, defaultImage: Int, onComplete: () -> Unit) {
        val duration = 150L

        // Animamos la primera carta
        view1.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                view1.setImageResource(defaultImage)
                view1.rotationY = -90f
                view1.animate().rotationY(0f).setDuration(duration).start()
            }.start()

        // Animamos la segunda carta y, al terminar, avisamos a la Activity
        view2.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                view2.setImageResource(defaultImage)
                view2.rotationY = -90f
                view2.animate().rotationY(0f).setDuration(duration).withEndAction {
                    // Avisamos que la animación terminó para desbloquear los clics
                    onComplete()
                }.start()
            }.start()
    }
}
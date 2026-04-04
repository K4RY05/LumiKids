package com.example.lumikids.minigame.memorygame.ui

import android.widget.ImageView

class MemoryGameUIHandler {

    // Función para voltear la carta y mostrar su imagen oculta (boca arriba)
    fun flipCardUp(view: ImageView, newImage: Int) {
        val duration = 150L

        view.animate()
            .rotationY(90f)
            .setDuration(duration)
            .withEndAction {
                view.setImageResource(newImage)
                view.rotationY = -90f
                view.animate().rotationY(0f).setDuration(duration).start()
            }.start()
    }

    // Función para voltear dos cartas de regreso a su estado original (boca abajo)
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
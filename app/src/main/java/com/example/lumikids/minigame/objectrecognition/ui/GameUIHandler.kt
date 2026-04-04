package com.example.lumikids.minigame.objectrecognition.ui

import android.widget.ImageView
import com.example.lumikids.R
import com.example.lumikids.minigame.utils.AudioPlayer

class GameUIHandler(
    private val images: List<ImageView>,
    private val audioManager: AudioPlayer,
    private val onNextRound: () -> Unit,
    private val onError: () -> Unit
) {

    fun handleSelection(view: ImageView, isCorrect: Boolean) {
        if (isCorrect) {
            // Lógica de acierto
            view.setBackgroundResource(R.drawable.bg_rounded_green)
            audioManager.playEffect(R.raw.win)

            images.forEach {
                it.isEnabled = false
                it.setOnClickListener(null)
            }

            // Usamos la misma vista para el delay, es igual de efectivo que el tvInstruction
            view.postDelayed({
                onNextRound()
            }, 1000)

        } else {
            // Lógica de error
            onError() // Avisamos a la Activity para que sume el error

            view.setBackgroundResource(R.drawable.bg_rounded_red)
            audioManager.playEffect(R.raw.fail)

            view.isEnabled = false

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
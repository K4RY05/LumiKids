package com.example.lumikids.minigame.utils

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.widget.ImageButton
import android.widget.TextView
import com.example.lumikids.R
import com.example.lumikids.model.GameResult

class ScoreManager(private val activity: Activity) {

    fun showResults(result: GameResult, onFinish: () -> Unit) {
        val builder = AlertDialog.Builder(activity)
        val view = activity.layoutInflater.inflate(R.layout.game_results, null)

        val dialog = builder.setView(view).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)


        view.findViewById<TextView>(R.id.tvTimeValue).text = "${result.timeSeconds}s"
        view.findViewById<TextView>(R.id.tvErrorValue).text = result.errors.toString()

        view.findViewById<ImageButton>(R.id.btnResultClose).setOnClickListener {
            dialog.dismiss()
            onFinish()
        }

        dialog.show()
    }
}
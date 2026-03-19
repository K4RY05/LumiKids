package com.example.lumikids.minigame.utils

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.lumikids.R

object GamePopupHelper {

    fun showStartPopup(activity: Activity) {
        val inflater = activity.layoutInflater
        val popupView = inflater.inflate(R.layout.popup_inicio, null)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        popupWindow.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = false

        // Usamos 'activity' para acceder a la pantalla correcta
        activity.window.decorView.post {
            if (!activity.isFinishing && !activity.isDestroyed) {
                popupWindow.showAtLocation(activity.window.decorView, Gravity.CENTER, 0, 0)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (popupWindow.isShowing) {
                popupWindow.dismiss()
            }
        }, 2000)
    }
}
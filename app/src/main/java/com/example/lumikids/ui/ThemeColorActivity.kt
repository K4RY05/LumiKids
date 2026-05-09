package com.example.lumikids.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.example.lumikids.R
import com.example.lumikids.utils.SessionManager

class ThemeColorActivity : BaseActivity() {

    private lateinit var sessionManager: SessionManager

    private lateinit var checkBlue: ImageView
    private lateinit var checkPink: ImageView
    private lateinit var checkGreen: ImageView
    private lateinit var checkRed: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color)

        sessionManager = SessionManager(this)

        checkBlue = findViewById(R.id.imgCheck1)
        checkPink = findViewById(R.id.imgCheck2)
        checkGreen = findViewById(R.id.imgCheck3)
        checkRed = findViewById(R.id.imgCheck4)

        findViewById<View>(R.id.btnTheme1).setOnClickListener { changeTheme("blue") }
        findViewById<View>(R.id.btnTheme2).setOnClickListener { changeTheme("pink") }
        findViewById<View>(R.id.btnTheme3).setOnClickListener { changeTheme("green") }
        findViewById<View>(R.id.btnTheme4).setOnClickListener { changeTheme("red") }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        markSelectedTopic()
    }


    private fun changeTheme(themeName: String) {
        sessionManager.setTheme(themeName)
        Toast.makeText(this, "Paleta actualizada", Toast.LENGTH_SHORT).show()
        recreate()
    }

    private fun markSelectedTopic() {
        val temaActual = sessionManager.getTheme()

        checkBlue.visibility = View.INVISIBLE
        checkPink.visibility = View.INVISIBLE
        checkGreen.visibility = View.INVISIBLE
        checkRed.visibility = View.INVISIBLE

        when (temaActual) {
            "blue" -> checkBlue.visibility = View.VISIBLE
            "pink" -> checkPink.visibility = View.VISIBLE
            "green" -> checkGreen.visibility = View.VISIBLE
            "red" -> checkRed.visibility = View.VISIBLE
            else -> checkBlue.visibility = View.VISIBLE // Azul por defecto
        }
    }
}
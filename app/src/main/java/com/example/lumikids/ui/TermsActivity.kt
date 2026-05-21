package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import java.io.BufferedReader
import java.io.InputStreamReader

class TermsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        val tvTerms = findViewById<TextView>(R.id.tvTermsText)
        tvTerms.text = readRawTextFile(R.raw.terms)

        val btnFinish = findViewById<Button>(R.id.btnFinish)

        btnFinish.setOnClickListener {
            // Redirigir al LoginActivity para que el usuario inicie sesión
            val intent = Intent(this, LoginActivity::class.java)

            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
        }
    }

    private fun readRawTextFile(resId: Int): String {
        val inputStream = resources.openRawResource(resId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val text = StringBuilder()
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            text.append(line).append('\n')
        }

        reader.close()
        return text.toString()
    }
}
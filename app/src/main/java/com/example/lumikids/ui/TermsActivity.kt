package com.example.lumikids.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import java.io.BufferedReader
import java.io.InputStreamReader

import android.content.Intent
import android.widget.Button
import com.example.lumikids.MainActivity

class TermsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        val tvTerms = findViewById<TextView>(R.id.tvTermsText)
        tvTerms.text = readRawTextFile(R.raw.terms)


        val btnFinish = findViewById<Button>(R.id.btnFinish)

        btnFinish.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
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

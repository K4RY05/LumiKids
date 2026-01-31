package com.example.lumikids

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class TermsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        val tvTerms = findViewById<TextView>(R.id.tvTermsText)
        tvTerms.text = readRawTextFile(R.raw.terms)
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

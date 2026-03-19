package com.example.lumikids.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.lumikids.R
import com.example.lumikids.minigame.objectrecognition.ui.GameActivityN1

class ObjectLevelPickerActivity : AppCompatActivity() {

    // Variables de estado del seleccionador
    private var selectedRounds: Int = 3 // Valor por defecto (como en image_1)
    private val minRounds = 1
    private val maxRounds = 10 // Límite razonable

    // Componentes de la Interfaz
    private lateinit var tvRoundCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_object_level)

        // 1. Recibimos el tema seleccionado de la pantalla anterior
        val theme = intent.getStringExtra("THEME") ?: "FURNITURE"

        // 2. Vinculamos vistas
        tvRoundCount = findViewById(R.id.tvRoundCount)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnDecrease = findViewById<ImageView>(R.id.btnDecrease)
        val btnIncrease = findViewById<ImageView>(R.id.btnIncrease)
        val btnStart = findViewById<TextView>(R.id.btnStart)

        // Actualizar UI inicial
        updateUi()

        // 3. Lógica de los botones del selector (Flechas rojas)
        btnDecrease.setOnClickListener {
            if (selectedRounds > minRounds) {
                selectedRounds--
                updateUi()
            }
        }

        btnIncrease.setOnClickListener {
            if (selectedRounds < maxRounds) {
                selectedRounds++
                updateUi()
            }
        }

        // 4. Lógica de navegación
        btnBack.setOnClickListener { finish() }

        // BOTÓN JUGAR: Inicia el juego pasando el tema Y el número de rondas
        btnStart.setOnClickListener {
            val intent = Intent(this, GameActivityN1::class.java).apply {
                putExtra("THEME", theme)
                putExtra("NUM_ROUNDS", selectedRounds) // ¡Enviamos el dato elegido!
            }
            startActivity(intent)
            // finish() // Opcional: Cerrar el seleccionador para que no vuelvan a él al dar atrás
        }
    }

    private fun updateUi() {
        tvRoundCount.text = selectedRounds.toString()
    }
}
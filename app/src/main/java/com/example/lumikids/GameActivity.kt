package com.example.lumikids

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// 1. Creamos un modelo de datos simple para representar cada objeto
data class LumiObject(val name: String, val imageResId: Int)

class GameActivity : AppCompatActivity() {

    private lateinit var imgOption1: ImageView
    private lateinit var imgOption2: ImageView
    private lateinit var imgOption3: ImageView
    private lateinit var btnBack: ImageView

    private var currentCorrectObject: LumiObject? = null
    private var currentThemeItems: List<LumiObject> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // 2. Vincular las vistas

        imgOption1 = findViewById(R.id.imgOption1)
        imgOption2 = findViewById(R.id.imgOption2)
        imgOption3 = findViewById(R.id.imgOption3)
        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        // 3. Recibir los datos de la pantalla anterior
        val theme = intent.getStringExtra("THEME") ?: "FOOD"
        val gameType = intent.getStringExtra("GAME_TYPE") ?: "OBJECT"

        if (gameType == "OBJECT") {
            // Cargar la lista de objetos según el tema
            currentThemeItems = getItemsForTheme(theme)
            startNewRound()
        } else {
            // Aquí irá el código del memorama en el futuro
            Toast.makeText(this, "Memorama en construcción", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startNewRound() {
        if (currentThemeItems.size < 3) return // Prevención de errores si hay pocas imágenes

        // 4. Elegir 3 objetos al azar
        val shuffledItems = currentThemeItems.shuffled()
        val roundOptions = shuffledItems.take(3)

        // 5. De esos 3, elegir 1 como la respuesta correcta
        currentCorrectObject = roundOptions.random()



        // 7. Poner las imágenes en los botones
        imgOption1.setImageResource(roundOptions[0].imageResId)
        imgOption2.setImageResource(roundOptions[1].imageResId)
        imgOption3.setImageResource(roundOptions[2].imageResId)

        // 8. Configurar los clics
        setupOptionClick(imgOption1, roundOptions[0])
        setupOptionClick(imgOption2, roundOptions[1])
        setupOptionClick(imgOption3, roundOptions[2])
    }

    private fun setupOptionClick(imageView: ImageView, clickedObject: LumiObject) {
        imageView.setOnClickListener {
            if (clickedObject == currentCorrectObject) {
                // ¡Correcto!
                Toast.makeText(this, "¡Muy bien!", Toast.LENGTH_SHORT).show()
                // Iniciar nueva ronda después de un pequeño retraso
                imageView.postDelayed({
                    startNewRound()
                }, 1000) // Espera 1 segundo
            } else {
                // Incorrecto
                Toast.makeText(this, "Intenta de nuevo", Toast.LENGTH_SHORT).show()
                // Aquí podrías agregar una pequeña animación de error
            }
        }
    }

    // 🔴 CAMBIA R.drawable.xxx POR TUS IMÁGENES REALES
    private fun getItemsForTheme(theme: String): List<LumiObject> {
        return when (theme) {
            "FOOD" -> listOf(
                LumiObject("la Manzana", R.drawable.ic_logo), // Cambia ic_logo por tu manzana
                LumiObject("el Plátano", R.drawable.ic_logo),
                LumiObject("las Uvas", R.drawable.ic_logo),
                LumiObject("la Naranja", R.drawable.ic_logo)
            )
            "EMOTIONS" -> listOf(
                LumiObject("Feliz", R.drawable.ic_emocionv2),
                LumiObject("Triste", R.drawable.ic_logo),
                LumiObject("Enojado", R.drawable.ic_logo),
                LumiObject("Asustado", R.drawable.ic_logo)
            )
            "CLOTHES" -> listOf(
                LumiObject("la Camisa", R.drawable.ic_logo),
                LumiObject("el Pantalón", R.drawable.ic_logo),
                LumiObject("los Zapatos", R.drawable.ic_logo),
                LumiObject("el Gorro", R.drawable.ic_logo)
            )
            else -> emptyList()
        }
    }
}
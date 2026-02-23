package com.example.lumikids

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// 1. Modelo de datos actualizado con audioResId
data class LumiObject(val name: String, val imageResId: Int, val audioResId: Int)

class GameActivity : AppCompatActivity() {

    private lateinit var imgOption1: ImageView
    private lateinit var imgOption2: ImageView
    private lateinit var imgOption3: ImageView
    private lateinit var btnBack: ImageView

    private var mediaPlayer: MediaPlayer? = null

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
            // Cargar la lista de objetos de forma dinámica
            currentThemeItems = getItemsForTheme(theme)

            // Verificamos si se encontraron elementos antes de jugar
            if (currentThemeItems.isEmpty()) {
                Toast.makeText(this, "Agrega imágenes en drawable y audios en raw", Toast.LENGTH_LONG).show()
            } else {
                startNewRound()
            }
        } else {
            // Aquí irá el código del memorama en el futuro
            Toast.makeText(this, "Memorama en construcción", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startNewRound() {
        if (currentThemeItems.size < 3) {
            Toast.makeText(this, "Se necesitan al menos 3 objetos para jugar", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Elegir 3 objetos al azar
        val shuffledItems = currentThemeItems.shuffled()
        val roundOptions = shuffledItems.take(3)

        // 5. De esos 3, elegir 1 como la respuesta correcta
        currentCorrectObject = roundOptions.random()

                // 6. REPRODUCIR EL AUDIO DE LA INSTRUCCIÓN
                // Libera el audio anterior si existía para no amontonar sonidos
                mediaPlayer?.release()
                currentCorrectObject?.let { correctObj ->
                    // Crea el reproductor con el audio del objeto ganador y lo inicia
                    mediaPlayer = MediaPlayer.create(this, correctObj.audioResId)
                    mediaPlayer?.start()
                }



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
                reproducirEfecto(R.raw.win)
                Toast.makeText(this, "¡Muy bien!", Toast.LENGTH_SHORT).show()
                // Iniciar nueva ronda después de un pequeño retraso
                imageView.postDelayed({
                    startNewRound()
                }, 1000) // Espera 1 segundo
            } else {
                reproducirEfecto(R.raw.fail)
                Toast.makeText(this, "Intenta de nuevo", Toast.LENGTH_SHORT).show()
            }
        }

    }

    // LECTURA DINÁMICA AUTOMÁTICA
    private fun getItemsForTheme(theme: String): List<LumiObject> {
        val prefijo = when (theme) {
            "FOOD" -> "food_"
            "EMOTIONS" -> "emo_"
            "CLOTHES" -> "clother_"
            else -> return emptyList()
        }

        val listaAutomatica = mutableListOf<LumiObject>()
        val todosLosDrawables = R.drawable::class.java.fields

        for (archivo in todosLosDrawables) {
            val nombreArchivo = archivo.name

            if (nombreArchivo.startsWith(prefijo)) {
                try {
                    val idImagen = archivo.getInt(null)
                    // Busca el audio con el mismo nombre en la carpeta res/raw/
                    val idAudio = resources.getIdentifier(nombreArchivo, "raw", packageName)

                    //if (idAudio != 0)
                    if (idAudio != 0) {
                        // Limpia el nombre por si necesitas imprimirlo (convierte food_manzana -> Manzana)
                        val nombreLimpio = nombreArchivo.removePrefix(prefijo)
                            .replace("_", " ")
                            .replaceFirstChar { it.uppercase() }

                        listaAutomatica.add(LumiObject(nombreLimpio, idImagen, idAudio))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return listaAutomatica
    }

    private fun reproducirEfecto(sonidoResId: Int) {
        val efectoPlayer = MediaPlayer.create(this, sonidoResId)
        efectoPlayer.setOnCompletionListener {
            it.release() // Destruye el reproductor en cuanto termina el sonido
        }
        efectoPlayer.start()
    }



    //  liberar el reproductor cuando se cierra la pantalla
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
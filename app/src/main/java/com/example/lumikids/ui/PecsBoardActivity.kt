package com.example.lumikids.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.lumikids.R
import com.example.lumikids.model.PecsItem
import com.example.lumikids.utils.PecsAdapter
import com.example.lumikids.utils.SessionManager
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class PecsBoardActivity : AppCompatActivity() {

    private lateinit var rvOptions: RecyclerView
    private lateinit var sentenceBar: LinearLayout
    private var currentStage = "pronoun"
    private var selectedPronounFolder = "" // Guardará si es "he", "she", "men", "women"
    private var mediaPlayer: MediaPlayer? = null

    private val BASE_URL = "http://192.168.100.251:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pecsboard)
        initClickListeners()
        rvOptions = findViewById(R.id.rvOptions)
        sentenceBar = findViewById(R.id.sentenceBar)
        rvOptions.layoutManager = GridLayoutManager(this, 3)

        clearBoardInDB {
            loadPecs("pronoun")
        }
    }
    private fun initClickListeners() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun loadPecs(category: String) {
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId() ?: return

        // Si la categoría es verbos, le agregamos el pronombre seleccionado para el filtrado
        val endpoint = if (category == "verb") "verb/$selectedPronounFolder" else category
        val url = "$BASE_URL/api/pecs/board/$endpoint/$userId"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(url).readText()
                val array = JSONArray(json)
                val list = mutableListOf<PecsItem>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PecsItem(
                            id = obj.getInt("id"),
                            text = obj.getString("text"),
                            imageUrl = obj.getString("imageUrl"),
                            type = obj.optString("type", "")
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    rvOptions.adapter = PecsAdapter(list) { onItemSelected(it) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PecsBoardActivity, "Error de red: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onItemSelected(item: PecsItem) {
        addToSentenceBar(item)
        saveSelectionToDB(item.id)

        // 🔥 LÓGICA DINÁMICA DE AUDIO
        val audioName = if (currentStage == "pronoun") {
            when {
                item.imageUrl.contains("/she/") -> "she"
                item.imageUrl.contains("/he/") -> "he"
                item.imageUrl.contains("/women/") -> "women"
                item.imageUrl.contains("/men/") -> "men"
                else -> item.text
            }
        } else {
            item.text // Para los verbos y complementos, el nombre suele ser exacto ("cut", "eat")
        }

        val soundUrl = "$BASE_URL/sounds/$currentStage/$audioName.mp3"
        Log.d("AUDIO_DEBUG", "Intentando reproducir URL: $soundUrl")

        playSound(soundUrl)

        // LÓGICA DE TRANSICIÓN ENTRE TABLEROS
        when (currentStage) {
            "pronoun" -> {
                selectedPronounFolder = audioName // Reutilizamos el nombre deducido (he, she, etc.)
                currentStage = "verb"
                loadPecs("verb")
            }
            "verb" -> {
                currentStage = "complement"
                loadPecs("food") // O la categoría que corresponda (school, games, etc.)
            }
            "complement" -> {
                Toast.makeText(this, "¡Oración completa! Muy bien 👏", Toast.LENGTH_SHORT).show()

                // Reinicio automático para hacer la siguiente oración
                sentenceBar.removeAllViews()
                currentStage = "pronoun"
                selectedPronounFolder = ""

                clearBoardInDB {
                    loadPecs("pronoun")
                }
            }
        }
    }

    private fun addToSentenceBar(item: PecsItem) {
        val view = LayoutInflater.from(this).inflate(R.layout.item_sentence, sentenceBar, false)
        val img = view.findViewById<ImageView>(R.id.imgSentence)

        img.load(item.imageUrl) {
            crossfade(true)
        }

        // Eliminar tarjeta si se toca la barra superior
        view.setOnClickListener {
            sentenceBar.removeView(view)
            deleteSelectionFromDB(item.id)
        }

        sentenceBar.addView(view)
    }

    // --- FUNCIONES DE RED Y MULTIMEDIA ---

    private fun playSound(soundUrl: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(soundUrl)

                setOnErrorListener { _, what, extra ->
                    Log.e("AUDIO_ERROR", "Error del MediaPlayer. Código: what=$what extra=$extra")
                    true
                }

                prepareAsync()
                setOnPreparedListener {
                    Log.d("AUDIO_DEBUG", "Audio descargado correctamente. ¡Reproduciendo!")
                    start()
                }
            }
        } catch (e: Exception) {
            Log.e("AUDIO_ERROR", "Error reproduciendo: ${e.message}")
        }
    }

    private fun saveSelectionToDB(themeId: Int) {
        val userId = SessionManager(this).getUserId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/pecs/board/select")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val jsonParam = JSONObject()
                jsonParam.put("ID_theme", themeId)
                jsonParam.put("ID_user", userId)

                OutputStreamWriter(conn.outputStream).use { it.write(jsonParam.toString()) }
                conn.responseCode
            } catch (e: Exception) {
                Log.e("DB_Error", "No se pudo guardar: ${e.message}")
            }
        }
    }

    private fun deleteSelectionFromDB(themeId: Int) {
        val userId = SessionManager(this).getUserId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/pecs/board/item/$userId/$themeId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.responseCode
            } catch (e: Exception) {
                Log.e("DB_Error", "No se pudo eliminar: ${e.message}")
            }
        }
    }

    private fun clearBoardInDB(onSuccess: () -> Unit = {}) {
        val userId = SessionManager(this).getUserId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/pecs/board/$userId")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "DELETE"
                conn.responseCode

                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Log.e("DB_Error", "No se pudo limpiar el tablero: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
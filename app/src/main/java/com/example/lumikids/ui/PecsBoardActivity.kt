package com.example.lumikids.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.lumikids.R
import com.example.lumikids.model.PecsItem
import com.example.lumikids.pecsadapter.PecsAdapter
import com.example.lumikids.utils.SessionManager // Asegúrate de importar el SessionManager
import kotlinx.coroutines.*
import org.json.JSONArray
import java.net.URL

class PecsBoardActivity : AppCompatActivity() {

    private lateinit var rvOptions: RecyclerView
    private lateinit var sentenceBar: LinearLayout
    private var currentStage = "pronoun"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pecsboard)

        rvOptions = findViewById(R.id.rvOptions)
        sentenceBar = findViewById(R.id.sentenceBar)
        rvOptions.layoutManager = GridLayoutManager(this, 3)

        // 🔥 Cargar primer tablero
        loadPecs("pronoun")
    }

    private fun loadPecs(category: String) {

        // 🔥 AQUÍ ESTÁ EL CAMBIO CLAVE: Usamos tu SessionManager en lugar de LumiPrefs
        val sessionManager = SessionManager(this)
        val userId = sessionManager.getUserId()

        // 1. Validamos que el ID exista.
        if (userId == null) {
            Toast.makeText(
                this,
                "Error: Usuario no logueado o ID no encontrado en SessionManager.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 2. Construimos la URL con la variable ya validada
        val url = "http://192.168.100.132:300/api/pecs/board/$category/$userId"

        // 3. Mostramos la URL para confirmar en pantalla
        Toast.makeText(this, "URL: $url", Toast.LENGTH_SHORT).show()

        // 4. Hacemos la petición de red
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(url).readText()
                val array = JSONArray(json)
                val list = mutableListOf<PecsItem>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PecsItem(
                            obj.getString("text"),
                            obj.getString("imageUrl"),
                            obj.optString("type", "")
                        )
                    )
                }

                // 5. Volvemos al hilo principal (Main) para actualizar la UI
                withContext(Dispatchers.Main) {
                    rvOptions.adapter = PecsAdapter(list) { onItemSelected(it) }
                }
            } catch (e: Exception) {
                // Si hay un error (como Cleartext traffic, Timeout, etc.), lo mostramos en un Toast
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PecsBoardActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun onItemSelected(item: PecsItem) {
        addToSentenceBar(item)

        when (currentStage) {
            "pronoun" -> {
                currentStage = "verb"
                loadPecs("verb")
            }
            "verb" -> {
                currentStage = "complement"
                loadPecs("food")
            }
            "complement" -> {
                Toast.makeText(this, "Oración completa", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addToSentenceBar(item: PecsItem) {
        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_sentence, sentenceBar, false)
        val img = view.findViewById<ImageView>(R.id.imgSentence)

        // Cargar imagen con Coil
        img.load(item.imageUrl) {
            crossfade(true)
            placeholder(R.drawable.ic_launcher_background)
            error(R.drawable.ic_launcher_background)
        }

        sentenceBar.addView(view)
    }
}
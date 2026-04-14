package com.example.lumikids.ui

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
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
    private var selectedPronounFolder = ""
    private var mediaPlayer: MediaPlayer? = null

    private val BASE_URL = "http://192.168.100.251:3000"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pecsboard)

        rvOptions = findViewById(R.id.rvOptions)
        sentenceBar = findViewById(R.id.sentenceBar)

        initClickListeners()

        // El RecyclerView principal mantiene sus 3 columnas
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

    private fun onItemSelected(item: PecsItem) {
        // Solo permitir 3 items en la barra de oración
        if (sentenceBar.childCount < 3) {
            addToSentenceBar(item)
            saveSelectionToDB(item.id)

            val audioName = if (currentStage == "pronoun") {
                when {
                    item.imageUrl.contains("/she/") -> "she"
                    item.imageUrl.contains("/he/") -> "he"
                    item.imageUrl.contains("/women/") -> "women"
                    item.imageUrl.contains("/men/") -> "men"
                    else -> item.text
                }
            } else {
                item.text
            }

            playSound("$BASE_URL/sounds/$currentStage/$audioName.mp3")

            when (currentStage) {
                "pronoun" -> {
                    selectedPronounFolder = audioName
                    currentStage = "verb"
                    loadPecs("verb")
                }
                "verb" -> {
                    currentStage = "complement"
                    loadPecs("food")
                }
                "complement" -> {
                    Toast.makeText(this, "¡Oración completa! 👏", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addToSentenceBar(item: PecsItem) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.item_sentence, sentenceBar, false)

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
        params.setMargins(1, 1, 1, 1)
        view.layoutParams = params

        val img = view.findViewById<ImageView>(R.id.imgSentence)
        img.load(item.imageUrl) {
            crossfade(true)
        }

        view.setOnClickListener {
            sentenceBar.removeView(view)
            deleteSelectionFromDB(item.id)
        }

        sentenceBar.addView(view)
    }


    private fun loadPecs(category: String) {
        val userId = SessionManager(this).getUserId() ?: return
        val endpoint = if (category == "verb") "verb/$selectedPronounFolder" else category
        val url = "$BASE_URL/api/pecs/board/$endpoint/$userId"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(url).readText()
                val array = JSONArray(json)
                val list = mutableListOf<PecsItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(PecsItem(obj.getInt("id"), obj.getString("text"), obj.getString("imageUrl"), obj.optString("type", "")))
                }
                withContext(Dispatchers.Main) {
                    rvOptions.adapter = PecsAdapter(list) { onItemSelected(it) }
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", e.message.toString())
            }
        }
    }

    private fun playSound(soundUrl: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(soundUrl)
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) { Log.e("AUDIO", e.message.toString()) }
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
                val jsonParam = JSONObject().apply { put("ID_theme", themeId); put("ID_user", userId) }
                OutputStreamWriter(conn.outputStream).use { it.write(jsonParam.toString()) }
                conn.responseCode
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun deleteSelectionFromDB(themeId: Int) {
        val userId = SessionManager(this).getUserId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/pecs/board/item/$userId/$themeId")
                val conn = (url.openConnection() as HttpURLConnection).apply { requestMethod = "DELETE" }
                conn.responseCode
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun clearBoardInDB(onSuccess: () -> Unit = {}) {
        val userId = SessionManager(this).getUserId() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("$BASE_URL/api/pecs/board/$userId")
                val conn = (url.openConnection() as HttpURLConnection).apply { requestMethod = "DELETE" }
                conn.responseCode
                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
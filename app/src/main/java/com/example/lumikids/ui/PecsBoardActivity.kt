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
import com.example.lumikids.network.RetrofitClient
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pecsboard)

        rvOptions = findViewById(R.id.rvOptions)
        sentenceBar = findViewById(R.id.sentenceBar)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<ImageButton>(R.id.btnClear).setOnClickListener {
            removeLastItem()
        }

        rvOptions.layoutManager = GridLayoutManager(this, 3)

        clearBoardInDB {
            resetToPronouns()
        }
    }

    // =========================
    // SELECCIÓN DE ITEMS
    // =========================
    private fun onItemSelected(item: PecsItem) {

        if (sentenceBar.childCount >= 3) return

        addToSentenceBar(item)
        saveSelectionToDB(item.id)

        val audioName = getAudioName(item)
        playSound("${RetrofitClient.BASE_URL_SOUNDS}$currentStage/$audioName.mp3")

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

    private fun getAudioName(item: PecsItem): String {
        return if (currentStage == "pronoun") {
            when {
                item.imageUrl.contains("/she/") -> "she"
                item.imageUrl.contains("/he/") -> "he"
                item.imageUrl.contains("/women/") -> "women"
                item.imageUrl.contains("/men/") -> "men"
                else -> item.text
            }
        } else item.text
    }

    // =========================
    // AGREGAR A LA BARRA
    // =========================
    private fun addToSentenceBar(item: PecsItem) {

        val view = LayoutInflater.from(this)
            .inflate(R.layout.item_sentence, sentenceBar, false)

        view.tag = item // 🔥 NECESARIO PARA EL BOTÓN CLEAR

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
        params.setMargins(1, 1, 1, 1)
        view.layoutParams = params

        val img = view.findViewById<ImageView>(R.id.imgSentence)
        img.load(item.imageUrl)

        view.setOnClickListener {

            val index = sentenceBar.indexOfChild(view)

            sentenceBar.removeView(view)
            deleteSelectionFromDB(item.id)

            handleDeletion(index)
        }

        sentenceBar.addView(view)
    }

    // =========================
    // LÓGICA DE ELIMINACIÓN
    // =========================
    private fun handleDeletion(index: Int) {

        when (index) {

            0 -> {
                resetToPronouns()
            }

            1 -> {
                currentStage = "verb"

                if (sentenceBar.childCount > 1) {
                    sentenceBar.removeViews(1, sentenceBar.childCount - 1)
                }

                loadPecs("verb")
            }

            2 -> {
                currentStage = "complement"
                loadPecs("food")
            }
        }
    }

    private fun resetToPronouns() {
        currentStage = "pronoun"
        selectedPronounFolder = ""
        sentenceBar.removeAllViews()
        loadPecs("pronoun")
    }

    // =========================
    // API GET
    // =========================
    private fun loadPecs(category: String) {

        val userId = SessionManager(this).getUserId() ?: return
        val endpoint = if (category == "verb") "verb/$selectedPronounFolder" else category

        val url = "${RetrofitClient.BASE_URL}api/pecs/board/$endpoint/$userId"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(url).readText()
                val array = JSONArray(json)

                val list = mutableListOf<PecsItem>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)

                    list.add(
                        PecsItem(
                            obj.getInt("id"),
                            obj.getString("text"),
                            obj.getString("imageUrl"),
                            obj.optString("type", "")
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    rvOptions.adapter = PecsAdapter(list) { onItemSelected(it) }
                }

            } catch (e: Exception) {
                Log.e("API_ERROR", e.message.toString())
            }
        }
    }

    // =========================
    // AUDIO
    // =========================
    private fun playSound(url: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(url)
                prepareAsync()
                setOnPreparedListener { start() }
            }
        } catch (e: Exception) {
            Log.e("AUDIO", e.message.toString())
        }
    }

    // =========================
    // API POST
    // =========================
    private fun saveSelectionToDB(themeId: Int) {

        val userId = SessionManager(this).getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${RetrofitClient.BASE_URL}api/pecs/board/select")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val json = JSONObject().apply {
                    put("ID_theme", themeId)
                    put("ID_user", userId)
                }

                OutputStreamWriter(conn.outputStream).use {
                    it.write(json.toString())
                }

                conn.responseCode

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =========================
    // API DELETE ITEM
    // =========================
    private fun deleteSelectionFromDB(themeId: Int) {

        val userId = SessionManager(this).getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${RetrofitClient.BASE_URL}api/pecs/board/item/$userId/$themeId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                }
                conn.responseCode

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =========================
    // LIMPIAR TABLERO
    // =========================
    private fun clearBoardInDB(onSuccess: () -> Unit = {}) {

        val userId = SessionManager(this).getUserId() ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${RetrofitClient.BASE_URL}api/pecs/board/$userId")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                }
                conn.responseCode

                withContext(Dispatchers.Main) {
                    onSuccess()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =========================
    // BOTÓN CLEAR (ELIMINA ÚLTIMO)
    // =========================
    private fun removeLastItem() {

        val count = sentenceBar.childCount
        if (count == 0) return

        val lastIndex = count - 1
        val lastView = sentenceBar.getChildAt(lastIndex)
        val item = lastView.tag as? PecsItem ?: return

        sentenceBar.removeViewAt(lastIndex)
        deleteSelectionFromDB(item.id)

        handleDeletion(lastIndex)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}